package com.sanu.carouselforge.features.editor.slice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SliceEngineTest {
    @Test
    fun calculateSquareSlices_1080By4320ProducesFourPixelExactSlices() {
        val slices = SliceEngine.calculateSquareSlices(
            sourceWidth = 1080,
            sourceHeight = 4320,
        )

        assertEquals(4, slices.size)
        assertEquals(
            listOf(
                PixelRect(0, 0, 1080, 1080),
                PixelRect(0, 1080, 1080, 2160),
                PixelRect(0, 2160, 1080, 3240),
                PixelRect(0, 3240, 1080, 4320),
            ),
            slices.map(ImageSlice::sourceRect),
        )
        slices.zipWithNext().forEach { (current, next) ->
            assertEquals(current.sourceRect.bottom, next.sourceRect.top)
        }
    }

    @Test
    fun calculateSlices_horizontalStripHasNoGapsOrOverlaps() {
        val slices = SliceEngine.calculateSlices(
            sourceWidth = 4320,
            sourceHeight = 1080,
            sliceWidth = 1080,
            sliceHeight = 1080,
        )

        assertEquals(4, slices.size)
        slices.forEachIndexed { index, slice ->
            assertEquals(index, slice.index)
            assertEquals(1080, slice.sourceRect.width)
            assertEquals(1080, slice.sourceRect.height)
        }
        slices.zipWithNext().forEach { (current, next) ->
            assertEquals(current.sourceRect.right, next.sourceRect.left)
        }
        assertEquals(0, slices.first().sourceRect.left)
        assertEquals(4320, slices.last().sourceRect.right)
    }

    @Test
    fun calculateSlices_rejectsRemainderPixels() {
        assertThrows(IllegalArgumentException::class.java) {
            SliceEngine.calculateSlices(
                sourceWidth = 1080,
                sourceHeight = 4321,
                sliceWidth = 1080,
                sliceHeight = 1080,
            )
        }
    }
}
