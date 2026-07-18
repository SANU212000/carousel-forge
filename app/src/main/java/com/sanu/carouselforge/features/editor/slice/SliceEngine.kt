package com.sanu.carouselforge.features.editor.slice

data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left >= 0 && top >= 0) { "Pixel coordinates must not be negative." }
        require(right > left && bottom > top) { "Pixel rectangle must have positive area." }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class ImageSlice(
    val index: Int,
    val sourceRect: PixelRect,
)

object SliceEngine {
    /**
     * Splits a one-dimensional image strip into equal, fixed-size slices.
     *
     * The source must match the slice size on one axis and divide exactly on the
     * other. Rejecting remainder pixels prevents resized or overlapping seams.
     */
    fun calculateSlices(
        sourceWidth: Int,
        sourceHeight: Int,
        sliceWidth: Int,
        sliceHeight: Int,
    ): List<ImageSlice> {
        require(sourceWidth > 0 && sourceHeight > 0) {
            "Source dimensions must be positive."
        }
        require(sliceWidth > 0 && sliceHeight > 0) {
            "Slice dimensions must be positive."
        }

        return when {
            sourceWidth == sliceWidth && sourceHeight % sliceHeight == 0 ->
                verticalSlices(sourceHeight / sliceHeight, sliceWidth, sliceHeight)

            sourceHeight == sliceHeight && sourceWidth % sliceWidth == 0 ->
                horizontalSlices(sourceWidth / sliceWidth, sliceWidth, sliceHeight)

            else -> throw IllegalArgumentException(
                "Source must be an exact horizontal or vertical strip of fixed-size slices.",
            )
        }
    }

    fun calculateSquareSlices(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<ImageSlice> {
        val squareSize = minOf(sourceWidth, sourceHeight)
        return calculateSlices(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            sliceWidth = squareSize,
            sliceHeight = squareSize,
        )
    }

    private fun verticalSlices(
        count: Int,
        sliceWidth: Int,
        sliceHeight: Int,
    ): List<ImageSlice> = List(count) { index ->
        val top = index * sliceHeight
        ImageSlice(
            index = index,
            sourceRect = PixelRect(
                left = 0,
                top = top,
                right = sliceWidth,
                bottom = top + sliceHeight,
            ),
        )
    }

    private fun horizontalSlices(
        count: Int,
        sliceWidth: Int,
        sliceHeight: Int,
    ): List<ImageSlice> = List(count) { index ->
        val left = index * sliceWidth
        ImageSlice(
            index = index,
            sourceRect = PixelRect(
                left = left,
                top = 0,
                right = left + sliceWidth,
                bottom = sliceHeight,
            ),
        )
    }
}
