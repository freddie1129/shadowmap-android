package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.shadowmap.ui.theme.ShadowMapDesign
import com.example.shadowmap.ui.theme.ShadowMapTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedLocationSheet(address: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = ShadowMapDesign.dimensions
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions.screenPadding,
                    end = dimensions.screenPadding,
                    bottom = dimensions.spacingHuge
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
        ) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Selected location",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Selected location sheet", showBackground = true)
@Composable
private fun SelectedLocationSheetPreview() {
    ShadowMapTheme(dynamicColor = false) {
        SelectedLocationSheet(
            address = "123 Main Street, Brisbane QLD 4000, Australia",
            onDismiss = {}
        )
    }
}
