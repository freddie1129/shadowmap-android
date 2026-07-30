package com.gooludou.shadowplanner.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDeveloperClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            if (BuildConfig.DEBUG) {
                item {
                    SettingsMenuItem(
                        title = stringResource(R.string.developer),
                        supportingText = stringResource(R.string.developer_settings_summary),
                        onClick = onDeveloperClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    horizontal = dimensions.screenPadding,
                    vertical = dimensions.spacingMedium
                ),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Preview(name = "Settings screen - light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        SettingsScreen(onDeveloperClick = {}, onBack = {})
    }
}

@Preview(name = "Settings screen - dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        SettingsScreen(onDeveloperClick = {}, onBack = {})
    }
}

@Preview(name = "Settings menu item", showBackground = true)
@Composable
private fun SettingsMenuItemPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsMenuItem(
            title = "Developer",
            supportingText = "Testing and premium overrides",
            onClick = {}
        )
    }
}
