package com.sanu.carouselforge.features.gallery

import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LayerType
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ShapeKind
import com.sanu.carouselforge.data.repository.TextAlignment
import java.util.UUID

/**
 * Built-in starter carousels. Each template is a pure factory that produces a fresh
 * [Project] (with new ids) from text layers, shapes and a background, so tapping one
 * lands the user straight in the editor on a pre-filled design.
 */
object TemplateCatalog {
    data class Template(
        val id: String,
        val title: String,
        val uses: String,
        val preset: CanvasPreset,
        val slideCount: Int,
        val bgColorStart: Long,
        val bgColorEnd: Long?,
        val layersFactory: (width: Int, height: Int) -> List<Layer>,
    ) {
        fun toProject(): Project {
            val now = System.currentTimeMillis()
            return Project(
                id = UUID.randomUUID().toString(),
                name = title,
                createdAt = now,
                updatedAt = now,
                canvasWidth = preset.width,
                canvasHeight = preset.height,
                slideCount = slideCount,
                bgColorStart = bgColorStart,
                bgColorEnd = bgColorEnd,
                layers = layersFactory(preset.width, preset.height),
            )
        }
    }

    private const val WHITE = 0xFFFFFFFFL
    private const val INK = 0xFF111111L
    private const val AMBER = 0xFFFBBF24L
    private const val BLUE = 0xFF3B82F6L

    val templates: List<Template> = listOf(
        Template("hook", "Bold Hook", "Hook slide", CanvasPreset.PORTRAIT, 1, INK, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.28f, w * 0.8f, h * 0.3f, "THE ONE THING NOBODY TELLS YOU", w * 0.11f, WHITE, 800),
                text(1, w * 0.1f, h * 0.62f, w * 0.8f, h * 0.1f, "swipe →", w * 0.05f, AMBER, 600),
            )
        },
        Template("quote", "Quote", "Quote", CanvasPreset.SQUARE, 1, 0xFF6366F1L, 0xFF06B6D4L) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.32f, w * 0.8f, h * 0.36f, "\"Done is better than perfect.\"", w * 0.09f, WHITE, 700),
                text(1, w * 0.1f, h * 0.74f, w * 0.8f, h * 0.08f, "— unknown", w * 0.045f, WHITE, 400),
            )
        },
        Template("tips", "Tips 1·2·3", "3 slides", CanvasPreset.PORTRAIT, 3, 0xFFF5F5F5L, null) { w, h ->
            (0..2).flatMap { slide ->
                val x = slide * w
                listOf(
                    text(slide * 2, x + w * 0.1f, h * 0.22f, w * 0.8f, h * 0.2f, "${slide + 1}", w * 0.3f, AMBER, 800, TextAlignment.LEFT),
                    text(slide * 2 + 1, x + w * 0.1f, h * 0.5f, w * 0.8f, h * 0.28f, "Tip number ${slide + 1} goes here.", w * 0.07f, INK, 600, TextAlignment.LEFT),
                )
            }
        },
        Template("beforeafter", "Before / After", "2 slides", CanvasPreset.PORTRAIT, 2, INK, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.44f, w * 0.8f, h * 0.12f, "BEFORE", w * 0.14f, WHITE, 800),
                text(1, w + w * 0.1f, h * 0.44f, w * 0.8f, h * 0.12f, "AFTER", w * 0.14f, AMBER, 800),
            )
        },
        Template("cta", "CTA Finish", "CTA slide", CanvasPreset.PORTRAIT, 1, AMBER, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.34f, w * 0.8f, h * 0.2f, "FOLLOW FOR MORE", w * 0.1f, INK, 800),
                shape(1, w * 0.35f, h * 0.58f, w * 0.3f, h * 0.06f, ShapeKind.ARROW, INK),
            )
        },
        Template("cover", "Minimal Cover", "Cover", CanvasPreset.PORTRAIT, 1, WHITE, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.2f, w * 0.8f, h * 0.06f, "GUIDE", w * 0.045f, BLUE, 700, TextAlignment.LEFT),
                text(1, w * 0.1f, h * 0.3f, w * 0.8f, h * 0.34f, "A short, punchy title for your carousel", w * 0.1f, INK, 800, TextAlignment.LEFT),
                shape(2, w * 0.1f, h * 0.68f, w * 0.35f, h * 0.01f, ShapeKind.LINE, BLUE),
            )
        },
        Template("stat", "Stat Punch", "Stat", CanvasPreset.SQUARE, 1, INK, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.3f, w * 0.8f, h * 0.24f, "92%", w * 0.28f, AMBER, 800),
                text(1, w * 0.1f, h * 0.6f, w * 0.8f, h * 0.14f, "of people miss this step", w * 0.06f, WHITE, 500),
            )
        },
        Template("checklist", "Checklist", "Checklist", CanvasPreset.PORTRAIT, 1, 0xFFA7F3D0L, null) { w, h ->
            listOf(
                text(0, w * 0.1f, h * 0.14f, w * 0.8f, h * 0.12f, "5-STEP CHECKLIST", w * 0.09f, INK, 800, TextAlignment.LEFT),
                text(1, w * 0.1f, h * 0.34f, w * 0.8f, h * 0.4f, "✓ Plan\n✓ Draft\n✓ Design\n✓ Review\n✓ Publish", w * 0.07f, INK, 600, TextAlignment.LEFT),
            )
        },
    )

    private fun text(
        zIndex: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        text: String,
        size: Float,
        color: Long,
        weight: Int,
        align: TextAlignment = TextAlignment.CENTER,
    ): Layer = Layer(
        id = UUID.randomUUID().toString(),
        type = LayerType.TEXT,
        imageUri = null,
        text = text,
        x = x,
        y = y,
        width = width,
        height = height,
        scale = 1f,
        rotation = 0f,
        zIndex = zIndex,
        textColor = color,
        textSizeSp = size,
        fontWeight = weight,
        textAlign = align,
    )

    private fun shape(
        zIndex: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        kind: ShapeKind,
        color: Long,
    ): Layer = Layer(
        id = UUID.randomUUID().toString(),
        type = LayerType.SHAPE,
        imageUri = null,
        text = null,
        x = x,
        y = y,
        width = width,
        height = height,
        scale = 1f,
        rotation = 0f,
        zIndex = zIndex,
        shapeKind = kind,
        fillColor = color,
    )
}
