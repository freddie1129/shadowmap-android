package com.gooludou.shadowplanner.presentation

import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.DrawnObjectSelection
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.ShadowAppearance
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.map.MapboxShadowMapController
import com.gooludou.shadowplanner.project.ProjectViewport

data class MapScreenDependencies(val mapControllerFactory: MapboxShadowMapController.Factory)

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
    val onViewportChanged: (ProjectViewport) -> Unit,
    val onShadowAppearanceChanged: (ShadowAppearance) -> Unit
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
    val onSelectDrawing: (DrawnObjectSelection?) -> Unit,
    val onStartMoving: () -> Unit,
    val onMoveSelectedObject: (Double, Double) -> Unit,
    val onFinishMoving: () -> Unit,
    val onCancelMoving: () -> Unit
)

data class SceneActions(val onClearScene: () -> Unit, val onRestoreClearedScene: () -> Unit)

data class ProjectActions(val onSaveProject: (String?) -> Unit)

data class ShadowMapActions(
    val map: MapActions,
    val drawing: DrawingActions,
    val scene: SceneActions,
    val project: ProjectActions
)
