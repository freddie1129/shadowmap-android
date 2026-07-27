package com.gooludou.shadowplanner.presentation.mapbox3D

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.ShadowAppearance
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinner
import com.gooludou.shadowplanner.presentation.components.DateTimeSpinnerCollapsed
import com.gooludou.shadowplanner.presentation.components.MapRoundIconButton
import com.gooludou.shadowplanner.presentation.components.MapToolBar
import com.gooludou.shadowplanner.presentation.components.PitchSlider
import com.gooludou.shadowplanner.presentation.components.ShadowColorButton
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
    onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit,
    showDome: Boolean,
    buildingSelection: SceneBuildingSelection,
    onBuildingSelectionChanged: (SceneBuildingSelection) -> Unit,
    shadowAppearance: ShadowAppearance,
    showShadowColorControl: Boolean,
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
                MapboxScene3DBottomControls(
                    mapViewportState = mapViewportState,
                    isTopDown = isTopDown,
                    basemapStyle = basemapStyle,
                    onBasemapStyleSelected = onBasemapStyleSelected,
                    showDome = showDome,
                    buildingSelection = buildingSelection,
                    onBuildingSelectionChanged = onBuildingSelectionChanged,
                    shadowAppearance = shadowAppearance,
                    showShadowColorControl = showShadowColorControl,
                    onOpenShadowColor = onOpenShadowColor,
                    onToggleDome = onToggleDome,
                    onStartEditing = onStartEditing
                )
            } else if (showEditingToolbar) {
                MapboxScene3DEditingControls(
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
                MapboxSceneDateTimeDisclosure(
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
private fun MapboxSceneDateTimeDisclosure(
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
                onExpand = { onExpandedChanged(true) }
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
private fun MapboxScene3DEditingControls(
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

@Composable
@Suppress("LongMethod")
private fun MapboxScene3DBottomControls(
    mapViewportState: MapViewportState,
    isTopDown: Boolean,
    basemapStyle: MapboxBasemapStyle,
    onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit,
    showDome: Boolean,
    buildingSelection: SceneBuildingSelection,
    onBuildingSelectionChanged: (SceneBuildingSelection) -> Unit,
    shadowAppearance: ShadowAppearance,
    showShadowColorControl: Boolean,
    onOpenShadowColor: () -> Unit,
    onToggleDome: () -> Unit,
    onStartEditing: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = showShadowColorControl) {
                ShadowColorButton(
                    appearance = shadowAppearance,
                    onClick = onOpenShadowColor
                )
            }
            if (buildingSelection.allowsBasemapSelection) {
                MapboxScene3DBasemapSelectionButton(
                    basemapStyle = basemapStyle,
                    onBasemapStyleSelected = onBasemapStyleSelected
                )
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
            val buildingsTitle = stringResource(R.string.buildings)
            val drawingBuildingsLabel = stringResource(R.string.drawing_buildings)
            val mapboxBuildingsLabel = stringResource(R.string.mapbox_buildings)
            val buildingOptions = listOf(
                drawingBuildingsLabel,
                mapboxBuildingsLabel
            )
            val selectedBuildingsIndex = buildingSelection.menuIndex
            MapboxScene3DSelectionButton(
                isSelected = buildingSelection == SceneBuildingSelection.MAPBOX,
                contentDescription = stringResource(
                    R.string.setting_current_value,
                    buildingsTitle,
                    buildingOptions[selectedBuildingsIndex]
                ),
                menuTitle = buildingsTitle,
                options = buildingOptions,
                selectedOptionIndex = selectedBuildingsIndex,
                onOptionSelected = { index ->
                    onBuildingSelectionChanged(
                        SceneBuildingSelection.fromMenuIndex(index)
                    )
                }
            ) {
                Icon(Icons.Outlined.Apartment, contentDescription = null)
            }
            val topDownLabel = stringResource(R.string.top_down_view)
            val threeDimensionalLabel = stringResource(R.string.view_3d_button)
            val cameraDescription = stringResource(
                R.string.setting_current_value,
                stringResource(R.string.camera_view),
                if (isTopDown) topDownLabel else threeDimensionalLabel
            )
            MapboxScene3DToggleButton(
                isSelected = !isTopDown,
                contentDescription = cameraDescription,
                onClick = {
                    mapViewportState.setCameraOptions {
                        pitch(
                            if (isTopDown) Scene3DCamera.ORBIT_PITCH_DEGREES
                            else Scene3DCamera.TOP_DOWN_PITCH_DEGREES
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (isTopDown) {
                            R.drawable.two_d_2_24dp
                        } else {
                            R.drawable.three_d_2_24dp
                        }
                    ),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            MapboxScene3DModeButton(
                onStartEditing = onStartEditing
            )
        }
    }
}

@Composable
private fun MapboxScene3DModeButton(
    onStartEditing: () -> Unit
) {
    MapboxScene3DToggleButton(
        isSelected = true,
        contentDescription = stringResource(R.string.edit),
        onClick = onStartEditing,
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
    }
}

@Composable
private fun MapboxScene3DBasemapSelectionButton(
    basemapStyle: MapboxBasemapStyle,
    onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit
) {
    val menuTitle = stringResource(R.string.basemap)
    val options = listOf(
        stringResource(R.string.standard_map_style),
        stringResource(R.string.satellite_map_style)
    )
    val selectedIndex = if (basemapStyle == MapboxBasemapStyle.STANDARD) 0 else 1
    MapboxScene3DSelectionButton(
        isSelected = basemapStyle == MapboxBasemapStyle.SATELLITE,
        contentDescription = stringResource(
            R.string.setting_current_value,
            menuTitle,
            options[selectedIndex]
        ),
        menuTitle = menuTitle,
        options = options,
        selectedOptionIndex = selectedIndex,
        onOptionSelected = { index ->
            onBasemapStyleSelected(
                if (index == 0) MapboxBasemapStyle.STANDARD else MapboxBasemapStyle.SATELLITE
            )
        }
    ) {
        Icon(Icons.Outlined.SatelliteAlt, contentDescription = null)
    }
}

@Composable
private fun MapboxScene3DSelectionButton(
    isSelected: Boolean,
    contentDescription: String,
    menuTitle: String,
    options: List<String>,
    selectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit,
    icon: @Composable () -> Unit
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Box {
        MapboxScene3DToggleButton(
            isSelected = isSelected,
            contentDescription = contentDescription,
            onClick = { isMenuExpanded = true },
            icon = icon
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.widthIn(min = 208.dp)
        ) {
            Text(
                text = menuTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = ShadowMapDesign.dimensions.screenPadding,
                    vertical = ShadowMapDesign.dimensions.spacingSmall
                )
            )
            HorizontalDivider()
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        isMenuExpanded = false
                        onOptionSelected(index)
                    },
                    trailingIcon = {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == selectedOptionIndex) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = stringResource(R.string.selected)
                                )
                            }
                        }
                    }
                )
            }
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
private fun MapboxScene3DControlsPreviewContent(
    darkTheme: Boolean,
    sceneMode: MapboxSceneMode
) {
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
                onBasemapStyleSelected = {},
                showDome = true,
                buildingSelection = SceneBuildingSelection.DRAWN,
                onBuildingSelectionChanged = {},
                shadowAppearance = ShadowAppearance.DEFAULT,
                showShadowColorControl = true,
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
