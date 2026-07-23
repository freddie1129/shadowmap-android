package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
internal fun MapboxScene3DControls(
    mapViewportState: MapViewportState,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    basemapStyle: MapboxBasemapStyle,
    onToggleBasemapStyle: () -> Unit,
    showDome: Boolean,
    useMapboxBuildings: Boolean,
    onToggleBuildingSource: () -> Unit,
    onToggleDome: () -> Unit,
    onBackToMap: () -> Unit
) {
    val currentPitch = mapViewportState.cameraState?.pitch ?: Scene3DCamera.ORBIT_PITCH_DEGREES
    val isTopDown = currentPitch <= Scene3DCamera.TOP_DOWN_THRESHOLD_DEGREES
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MapboxScene3DTopControls(
            basemapStyle = basemapStyle,
            onToggleBasemapStyle = onToggleBasemapStyle,
            showDome = showDome,
            useMapboxBuildings = useMapboxBuildings,
            onToggleBuildingSource = onToggleBuildingSource,
            onToggleDome = onToggleDome,
            onBackToMap = onBackToMap
        )
        MapboxScene3DBottomControls(
            mapViewportState = mapViewportState,
            currentPitch = currentPitch,
            isTopDown = isTopDown,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected
        )
    }
}

@Composable
private fun BoxScope.MapboxScene3DTopControls(
    basemapStyle: MapboxBasemapStyle,
    onToggleBasemapStyle: () -> Unit,
    showDome: Boolean,
    useMapboxBuildings: Boolean,
    onToggleBuildingSource: () -> Unit,
    onToggleDome: () -> Unit,
    onBackToMap: () -> Unit
) {
    SceneViewSwitchButton(
        label = stringResource(R.string.map_view),
        icon = Icons.Outlined.Map,
        onClick = onBackToMap,
        modifier = Modifier.align(Alignment.TopStart)
    )
    SceneViewSwitchButton(
        label = stringResource(
            if (basemapStyle == MapboxBasemapStyle.SATELLITE) {
                R.string.standard_map_style
            } else {
                R.string.satellite_map_style
            }
        ),
        icon = if (basemapStyle == MapboxBasemapStyle.SATELLITE) {
            Icons.Outlined.Map
        } else {
            Icons.Outlined.SatelliteAlt
        },
        onClick = onToggleBasemapStyle,
        modifier = Modifier.align(Alignment.TopEnd)
    )
    MapboxDomeToggle(
        showDome = showDome,
        onClick = onToggleDome,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = Scene3DControlLayout.DOME_CONTROL_OFFSET)
    )
    MapboxBuildingSourceToggle(
        useMapboxBuildings = useMapboxBuildings,
        onClick = onToggleBuildingSource,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = Scene3DControlLayout.BUILDING_CONTROL_OFFSET)
    )
}

@Composable
private fun BoxScope.MapboxScene3DBottomControls(
    mapViewportState: MapViewportState,
    currentPitch: Double,
    isTopDown: Boolean,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit
) {
    SceneViewSwitchButton(
        label = stringResource(
            if (isTopDown) R.string.view_3d_button else R.string.top_down_view
        ),
        icon = if (isTopDown) Icons.Outlined.ViewInAr else Icons.Outlined.Map,
        onClick = {
            mapViewportState.setCameraOptions {
                pitch(
                    if (isTopDown) {
                        Scene3DCamera.ORBIT_PITCH_DEGREES
                    } else {
                        Scene3DCamera.TOP_DOWN_PITCH_DEGREES
                    }
                )
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = ShadowMapDesign.dimensions.screenPadding,
                bottom = Scene3DControlLayout.BOTTOM_CONTROL_CLEARANCE
            )
    )
    PitchSlider(
        pitch = currentPitch.toFloat(),
        onPitchChange = { pitch ->
            mapViewportState.setCameraOptions {
                pitch(pitch.toDouble())
            }
        },
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    DateTimeSpinner(
        selectedEpochMillis = selectedEpochMillis,
        timeZoneId = timeZoneId,
        onDateTimeChanged = onDateTimeChanged,
        onNowSelected = onNowSelected,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

@Preview(name = "Mapbox 3D controls - light", showBackground = true)
@Composable
private fun MapboxScene3DControlsLightPreview() {
    MapboxScene3DControlsPreviewContent(darkTheme = false)
}

@Preview(name = "Mapbox 3D controls - dark", showBackground = true)
@Composable
private fun MapboxScene3DControlsDarkPreview() {
    MapboxScene3DControlsPreviewContent(darkTheme = true)
}

@Composable
private fun MapboxScene3DControlsPreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            val mapViewportState = rememberMapViewportState()
            MapboxScene3DControls(
                mapViewportState = mapViewportState,
                selectedEpochMillis = 1_752_640_000_000L,
                timeZoneId = "Australia/Brisbane",
                onDateTimeChanged = {},
                onNowSelected = {},
                basemapStyle = MapboxBasemapStyle.SATELLITE,
                onToggleBasemapStyle = {},
                showDome = true,
                useMapboxBuildings = false,
                onToggleBuildingSource = {},
                onToggleDome = {},
                onBackToMap = {}
            )
        }
    }
}
