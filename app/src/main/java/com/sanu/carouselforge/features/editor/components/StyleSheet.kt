package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.core.util.LayerColorMatrix
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType

private val SHAPE_COLORS = listOf(
    0xFF000000L, 0xFFFFFFFFL, 0xFFEF4444L, 0xFFF97316L,
    0xFFFBBF24L, 0xFF22C55EL, 0xFF3B82F6L, 0xFF8B5CF6L,
    0xFFEC4899L, 0xFF64748BL,
)

/** Styling controls for the selected layer: opacity, corners, shadow, filters, fill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSheet(
    layer: LayerModel,
    canvasWidth: Int,
    onUpdate: (pushHistory: Boolean, (LayerModel) -> LayerModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    var checkpointed by remember { mutableStateOf(false) }
    val change: ((LayerModel) -> LayerModel) -> Unit = { transform ->
        onUpdate(!checkpointed, transform)
        checkpointed = true
    }
    val isImage = layer.type == LayerType.IMAGE || layer.type == LayerType.STICKER
    val maxRadius = canvasWidth * 0.25f

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text("Style", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            LabeledSlider("Opacity", layer.alpha, 0f..1f) { value ->
                change { it.copy(alpha = value.coerceIn(0.05f, 1f)) }
            }
            LabeledSlider("Corner radius", layer.cornerRadius, 0f..maxRadius) { value ->
                change { it.copy(cornerRadius = value) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Shadow", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = layer.hasShadow,
                    onCheckedChange = { checked -> change { it.copy(hasShadow = checked) } },
                )
            }

            if (isImage) {
                Text("Adjust", style = MaterialTheme.typography.labelLarge)
                LabeledSlider("Brightness", layer.brightness, -0.5f..0.5f) { value ->
                    change { it.copy(brightness = value) }
                }
                LabeledSlider("Contrast", layer.contrast, 0.5f..1.5f) { value ->
                    change { it.copy(contrast = value) }
                }
                LabeledSlider("Saturation", layer.saturation, 0f..2f) { value ->
                    change { it.copy(saturation = value) }
                }
                Text("Filters", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    PresetChip("None", layer.filterPreset == null) {
                        change { it.copy(filterPreset = null) }
                    }
                    LayerColorMatrix.PRESETS.forEach { preset ->
                        PresetChip(preset, layer.filterPreset == preset) {
                            change { current ->
                                current.copy(
                                    filterPreset = if (current.filterPreset == preset) null else preset,
                                )
                            }
                        }
                    }
                }
            }

            if (layer.type == LayerType.SHAPE) {
                Text("Fill", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    SHAPE_COLORS.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(AppTheme.spacing.swatch)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    AppTheme.spacing.selectionStroke,
                                    if (layer.fillColor == color) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    CircleShape,
                                )
                                .clickable { change { it.copy(fillColor = color) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppTheme.spacing.xxs))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
    ) {
        Text(
            label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
