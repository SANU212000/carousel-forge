package com.sanu.carouselforge.core.error

import android.util.Log

fun interface ErrorObserver {
    fun record(error: AppError)
}

object LogcatErrorObserver : ErrorObserver {
    override fun record(error: AppError) {
        when (error) {
            is AppError.StorageError -> Log.e(TAG, "Storage operation failed", error.cause)
            else -> Log.e(TAG, error.toString())
        }
    }

    private const val TAG = "CarouselForge"
}
