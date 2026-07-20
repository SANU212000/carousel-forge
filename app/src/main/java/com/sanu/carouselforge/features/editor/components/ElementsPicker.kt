package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.HorizontalRule
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.data.repository.ShapeKind

private const val DEFAULT_SHAPE_COLOR = 0xFF3B82F6L

private data class ShapeOption(val kind: ShapeKind, val label: String, val icon: ImageVector)

private val SHAPE_OPTIONS = listOf(
    ShapeOption(ShapeKind.RECT, "Rectangle", Icons.Default.CropSquare),
    ShapeOption(ShapeKind.CIRCLE, "Circle", Icons.Default.Circle),
    ShapeOption(ShapeKind.LINE, "Line", Icons.Default.HorizontalRule),
    ShapeOption(ShapeKind.ARROW, "Arrow", Icons.AutoMirrored.Filled.ArrowRightAlt),
)

/** Two-tab picker for vector shapes and the bundled sticker pack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElementsPicker(
    onAddShape: (ShapeKind, Long) -> Unit,
    onAddSticker: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier.padding(
                start = AppTheme.spacing.lg,
                end = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Text("Elements", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Shapes") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Stickers") })
            }
            if (tab == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    SHAPE_OPTIONS.forEach { option ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppTheme.spacing.xs))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onAddShape(option.kind, DEFAULT_SHAPE_COLOR)
                                    onDismiss()
                                }
                                .padding(AppTheme.spacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
                        ) {
                            Icon(option.icon, contentDescription = option.label)
                            Text(option.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(StickerCatalog.stickers) { resId ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(AppTheme.spacing.xs))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onAddSticker(StickerCatalog.uri(context, resId))
                                    onDismiss()
                                }
                                .padding(AppTheme.spacing.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(resId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
