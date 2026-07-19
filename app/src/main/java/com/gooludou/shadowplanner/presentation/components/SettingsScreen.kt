package com.gooludou.shadowplanner.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.gooludou.shadowplanner.R
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Preview(name = "Settings screen", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsScreen(onBack = {})
    }
}
