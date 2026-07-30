package com.gooludou.shadowplanner.feature.shadowmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.purchase.model.EntitlementState
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController

@Composable
internal fun ShadowMapRoute(
    mapControllerFactory: MapboxShadowMapController.Factory,
    entitlementState: EntitlementState,
    onPremiumRequired: () -> Unit,
    modifier: Modifier = Modifier,
    pendingLocation: LocationSearchResult? = null,
    pendingProjectId: String? = null,
    onLocationApplied: () -> Unit = {},
    onProjectApplied: () -> Unit = {},
    onOpenLocationSearch: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ShadowMapViewModel = hiltViewModel(),
    locationPermissionViewModel: LocationPermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasRequestedLocationPermission by locationPermissionViewModel
        .hasRequestedLocationPermission
        .collectAsStateWithLifecycle()
    LaunchedEffect(pendingLocation) {
        val latitude = pendingLocation?.latitude
        val longitude = pendingLocation?.longitude
        if (latitude != null && longitude != null) {
            viewModel.onLocationSelected(
                location = GeoPoint(latitude = latitude, longitude = longitude),
                label = pendingLocation.address.ifBlank { pendingLocation.name }
            )
            onLocationApplied()
        }
    }
    LaunchedEffect(pendingProjectId) {
        pendingProjectId?.let {
            viewModel.loadProject(it)
            onProjectApplied()
        }
    }
    ShadowMapScreen(
        uiState = uiState,
        entitlementState = entitlementState,
        onPremiumRequired = onPremiumRequired,
        dependencies = MapScreenDependencies(mapControllerFactory),
        actions = ShadowMapActions(
            map = MapActions(
                onDateTimeChanged = viewModel::onDateTimeChanged,
                onNowSelected = viewModel::onNowSelected,
                onLoadStarted = viewModel::onBuildingLoadStarted,
                onBuildingsLoaded = viewModel::onBuildingsLoaded,
                onLoadFailed = viewModel::onBuildingLoadFailed,
                onViewportChanged = viewModel::onViewportChanged,
                onShadowAppearanceChanged = viewModel::onShadowAppearanceChanged,
                onCurrentLocationReceived = viewModel::onCurrentLocationReceived
            ),
            drawing = DrawingActions(
                onSelectDrawMode = viewModel::selectDrawMode,
                onStopDrawing = viewModel::stopDrawing,
                onAddVertex = viewModel::addVertex,
                onUndo = viewModel::undoLastVertex,
                onDrawingError = viewModel::setDrawingError,
                onFinishBuilding = viewModel::finishBuilding,
                onFinishWall = viewModel::finishWall,
                onStartTree = viewModel::startTree,
                onReturnPendingToDrawing = viewModel::returnPendingToDrawing,
                onCommitPendingDrawing = viewModel::commitPendingDrawing,
                onUpdateSelectedDrawing = viewModel::updateSelectedDrawing,
                onDeleteSelectedDrawing = viewModel::deleteSelectedDrawing,
                onRestoreDeletedObject = viewModel::restoreLastDeletedObject,
                onSelectDrawing = viewModel::selectDrawing,
                onStartMoving = viewModel::startMoving,
                onMoveSelectedObject = viewModel::moveSelectedObject,
                onFinishMoving = viewModel::finishMoving,
                onCancelMoving = viewModel::cancelMoving
            ),
            scene = SceneActions(
                onClearScene = viewModel::clearScene,
                onRestoreClearedScene = viewModel::restoreClearedScene
            ),
            project = ProjectActions(
                onSaveProject = viewModel::saveProject,
                onSaveProjectAsNew = viewModel::saveProjectAsNew
            )
        ),
        navigation = ShadowMapNavigation(
            onOpenProjects = onOpenProjects,
            onOpenLocationSearch = onOpenLocationSearch,
            onOpenSettings = onOpenSettings
        ),
        hasRequestedLocationPermission = hasRequestedLocationPermission,
        onLocationPermissionRequested =
            locationPermissionViewModel::markLocationPermissionRequested,
        modifier = modifier
    )
}
