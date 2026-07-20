package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.render.LayerModel

private const val MIN_CROP = 0.12f

/**
 * Crop editor for an image layer. Drag any of the four corners to move the crop frame;
 * the fractional rect is applied live and rendered identically in the editor and export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropSheet(
    layer: LayerModel,
    onUpdate: (pushHistory: Boolean, (LayerModel) -> LayerModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    var checkpointed by remember { mutableStateOf(false) }
    var rect by remember {
        mutableStateOf(Rect(layer.cropLeft, layer.cropTop, layer.cropRight, layer.cropBottom))
    }
    val apply: (Rect) -> Unit = { next ->
        rect = next
        onUpdate(!checkpointed) {
            it.copy(cropLeft = next.left, cropTop = next.top, cropRight = next.right, cropBottom = next.bottom)
        }
        checkpointed = true
    }
    val density = LocalDensity.current
    val handleDp = AppTheme.spacing.lg
    val handlePx = with(density) { handleDp.toPx() }
    val aspect = (layer.width / layer.height).coerceIn(0.2f, 5f)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text("Crop", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val previewWidthDp = maxWidth
                val previewHeightDp = previewWidthDp / aspect
                val wPx = with(density) { previewWidthDp.toPx() }
                val hPx = with(density) { previewHeightDp.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeightDp)
                        .clipToBounds()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = layer.imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                val left = rect.left * size.width
                                val top = rect.top * size.height
                                val right = rect.right * size.width
                                val bottom = rect.bottom * size.height
                                val dim = Color.Black.copy(alpha = 0.45f)
                                drawRect(dim, size = Size(size.width, top))
                                drawRect(dim, Offset(0f, bottom), Size(size.width, size.height - bottom))
                                drawRect(dim, Offset(0f, top), Size(left, bottom - top))
                                drawRect(dim, Offset(right, top), Size(size.width - right, bottom - top))
                                drawRect(
                                    color = accent,
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top),
                                    style = Stroke(width = handlePx / 6f),
                                )
                            },
                    )
                    listOf(
                        Corner.TOP_LEFT to Offset(rect.left, rect.top),
                        Corner.TOP_RIGHT to Offset(rect.right, rect.top),
                        Corner.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
                        Corner.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
                    ).forEach { (corner, fraction) ->
                        val xDp = with(density) { (fraction.x * wPx - handlePx / 2f).toDp() }
                        val yDp = with(density) { (fraction.y * hPx - handlePx / 2f).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(xDp, yDp)
                                .size(handleDp)
                                .background(Color.White, CircleShape)
                                .pointerInput(corner) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        apply(adjust(rect, corner, dragAmount, wPx, hPx))
                                    }
                                },
                        )
                    }
                }
            }
            TextButton(onClick = { apply(Rect(0f, 0f, 1f, 1f)) }) { Text("Reset crop") }
        }
    }
}

private enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private fun adjust(rect: Rect, corner: Corner, delta: Offset, wPx: Float, hPx: Float): Rect {
    val dx = delta.x / wPx
    val dy = delta.y / hPx
    var l = rect.left
    var t = rect.top
    var r = rect.right
    var b = rect.bottom
    when (corner) {
        Corner.TOP_LEFT -> {
            l = (l + dx).coerceIn(0f, r - MIN_CROP)
            t = (t + dy).coerceIn(0f, b - MIN_CROP)
        }
        Corner.TOP_RIGHT -> {
            r = (r + dx).coerceIn(l + MIN_CROP, 1f)
            t = (t + dy).coerceIn(0f, b - MIN_CROP)
        }
        Corner.BOTTOM_LEFT -> {
            l = (l + dx).coerceIn(0f, r - MIN_CROP)
            b = (b + dy).coerceIn(t + MIN_CROP, 1f)
        }
        Corner.BOTTOM_RIGHT -> {
            r = (r + dx).coerceIn(l + MIN_CROP, 1f)
            b = (b + dy).coerceIn(t + MIN_CROP, 1f)
        }
    }
    return Rect(l, t, r, b)
}
