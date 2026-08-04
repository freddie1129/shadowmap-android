package com.gooludou.shadowplanner.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.BuildConfig
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SettingsScreen(
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
    onDeveloperClick: () -> Unit,
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
                PremiumCard(
                    isPremium = isPremium,
                    onClick = onPremiumClick,
                    modifier = Modifier.padding(
                        horizontal = dimensions.screenPadding,
                        vertical = dimensions.spacingSmall
                    )
                )
            }
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
            if (BuildConfig.DEBUG) {
                item {
                    SettingsMenuItem(
                        icon = Icons.Outlined.Build,
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
private fun PremiumCard(isPremium: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (isPremium) {
        PremiumActiveCard(onClick = onClick, modifier = modifier)
    } else {
        UpgradeCard(onClick = onClick, modifier = modifier)
    }
}

private val PremiumCardText = Color(0xFF2C1A04)
private val PremiumGold = Color(0xFFFFC400)
private val PremiumActiveGreen = Color(0xFF238636)

@Composable
private fun UpgradeCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFF9500), Color(0xFFFFD000))
                    )
                )
                .padding(dimensions.spacingLarge)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.outline_crown_24),
                    contentDescription = null,
                    tint = PremiumCardText,
                    modifier = Modifier.size(dimensions.iconSize)
                )
                Spacer(modifier = Modifier.width(dimensions.spacingSmall))
                Text(
                    text = stringResource(R.string.premium_card_title),
                    color = PremiumCardText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            Column(verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                PremiumFeature(
                    icon = Icons.Outlined.CalendarMonth,
                    text = stringResource(R.string.paywall_benefit_dates)
                )
                PremiumFeature(
                    icon = Icons.Outlined.FolderOpen,
                    text = stringResource(R.string.paywall_benefit_projects)
                )
            }
            Spacer(modifier = Modifier.height(dimensions.spacingLarge))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumCardText,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(R.string.premium_card_cta),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PremiumFeature(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = modifier.padding(start = dimensions.spacingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumCardText.copy(alpha = 0.8f),
            modifier = Modifier.size(dimensions.spacingLarge)
        )
        Spacer(modifier = Modifier.width(dimensions.spacingSmall))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumCardText
        )
    }
}

@Composable
private fun PremiumActiveCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(dimensions.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = PremiumGold.copy(alpha = 0.15f),
                modifier = Modifier.size(dimensions.minimumTouchTarget)
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_crown_24),
                    contentDescription = null,
                    tint = PremiumGold,
                    modifier = Modifier.padding(dimensions.spacingMedium)
                )
            }
            Spacer(modifier = Modifier.width(dimensions.spacingMedium))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)
                ) {
                    Text(
                        text = stringResource(R.string.premium),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    ActiveBadge()
                }
                Text(
                    text = stringResource(R.string.premium_card_active_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActiveBadge(modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = PremiumActiveGreen.copy(alpha = 0.15f)
    ) {
        Text(
            text = stringResource(R.string.premium_status_active),
            style = MaterialTheme.typography.labelSmall,
            color = PremiumActiveGreen,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = dimensions.spacingSmall,
                vertical = dimensions.spacingXs
            )
        )
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
            isPremium = false,
            onPremiumClick = {},
            onDeveloperClick = {},
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
            isPremium = true,
            onPremiumClick = {},
            onDeveloperClick = {},
            onRateClick = {},
            onShareClick = {},
            onLanguageClick = {},
            onAboutClick = {},
            onBack = {}
        )
    }
}

@Preview(name = "Premium upgrade card", showBackground = true)
@Composable
private fun PremiumUpgradeCardPreview() {
    ShadowMapTheme(dynamicColor = false) {
        PremiumCard(isPremium = false, onClick = {})
    }
}

@Preview(name = "Premium active card - dark", showBackground = true)
@Composable
private fun PremiumActiveCardPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        PremiumCard(isPremium = true, onClick = {})
    }
}

@Preview(name = "Settings menu item", showBackground = true)
@Composable
private fun SettingsMenuItemPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsMenuItem(
            icon = Icons.Outlined.Info,
            title = "Developer",
            supportingText = "Testing and premium overrides",
            onClick = {}
        )
    }
}
