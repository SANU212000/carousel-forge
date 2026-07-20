package com.sanu.carouselforge.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sanu.carouselforge.CarouselForgeApp
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.EditorScreen
import com.sanu.carouselforge.features.editor.EditorViewModel
import com.sanu.carouselforge.features.editor.overlay.SafeZoneOverlay
import com.sanu.carouselforge.features.export.ExportEngine
import com.sanu.carouselforge.features.export.ExportScreen
import com.sanu.carouselforge.features.export.ExportViewModel
import com.sanu.carouselforge.features.gallery.GalleryScreen
import com.sanu.carouselforge.features.gallery.GalleryViewModel
import com.sanu.carouselforge.features.settings.SettingsScreen
import com.sanu.carouselforge.features.settings.SettingsViewModel

@Composable
fun CarouselForgeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as CarouselForgeApp
    val repository = app.appModule.projectRepository
    val fileStore = app.appModule.projectFileStore
    val userPreferences = app.appModule.userPreferences
    val exportEngine = remember(context) { ExportEngine(context) }

    NavHost(
        navController = navController,
        startDestination = GalleryRoute,
        modifier = modifier,
    ) {
        composable<GalleryRoute> {
            val factory = remember(repository, userPreferences) {
                viewModelFactory {
                    initializer { GalleryViewModel(repository, userPreferences) }
                }
            }
            val viewModel: GalleryViewModel = viewModel(factory = factory)
            GalleryScreen(
                viewModel = viewModel,
                onOpenProject = { projectId ->
                    navController.navigate(EditorRoute(projectId)) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = {
                    navController.navigate(SettingsRoute) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            val factory = remember(route.projectId, repository, fileStore, userPreferences) {
                viewModelFactory {
                    initializer {
                        EditorViewModel(route.projectId, repository, fileStore, userPreferences)
                    }
                }
            }
            val viewModel: EditorViewModel = viewModel(
                key = route.projectId,
                factory = factory,
            )
            EditorScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
                onExport = {
                    navController.navigate(ExportRoute(route.projectId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        dialog<SafeZonePreviewRoute> {
            AlertDialog(
                onDismissRequest = navController::popBackStack,
                title = { Text("Safe-zone preview") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        Text("Keep important content outside the marked profile-avatar area.")
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            SafeZoneOverlay(visible = true, slideIndex = 0)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = navController::popBackStack) { Text("Done") }
                },
            )
        }
        composable<ExportRoute> { entry ->
            val route = entry.toRoute<ExportRoute>()
            val factory = remember(route.projectId, repository, exportEngine, userPreferences) {
                viewModelFactory {
                    initializer {
                        ExportViewModel(route.projectId, repository, exportEngine, userPreferences)
                    }
                }
            }
            val viewModel: ExportViewModel = viewModel(
                key = "export-${route.projectId}",
                factory = factory,
            )
            ExportScreen(viewModel = viewModel, onBack = navController::popBackStack)
        }
        composable<SettingsRoute> {
            val factory = remember(userPreferences) {
                viewModelFactory { initializer { SettingsViewModel(userPreferences) } }
            }
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(viewModel = viewModel, onBack = navController::popBackStack)
        }
    }
}
