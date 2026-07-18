package com.example.shadowmap.presentation

import com.example.shadowmap.domain.Building
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnObjectSelection
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.location.LocationSearchResult
import com.example.shadowmap.map.MapboxShadowMapController
import com.example.shadowmap.project.ProjectViewport

data class MapScreenDependencies(
    val mapControllerFactory: MapboxShadowMapController.Factory
)

data class ShadowMapNavigation(
    val pendingLocation: LocationSearchResult?,
    val onLocationApplied: () -> Unit,
    val onOpenProjects: () -> Unit,
    val onOpenLocationSearch: () -> Unit,
    val onOpenSettings: () -> Unit
)

data class MapActions(
    val onDateTimeChanged: (Long) -> Unit,
    val onNowSelected: () -> Unit,
    val onLoadStarted: () -> Unit,
    val onBuildingsLoaded: (List<Building>, GeoPoint) -> Unit,
    val onLoadFailed: (Throwable) -> Unit,
    val onViewportChanged: (ProjectViewport) -> Unit
)

data class DrawingActions(
    val onSelectDrawMode: (DrawMode) -> Boolean,
    val onStopDrawing: () -> Unit,
    val onAddVertex: (GeoPoint) -> Unit,
    val onUndo: () -> Unit,
    val onDrawingError: (String) -> Unit,
    val onFinishBuilding: () -> Boolean,
    val onFinishWall: () -> Boolean,
    val onStartTree: (GeoPoint) -> Unit,
    val onReturnPendingToDrawing: () -> Unit,
    val onCommitPendingDrawing: (Double, Double?) -> Unit,
    val onUpdateSelectedDrawing: (Double, Double?) -> Unit,
    val onDeleteSelectedDrawing: () -> Boolean,
    val onRestoreDeletedObject: () -> Unit,
    val onSelectDrawing: (DrawnObjectSelection?) -> Unit
)

data class SceneActions(
    val onClearScene: () -> Unit,
    val onRestoreClearedScene: () -> Unit
)

data class ProjectActions(
    val onSaveProject: (String?) -> Unit
)

data class ShadowMapActions(
    val map: MapActions,
    val drawing: DrawingActions,
    val scene: SceneActions,
    val project: ProjectActions
)
