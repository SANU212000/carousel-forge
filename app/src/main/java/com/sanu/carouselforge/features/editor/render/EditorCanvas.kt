package com.sanu.carouselforge.features.editor.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.overlay.SafeZoneOverlay
import com.sanu.carouselforge.features.editor.slice.SliceEngine
import kotlin.math.min

@Composable
fun EditorCanvas(
    layers: List<LayerModel>,
    selectedLayerId: String?,
    canvasWidth: Int,
    canvasHeight: Int,
    safeZoneVisible: Boolean,
    splitGuidesVisible: Boolean,
    splitCount: Int,
    onSelectLayer: (String) -> Unit,
    onTransform: (String, TransformDelta) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val selectionElevation = with(density) { AppTheme.spacing.xs.toPx() }
    val selectionStroke = AppTheme.spacing.selectionStroke
    val selectionStrokePx = with(density) { selectionStroke.toPx() }
    val selectionHandlePx = with(density) { AppTheme.spacing.selectionHandle.toPx() }
    val selectionColor = MaterialTheme.colorScheme.primary
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val displayScale = remember(viewportSize, canvasWidth, canvasHeight) {
        if (viewportSize == IntSize.Zero || canvasWidth <= 0 || canvasHeight <= 0) {
            1f
        } else {
            min(
                viewportSize.width.toFloat() / canvasWidth,
                viewportSize.height.toFloat() / canvasHeight,
            )
        }
    }
    val snapThreshold = with(density) { AppTheme.spacing.snapThreshold.toPx() } / displayScale
    val gridSpacing = with(density) { AppTheme.spacing.md.toPx() } / displayScale

    Box(
        modifier = modifier
            .testTag(EDITOR_CANVAS_TEST_TAG)
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .clipToBounds()
            .background(MaterialTheme.colorScheme.surfaceBright),
    ) {
        layers.sortedBy(LayerModel::zIndex).forEach { layer ->
            key(layer.id) {
                val width = with(density) { (layer.width * displayScale).toDp() }
                val height = with(density) { (layer.height * displayScale).toDp() }
                val selected = layer.id == selectedLayerId
                var dragging by remember(layer.id) { mutableStateOf(false) }
                val settledX by animateFloatAsState(
                    targetValue = layer.x,
                    animationSpec = spring(
                        dampingRatio = AppTheme.motion.snapDampingRatio,
                        stiffness = AppTheme.motion.snapStiffness,
                    ),
                    label = "layerX",
                )
                val settledY by animateFloatAsState(
                    targetValue = layer.y,
                    animationSpec = spring(
                        dampingRatio = AppTheme.motion.snapDampingRatio,
                        stiffness = AppTheme.motion.snapStiffness,
                    ),
                    label = "layerY",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width, height)
                        .zIndex(layer.zIndex.toFloat())
                        .graphicsLayer {
                            translationX = (if (dragging) layer.x else settledX) * displayScale
                            translationY = (if (dragging) layer.y else settledY) * displayScale
                            scaleX = layer.scale
                            scaleY = layer.scale
                            rotationZ = layer.rotation
                            shadowElevation = if (selected) selectionElevation else 0f
                            ambientShadowColor = selectionColor
                            spotShadowColor = selectionColor
                            clip = true
                            shape = RectangleShape
                        }
                        .clip(RectangleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(
                            if (selected) {
                                Modifier
                                    .border(selectionStroke, selectionColor, RectangleShape)
                                    .drawWithContent {
                                        drawContent()
                                        val radius = selectionHandlePx / 2f
                                        listOf(
                                            Offset.Zero,
                                            Offset(size.width, 0f),
                                            Offset(0f, size.height),
                                            Offset(size.width, size.height),
                                        ).forEach { center ->
                                            drawCircle(
                                                color = selectionColor,
                                                radius = radius,
                                                center = center,
                                            )
                                        }
                                    }
                            } else {
                                Modifier
                            },
                        )
                        .layerTransformGestures(
                            layerId = layer.id,
                            onGestureStart = { id ->
                                dragging = true
                                onSelectLayer(id)
                            },
                            onTransform = { id, delta ->
                                onTransform(
                                    id,
                                    delta.copy(
                                        panX = delta.panX / displayScale,
                                        panY = delta.panY / displayScale,
                                    ),
                                )
                            },
                            onGestureEnd = { id ->
                                onGestureEnd(id, snapThreshold, gridSpacing)
                                dragging = false
                            },
                        ),
                ) {
                    when (layer.type) {
                        LayerType.IMAGE -> AsyncImage(
                            model = layer.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                        LayerType.TEXT -> Text(
                            text = layer.text.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        )

                        LayerType.SHAPE -> Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        if (splitGuidesVisible && splitCount > 1) {
            SplitGuideOverlay(
                splitCount = splitCount,
                logicalCanvasWidth = canvasWidth,
                logicalCanvasHeight = canvasHeight,
                color = selectionColor,
                strokeWidth = selectionStrokePx,
            )
        }
        SafeZoneOverlay(
            visible = safeZoneVisible,
            slideIndex = 0,
        )
    }
}

@Composable
private fun SplitGuideOverlay(
    splitCount: Int,
    logicalCanvasWidth: Int,
    logicalCanvasHeight: Int,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
) {
    val guideFractions = remember(splitCount, logicalCanvasWidth, logicalCanvasHeight) {
        val exactSlices = runCatching {
            SliceEngine.calculateSlices(
                sourceWidth = logicalCanvasWidth,
                sourceHeight = logicalCanvasHeight,
                sliceWidth = logicalCanvasWidth / splitCount,
                sliceHeight = logicalCanvasHeight,
            )
        }.getOrNull()
        if (exactSlices?.size == splitCount) {
            exactSlices.dropLast(1).map {
                it.sourceRect.right.toFloat() / logicalCanvasWidth
            }
        } else {
            List(splitCount - 1) { index -> (index + 1f) / splitCount }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        guideFractions.forEach { fraction ->
            val x = size.width * fraction
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
            )
        }
    }
}

const val EDITOR_CANVAS_TEST_TAG = "editor_canvas"
