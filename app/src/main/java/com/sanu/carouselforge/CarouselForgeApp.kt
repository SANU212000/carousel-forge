package com.sanu.carouselforge

import android.app.Application
import com.sanu.carouselforge.core.di.AppModule

class CarouselForgeApp : Application() {
    val appModule: AppModule by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppModule(this)
    }
}
