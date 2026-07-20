package com.sanu.carouselforge.features.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.error.ErrorObserver
import com.sanu.carouselforge.core.error.LogcatErrorObserver
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.prefs.UserPreferencesRepository
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LocalProjectRepository
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ProjectFileStore
import com.sanu.carouselforge.data.repository.ProjectRepository
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType
import com.sanu.carouselforge.features.editor.render.TransformDelta
import com.sanu.carouselforge.features.editor.snap.GuideEngine
import com.sanu.carouselforge.features.editor.snap.SnapEngine
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EditorState {
    data object Loading : EditorState
    data class Editing(
        val layers: List<LayerModel>,
        val selectedLayerId: String?,
        val gridSnapEnabled: Boolean,
        val canvasWidth: Int,
        val canvasHeight: Int,
        val slideCount: Int,
        val bgColorStart: Long = 0xFFFFFFFFL,
        val bgColorEnd: Long? = null,
        val safeZoneVisible: Boolean = false,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
        val activeGuides: GuideEngine.GuideResult? = null,
        val notice: AppError? = null,
    ) : EditorState {
        /** Total logical canvas width across all connected slides. */
        val totalWidth: Int get() = canvasWidth * slideCount

        val selectedLayer: LayerModel? get() = layers.firstOrNull { it.id == selectedLayerId }
    }
    data class Exporting(val progress: Float) : EditorState
    data class Error(val error: AppError) : EditorState
}

class EditorViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val fileStore: ProjectFileStore,
    private val userPreferences: UserPreferencesRepository? = null,
    private val errorObserver: ErrorObserver = LogcatErrorObserver,
) : ViewModel() {
    private val mutableState = MutableStateFlow<EditorState>(EditorState.Loading)
    val state: StateFlow<EditorState> = mutableState.asStateFlow()
    private var project: Project? = null

    private val undoStack = ArrayDeque<HistoryEntry>()
    private val redoStack = ArrayDeque<HistoryEntry>()
    private var gestureRecorded = false

    // Raw (un-snapped) finger position for the layer being dragged. Keeping it separate
    // from the displayed position is what gives snapping single-threshold hysteresis.
    private var dragRawX = 0f
    private var dragRawY = 0f

    private data class HistoryEntry(
        val layers: List<LayerModel>,
        val selectedLayerId: String?,
        val canvasWidth: Int,
        val canvasHeight: Int,
        val slideCount: Int,
        val bgColorStart: Long,
        val bgColorEnd: Long?,
    )

    init {
        loadProject()
    }

    private fun EditorState.Editing.toHistoryEntry() = HistoryEntry(
        layers = layers,
        selectedLayerId = selectedLayerId,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        slideCount = slideCount,
        bgColorStart = bgColorStart,
        bgColorEnd = bgColorEnd,
    )

    private fun EditorState.Editing.applying(entry: HistoryEntry) = copy(
        layers = entry.layers,
        selectedLayerId = entry.selectedLayerId,
        canvasWidth = entry.canvasWidth,
        canvasHeight = entry.canvasHeight,
        slideCount = entry.slideCount,
        bgColorStart = entry.bgColorStart,
        bgColorEnd = entry.bgColorEnd,
    )

    private fun recordHistory(current: EditorState.Editing) {
        undoStack.addLast(current.toHistoryEntry())
        while (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
    }

    /**
     * Central path for every content edit: optionally records an undo checkpoint,
     * applies the change, refreshes undo/redo availability, and autosaves.
     */
    private fun editWithHistory(
        recordHistory: Boolean = true,
        block: (EditorState.Editing) -> EditorState.Editing,
    ) {
        val current = mutableState.value as? EditorState.Editing ?: return
        if (recordHistory) recordHistory(current)
        mutableState.value = block(current).copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
        )
        snapshotAndAutosave()
    }

    fun undo() {
        val current = mutableState.value as? EditorState.Editing ?: return
        val entry = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current.toHistoryEntry())
        mutableState.value = current.applying(entry).copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
        )
        snapshotAndAutosave()
    }

    fun redo() {
        val current = mutableState.value as? EditorState.Editing ?: return
        val entry = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current.toHistoryEntry())
        mutableState.value = current.applying(entry).copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
        )
        snapshotAndAutosave()
    }

    fun selectLayer(id: String) {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(selectedLayerId = id) else current
        }
    }

    fun deselectLayer() {
        mutableState.update { current ->
            if (current is EditorState.Editing && current.selectedLayerId != null) {
                current.copy(selectedLayerId = null)
            } else {
                current
            }
        }
    }

    fun transformLayer(id: String, delta: TransformDelta, thresholdPx: Float = 0f) {
        mutableState.update { current ->
            if (current !is EditorState.Editing) return@update current
            val layer = current.layers.firstOrNull { it.id == id } ?: return@update current
            // Record one undo checkpoint per drag, only once real movement happens, and
            // seed the raw-position tracker from the layer's current position.
            if (!gestureRecorded) {
                recordHistory(current)
                gestureRecorded = true
                dragRawX = layer.x
                dragRawY = layer.y
            }
            dragRawX += delta.panX
            dragRawY += delta.panY
            val probe = layer.copy(
                x = dragRawX,
                y = dragRawY,
                scale = (layer.scale * delta.zoom).coerceIn(MIN_SCALE, MAX_SCALE),
                rotation = layer.rotation + delta.rotation,
            )
            val guides = GuideEngine.compute(
                moving = CanvasTransforms.layerBounds(probe),
                siblings = current.layers.filterNot { it.id == id }
                    .map(CanvasTransforms::layerBounds),
                totalWidth = current.totalWidth.toFloat(),
                canvasHeight = current.canvasHeight.toFloat(),
                cutLinesX = slideBoundaryXs(current),
                thresholdPx = thresholdPx,
            )
            val snapped = probe.copy(
                x = dragRawX + guides.translationXPx,
                y = dragRawY + guides.translationYPx,
            )
            current.copy(
                layers = current.layers.map { if (it.id == id) snapped else it },
                activeGuides = guides.takeIf { it.didSnap || it.badges.isNotEmpty() },
            )
        }
    }

    fun finishGesture(id: String, thresholdPx: Float, gridSpacingPx: Float) {
        // History was recorded on the drag's first move, so it undoes as one step.
        gestureRecorded = false
        editWithHistory(recordHistory = false) { current ->
            val moving = current.layers.firstOrNull { it.id == id } ?: return@editWithHistory current
            val result = SnapEngine.resolve(
                moving = CanvasTransforms.layerBounds(moving),
                siblings = current.layers.filterNot { it.id == id }
                    .map(CanvasTransforms::layerBounds),
                thresholdPx = thresholdPx,
                gridSpacingPx = gridSpacingPx.takeIf { current.gridSnapEnabled },
                snapLinesX = slideBoundaryXs(current),
            )
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id != id) return@map layer
                    CanvasTransforms.clampToCanvas(
                        layer = layer.copy(
                            x = layer.x + result.translationXPx,
                            y = layer.y + result.translationYPx,
                        ),
                        totalWidth = current.totalWidth.toFloat(),
                        canvasHeight = current.canvasHeight.toFloat(),
                    )
                },
                activeGuides = null,
            )
        }
    }

    fun setGridSnapEnabled(enabled: Boolean) {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(gridSnapEnabled = enabled) else current
        }
    }

    fun setSafeZoneVisible(visible: Boolean) {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(safeZoneVisible = visible) else current
        }
    }

    fun reportNotice(error: AppError) {
        errorObserver.record(error)
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(notice = error) else current
        }
    }

    fun dismissNotice() {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(notice = null) else current
        }
    }

    fun importImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val imported = fileStore.importImage(projectId, context, uri)
                addImage(imported.localPath, imported.aspectRatio)
            } catch (error: Exception) {
                reportNotice(
                    AppError.ImageDecodeError(
                        uri = uri.toString(),
                        reason = error.message ?: "Could not read image",
                    ),
                )
            }
        }
    }

    fun importReplacementImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val imported = fileStore.importImage(projectId, context, uri)
                replaceSelectedImage(imported.localPath, imported.aspectRatio)
            } catch (error: Exception) {
                reportNotice(
                    AppError.ImageDecodeError(
                        uri = uri.toString(),
                        reason = error.message ?: "Could not read image",
                    ),
                )
            }
        }
    }

    /**
     * Applies a new per-slide ratio to every slide at once. Each layer keeps its
     * fractional center within the total canvas and its own size/scale, then is
     * clamped back inside the reshaped bounds.
     */
    fun setRatio(preset: CanvasPreset) {
        val current = mutableState.value as? EditorState.Editing ?: return
        if (current.canvasWidth == preset.width && current.canvasHeight == preset.height) return
        editWithHistory { editing ->
            editing.copy(
                canvasWidth = preset.width,
                canvasHeight = preset.height,
                layers = CanvasTransforms.reflowForRatio(
                    layers = editing.layers,
                    oldTotalWidth = editing.totalWidth.toFloat(),
                    oldHeight = editing.canvasHeight.toFloat(),
                    newTotalWidth = (preset.width * editing.slideCount).toFloat(),
                    newHeight = preset.height.toFloat(),
                ),
            )
        }
    }

    /** Grows or shrinks the connected carousel. Shrinking clamps layers back in bounds. */
    fun setSlideCount(count: Int) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val clamped = count.coerceIn(MIN_SLIDE_COUNT, MAX_SLIDE_COUNT)
        if (clamped == current.slideCount) return
        editWithHistory { editing ->
            val newTotalWidth = (editing.canvasWidth * clamped).toFloat()
            val canvasHeight = editing.canvasHeight.toFloat()
            editing.copy(
                slideCount = clamped,
                layers = if (clamped < editing.slideCount) {
                    editing.layers.map {
                        CanvasTransforms.clampToCanvas(it, newTotalWidth, canvasHeight)
                    }
                } else {
                    editing.layers
                },
            )
        }
    }

    fun setBackground(colorStart: Long, colorEnd: Long?) {
        editWithHistory { it.copy(bgColorStart = colorStart, bgColorEnd = colorEnd) }
    }

    fun addImage(uri: String, aspectRatio: Float = 1f) {
        editWithHistory { current ->
            val (width, height) = containedLayerSize(
                aspectRatio = aspectRatio,
                canvasWidth = current.canvasWidth.toFloat(),
                canvasHeight = current.canvasHeight.toFloat(),
                coverage = DEFAULT_LAYER_COVERAGE,
            )
            val (posX, posY) = cascadedTopLeft(current, width, height)
            val layer = LayerModel(
                id = UUID.randomUUID().toString(),
                type = LayerType.IMAGE,
                imageUri = uri,
                x = posX,
                y = posY,
                width = width,
                height = height,
                zIndex = current.layers.size,
            )
            current.copy(
                layers = current.layers + layer,
                selectedLayerId = layer.id,
            )
        }
    }

    fun replaceSelectedImage(uri: String, aspectRatio: Float = 1f) {
        val selectedId = (mutableState.value as? EditorState.Editing)?.selectedLayerId ?: return
        editWithHistory { current ->
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id == selectedId &&
                        (layer.type == LayerType.IMAGE || layer.type == LayerType.STICKER)
                    ) {
                        val (width, height) = containedLayerSize(
                            aspectRatio = aspectRatio,
                            canvasWidth = current.canvasWidth.toFloat(),
                            canvasHeight = current.canvasHeight.toFloat(),
                            coverage = DEFAULT_LAYER_COVERAGE,
                        )
                        layer.copy(
                            imageUri = uri,
                            width = width,
                            height = height,
                            x = (current.canvasWidth - width) / 2f,
                            y = (current.canvasHeight - height) / 2f,
                            scale = 1f,
                            rotation = 0f,
                        )
                    } else {
                        layer
                    }
                },
            )
        }
    }

    fun fitSelectedLayer() {
        val selectedId = (mutableState.value as? EditorState.Editing)?.selectedLayerId ?: return
        editWithHistory { current ->
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id != selectedId) return@map layer
                    val (width, height) = containedLayerSize(
                        aspectRatio = layer.width / layer.height,
                        canvasWidth = current.canvasWidth.toFloat(),
                        canvasHeight = current.canvasHeight.toFloat(),
                        coverage = FIT_LAYER_COVERAGE,
                    )
                    layer.copy(
                        x = (current.canvasWidth - width) / 2f,
                        y = (current.canvasHeight - height) / 2f,
                        width = width,
                        height = height,
                        scale = 1f,
                        rotation = 0f,
                    )
                },
            )
        }
    }

    fun moveSelectedLayer(forward: Boolean) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selectedId = current.selectedLayerId ?: return
        editWithHistory { editing ->
            val ordered = editing.layers.sortedBy(LayerModel::zIndex).toMutableList()
            val index = ordered.indexOfFirst { it.id == selectedId }
            if (index < 0) return@editWithHistory editing
            val target = (index + if (forward) 1 else -1).coerceIn(ordered.indices)
            if (target == index) return@editWithHistory editing
            val moved = ordered.removeAt(index)
            ordered.add(target, moved)
            editing.copy(
                layers = ordered.mapIndexed { zIndex, layer -> layer.copy(zIndex = zIndex) },
            )
        }
    }

    fun removeLayer(id: String) {
        editWithHistory { current ->
            current.copy(
                layers = current.layers
                    .filterNot { it.id == id }
                    .mapIndexed { zIndex, layer -> layer.copy(zIndex = zIndex) },
                selectedLayerId = current.selectedLayerId.takeUnless { it == id },
            )
        }
    }

    fun addText(text: String = "Tap to edit") {
        editWithHistory { current ->
            val width = current.canvasWidth * TEXT_LAYER_WIDTH_FRACTION
            val height = current.canvasHeight * TEXT_LAYER_HEIGHT_FRACTION
            val (posX, posY) = cascadedTopLeft(current, width, height)
            val layer = LayerModel(
                id = UUID.randomUUID().toString(),
                type = LayerType.TEXT,
                text = text,
                x = posX,
                y = posY,
                width = width,
                height = height,
                textSizeSp = defaultTextSizeForLayer(height, current.canvasWidth.toFloat()),
                zIndex = current.layers.size,
            )
            current.copy(layers = current.layers + layer, selectedLayerId = layer.id)
        }
    }

    fun addShape(kind: com.sanu.carouselforge.data.repository.ShapeKind, fillColor: Long) {
        editWithHistory { current ->
            val size = current.canvasWidth * SHAPE_LAYER_FRACTION
            val (posX, posY) = cascadedTopLeft(current, size, size)
            val layer = LayerModel(
                id = UUID.randomUUID().toString(),
                type = LayerType.SHAPE,
                shapeKind = kind,
                fillColor = fillColor,
                x = posX,
                y = posY,
                width = size,
                height = size,
                zIndex = current.layers.size,
            )
            current.copy(layers = current.layers + layer, selectedLayerId = layer.id)
        }
    }

    fun addSticker(uri: String, aspectRatio: Float = 1f) {
        editWithHistory { current ->
            val (width, height) = containedLayerSize(
                aspectRatio = aspectRatio,
                canvasWidth = current.canvasWidth.toFloat(),
                canvasHeight = current.canvasHeight.toFloat(),
                coverage = STICKER_LAYER_COVERAGE,
            )
            val (posX, posY) = cascadedTopLeft(current, width, height)
            val layer = LayerModel(
                id = UUID.randomUUID().toString(),
                type = LayerType.STICKER,
                imageUri = uri,
                x = posX,
                y = posY,
                width = width,
                height = height,
                zIndex = current.layers.size,
            )
            current.copy(layers = current.layers + layer, selectedLayerId = layer.id)
        }
    }

    /**
     * Applies a style change to the selected layer. [pushHistory] should be true for the
     * first change of an editing session (a slider drag, a text edit) and false for the
     * continuous updates that follow, so the whole session collapses into one undo step.
     */
    fun updateSelectedLayer(pushHistory: Boolean = true, transform: (LayerModel) -> LayerModel) {
        val selectedId = (mutableState.value as? EditorState.Editing)?.selectedLayerId ?: return
        editWithHistory(recordHistory = pushHistory) { current ->
            current.copy(
                layers = current.layers.map { if (it.id == selectedId) transform(it) else it },
            )
        }
    }

    fun duplicateSelectedLayer() {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selected = current.selectedLayer ?: return
        editWithHistory { editing ->
            val copy = selected.copy(
                id = UUID.randomUUID().toString(),
                x = selected.x + DUPLICATE_OFFSET,
                y = selected.y + DUPLICATE_OFFSET,
                zIndex = editing.layers.size,
            )
            editing.copy(layers = editing.layers + copy, selectedLayerId = copy.id)
        }
    }

    /** Places a copy of the selected layer at the same in-slide position on every slide. */
    fun copySelectedToAllSlides() {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selected = current.selectedLayer ?: return
        if (current.slideCount <= 1) return
        // The slide the selected layer currently sits in (by its center).
        val originSlide = ((selected.x + selected.width / 2f) / current.canvasWidth)
            .toInt()
            .coerceIn(0, current.slideCount - 1)
        editWithHistory { editing ->
            val additions = (0 until editing.slideCount)
                .filter { it != originSlide }
                .mapIndexed { offsetIndex, slide ->
                    selected.copy(
                        id = UUID.randomUUID().toString(),
                        x = selected.x + (slide - originSlide) * editing.canvasWidth,
                        zIndex = editing.layers.size + offsetIndex,
                    )
                }
            editing.copy(layers = editing.layers + additions)
        }
    }

    /** Reorders layers to match the given top-to-bottom (highest-z first) id list. */
    fun reorderLayers(idsTopToBottom: List<String>) {
        editWithHistory { current ->
            val byId = current.layers.associateBy { it.id }
            val bottomToTop = idsTopToBottom.asReversed().mapNotNull { byId[it] }
            if (bottomToTop.size != current.layers.size) return@editWithHistory current
            current.copy(
                layers = bottomToTop.mapIndexed { zIndex, layer -> layer.copy(zIndex = zIndex) },
            )
        }
    }

    /**
     * Immediately persists any pending edits (canceling the debounced autosave) so a
     * navigation to export never races the 400ms save window.
     */
    fun flushPendingSave(onFlushed: () -> Unit) {
        val snapshot = buildSnapshot()
        if (snapshot == null) {
            onFlushed()
            return
        }
        project = snapshot
        viewModelScope.launch {
            runCatching { repository.saveProject(snapshot) }
                .onFailure { errorObserver.record(AppError.StorageError(it)) }
            onFlushed()
        }
    }

    private fun loadProject() {
        viewModelScope.launch {
            try {
                val loaded = repository.getProject(projectId)
                project = loaded
                val gridDefault = userPreferences?.preferences?.first()?.gridEnabledByDefault ?: true
                mutableState.value = EditorState.Editing(
                    layers = loaded.layers.map(::layerModel),
                    selectedLayerId = null,
                    gridSnapEnabled = gridDefault,
                    canvasWidth = loaded.canvasWidth,
                    canvasHeight = loaded.canvasHeight,
                    slideCount = loaded.slideCount,
                    bgColorStart = loaded.bgColorStart,
                    bgColorEnd = loaded.bgColorEnd,
                )
            } catch (error: Exception) {
                val appError = AppError.StorageError(error)
                errorObserver.record(appError)
                mutableState.value = EditorState.Error(appError)
            }
        }
    }

    private fun buildSnapshot(): Project? {
        val editing = mutableState.value as? EditorState.Editing ?: return null
        val previous = project ?: return null
        return previous.copy(
            updatedAt = System.currentTimeMillis(),
            canvasWidth = editing.canvasWidth,
            canvasHeight = editing.canvasHeight,
            slideCount = editing.slideCount,
            bgColorStart = editing.bgColorStart,
            bgColorEnd = editing.bgColorEnd,
            layers = editing.layers.map(::domainLayer),
        )
    }

    private fun snapshotAndAutosave() {
        val snapshot = buildSnapshot() ?: return
        project = snapshot
        val localRepository = repository as? LocalProjectRepository
        if (localRepository != null) {
            localRepository.scheduleAutosave(snapshot)
        } else {
            viewModelScope.launch {
                runCatching { repository.saveProject(snapshot) }
                    .onFailure { errorObserver.record(AppError.StorageError(it)) }
            }
        }
    }

    private fun layerModel(layer: Layer): LayerModel {
        return LayerModel(
            id = layer.id,
            type = when (layer.type) {
                com.sanu.carouselforge.data.repository.LayerType.IMAGE -> LayerType.IMAGE
                com.sanu.carouselforge.data.repository.LayerType.STICKER -> LayerType.STICKER
                com.sanu.carouselforge.data.repository.LayerType.TEXT -> LayerType.TEXT
                com.sanu.carouselforge.data.repository.LayerType.SHAPE -> LayerType.SHAPE
            },
            imageUri = layer.imageUri,
            text = layer.text,
            x = layer.x,
            y = layer.y,
            width = layer.width,
            height = layer.height,
            scale = layer.scale,
            rotation = layer.rotation,
            zIndex = layer.zIndex,
            textColor = layer.textColor,
            textSizeSp = layer.textSizeSp,
            fontWeight = layer.fontWeight,
            textAlign = layer.textAlign,
            fontFamily = layer.fontFamily,
            alpha = layer.alpha,
            cornerRadius = layer.cornerRadius,
            hasShadow = layer.hasShadow,
            cropLeft = layer.cropLeft,
            cropTop = layer.cropTop,
            cropRight = layer.cropRight,
            cropBottom = layer.cropBottom,
            brightness = layer.brightness,
            contrast = layer.contrast,
            saturation = layer.saturation,
            filterPreset = layer.filterPreset,
            shapeKind = layer.shapeKind,
            fillColor = layer.fillColor,
        )
    }

    private fun domainLayer(layer: LayerModel): Layer = Layer(
        id = layer.id,
        type = when (layer.type) {
            LayerType.IMAGE -> com.sanu.carouselforge.data.repository.LayerType.IMAGE
            LayerType.STICKER -> com.sanu.carouselforge.data.repository.LayerType.STICKER
            LayerType.TEXT -> com.sanu.carouselforge.data.repository.LayerType.TEXT
            LayerType.SHAPE -> com.sanu.carouselforge.data.repository.LayerType.SHAPE
        },
        imageUri = layer.imageUri,
        text = layer.text,
        x = layer.x,
        y = layer.y,
        width = layer.width,
        height = layer.height,
        scale = layer.scale,
        rotation = layer.rotation,
        zIndex = layer.zIndex,
        textColor = layer.textColor,
        textSizeSp = layer.textSizeSp,
        fontWeight = layer.fontWeight,
        textAlign = layer.textAlign,
        fontFamily = layer.fontFamily,
        alpha = layer.alpha,
        cornerRadius = layer.cornerRadius,
        hasShadow = layer.hasShadow,
        cropLeft = layer.cropLeft,
        cropTop = layer.cropTop,
        cropRight = layer.cropRight,
        cropBottom = layer.cropBottom,
        brightness = layer.brightness,
        contrast = layer.contrast,
        saturation = layer.saturation,
        filterPreset = layer.filterPreset,
        shapeKind = layer.shapeKind,
        fillColor = layer.fillColor,
    )

    private fun slideBoundaryXs(editing: EditorState.Editing): List<Float> =
        (1 until editing.slideCount).map { (it * editing.canvasWidth).toFloat() }

    private fun cascadedTopLeft(
        current: EditorState.Editing,
        layerWidth: Float,
        layerHeight: Float,
    ): Pair<Float, Float> {
        val baseX = (current.canvasWidth - layerWidth) / 2f
        val baseY = (current.canvasHeight - layerHeight) / 2f
        val step = (current.layers.size % CASCADE_WRAP) * CASCADE_OFFSET
        val x = (baseX + step).coerceIn(0f, (current.canvasWidth - layerWidth).coerceAtLeast(0f))
        val y = (baseY + step).coerceIn(0f, (current.canvasHeight - layerHeight).coerceAtLeast(0f))
        return x to y
    }

    private fun defaultTextSizeForLayer(layerHeight: Float, canvasWidth: Float): Float =
        (layerHeight * TEXT_SIZE_TO_LAYER_HEIGHT)
            .coerceIn(canvasWidth * MIN_TEXT_SIZE_FRACTION, canvasWidth * MAX_TEXT_SIZE_FRACTION)

    private fun containedLayerSize(
        aspectRatio: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        coverage: Float,
    ): Pair<Float, Float> {
        val safeAspectRatio = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val maxWidth = canvasWidth * coverage
        val maxHeight = canvasHeight * coverage
        return if (maxWidth / maxHeight > safeAspectRatio) {
            (maxHeight * safeAspectRatio) to maxHeight
        } else {
            maxWidth to (maxWidth / safeAspectRatio)
        }
    }

    private companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 8f
        const val DEFAULT_LAYER_COVERAGE = 0.72f
        const val FIT_LAYER_COVERAGE = 0.9f
        const val STICKER_LAYER_COVERAGE = 0.4f
        const val TEXT_LAYER_WIDTH_FRACTION = 0.8f
        const val TEXT_LAYER_HEIGHT_FRACTION = 0.22f
        const val TEXT_SIZE_TO_LAYER_HEIGHT = 0.38f
        const val MIN_TEXT_SIZE_FRACTION = 0.04f
        const val MAX_TEXT_SIZE_FRACTION = 0.2f
        const val SHAPE_LAYER_FRACTION = 0.4f
        const val DUPLICATE_OFFSET = 24f
        const val CASCADE_OFFSET = 24f
        const val CASCADE_WRAP = 6
        const val MIN_SLIDE_COUNT = 1
        const val MAX_SLIDE_COUNT = 10
        const val MAX_HISTORY = 30
    }
}
