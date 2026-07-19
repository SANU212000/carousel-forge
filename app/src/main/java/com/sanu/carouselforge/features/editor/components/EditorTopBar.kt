package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Tune
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
            EditorAction(onBack, Icons.AutoMirrored.Filled.ArrowBack, "Back")
            EditorAction(onAddImage, Icons.Default.AddPhotoAlternate, "Add image")
            EditorAction(
                onToggleSafeZone,
                if (safeZoneVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                "Toggle safe zone",
            )
            EditorAction(
                onToggleGrid,
                if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                "Toggle grid snapping",
            )
            FilledIconButton(onClick = onExport) {
                Icon(Icons.Default.AutoAwesome, "Export")
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        text = "CarouselForge",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Button(
                        onClick = onExport,
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
                    Spacer(Modifier.width(AppTheme.spacing.xs))
                    Box(
                        modifier = Modifier
                            .size(AppTheme.spacing.xl)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(
                                AppTheme.spacing.selectionStroke,
                                MaterialTheme.colorScheme.outline,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("●", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        .shadow(
                            AppTheme.spacing.xs,
                            RoundedCornerShape(AppTheme.spacing.sm),
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(AppTheme.spacing.sm),
                        )
                        .border(
                            AppTheme.spacing.selectionStroke,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            RoundedCornerShape(AppTheme.spacing.sm),
                        )
                        .padding(
                            horizontal = AppTheme.spacing.xs,
                            vertical = AppTheme.spacing.xxs,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrayAction(Icons.Default.Crop, "Crop", onAddImage)
                    TrayAction(
                        Icons.Default.AddPhotoAlternate,
                        "Add image",
                        onAddImage,
                        selected = true,
                    )
                    TrayAction(
                        if (gridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        "Grid snapping",
                        onToggleGrid,
                    )
                    TrayAction(Icons.Default.Tune, "Adjust", onToggleGrid)
                    TrayAction(Icons.Default.Title, "Text", onAddImage)
                    TrayAction(Icons.Default.Layers, "Layers", onToggleSafeZone)
                    TrayAction(
                        if (safeZoneVisible) Icons.Default.Visibility else Icons.Default.AutoAwesome,
                        "Safe zone",
                        onToggleSafeZone,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorAction(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
) {
    IconButton(onClick = onClick) {
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
