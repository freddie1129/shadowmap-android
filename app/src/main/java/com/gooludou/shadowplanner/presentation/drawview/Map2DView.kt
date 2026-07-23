package com.gooludou.shadowplanner.presentation.drawview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.BuildingSource
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.GeoPolygon
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun Map2DView(
    uiState: ShadowMapUiState,
    autoToolState: AutoToolState,
    isTimeVisible: Boolean,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onToggleTime: () -> Unit,
    onOpenShadowColor: () -> Unit,
    onDrawMode: (DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpen3D: () -> Unit,
    onOpenMapbox3D: () -> Unit,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Map2DTopControls(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onOpenLocationSearch = onOpenLocationSearch,
            onShowLocationInfo = onShowLocationInfo,
            onOpen3D = onOpen3D,
            onOpenMapbox3D = onOpenMapbox3D,
            onOpenProjects = onOpenProjects,
            onSaveProject = onSaveProject,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Map2DBottomControls(
            uiState = uiState,
            autoToolState = autoToolState,
            isTimeVisible = isTimeVisible,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            onToggleTime = onToggleTime,
            onOpenShadowColor = onOpenShadowColor,
            onDrawMode = onDrawMode,
            onAutoLoad = onAutoLoad,
            onClear = onClear,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(name = "2D map view", showBackground = true, backgroundColor = 0xFF6B8064)
@Composable
private fun Map2DViewLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Map2DViewPreview()
    }
}

@Preview(name = "2D map view (dark)", showBackground = true, backgroundColor = 0xFF263238)
@Composable
private fun Map2DViewDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Map2DViewPreview()
    }
}

@Composable
private fun Map2DViewPreview() {
    Map2DView(
        uiState = ShadowMapUiState(
            selectedEpochMillis = 1_752_640_000_000L,
            displayTimeZoneId = "Australia/Brisbane",
            calculationLocation = GeoPoint(153.0251, -27.4698),
            selectedLocationLabel = "Brisbane, Queensland",
            drawnBuildings = listOf(
                Building(
                    id = "preview-building",
                    polygon = GeoPolygon(
                        listOf(
                            listOf(
                                GeoPoint(153.025, -27.469),
                                GeoPoint(153.026, -27.469),
                                GeoPoint(153.026, -27.470),
                                GeoPoint(153.025, -27.470)
                            )
                        )
                    ),
                    heightMeters = 6.0,
                    source = BuildingSource.MANUAL
                )
            )
        ),
        autoToolState = AutoToolState.READY,
        isTimeVisible = true,
        onDateTimeChanged = {},
        onNowSelected = {},
        onToggleTime = {},
        onOpenShadowColor = {},
        onDrawMode = {},
        onAutoLoad = {},
        onClear = {},
        onOpenSettings = {},
        onOpen3D = {},
        onOpenMapbox3D = {},
        onOpenProjects = {},
        onSaveProject = {},
        onOpenLocationSearch = {},
        onShowLocationInfo = {}
    )
}
