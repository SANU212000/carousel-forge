package com.sanu.carouselforge.features.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.error.ErrorObserver
import com.sanu.carouselforge.core.error.LogcatErrorObserver
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.prefs.UserPreferencesRepository
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ProjectRepository
import com.sanu.carouselforge.data.repository.ProjectSummary
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GalleryState {
    data object Loading : GalleryState
    data class Content(val projects: List<ProjectSummary>) : GalleryState
    data class Error(val error: AppError) : GalleryState
}

class GalleryViewModel(
    private val repository: ProjectRepository,
    private val userPreferences: UserPreferencesRepository,
    private val errorObserver: ErrorObserver = LogcatErrorObserver,
) : ViewModel() {
    private val mutableState = MutableStateFlow<GalleryState>(GalleryState.Loading)
    val state: StateFlow<GalleryState> = mutableState.asStateFlow()

    private fun fail(error: Exception) {
        val appError = AppError.StorageError(error)
        errorObserver.record(appError)
        mutableState.value = GalleryState.Error(appError)
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = GalleryState.Loading
            try {
                mutableState.value = GalleryState.Content(repository.listProjects())
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun createProject(
        preset: CanvasPreset? = null,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val resolved = preset ?: userPreferences.preferences.first().defaultRatio
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val project = Project(
                id = id,
                name = "Untitled carousel",
                createdAt = now,
                updatedAt = now,
                canvasWidth = resolved.width,
                canvasHeight = resolved.height,
                slideCount = 1,
                layers = emptyList(),
            )
            try {
                repository.saveProject(project)
                onCreated(id)
                refresh()
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun createFromTemplate(
        template: TemplateCatalog.Template,
        onCreated: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val project = template.toProject()
            try {
                repository.saveProject(project)
                onCreated(project.id)
                refresh()
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun renameProject(id: String, newName: String) {
        val trimmed = newName.trim().ifBlank { return }
        viewModelScope.launch {
            try {
                val project = repository.getProject(id)
                repository.saveProject(
                    project.copy(name = trimmed, updatedAt = System.currentTimeMillis()),
                )
                refresh()
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun duplicateProject(id: String) {
        viewModelScope.launch {
            try {
                val original = repository.getProject(id)
                val now = System.currentTimeMillis()
                val copy = original.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${original.name} copy",
                    createdAt = now,
                    updatedAt = now,
                    layers = original.layers.map { it.copy(id = UUID.randomUUID().toString()) },
                )
                repository.saveProject(copy)
                refresh()
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteProject(id)
                refresh()
            } catch (error: Exception) {
                fail(error)
            }
        }
    }
}
