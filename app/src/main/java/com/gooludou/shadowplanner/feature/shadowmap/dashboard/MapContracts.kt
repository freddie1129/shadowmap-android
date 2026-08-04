package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.feature.shadowmap.components.AutoToolState
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView

internal data class MapControlsState(
    val dateTimeLocation: GeoPoint,
    val canRecenterCurrentLocation: Boolean,
    val displayMode: MapDisplayMode,
    val sceneMode: MapboxSceneMode,
    val autoToolState: AutoToolState,
    val hasCompletedEditingTooltips: Boolean,
    val hasCompletedMainViewTooltips: Boolean
)

internal data class MapControlsActions(
    val navigation: MapNavigationActions,
    val display: MapDisplayActions,
    val editing: MapEditingActions,
    val dateTime: MapDateTimeActions,
    val onEditingTooltipsCompleted: () -> Unit,
    val onMainViewTooltipsCompleted: () -> Unit
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
    val onDisplayModeChanged: (MapDisplayMode) -> Unit,
    val onRefreshSky: () -> Unit,
    val onOpenShadowColor: () -> Unit,
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
    val sceneMode: MapboxSceneMode,
    val autoToolState: AutoToolState,
    val canRecenterCurrentLocation: Boolean,
    val editingCrosshairPoint: GeoPoint?,
    val hasCompletedEditingTooltips: Boolean,
    val hasCompletedMainViewTooltips: Boolean
)

internal data class ShadowPlannerSceneActions(
    val navigation: MapNavigationActions,
    val editing: MapEditingActions,
    val dateTime: MapDateTimeActions,
    val onOpenShadowColor: () -> Unit,
    val onEditingTooltipsCompleted: () -> Unit,
    val onMainViewTooltipsCompleted: () -> Unit,
    val onSceneModeChanged: (MapboxSceneMode) -> Unit,
    val onSceneMapViewReady: (MapView) -> Unit,
    val onSceneMapClick: (Point) -> Boolean
)
