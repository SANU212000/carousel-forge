package com.sanu.carouselforge.data.repository

enum class LayerType {
    IMAGE,
    TEXT,
    STICKER,
    SHAPE,
}

enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

enum class ShapeKind {
    RECT,
    CIRCLE,
    LINE,
    ARROW,
}

data class Layer(
    val id: String,
    val type: LayerType,
    val imageUri: String?,
    val text: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
    val rotation: Float,
    val zIndex: Int,
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
) {
    init {
        require(id.isNotBlank()) { "Layer id must not be blank" }
        require(imageUri == null || imageUri.isNotBlank()) {
            "Layer image URI must be null or non-blank"
        }
        require(x.isFinite() && y.isFinite()) { "Layer position must be finite" }
        require(width.isFinite() && height.isFinite() && width > 0f && height > 0f) {
            "Layer dimensions must be finite and positive"
        }
        require(scale.isFinite() && scale > 0f) { "Layer scale must be finite and positive" }
        require(rotation.isFinite()) { "Layer rotation must be finite" }
        require(type !in setOf(LayerType.IMAGE, LayerType.STICKER) || imageUri != null) {
            "$type layers require an image URI"
        }
        require(type != LayerType.TEXT || text != null) { "TEXT layers require text content" }
        require(alpha in 0f..1f) { "Layer alpha must be within 0..1" }
        require(cornerRadius.isFinite() && cornerRadius >= 0f) {
            "Layer corner radius must be finite and non-negative"
        }
        require(cropLeft in 0f..1f && cropTop in 0f..1f && cropRight in 0f..1f && cropBottom in 0f..1f) {
            "Crop fractions must be within 0..1"
        }
        require(cropRight > cropLeft && cropBottom > cropTop) {
            "Crop rectangle must have positive area"
        }
    }
}

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val slideCount: Int = 1,
    val bgColorStart: Long = 0xFFFFFFFFL,
    val bgColorEnd: Long? = null,
    val layers: List<Layer>,
) {
    init {
        require(id.isNotBlank()) { "Project id must not be blank" }
        require(name.isNotBlank()) { "Project name must not be blank" }
        require(createdAt >= 0L) { "Project creation time must not be negative" }
        require(updatedAt >= createdAt) {
            "Project update time must not precede its creation time"
        }
        require(canvasWidth > 0 && canvasHeight > 0) {
            "Project canvas dimensions must be positive"
        }
        require(slideCount >= 1) { "Project must have at least one slide" }
        require(layers.map(Layer::id).distinct().size == layers.size) {
            "Layer ids must be unique within a project"
        }
        require(layers.map(Layer::zIndex).distinct().size == layers.size) {
            "Layer z-indices must be unique within a project"
        }
    }

    /** Total logical canvas width across all connected slides. */
    val totalWidth: Int get() = canvasWidth * slideCount
}

data class ProjectSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val slideCount: Int,
    val layerCount: Int,
)

interface ProjectRepository {
    suspend fun getProject(id: String): Project

    suspend fun saveProject(project: Project)

    suspend fun listProjects(): List<ProjectSummary>

    suspend fun deleteProject(id: String)
}
