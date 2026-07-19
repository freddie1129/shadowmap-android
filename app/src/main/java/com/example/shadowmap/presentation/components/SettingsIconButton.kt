package com.example.shadowmap.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.example.shadowmap.R
import com.example.shadowmap.ui.theme.ShadowMapTheme

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
