package com.sanu.carouselforge.core.util

/**
 * Pure color-matrix math shared by the editor preview (Compose `ColorMatrix`) and the
 * export renderer (`android.graphics.ColorMatrix`). Keeping it framework-free means the
 * same 4x5 matrix drives both paths, so what the user sees is exactly what exports, and
 * the composition can be unit-tested without Android.
 */
object LayerColorMatrix {
    const val WARM = "WARM"
    const val COOL = "COOL"
    const val MONO = "B&W"
    const val FADE = "FADE"
    const val VIVID = "VIVID"

    /** Ordered preset ids offered in the filter row. */
    val PRESETS: List<String> = listOf(WARM, COOL, MONO, FADE, VIVID)

    /** True when the adjustments and preset leave pixels untouched (skip the filter). */
    fun isIdentity(brightness: Float, contrast: Float, saturation: Float, preset: String?): Boolean =
        preset == null && brightness == 0f && contrast == 1f && saturation == 1f

    /**
     * Builds the combined 4x5 color matrix. [brightness] is -1..1 (added lightness),
     * [contrast] is a multiplier around mid-grey (1 = neutral), [saturation] is 0..2
     * (1 = neutral). The optional [preset] is applied before the manual adjustments.
     */
    fun build(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        preset: String?,
    ): FloatArray {
        var matrix = identity()
        matrix = concat(saturationMatrix(saturation), matrix)
        preset?.let { matrix = concat(presetMatrix(it), matrix) }
        matrix = concat(contrastMatrix(contrast), matrix)
        matrix = concat(brightnessMatrix(brightness), matrix)
        return matrix
    }

    private fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private fun saturationMatrix(saturation: Float): FloatArray {
        val s = saturation
        val invSat = 1f - s
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val b = 0.072f * invSat
        return floatArrayOf(
            r + s, g, b, 0f, 0f,
            r, g + s, b, 0f, 0f,
            r, g, b + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private fun contrastMatrix(contrast: Float): FloatArray {
        val c = contrast
        val translate = 0.5f * 255f * (1f - c)
        return floatArrayOf(
            c, 0f, 0f, 0f, translate,
            0f, c, 0f, 0f, translate,
            0f, 0f, c, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private fun brightnessMatrix(brightness: Float): FloatArray {
        val offset = brightness * 255f
        return floatArrayOf(
            1f, 0f, 0f, 0f, offset,
            0f, 1f, 0f, 0f, offset,
            0f, 0f, 1f, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    private fun presetMatrix(preset: String): FloatArray = when (preset) {
        MONO -> saturationMatrix(0f)
        WARM -> floatArrayOf(
            1.1f, 0f, 0f, 0f, 8f,
            0f, 1.02f, 0f, 0f, 4f,
            0f, 0f, 0.88f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        COOL -> floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f, 2f,
            0f, 0f, 1.12f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f,
        )
        FADE -> concat(
            // Lift blacks then reduce contrast for a soft, matte look.
            contrastMatrix(0.82f),
            floatArrayOf(
                1f, 0f, 0f, 0f, 24f,
                0f, 1f, 0f, 0f, 22f,
                0f, 0f, 1f, 0f, 26f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        VIVID -> concat(contrastMatrix(1.12f), saturationMatrix(1.35f))
        else -> identity()
    }

    /** Returns A∘B: the matrix that applies B first, then A, to an RGBA color vector. */
    internal fun concat(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(20)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) sum += a[row * 5 + k] * b[k * 5 + col]
                result[row * 5 + col] = sum
            }
            var translate = a[row * 5 + 4]
            for (k in 0..3) translate += a[row * 5 + k] * b[k * 5 + 4]
            result[row * 5 + 4] = translate
        }
        return result
    }
}
