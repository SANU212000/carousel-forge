package com.sanu.carouselforge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.sanu.carouselforge.core.theme.CarouselForgeTheme
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LayerType
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.features.editor.render.EDITOR_CANVAS_TEST_TAG
import com.sanu.carouselforge.features.editor.render.EditorCanvas
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.TransformDelta
import com.sanu.carouselforge.features.export.ExportEngine
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CarouselGoldenPathTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun importFourImages_dragLayer_exportHasExpectedDimensions() {
        val context = composeRule.activity
        val imageFiles = List(4) { index ->
            File(context.cacheDir, "golden-$index.png").also { file ->
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
        var layers by mutableStateOf(
            imageFiles.mapIndexed { index, file ->
                LayerModel(
                    id = "layer-$index",
                    type = com.sanu.carouselforge.features.editor.render.LayerType.IMAGE,
                    imageUri = file.toURI().toString(),
                    x = index * 120f,
                    y = index * 120f,
                    width = 400f,
                    height = 400f,
                    zIndex = index,
                )
            },
        )
        val initialX = layers.first().x
        composeRule.activity.setContent {
            CarouselForgeTheme {
                EditorCanvas(
                    layers = layers,
                    selectedLayerId = layers.first().id,
                    onSelectLayer = {},
                    onTransform = { id, delta: TransformDelta ->
                        layers = layers.map { layer ->
                            if (layer.id == id) {
                                layer.copy(x = layer.x + delta.panX, y = layer.y + delta.panY)
                            } else {
                                layer
                            }
                        }
                    },
                    onGestureEnd = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag(EDITOR_CANVAS_TEST_TAG).performTouchInput {
            swipe(start = Offset(100f, 100f), end = Offset(240f, 240f), durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertTrue(layers.first().x > initialX)

        val project = Project(
            id = "golden-project",
            name = "Golden export",
            createdAt = 1L,
            updatedAt = 2L,
            canvasWidth = 1080,
            canvasHeight = 1080,
            layers = layers.map { model ->
                Layer(
                    id = model.id,
                    type = LayerType.IMAGE,
                    imageUri = model.imageUri,
                    text = null,
                    x = model.x,
                    y = model.y,
                    scale = model.scale,
                    rotation = model.rotation,
                    zIndex = model.zIndex,
                )
            },
        )
        val result = runBlocking { ExportEngine(context).export(project) }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(result.uri).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        assertEquals(result.width, bounds.outWidth)
        assertEquals(result.height, bounds.outHeight)
        assertTrue(result.width == 2160 || result.width == 3240)
        context.contentResolver.delete(result.uri, null, null)
        imageFiles.forEach(File::delete)
    }
}
