package com.sanu.carouselforge.data.repository

enum class LayerType {
    IMAGE,
    TEXT,
    STICKER,
    SHAPE,
}

data class Layer(
    val id: String,
    val type: LayerType,
    val imageUri: String?,
    val text: String?,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val zIndex: Int,
) {
    init {
        require(id.isNotBlank()) { "Layer id must not be blank" }
        require(imageUri == null || imageUri.isNotBlank()) {
            "Layer image URI must be null or non-blank"
        }
        require(x.isFinite() && y.isFinite()) { "Layer position must be finite" }
        require(scale.isFinite() && scale > 0f) { "Layer scale must be finite and positive" }
        require(rotation.isFinite()) { "Layer rotation must be finite" }
        require(type !in setOf(LayerType.IMAGE, LayerType.STICKER) || imageUri != null) {
            "$type layers require an image URI"
        }
        require(type != LayerType.TEXT || text != null) { "TEXT layers require text content" }
    }
}

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
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
        require(layers.map(Layer::id).distinct().size == layers.size) {
            "Layer ids must be unique within a project"
        }
        require(layers.map(Layer::zIndex).distinct().size == layers.size) {
            "Layer z-indices must be unique within a project"
        }
    }
}

data class ProjectSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
)

interface ProjectRepository {
    suspend fun getProject(id: String): Project

    suspend fun saveProject(project: Project)

    suspend fun listProjects(): List<ProjectSummary>

    suspend fun deleteProject(id: String)
}
