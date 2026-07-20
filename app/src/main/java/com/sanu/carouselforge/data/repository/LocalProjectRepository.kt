package com.sanu.carouselforge.data.repository

import com.sanu.carouselforge.data.local.LayerEntity
import com.sanu.carouselforge.data.local.ProjectDao
import com.sanu.carouselforge.data.local.ProjectEntity
import com.sanu.carouselforge.data.local.ProjectSummaryRow
import com.sanu.carouselforge.data.local.ProjectWithLayers
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalProjectRepository(
    private val dao: ProjectDao,
    private val fileStore: ProjectFileStore,
    autosaveContext: CoroutineContext = Dispatchers.IO,
    private val onAutosaveError: (Throwable) -> Unit = {},
) : ProjectRepository, AutoCloseable {
    private val repositoryJob = SupervisorJob()
    private val autosaveScope = CoroutineScope(autosaveContext + repositoryJob)
    private val writeMutex = Mutex()
    private val autosaveLock = Any()
    private val autosaveJobs = mutableMapOf<String, Job>()

    override suspend fun getProject(id: String): Project {
        require(id.isNotBlank()) { "Project id must not be blank" }
        return dao.getProject(id)?.let(::projectWithLayersToDomain)
            ?: throw NoSuchElementException("Project not found: $id")
    }

    override suspend fun saveProject(project: Project) {
        cancelPendingAutosave(project.id)
        persistProject(project)
    }

    override suspend fun listProjects(): List<ProjectSummary> =
        dao.listProjects().map(::summaryRowToDomain)

    override suspend fun deleteProject(id: String) {
        require(id.isNotBlank()) { "Project id must not be blank" }
        cancelPendingAutosave(id)
        writeMutex.withLock {
            dao.deleteProject(id)
            fileStore.deleteProjectFiles(id)
        }
    }

    fun scheduleAutosave(project: Project) {
        val snapshot = project.copy(layers = project.layers.toList())
        val job = autosaveScope.launch(start = CoroutineStart.LAZY) {
            try {
                delay(AUTOSAVE_DEBOUNCE_MILLIS)
                persistProject(snapshot)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                onAutosaveError(error)
            } finally {
                val currentJob = coroutineContext[Job]
                synchronized(autosaveLock) {
                    if (autosaveJobs[snapshot.id] === currentJob) {
                        autosaveJobs.remove(snapshot.id)
                    }
                }
            }
        }

        synchronized(autosaveLock) {
            autosaveJobs.put(snapshot.id, job)?.cancel()
            job.start()
        }
    }

    fun cancelPendingAutosave(projectId: String) {
        synchronized(autosaveLock) {
            autosaveJobs.remove(projectId)?.cancel()
        }
    }

    override fun close() {
        synchronized(autosaveLock) {
            autosaveJobs.values.forEach(Job::cancel)
            autosaveJobs.clear()
        }
        autosaveScope.cancel()
    }

    private suspend fun persistProject(project: Project) {
        writeMutex.withLock {
            dao.replaceProject(
                project = project.toEntity(),
                layers = project.layers.map { layer -> layer.toEntity(project.id) },
            )
        }
    }

    private fun projectWithLayersToDomain(value: ProjectWithLayers): Project =
        Project(
            id = value.project.id,
            name = value.project.name,
            createdAt = value.project.createdAt,
            updatedAt = value.project.updatedAt,
            canvasWidth = value.project.canvasWidth,
            canvasHeight = value.project.canvasHeight,
            slideCount = value.project.slideCount,
            bgColorStart = value.project.bgColorStart,
            bgColorEnd = value.project.bgColorEnd,
            layers = value.layers
                .sortedBy(LayerEntity::zIndex)
                .map(::layerEntityToDomain),
        )

    private fun Project.toEntity(): ProjectEntity =
        ProjectEntity(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            slideCount = slideCount,
            bgColorStart = bgColorStart,
            bgColorEnd = bgColorEnd,
        )

    private fun Layer.toEntity(projectId: String): LayerEntity =
        LayerEntity(
            id = id,
            projectId = projectId,
            type = type.name,
            imageUri = imageUri,
            text = text,
            x = x,
            y = y,
            width = width,
            height = height,
            scale = scale,
            rotation = rotation,
            zIndex = zIndex,
            textColor = textColor,
            textSizeSp = textSizeSp,
            fontWeight = fontWeight,
            textAlign = textAlign.name,
            fontFamily = fontFamily,
            alpha = alpha,
            cornerRadius = cornerRadius,
            hasShadow = hasShadow,
            cropLeft = cropLeft,
            cropTop = cropTop,
            cropRight = cropRight,
            cropBottom = cropBottom,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            filterPreset = filterPreset,
            shapeKind = shapeKind?.name,
            fillColor = fillColor,
        )

    private fun layerEntityToDomain(value: LayerEntity): Layer =
        Layer(
            id = value.id,
            type = LayerType.valueOf(value.type),
            imageUri = value.imageUri,
            text = value.text,
            x = value.x,
            y = value.y,
            width = value.width,
            height = value.height,
            scale = value.scale,
            rotation = value.rotation,
            zIndex = value.zIndex,
            textColor = value.textColor,
            textSizeSp = value.textSizeSp,
            fontWeight = value.fontWeight,
            textAlign = runCatching { TextAlignment.valueOf(value.textAlign) }
                .getOrDefault(TextAlignment.CENTER),
            fontFamily = value.fontFamily,
            alpha = value.alpha,
            cornerRadius = value.cornerRadius,
            hasShadow = value.hasShadow,
            cropLeft = value.cropLeft,
            cropTop = value.cropTop,
            cropRight = value.cropRight,
            cropBottom = value.cropBottom,
            brightness = value.brightness,
            contrast = value.contrast,
            saturation = value.saturation,
            filterPreset = value.filterPreset,
            shapeKind = value.shapeKind?.let { kind ->
                runCatching { ShapeKind.valueOf(kind) }.getOrNull()
            },
            fillColor = value.fillColor,
        )

    private fun summaryRowToDomain(value: ProjectSummaryRow): ProjectSummary =
        ProjectSummary(
            id = value.id,
            name = value.name,
            createdAt = value.createdAt,
            updatedAt = value.updatedAt,
            canvasWidth = value.canvasWidth,
            canvasHeight = value.canvasHeight,
            slideCount = value.slideCount,
            layerCount = value.layerCount,
            coverImageUri = value.coverImageUri,
        )

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MILLIS = 400L
    }
}
