package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.sanu.carouselforge.core.theme.AppTheme

class EditorTopBarActions(
    val onBack: () -> Unit,
    val onAddImage: () -> Unit,
    val onAddText: () -> Unit,
    val onOpenElements: () -> Unit,
    val onOpenBackground: () -> Unit,
    val onOpenRatio: () -> Unit,
    val onToggleGrid: () -> Unit,
    val onToggleSafeZone: () -> Unit,
    val onOpenLayers: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onExport: () -> Unit,
)

@Composable
fun EditorTopBar(
    gridEnabled: Boolean,
    safeZoneVisible: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: EditorTopBarActions,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    if (vertical) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(AppTheme.spacing.huge)
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = AppTheme.spacing.xs)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconAction(actions.onBack, Icons.AutoMirrored.Filled.ArrowBack, "Back")
            IconAction(actions.onAddImage, Icons.Default.AddPhotoAlternate, "Add image")
            IconAction(actions.onAddText, Icons.Default.Title, "Add text")
            IconAction(actions.onOpenElements, Icons.Default.Category, "Elements")
            IconAction(actions.onOpenBackground, Icons.Default.FormatColorFill, "Background")
            IconAction(actions.onOpenRatio, Icons.Default.AspectRatio, "Canvas ratio")
            IconAction(actions.onOpenLayers, Icons.Default.Layers, "Layers")
            IconAction(
                actions.onToggleGrid,
                if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                "Toggle grid snapping",
            )
            IconAction(
                actions.onToggleSafeZone,
                if (safeZoneVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                "Toggle safe zone",
            )
            IconAction(actions.onUndo, Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo)
            IconAction(actions.onRedo, Icons.AutoMirrored.Filled.Redo, "Redo", enabled = canRedo)
            FilledIconButton(onClick = actions.onExport) {
                Icon(Icons.Default.AutoAwesome, "Export")
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = AppTheme.spacing.xxs,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.spacing.huge)
                        .padding(horizontal = AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        text = "CarouselForge",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    IconButton(onClick = actions.onUndo, enabled = canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                    IconButton(onClick = actions.onRedo, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                    }
                    Button(
                        onClick = actions.onExport,
                        shape = RoundedCornerShape(AppTheme.spacing.xxs),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = AppTheme.spacing.md,
                        ),
                    ) {
                        Text("EXPORT", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier
                        .shadow(AppTheme.spacing.xs, RoundedCornerShape(AppTheme.spacing.sm))
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(AppTheme.spacing.sm),
                        )
                        .border(
                            AppTheme.spacing.selectionStroke,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            RoundedCornerShape(AppTheme.spacing.sm),
                        )
                        .horizontalScroll(rememberScrollState())
                        .padding(
                            horizontal = AppTheme.spacing.xs,
                            vertical = AppTheme.spacing.xxs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrayAction(Icons.Default.AddPhotoAlternate, "Add image", actions.onAddImage)
                    TrayAction(Icons.Default.Title, "Text", actions.onAddText)
                    TrayAction(Icons.Default.Category, "Elements", actions.onOpenElements)
                    TrayAction(Icons.Default.FormatColorFill, "Background", actions.onOpenBackground)
                    TrayAction(Icons.Default.AspectRatio, "Ratio", actions.onOpenRatio)
                    TrayAction(Icons.Default.Layers, "Layers", actions.onOpenLayers)
                    TrayAction(
                        if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        "Grid snapping",
                        actions.onToggleGrid,
                        selected = gridEnabled,
                    )
                    TrayAction(
                        if (safeZoneVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        "Safe zone",
                        actions.onToggleSafeZone,
                        selected = safeZoneVisible,
                    )
                }
            }
        }
    }
}

@Composable
private fun IconAction(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun TrayAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = if (selected) {
            Modifier.background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(AppTheme.spacing.xxs),
            )
        } else {
            Modifier
        },
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
