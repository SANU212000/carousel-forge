package com.sanu.carouselforge.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class AppMotion(
    val fastMillis: Int = 120,
    val normalMillis: Int = 220,
    val slowMillis: Int = 360,
    val standardEasing: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val snapDampingRatio: Float = Spring.DampingRatioNoBouncy,
    val snapStiffness: Float = Spring.StiffnessMedium,
)

internal val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
