package com.sanu.carouselforge.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.features.export.ExportFormat
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Simple, non-sensitive user settings that persist across process death. */
data class UserPreferences(
    val defaultRatio: CanvasPreset,
    val gridEnabledByDefault: Boolean,
    val lastExportFormat: ExportFormat,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "carousel_settings",
)

class UserPreferencesRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val preferences: Flow<UserPreferences> = store.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            UserPreferences(
                defaultRatio = prefs[Keys.DEFAULT_RATIO]
                    ?.let { runCatching { CanvasPreset.valueOf(it) }.getOrNull() }
                    ?: CanvasPreset.DEFAULT,
                gridEnabledByDefault = prefs[Keys.GRID_DEFAULT] ?: true,
                lastExportFormat = prefs[Keys.LAST_EXPORT_FORMAT]
                    ?.let { runCatching { ExportFormat.valueOf(it) }.getOrNull() }
                    ?: ExportFormat.PNG,
            )
        }

    suspend fun setDefaultRatio(preset: CanvasPreset) {
        store.edit { it[Keys.DEFAULT_RATIO] = preset.name }
    }

    suspend fun setGridEnabledByDefault(enabled: Boolean) {
        store.edit { it[Keys.GRID_DEFAULT] = enabled }
    }

    suspend fun setLastExportFormat(format: ExportFormat) {
        store.edit { it[Keys.LAST_EXPORT_FORMAT] = format.name }
    }

    private object Keys {
        val DEFAULT_RATIO = stringPreferencesKey("default_ratio")
        val GRID_DEFAULT = booleanPreferencesKey("grid_enabled_by_default")
        val LAST_EXPORT_FORMAT = stringPreferencesKey("last_export_format")
    }
}
