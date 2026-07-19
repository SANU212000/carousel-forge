package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun EditorTopBar(
    gridEnabled: Boolean,
    safeZoneVisible: Boolean,
    onBack: () -> Unit,
    onAddImage: () -> Unit,
    onToggleSafeZone: () -> Unit,
    onToggleGrid: () -> Unit,
    onExport: () -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        EditorAction(
            onClick = onBack,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") },
        )
        EditorAction(
            onClick = onAddImage,
            icon = { Icon(Icons.Default.AddPhotoAlternate, "Add image") },
        )
        EditorAction(
            onClick = onToggleSafeZone,
            icon = {
                Icon(
                    if (safeZoneVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    "Toggle safe zone",
                )
            },
        )
        EditorAction(
            onClick = onToggleGrid,
            icon = {
                Icon(
                    if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                    "Toggle grid snapping",
                )
            },
        )
        FilledIconButton(onClick = onExport) {
            Icon(Icons.Default.IosShare, "Export")
        }
    }

    if (vertical) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(AppTheme.spacing.huge)
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = AppTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AppTheme.spacing.xs, vertical = AppTheme.spacing.xxs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun EditorAction(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, content = icon)
}
