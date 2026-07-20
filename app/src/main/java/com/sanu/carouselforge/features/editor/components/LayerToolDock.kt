package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.features.editor.render.LayerType

class LayerToolActions(
    val onEditText: () -> Unit,
    val onStyle: () -> Unit,
    val onReplace: () -> Unit,
    val onCrop: () -> Unit,
    val onDuplicate: () -> Unit,
    val onCopyToAllSlides: () -> Unit,
    val onDelete: () -> Unit,
    val onFit: () -> Unit,
    val onBackward: () -> Unit,
    val onForward: () -> Unit,
)

@Composable
fun LayerToolDock(
    selectedType: LayerType?,
    slideCount: Int,
    actions: LayerToolActions,
    modifier: Modifier = Modifier,
) {
    if (selectedType == null) return
    val isImage = selectedType == LayerType.IMAGE || selectedType == LayerType.STICKER
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xxs)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(AppTheme.spacing.sm),
            )
            .border(
                AppTheme.spacing.selectionStroke,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                RoundedCornerShape(AppTheme.spacing.sm),
            )
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectedType == LayerType.TEXT) {
            ToolAction("Edit", Icons.Default.Edit, actions.onEditText)
        }
        ToolAction("Style", Icons.Default.Tune, actions.onStyle)
        if (isImage) {
            ToolAction("Replace", Icons.Default.FindReplace, actions.onReplace)
            ToolAction("Crop", Icons.Default.Crop, actions.onCrop)
        }
        ToolAction("Duplicate", Icons.Default.ContentCopy, actions.onDuplicate)
        if (slideCount > 1) {
            ToolAction("To all", Icons.Default.DynamicFeed, actions.onCopyToAllSlides)
        }
        ToolAction("Fit", Icons.Default.FitScreen, actions.onFit)
        ToolAction("Backward", Icons.Default.FlipToBack, actions.onBackward)
        ToolAction("Forward", Icons.Default.FlipToFront, actions.onForward)
        ToolAction("Delete", Icons.Default.DeleteOutline, actions.onDelete)
    }
}

@Composable
private fun ToolAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(AppTheme.spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}
