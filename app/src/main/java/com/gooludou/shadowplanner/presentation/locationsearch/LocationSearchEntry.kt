package com.gooludou.shadowplanner.presentation.locationsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun LocationSearchEntry(
    label: String?,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchLabel = label ?: stringResource(R.string.search_for_location)
    val infoDescription = stringResource(R.string.show_selected_location_address)
    val dimensions = ShadowMapDesign.dimensions
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions.spacingLarge)
            .semantics { contentDescription = searchLabel },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = dimensions.floatingControlElevation
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = dimensions.spacingSmall,
                vertical = dimensions.spacingXs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = dimensions.spacingSmall,
                        vertical = dimensions.spacingSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Text(
                    text = searchLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = dimensions.spacingMedium)
                )
            }
            if (label != null) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.semantics {
                        contentDescription = infoDescription
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
