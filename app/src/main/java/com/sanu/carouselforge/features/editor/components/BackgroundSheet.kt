package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.sanu.carouselforge.core.theme.AppTheme

/** A background choice: a solid color, or a vertical gradient when [end] is set. */
data class BackgroundOption(val start: Long, val end: Long? = null)

private val SOLID_COLORS = listOf(
    0xFFFFFFFFL, 0xFF000000L, 0xFFF5F5F5L, 0xFF1C1C1EL,
    0xFFFDE68AL, 0xFFF9A8D4L, 0xFFA7F3D0L, 0xFFBFDBFEL,
    0xFFFCA5A5L, 0xFFC4B5FDL, 0xFFFBBF24L, 0xFF10B981L,
    0xFF3B82F6L, 0xFFEF4444L, 0xFF8B5CF6L, 0xFF0F172AL,
)

private val GRADIENTS = listOf(
    BackgroundOption(0xFFF97316L, 0xFFDB2777L),
    BackgroundOption(0xFF6366F1L, 0xFF06B6D4L),
    BackgroundOption(0xFF22C55EL, 0xFF15803DL),
    BackgroundOption(0xFF0F172AL, 0xFF334155L),
    BackgroundOption(0xFFFDE047L, 0xFFF97316L),
    BackgroundOption(0xFFEC4899L, 0xFF8B5CF6L),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSheet(
    onSelect: (BackgroundOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text(
                "Background",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text("Solid", style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                modifier = Modifier.fillMaxWidth().padding(bottom = AppTheme.spacing.xs),
            ) {
                items(SOLID_COLORS) { color ->
                    Swatch(
                        brush = Brush.verticalGradient(listOf(Color(color), Color(color))),
                        onClick = { onSelect(BackgroundOption(color)); onDismiss() },
                    )
                }
            }
            Text("Gradients", style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(GRADIENTS) { option ->
                    Swatch(
                        brush = Brush.verticalGradient(
                            listOf(Color(option.start), Color(option.end ?: option.start)),
                        ),
                        onClick = { onSelect(option); onDismiss() },
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(brush: Brush, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(AppTheme.spacing.swatch)
            .clip(RoundedCornerShape(AppTheme.spacing.xxs))
            .background(brush)
            .border(
                AppTheme.spacing.selectionStroke,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(AppTheme.spacing.xxs),
            )
            .clickable(onClick = onClick),
    )
}
