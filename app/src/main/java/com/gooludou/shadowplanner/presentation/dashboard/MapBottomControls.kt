package com.gooludou.shadowplanner.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.ShadowAppearance
import com.gooludou.shadowplanner.presentation.components.MapRoundIconButton
import com.gooludou.shadowplanner.presentation.components.ShadowColorButton
import com.gooludou.shadowplanner.ui.theme.Map3DActionBlue
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign

@Composable
@Suppress("LongMethod")
internal fun MapBottomControls(
    displayMode: MapDisplayMode,
    onDisplayModeChanged: (MapDisplayMode) -> Unit,
    shadowAppearance: ShadowAppearance,
    onOpenShadowColor: () -> Unit,
    onStartEditing: () -> Unit
) {
    val isTopDown = displayMode.cameraMode == MapCameraMode.TOP_DOWN
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
            MapboxScene3DModeButton(
                onStartEditing = onStartEditing
            )
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
                onClick = onOpenShadowColor
            )
            MapboxScene3DBasemapSelectionButton(
                basemapStyle = displayMode.basemapStyle,
                onBasemapStyleSelected = { selectedStyle ->
                    onDisplayModeChanged(displayMode.copy(basemapStyle = selectedStyle))
                }
            )
            MapboxScene3DToggleButton(
                isSelected = displayMode.isDomeVisible,
                contentDescription = stringResource(
                    if (displayMode.isDomeVisible) {
                        R.string.hide_sky_overview
                    } else {
                        R.string.show_sky_overview
                    }
                ),
                onClick = {
                    onDisplayModeChanged(
                        displayMode.copy(isDomeVisible = !displayMode.isDomeVisible)
                    )
                }
            ) {
                Icon(Icons.Outlined.WbSunny, contentDescription = null)
            }
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
                menuTitle = buildingsTitle,
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
                }
            ) {
                Icon(Icons.Outlined.Apartment, contentDescription = null)
            }
            Spacer(modifier = Modifier.Companion.weight(1f))
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
                    onDisplayModeChanged(
                        displayMode.copy(
                            cameraPitchDegrees = if (isTopDown) {
                                Scene3DCamera.ORBIT_PITCH_DEGREES
                            } else {
                                Scene3DCamera.TOP_DOWN_PITCH_DEGREES
                            }
                        )
                    )
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
        }
    }
}

@Composable
fun MapboxScene3DToggleButton(
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
private fun MapboxScene3DModeButton(onStartEditing: () -> Unit) {
    MapboxScene3DToggleButton(
        isSelected = true,
        contentDescription = stringResource(R.string.edit),
        onClick = onStartEditing
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
    }
}
