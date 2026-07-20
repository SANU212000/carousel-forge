package com.sanu.carouselforge.features.editor.snap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapEngineTest {
    @Test
    fun resolve_snapsEachAxisToNearestGridLine() {
        val moving = LayerBounds("moving", left = 93f, top = 207f, right = 143f, bottom = 257f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = emptyList(),
            thresholdPx = 8f,
            gridSpacingPx = 100f,
        )

        assertTrue(result.didSnap)
        assertEquals(7f, result.translationXPx, 0f)
        assertEquals(-7f, result.translationYPx, 0f)
        assertEquals(100f, result.targetBounds.left, 0f)
        assertEquals(200f, result.targetBounds.top, 0f)
        assertTrue(result.horizontal?.target is SnapTarget.GridLine)
        assertTrue(result.vertical?.target is SnapTarget.GridLine)
    }

    @Test
    fun resolve_snapsMovingEdgeToSiblingEdge() {
        val moving = LayerBounds("moving", left = 90f, top = 20f, right = 140f, bottom = 70f)
        val sibling = LayerBounds("sibling", left = 142f, top = 100f, right = 192f, bottom = 150f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = listOf(sibling),
            thresholdPx = 3f,
        )

        assertEquals(2f, result.translationXPx, 0f)
        assertEquals(0f, result.translationYPx, 0f)
        assertEquals(SnapAnchor.END, result.horizontal?.movingAnchor)
        val target = result.horizontal?.target as SnapTarget.SiblingEdge
        assertEquals("sibling", target.siblingId)
        assertEquals(SnapAnchor.START, target.siblingAnchor)
        assertNull(result.vertical)
    }

    @Test
    fun resolve_usesSiblingWhenSiblingAndGridAreEquallyNear() {
        val moving = LayerBounds("moving", left = 98f, top = 20f, right = 148f, bottom = 70f)
        val sibling = LayerBounds("sibling", left = 100f, top = 100f, right = 150f, bottom = 150f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = listOf(sibling),
            thresholdPx = 2f,
            gridSpacingPx = 100f,
        )

        assertTrue(result.horizontal?.target is SnapTarget.SiblingEdge)
        assertEquals(2f, result.translationXPx, 0f)
    }

    @Test
    fun resolve_doesNotSnapOutsideInclusiveThreshold() {
        val moving = LayerBounds("moving", left = 94f, top = 14f, right = 144f, bottom = 64f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = emptyList(),
            thresholdPx = 5f,
            gridSpacingPx = 100f,
        )

        assertFalse(result.didSnap)
        assertEquals(moving, result.targetBounds)
    }

    @Test
    fun resolve_snapsLeftEdgeToSlideBoundary() {
        // Moving layer's left edge is 4px shy of a cut line at x = 1000.
        val moving = LayerBounds("moving", left = 996f, top = 20f, right = 1096f, bottom = 120f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = emptyList(),
            thresholdPx = 6f,
            snapLinesX = listOf(500f, 1000f),
        )

        assertTrue(result.horizontal?.target is SnapTarget.SlideBoundary)
        assertEquals(4f, result.translationXPx, 0f)
        assertEquals(1000f, result.targetBounds.left, 0f)
        assertNull(result.vertical)
    }

    @Test
    fun resolve_ignoresSiblingWithSameId() {
        val moving = LayerBounds("same", left = 10f, top = 10f, right = 20f, bottom = 20f)
        val staleCopy = LayerBounds("same", left = 11f, top = 11f, right = 21f, bottom = 21f)

        val result = SnapEngine.resolve(
            moving = moving,
            siblings = listOf(staleCopy),
            thresholdPx = 2f,
        )

        assertFalse(result.didSnap)
    }
}
