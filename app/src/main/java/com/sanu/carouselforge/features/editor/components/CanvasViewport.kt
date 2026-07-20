package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.EditorState
import com.sanu.carouselforge.features.editor.render.EditorCanvas
import com.sanu.carouselforge.features.editor.render.TransformDelta

@Composable
fun CanvasViewport(
    state: EditorState.Editing,
    onSelectLayer: (String) -> Unit,
    onTransform: (String, TransformDelta, Float) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    onAddImage: () -> Unit,
    modifier: Modifier = Modifier,
    onEditText: (String) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(AppTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        val canvasRatio = state.totalWidth.toFloat() / state.canvasHeight
        val availableWidth = minOf(maxWidth, AppTheme.spacing.canvasMaxWidth)
        val availableHeight = maxHeight
        val canvasWidth = if (availableWidth / availableHeight > canvasRatio) {
            availableHeight * canvasRatio
        } else {
            availableWidth
        }
        val canvasHeight = canvasWidth / canvasRatio

        val density = androidx.compose.ui.platform.LocalDensity.current
        val canvasWidthPx = with(density) { canvasWidth.toPx() }
        val canvasHeightPx = with(density) { canvasHeight.toPx() }
        val offsetXPx = with(density) { ((maxWidth - canvasWidth) / 2f).toPx() }
        val offsetYPx = with(density) { ((maxHeight - canvasHeight) / 2f).toPx() }
        RulersOverlay(
            offsetXPx = offsetXPx,
            offsetYPx = offsetYPx,
            canvasWidthPx = canvasWidthPx,
            canvasHeightPx = canvasHeightPx,
            slideCount = state.slideCount,
        )

        Box(
            modifier = Modifier
                .size(canvasWidth, canvasHeight)
                .shadow(
                    elevation = AppTheme.spacing.md,
                    shape = RoundedCornerShape(AppTheme.spacing.xs),
                    spotColor = MaterialTheme.colorScheme.primary,
                )
                .clip(RoundedCornerShape(AppTheme.spacing.xs))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .border(
                    width = AppTheme.spacing.selectionStroke,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(AppTheme.spacing.xs),
                ),
        ) {
            EditorCanvas(
                layers = state.layers,
                selectedLayerId = state.selectedLayerId,
                canvasWidth = state.canvasWidth,
                canvasHeight = state.canvasHeight,
                slideCount = state.slideCount,
                bgColorStart = state.bgColorStart,
                bgColorEnd = state.bgColorEnd,
                safeZoneVisible = state.safeZoneVisible,
                guides = state.activeGuides,
                onSelectLayer = onSelectLayer,
                onTransform = onTransform,
                onGestureEnd = onGestureEnd,
                onEditText = onEditText,
            )
            if (state.layers.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    Text(
                        text = "Your canvas is ready",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Button(onClick = onAddImage) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Text("Add image")
                    }
                }
            }
            Text(
                text = "${state.canvasWidth}×${state.canvasHeight} · ${state.slideCount} slides",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
                    .padding(
                        horizontal = AppTheme.spacing.xs,
                        vertical = AppTheme.spacing.xxs,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
