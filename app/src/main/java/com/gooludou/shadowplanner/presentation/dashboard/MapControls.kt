package com.gooludou.shadowplanner.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.ShadowAppearance
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinner
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinnerCollapsed
import com.gooludou.shadowplanner.presentation.components.MapToolBar
import com.gooludou.shadowplanner.presentation.components.PitchSlider
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@Composable
@Suppress("LongMethod")
internal fun MapControls(
    mapViewportState: MapViewportState,
    selectedEpochMillis: Long,
    timeZoneId: String,
    location: GeoPoint,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    onRecenterCurrentLocation: () -> Unit,
    canRecenterCurrentLocation: Boolean,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    basemapStyle: MapboxBasemapStyle,
    onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit,
    showDome: Boolean,
    buildingSelection: SceneBuildingSelection,
    onBuildingSelectionChanged: (SceneBuildingSelection) -> Unit,
    shadowAppearance: ShadowAppearance,
    onOpenShadowColor: () -> Unit,
    onToggleDome: () -> Unit,
    sceneMode: MapboxSceneMode,
    uiState: ShadowMapUiState,
    autoToolState: AutoToolState,
    onStartEditing: () -> Unit,
    onFinishEditing: () -> Unit,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit
) {
    val currentPitch = mapViewportState.cameraState?.pitch ?: Scene3DCamera.ORBIT_PITCH_DEGREES
    val isTopDown = currentPitch <= Scene3DCamera.TOP_DOWN_THRESHOLD_DEGREES
    var isDateTimeVisible by rememberSaveable { mutableStateOf(true) }
    val showEditingToolbar = sceneMode == MapboxSceneMode.EDIT &&
        uiState.canShowMapboxEditingToolbar()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MapTopControls(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onOpenLocationSearch = onOpenLocationSearch,
            onShowLocationInfo = onShowLocationInfo,
            onRecenterCurrentLocation = onRecenterCurrentLocation,
            canRecenterCurrentLocation = canRecenterCurrentLocation,
            isSatelliteMap = basemapStyle == MapboxBasemapStyle.SATELLITE,
            onToggleMapStyle = {
                onBasemapStyleSelected(
                    when (basemapStyle) {
                        MapboxBasemapStyle.STANDARD -> MapboxBasemapStyle.SATELLITE
                        MapboxBasemapStyle.SATELLITE -> MapboxBasemapStyle.STANDARD
                    }
                )
            },
            onOpenProjects = onOpenProjects,
            onSaveProject = onSaveProject,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        if (sceneMode == MapboxSceneMode.VIEW) {
            PitchSlider(
                pitch = currentPitch.toFloat(),
                onPitchChange = { pitch ->
                    mapViewportState.setCameraOptions {
                        pitch(pitch.toDouble())
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                ShadowMapDesign.dimensions.spacingSmall
            )
        ) {
            if (sceneMode == MapboxSceneMode.VIEW) {
                MapBottomControls(
                    mapViewportState = mapViewportState,
                    isTopDown = isTopDown,
                    basemapStyle = basemapStyle,
                    onBasemapStyleSelected = onBasemapStyleSelected,
                    showDome = showDome,
                    buildingSelection = buildingSelection,
                    onBuildingSelectionChanged = onBuildingSelectionChanged,
                    shadowAppearance = shadowAppearance,
                    onOpenShadowColor = onOpenShadowColor,
                    onToggleDome = onToggleDome,
                    onStartEditing = onStartEditing
                )
            } else if (showEditingToolbar) {
                MapEditingControls(
                    uiState = uiState,
                    autoToolState = autoToolState,
                    isDateTimeVisible = isDateTimeVisible,
                    onOpenShadowColor = onOpenShadowColor,
                    onDrawMode = onDrawMode,
                    onAutoLoad = onAutoLoad,
                    onClear = onClear,
                    onFinishEditing = onFinishEditing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ShadowMapDesign.dimensions.screenPadding)
                )
            }
            if (sceneMode == MapboxSceneMode.VIEW || showEditingToolbar) {
                MapDateTimeControls(
                    selectedEpochMillis = selectedEpochMillis,
                    timeZoneId = timeZoneId,
                    location = location,
                    isExpanded = isDateTimeVisible,
                    onExpandedChanged = { isDateTimeVisible = it },
                    onDateTimeChanged = onDateTimeChanged,
                    onNowSelected = onNowSelected
                )
            }
        }
    }
}

@Composable
private fun MapDateTimeControls(
    selectedEpochMillis: Long,
    timeZoneId: String,
    location: GeoPoint,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit
) {
    AnimatedContent(
        targetState = isExpanded,
        label = "date-time-spinner"
    ) { expanded ->
        if (expanded) {
            DateTimeSpinner(
                selectedEpochMillis = selectedEpochMillis,
                timeZoneId = timeZoneId,
                location = location,
                onCollapse = { onExpandedChanged(false) },
                onDateTimeChanged = onDateTimeChanged,
                onNowSelected = onNowSelected
            )
        } else {
            DateTimeSpinnerCollapsed(
                selectedEpochMillis = selectedEpochMillis,
                timeZoneId = timeZoneId,
                onExpand = { onExpandedChanged(true) },
                location = location
            )
        }
    }
}

private fun ShadowMapUiState.canShowMapboxEditingToolbar(): Boolean = when {
    activeDrawMode != null -> false
    pendingDrawing != null -> false
    selectedDrawing != null -> false
    moveSession != null -> false
    else -> true
}

@Composable
private fun MapEditingControls(
    uiState: ShadowMapUiState,
    autoToolState: AutoToolState,
    isDateTimeVisible: Boolean,
    onOpenShadowColor: () -> Unit,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit,
    onFinishEditing: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapToolBar(
        autoState = autoToolState,
        hasSceneObjects = uiState.hasSceneObjects,
        isTimeVisible = isDateTimeVisible,
        shadowAppearance = uiState.shadowAppearance,
        onDrawMode = onDrawMode,
        onAutoLoad = onAutoLoad,
        onClear = onClear,
        onToggleTime = {},
        onOpenShadowColor = onOpenShadowColor,
        onOpenMapbox3D = onFinishEditing,
        modifier = modifier,
        showTimeToggle = false
    )
}










@Preview(name = "Mapbox 3D controls - light", showBackground = true)
@Composable
private fun MapboxScene3DControlsLightPreview() {
    MapboxScene3DControlsPreviewContent(
        darkTheme = false,
        sceneMode = MapboxSceneMode.VIEW
    )
}

@Preview(name = "Mapbox 3D controls - dark", showBackground = true)
@Composable
private fun MapboxScene3DControlsDarkPreview() {
    MapboxScene3DControlsPreviewContent(
        darkTheme = true,
        sceneMode = MapboxSceneMode.VIEW
    )
}

@Preview(name = "Mapbox editing controls - light", showBackground = true)
@Composable
private fun MapboxScene3DEditingControlsLightPreview() {
    MapboxScene3DControlsPreviewContent(
        darkTheme = false,
        sceneMode = MapboxSceneMode.EDIT
    )
}

@Preview(name = "Mapbox editing controls - dark", showBackground = true)
@Composable
private fun MapboxScene3DEditingControlsDarkPreview() {
    MapboxScene3DControlsPreviewContent(
        darkTheme = true,
        sceneMode = MapboxSceneMode.EDIT
    )
}

@Composable
private fun MapboxScene3DControlsPreviewContent(darkTheme: Boolean, sceneMode: MapboxSceneMode) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            val mapViewportState = rememberMapViewportState()
            MapControls(
                mapViewportState = mapViewportState,
                selectedEpochMillis = 1_752_640_000_000L,
                timeZoneId = "Australia/Brisbane",
                location = GeoPoint(longitude = 153.0251, latitude = -27.4698),
                onDateTimeChanged = {},
                onNowSelected = {},
                onOpenSettings = {},
                onOpenLocationSearch = {},
                onShowLocationInfo = {},
                onRecenterCurrentLocation = {},
                canRecenterCurrentLocation = true,
                onOpenProjects = {},
                onSaveProject = {},
                basemapStyle = MapboxBasemapStyle.SATELLITE,
                onBasemapStyleSelected = {},
                showDome = true,
                buildingSelection = SceneBuildingSelection.DRAWN,
                onBuildingSelectionChanged = {},
                shadowAppearance = ShadowAppearance.DEFAULT,
                onOpenShadowColor = {},
                onToggleDome = {},
                sceneMode = sceneMode,
                uiState = ShadowMapUiState(
                    selectedEpochMillis = 1_752_640_000_000L,
                    displayTimeZoneId = "Australia/Brisbane",
                    calculationLocation = GeoPoint(153.0251, -27.4698)
                ),
                autoToolState = AutoToolState.READY,
                onStartEditing = {},
                onFinishEditing = {},
                onDrawMode = {},
                onAutoLoad = {},
                onClear = {}
            )
        }
    }
}
