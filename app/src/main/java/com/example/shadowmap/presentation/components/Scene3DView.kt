package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.shadowmap.domain.Building
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.scene.FilamentBuildingView
import com.example.shadowmap.scene.SceneCameraView
import com.example.shadowmap.scene.SceneViewport

@Composable
fun Scene3DView(
    buildings: List<Building>,
    walls: List<DrawnWall>,
    trees: List<DrawnTree>,
    viewport: SceneViewport?,
    azimuth: Float,
    zenith: Float,
    sunVisible: Boolean,
    cameraView: SceneCameraView,
    showSatellite: Boolean,
    onCameraViewChanged: (SceneCameraView) -> Unit,
    onToggleSatellite: () -> Unit,
    onBackToMap: () -> Unit,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        FilamentBuildingView(
            buildings = buildings,
            walls = walls,
            trees = trees,
            viewport = viewport,
            azimuth = azimuth,
            zenith = zenith,
            sunVisible = sunVisible,
            cameraView = cameraView,
            onCameraViewChanged = onCameraViewChanged,
            modifier = Modifier.fillMaxSize()
        )
        Scene3DControlsOverlay(
            cameraView = cameraView,
            showSatellite = showSatellite,
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
            onBackToMap = onBackToMap,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected
        )
    }
}
