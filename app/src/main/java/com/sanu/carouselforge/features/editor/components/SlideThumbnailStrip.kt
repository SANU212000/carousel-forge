package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun SlideThumbnailStrip(
    splitCount: Int,
    imageUri: String?,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = AppTheme.spacing.md,
        ),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        items((1..splitCount).toList(), key = { it }) { slide ->
            val active = slide == 1
            val shape = RoundedCornerShape(AppTheme.spacing.xxs)
            Box(
                modifier = Modifier
                    .width(AppTheme.spacing.huge + AppTheme.spacing.xl)
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = AppTheme.spacing.selectionStroke,
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = shape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (active && imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Slide $slide",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
                Text(
                    text = slide.toString(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            shape,
                        )
                        .padding(
                            horizontal = AppTheme.spacing.xxs,
                            vertical = AppTheme.spacing.xxs,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
