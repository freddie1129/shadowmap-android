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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SettingsScreen(
    onRateClick: () -> Unit,
    onShareClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val dimensions = ShadowMapDesign.dimensions
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
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
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
        ) {
            item {
                SettingsMenuItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.rate_on_google_play),
                    supportingText = stringResource(R.string.rate_on_google_play_summary),
                    onClick = onRateClick
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Outlined.Share,
                    title = stringResource(R.string.share_shadow_planner),
                    supportingText = stringResource(R.string.share_shadow_planner_summary),
                    onClick = onShareClick
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.language),
                    supportingText = stringResource(R.string.language_summary),
                    onClick = onLanguageClick
                )
            }
            item {
                SettingsMenuItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about),
                    supportingText = stringResource(R.string.about_settings_summary),
                    onClick = onAboutClick
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier
            .padding(horizontal = dimensions.screenPadding)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
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
                imageVector = icon,
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
        SettingsScreen(
            onRateClick = {},
            onShareClick = {},
            onLanguageClick = {},
            onAboutClick = {},
            onBack = {}
        )
    }
}

@Preview(name = "Settings screen - dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        SettingsScreen(
            onRateClick = {},
            onShareClick = {},
            onLanguageClick = {},
            onAboutClick = {},
            onBack = {}
        )
    }
}

@Preview(name = "Settings menu item", showBackground = true)
@Composable
private fun SettingsMenuItemPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsMenuItem(
            icon = Icons.Outlined.Info,
            title = "Developer",
            supportingText = "Application information",
            onClick = {}
        )
    }
}
