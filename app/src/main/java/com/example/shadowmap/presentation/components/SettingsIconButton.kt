package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shadowmap.ui.theme.ShadowMapTheme

@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(56.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 6.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = "Open settings" }
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }
    }
}

@Preview(name = "Settings icon button", showBackground = true)
@Composable
private fun SettingsIconButtonPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SettingsIconButton(onClick = {})
    }
}
