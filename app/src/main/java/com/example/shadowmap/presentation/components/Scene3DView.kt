package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.Building
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.SolarPosition
import com.example.shadowmap.scene.FilamentBuildingView
import com.example.shadowmap.scene.SceneCameraView
import com.example.shadowmap.scene.SceneViewport
import com.example.shadowmap.scene.SceneSkyPadding

@Composable
fun Scene3DView(
    buildings: List<Building>,
    walls: List<DrawnWall>,
    trees: List<DrawnTree>,
    viewport: SceneViewport?,
    azimuth: Float,
    zenith: Float,
    sunVisible: Boolean,
    sunPath: List<SolarPosition>,
    cameraView: SceneCameraView,
    showSatellite: Boolean,
    showSky: Boolean,
    onCameraViewChanged: (SceneCameraView) -> Unit,
    onToggleSatellite: () -> Unit,
    onToggleSky: () -> Unit,
    onBackToMap: () -> Unit,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val skyPadding = with(density) {
        SceneSkyPadding(
            leftPx = 24.dp.roundToPx(),
            topPx = 24.dp.roundToPx(),
            rightPx = 24.dp.roundToPx(),
            bottomPx = 176.dp.roundToPx()
        )
    }
    val sunPathWidthPx = with(density) { 4.dp.toPx() }
    Box(modifier = modifier.fillMaxSize()) {
        FilamentBuildingView(
            buildings = buildings,
            walls = walls,
            trees = trees,
            viewport = viewport,
            azimuth = azimuth,
            zenith = zenith,
            sunVisible = sunVisible,
            sunPath = sunPath,
            skyVisible = showSky,
            skyPadding = skyPadding,
            sunPathWidthPx = sunPathWidthPx,
            cameraView = cameraView,
            onCameraViewChanged = onCameraViewChanged,
            modifier = Modifier.fillMaxSize()
        )
        Scene3DControlsOverlay(
            cameraView = cameraView,
            showSatellite = showSatellite,
            showSky = showSky,
            onCameraViewChanged = {
                onCameraViewChanged(
                    if (cameraView == SceneCameraView.TOP_DOWN) {
                        SceneCameraView.ORBIT
                    } else {
                        SceneCameraView.TOP_DOWN
                    }
                )
            },
            onToggleSatellite = onToggleSatellite,
            onToggleSky = onToggleSky,
            onBackToMap = onBackToMap,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected
        )
    }
}
