package com.sanu.carouselforge.features.editor.render

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun EditorCanvas(
    layers: List<LayerModel>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onTransform: (String, TransformDelta) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val selectionElevation = with(density) { AppTheme.spacing.xs.toPx() }
    val snapThreshold = with(density) { AppTheme.spacing.snapThreshold.toPx() }
    val gridSpacing = with(density) { AppTheme.spacing.md.toPx() }
    val selectionColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
    ) {
        layers.sortedBy(LayerModel::zIndex).forEach { layer ->
            key(layer.id) {
                val width = with(density) { layer.width.toDp() }
                val height = with(density) { layer.height.toDp() }
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
                            translationX = if (dragging) layer.x else settledX
                            translationY = if (dragging) layer.y else settledY
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
                        .layerTransformGestures(
                            layerId = layer.id,
                            onGestureStart = { id ->
                                dragging = true
                                onSelectLayer(id)
                            },
                            onTransform = onTransform,
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
    }
}
