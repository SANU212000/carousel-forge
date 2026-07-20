package com.sanu.carouselforge.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.prefs.UserPreferences
import com.sanu.carouselforge.core.prefs.UserPreferencesRepository
import com.sanu.carouselforge.features.export.ExportFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    val state: StateFlow<UserPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(
            defaultRatio = CanvasPreset.DEFAULT,
            gridEnabledByDefault = true,
            lastExportFormat = ExportFormat.PNG,
        ),
    )

    fun setDefaultRatio(preset: CanvasPreset) {
        viewModelScope.launch { repository.setDefaultRatio(preset) }
    }

    fun setGridEnabledByDefault(enabled: Boolean) {
        viewModelScope.launch { repository.setGridEnabledByDefault(enabled) }
    }

    fun setDefaultExportFormat(format: ExportFormat) {
        viewModelScope.launch { repository.setLastExportFormat(format) }
    }
}
