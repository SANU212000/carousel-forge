package com.sanu.carouselforge.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert

data class ProjectWithLayers(
    @Embedded
    val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId",
    )
    val layers: List<LayerEntity>,
)

data class ProjectSummaryRow(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val slideCount: Int,
    val layerCount: Int,
)

@Dao
abstract class ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    abstract suspend fun getProject(id: String): ProjectWithLayers?

    @Query(
        """
        SELECT
            projects.id,
            projects.name,
            projects.createdAt,
            projects.updatedAt,
            projects.canvasWidth,
            projects.canvasHeight,
            projects.slideCount,
            COUNT(layers.id) AS layerCount
        FROM projects
        LEFT JOIN layers ON layers.projectId = projects.id
        GROUP BY projects.id
        ORDER BY projects.updatedAt DESC
        """,
    )
    abstract suspend fun listProjects(): List<ProjectSummaryRow>

    @Upsert
    protected abstract suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertLayers(layers: List<LayerEntity>)

    @Query("DELETE FROM layers WHERE projectId = :projectId")
    protected abstract suspend fun deleteLayers(projectId: String)

    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteProject(id: String): Int

    @Transaction
    open suspend fun replaceProject(
        project: ProjectEntity,
        layers: List<LayerEntity>,
    ) {
        upsertProject(project)
        deleteLayers(project.id)
        if (layers.isNotEmpty()) {
            insertLayers(layers)
        }
    }
}
