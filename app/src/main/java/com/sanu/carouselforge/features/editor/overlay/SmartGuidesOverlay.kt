package com.sanu.carouselforge.features.editor.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.snap.GuideEngine
import kotlin.math.roundToInt

/**
 * Draws the live alignment guide lines and gap badges produced by [GuideEngine] while a
 * layer is dragged. Positions are in logical canvas pixels, scaled to the viewport by
 * [displayScale]. Composes only while a drag is active (guides is non-null).
 */
@Composable
fun SmartGuidesOverlay(
    guides: GuideEngine.GuideResult?,
    displayScale: Float,
    modifier: Modifier = Modifier,
) {
    if (guides == null) return
    val accent = MaterialTheme.colorScheme.primary
    val strokePx = with(LocalDensity.current) { AppTheme.spacing.selectionStroke.toPx() }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val onAccent = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier.fillMaxSize()) {
        guides.verticalLinesPx.forEach { x ->
            val px = x * displayScale
            drawLine(accent, Offset(px, 0f), Offset(px, size.height), strokePx)
        }
        guides.horizontalLinesPx.forEach { y ->
            val py = y * displayScale
            drawLine(accent, Offset(0f, py), Offset(size.width, py), strokePx)
        }
        guides.badges.forEach { badge ->
            val text = "${(badge.distancePx).roundToInt()}"
            val layout = measurer.measure(text, labelStyle)
            val cx = badge.xPx * displayScale
            val cy = badge.yPx * displayScale
            val padding = strokePx * 2f
            val w = layout.size.width + padding * 2f
            val h = layout.size.height + padding
            drawRoundRect(
                color = accent,
                topLeft = Offset(cx - w / 2f, cy - h / 2f),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(padding, padding),
            )
            drawText(
                textMeasurer = measurer,
                text = text,
                style = labelStyle.copy(color = onAccent),
                topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f),
            )
        }
    }
}
