package com.sanu.carouselforge.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacing(
    val xxs: androidx.compose.ui.unit.Dp = 4.dp,
    val snapThreshold: androidx.compose.ui.unit.Dp = 6.dp,
    val xs: androidx.compose.ui.unit.Dp = 8.dp,
    val sm: androidx.compose.ui.unit.Dp = 12.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp,
    val xxl: androidx.compose.ui.unit.Dp = 48.dp,
    val huge: androidx.compose.ui.unit.Dp = 64.dp,
)

internal val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
