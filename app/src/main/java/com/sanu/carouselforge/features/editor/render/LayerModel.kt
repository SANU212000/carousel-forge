package com.sanu.carouselforge.features.editor.render

import androidx.compose.runtime.Immutable
import com.sanu.carouselforge.data.repository.ShapeKind
import com.sanu.carouselforge.data.repository.TextAlignment

@Immutable
data class LayerModel(
    val id: String,
    val type: LayerType,
    val imageUri: String? = null,
    val text: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float,
    val height: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
    val textColor: Long = 0xFF000000L,
    val textSizeSp: Float = 32f,
    val fontWeight: Int = 400,
    val textAlign: TextAlignment = TextAlignment.CENTER,
    val fontFamily: String? = null,
    val alpha: Float = 1f,
    val cornerRadius: Float = 0f,
    val hasShadow: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val filterPreset: String? = null,
    val shapeKind: ShapeKind? = null,
    val fillColor: Long? = null,
)

enum class LayerType {
    IMAGE,
    TEXT,
    STICKER,
    SHAPE,
}

data class TransformDelta(
    val panX: Float,
    val panY: Float,
    val zoom: Float,
    val rotation: Float,
)
