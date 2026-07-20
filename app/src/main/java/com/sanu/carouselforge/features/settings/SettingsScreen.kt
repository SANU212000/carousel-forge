package com.sanu.carouselforge.features.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.export.ExportFormat

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        SettingsSection(
            title = "Default canvas ratio",
            subtitle = "Used when you start a new carousel.",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                CanvasPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = prefs.defaultRatio == preset,
                        onClick = { viewModel.setDefaultRatio(preset) },
                        label = { Text(preset.label) },
                    )
                }
            }
        }

        SettingsSection(
            title = "Grid snapping",
            subtitle = "Turn snapping on automatically in the editor.",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (prefs.gridEnabledByDefault) "Enabled by default" else "Disabled by default",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = prefs.gridEnabledByDefault,
                    onCheckedChange = viewModel::setGridEnabledByDefault,
                )
            }
        }

        SettingsSection(
            title = "Default export format",
            subtitle = "Pre-selected in the export screen.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = prefs.lastExportFormat == format,
                        onClick = { viewModel.setDefaultExportFormat(format) },
                        label = { Text(format.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
