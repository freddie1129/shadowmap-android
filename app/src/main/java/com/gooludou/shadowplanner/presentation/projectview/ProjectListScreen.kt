package com.gooludou.shadowplanner.presentation.projectview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.presentation.components.ShadowMapDialog
import com.gooludou.shadowplanner.project.ProjectSummary
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    projects: List<ProjectSummary>,
    onProjectSelected: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onBack: () -> Unit
) {
    var projectPendingDeletion by remember { mutableStateOf<ProjectSummary?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dimensions = ShadowMapDesign.dimensions
                Surface(
                    modifier = Modifier.size(
                        dimensions.minimumTouchTarget + dimensions.spacingLarge
                    ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(dimensions.iconSize),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge))
                Text(
                    stringResource(R.string.no_saved_projects),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(dimensions.spacingSmall))
                Text(
                    text = stringResource(R.string.save_project_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge))
                OutlinedButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                    Text(stringResource(R.string.back_to_map))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(ShadowMapDesign.dimensions.spacingLarge),
                verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingMedium)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectListItem(
                        project = project,
                        onClick = { onProjectSelected(project.id) },
                        onDelete = { projectPendingDeletion = project }
                    )
                }
            }
        }
    }
    projectPendingDeletion?.let { project ->
        DeleteProjectDialog(
            project = project,
            onDismissRequest = { projectPendingDeletion = null },
            onDelete = {
                projectPendingDeletion = null
                onDeleteProject(project.id)
            }
        )
    }
}

@Composable
private fun ProjectListItem(project: ProjectSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    val dimensions = ShadowMapDesign.dimensions
    var showMenu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.spacingLarge),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Surface(
                modifier = Modifier.size(dimensions.minimumTouchTarget),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.padding(dimensions.spacingMedium),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                project.locationLabel?.takeIf { it.isNotBlank() }?.let { locationLabel ->
                    Text(
                        text = locationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(
                        R.string.updated,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(project.updatedAt))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_project)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteProjectDialog(
    project: ProjectSummary,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    ShadowMapDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Text(
                text = stringResource(R.string.delete_project),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(stringResource(R.string.delete_project_message, project.name))
        },
        dismissAction = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        positiveAction = {
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ProjectListScreenPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        ProjectListScreen(emptyList(), onProjectSelected = {}, onDeleteProject = {}, onBack = {})
    }
}

@Preview(name = "Empty project list · dark", showBackground = true)
@Composable
private fun ProjectListScreenEmptyDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        ProjectListScreen(emptyList(), onProjectSelected = {}, onDeleteProject = {}, onBack = {})
    }
}

@Preview(name = "Project list with projects", showBackground = true)
@Composable
private fun ProjectListScreenWithProjectsPreview() {
    ShadowMapTheme(dynamicColor = false) {
        ProjectListScreen(
            projects = listOf(
                ProjectSummary(
                    id = "riverside-house",
                    name = "Riverside house",
                    locationLabel = "123 Riverside Drive, Brisbane QLD",
                    updatedAt = 1_752_640_000_000L,
                    fileName = "riverside-house.json"
                ),
                ProjectSummary(
                    id = "garden-study",
                    name = "Garden study",
                    locationLabel = "45 Garden Street, Brisbane QLD",
                    updatedAt = 1_752_550_000_000L,
                    fileName = "garden-study.json"
                )
            ),
            onProjectSelected = {},
            onDeleteProject = {},
            onBack = {}
        )
    }
}

@Preview(name = "Project list with projects · dark", showBackground = true)
@Composable
private fun ProjectListScreenWithProjectsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        ProjectListScreen(
            projects = listOf(
                ProjectSummary(
                    id = "riverside-house",
                    name = "Riverside house",
                    locationLabel = "123 Riverside Drive, Brisbane QLD",
                    updatedAt = 1_752_640_000_000L,
                    fileName = "riverside-house.json"
                ),
                ProjectSummary(
                    id = "garden-study",
                    name = "Garden study",
                    locationLabel = "45 Garden Street, Brisbane QLD",
                    updatedAt = 1_752_550_000_000L,
                    fileName = "garden-study.json"
                )
            ),
            onProjectSelected = {},
            onDeleteProject = {},
            onBack = {}
        )
    }
}
