package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PanoramaPhotosphere
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.Config
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.core.ui.components.MapRoundIconButton
import com.gooludou.shadowplanner.core.ui.theme.Map3DActionBlue
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import com.gooludou.shadowplanner.feature.shadowmap.components.ShadowColorButton

@Composable
@Suppress("LongMethod")
internal fun MapBottomControls(
    displayMode: MapDisplayMode,
    onDisplayModeChanged: (MapDisplayMode) -> Unit,
    onRefreshSky: () -> Unit,
    shadowAppearance: ShadowAppearance,
    onOpenShadowColor: () -> Unit,
    onStartEditing: () -> Unit,
    hasDrawings: Boolean,
    onTooltipTargetBoundsChanged: (MainViewTooltipTarget, Rect) -> Unit = { _, _ -> }
) {
    var showEmptyDrawingsDialog by rememberSaveable { mutableStateOf(false) }
    val isTopDown = displayMode.cameraMode == MapCameraMode.TOP_DOWN
    val topDownLabel = stringResource(R.string.top_down_view)
    val threeDimensionalLabel = stringResource(R.string.view_3d_button)
    val cameraDescription = stringResource(
        R.string.setting_current_value,
        stringResource(R.string.camera_view),
        if (isTopDown) topDownLabel else threeDimensionalLabel
    )
    Column(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            MapboxScene3DToggleButton(
                isSelected = displayMode.skyDisplayMode != SkyDisplayMode.HIDDEN,
                contentDescription = stringResource(
                    when (displayMode.skyDisplayMode) {
                        SkyDisplayMode.FULL -> R.string.hide_sky_overview
                        SkyDisplayMode.HIDDEN -> R.string.show_sky_overlays
                        SkyDisplayMode.OVERLAYS_ONLY -> R.string.show_sky_overview
                    }
                ),
                onClick = {
                    onDisplayModeChanged(
                        displayMode.copy(skyDisplayMode = displayMode.skyDisplayMode.next())
                    )
                },
                showSlashWhenUnselected = false,
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.SKY_DISPLAY.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.SKY_DISPLAY, bounds)
                }
            ) {
                Icon(
                    imageVector = when (displayMode.skyDisplayMode) {
                        SkyDisplayMode.FULL -> Icons.Outlined.PanoramaPhotosphere
                        SkyDisplayMode.HIDDEN -> Icons.Outlined.VisibilityOff
                        SkyDisplayMode.OVERLAYS_ONLY -> Icons.Outlined.Explore
                    },
                    contentDescription = null
                )
            }
        }
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            MapboxScene3DToggleButton(
                isSelected = true,
                contentDescription = stringResource(R.string.refresh_sky_overview),
                onClick = onRefreshSky,
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.REFRESH_SKY.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.REFRESH_SKY, bounds)
                }
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
        }
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(horizontal = ShadowMapDesign.dimensions.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingSmall),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            ShadowColorButton(
                appearance = shadowAppearance,
                onClick = onOpenShadowColor,
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.SHADOW_COLOUR.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.SHADOW_COLOUR, bounds)
                }
            )
            MapboxScene3DBasemapSelectionButton(
                basemapStyle = displayMode.basemapStyle,
                onBasemapStyleSelected = { selectedStyle ->
                    onDisplayModeChanged(displayMode.copy(basemapStyle = selectedStyle))
                },
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.BASEMAP.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.BASEMAP, bounds)
                }
            )
            val buildingsTitle = stringResource(R.string.map_display)
            val drawingBuildingsLabel = stringResource(R.string.my_drawings)
            val mapboxBuildingsLabel = stringResource(R.string.built_in_map_buildings)
            val buildingOptions = listOf(
                drawingBuildingsLabel,
                mapboxBuildingsLabel
            )
            val selectedBuildingsIndex = displayMode.content.menuIndex
            MapboxScene3DSelectionButton(
                isSelected = displayMode.content == SceneBuildingSelection.MAPBOX,
                contentDescription = stringResource(
                    R.string.setting_current_value,
                    buildingsTitle,
                    buildingOptions[selectedBuildingsIndex]
                ),
                options = buildingOptions,
                selectedOptionIndex = selectedBuildingsIndex,
                onOptionSelected = { index ->
                    val selectedContent = SceneBuildingSelection.fromMenuIndex(index)
                    onDisplayModeChanged(
                        displayMode.copy(
                            content = selectedContent,
                            basemapStyle = basemapStyleAfterBuildingSelection(
                                buildingSelection = selectedContent,
                                currentBasemapStyle = displayMode.basemapStyle
                            )
                        )
                    )
                    if (selectedContent == SceneBuildingSelection.DRAWN && !hasDrawings) {
                        showEmptyDrawingsDialog = true
                    }
                },
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.MAP_DISPLAY.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.MAP_DISPLAY, bounds)
                }
            ) {
                Icon(
                    imageVector = if (displayMode.content == SceneBuildingSelection.DRAWN) {
                        Icons.Outlined.Home
                    } else {
                        Icons.Outlined.Apartment
                    },
                    contentDescription = null
                )
            }
            MapboxScene3DToggleButton(
                isSelected = !isTopDown,
                contentDescription = cameraDescription,
                onClick = {
                    onDisplayModeChanged(
                        displayMode.copy(
                            cameraPitchDegrees = if (isTopDown) {
                                Config.DEFAULT_3D_PITCH_DEGREES
                            } else {
                                Scene3DCamera.TOP_DOWN_PITCH_DEGREES
                            }
                        )
                    )
                },
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.CAMERA.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.CAMERA, bounds)
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
            Spacer(modifier = Modifier.Companion.weight(1f))
            MapboxScene3DModeButton(
                onStartEditing = onStartEditing,
                modifier = Modifier.reportFeatureTourTarget(
                    MainViewTooltipTarget.EDIT.name
                ) { _, bounds ->
                    onTooltipTargetBoundsChanged(MainViewTooltipTarget.EDIT, bounds)
                }
            )
        }
    }
    if (showEmptyDrawingsDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDrawingsDialog = false },
            title = { Text(stringResource(R.string.no_drawings_yet_title)) },
            text = { Text(stringResource(R.string.no_drawings_yet_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyDrawingsDialog = false
                        onStartEditing()
                    }
                ) {
                    Text(stringResource(R.string.add_drawing))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDrawingsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun MapboxScene3DToggleButton(
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSlashWhenUnselected: Boolean = false,
    icon: @Composable () -> Unit
) {
    MapboxScene3DTooltip(tooltip = contentDescription) {
        MapRoundIconButton(
            modifier = modifier,
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
            showIconSlash = showSlashWhenUnselected && !isSelected,
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
fun MapboxScene3DTooltip(tooltip: String, content: @Composable () -> Unit) {
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

@Composable
fun MapboxScene3DSelectionButton(
    isSelected: Boolean,
    contentDescription: String,
    options: List<String>,
    selectedOptionIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Box {
        MapboxScene3DToggleButton(
            isSelected = isSelected,
            contentDescription = contentDescription,
            onClick = { isMenuExpanded = true },
            modifier = modifier,
            icon = icon
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.widthIn(min = 208.dp)
        ) {
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
private fun MapboxScene3DBasemapSelectionButton(
    basemapStyle: MapboxBasemapStyle,
    onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit,
    modifier: Modifier = Modifier
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
        options = options,
        selectedOptionIndex = selectedIndex,
        onOptionSelected = { index ->
            onBasemapStyleSelected(
                if (index == 0) MapboxBasemapStyle.STANDARD else MapboxBasemapStyle.SATELLITE
            )
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (basemapStyle == MapboxBasemapStyle.STANDARD) {
                Icons.Outlined.Map
            } else {
                Icons.Outlined.SatelliteAlt
            },
            contentDescription = null
        )
    }
}

@Composable
private fun MapboxScene3DModeButton(onStartEditing: () -> Unit, modifier: Modifier = Modifier) {
    MapboxScene3DToggleButton(
        isSelected = true,
        contentDescription = stringResource(R.string.edit),
        onClick = onStartEditing,
        modifier = modifier
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
    }
}

@Preview(
    name = "Map bottom controls",
    widthDp = 400,
    heightDp = 176,
    showBackground = true,
    backgroundColor = 0xFF6B8064
)
@Composable
private fun MapBottomControlsLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        MapBottomControlsPreviewContent(
            backgroundColor = Color(0xFF6B8064),
            skyDisplayMode = SkyDisplayMode.FULL
        )
    }
}

@Preview(
    name = "Map bottom controls (dark)",
    widthDp = 400,
    heightDp = 176,
    showBackground = true,
    backgroundColor = 0xFF263238
)
@Composable
private fun MapBottomControlsDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        MapBottomControlsPreviewContent(
            backgroundColor = Color(0xFF263238),
            skyDisplayMode = SkyDisplayMode.HIDDEN
        )
    }
}

@Composable
private fun MapBottomControlsPreviewContent(
    backgroundColor: Color,
    skyDisplayMode: SkyDisplayMode
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.BottomCenter
    ) {
        MapBottomControls(
            displayMode = MapDisplayMode(
                basemapStyle = MapboxBasemapStyle.STANDARD,
                skyDisplayMode = skyDisplayMode,
                content = SceneBuildingSelection.MAPBOX,
                cameraPitchDegrees = Config.DEFAULT_3D_PITCH_DEGREES
            ),
            onDisplayModeChanged = {},
            onRefreshSky = {},
            shadowAppearance = ShadowAppearance.DEFAULT,
            onOpenShadowColor = {},
            onStartEditing = {},
            hasDrawings = true
        )
    }
}
