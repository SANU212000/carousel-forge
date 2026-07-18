package com.sanu.carouselforge.features.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.theme.AppTheme

@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            AppTheme.spacing.md,
            Alignment.CenterVertically,
        ),
    ) {
        when (val current = state) {
            ExportState.Ready -> {
                Text("Export carousel", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = viewModel::export) { Text("Export PNG") }
            }
            is ExportState.Exporting -> {
                CircularProgressIndicator(progress = { current.progress })
                Text("Rendering high-resolution PNG…")
            }
            is ExportState.Complete -> {
                Text("Export complete", style = MaterialTheme.typography.headlineMedium)
                Text("${current.width} × ${current.height} px")
                current.warning?.let {
                    Text(it.userMessage(), color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onBack) { Text("Back to editor") }
            }
            is ExportState.Error -> {
                Text(current.error.userMessage(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::export) { Text("Retry") }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
    }
}
