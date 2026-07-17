package com.example.shadowmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.presentation.ShadowMapUiState
import com.example.shadowmap.ui.theme.ShadowMapDesign
import com.example.shadowmap.ui.theme.ShadowMapTheme

@Composable
fun Map2DView(
    uiState: ShadowMapUiState,
    autoToolState: AutoToolState,
    isTimeVisible: Boolean,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onToggleTime: () -> Unit,
    onDrawMode: (com.example.shadowmap.domain.DrawMode) -> Unit,
    onAutoLoad: () -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpen3D: () -> Unit,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Box(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = dimensions.spacingSmall),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.spacingLarge),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconButton(onClick = onOpenSettings)
                LocationSearchEntry(
                    label = uiState.selectedLocationLabel,
                    onClick = onOpenLocationSearch,
                    onInfoClick = onShowLocationInfo,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensions.spacingSmall,
                        start = dimensions.spacingLarge,
                        end = dimensions.spacingLarge
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.hasSceneObjects) {
                    SceneViewSwitchButton(
                        label = "3D view",
                        icon = Icons.Outlined.ViewInAr,
                        onClick = onOpen3D
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                MapRoundIconButton(onClick = onOpenProjects, contentDescription = "Open projects") {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                }
                MapRoundIconButton(onClick = onSaveProject, contentDescription = "Save project") {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapToolBar(
                autoState = autoToolState,
                hasSceneObjects = uiState.hasSceneObjects,
                isTimeVisible = isTimeVisible,
                onDrawMode = onDrawMode,
                onAutoLoad = onAutoLoad,
                onClear = onClear,
                onToggleTime = onToggleTime,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            androidx.compose.animation.AnimatedVisibility(visible = isTimeVisible) {
                DateTimeSpinner(
                    selectedEpochMillis = uiState.selectedEpochMillis,
                    timeZoneId = uiState.displayTimeZoneId,
                    onDateTimeChanged = onDateTimeChanged,
                    onNowSelected = onNowSelected
                )
            }
        }
    }
}

@Preview(name = "2D map view", showBackground = true, backgroundColor = 0xFF6B8064)
@Composable
private fun Map2DViewPreview() {
    ShadowMapTheme(dynamicColor = false) {
        Map2DView(
            uiState = ShadowMapUiState(
                selectedEpochMillis = 1_752_640_000_000L,
                displayTimeZoneId = "Australia/Brisbane",
                calculationLocation = GeoPoint(153.0251, -27.4698),
                selectedLocationLabel = "Brisbane, Queensland",
                drawnBuildings = listOf(
                    DrawnBuilding(
                        polygon = GeoPolygon(
                            listOf(
                                listOf(
                                    GeoPoint(153.025, -27.469),
                                    GeoPoint(153.026, -27.469),
                                    GeoPoint(153.026, -27.470),
                                    GeoPoint(153.025, -27.470)
                                )
                            )
                        )
                    )
                )
            ),
            autoToolState = AutoToolState.READY,
            isTimeVisible = false,
            onDateTimeChanged = {},
            onNowSelected = {},
            onToggleTime = {},
            onDrawMode = {},
            onAutoLoad = {},
            onClear = {},
            onOpenSettings = {},
            onOpen3D = {},
            onOpenProjects = {},
            onSaveProject = {},
            onOpenLocationSearch = {},
            onShowLocationInfo = {}
        )
    }
}
