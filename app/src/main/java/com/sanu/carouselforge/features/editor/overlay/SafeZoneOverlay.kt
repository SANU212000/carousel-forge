package com.sanu.carouselforge.features.editor.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun SafeZoneOverlay(
    visible: Boolean,
    slideIndex: Int,
    modifier: Modifier = Modifier,
    slideCount: Int = 1,
    avatarCenterXFraction: Float = 0.12f,
    avatarCenterYFraction: Float = 0.12f,
    avatarRadiusFraction: Float = 0.075f,
) {
    if (!visible || slideIndex != 0) return

    val overlayColor = MaterialTheme.colorScheme.primary
    val strokeWidth = with(LocalDensity.current) { AppTheme.spacing.xxs.toPx() }
    Canvas(modifier = modifier.fillMaxSize()) {
        // The overlay spans the full connected strip, but the profile-avatar zone
        // only applies to the first slide, so measure against a single slide width.
        val slideWidth = size.width / slideCount.coerceAtLeast(1)
        drawCircle(
            color = overlayColor,
            radius = slideWidth.coerceAtMost(size.height) * avatarRadiusFraction,
            center = Offset(
                x = slideWidth * avatarCenterXFraction,
                y = size.height * avatarCenterYFraction,
            ),
            style = Stroke(width = strokeWidth),
        )
    }
}
