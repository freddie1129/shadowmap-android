package com.gooludou.shadowplanner.presentation.projectview

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.presentation.components.ShadowMapDialog
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun SaveProjectDialog(
    projectName: String,
    activeProjectName: String?,
    hasActiveProject: Boolean,
    onProjectNameChanged: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveUpdate: (String) -> Unit,
    onSaveAsNew: (String) -> Unit
) {
    val trimmedProjectName = projectName.trim()
    val canSaveAsNew =
        hasActiveProject &&
            trimmedProjectName.isNotEmpty() &&
            trimmedProjectName != activeProjectName.orEmpty().trim()

    ShadowMapDialog(
        onDismissRequest = onDismissRequest,
        content = {
            SaveProjectDialogContent(
                projectName = projectName,
                onProjectNameChanged = onProjectNameChanged
            )
        },
        dismissAction = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        neutralAction = {
            if (hasActiveProject) {
                OutlinedButton(
                    enabled = canSaveAsNew,
                    onClick = { onSaveAsNew(trimmedProjectName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_as_new))
                }
            }
        },
        positiveAction = {
            Button(
                enabled = trimmedProjectName.isNotEmpty(),
                onClick = { onSaveUpdate(trimmedProjectName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (hasActiveProject) R.string.save_update else R.string.save_project
                    )
                )
            }
        }
    )
}

@Composable
private fun ColumnScope.SaveProjectDialogContent(
    projectName: String,
    onProjectNameChanged: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.save_project),
        style = MaterialTheme.typography.headlineSmall
    )
    OutlinedTextField(
        value = projectName,
        onValueChange = onProjectNameChanged,
        label = { Text(stringResource(R.string.project_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(name = "Save project dialog · light", showBackground = true)
@Composable
private fun SaveProjectDialogLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        ShadowMapDialog(
            onDismissRequest = {},
            content = {
                SaveProjectDialogContent(
                    projectName = "Morning study",
                    onProjectNameChanged = {}
                )
            },
            dismissAction = {},
            neutralAction = {},
            positiveAction = {}
        )
    }
}

@Preview(name = "Save project dialog · dark", showBackground = true)
@Composable
private fun SaveProjectDialogDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        ShadowMapDialog(
            onDismissRequest = {},
            content = {
                SaveProjectDialogContent(
                    projectName = "Morning study",
                    onProjectNameChanged = {}
                )
            },
            dismissAction = {},
            neutralAction = {},
            positiveAction = {}
        )
    }
}
