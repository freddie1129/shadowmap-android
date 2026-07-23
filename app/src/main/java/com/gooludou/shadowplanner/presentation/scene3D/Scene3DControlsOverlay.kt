package com.gooludou.shadowplanner.presentation.scene3D

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinner
import com.gooludou.shadowplanner.presentation.components.SceneViewSwitchButton
import com.gooludou.shadowplanner.scene.SceneCameraView
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun Scene3DControlsOverlay(
    cameraView: SceneCameraView,
    showSatellite: Boolean,
    showSky: Boolean,
    onCameraViewChanged: (SceneCameraView) -> Unit,
    onToggleSatellite: () -> Unit,
    onToggleSky: () -> Unit,
    onRefreshSky: () -> Unit,
    onBackToMap: () -> Unit,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        SceneViewSwitchButton(
            label = stringResource(R.string.map_view),
            icon = Icons.Outlined.Map,
            onClick = onBackToMap,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Scene3DControls(
            cameraView = cameraView,
            showSatellite = showSatellite,
            showSky = showSky,
            onToggleCameraView = {
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
            onRefreshSky = onRefreshSky,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = ShadowMapDesign.dimensions.screenPadding,
                    bottom = THREE_D_BOTTOM_CONTROL_CLEARANCE
                )
        )
        DateTimeSpinner(
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(name = "3D controls with time", showBackground = true)
@Composable
private fun Scene3DControlsOverlayPreview() {
    ShadowMapTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Scene3DControlsOverlay(
                cameraView = SceneCameraView.ORBIT,
                showSatellite = true,
                showSky = true,
                onCameraViewChanged = {},
                onToggleSatellite = {},
                onToggleSky = {},
                onRefreshSky = {},
                onBackToMap = {},
                selectedEpochMillis = 1_752_640_000_000L,
                timeZoneId = "Australia/Brisbane",
                onDateTimeChanged = {},
                onNowSelected = {}
            )
        }
    }
}

private val THREE_D_BOTTOM_CONTROL_CLEARANCE = 156.dp
