package com.gooludou.shadowplanner.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    forcePremium: Boolean,
    onForcePremiumChange: (Boolean) -> Unit,
    onClearAllStoredData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.developer_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ForcePremiumSetting(
                enabled = forcePremium,
                onEnabledChange = onForcePremiumChange
            )
            HorizontalDivider()
            ClearStoredDataSetting(onClick = { showClearConfirmation = true })
        }
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_stored_data)) },
            text = { Text(stringResource(R.string.clear_stored_data_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearAllStoredData()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ClearStoredDataSetting(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensions.screenPadding,
                vertical = dimensions.spacingMedium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.clear_stored_data),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.clear_stored_data_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ForcePremiumSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEnabledChange(!enabled) }
            .padding(
                horizontal = dimensions.screenPadding,
                vertical = dimensions.spacingMedium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.force_premium),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.force_premium_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Preview(name = "Developer settings - light", showBackground = true)
@Composable
private fun DeveloperSettingsLightPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        DeveloperSettingsScreen(
            forcePremium = true,
            onForcePremiumChange = {},
            onClearAllStoredData = {},
            onBack = {}
        )
    }
}

@Preview(name = "Developer settings - dark", showBackground = true)
@Composable
private fun DeveloperSettingsDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        DeveloperSettingsScreen(
            forcePremium = false,
            onForcePremiumChange = {},
            onClearAllStoredData = {},
            onBack = {}
        )
    }
}

@Preview(name = "Force premium setting", showBackground = true)
@Composable
private fun ForcePremiumSettingPreview() {
    ShadowMapTheme(dynamicColor = false) {
        ForcePremiumSetting(enabled = true, onEnabledChange = {})
    }
}
