package com.sanu.carouselforge.core.model

/**
 * Per-slide canvas dimensions offered to the user. Width/height are the source of
 * truth persisted on the project; the preset is only used to seed and to highlight
 * the currently matching option in the ratio picker.
 */
enum class CanvasPreset(
    val label: String,
    val width: Int,
    val height: Int,
) {
    PORTRAIT("4:5", 1080, 1350),
    SQUARE("1:1", 1080, 1080),
    CLASSIC("3:4", 1080, 1440),
    STORY("9:16", 1080, 1920),
    LANDSCAPE("16:9", 1920, 1080);

    val aspectRatio: Float get() = width.toFloat() / height

    companion object {
        val DEFAULT: CanvasPreset = PORTRAIT

        /** Returns the preset whose exact pixel size matches, or null for a custom size. */
        fun matching(width: Int, height: Int): CanvasPreset? =
            entries.firstOrNull { it.width == width && it.height == height }
    }
}
