package com.sanu.carouselforge.features.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.util.BitmapUtils
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LayerType
import com.sanu.carouselforge.data.repository.Project
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExportResult(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val warning: AppError.MemoryError? = null,
)

class ExportEngine(
    context: Context,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun export(project: Project): ExportResult {
        val scale = if (availableHeapBytes() < HIGH_QUALITY_HEAP_BYTES) {
            REDUCED_SUPERSAMPLE
        } else {
            DEFAULT_SUPERSAMPLE
        }
        val bitmap = withContext(computeDispatcher) {
            composite(project, scale)
        }
        return try {
            val uri = withContext(ioDispatcher) {
                savePng(bitmap, project.name)
            }
            ExportResult(
                uri = uri,
                width = bitmap.width,
                height = bitmap.height,
                warning = if (scale < DEFAULT_SUPERSAMPLE) {
                    AppError.MemoryError("high-resolution export")
                } else {
                    null
                },
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun composite(project: Project, supersample: Int): Bitmap {
        val width = Math.multiplyExact(project.canvasWidth, supersample)
        val height = Math.multiplyExact(project.canvasHeight, supersample)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.WHITE)
        project.layers.sortedBy(Layer::zIndex).forEach { layer ->
            drawLayer(canvas, layer, project, supersample)
        }
        return output
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: Layer,
        project: Project,
        supersample: Int,
    ) {
        val baseSize = minOf(project.canvasWidth, project.canvasHeight) * DEFAULT_LAYER_FRACTION
        val targetSize = (baseSize * supersample).toInt()
        canvas.save()
        canvas.translate(layer.x * supersample, layer.y * supersample)
        canvas.rotate(layer.rotation, targetSize / 2f, targetSize / 2f)
        canvas.scale(layer.scale, layer.scale, targetSize / 2f, targetSize / 2f)
        when (layer.type) {
            LayerType.IMAGE, LayerType.STICKER -> {
                val bitmap = layer.imageUri?.let {
                    decodeWorkingBitmap(it, targetSize, targetSize)
                }
                if (bitmap != null) {
                    val source = centerCropRect(bitmap.width, bitmap.height)
                    canvas.drawBitmap(
                        bitmap,
                        source,
                        Rect(0, 0, targetSize, targetSize),
                        IMAGE_PAINT,
                    )
                    bitmap.recycle()
                }
            }

            LayerType.TEXT -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.BLACK
                    textSize = TEXT_SIZE_FRACTION * targetSize
                }
                canvas.drawText(layer.text.orEmpty(), 0f, targetSize / 2f, paint)
            }

            LayerType.SHAPE -> {
                canvas.drawRect(
                    0f,
                    0f,
                    targetSize.toFloat(),
                    targetSize.toFloat(),
                    SHAPE_PAINT,
                )
            }
        }
        canvas.restore()
    }

    private fun decodeWorkingBitmap(uriValue: String, width: Int, height: Int): Bitmap? {
        val uri = Uri.parse(uriValue)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInput(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = BitmapUtils.calculateInSampleSize(
                options.outWidth,
                options.outHeight,
                width,
                height,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = openInput(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null
        val orientation = openInput(uri)?.use { stream ->
            runCatching {
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        return orientBitmap(decoded, orientation)
    }

    private fun openInput(uri: Uri) = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> resolver.openInputStream(uri)
        ContentResolver.SCHEME_FILE, null -> File(uri.path ?: uri.toString()).inputStream()
        else -> resolver.openInputStream(uri)
    }

    private fun orientBitmap(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        if (matrix.isIdentity) return source
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            .also { if (it !== source) source.recycle() }
    }

    private fun savePng(bitmap: Bitmap, projectName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, safeFileName(projectName))
            put(MediaStore.Images.Media.MIME_TYPE, PNG_MIME_TYPE)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$EXPORT_DIRECTORY",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore did not create an export destination")
        try {
            resolver.openOutputStream(uri, "w")?.use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) {
                    "PNG encoder failed"
                }
            } ?: throw IOException("MediaStore output stream was unavailable")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun safeFileName(projectName: String): String {
        val base = projectName.replace(UNSAFE_FILE_NAME, "_").trim('_').ifBlank { "carousel" }
        return "${base}_${System.currentTimeMillis()}.png"
    }

    private fun availableHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }

    private fun centerCropRect(width: Int, height: Int): Rect {
        val side = minOf(width, height)
        val left = (width - side) / 2
        val top = (height - side) / 2
        return Rect(left, top, left + side, top + side)
    }

    private companion object {
        const val DEFAULT_SUPERSAMPLE = 3
        const val REDUCED_SUPERSAMPLE = 2
        const val HIGH_QUALITY_HEAP_BYTES = 2L * 1024L * 1024L * 1024L
        const val DEFAULT_LAYER_FRACTION = 0.45f
        const val TEXT_SIZE_FRACTION = 0.12f
        const val PNG_QUALITY = 100
        const val PNG_MIME_TYPE = "image/png"
        const val EXPORT_DIRECTORY = "CarouselForge"
        val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9_-]+")
        val IMAGE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val SHAPE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
        }
    }
}
