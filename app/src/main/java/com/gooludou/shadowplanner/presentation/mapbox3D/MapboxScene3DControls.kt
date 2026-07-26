package com.gooludou.shadowplanner.presentation.mapbox3D

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinner
import com.gooludou.shadowplanner.presentation.components.MapRoundIconButton
import com.gooludou.shadowplanner.presentation.components.PitchSlider
import com.gooludou.shadowplanner.ui.theme.Map3DActionBlue
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
internal fun MapboxScene3DControls(
    mapViewportState: MapViewportState,
    selectedEpochMillis: Long,
    timeZoneId: String,
    location: GeoPoint,
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
    var isDateTimeVisible by rememberSaveable { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MapboxScene3DBottomControls(
            mapViewportState = mapViewportState,
            currentPitch = currentPitch,
            isTopDown = isTopDown,
            basemapStyle = basemapStyle,
            onToggleBasemapStyle = onToggleBasemapStyle,
            showDome = showDome,
            useMapboxBuildings = useMapboxBuildings,
            onToggleBuildingSource = onToggleBuildingSource,
            onToggleDome = onToggleDome,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            location = location,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            isDateTimeVisible = isDateTimeVisible,
            onToggleDateTime = { isDateTimeVisible = !isDateTimeVisible },
            onBackToMap = onBackToMap
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun BoxScope.MapboxScene3DBottomControls(
    mapViewportState: MapViewportState,
    currentPitch: Double,
    isTopDown: Boolean,
    basemapStyle: MapboxBasemapStyle,
    onToggleBasemapStyle: () -> Unit,
    showDome: Boolean,
    useMapboxBuildings: Boolean,
    onToggleBuildingSource: () -> Unit,
    onToggleDome: () -> Unit,
    selectedEpochMillis: Long,
    timeZoneId: String,
    location: GeoPoint,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    isDateTimeVisible: Boolean,
    onToggleDateTime: () -> Unit,
    onBackToMap: () -> Unit
) {
    PitchSlider(
        pitch = currentPitch.toFloat(),
        onPitchChange = { pitch ->
            mapViewportState.setCameraOptions {
                pitch(pitch.toDouble())
            }
        },
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
        ) {
            MapboxScene3DToggleButton(
                isSelected = false,
                contentDescription = stringResource(R.string.back_to_map),
                onClick = onBackToMap
            ) {
                Icon(Icons.Outlined.Map, contentDescription = null)
            }
            MapboxScene3DToggleButton(
                isSelected = basemapStyle == MapboxBasemapStyle.SATELLITE,
                contentDescription = stringResource(
                    if (basemapStyle == MapboxBasemapStyle.SATELLITE) {
                        R.string.standard_map_style
                    } else {
                        R.string.satellite_map_style
                    }
                ),
                onClick = onToggleBasemapStyle
            ) {
                Icon(Icons.Outlined.SatelliteAlt, contentDescription = null)
            }
            MapboxScene3DToggleButton(
                isSelected = showDome,
                contentDescription = stringResource(
                    if (showDome) R.string.hide_sky_overview else R.string.show_sky_overview
                ),
                onClick = onToggleDome
            ) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null)
            }
            MapboxScene3DToggleButton(
                isSelected = useMapboxBuildings,
                contentDescription = stringResource(
                    if (useMapboxBuildings) {
                        R.string.show_drawing_buildings
                    } else {
                        R.string.show_mapbox_buildings
                    }
                ),
                onClick = onToggleBuildingSource
            ) {
                Icon(Icons.Outlined.Apartment, contentDescription = null)
            }
            MapboxScene3DToggleButton(
                isSelected = isTopDown,
                contentDescription = stringResource(
                    if (isTopDown) R.string.view_3d_button else R.string.show_top_down_view
                ),
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
                }
            ) {
                Icon(Icons.Outlined.Map, contentDescription = null)
            }
            val dateTimeDescription = stringResource(
                if (isDateTimeVisible) R.string.hide_date_time else R.string.show_date_time
            )
            MapboxScene3DTooltip(tooltip = dateTimeDescription) {
                MapRoundIconButton(
                    onClick = onToggleDateTime,
                    contentDescription = dateTimeDescription
                ) {
                    Icon(
                        imageVector = if (isDateTimeVisible) {
                            Icons.Outlined.ExpandLess
                        } else {
                            Icons.Outlined.ExpandMore
                        },
                        contentDescription = null
                    )
                }
            }
        }
        AnimatedVisibility(visible = isDateTimeVisible) {
            DateTimeSpinner(
                selectedEpochMillis = selectedEpochMillis,
                timeZoneId = timeZoneId,
                location = location,
                onDateTimeChanged = onDateTimeChanged,
                onNowSelected = onNowSelected
            )
        }
    }
}

@Composable
private fun MapboxScene3DToggleButton(
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    MapboxScene3DTooltip(tooltip = contentDescription) {
        MapRoundIconButton(
            onClick = onClick,
            contentDescription = contentDescription,
            containerColor = if (isSelected) {
                Map3DActionBlue
            } else {
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.96f
                )
            },
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            border = if (isSelected) {
                null
            } else {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            }
        ) {
            icon()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapboxScene3DTooltip(tooltip: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        content()
    }
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
                location = GeoPoint(longitude = 153.0251, latitude = -27.4698),
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
