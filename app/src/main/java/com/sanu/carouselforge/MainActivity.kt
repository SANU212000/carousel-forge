package com.sanu.carouselforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sanu.carouselforge.core.theme.CarouselForgeTheme
import com.sanu.carouselforge.navigation.CarouselForgeNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarouselForgeTheme {
                CarouselForgeNavGraph()
            }
        }
    }
}