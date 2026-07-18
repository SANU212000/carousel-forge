package com.sanu.carouselforge.features.editor.render

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.layerTransformGestures(
    layerId: String,
    onGestureStart: (String) -> Unit,
    onTransform: (String, TransformDelta) -> Unit,
    onGestureEnd: (String) -> Unit,
): Modifier = pointerInput(layerId) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onGestureStart(layerId)
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pan = event.calculatePan()
            val zoom = event.calculateZoom()
            val rotation = event.calculateRotation()
            if (pan.x != 0f || pan.y != 0f || zoom != 1f || rotation != 0f) {
                onTransform(
                    layerId,
                    TransformDelta(
                        panX = pan.x,
                        panY = pan.y,
                        zoom = zoom,
                        rotation = rotation,
                    ),
                )
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        onGestureEnd(layerId)
    }
}
