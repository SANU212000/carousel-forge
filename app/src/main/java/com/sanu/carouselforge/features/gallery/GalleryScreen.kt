package com.sanu.carouselforge.features.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CarouselForge", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
        Button(onClick = { viewModel.createProject(onOpenProject) }) {
            Text("New carousel")
        }
        when (val current = state) {
            GalleryState.Loading -> CircularProgressIndicator()
            is GalleryState.Error -> {
                Text(current.error.userMessage(), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::refresh) { Text("Retry") }
            }
            is GalleryState.Content -> {
                if (current.projects.isEmpty()) {
                    Text("Create a carousel to start designing.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                        items(current.projects, key = { it.id }) { project ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenProject(project.id) }
                                    .padding(AppTheme.spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(project.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${project.layerCount} layers",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                TextButton(onClick = { viewModel.deleteProject(project.id) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
