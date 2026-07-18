package com.sanu.carouselforge.features.editor.snap

import kotlin.math.abs
import kotlin.math.round

data class LayerBounds(
    val id: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(id.isNotBlank()) { "Layer id must not be blank." }
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
            "Layer bounds must be finite."
        }
        require(right >= left && bottom >= top) { "Layer bounds must not be inverted." }
    }

    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun translated(deltaX: Float, deltaY: Float): LayerBounds = copy(
        left = left + deltaX,
        top = top + deltaY,
        right = right + deltaX,
        bottom = bottom + deltaY,
    )
}

enum class SnapAxis {
    HORIZONTAL,
    VERTICAL,
}

enum class SnapAnchor {
    START,
    CENTER,
    END,
}

sealed interface SnapTarget {
    val positionPx: Float

    data class GridLine(
        override val positionPx: Float,
    ) : SnapTarget

    data class SiblingEdge(
        val siblingId: String,
        val siblingAnchor: SnapAnchor,
        override val positionPx: Float,
    ) : SnapTarget
}

data class AxisSnap(
    val axis: SnapAxis,
    val movingAnchor: SnapAnchor,
    val target: SnapTarget,
    val translationPx: Float,
)

data class SnapResult(
    val originalBounds: LayerBounds,
    val targetBounds: LayerBounds,
    val horizontal: AxisSnap?,
    val vertical: AxisSnap?,
) {
    val translationXPx: Float get() = targetBounds.left - originalBounds.left
    val translationYPx: Float get() = targetBounds.top - originalBounds.top
    val didSnap: Boolean get() = horizontal != null || vertical != null
}

object SnapEngine {
    fun resolve(
        moving: LayerBounds,
        siblings: List<LayerBounds>,
        thresholdPx: Float,
        gridSpacingPx: Float? = null,
        gridOriginXPx: Float = 0f,
        gridOriginYPx: Float = 0f,
    ): SnapResult {
        require(thresholdPx.isFinite() && thresholdPx >= 0f) {
            "Snap threshold must be a finite, non-negative pixel value."
        }
        require(gridSpacingPx == null || (gridSpacingPx.isFinite() && gridSpacingPx > 0f)) {
            "Grid spacing must be null or a finite, positive pixel value."
        }
        require(gridOriginXPx.isFinite() && gridOriginYPx.isFinite()) {
            "Grid origins must be finite."
        }

        val eligibleSiblings = siblings.filter { it.id != moving.id }
        val horizontal = nearestSnap(
            axis = SnapAxis.HORIZONTAL,
            movingPositions = moving.horizontalAnchors(),
            siblingPositions = eligibleSiblings.map { it.id to it.horizontalAnchors() },
            thresholdPx = thresholdPx,
            gridSpacingPx = gridSpacingPx,
            gridOriginPx = gridOriginXPx,
        )
        val vertical = nearestSnap(
            axis = SnapAxis.VERTICAL,
            movingPositions = moving.verticalAnchors(),
            siblingPositions = eligibleSiblings.map { it.id to it.verticalAnchors() },
            thresholdPx = thresholdPx,
            gridSpacingPx = gridSpacingPx,
            gridOriginPx = gridOriginYPx,
        )

        return SnapResult(
            originalBounds = moving,
            targetBounds = moving.translated(
                deltaX = horizontal?.translationPx ?: 0f,
                deltaY = vertical?.translationPx ?: 0f,
            ),
            horizontal = horizontal,
            vertical = vertical,
        )
    }

    private fun nearestSnap(
        axis: SnapAxis,
        movingPositions: Map<SnapAnchor, Float>,
        siblingPositions: List<Pair<String, Map<SnapAnchor, Float>>>,
        thresholdPx: Float,
        gridSpacingPx: Float?,
        gridOriginPx: Float,
    ): AxisSnap? {
        val candidates = buildList {
            movingPositions.forEach { (movingAnchor, movingPosition) ->
                siblingPositions.forEach { (siblingId, anchors) ->
                    anchors.forEach { (siblingAnchor, siblingPosition) ->
                        add(
                            Candidate(
                                snap = AxisSnap(
                                    axis = axis,
                                    movingAnchor = movingAnchor,
                                    target = SnapTarget.SiblingEdge(
                                        siblingId = siblingId,
                                        siblingAnchor = siblingAnchor,
                                        positionPx = siblingPosition,
                                    ),
                                    translationPx = siblingPosition - movingPosition,
                                ),
                                targetPriority = SIBLING_PRIORITY,
                            ),
                        )
                    }
                }

                if (gridSpacingPx != null) {
                    val gridPosition = gridOriginPx +
                        round((movingPosition - gridOriginPx) / gridSpacingPx) * gridSpacingPx
                    add(
                        Candidate(
                            snap = AxisSnap(
                                axis = axis,
                                movingAnchor = movingAnchor,
                                target = SnapTarget.GridLine(gridPosition),
                                translationPx = gridPosition - movingPosition,
                            ),
                            targetPriority = GRID_PRIORITY,
                        ),
                    )
                }
            }
        }

        return candidates
            .asSequence()
            .filter { abs(it.snap.translationPx) <= thresholdPx }
            .minWithOrNull(
                compareBy<Candidate>(
                    { abs(it.snap.translationPx) },
                    { it.targetPriority },
                    { it.snap.movingAnchor.ordinal },
                ),
            )
            ?.snap
    }

    private fun LayerBounds.horizontalAnchors(): Map<SnapAnchor, Float> = linkedMapOf(
        SnapAnchor.START to left,
        SnapAnchor.CENTER to centerX,
        SnapAnchor.END to right,
    )

    private fun LayerBounds.verticalAnchors(): Map<SnapAnchor, Float> = linkedMapOf(
        SnapAnchor.START to top,
        SnapAnchor.CENTER to centerY,
        SnapAnchor.END to bottom,
    )

    private data class Candidate(
        val snap: AxisSnap,
        val targetPriority: Int,
    )

    private const val SIBLING_PRIORITY = 0
    private const val GRID_PRIORITY = 1
}
