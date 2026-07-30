package com.gooludou.shadowplanner.feature.shadowmap

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
internal fun LocationPermissionDialog(
    state: LocationPermissionDialogState,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isSettingsRequired = state == LocationPermissionDialogState.SETTINGS
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isSettingsRequired) {
                        R.string.location_permission_needed
                    } else {
                        R.string.use_current_location_question
                    }
                )
            )
        },
        text = {
            Text(
                stringResource(
                    if (isSettingsRequired) {
                        R.string.location_permission_settings_explanation
                    } else {
                        R.string.location_permission_explanation
                    }
                )
            )
        },
        confirmButton = {
            Button(onClick = if (isSettingsRequired) onOpenSettings else onContinue) {
                Text(
                    stringResource(
                        if (isSettingsRequired) {
                            R.string.open_settings
                        } else {
                            R.string.continue_label
                        }
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.not_now))
            }
        }
    )
}

@Preview(name = "Location permission rationale - light", showBackground = true)
@Composable
private fun LocationPermissionDialogLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        LocationPermissionDialog(LocationPermissionDialogState.RATIONALE, {}, {}, {})
    }
}

@Preview(name = "Location permission settings - dark", showBackground = true)
@Composable
private fun LocationPermissionDialogDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        LocationPermissionDialog(LocationPermissionDialogState.SETTINGS, {}, {}, {})
    }
}
