package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.render.LayerModel
import com.sanu.carouselforge.features.editor.render.LayerType

/**
 * Reorderable z-order list. Layers are shown top-to-bottom (front-most first). The up/down
 * controls rebuild the front-to-back id order and hand it to [onReorder].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersPanel(
    layers: List<LayerModel>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    val topToBottom = layers.sortedByDescending(LayerModel::zIndex)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Text("Layers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            if (topToBottom.isEmpty()) {
                Text(
                    "No layers yet. Add an image, text or element.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = AppTheme.spacing.huge * 5),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                items(topToBottom, key = LayerModel::id) { layer ->
                    val index = topToBottom.indexOf(layer)
                    LayerRow(
                        layer = layer,
                        selected = layer.id == selectedId,
                        canMoveUp = index > 0,
                        canMoveDown = index < topToBottom.lastIndex,
                        onSelect = { onSelect(layer.id) },
                        onMoveUp = { onReorder(moved(topToBottom, index, index - 1)) },
                        onMoveDown = { onReorder(moved(topToBottom, index, index + 1)) },
                    )
                }
            }
        }
    }
}

private fun moved(list: List<LayerModel>, from: Int, to: Int): List<String> {
    val ids = list.map(LayerModel::id).toMutableList()
    val item = ids.removeAt(from)
    ids.add(to, item)
    return ids
}

@Composable
private fun LayerRow(
    layer: LayerModel,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.spacing.xs))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            iconFor(layer.type),
            contentDescription = null,
            modifier = Modifier.size(AppTheme.spacing.lg),
        )
        Text(
            text = labelFor(layer),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppTheme.spacing.sm),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move forward")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move backward")
        }
    }
}

private fun iconFor(type: LayerType): ImageVector = when (type) {
    LayerType.TEXT -> Icons.Default.Title
    LayerType.SHAPE -> Icons.Default.Category
    else -> Icons.Default.Image
}

private fun labelFor(layer: LayerModel): String = when (layer.type) {
    LayerType.TEXT -> layer.text?.takeIf { it.isNotBlank() } ?: "Text"
    LayerType.SHAPE -> layer.shapeKind?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Shape"
    LayerType.STICKER -> "Sticker"
    LayerType.IMAGE -> "Image"
}
