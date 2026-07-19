package com.sanu.carouselforge.features.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.error.ErrorObserver
import com.sanu.carouselforge.core.error.LogcatErrorObserver
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LocalProjectRepository
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ProjectRepository
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType
import com.sanu.carouselforge.features.editor.render.TransformDelta
import com.sanu.carouselforge.features.editor.snap.LayerBounds
import com.sanu.carouselforge.features.editor.snap.SnapEngine
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        val safeZoneVisible: Boolean = false,
        val splitGuidesVisible: Boolean = false,
        val splitCount: Int = 2,
        val notice: AppError? = null,
    ) : EditorState
    data class Exporting(val progress: Float) : EditorState
    data class Error(val error: AppError) : EditorState
}

class EditorViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val errorObserver: ErrorObserver = LogcatErrorObserver,
) : ViewModel() {
    private val mutableState = MutableStateFlow<EditorState>(EditorState.Loading)
    val state: StateFlow<EditorState> = mutableState.asStateFlow()
    private var project: Project? = null

    init {
        loadProject()
    }

    fun selectLayer(id: String) {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(selectedLayerId = id) else current
        }
    }

    fun transformLayer(id: String, delta: TransformDelta) {
        mutableState.update { current ->
            if (current !is EditorState.Editing) return@update current
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id != id) layer else layer.copy(
                        x = layer.x + delta.panX,
                        y = layer.y + delta.panY,
                        scale = (layer.scale * delta.zoom).coerceIn(MIN_SCALE, MAX_SCALE),
                        rotation = layer.rotation + delta.rotation,
                    )
                },
            )
        }
    }

    fun finishGesture(id: String, thresholdPx: Float, gridSpacingPx: Float) {
        mutableState.update { current ->
            if (current !is EditorState.Editing) return@update current
            val moving = current.layers.firstOrNull { it.id == id } ?: return@update current
            val result = SnapEngine.resolve(
                moving = layerBounds(moving),
                siblings = current.layers.filterNot { it.id == id }.map(::layerBounds),
                thresholdPx = thresholdPx,
                gridSpacingPx = gridSpacingPx.takeIf { current.gridSnapEnabled },
            )
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id != id) return@map layer
                    clampToCanvas(
                        layer = layer.copy(
                            x = layer.x + result.translationXPx,
                            y = layer.y + result.translationYPx,
                        ),
                        canvasWidth = current.canvasWidth.toFloat(),
                        canvasHeight = current.canvasHeight.toFloat(),
                    )
                },
            )
        }
        snapshotAndAutosave()
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

    fun setSplitGuidesVisible(visible: Boolean) {
        mutableState.update { current ->
            if (current is EditorState.Editing) current.copy(splitGuidesVisible = visible) else current
        }
    }

    fun setSplitCount(count: Int) {
        mutableState.update { current ->
            if (current is EditorState.Editing) {
                current.copy(splitCount = count.coerceIn(MIN_SPLIT_COUNT, MAX_SPLIT_COUNT))
            } else {
                current
            }
        }
    }

    fun addImage(uri: String, aspectRatio: Float = 1f) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val (width, height) = containedLayerSize(
            aspectRatio = aspectRatio,
            canvasWidth = current.canvasWidth.toFloat(),
            canvasHeight = current.canvasHeight.toFloat(),
            coverage = DEFAULT_LAYER_COVERAGE,
        )
        val layer = LayerModel(
            id = UUID.randomUUID().toString(),
            type = LayerType.IMAGE,
            imageUri = uri,
            x = (current.canvasWidth - width) / 2f,
            y = (current.canvasHeight - height) / 2f,
            width = width,
            height = height,
            zIndex = current.layers.size,
        )
        mutableState.value = current.copy(
            layers = current.layers + layer,
            selectedLayerId = layer.id,
        )
        snapshotAndAutosave()
    }

    fun replaceSelectedImage(uri: String, aspectRatio: Float = 1f) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selectedId = current.selectedLayerId ?: return
        mutableState.value = current.copy(
            layers = current.layers.map { layer ->
                if (layer.id == selectedId && layer.type == LayerType.IMAGE) {
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
        snapshotAndAutosave()
    }

    fun fitSelectedLayer() {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selectedId = current.selectedLayerId ?: return
        mutableState.value = current.copy(
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
        snapshotAndAutosave()
    }

    fun moveSelectedLayer(forward: Boolean) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val selectedId = current.selectedLayerId ?: return
        val ordered = current.layers.sortedBy(LayerModel::zIndex).toMutableList()
        val index = ordered.indexOfFirst { it.id == selectedId }
        if (index < 0) return
        val target = (index + if (forward) 1 else -1).coerceIn(ordered.indices)
        if (target == index) return
        val moved = ordered.removeAt(index)
        ordered.add(target, moved)
        mutableState.value = current.copy(
            layers = ordered.mapIndexed { zIndex, layer -> layer.copy(zIndex = zIndex) },
        )
        snapshotAndAutosave()
    }

    fun removeLayer(id: String) {
        mutableState.update { current ->
            if (current !is EditorState.Editing) return@update current
            current.copy(
                layers = current.layers
                    .filterNot { it.id == id }
                    .mapIndexed { zIndex, layer -> layer.copy(zIndex = zIndex) },
                selectedLayerId = current.selectedLayerId.takeUnless { it == id },
            )
        }
        snapshotAndAutosave()
    }

    private fun loadProject() {
        viewModelScope.launch {
            try {
                val loaded = repository.getProject(projectId)
                project = loaded
                mutableState.value = EditorState.Editing(
                    layers = loaded.layers.map(::layerModel),
                    selectedLayerId = null,
                    gridSnapEnabled = true,
                    canvasWidth = loaded.canvasWidth,
                    canvasHeight = loaded.canvasHeight,
                )
            } catch (error: Exception) {
                val appError = AppError.StorageError(error)
                errorObserver.record(appError)
                mutableState.value = EditorState.Error(appError)
            }
        }
    }

    private fun snapshotAndAutosave() {
        val editing = mutableState.value as? EditorState.Editing ?: return
        val previous = project ?: return
        val snapshot = previous.copy(
            updatedAt = System.currentTimeMillis(),
            layers = editing.layers.map(::domainLayer),
        )
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
                com.sanu.carouselforge.data.repository.LayerType.IMAGE,
                com.sanu.carouselforge.data.repository.LayerType.STICKER -> LayerType.IMAGE
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
        )
    }

    private fun domainLayer(layer: LayerModel): Layer = Layer(
        id = layer.id,
        type = when (layer.type) {
            LayerType.IMAGE -> com.sanu.carouselforge.data.repository.LayerType.IMAGE
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
    )

    private fun layerBounds(layer: LayerModel): LayerBounds {
        val radians = Math.toRadians(layer.rotation.toDouble())
        val scaledWidth = layer.width * layer.scale
        val scaledHeight = layer.height * layer.scale
        val rotatedWidth = (
            abs(scaledWidth * cos(radians)) + abs(scaledHeight * sin(radians))
            ).toFloat()
        val rotatedHeight = (
            abs(scaledWidth * sin(radians)) + abs(scaledHeight * cos(radians))
            ).toFloat()
        val centerX = layer.x + layer.width / 2f
        val centerY = layer.y + layer.height / 2f
        return LayerBounds(
            id = layer.id,
            left = centerX - rotatedWidth / 2f,
            top = centerY - rotatedHeight / 2f,
            right = centerX + rotatedWidth / 2f,
            bottom = centerY + rotatedHeight / 2f,
        )
    }

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

    private fun clampToCanvas(
        layer: LayerModel,
        canvasWidth: Float,
        canvasHeight: Float,
    ): LayerModel {
        val bounds = layerBounds(layer)
        val renderedWidth = bounds.right - bounds.left
        val renderedHeight = bounds.bottom - bounds.top
        val x = if (renderedWidth <= canvasWidth) {
            layer.x + when {
                bounds.left < 0f -> -bounds.left
                bounds.right > canvasWidth -> canvasWidth - bounds.right
                else -> 0f
            }
        } else {
            (canvasWidth - layer.width) / 2f
        }
        val y = if (renderedHeight <= canvasHeight) {
            layer.y + when {
                bounds.top < 0f -> -bounds.top
                bounds.bottom > canvasHeight -> canvasHeight - bounds.bottom
                else -> 0f
            }
        } else {
            (canvasHeight - layer.height) / 2f
        }
        return layer.copy(x = x, y = y)
    }

    private companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 8f
        const val DEFAULT_LAYER_COVERAGE = 0.72f
        const val FIT_LAYER_COVERAGE = 0.9f
        const val MIN_SPLIT_COUNT = 2
        const val MAX_SPLIT_COUNT = 9
    }
}
