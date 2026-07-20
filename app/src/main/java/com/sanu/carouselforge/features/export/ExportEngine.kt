package com.sanu.carouselforge.features.export

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.sanu.carouselforge.core.error.AppError
import com.sanu.carouselforge.core.text.FontCatalog
import com.sanu.carouselforge.core.util.BitmapUtils
import com.sanu.carouselforge.core.util.LayerColorMatrix
import com.sanu.carouselforge.data.repository.Layer
import com.sanu.carouselforge.data.repository.LayerType
import com.sanu.carouselforge.data.repository.Project
import com.sanu.carouselforge.data.repository.ShapeKind
import com.sanu.carouselforge.data.repository.TextAlignment
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Output container format for an export run. */
enum class ExportFormat(val mimeType: String, val label: String) {
    PNG("image/png", "PNG"),
    PDF("application/pdf", "PDF"),
}

data class ExportedPage(
    val uri: Uri,
    val index: Int,
    val width: Int,
    val height: Int,
)

data class ExportResult(
    val pages: List<ExportedPage>,
    val format: ExportFormat,
    val slideCount: Int,
    val warning: AppError.MemoryError? = null,
) {
    val width: Int get() = pages.firstOrNull()?.width ?: 0
    val height: Int get() = pages.firstOrNull()?.height ?: 0
    /** Every content/file URI produced, for a follow-up system share. */
    val uris: List<Uri> get() = pages.map(ExportedPage::uri)
}

