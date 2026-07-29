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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
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
import kotlin.math.abs

@Composable
@Suppress("LongMethod")
internal fun MapControls(
    mapViewportState: MapViewportState,
    uiState: ShadowMapUiState,
    state: MapControlsState,
    actions: MapControlsActions
) {
    val currentPitch = mapViewportState.cameraState?.pitch
        ?: state.displayMode.cameraPitchDegrees
    var isDateTimeVisible by rememberSaveable { mutableStateOf(true) }
    val showEditingToolbar = state.sceneMode == MapboxSceneMode.EDIT &&
        uiState.canShowMapboxEditingToolbar()
    LaunchedEffect(currentPitch) {
        if (abs(currentPitch - state.displayMode.cameraPitchDegrees) > CAMERA_PITCH_SYNC_EPSILON) {
            actions.display.onDisplayModeChanged(
                state.displayMode.copy(cameraPitchDegrees = currentPitch)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        MapTopControls(
            uiState = uiState,
            onOpenSettings = actions.navigation.onOpenSettings,
            onOpenLocationSearch = actions.navigation.onOpenLocationSearch,
            onShowLocationInfo = actions.navigation.onShowLocationInfo,
            onRecenterCurrentLocation = actions.navigation.onRecenterCurrentLocation,
            canRecenterCurrentLocation = state.canRecenterCurrentLocation,
            onOpenProjects = actions.navigation.onOpenProjects,
            onSaveProject = actions.navigation.onSaveProject,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        if (state.sceneMode == MapboxSceneMode.VIEW) {
            PitchSlider(
                pitch = currentPitch.toFloat(),
                onPitchChange = { pitch ->
                    actions.display.onDisplayModeChanged(
                        state.displayMode.copy(cameraPitchDegrees = pitch.toDouble())
                    )
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
            if (state.sceneMode == MapboxSceneMode.VIEW) {
                MapBottomControls(
                    displayMode = state.displayMode,
                    onDisplayModeChanged = actions.display.onDisplayModeChanged,
                    shadowAppearance = uiState.shadowAppearance,
                    onOpenShadowColor = actions.display.onOpenShadowColor,
                    onStartEditing = actions.display.onStartEditing
                )
            } else if (showEditingToolbar) {
                MapEditingControls(
                    uiState = uiState,
                    autoToolState = state.autoToolState,
                    isDateTimeVisible = isDateTimeVisible,
                    onOpenShadowColor = actions.display.onOpenShadowColor,
                    onDrawMode = actions.editing.onDrawMode,
                    onAutoLoad = actions.editing.onAutoLoad,
                    onClear = actions.editing.onClear,
                    onFinishEditing = actions.editing.onFinishEditing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ShadowMapDesign.dimensions.screenPadding)
                )
            }
            if (state.sceneMode == MapboxSceneMode.VIEW || showEditingToolbar) {
                MapDateTimeControls(
                    selectedEpochMillis = uiState.selectedEpochMillis,
                    timeZoneId = uiState.displayTimeZoneId,
                    location = state.dateTimeLocation,
                    isExpanded = isDateTimeVisible,
                    onExpandedChanged = { isDateTimeVisible = it },
                    onDateTimeChanged = actions.dateTime.onDateTimeChanged,
                    onNowSelected = actions.dateTime.onNowSelected
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
                uiState = ShadowMapUiState(
                    selectedEpochMillis = 1_752_640_000_000L,
                    displayTimeZoneId = "Australia/Brisbane",
                    calculationLocation = GeoPoint(153.0251, -27.4698)
                ),
                state = MapControlsState(
                    dateTimeLocation = GeoPoint(153.0251, -27.4698),
                    canRecenterCurrentLocation = true,
                    displayMode = MapDisplayMode(
                        basemapStyle = MapboxBasemapStyle.SATELLITE,
                        isDomeVisible = true,
                        content = SceneBuildingSelection.DRAWN,
                        cameraPitchDegrees = Scene3DCamera.ORBIT_PITCH_DEGREES
                    ),
                    sceneMode = sceneMode,
                    autoToolState = AutoToolState.READY
                ),
                actions = previewMapControlsActions()
            )
        }
    }
}

private fun previewMapControlsActions(): MapControlsActions = MapControlsActions(
    navigation = MapNavigationActions(
        onOpenSettings = {},
        onOpenLocationSearch = {},
        onShowLocationInfo = {},
        onRecenterCurrentLocation = {},
        onOpenProjects = {},
        onSaveProject = {}
    ),
    display = MapDisplayActions(
        onDisplayModeChanged = {},
        onOpenShadowColor = {},
        onStartEditing = {}
    ),
    editing = MapEditingActions(
        onFinishEditing = {},
        onDrawMode = {},
        onAutoLoad = {},
        onClear = {}
    ),
    dateTime = MapDateTimeActions(
        onDateTimeChanged = {},
        onNowSelected = {}
    )
)

private const val CAMERA_PITCH_SYNC_EPSILON = 0.01
