package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.sanu.carouselforge.core.theme.AppTheme

const val MIN_SLIDE_COUNT = 1
const val MAX_SLIDE_COUNT = 10

/**
 * The always-visible carousel slide counter. Adding a slide extends the connected
 * canvas to the right; removing one shrinks it back.
 */
@Composable
fun SlideCounter(
    slideCount: Int,
    onCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(AppTheme.spacing.sm),
                )
                .border(
                    AppTheme.spacing.selectionStroke,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    RoundedCornerShape(AppTheme.spacing.sm),
                )
                .padding(horizontal = AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onCountChanged(slideCount - 1) },
                enabled = slideCount > MIN_SLIDE_COUNT,
            ) {
                Icon(Icons.Default.Remove, "Remove slide")
            }
            Text(
                text = slideCount.toString(),
                modifier = Modifier.widthIn(min = AppTheme.spacing.lg),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = { onCountChanged(slideCount + 1) },
                enabled = slideCount < MAX_SLIDE_COUNT,
            ) {
                Icon(Icons.Default.Add, "Add slide")
            }
        }
    }
}
