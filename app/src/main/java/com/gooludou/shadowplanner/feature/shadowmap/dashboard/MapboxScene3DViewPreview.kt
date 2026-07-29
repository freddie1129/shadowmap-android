package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.solar.SolarPosition
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import com.gooludou.shadowplanner.feature.shadowmap.ShadowMapUiState
import com.gooludou.shadowplanner.feature.shadowmap.components.AutoToolState

@Preview(name = "Mapbox 3D view - light", showBackground = true)
@Composable
private fun MapboxScene3DViewLightPreview() {
    MapboxScene3DViewPreviewContent(darkTheme = false)
}

@Preview(name = "Mapbox 3D view - dark", showBackground = true)
@Composable
private fun MapboxScene3DViewDarkPreview() {
    MapboxScene3DViewPreviewContent(darkTheme = true)
}

@Composable
private fun MapboxScene3DViewPreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        ShadowPlannerSceneView(
            uiState = ShadowMapUiState(
                selectedEpochMillis = 1_752_640_000_000L,
                displayTimeZoneId = "Australia/Brisbane",
                calculationLocation = GeoPoint(153.0251, -27.4698),
                selectedLocationLabel = "Brisbane, Queensland",
                solarPosition = SolarPosition(azimuthDegrees = 315.0, zenithDegrees = 45.0),
                sunPath = listOf(
                    SolarPosition(azimuthDegrees = 80.0, zenithDegrees = 85.0),
                    SolarPosition(azimuthDegrees = 120.0, zenithDegrees = 55.0),
                    SolarPosition(azimuthDegrees = 180.0, zenithDegrees = 35.0),
                    SolarPosition(azimuthDegrees = 240.0, zenithDegrees = 55.0),
                    SolarPosition(azimuthDegrees = 280.0, zenithDegrees = 85.0)
                )
            ),
            state = ShadowPlannerSceneState(
                viewport = MapboxScene3DViewport(
                    center = GeoPoint(153.0251, -27.4698),
                    zoom = 17.0,
                    bearing = 0.0,
                    widthMeters = 120.0,
                    heightMeters = 240.0,
                    widthPixels = 1080,
                    heightPixels = 2400
                ),
                sceneMode = MapboxSceneMode.VIEW,
                autoToolState = AutoToolState.READY,
                canRecenterCurrentLocation = false,
                editingCrosshairPoint = null
            ),
            actions = previewShadowPlannerSceneActions()
        )
    }
}

private fun previewShadowPlannerSceneActions(): ShadowPlannerSceneActions =
    ShadowPlannerSceneActions(
        navigation = MapNavigationActions(
            onOpenSettings = {},
            onOpenLocationSearch = {},
            onShowLocationInfo = {},
            onRecenterCurrentLocation = {},
            onOpenProjects = {},
            onSaveProject = {}
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
        ),
        onOpenShadowColor = {},
        onSceneModeChanged = {},
        onSceneMapViewReady = {},
        onSceneMapClick = { false }
    )
