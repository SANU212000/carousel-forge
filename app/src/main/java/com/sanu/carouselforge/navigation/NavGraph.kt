package com.sanu.carouselforge.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.sanu.carouselforge.features.export.ExportEngine
import com.sanu.carouselforge.features.export.ExportScreen
import com.sanu.carouselforge.features.export.ExportViewModel
import com.sanu.carouselforge.features.gallery.GalleryScreen
import com.sanu.carouselforge.features.gallery.GalleryViewModel

@Composable
fun CarouselForgeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as CarouselForgeApp
    val repository = app.appModule.projectRepository
    val exportEngine = remember(context) { ExportEngine(context) }

    NavHost(
        navController = navController,
        startDestination = GalleryRoute,
        modifier = modifier,
    ) {
        composable<GalleryRoute> {
            val factory = remember(repository) {
                viewModelFactory { initializer { GalleryViewModel(repository) } }
            }
            val viewModel: GalleryViewModel = viewModel(factory = factory)
            GalleryScreen(
                viewModel = viewModel,
                onOpenProject = { navController.navigate(EditorRoute(it)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<EditorRoute> { entry ->
            val route = entry.toRoute<EditorRoute>()
            val factory = remember(route.projectId, repository) {
                viewModelFactory {
                    initializer { EditorViewModel(route.projectId, repository) }
                }
            }
            val viewModel: EditorViewModel = viewModel(
                key = route.projectId,
                factory = factory,
            )
            EditorScreen(
                viewModel = viewModel,
                onBack = navController::popBackStack,
                onExport = { navController.navigate(ExportRoute(route.projectId)) },
                onShowSafeZone = {
                    navController.navigate(SafeZonePreviewRoute(route.projectId))
                },
            )
        }
        dialog<SafeZonePreviewRoute> {
            AlertDialog(
                onDismissRequest = navController::popBackStack,
                title = { Text("Safe-zone preview") },
                text = {
                    Text("Keep important content outside the marked profile-avatar area.")
                },
                confirmButton = {
                    TextButton(onClick = navController::popBackStack) { Text("Done") }
                },
            )
        }
        composable<ExportRoute> { entry ->
            val route = entry.toRoute<ExportRoute>()
            val factory = remember(route.projectId, repository, exportEngine) {
                viewModelFactory {
                    initializer {
                        ExportViewModel(route.projectId, repository, exportEngine)
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
            SettingsScreen(onBack = navController::popBackStack)
        }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(
            AppTheme.spacing.md,
            Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Settings")
        Text("Grid snapping is enabled by default for new editor sessions.")
        Button(onClick = onBack) { Text("Back") }
    }
}
