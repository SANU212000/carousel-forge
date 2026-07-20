package com.sanu.carouselforge.features.editor.snap

import kotlin.math.abs

/**
 * Pure, framework-free smart-guide math run while a layer is dragged. Given the dragged
 * layer's raw bounds plus siblings and canvas geometry, it returns the snap-adjusted
 * translation, the alignment guide lines to draw, and distance badges to nearest
 * neighbours. Single-threshold hysteresis lives in the caller, which keeps the raw finger
 * position: while raw stays within [thresholdPx] of a guide the returned translation locks
 * onto it; dragging past the threshold releases it.
 */
object GuideEngine {
    data class DistanceBadge(
        val xPx: Float,
        val yPx: Float,
        val distancePx: Float,
    )

    data class GuideResult(
        val translationXPx: Float,
        val translationYPx: Float,
        val verticalLinesPx: List<Float>,
        val horizontalLinesPx: List<Float>,
        val badges: List<DistanceBadge>,
    ) {
        val didSnap: Boolean
            get() = verticalLinesPx.isNotEmpty() || horizontalLinesPx.isNotEmpty()

        companion object {
            val EMPTY = GuideResult(0f, 0f, emptyList(), emptyList(), emptyList())
        }
    }

    fun compute(
        moving: LayerBounds,
        siblings: List<LayerBounds>,
        totalWidth: Float,
        canvasHeight: Float,
        cutLinesX: List<Float>,
        thresholdPx: Float,
    ): GuideResult {
        val eligible = siblings.filter { it.id != moving.id }

        val verticalGuides = buildList {
            add(0f)
            add(totalWidth / 2f)
            add(totalWidth)
            addAll(cutLinesX)
            eligible.forEach { add(it.left); add(it.centerX); add(it.right) }
        }
        val horizontalGuides = buildList {
            add(0f)
            add(canvasHeight / 2f)
            add(canvasHeight)
            eligible.forEach { add(it.top); add(it.centerY); add(it.bottom) }
        }

        val dx = bestTranslation(listOf(moving.left, moving.centerX, moving.right), verticalGuides, thresholdPx)
        val dy = bestTranslation(listOf(moving.top, moving.centerY, moving.bottom), horizontalGuides, thresholdPx)

        val moved = moving.translated(dx, dy)
        val activeVertical = activeGuides(listOf(moved.left, moved.centerX, moved.right), verticalGuides)
        val activeHorizontal = activeGuides(listOf(moved.top, moved.centerY, moved.bottom), horizontalGuides)

        return GuideResult(
            translationXPx = dx,
            translationYPx = dy,
            verticalLinesPx = activeVertical,
            horizontalLinesPx = activeHorizontal,
            badges = spacingBadges(moved, eligible),
        )
    }

    private fun bestTranslation(
        anchors: List<Float>,
        guides: List<Float>,
        thresholdPx: Float,
    ): Float {
        var best = 0f
        var bestDistance = thresholdPx
        anchors.forEach { anchor ->
            guides.forEach { guide ->
                val delta = guide - anchor
                if (abs(delta) <= bestDistance) {
                    bestDistance = abs(delta)
                    best = delta
                }
            }
        }
        return best
    }

    private fun activeGuides(anchors: List<Float>, guides: List<Float>): List<Float> =
        guides.filter { guide -> anchors.any { abs(it - guide) < ALIGN_EPSILON } }.distinct()

    /** Gap badges to the nearest overlapping sibling on each side of the moved layer. */
    private fun spacingBadges(moved: LayerBounds, siblings: List<LayerBounds>): List<DistanceBadge> {
        val badges = mutableListOf<DistanceBadge>()

        siblings.filter { verticallyOverlap(moved, it) && it.right <= moved.left }
            .maxByOrNull { it.right }
            ?.let { left ->
                val gap = moved.left - left.right
                if (gap > 0f) {
                    badges += DistanceBadge((left.right + moved.left) / 2f, moved.centerY, gap)
                }
            }
        siblings.filter { verticallyOverlap(moved, it) && it.left >= moved.right }
            .minByOrNull { it.left }
            ?.let { right ->
                val gap = right.left - moved.right
                if (gap > 0f) {
                    badges += DistanceBadge((moved.right + right.left) / 2f, moved.centerY, gap)
                }
            }
        siblings.filter { horizontallyOverlap(moved, it) && it.bottom <= moved.top }
            .maxByOrNull { it.bottom }
            ?.let { top ->
                val gap = moved.top - top.bottom
                if (gap > 0f) {
                    badges += DistanceBadge(moved.centerX, (top.bottom + moved.top) / 2f, gap)
                }
            }
        siblings.filter { horizontallyOverlap(moved, it) && it.top >= moved.bottom }
            .minByOrNull { it.top }
            ?.let { bottom ->
                val gap = bottom.top - moved.bottom
                if (gap > 0f) {
                    badges += DistanceBadge(moved.centerX, (moved.bottom + bottom.top) / 2f, gap)
                }
            }
        return badges
    }

    private fun verticallyOverlap(a: LayerBounds, b: LayerBounds): Boolean =
        a.top < b.bottom && b.top < a.bottom

    private fun horizontallyOverlap(a: LayerBounds, b: LayerBounds): Boolean =
        a.left < b.right && b.left < a.right

    private const val ALIGN_EPSILON = 0.5f
}
