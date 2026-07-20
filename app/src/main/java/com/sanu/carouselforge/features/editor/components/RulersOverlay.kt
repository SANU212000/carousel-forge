package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import com.sanu.carouselforge.core.theme.AppTheme

/**
 * Thin pixel rulers along the top and left edges of the canvas viewport. Ticks are placed
 * in logical canvas coordinates scaled to the letterboxed canvas, with slide boundaries
 * emphasized so the connected carousel's cuts are readable at a glance.
 */
@Composable
fun RulersOverlay(
    offsetXPx: Float,
    offsetYPx: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    slideCount: Int,
    modifier: Modifier = Modifier,
) {
    val thickness = with(LocalDensity.current) { AppTheme.spacing.rulerThickness.toPx() }
    val strokePx = with(LocalDensity.current) { AppTheme.spacing.selectionStroke.toPx() }
    val stripColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val boundaryColor = MaterialTheme.colorScheme.primary
    val slides = slideCount.coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Strips.
        drawRect(stripColor, topLeft = Offset(0f, 0f), size = Size(size.width, thickness))
        drawRect(stripColor, topLeft = Offset(0f, 0f), size = Size(thickness, size.height))

        val slideWidthPx = canvasWidthPx / slides
        for (i in 0..slides) {
            val x = offsetXPx + i * slideWidthPx
            drawLine(boundaryColor, Offset(x, 0f), Offset(x, thickness), strokePx)
            if (i < slides) {
                val mid = x + slideWidthPx / 2f
                drawLine(tickColor, Offset(mid, thickness * 0.5f), Offset(mid, thickness), strokePx)
            }
        }
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val y = offsetYPx + fraction * canvasHeightPx
            val emphasized = fraction == 0f || fraction == 0.5f || fraction == 1f
            drawLine(
                if (emphasized) boundaryColor else tickColor,
                Offset(0f, y),
                Offset(if (emphasized) thickness else thickness * 0.5f, y),
                strokePx,
            )
        }
    }
}
