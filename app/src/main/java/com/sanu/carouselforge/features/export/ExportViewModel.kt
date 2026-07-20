package com.sanu.carouselforge.features.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.error.ErrorObserver
import com.sanu.carouselforge.core.error.LogcatErrorObserver
import com.sanu.carouselforge.core.prefs.UserPreferencesRepository
import com.sanu.carouselforge.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExportState {
    data object Ready : ExportState
    data class Exporting(val progress: Float) : ExportState
    data class Complete(
        val uris: List<Uri>,
        val slideCount: Int,
        val width: Int,
        val height: Int,
        val format: ExportFormat,
        val warning: AppError.MemoryError?,
    ) : ExportState
    data class Error(val error: AppError.ExportError) : ExportState
}

class ExportViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val exportEngine: ExportEngine,
    private val userPreferences: UserPreferencesRepository? = null,
    private val errorObserver: ErrorObserver = LogcatErrorObserver,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ExportState>(ExportState.Ready)
    val state: StateFlow<ExportState> = mutableState.asStateFlow()

    /** The last format the user exported, so the chooser opens on their preferred option. */
    val selectedFormat: StateFlow<ExportFormat> =
        (userPreferences?.preferences?.map { it.lastExportFormat }
            ?.stateIn(viewModelScope, SharingStarted.Eagerly, ExportFormat.PNG))
            ?: MutableStateFlow(ExportFormat.PNG).asStateFlow()

    fun selectFormat(format: ExportFormat) {
        viewModelScope.launch { userPreferences?.setLastExportFormat(format) }
    }

    fun export(format: ExportFormat = ExportFormat.PNG) {
        selectFormat(format)
        if (mutableState.value is ExportState.Exporting) return
        viewModelScope.launch {
            mutableState.value = ExportState.Exporting(0.05f)
            try {
                val project = repository.getProject(projectId)
                val result = exportEngine.export(project, format) { progress ->
                    mutableState.update { current ->
                        if (current is ExportState.Exporting) {
                            ExportState.Exporting(progress.coerceIn(0.05f, 1f))
                        } else {
                            current
                        }
                    }
                }
                mutableState.value = ExportState.Complete(
                    uris = result.uris,
                    slideCount = result.slideCount,
                    width = result.width,
                    height = result.height,
                    format = result.format,
                    warning = result.warning,
                )
            } catch (error: Exception) {
                val appError = AppError.ExportError(
                    error.message?.takeIf(String::isNotBlank) ?: "Unable to write the image",
                )
                errorObserver.record(appError)
                mutableState.value = ExportState.Error(appError)
            }
        }
    }
}
