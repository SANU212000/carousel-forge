package com.sanu.carouselforge.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val AppColorScheme = darkColorScheme(
    primary = AccentActive,
    onPrimary = ChromeBackground,
    primaryContainer = SurfaceSelected,
    onPrimaryContainer = ContentPrimary,
    background = ChromeBackground,
    onBackground = ContentPrimary,
    surface = SurfaceCanvas,
    surfaceBright = CanvasBackground,
    onSurface = ContentPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = ContentSecondary,
    outline = OutlineSubtle,
    error = ErrorContent,
    errorContainer = ErrorContainer,
)

object AppTheme {
    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val motion: AppMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMotion.current
}

@Composable
fun CarouselForgeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalAppMotion provides AppMotion(),
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
