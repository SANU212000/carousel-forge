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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.model.CanvasPreset
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.components.BackgroundSheet
import com.sanu.carouselforge.features.editor.components.CanvasRatioSheet
import com.sanu.carouselforge.features.editor.components.CanvasViewport
import com.sanu.carouselforge.features.editor.components.CropSheet
import com.sanu.carouselforge.features.editor.components.EditorTopBar
import com.sanu.carouselforge.features.editor.components.EditorTopBarActions
import com.sanu.carouselforge.features.editor.components.ElementsPicker
import com.sanu.carouselforge.features.editor.components.LayerToolActions
import com.sanu.carouselforge.features.editor.components.LayerToolDock
import com.sanu.carouselforge.features.editor.components.LayersPanel
import com.sanu.carouselforge.features.editor.components.SlideCounter
import com.sanu.carouselforge.features.editor.components.StyleSheet
import com.sanu.carouselforge.features.editor.components.TextEditSheet
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType
import com.sanu.carouselforge.features.editor.render.TransformDelta
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
                    viewModel.replaceSelectedImage(it.toString(), readImageAspectRatio(context, it))
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
            viewModel = viewModel,
            state = current,
            onBack = onBack,
            onExport = { viewModel.flushPendingSave(onExport) },
            onAddImage = openImagePicker,
            onReplaceImage = { replaceImagePicker.launch(arrayOf("image/*")) },
            modifier = modifier,
        )
    }
}

@Composable
private fun ResponsiveEditor(
    viewModel: EditorViewModel,
    state: EditorState.Editing,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onAddImage: () -> Unit,
    onReplaceImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRatioSheet by remember { mutableStateOf(false) }
    var showBackgroundSheet by remember { mutableStateOf(false) }
    var showElements by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    var showStyle by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showCrop by remember { mutableStateOf(false) }

    val selected = state.selectedLayer
    val styleUpdate: (Boolean, (LayerModel) -> LayerModel) -> Unit = { push, transform ->
        viewModel.updateSelectedLayer(push, transform)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val landscape = maxWidth > maxHeight &&
            maxHeight < AppTheme.spacing.shortHeightBreakpoint
        val topBarActions = EditorTopBarActions(
            onBack = onBack,
            onAddImage = onAddImage,
            onAddText = {
                viewModel.addText()
                showText = true
            },
            onOpenElements = { showElements = true },
            onOpenBackground = { showBackgroundSheet = true },
            onOpenRatio = { showRatioSheet = true },
            onToggleGrid = { viewModel.setGridSnapEnabled(!state.gridSnapEnabled) },
            onToggleSafeZone = { viewModel.setSafeZoneVisible(!state.safeZoneVisible) },
            onOpenLayers = { showLayers = true },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onExport = onExport,
        )
        val canvas: @Composable (Modifier) -> Unit = { canvasModifier ->
            CanvasViewport(
                state = state,
                onSelectLayer = viewModel::selectLayer,
                onTransform = viewModel::transformLayer,
                onGestureEnd = viewModel::finishGesture,
                onAddImage = onAddImage,
                onEditText = { showText = true },
                modifier = canvasModifier,
            )
        }
        val topBar: @Composable (Boolean) -> Unit = { vertical ->
            EditorTopBar(
                gridEnabled = state.gridSnapEnabled,
                safeZoneVisible = state.safeZoneVisible,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                actions = topBarActions,
                vertical = vertical,
            )
        }
        val controls: @Composable () -> Unit = {
            SlideCounter(
                slideCount = state.slideCount,
                onCountChanged = viewModel::setSlideCount,
            )
            LayerToolDock(
                selectedType = selected?.type,
                slideCount = state.slideCount,
                actions = LayerToolActions(
                    onEditText = { showText = true },
                    onStyle = { showStyle = true },
                    onReplace = onReplaceImage,
                    onCrop = { showCrop = true },
                    onDuplicate = viewModel::duplicateSelectedLayer,
                    onCopyToAllSlides = viewModel::copySelectedToAllSlides,
                    onDelete = { state.selectedLayerId?.let(viewModel::removeLayer) },
                    onFit = viewModel::fitSelectedLayer,
                    onBackward = { viewModel.moveSelectedLayer(forward = false) },
                    onForward = { viewModel.moveSelectedLayer(forward = true) },
                ),
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

        if (showRatioSheet) {
            CanvasRatioSheet(
                currentWidth = state.canvasWidth,
                currentHeight = state.canvasHeight,
                onSelect = viewModel::setRatio,
                onDismiss = { showRatioSheet = false },
            )
        }
        if (showBackgroundSheet) {
            BackgroundSheet(
                onSelect = { viewModel.setBackground(it.start, it.end) },
                onDismiss = { showBackgroundSheet = false },
            )
        }
        if (showElements) {
            ElementsPicker(
                onAddShape = viewModel::addShape,
                onAddSticker = { viewModel.addSticker(it) },
                onDismiss = { showElements = false },
            )
        }
        if (showLayers) {
            LayersPanel(
                layers = state.layers,
                selectedId = state.selectedLayerId,
                onSelect = viewModel::selectLayer,
                onReorder = viewModel::reorderLayers,
                onDismiss = { showLayers = false },
            )
        }
        if (showText && selected?.type == LayerType.TEXT) {
            TextEditSheet(
                layer = selected,
                canvasWidth = state.canvasWidth,
                onUpdate = styleUpdate,
                onDismiss = { showText = false },
            )
        }
        if (showStyle && selected != null) {
            StyleSheet(
                layer = selected,
                canvasWidth = state.canvasWidth,
                onUpdate = styleUpdate,
                onDismiss = { showStyle = false },
            )
        }
        if (showCrop && (selected?.type == LayerType.IMAGE || selected?.type == LayerType.STICKER)) {
            CropSheet(
                layer = selected,
                onUpdate = styleUpdate,
                onDismiss = { showCrop = false },
            )
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
