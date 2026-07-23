package com.gooludou.shadowplanner.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.presentation.components.MapRoundIconButton
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun SettingsIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    MapRoundIconButton(
        onClick = onClick,
        contentDescription = stringResource(R.string.open_settings),
        modifier = modifier
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = null)
    }
}

@Preview(name = "Settings icon button", showBackground = true)
@Composable
private fun SettingsIconButtonPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsIconButton(onClick = {})
    }
}
