package com.sanu.carouselforge.core.di

import android.content.Context
import com.sanu.carouselforge.core.prefs.UserPreferencesRepository
import com.sanu.carouselforge.data.local.AppDatabase
import com.sanu.carouselforge.data.repository.InternalProjectFileStore
import com.sanu.carouselforge.data.repository.LocalProjectRepository
import com.sanu.carouselforge.data.repository.ProjectFileStore
import com.sanu.carouselforge.data.repository.ProjectRepository

class AppModule(context: Context) {
    private val database = AppDatabase.create(context)
    val projectFileStore: ProjectFileStore = InternalProjectFileStore(context)

    val projectRepository: ProjectRepository = LocalProjectRepository(
        dao = database.projectDao(),
        fileStore = projectFileStore,
    )

    val userPreferences = UserPreferencesRepository(context)
}
