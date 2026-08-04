package com.gooludou.shadowplanner.feature.shadowmap.dashboard

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
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.BuildingSource
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.ui.components.MapRoundIconButton
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import com.gooludou.shadowplanner.feature.locationsearch.LocationSearchEntry
import com.gooludou.shadowplanner.feature.settings.SettingsIconButton
import com.gooludou.shadowplanner.feature.shadowmap.ShadowMapUiState

@Composable
@Suppress("LongMethod")
internal fun MapTopControls(
    uiState: ShadowMapUiState,
    onOpenSettings: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    onRecenterCurrentLocation: () -> Unit,
    canRecenterCurrentLocation: Boolean,
    modifier: Modifier = Modifier,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    onTooltipTargetBoundsChanged: (MainViewTooltipTarget, Rect) -> Unit = { _, _ -> }
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
                modifier = Modifier.Companion
                    .weight(1f)
                    .reportFeatureTourTarget(
                        MainViewTooltipTarget.LOCATION_SEARCH.name
                    ) { _, bounds ->
                        onTooltipTargetBoundsChanged(MainViewTooltipTarget.LOCATION_SEARCH, bounds)
                    }
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
            MapRoundIconButton(
                onClick = onRecenterCurrentLocation,
                contentDescription = stringResource(R.string.center_on_current_location),
                enabled = canRecenterCurrentLocation,
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.CURRENT_LOCATION.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.CURRENT_LOCATION, bounds)
                }
            ) {
                Icon(Icons.Outlined.MyLocation, contentDescription = null)
            }
            Spacer(modifier = Modifier.weight(1f))
            MapRoundIconButton(
                onClick = onOpenProjects,
                contentDescription = stringResource(R.string.open_projects),
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.OPEN_PROJECT.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.OPEN_PROJECT, bounds)
                }
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(ShadowMapDesign.dimensions.spacingSmall))
            MapRoundIconButton(
                onClick = onSaveProject,
                contentDescription = stringResource(R.string.save_project),
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.SAVE_PROJECT.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.SAVE_PROJECT, bounds)
                }
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
        MapTopControls(
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
            onRecenterCurrentLocation = {},
            canRecenterCurrentLocation = true,
            onOpenProjects = {},
            onSaveProject = {},
        )
    }
}
