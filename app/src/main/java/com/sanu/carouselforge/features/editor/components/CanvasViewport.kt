package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.EditorState
import com.sanu.carouselforge.features.editor.render.EditorCanvas
import com.sanu.carouselforge.features.editor.render.TransformDelta
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_ZOOM = 0.25f
private const val MAX_ZOOM = 4f
private const val ZOOM_STEP = 0.25f

@Composable
fun CanvasViewport(
    state: EditorState.Editing,
    onSelectLayer: (String) -> Unit,
    onDeselectLayer: () -> Unit = {},
    onTransform: (String, TransformDelta, Float) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    onAddImage: () -> Unit,
    modifier: Modifier = Modifier,
    onEditText: (String) -> Unit = {},
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(AppTheme.spacing.md),
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val availableHeightPx = with(density) { maxHeight.toPx() }
        val slideWidthLogical = state.canvasWidth.toFloat()
        val slideHeightLogical = state.canvasHeight.toFloat()
        val totalWidthLogical = slideWidthLogical * state.slideCount

        val baseScale = if (
            slideWidthLogical > 0f &&
            slideHeightLogical > 0f &&
            availableWidthPx > 0f &&
            availableHeightPx > 0f
        ) {
            min(
                availableHeightPx / slideHeightLogical,
                availableWidthPx / slideWidthLogical,
            )
        } else {
            1f
        }
        val effectiveScale = baseScale * zoom

        val contentWidthDp = with(density) { (totalWidthLogical * effectiveScale).toDp() }
        val contentHeightDp = with(density) { (slideHeightLogical * effectiveScale).toDp() }
        val contentWidthPx = totalWidthLogical * effectiveScale
        val contentHeightPx = slideHeightLogical * effectiveScale

        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(contentWidthDp, contentHeightDp),
                ) {
                    RulersOverlay(
                        offsetXPx = 0f,
                        offsetYPx = 0f,
                        canvasWidthPx = contentWidthPx,
                        canvasHeightPx = contentHeightPx,
                        slideCount = state.slideCount,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                            onDeselectLayer = onDeselectLayer,
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
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                )
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

            ZoomControls(
                zoom = zoom,
                onZoomIn = { zoom = (zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM) },
                onZoomOut = { zoom = (zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM) },
                onFit = { zoom = 1f },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = AppTheme.spacing.xs,
                        bottom = AppTheme.spacing.rulerThickness + AppTheme.spacing.xxs,
                    ),
            )
        }
    }
}

@Composable
private fun ZoomControls(
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppTheme.spacing.xs),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        shadowElevation = AppTheme.spacing.xxs,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.xxs,
                vertical = AppTheme.spacing.xxs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        ) {
            IconButton(
                onClick = onZoomOut,
                enabled = zoom > MIN_ZOOM,
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }
            Text(
                text = "${(zoom * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.xxs),
            )
            IconButton(
                onClick = onZoomIn,
                enabled = zoom < MAX_ZOOM,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            TextButton(onClick = onFit) {
                Text("Fit")
            }
        }
    }
}
