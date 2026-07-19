package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.gooludou.shadowplanner.R
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
    onBack: () -> Unit
) {
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
                modifier = Modifier.fillMaxSize().padding(
                    padding
                ).padding(ShadowMapDesign.dimensions.screenPadding),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Text(stringResource(R.string.no_saved_projects), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.save_project_hint))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(projects, key = { it.id }) { project ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onProjectSelected(project.id)
                        }
                            .padding(ShadowMapDesign.dimensions.screenPadding),
                        horizontalArrangement = Arrangement.spacedBy(
                            ShadowMapDesign.dimensions.spacingMedium
                        )
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Column {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            Text(DateFormat.getDateTimeInstance().format(Date(project.updatedAt)))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectListScreenPreview() {
    ShadowMapTheme(dynamicColor = false) {
        ProjectListScreen(emptyList(), onProjectSelected = {}, onBack = {})
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
                    updatedAt = 1_752_640_000_000L,
                    fileName = "riverside-house.json"
                ),
                ProjectSummary(
                    id = "garden-study",
                    name = "Garden study",
                    updatedAt = 1_752_550_000_000L,
                    fileName = "garden-study.json"
                )
            ),
            onProjectSelected = {},
            onBack = {}
        )
    }
}
