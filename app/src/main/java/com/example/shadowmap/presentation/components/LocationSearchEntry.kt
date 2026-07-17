package com.example.shadowmap.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.shadowmap.ui.theme.ShadowMapDesign
import com.example.shadowmap.ui.theme.ShadowMapTheme

@Composable
fun LocationSearchEntry(
    label: String?,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingLarge)
            .semantics { contentDescription = label ?: "Search for a location" },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = dimensions.floatingControlElevation
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(
                horizontal = dimensions.spacingSmall,
                vertical = dimensions.spacingXs
            ),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = dimensions.spacingSmall,
                        vertical = dimensions.spacingSmall
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text(
                    text = label ?: "Search for a location",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = dimensions.spacingMedium)
                )
            }
            if (label != null) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.semantics {
                        contentDescription = "Show selected location address"
                    }
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                }
            }
        }
    }
}

@Preview(name = "Location search entry", showBackground = true)
@Composable
private fun LocationSearchEntryPreview() {
    ShadowMapTheme(dynamicColor = false) {
        LocationSearchEntry(label = null, onClick = {}, onInfoClick = {})
    }
}
