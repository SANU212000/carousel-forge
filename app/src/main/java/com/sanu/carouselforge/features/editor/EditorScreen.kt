package com.sanu.carouselforge.features.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val addImagePicker = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.importImage(context, it) }
        },
    )
    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.importReplacementImage(context, it) }
        },
    )
    val openImagePicker = {
        addImagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

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
            onReplaceImage = {
                replaceImagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            },
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
                onDeselectLayer = viewModel::deselectLayer,
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
                    state.notice?.let { notice ->
                        EditorNoticeBanner(
                            message = notice.userMessage(),
                            onDismiss = viewModel::dismissNotice,
                        )
                    }
                    canvas(Modifier.weight(1f))
                    controls()
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                topBar(false)
                state.notice?.let { notice ->
                    EditorNoticeBanner(
                        message = notice.userMessage(),
                        onDismiss = viewModel::dismissNotice,
                    )
                }
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
private fun EditorNoticeBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xs),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(AppTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
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
