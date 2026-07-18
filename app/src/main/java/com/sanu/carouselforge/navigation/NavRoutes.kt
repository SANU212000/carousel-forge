package com.sanu.carouselforge.navigation

import kotlinx.serialization.Serializable

@Serializable
data object GalleryRoute

@Serializable
data class EditorRoute(val projectId: String)

@Serializable
data class SafeZonePreviewRoute(val projectId: String)

@Serializable
data class ExportRoute(val projectId: String)

@Serializable
data object SettingsRoute
