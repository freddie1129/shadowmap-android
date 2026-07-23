package com.gooludou.shadowplanner.presentation.drawview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.BuildingSource
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.GeoPolygon
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.components.MapRoundIconButton
import com.gooludou.shadowplanner.presentation.locationsearch.LocationSearchEntry
import com.gooludou.shadowplanner.presentation.settings.SettingsIconButton
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun Map2DTopControls(
    uiState: ShadowMapUiState,
    onOpenSettings: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    onOpen3D: () -> Unit,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = dimensions.spacingSmall)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = dimensions.spacingLarge),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            SettingsIconButton(onClick = onOpenSettings)
            LocationSearchEntry(
                label = uiState.selectedLocationLabel,
                onClick = onOpenLocationSearch,
                onInfoClick = onShowLocationInfo,
                modifier = Modifier.Companion.weight(1f)
            )
        }
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(
                    top = dimensions.spacingSmall,
                    start = dimensions.spacingLarge,
                    end = dimensions.spacingLarge
                ),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            MapRoundIconButton(
                onClick = onOpenProjects,
                contentDescription = stringResource(R.string.open_projects)
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(ShadowMapDesign.dimensions.spacingSmall))
            MapRoundIconButton(
                onClick = onSaveProject,
                contentDescription = stringResource(R.string.save_project)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
            }
        }
    }
}

@Preview(name = "Map top controls", showBackground = true, backgroundColor = 0xFF6B8064)
@Composable
private fun Map2DTopControlsLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Map2DTopControlsPreview()
    }
}

@Preview(name = "Map top controls (dark)", showBackground = true, backgroundColor = 0xFF263238)
@Composable
private fun Map2DTopControlsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Map2DTopControlsPreview()
    }
}

@Composable
private fun Map2DTopControlsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6B8064)),
        contentAlignment = Alignment.TopCenter
    ) {
        Map2DTopControls(
            uiState = ShadowMapUiState(
                selectedEpochMillis = 1_752_640_000_000L,
                displayTimeZoneId = "Australia/Brisbane",
                selectedLocationLabel = "Brisbane, Queensland",
                drawnBuildings = listOf(
                    Building(
                        id = "preview-building",
                        polygon = GeoPolygon(
                            listOf(
                                listOf(
                                    GeoPoint(153.025, -27.469),
                                    GeoPoint(153.026, -27.469),
                                    GeoPoint(153.026, -27.470)
                                )
                            )
                        ),
                        heightMeters = 6.0,
                        source = BuildingSource.MANUAL
                    )
                )
            ),
            onOpenSettings = {},
            onOpenLocationSearch = {},
            onShowLocationInfo = {},
            onOpen3D = {},
            onOpenProjects = {},
            onSaveProject = {}
        )
    }
}
