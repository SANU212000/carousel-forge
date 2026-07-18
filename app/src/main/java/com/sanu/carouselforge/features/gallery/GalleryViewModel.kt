package com.sanu.carouselforge.features.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ProjectRepository
import com.sanu.carouselforge.data.repository.ProjectSummary
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GalleryState {
    data object Loading : GalleryState
    data class Content(val projects: List<ProjectSummary>) : GalleryState
    data class Error(val error: AppError) : GalleryState
}

class GalleryViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<GalleryState>(GalleryState.Loading)
    val state: StateFlow<GalleryState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = GalleryState.Loading
            mutableState.value = try {
                GalleryState.Content(repository.listProjects())
            } catch (error: Exception) {
                GalleryState.Error(AppError.StorageError(error))
            }
        }
    }

    fun createProject(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val project = Project(
                id = id,
                name = "Untitled carousel",
                createdAt = now,
                updatedAt = now,
                canvasWidth = DEFAULT_CANVAS_SIZE,
                canvasHeight = DEFAULT_CANVAS_SIZE,
                layers = emptyList(),
            )
            try {
                repository.saveProject(project)
                onCreated(id)
                refresh()
            } catch (error: Exception) {
                mutableState.value = GalleryState.Error(AppError.StorageError(error))
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(id)
                refresh()
            } catch (error: Exception) {
                mutableState.value = GalleryState.Error(AppError.StorageError(error))
            }
        }
    }

    private companion object {
        const val DEFAULT_CANVAS_SIZE = 1080
    }
}
