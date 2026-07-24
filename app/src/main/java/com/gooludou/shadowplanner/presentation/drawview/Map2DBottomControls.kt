package com.gooludou.shadowplanner.presentation.drawview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinner
import com.gooludou.shadowplanner.presentation.components.MapToolBar
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun Map2DBottomControls(
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
    onOpenMapbox3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        MapToolBar(
            autoState = autoToolState,
            hasSceneObjects = uiState.hasSceneObjects,
            isTimeVisible = isTimeVisible,
            shadowAppearance = uiState.shadowAppearance,
            onDrawMode = onDrawMode,
            onAutoLoad = onAutoLoad,
            onClear = onClear,
            onToggleTime = onToggleTime,
            onOpenShadowColor = onOpenShadowColor,
            onOpenMapbox3D = onOpenMapbox3D,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(horizontal = 8.dp)
        )
        AnimatedVisibility(visible = isTimeVisible) {
            DateTimeSpinner(
                selectedEpochMillis = uiState.selectedEpochMillis,
                timeZoneId = uiState.displayTimeZoneId,
                location = uiState.calculationLocation,
                onDateTimeChanged = onDateTimeChanged,
                onNowSelected = onNowSelected
            )
        }
    }
}

@Preview(name = "Map bottom controls", showBackground = true, backgroundColor = 0xFF6B8064)
@Composable
private fun Map2DBottomControlsLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Map2DBottomControlsPreview()
    }
}

@Preview(name = "Map bottom controls (dark)", showBackground = true, backgroundColor = 0xFF263238)
@Composable
private fun Map2DBottomControlsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Map2DBottomControlsPreview()
    }
}

@Composable
private fun Map2DBottomControlsPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6B8064)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Map2DBottomControls(
            uiState = ShadowMapUiState(
                selectedEpochMillis = 1_752_640_000_000L,
                displayTimeZoneId = "Australia/Brisbane",
                calculationLocation = GeoPoint(longitude = 153.0251, latitude = -27.4698)
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
            onOpenMapbox3D = {}
        )
    }
}
