package com.sanu.carouselforge.features.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.error.ErrorObserver
import com.sanu.carouselforge.core.error.LogcatErrorObserver
import com.sanu.carouselforge.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExportState {
    data object Ready : ExportState
    data class Exporting(val progress: Float) : ExportState
    data class Complete(
        val uri: Uri,
        val width: Int,
        val height: Int,
        val warning: AppError.MemoryError?,
    ) : ExportState
    data class Error(val error: AppError.ExportError) : ExportState
}

class ExportViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val exportEngine: ExportEngine,
    private val errorObserver: ErrorObserver = LogcatErrorObserver,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ExportState>(ExportState.Ready)
    val state: StateFlow<ExportState> = mutableState.asStateFlow()

    fun export() {
        if (mutableState.value is ExportState.Exporting) return
        viewModelScope.launch {
            mutableState.value = ExportState.Exporting(0.1f)
            try {
                val project = repository.getProject(projectId)
                mutableState.value = ExportState.Exporting(0.35f)
                val result = exportEngine.export(project)
                mutableState.value = ExportState.Complete(
                    uri = result.uri,
                    width = result.width,
                    height = result.height,
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
