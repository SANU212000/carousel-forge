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
    avatarCenterXFraction: Float = 0.12f,
    avatarCenterYFraction: Float = 0.12f,
    avatarRadiusFraction: Float = 0.075f,
) {
    if (!visible || slideIndex != 0) return

    val overlayColor = MaterialTheme.colorScheme.primary
    val strokeWidth = with(LocalDensity.current) { AppTheme.spacing.xxs.toPx() }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            color = overlayColor,
            radius = size.minDimension * avatarRadiusFraction,
            center = Offset(
                x = size.width * avatarCenterXFraction,
                y = size.height * avatarCenterYFraction,
            ),
            style = Stroke(width = strokeWidth),
        )
    }
}
