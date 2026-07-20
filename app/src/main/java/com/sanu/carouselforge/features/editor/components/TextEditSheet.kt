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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sanu.carouselforge.core.text.FontCatalog
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.data.repository.TextAlignment
import com.sanu.carouselforge.features.editor.render.LayerModel

private val TEXT_COLORS = listOf(
    0xFF000000L, 0xFFFFFFFFL, 0xFFEF4444L, 0xFFF97316L,
    0xFFFBBF24L, 0xFF22C55EL, 0xFF3B82F6L, 0xFF8B5CF6L,
    0xFFEC4899L, 0xFF64748BL,
)

/**
 * Editing surface for a TEXT layer. The first change of the session pushes an undo
 * checkpoint; subsequent live changes reuse it so the whole edit collapses to one step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditSheet(
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
    val minSize = canvasWidth * 0.02f
    val maxSize = canvasWidth * 0.28f

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text("Text", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            OutlinedTextField(
                value = layer.text.orEmpty(),
                onValueChange = { value -> change { it.copy(text = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Content") },
            )

            Text("Size", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = layer.textSizeSp.coerceIn(minSize, maxSize),
                onValueChange = { value -> change { it.copy(textSizeSp = value) } },
                valueRange = minSize..maxSize,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bold = layer.fontWeight >= 700
                ToggleChip(
                    selected = bold,
                    onClick = { change { it.copy(fontWeight = if (bold) 400 else 700) } },
                ) { Icon(Icons.Default.FormatBold, contentDescription = "Bold") }

                ToggleChip(
                    selected = layer.textAlign == TextAlignment.LEFT,
                    onClick = { change { it.copy(textAlign = TextAlignment.LEFT) } },
                ) { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align left") }
                ToggleChip(
                    selected = layer.textAlign == TextAlignment.CENTER,
                    onClick = { change { it.copy(textAlign = TextAlignment.CENTER) } },
                ) { Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align center") }
                ToggleChip(
                    selected = layer.textAlign == TextAlignment.RIGHT,
                    onClick = { change { it.copy(textAlign = TextAlignment.RIGHT) } },
                ) { Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = "Align right") }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                TEXT_COLORS.forEach { color ->
                    ColorDot(
                        color = color,
                        selected = layer.textColor == color,
                        onClick = { change { it.copy(textColor = color) } },
                    )
                }
            }

            Text("Font", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                FontChip(
                    label = "Default",
                    fontName = null,
                    selected = layer.fontFamily == null,
                    onClick = { change { it.copy(fontFamily = null) } },
                )
                FontCatalog.fonts.forEach { name ->
                    FontChip(
                        label = name,
                        fontName = name,
                        selected = layer.fontFamily == name,
                        onClick = { change { it.copy(fontFamily = name) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(AppTheme.spacing.xxl)
            .clip(RoundedCornerShape(AppTheme.spacing.xxs))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ColorDot(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AppTheme.spacing.swatch)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                AppTheme.spacing.selectionStroke,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

@Composable
private fun FontChip(
    label: String,
    fontName: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
            text = label,
            fontFamily = FontCatalog.fontFamily(fontName),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
