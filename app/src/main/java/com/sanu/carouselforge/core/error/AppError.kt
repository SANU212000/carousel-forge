package com.sanu.carouselforge.core.error

sealed class AppError {
    data class ImageDecodeError(val uri: String, val reason: String) : AppError()
    data class MemoryError(val duringOperation: String) : AppError()
    data class StorageError(val cause: Throwable) : AppError()
    data class ExportError(val reason: String) : AppError()
    data class PermissionError(
        val permission: String,
        val permanentlyDenied: Boolean,
    ) : AppError()
    data class ValidationError(val message: String) : AppError()
    data object UnknownError : AppError()
}

fun AppError.userMessage(): String = when (this) {
    is AppError.ImageDecodeError -> "This image could not be opened: $reason"
    is AppError.MemoryError -> "Export quality was reduced to keep the app responsive."
    is AppError.StorageError -> "The project could not be saved. Check available storage."
    is AppError.ExportError -> "Export failed: $reason"
    is AppError.PermissionError -> "Photo access is required to continue."
    is AppError.ValidationError -> message
    AppError.UnknownError -> "Something unexpected happened. Please try again."
}
