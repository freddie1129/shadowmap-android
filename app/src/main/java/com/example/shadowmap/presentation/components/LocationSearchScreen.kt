package com.example.shadowmap.presentation.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shadowmap.location.LocationSearchResult
import com.example.shadowmap.presentation.LocationSearchUiState
import com.example.shadowmap.ui.theme.ShadowMapTheme

@Composable
fun LocationSearchScreen(
    uiState: LocationSearchUiState,
    onQueryChanged: (String) -> Unit,
    onResultSelected: (LocationSearchResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    keyboardController?.hide()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                TextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search for a location") },
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp)
                    )
                }
                uiState.errorMessage != null -> SearchMessage(
                    icon = Icons.Outlined.ErrorOutline,
                    message = uiState.errorMessage
                )
                uiState.query.length >= 2 && uiState.results.isEmpty() -> SearchMessage(
                    icon = Icons.Outlined.Search,
                    message = "No locations found"
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
private fun LocationSearchResultItem(
    result: LocationSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
private fun SearchMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            message = "No locations found"
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
