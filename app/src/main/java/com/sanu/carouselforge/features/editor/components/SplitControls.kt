package com.sanu.carouselforge.features.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun SplitControls(
    visible: Boolean,
    splitCount: Int,
    onVisibleChanged: (Boolean) -> Unit,
    onCountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.xxs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Split guides", style = MaterialTheme.typography.labelLarge)
        Switch(checked = visible, onCheckedChange = onVisibleChanged)
        IconButton(
            onClick = { onCountChanged(splitCount - 1) },
            enabled = visible && splitCount > MIN_SPLIT_COUNT,
        ) {
            Icon(Icons.Default.Remove, "Fewer slides")
        }
        Text(splitCount.toString(), style = MaterialTheme.typography.titleLarge)
        IconButton(
            onClick = { onCountChanged(splitCount + 1) },
            enabled = visible && splitCount < MAX_SPLIT_COUNT,
        ) {
            Icon(Icons.Default.Add, "More slides")
        }
    }
}

private const val MIN_SPLIT_COUNT = 2
private const val MAX_SPLIT_COUNT = 9
