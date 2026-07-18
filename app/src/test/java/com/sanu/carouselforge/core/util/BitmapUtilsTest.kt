package com.sanu.carouselforge.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BitmapUtilsTest {
    @Test
    fun calculateInSampleSize_returnsLargestSafePowerOfTwo() {
        assertEquals(
            4,
            BitmapUtils.calculateInSampleSize(
                sourceWidth = 4000,
                sourceHeight = 3000,
                requestedWidth = 1000,
                requestedHeight = 750,
            ),
        )
    }

    @Test
    fun calculateInSampleSize_honorsBothRequestedDimensions() {
        assertEquals(
            2,
            BitmapUtils.calculateInSampleSize(
                sourceWidth = 4032,
                sourceHeight = 3024,
                requestedWidth = 1080,
                requestedHeight = 1080,
            ),
        )
    }

    @Test
    fun calculateInSampleSize_returnsOneWhenSourceIsAlreadySmallEnough() {
        assertEquals(
            1,
            BitmapUtils.calculateInSampleSize(
                sourceWidth = 800,
                sourceHeight = 600,
                requestedWidth = 1080,
                requestedHeight = 1080,
            ),
        )
    }

    @Test
    fun calculateInSampleSize_rejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            BitmapUtils.calculateInSampleSize(
                sourceWidth = 0,
                sourceHeight = 600,
                requestedWidth = 100,
                requestedHeight = 100,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            BitmapUtils.calculateInSampleSize(
                sourceWidth = 600,
                sourceHeight = 600,
                requestedWidth = -1,
                requestedHeight = 100,
            )
        }
    }
}
