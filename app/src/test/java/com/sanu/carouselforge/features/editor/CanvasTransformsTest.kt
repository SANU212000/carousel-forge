package com.sanu.carouselforge.features.editor

import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType
import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasTransformsTest {
    private fun layer(
        id: String = "l",
        x: Float,
        y: Float,
        width: Float = 100f,
        height: Float = 100f,
    ) = LayerModel(id = id, type = LayerType.IMAGE, imageUri = "uri", x = x, y = y, width = width, height = height)

    @Test
    fun reflowForRatio_preservesFractionalCenterAndSize() {
        val original = layer(x = 450f, y = 450f) // center (500, 500) on a 1000x1000 canvas

        val reflowed = CanvasTransforms.reflowForRatio(
            layers = listOf(original),
            oldTotalWidth = 1000f,
            oldHeight = 1000f,
            newTotalWidth = 2000f,
            newHeight = 1000f,
        ).single()

        // Center fraction 0.5 must be preserved: new center (1000, 500).
        assertEquals(950f, reflowed.x, 0.01f)
        assertEquals(450f, reflowed.y, 0.01f)
        // Size is never changed by a reflow.
        assertEquals(original.width, reflowed.width, 0f)
        assertEquals(original.height, reflowed.height, 0f)
    }

    @Test
    fun reflowForRatio_clampsLayersInsideNewBounds() {
        // Layer at the far right of a 3-slide (3000 wide) strip.
        val original = layer(x = 2850f, y = 450f) // right edge 2950

        val reflowed = CanvasTransforms.reflowForRatio(
            layers = listOf(original),
            oldTotalWidth = 3000f,
            oldHeight = 1000f,
            newTotalWidth = 3000f,
            newHeight = 800f, // shorter canvas
        ).single()

        // Vertical center fraction 0.5 -> 400; but 100 tall layer must stay inside 0..800.
        assertEquals(350f, reflowed.y, 0.01f)
        assertEquals(0f, reflowed.y % 1f, 0f)
    }

    @Test
    fun clampToCanvas_pullsLayerBackInsideAfterSlideRemoval() {
        // Layer living in what was slide 3 (x=2400..2800) of a 3000-wide strip.
        val original = layer(x = 2400f, y = 450f, width = 400f, height = 100f)

        val clamped = CanvasTransforms.clampToCanvas(
            layer = original,
            totalWidth = 1000f, // shrunk to a single slide
            canvasHeight = 1000f,
        )

        // Right edge must land exactly on the new right boundary (1000).
        val bounds = CanvasTransforms.layerBounds(clamped)
        assertEquals(1000f, bounds.right, 0.01f)
        assertEquals(600f, clamped.x, 0.01f)
    }

    @Test
    fun clampToCanvas_leavesInBoundsLayerUntouched() {
        val original = layer(x = 100f, y = 100f)

        val clamped = CanvasTransforms.clampToCanvas(original, totalWidth = 1000f, canvasHeight = 1000f)

        assertEquals(original.x, clamped.x, 0f)
        assertEquals(original.y, clamped.y, 0f)
    }
}
