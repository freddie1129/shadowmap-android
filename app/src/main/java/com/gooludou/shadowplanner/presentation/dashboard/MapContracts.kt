package com.gooludou.shadowplanner.presentation.dashboard

import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView

internal data class MapControlsState(
    val dateTimeLocation: GeoPoint,
    val canRecenterCurrentLocation: Boolean,
    val basemapStyle: MapboxBasemapStyle,
    val showDome: Boolean,
    val buildingSelection: SceneBuildingSelection,
    val sceneMode: MapboxSceneMode,
    val autoToolState: AutoToolState
)

internal data class MapControlsActions(
    val navigation: MapNavigationActions,
    val display: MapDisplayActions,
    val editing: MapEditingActions,
    val dateTime: MapDateTimeActions
)

internal data class MapNavigationActions(
    val onOpenSettings: () -> Unit,
    val onOpenLocationSearch: () -> Unit,
    val onShowLocationInfo: () -> Unit,
    val onRecenterCurrentLocation: () -> Unit,
    val onOpenProjects: () -> Unit,
    val onSaveProject: () -> Unit
)

internal data class MapDisplayActions(
    val onBasemapStyleSelected: (MapboxBasemapStyle) -> Unit,
    val onBuildingSelectionChanged: (SceneBuildingSelection) -> Unit,
    val onOpenShadowColor: () -> Unit,
    val onToggleDome: () -> Unit,
    val onStartEditing: () -> Unit
)

internal data class MapEditingActions(
    val onFinishEditing: () -> Unit,
    val onDrawMode: (DrawMode) -> Unit,
    val onAutoLoad: () -> Unit,
    val onClear: () -> Unit
)

internal data class MapDateTimeActions(
    val onDateTimeChanged: (Long) -> Unit,
    val onNowSelected: () -> Unit
)

internal data class ShadowPlannerSceneState(
    val viewport: MapboxScene3DViewport,
    val showDome: Boolean,
    val sceneMode: MapboxSceneMode,
    val autoToolState: AutoToolState,
    val canRecenterCurrentLocation: Boolean,
    val editingCrosshairPoint: GeoPoint?
)

internal data class ShadowPlannerSceneActions(
    val navigation: MapNavigationActions,
    val editing: MapEditingActions,
    val dateTime: MapDateTimeActions,
    val onOpenShadowColor: () -> Unit,
    val onSceneModeChanged: (MapboxSceneMode) -> Unit,
    val onSceneMapViewReady: (MapView) -> Unit,
    val onSceneMapClick: (Point) -> Boolean,
    val onToggleDome: () -> Unit
)
