package com.sanu.carouselforge.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDaoInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ProjectDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.projectDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replaceProject_roundTripsProjectAndLayers() = runBlocking {
        val project = ProjectEntity(
            id = "project",
            name = "Test carousel",
            createdAt = 1L,
            updatedAt = 2L,
            canvasWidth = 1080,
            canvasHeight = 1080,
        )
        val layer = LayerEntity(
            id = "layer",
            projectId = project.id,
            type = "TEXT",
            imageUri = null,
            text = "Hello",
            x = 12f,
            y = 24f,
            scale = 1f,
            rotation = 0f,
            zIndex = 0,
        )

        dao.replaceProject(project, listOf(layer))

        val stored = dao.getProject(project.id)
        assertNotNull(stored)
        assertEquals(project, stored?.project)
        assertEquals(listOf(layer), stored?.layers)
    }
}
