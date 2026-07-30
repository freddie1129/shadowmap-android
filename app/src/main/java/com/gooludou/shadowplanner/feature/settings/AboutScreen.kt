package com.gooludou.shadowplanner.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    actions: AboutActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val dimensions = ShadowMapDesign.dimensions
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                AppInfoCard(
                    versionName = versionName,
                    modifier = Modifier
                        .padding(horizontal = dimensions.screenPadding)
                        .padding(top = dimensions.spacingSmall)
                )
            }
            item { Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge)) }
            item {
                PerfectForSection(
                    modifier = Modifier.padding(horizontal = dimensions.screenPadding)
                )
            }
            item { Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge)) }
            item {
                ActionCard(
                    actions = actions,
                    modifier = Modifier.padding(horizontal = dimensions.screenPadding)
                )
            }
            item { Spacer(modifier = Modifier.height(dimensions.spacingExtraLarge)) }
        }
    }
}

@Composable
private fun AppInfoCard(versionName: String, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    AboutCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(dimensions.spacingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = stringResource(R.string.about_app_icon_description),
                modifier = Modifier
                    .size(dimensions.minimumTouchTarget + dimensions.spacingHuge)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.height(dimensions.spacingLarge))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(dimensions.spacingXs))
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensions.spacingMedium))
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PerfectForSection(modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.about_perfect_for),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = dimensions.spacingSmall,
                bottom = dimensions.spacingSmall
            )
        )
        AboutCard {
            Column(modifier = Modifier.padding(vertical = dimensions.spacingSmall)) {
                AboutRow(Icons.Outlined.Home, stringResource(R.string.about_use_home))
                AboutDivider()
                AboutRow(Icons.Outlined.Deck, stringResource(R.string.about_use_outdoor))
                AboutDivider()
                AboutRow(Icons.Outlined.Yard, stringResource(R.string.about_use_garden))
                AboutDivider()
                AboutRow(
                    Icons.Outlined.EnergySavingsLeaf,
                    stringResource(R.string.about_use_solar)
                )
            }
        }
    }
}

@Composable
private fun ActionCard(actions: AboutActions, modifier: Modifier = Modifier) {
    AboutCard(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = ShadowMapDesign.dimensions.spacingSmall)) {
            AboutRow(
                Icons.Outlined.Language,
                stringResource(R.string.about_website),
                actions.onWebsiteClick
            )
            AboutDivider()
            AboutRow(
                Icons.Outlined.PrivacyTip,
                stringResource(R.string.about_privacy_policy),
                actions.onPrivacyPolicyClick
            )
            AboutDivider()
            AboutRow(
                Icons.Outlined.Email,
                stringResource(R.string.about_contact_us),
                actions.onContactUsClick
            )
            AboutDivider()
            AboutRow(
                Icons.Outlined.Share,
                stringResource(R.string.about_share),
                actions.onShareClick
            )
            AboutDivider()
            AboutRow(
                Icons.Outlined.Star,
                stringResource(R.string.about_rate),
                actions.onRateClick
            )
        }
    }
}

@Composable
private fun AboutCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            ShadowMapDesign.dimensions.spacingXxs / 2,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { content() }
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(dimensions.spacingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(dimensions.iconSize)
        )
        Spacer(modifier = Modifier.width(dimensions.spacingLarge))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = ShadowMapDesign.dimensions.spacingLarge)
    )
}

private val PreviewAboutActions = AboutActions({}, {}, {}, {}, {})

@Preview(name = "About screen - light", showBackground = true)
@Composable
private fun AboutScreenLightPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = false) {
        AboutScreen("1.0.0", PreviewAboutActions, onBack = {})
    }
}

@Preview(name = "About screen - dark", showBackground = true)
@Composable
private fun AboutScreenDarkPreview() {
    ShadowMapTheme(dynamicColor = false, darkTheme = true) {
        AboutScreen("1.0.0", PreviewAboutActions, onBack = {})
    }
}

@Preview(name = "About action row", showBackground = true)
@Composable
private fun AboutRowPreview() {
    ShadowMapTheme(dynamicColor = false) {
        AboutRow(Icons.Outlined.Language, "Website", onClick = {})
    }
}
