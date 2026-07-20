package com.sanu.carouselforge.features.editor.render

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.sanu.carouselforge.core.text.FontCatalog
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.core.util.LayerColorMatrix
import com.sanu.carouselforge.data.repository.ShapeKind
import com.sanu.carouselforge.data.repository.TextAlignment
import com.sanu.carouselforge.features.editor.overlay.SafeZoneOverlay
import com.sanu.carouselforge.features.editor.overlay.SmartGuidesOverlay
import com.sanu.carouselforge.features.editor.snap.GuideEngine
import kotlin.math.min

@Composable
fun EditorCanvas(
    layers: List<LayerModel>,
    selectedLayerId: String?,
    canvasWidth: Int,
    canvasHeight: Int,
    slideCount: Int,
    bgColorStart: Long,
    bgColorEnd: Long?,
    safeZoneVisible: Boolean,
    onSelectLayer: (String) -> Unit,
    onTransform: (String, TransformDelta, Float) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    guides: GuideEngine.GuideResult? = null,
    onEditText: (String) -> Unit = {},
) {
    val density = LocalDensity.current
    val selectionElevation = with(density) { AppTheme.spacing.xs.toPx() }
    val layerShadowElevation = with(density) { AppTheme.spacing.sm.toPx() }
    val selectionStroke = AppTheme.spacing.selectionStroke
    val cutLineStrokePx = with(density) { AppTheme.spacing.selectionStroke.toPx() }
    val selectionHandlePx = with(density) { AppTheme.spacing.selectionHandle.toPx() }
    val selectionColor = MaterialTheme.colorScheme.primary
    val cutLineColor = MaterialTheme.colorScheme.outline
    val totalWidth = (canvasWidth * slideCount).coerceAtLeast(1)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val displayScale = remember(viewportSize, totalWidth, canvasHeight) {
        if (viewportSize == IntSize.Zero || totalWidth <= 0 || canvasHeight <= 0) {
            1f
        } else {
            min(
                viewportSize.width.toFloat() / totalWidth,
                viewportSize.height.toFloat() / canvasHeight,
            )
        }
    }
    val snapThreshold = with(density) { AppTheme.spacing.snapThreshold.toPx() } / displayScale
    val gridSpacing = with(density) { AppTheme.spacing.md.toPx() } / displayScale
    val background = remember(bgColorStart, bgColorEnd) {
        if (bgColorEnd == null) {
            Brush.verticalGradient(listOf(Color(bgColorStart), Color(bgColorStart)))
        } else {
            Brush.verticalGradient(listOf(Color(bgColorStart), Color(bgColorEnd)))
        }
    }

    Box(
        modifier = modifier
            .testTag(EDITOR_CANVAS_TEST_TAG)
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .clipToBounds()
            .background(background),
    ) {
        layers.sortedBy(LayerModel::zIndex).forEach { layer ->
            key(layer.id) {
                val width = with(density) { (layer.width * displayScale).toDp() }
                val height = with(density) { (layer.height * displayScale).toDp() }
                val selected = layer.id == selectedLayerId
                var dragging by remember(layer.id) { mutableStateOf(false) }
                val cornerRadiusPx = layer.cornerRadius * displayScale
                val layerShape: Shape = remember(cornerRadiusPx) {
                    if (cornerRadiusPx > 0f) RoundedCornerShape(cornerRadiusPx) else RectangleShape
                }
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
                            alpha = layer.alpha
                            shadowElevation = maxOf(
                                if (selected) selectionElevation else 0f,
                                if (layer.hasShadow) layerShadowElevation else 0f,
                            )
                            if (selected) {
                                ambientShadowColor = selectionColor
                                spotShadowColor = selectionColor
                            }
                            clip = true
                            shape = layerShape
                        }
                        .clip(layerShape)
                        .then(
                            if (layer.type == LayerType.IMAGE || layer.type == LayerType.STICKER) {
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (selected) {
                                Modifier
                                    .border(selectionStroke, selectionColor, layerShape)
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
                        .pointerInput(layer.id, layer.type) {
                            detectTapGestures(
                                onTap = { onSelectLayer(layer.id) },
                                onDoubleTap = {
                                    if (layer.type == LayerType.TEXT) onEditText(layer.id)
                                },
                            )
                        }
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
                                    snapThreshold,
                                )
                            },
                            onGestureEnd = { id ->
                                onGestureEnd(id, snapThreshold, gridSpacing)
                                dragging = false
                            },
                        ),
                ) {
                    LayerContent(layer = layer, displayScale = displayScale)
                }
            }
        }
        if (slideCount > 1) {
            CutLineOverlay(
                slideCount = slideCount,
                color = cutLineColor,
                strokeWidth = cutLineStrokePx,
            )
        }
        SafeZoneOverlay(
            visible = safeZoneVisible,
            slideIndex = 0,
            slideCount = slideCount,
        )
        SmartGuidesOverlay(guides = guides, displayScale = displayScale)
    }
}

