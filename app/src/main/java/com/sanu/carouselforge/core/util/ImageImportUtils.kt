package com.sanu.carouselforge.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

object ImageImportUtils {
    /**
     * Copies a picker/content URI into [dest], trying stream, file-descriptor, then
     * ImageDecoder fallbacks so gallery providers that reject [openInputStream] still work.
     */
    fun copyUriToFile(context: Context, uri: Uri, dest: File) {
        val resolver = context.contentResolver
        try {
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                return
            }
        } catch (_: Exception) {
            // Fall through to the next strategy.
        }

        try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                return
            }
        } catch (_: Exception) {
            // Fall through to the next strategy.
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri))
            try {
                dest.outputStream().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) {
                        error("Could not save image")
                    }
                }
            } finally {
                bitmap.recycle()
            }
            return
        }

        throw IOException("Could not open image")
    }

    fun readAspectRatio(file: File): Float {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            error("Could not read image dimensions")
        }
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        return if (rotated) {
            options.outHeight.toFloat() / options.outWidth
        } else {
            options.outWidth.toFloat() / options.outHeight
        }
    }
}
