package com.sanu.carouselforge.features.editor.render

import androidx.compose.runtime.Immutable

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
)

enum class LayerType {
    IMAGE,
    TEXT,
    SHAPE,
}

data class TransformDelta(
    val panX: Float,
    val panY: Float,
    val zoom: Float,
    val rotation: Float,
)