@Composable
private fun LayerContent(layer: LayerModel, displayScale: Float) {
    when (layer.type) {
        LayerType.IMAGE, LayerType.STICKER -> {
            val colorFilter = remember(
                layer.brightness,
                layer.contrast,
                layer.saturation,
                layer.filterPreset,
            ) {
                if (LayerColorMatrix.isIdentity(
                        layer.brightness,
                        layer.contrast,
                        layer.saturation,
                        layer.filterPreset,
                    )
                ) {
                    null
                } else {
                    ColorFilter.colorMatrix(
                        ColorMatrix(
                            LayerColorMatrix.build(
                                brightness = layer.brightness,
                                contrast = layer.contrast,
                                saturation = layer.saturation,
                                preset = layer.filterPreset,
                            ),
                        ),
                    )
                }
            }
            val cropW = (layer.cropRight - layer.cropLeft).coerceAtLeast(0.001f)
            val cropH = (layer.cropBottom - layer.cropTop).coerceAtLeast(0.001f)
            val cropped = layer.cropLeft != 0f || layer.cropTop != 0f ||
                layer.cropRight != 1f || layer.cropBottom != 1f
            AsyncImage(
                model = layer.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (cropped) {
                            Modifier.graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = 1f / cropW
                                scaleY = 1f / cropH
                                translationX = -layer.cropLeft * size.width / cropW
                                translationY = -layer.cropTop * size.height / cropH
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        LayerType.TEXT -> Text(
            text = layer.text.orEmpty(),
            color = Color(layer.textColor),
            fontSize = with(LocalDensity.current) { (layer.textSizeSp * displayScale).toSp() },
            fontWeight = FontWeight(layer.fontWeight.coerceIn(100, 900)),
            fontFamily = FontCatalog.fontFamily(layer.fontFamily),
            textAlign = when (layer.textAlign) {
                TextAlignment.LEFT -> TextAlign.Start
                TextAlignment.CENTER -> TextAlign.Center
                TextAlignment.RIGHT -> TextAlign.End
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LayerType.SHAPE -> ShapeContent(layer)
    }
}

@Composable
private fun ShapeContent(layer: LayerModel) {
    val color = layer.fillColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        when (layer.shapeKind) {
            ShapeKind.CIRCLE -> drawOval(color = color)
            ShapeKind.LINE -> drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height.coerceAtMost(size.width) * LINE_THICKNESS_FRACTION,
                cap = StrokeCap.Round,
            )
            ShapeKind.ARROW -> {
                val thickness = size.height.coerceAtMost(size.width) * LINE_THICKNESS_FRACTION
                val midY = size.height / 2f
                drawLine(
                    color = color,
                    start = Offset(0f, midY),
                    end = Offset(size.width - thickness, midY),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                )
                val head = thickness * 3f
                drawLine(color, Offset(size.width, midY), Offset(size.width - head, midY - head), thickness, StrokeCap.Round)
                drawLine(color, Offset(size.width, midY), Offset(size.width - head, midY + head), thickness, StrokeCap.Round)
            }
            else -> drawRect(color = color)
        }
    }
}

@Composable
private fun CutLineOverlay(
    slideCount: Int,
    color: Color,
    strokeWidth: Float,
) {
    Canvas(Modifier.fillMaxSize()) {
        val slideWidth = size.width / slideCount
        for (index in 1 until slideCount) {
            val x = slideWidth * index
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
            )
        }
    }
}

private const val LINE_THICKNESS_FRACTION = 0.12f
const val EDITOR_CANVAS_TEST_TAG = "editor_canvas"
