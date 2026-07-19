package com.sanu.carouselforge.features.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanu.carouselforge.core.error.userMessage
import com.sanu.carouselforge.core.theme.AppTheme
import com.sanu.carouselforge.data.repository.ProjectSummary

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenProject: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppTheme.spacing.md,
                top = AppTheme.spacing.huge + AppTheme.spacing.md,
                end = AppTheme.spacing.md,
                bottom = AppTheme.spacing.huge + AppTheme.spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            item { GalleryHero() }
            item { TemplateSection() }
            item {
                SectionHeading(
                    title = "DRAFTS",
                    action = when (val current = state) {
                        is GalleryState.Content -> "${current.projects.size} PROJECTS"
                        else -> ""
                    },
                )
            }
            when (val current = state) {
                GalleryState.Loading -> item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GalleryState.Error -> item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            current.error.userMessage(),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::refresh) { Text("Retry") }
                    }
                }
                is GalleryState.Content -> {
                    if (current.projects.isEmpty()) {
                        item { EmptyDraftCard() }
                    } else {
                        items(current.projects, key = ProjectSummary::id) { project ->
                            DraftCard(
                                project = project,
                                onOpen = { onOpenProject(project.id) },
                                onDelete = { viewModel.deleteProject(project.id) },
                            )
                        }
                    }
                }
            }
        }

        StudioTopBar(
            modifier = Modifier.align(Alignment.TopCenter),
            onSettings = onOpenSettings,
        )

        Button(
            onClick = { viewModel.createProject(onOpenProject) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AppTheme.spacing.huge + AppTheme.spacing.sm)
                .height(AppTheme.spacing.toolbarHeight)
                .shadow(
                    elevation = AppTheme.spacing.md,
                    shape = RoundedCornerShape(AppTheme.spacing.sm),
                    spotColor = MaterialTheme.colorScheme.secondary,
                ),
            shape = RoundedCornerShape(AppTheme.spacing.sm),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
            ),
            contentPadding = PaddingValues(horizontal = AppTheme.spacing.xl),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(AppTheme.spacing.xs))
            Text("NEW PROJECT", fontWeight = FontWeight.Bold)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = AppTheme.spacing.xs,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.spacing.huge)
                    .padding(horizontal = AppTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomDestination(Icons.Default.Collections, "Edit", selected = true)
                BottomDestination(Icons.Default.AutoAwesome, "Templates")
                BottomDestination(Icons.Default.Tune, "Tools")
                BottomDestination(
                    Icons.Default.Settings,
                    "Settings",
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun StudioTopBar(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = AppTheme.spacing.xxs,
    ) {
        Row(
            modifier = Modifier
                .height(AppTheme.spacing.huge)
                .padding(horizontal = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Studio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.PersonOutline, contentDescription = "Profile and settings")
                }
            }
        }
    }
}

@Composable
private fun GalleryHero() {
    val shape = RoundedCornerShape(AppTheme.spacing.xs)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.85f)
            .shadow(AppTheme.spacing.xs, shape)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.onSurface,
                    ),
                ),
            )
            .padding(AppTheme.spacing.md),
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(AppTheme.spacing.xxl),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                "AUTO-EDIT",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.surfaceBright,
                fontWeight = FontWeight.Black,
            )
            Text(
                "SMART TEMPLATES FOR REELS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surfaceBright,
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Text(
                "NEW",
                modifier = Modifier.padding(
                    horizontal = AppTheme.spacing.xs,
                    vertical = AppTheme.spacing.xxs,
                ),
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TemplateSection() {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        SectionHeading("TEMPLATES", "VIEW ALL", accentAction = true)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            item { TemplateCard("Glitch Drift", "1.2M USES", Icons.Default.AutoAwesome) }
            item { TemplateCard("Macro Flow", "458K USES", Icons.Default.Tune) }
            item { TemplateCard("Still Cut", "89K USES", Icons.Default.Image) }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    action: String,
    accentAction: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            action,
            style = MaterialTheme.typography.bodySmall,
            color = if (accentAction) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun TemplateCard(title: String, uses: String, icon: ImageVector) {
    val shape = RoundedCornerShape(AppTheme.spacing.xxs)
    Box(
        modifier = Modifier
            .width(AppTheme.spacing.huge + AppTheme.spacing.xxl)
            .aspectRatio(0.68f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.onSurface,
                    ),
                ),
            )
            .padding(AppTheme.spacing.xs),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(AppTheme.spacing.xl),
            tint = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.7f),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.surfaceBright,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                uses,
                color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DraftCard(
    project: ProjectSummary,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(AppTheme.spacing.xs),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = AppTheme.spacing.xxs,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.huge)
                    .clip(RoundedCornerShape(AppTheme.spacing.xxs))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceBright,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.sm),
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${project.layerCount} layers · ${project.canvasWidth}×${project.canvasHeight}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete project")
            }
        }
    }
}

@Composable
private fun EmptyDraftCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppTheme.spacing.xs),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Collections, contentDescription = null)
            Spacer(Modifier.height(AppTheme.spacing.xs))
            Text("Your drafts will appear here", fontWeight = FontWeight.Bold)
            Text(
                "Tap New Project to start a carousel.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BottomDestination(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