class ExportEngine(
    context: Context,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    /**
     * Renders every slide of the connected carousel to its own supersampled PNG and
     * saves them as sequentially numbered files. Each slide is composited independently
     * (translating the shared layer coordinate space) so seams are pixel-exact and no
     * single giant bitmap is allocated.
     */
    suspend fun export(
        project: Project,
        format: ExportFormat = ExportFormat.PNG,
        onProgress: (Float) -> Unit = {},
    ): ExportResult = when (format) {
        ExportFormat.PNG -> exportPng(project, onProgress)
        ExportFormat.PDF -> exportPdf(project, onProgress)
    }

    private suspend fun exportPng(project: Project, onProgress: (Float) -> Unit): ExportResult {
        val scale = if (availableHeapBytes() < HIGH_QUALITY_HEAP_BYTES) {
            REDUCED_SUPERSAMPLE
        } else {
            DEFAULT_SUPERSAMPLE
        }
        val slideCount = project.slideCount.coerceAtLeast(1)
        val timestamp = System.currentTimeMillis()
        val pages = mutableListOf<ExportedPage>()
        for (index in 0 until slideCount) {
            val bitmap = withContext(computeDispatcher) {
                compositeSlide(project, index, scale)
            }
            try {
                val uri = withContext(ioDispatcher) {
                    savePng(bitmap, project.name, index + 1, slideCount, timestamp)
                }
                pages += ExportedPage(uri, index + 1, bitmap.width, bitmap.height)
            } finally {
                bitmap.recycle()
            }
            onProgress((index + 1).toFloat() / slideCount)
        }
        return ExportResult(
            pages = pages,
            format = ExportFormat.PNG,
            slideCount = slideCount,
            warning = if (scale < DEFAULT_SUPERSAMPLE) {
                AppError.MemoryError("high-resolution export")
            } else {
                null
            },
        )
    }

    /**
     * Renders the whole carousel to a single multi-page PDF (one page per slide), which is
     * what LinkedIn's document-carousel upload expects. Each page is drawn vector-first via the
     * shared [renderSlide] pass, then saved to app-scoped storage and exposed through a
     * [FileProvider] URI so it can be shared without any storage permission.
     */
    private suspend fun exportPdf(project: Project, onProgress: (Float) -> Unit): ExportResult {
        val slideCount = project.slideCount.coerceAtLeast(1)
        val width = Math.multiplyExact(project.canvasWidth, PDF_SUPERSAMPLE)
        val height = Math.multiplyExact(project.canvasHeight, PDF_SUPERSAMPLE)
        val timestamp = System.currentTimeMillis()
        val uri = withContext(computeDispatcher) {
            val document = PdfDocument()
            try {
                for (index in 0 until slideCount) {
                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    renderSlide(page.canvas, project, index, PDF_SUPERSAMPLE, width, height)
                    document.finishPage(page)
                    onProgress((index + 1).toFloat() / slideCount)
                }
                withContext(ioDispatcher) { savePdf(document, project.name, timestamp) }
            } finally {
                document.close()
            }
        }
        return ExportResult(
            pages = listOf(ExportedPage(uri, 1, width, height)),
            format = ExportFormat.PDF,
            slideCount = slideCount,
        )
    }

    private fun compositeSlide(project: Project, slideIndex: Int, supersample: Int): Bitmap {
        val width = Math.multiplyExact(project.canvasWidth, supersample)
        val height = Math.multiplyExact(project.canvasHeight, supersample)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        renderSlide(Canvas(output), project, slideIndex, supersample, width, height)
        return output
    }

    /** Draws one slide's background and layers onto [canvas] in the target coordinate space. */
    private fun renderSlide(
        canvas: Canvas,
        project: Project,
        slideIndex: Int,
        supersample: Int,
        width: Int,
        height: Int,
    ) {
        drawBackground(canvas, project, width, height)
        canvas.save()
        // Shift the shared coordinate space so this slide's region maps onto the page.
        canvas.translate(-(slideIndex * width).toFloat(), 0f)
        project.layers.sortedBy(Layer::zIndex).forEach { layer ->
            drawLayer(canvas, layer, supersample)
        }
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas, project: Project, width: Int, height: Int) {
        val end = project.bgColorEnd
        if (end == null) {
            canvas.drawColor(project.bgColorStart.toInt())
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    height.toFloat(),
                    project.bgColorStart.toInt(),
                    end.toInt(),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: Layer,
        supersample: Int,
    ) {
        val targetWidth = (layer.width * supersample).toInt()
        val targetHeight = (layer.height * supersample).toInt()
        if (targetWidth <= 0 || targetHeight <= 0) return
        val alpha = (layer.alpha.coerceIn(0f, 1f) * 255f).toInt()
        val cornerRadius = layer.cornerRadius * supersample
        canvas.save()
        canvas.translate(layer.x * supersample, layer.y * supersample)
        canvas.rotate(layer.rotation, targetWidth / 2f, targetHeight / 2f)
        canvas.scale(layer.scale, layer.scale, targetWidth / 2f, targetHeight / 2f)

        if (layer.hasShadow && layer.type != LayerType.TEXT) {
            drawLayerShadow(canvas, targetWidth, targetHeight, cornerRadius, supersample)
        }

        canvas.save()
        if (cornerRadius > 0f && layer.type != LayerType.TEXT) {
            val clip = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat()),
                    cornerRadius,
                    cornerRadius,
                    Path.Direction.CW,
                )
            }
            canvas.clipPath(clip)
        }
        when (layer.type) {
            LayerType.IMAGE, LayerType.STICKER -> drawImageLayer(canvas, layer, targetWidth, targetHeight, alpha)
            LayerType.TEXT -> drawTextLayer(canvas, layer, targetWidth, targetHeight, supersample, alpha)
            LayerType.SHAPE -> drawShapeLayer(canvas, layer, targetWidth, targetHeight, cornerRadius, alpha)
        }
        canvas.restore()
        canvas.restore()
    }

    private fun drawImageLayer(
        canvas: Canvas,
        layer: Layer,
        targetWidth: Int,
        targetHeight: Int,
        alpha: Int,
    ) {
        val bitmap = layer.imageUri?.let {
            decodeWorkingBitmap(it, targetWidth, targetHeight)
        } ?: return
        val cover = centerCropRect(
            width = bitmap.width,
            height = bitmap.height,
            targetAspectRatio = targetWidth.toFloat() / targetHeight,
        )
        // Apply the fractional crop within the center-cropped region.
        val source = Rect(
            (cover.left + layer.cropLeft * cover.width()).toInt(),
            (cover.top + layer.cropTop * cover.height()).toInt(),
            (cover.left + layer.cropRight * cover.width()).toInt(),
            (cover.top + layer.cropBottom * cover.height()).toInt(),
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = alpha
            colorFilter = colorFilterFor(layer)
        }
        canvas.drawBitmap(bitmap, source, Rect(0, 0, targetWidth, targetHeight), paint)
        bitmap.recycle()
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: Layer,
        targetWidth: Int,
        targetHeight: Int,
        supersample: Int,
        alpha: Int,
    ) {
        val bold = layer.fontWeight >= 700
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.textColor.toInt()
            this.alpha = alpha
            textSize = layer.textSizeSp * supersample
            typeface = FontCatalog.typeface(appContext, layer.fontFamily, bold)
            if (layer.hasShadow) {
                setShadowLayer(textSize * 0.08f, 0f, textSize * 0.04f, 0x66000000)
            }
        }
        val alignment = when (layer.textAlign) {
            TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }
        @Suppress("DEPRECATION")
        val layout = StaticLayout.Builder
            .obtain(layer.text.orEmpty(), 0, layer.text.orEmpty().length, paint, targetWidth)
            .setAlignment(alignment)
            .build()
        canvas.save()
        canvas.translate(0f, ((targetHeight - layout.height) / 2f).coerceAtLeast(0f))
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        layer: Layer,
        targetWidth: Int,
        targetHeight: Int,
        cornerRadius: Float,
        alpha: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (layer.fillColor ?: 0xFF000000L).toInt()
            this.alpha = alpha
        }
        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()
        when (layer.shapeKind) {
            ShapeKind.CIRCLE -> canvas.drawOval(RectF(0f, 0f, w, h), paint)
            ShapeKind.LINE -> {
                paint.strokeWidth = minOf(w, h) * SHAPE_LINE_FRACTION
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(0f, h / 2f, w, h / 2f, paint)
            }
            ShapeKind.ARROW -> {
                paint.strokeWidth = minOf(w, h) * SHAPE_LINE_FRACTION
                paint.strokeCap = Paint.Cap.ROUND
                val head = paint.strokeWidth * 3f
                canvas.drawLine(0f, h / 2f, w - head, h / 2f, paint)
                canvas.drawLine(w, h / 2f, w - head, h / 2f - head, paint)
                canvas.drawLine(w, h / 2f, w - head, h / 2f + head, paint)
            }
            else -> if (cornerRadius > 0f) {
                canvas.drawRoundRect(RectF(0f, 0f, w, h), cornerRadius, cornerRadius, paint)
            } else {
                canvas.drawRect(0f, 0f, w, h, paint)
            }
        }
    }

    private fun drawLayerShadow(
        canvas: Canvas,
        targetWidth: Int,
        targetHeight: Int,
        cornerRadius: Float,
        supersample: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000
            maskFilter = BlurMaskFilter(SHADOW_BLUR_DP * supersample, BlurMaskFilter.Blur.NORMAL)
        }
        val offset = SHADOW_OFFSET_DP * supersample
        canvas.drawRoundRect(
            RectF(offset, offset, targetWidth + offset, targetHeight + offset),
            cornerRadius,
            cornerRadius,
            paint,
        )
    }

    private fun colorFilterFor(layer: Layer): ColorMatrixColorFilter? {
        if (LayerColorMatrix.isIdentity(layer.brightness, layer.contrast, layer.saturation, layer.filterPreset)) {
            return null
        }
        val matrix = LayerColorMatrix.build(
            brightness = layer.brightness,
            contrast = layer.contrast,
            saturation = layer.saturation,
            preset = layer.filterPreset,
        )
        return ColorMatrixColorFilter(ColorMatrix(matrix))
    }

    private fun decodeWorkingBitmap(uriValue: String, width: Int, height: Int): Bitmap? {
        val uri = Uri.parse(uriValue)
        if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
            return decodeResourceDrawable(uri, width, height)
        }
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

    /** Renders a (possibly vector) resource drawable — e.g. a bundled sticker — to a bitmap. */
    private fun decodeResourceDrawable(uri: Uri, width: Int, height: Int): Bitmap? {
        val resId = uri.lastPathSegment?.toIntOrNull() ?: return null
        val drawable = runCatching {
            appContext.resources.getDrawable(resId, appContext.theme)
        }.getOrNull() ?: return null
        val output = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        drawable.setBounds(0, 0, output.width, output.height)
        drawable.draw(canvas)
        return output
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

    private fun savePng(
        bitmap: Bitmap,
        projectName: String,
        page: Int,
        totalPages: Int,
        timestamp: Long,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, safeFileName(projectName, page, timestamp))
            put(MediaStore.Images.Media.MIME_TYPE, PNG_MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$EXPORT_DIRECTORY",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore did not create an export destination")
        try {
            resolver.openOutputStream(uri, "w")?.use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) {
                    "PNG encoder failed"
                }
            } ?: throw IOException("MediaStore output stream was unavailable")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun savePdf(document: PdfDocument, projectName: String, timestamp: Long): Uri {
        val base = projectName.replace(UNSAFE_FILE_NAME, "_").trim('_').ifBlank { "carousel" }
        val dir = File(appContext.getExternalFilesDir(null), EXPORT_DIRECTORY).apply { mkdirs() }
        val file = File(dir, "${base}_$timestamp.pdf")
        file.outputStream().use { document.writeTo(it) }
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    private fun safeFileName(projectName: String, page: Int, timestamp: Long): String {
        val base = projectName.replace(UNSAFE_FILE_NAME, "_").trim('_').ifBlank { "carousel" }
        val paddedPage = page.toString().padStart(2, '0')
        return "${base}_${timestamp}_$paddedPage.png"
    }

    private fun availableHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }

    private fun centerCropRect(width: Int, height: Int, targetAspectRatio: Float): Rect {
        val sourceAspectRatio = width.toFloat() / height
        return if (sourceAspectRatio > targetAspectRatio) {
            val cropWidth = (height * targetAspectRatio).toInt()
            val left = (width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, height)
        } else {
            val cropHeight = (width / targetAspectRatio).toInt()
            val top = (height - cropHeight) / 2
            Rect(0, top, width, top + cropHeight)
        }
    }

    private companion object {
        const val DEFAULT_SUPERSAMPLE = 3
        const val REDUCED_SUPERSAMPLE = 2
        const val PDF_SUPERSAMPLE = 2
        const val HIGH_QUALITY_HEAP_BYTES = 2L * 1024L * 1024L * 1024L
        const val SHAPE_LINE_FRACTION = 0.12f
        const val SHADOW_BLUR_DP = 8f
        const val SHADOW_OFFSET_DP = 4f
        const val PNG_QUALITY = 100
        const val PNG_MIME_TYPE = "image/png"
        const val EXPORT_DIRECTORY = "CarouselForge"
        val UNSAFE_FILE_NAME = Regex("[^A-Za-z0-9_-]+")
    }
}
