package com.gooludou.shadowplanner.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
internal fun MapboxBuildingSourceToggle(
    useMapboxBuildings: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SceneViewSwitchButton(
        label = stringResource(
            if (useMapboxBuildings) {
                R.string.show_drawing_buildings
            } else {
                R.string.show_mapbox_buildings
            }
        ),
        icon = Icons.Outlined.Apartment,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun MapboxDomeToggle(
    showDome: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SceneViewSwitchButton(
        label = stringResource(
            if (showDome) R.string.hide_sky_overview else R.string.show_sky_overview
        ),
        icon = Icons.Outlined.WbSunny,
        onClick = onClick,
        modifier = modifier
    )
}

internal fun mapboxNative3dConfig(showBuildings: Boolean): Map<String, Boolean> = mapOf(
    "show3dObjects" to showBuildings,
    "show3dBuildings" to showBuildings,
    "show3dTrees" to false,
    "show3dLandmarks" to false,
    "show3dFacades" to showBuildings
)

@Preview(name = "Mapbox building toggle - light", showBackground = true)
@Composable
private fun MapboxBuildingSourceToggleLightPreview() {
    MapboxBuildingSourceTogglePreviewContent(darkTheme = false)
}

@Preview(name = "Mapbox building toggle - dark", showBackground = true)
@Composable
private fun MapboxBuildingSourceToggleDarkPreview() {
    MapboxBuildingSourceTogglePreviewContent(darkTheme = true)
}

@Composable
private fun MapboxBuildingSourceTogglePreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MapboxBuildingSourceToggle(useMapboxBuildings = false, onClick = {})
        }
    }
}
