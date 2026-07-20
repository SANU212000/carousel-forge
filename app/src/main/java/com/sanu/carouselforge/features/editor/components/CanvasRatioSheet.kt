package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.theme.AppTheme

/**
 * Bottom sheet that lets the user pick a canvas ratio. The chosen ratio applies to
 * every slide at once. The currently active preset is highlighted with the accent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasRatioSheet(
    currentWidth: Int,
    currentHeight: Int,
    onSelect: (CanvasPreset) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val activePreset = CanvasPreset.matching(currentWidth, currentHeight)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(
                text = "Canvas ratio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Applies to every slide in the carousel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                CanvasPreset.entries.forEach { preset ->
                    RatioChip(
                        preset = preset,
                        selected = preset == activePreset,
                        onClick = {
                            onSelect(preset)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RatioChip(
    preset: CanvasPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppTheme.spacing.xs))
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
    ) {
        // A small outline shaped like the ratio.
        val previewWidth = AppTheme.spacing.xl
        val ratio = preset.aspectRatio
        Box(
            modifier = Modifier
                .size(
                    width = if (ratio >= 1f) previewWidth else previewWidth * ratio,
                    height = if (ratio >= 1f) previewWidth / ratio else previewWidth,
                )
                .background(
                    if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(AppTheme.spacing.xxs),
                )
                .border(
                    AppTheme.spacing.selectionStroke,
                    if (selected) accent else outline,
                    RoundedCornerShape(AppTheme.spacing.xxs),
                ),
        )
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
