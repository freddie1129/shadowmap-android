package com.gooludou.shadowplanner.feature.locationsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
fun LocationSearchScreen(
    uiState: LocationSearchUiState,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (LocationSearchResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensions.spacingSmall,
                        vertical = dimensions.spacingSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    keyboardController?.hide()
                    onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
                TextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_for_location)) },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = dimensions.spacingHuge)
                    )
                }

                uiState.errorMessageRes != null -> SearchMessage(
                    icon = Icons.Outlined.ErrorOutline,
                    message = stringResource(uiState.errorMessageRes)
                )

                uiState.query.length >= 2 && uiState.results.isEmpty() -> SearchMessage(
                    icon = Icons.Outlined.Search,
                    message = stringResource(R.string.no_locations_found)
                )

                else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(uiState.results, key = { it.id }) { result ->
                        LocationSearchResultItem(result, onClick = {
                            keyboardController?.hide()
                            onResultSelected(result)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSearchResultItem(result: LocationSearchResult, onClick: () -> Unit) {
    val dimensions = ShadowMapDesign.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensions.spacingExtraLarge,
                vertical = dimensions.spacingMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, style = MaterialTheme.typography.bodyLarge)
            if (result.address.isNotBlank() && result.address != result.name) {
                Text(
                    result.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(name = "Location search result", showBackground = true)
@Composable
private fun LocationSearchResultItemPreview() {
    ShadowMapTheme(dynamicColor = false) {
        LocationSearchResultItem(
            result = LocationSearchResult(
                id = "preview",
                name = "123 Main Street",
                address = "Brisbane QLD 4000, Australia",
                latitude = -27.4698,
                longitude = 153.0251
            ),
            onClick = {}
        )
    }
}

@Composable
private fun SearchMessage(icon: ImageVector, message: String) {
    val dimensions = ShadowMapDesign.dimensions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensions.spacingHuge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Location search empty state", showBackground = true)
@Composable
private fun SearchMessagePreview() {
    ShadowMapTheme(dynamicColor = false) {
        SearchMessage(
            icon = Icons.Outlined.Search,
            message = stringResource(R.string.no_locations_found)
        )
    }
}

@Preview(name = "Location search screen", showBackground = true)
@Composable
private fun LocationSearchScreenPreview() {
    ShadowMapTheme(dynamicColor = false) {
        LocationSearchScreen(
            uiState = LocationSearchUiState(
                query = "Brisbane",
                results = listOf(
                    LocationSearchResult(
                        id = "brisbane",
                        name = "Brisbane",
                        address = "Queensland, Australia",
                        latitude = -27.4698,
                        longitude = 153.0251
                    ),
                    LocationSearchResult(
                        id = "brisbane-city",
                        name = "Brisbane City",
                        address = "Brisbane QLD, Australia",
                        latitude = -27.4705,
                        longitude = 153.0260
                    )
                )
            ),
            onQueryChanged = {},
            onResultSelected = {},
            onBack = {}
        )
    }
}
