package com.gooludou.shadowplanner.feature.shadowmap

import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.core.model.TreeCrownShape
import com.gooludou.shadowplanner.project.ProjectViewport
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController

data class MapScreenDependencies(val mapControllerFactory: MapboxShadowMapController.Factory)

data class ShadowMapNavigation(
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
    val onShadowAppearanceChanged: (ShadowAppearance) -> Unit,
    val onCurrentLocationReceived: (GeoPoint, String) -> Unit
)

data class DrawingActions(
    val onSelectDrawMode: (DrawMode) -> Boolean,
    val onStopDrawing: () -> Unit,
    val onAddVertex: (GeoPoint) -> Unit,
    val onUndo: () -> Unit,
    val onDrawingError: (String) -> Unit,
    val onFinishBuilding: (GeoPoint) -> Boolean,
    val onFinishWall: (GeoPoint) -> Boolean,
    val onStartTree: (GeoPoint) -> Unit,
    val onReturnPendingToDrawing: () -> Unit,
    val onCommitPendingDrawing: (Double, Double?, TreeCrownShape) -> Unit,
    val onUpdateSelectedDrawing: (Double, Double?, TreeCrownShape) -> Unit,
    val onDeleteSelectedDrawing: () -> Boolean,
    val onRestoreDeletedObject: () -> Unit,
    val onSelectDrawing: (DrawnObjectSelection?) -> Unit,
    val onStartMoving: () -> Unit,
    val onMoveSelectedObject: (Double, Double) -> Unit,
    val onFinishMoving: () -> Unit,
    val onCancelMoving: () -> Unit
)

data class SceneActions(val onClearScene: () -> Unit, val onRestoreClearedScene: () -> Unit)

data class ProjectActions(
    val onSaveProject: (String?) -> Unit,
    val onSaveProjectAsNew: (String) -> Unit
)

data class ShadowMapActions(
    val map: MapActions,
    val drawing: DrawingActions,
    val scene: SceneActions,
    val project: ProjectActions
)
