package com.sanu.carouselforge.features.editor

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.components.CanvasViewport
import com.sanu.carouselforge.features.editor.components.EditorTopBar
import com.sanu.carouselforge.features.editor.components.LayerToolDock
import com.sanu.carouselforge.features.editor.components.SplitControls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    persistReadPermission(context, it)
                    viewModel.addImage(it.toString(), readImageAspectRatio(context, it))
                }
            }
        },
    )
    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    persistReadPermission(context, it)
                    viewModel.replaceSelectedImage(
                        it.toString(),
                        readImageAspectRatio(context, it),
                    )
                }
            }
        },
    )
    val openImagePicker = { addImagePicker.launch(arrayOf("image/*")) }

    when (val current = state) {
        EditorState.Loading -> LoadingContent(modifier)
        is EditorState.Exporting -> LoadingContent(modifier, current.progress)
        is EditorState.Error -> ErrorContent(
            message = current.error.userMessage(),
            onBack = onBack,
            modifier = modifier,
        )
        is EditorState.Editing -> ResponsiveEditor(
            state = current,
            onBack = onBack,
            onExport = onExport,
            onAddImage = openImagePicker,
            onReplaceImage = { replaceImagePicker.launch(arrayOf("image/*")) },
            onToggleSafeZone = {
                viewModel.setSafeZoneVisible(!current.safeZoneVisible)
            },
            onToggleGrid = {
                viewModel.setGridSnapEnabled(!current.gridSnapEnabled)
            },
            onSplitVisibleChanged = viewModel::setSplitGuidesVisible,
            onSplitCountChanged = viewModel::setSplitCount,
            onSelectLayer = viewModel::selectLayer,
            onTransform = viewModel::transformLayer,
            onGestureEnd = viewModel::finishGesture,
            onDelete = { current.selectedLayerId?.let(viewModel::removeLayer) },
            onFit = viewModel::fitSelectedLayer,
            onBackward = { viewModel.moveSelectedLayer(forward = false) },
            onForward = { viewModel.moveSelectedLayer(forward = true) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ResponsiveEditor(
    state: EditorState.Editing,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onAddImage: () -> Unit,
    onReplaceImage: () -> Unit,
    onToggleSafeZone: () -> Unit,
    onToggleGrid: () -> Unit,
    onSplitVisibleChanged: (Boolean) -> Unit,
    onSplitCountChanged: (Int) -> Unit,
    onSelectLayer: (String) -> Unit,
    onTransform: (String, com.sanu.carouselforge.features.editor.render.TransformDelta) -> Unit,
    onGestureEnd: (String, Float, Float) -> Unit,
    onDelete: () -> Unit,
    onFit: () -> Unit,
    onBackward: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val landscape = maxWidth > maxHeight &&
            maxHeight < AppTheme.spacing.shortHeightBreakpoint
        val canvas: @Composable (Modifier) -> Unit = { canvasModifier ->
            CanvasViewport(
                state = state,
                onSelectLayer = onSelectLayer,
                onTransform = onTransform,
                onGestureEnd = onGestureEnd,
                onAddImage = onAddImage,
                modifier = canvasModifier,
            )
        }
        val topBar: @Composable (Boolean) -> Unit = { vertical ->
            EditorTopBar(
                gridEnabled = state.gridSnapEnabled,
                safeZoneVisible = state.safeZoneVisible,
                onBack = onBack,
                onAddImage = onAddImage,
                onToggleSafeZone = onToggleSafeZone,
                onToggleGrid = onToggleGrid,
                onExport = onExport,
                vertical = vertical,
            )
        }
        val controls: @Composable () -> Unit = {
            SplitControls(
                visible = state.splitGuidesVisible,
                splitCount = state.splitCount,
                onVisibleChanged = onSplitVisibleChanged,
                onCountChanged = onSplitCountChanged,
            )
            LayerToolDock(
                hasSelection = state.selectedLayerId != null,
                onReplace = onReplaceImage,
                onDelete = onDelete,
                onFit = onFit,
                onBackward = onBackward,
                onForward = onForward,
            )
        }

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                topBar(true)
                Column(Modifier.weight(1f)) {
                    canvas(Modifier.weight(1f))
                    controls()
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                topBar(false)
                canvas(Modifier.weight(1f))
                controls()
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier,
    progress: Float? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (progress == null) {
            CircularProgressIndicator()
        } else {
            CircularProgressIndicator(progress = { progress })
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onBack) { Text("Back to projects") }
    }
}

private suspend fun persistReadPermission(context: Context, uri: Uri) {
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}

private suspend fun readImageAspectRatio(context: Context, uri: Uri): Float =
    withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext 1f
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            runCatching {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val rotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        if (rotated) {
            options.outHeight.toFloat() / options.outWidth
        } else {
            options.outWidth.toFloat() / options.outHeight
        }
    }
