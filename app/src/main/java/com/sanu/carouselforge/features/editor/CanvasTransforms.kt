package com.sanu.carouselforge.features.editor

import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.snap.LayerBounds
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure geometry for the connected-carousel canvas: axis-aligned layer bounds,
 * clamping into the total canvas, and reflowing layers when the ratio changes.
 * Kept free of Android/Compose runtime state so the math is fully unit-testable.
 */
object CanvasTransforms {
    /** Axis-aligned bounds after the layer's scale and rotation are applied. */
    fun layerBounds(layer: LayerModel): LayerBounds {
        val radians = Math.toRadians(layer.rotation.toDouble())
        val scaledWidth = layer.width * layer.scale
        val scaledHeight = layer.height * layer.scale
        val rotatedWidth = (
            abs(scaledWidth * cos(radians)) + abs(scaledHeight * sin(radians))
            ).toFloat()
        val rotatedHeight = (
            abs(scaledWidth * sin(radians)) + abs(scaledHeight * cos(radians))
            ).toFloat()
        val centerX = layer.x + layer.width / 2f
        val centerY = layer.y + layer.height / 2f
        return LayerBounds(
            id = layer.id,
            left = centerX - rotatedWidth / 2f,
            top = centerY - rotatedHeight / 2f,
            right = centerX + rotatedWidth / 2f,
            bottom = centerY + rotatedHeight / 2f,
        )
    }

    /** Nudges a layer back inside [0, totalWidth] x [0, canvasHeight] when possible. */
    fun clampToCanvas(
        layer: LayerModel,
        totalWidth: Float,
        canvasHeight: Float,
    ): LayerModel {
        val bounds = layerBounds(layer)
        val renderedWidth = bounds.right - bounds.left
        val renderedHeight = bounds.bottom - bounds.top
        val x = if (renderedWidth <= totalWidth) {
            layer.x + when {
                bounds.left < 0f -> -bounds.left
                bounds.right > totalWidth -> totalWidth - bounds.right
                else -> 0f
            }
        } else {
            (totalWidth - layer.width) / 2f
        }
        val y = if (renderedHeight <= canvasHeight) {
            layer.y + when {
                bounds.top < 0f -> -bounds.top
                bounds.bottom > canvasHeight -> canvasHeight - bounds.bottom
                else -> 0f
            }
        } else {
            (canvasHeight - layer.height) / 2f
        }
        return layer.copy(x = x, y = y)
    }

    /**
     * Reflows every layer when the per-slide ratio changes: each layer keeps its
     * fractional center within the total canvas and its own size/scale, then is
     * clamped back inside the reshaped bounds.
     */
    fun reflowForRatio(
        layers: List<LayerModel>,
        oldTotalWidth: Float,
        oldHeight: Float,
        newTotalWidth: Float,
        newHeight: Float,
    ): List<LayerModel> = layers.map { layer ->
        val fractionX = (layer.x + layer.width / 2f) / oldTotalWidth
        val fractionY = (layer.y + layer.height / 2f) / oldHeight
        val reflowed = layer.copy(
            x = fractionX * newTotalWidth - layer.width / 2f,
            y = fractionY * newHeight - layer.height / 2f,
        )
        clampToCanvas(reflowed, newTotalWidth, newHeight)
    }
}
