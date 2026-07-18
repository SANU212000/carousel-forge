package com.sanu.carouselforge.data.repository

import android.content.Context
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProjectFileStore {
    suspend fun prepareProjectDirectory(projectId: String): File

    suspend fun deleteProjectFiles(projectId: String)
}

class InternalProjectFileStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProjectFileStore {
    private val projectsDirectory = File(context.applicationContext.filesDir, PROJECTS_DIRECTORY)

    override suspend fun prepareProjectDirectory(projectId: String): File =
        withContext(ioDispatcher) {
            val directory = projectDirectory(projectId)
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Unable to create project file directory")
            }
            directory
        }

    override suspend fun deleteProjectFiles(projectId: String) {
        withContext(ioDispatcher) {
            val directory = projectDirectory(projectId)
            if (directory.exists() && !directory.deleteRecursively()) {
                throw IOException("Unable to delete project files")
            }
        }
    }

    private fun projectDirectory(projectId: String): File {
        require(projectId.isNotBlank()) { "Project id must not be blank" }
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(projectId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return File(projectsDirectory, digest)
    }

    private companion object {
        const val PROJECTS_DIRECTORY = "projects"
        const val HASH_ALGORITHM = "SHA-256"
    }
}
