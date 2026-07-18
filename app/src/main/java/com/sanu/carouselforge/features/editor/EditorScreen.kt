package com.sanu.carouselforge.features.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.overlay.SafeZoneOverlay
import com.sanu.carouselforge.features.editor.render.EditorCanvas

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onShowSafeZone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                viewModel.addImage(it.toString())
            }
        },
    )

    when (val current = state) {
        EditorState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is EditorState.Exporting -> Box(
            modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(progress = { current.progress })
        }

        is EditorState.Error -> ErrorContent(
            message = current.error.userMessage(),
            onBack = onBack,
            modifier = modifier,
        )

        is EditorState.Editing -> Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                EditorToolbar(
                    gridEnabled = current.gridSnapEnabled,
                    onGridChanged = viewModel::setGridSnapEnabled,
                    onBack = onBack,
                    onAddImage = { imagePicker.launch(arrayOf("image/*")) },
                    onSafeZone = onShowSafeZone,
                    onExport = onExport,
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EditorCanvas(
                    layers = current.layers,
                    selectedLayerId = current.selectedLayerId,
                    onSelectLayer = viewModel::selectLayer,
                    onTransform = viewModel::transformLayer,
                    onGestureEnd = viewModel::finishGesture,
                )
                SafeZoneOverlay(visible = false, slideIndex = 0)
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    gridEnabled: Boolean,
    onGridChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onAddImage: () -> Unit,
    onSafeZone: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        TextButton(onClick = onAddImage) { Text("Add image") }
        TextButton(onClick = onSafeZone) { Text("Safe zone") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grid", style = MaterialTheme.typography.labelLarge)
            Switch(checked = gridEnabled, onCheckedChange = onGridChanged)
        }
        Button(onClick = onExport) { Text("Export") }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onBack) { Text("Back to projects") }
    }
}
