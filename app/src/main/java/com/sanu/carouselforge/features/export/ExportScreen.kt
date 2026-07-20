package com.sanu.carouselforge.features.export

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val format by viewModel.selectedFormat.collectAsStateWithLifecycle()
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
                Text(
                    "Choose a format",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    ExportFormat.entries.forEach { option ->
                        FilterChip(
                            selected = format == option,
                            onClick = { viewModel.selectFormat(option) },
                            label = { Text(formatLabel(option)) },
                        )
                    }
                }
                Button(onClick = { viewModel.export(format) }) {
                    Text("Export ${format.label}")
                }
            }
            is ExportState.Exporting -> {
                CircularProgressIndicator(progress = { current.progress })
                Text("Rendering high-resolution ${format.label}…")
            }
            is ExportState.Complete -> {
                Text("Export complete", style = MaterialTheme.typography.headlineMedium)
                Text("${current.slideCount} slides · ${current.width} × ${current.height} px")
                Text(
                    completeDestination(current.format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                current.warning?.let {
                    Text(it.userMessage(), color = MaterialTheme.colorScheme.primary)
                }
                Button(onClick = { shareExports(context, current.uris, current.format) }) {
                    Text("Share")
                }
                TextButton(onClick = onBack) { Text("Back to editor") }
            }
            is ExportState.Error -> {
                Text(current.error.userMessage(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.export(format) }) { Text("Retry") }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
    }
}

private fun formatLabel(format: ExportFormat): String = when (format) {
    ExportFormat.PNG -> "PNG images"
    ExportFormat.PDF -> "PDF document"
}

private fun completeDestination(format: ExportFormat): String = when (format) {
    ExportFormat.PNG -> "Saved to Pictures/CarouselForge in carousel order."
    ExportFormat.PDF -> "Saved as a multi-page PDF, ready to share to LinkedIn."
}

private fun shareExports(
    context: android.content.Context,
    uris: List<Uri>,
    format: ExportFormat,
) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }.apply {
        type = format.mimeType
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share carousel"))
}
