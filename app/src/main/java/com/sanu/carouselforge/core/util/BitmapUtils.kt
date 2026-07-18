package com.sanu.carouselforge.core.util

object BitmapUtils {
    /**
     * Returns the largest power-of-two sample size whose decoded dimensions
     * remain at least as large as both requested dimensions.
     */
    fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        requestedWidth: Int,
        requestedHeight: Int,
    ): Int {
        require(sourceWidth > 0 && sourceHeight > 0) {
            "Source dimensions must be positive."
        }
        require(requestedWidth > 0 && requestedHeight > 0) {
            "Requested dimensions must be positive."
        }

        var sampleSize = 1L
        while (
            sourceWidth.toLong() / (sampleSize * 2L) >= requestedWidth &&
            sourceHeight.toLong() / (sampleSize * 2L) >= requestedHeight
        ) {
            sampleSize *= 2L
        }
        return sampleSize.toInt()
    }
}
