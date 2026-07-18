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
            if (!result.didSnap) current else current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id == id) {
                        layer.copy(
                            x = layer.x + result.translationXPx,
                            y = layer.y + result.translationYPx,
                        )
                    } else {
                        layer
                    }
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

    fun addImage(uri: String) {
        val current = mutableState.value as? EditorState.Editing ?: return
        val size = minOf(current.canvasWidth, current.canvasHeight) * DEFAULT_LAYER_FRACTION
        val layer = LayerModel(
            id = UUID.randomUUID().toString(),
            type = LayerType.IMAGE,
            imageUri = uri,
            x = (current.canvasWidth - size) / 2f,
            y = (current.canvasHeight - size) / 2f,
            width = size,
            height = size,
            zIndex = current.layers.size,
        )
        mutableState.value = current.copy(
            layers = current.layers + layer,
            selectedLayerId = layer.id,
        )
        snapshotAndAutosave()
    }

    fun removeLayer(id: String) {
        mutableState.update { current ->
            if (current !is EditorState.Editing) return@update current
            current.copy(
                layers = current.layers.filterNot { it.id == id },
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
                    layers = loaded.layers.map { layer -> layerModel(layer, loaded) },
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

    override fun onCleared() {
        (repository as? LocalProjectRepository)?.cancelPendingAutosave(projectId)
        super.onCleared()
    }

    private fun layerModel(layer: Layer, project: Project): LayerModel {
        val size = minOf(project.canvasWidth, project.canvasHeight) * DEFAULT_LAYER_FRACTION
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
            width = size,
            height = size,
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
        scale = layer.scale,
        rotation = layer.rotation,
        zIndex = layer.zIndex,
    )

    private fun layerBounds(layer: LayerModel): LayerBounds = LayerBounds(
        id = layer.id,
        left = layer.x,
        top = layer.y,
        right = layer.x + layer.width * layer.scale,
        bottom = layer.y + layer.height * layer.scale,
    )

    private companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 8f
        const val DEFAULT_LAYER_FRACTION = 0.45f
    }
}
