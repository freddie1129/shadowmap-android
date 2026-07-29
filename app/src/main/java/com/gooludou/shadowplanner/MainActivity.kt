package com.gooludou.shadowplanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.model.DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
import com.gooludou.shadowplanner.core.model.DEFAULT_DRAWN_TREE_HEIGHT_METERS
import com.gooludou.shadowplanner.core.model.DEFAULT_DRAWN_TREE_RADIUS_METERS
import com.gooludou.shadowplanner.core.model.DEFAULT_DRAWN_WALL_HEIGHT_METERS
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.DrawnObjectType
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.PendingDrawing
import com.gooludou.shadowplanner.core.model.SceneObjectSource
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.renderer.filament.SceneViewport
import com.gooludou.shadowplanner.renderer.mapbox.BuildingLoadArea
import com.gooludou.shadowplanner.renderer.mapbox.BuildingLoadType
import com.gooludou.shadowplanner.renderer.mapbox.MapboxShadowMapController
import com.gooludou.shadowplanner.app.navigation.AppNavigation
import com.gooludou.shadowplanner.presentation.BuildingLoadState
import com.gooludou.shadowplanner.presentation.DrawingActions
import com.gooludou.shadowplanner.presentation.MapActions
import com.gooludou.shadowplanner.presentation.MapScreenDependencies
import com.gooludou.shadowplanner.presentation.ProjectActions
import com.gooludou.shadowplanner.presentation.SceneActions
import com.gooludou.shadowplanner.presentation.ShadowMapActions
import com.gooludou.shadowplanner.presentation.ShadowMapNavigation
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.ShadowMapViewModel
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.presentation.dashboard.MapDateTimeActions
import com.gooludou.shadowplanner.presentation.dashboard.MapEditingActions
import com.gooludou.shadowplanner.presentation.dashboard.MapNavigationActions
import com.gooludou.shadowplanner.presentation.dashboard.MapboxScene3DViewport
import com.gooludou.shadowplanner.presentation.dashboard.MapboxSceneMode
import com.gooludou.shadowplanner.presentation.dashboard.ShadowPlannerSceneActions
import com.gooludou.shadowplanner.presentation.dashboard.ShadowPlannerSceneState
import com.gooludou.shadowplanner.presentation.dashboard.ShadowPlannerSceneView
import com.gooludou.shadowplanner.presentation.dashboard.currentScene3DViewport
import com.gooludou.shadowplanner.presentation.drawview.ActiveDrawingControls
import com.gooludou.shadowplanner.presentation.drawview.DrawingCrosshair
import com.gooludou.shadowplanner.presentation.drawview.DrawingPropertiesSheet
import com.gooludou.shadowplanner.presentation.drawview.ShadowColorSheet
import com.gooludou.shadowplanner.presentation.locationsearch.SelectedLocationSheet
import com.gooludou.shadowplanner.presentation.projectview.SaveProjectDialog
import com.gooludou.shadowplanner.project.ProjectViewport
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val FALLBACK_MAPBOX_SCENE_VIEWPORT = MapboxScene3DViewport(
    center = GeoPoint(
        longitude = Config.FALLBACK_MAP_CENTER_LONGITUDE,
        latitude = Config.FALLBACK_MAP_CENTER_LATITUDE
    ),
    zoom = Config.FALLBACK_MAP_ZOOM,
    bearing = Config.FALLBACK_MAP_BEARING_DEGREES,
    widthMeters = Config.MAPBOX_3D_FALLBACK_WIDTH_METERS,
    heightMeters = Config.MAPBOX_3D_FALLBACK_HEIGHT_METERS,
    widthPixels = 1,
    heightPixels = 1
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mapControllerFactory: MapboxShadowMapController.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            ShadowMapTheme {
                AppNavigation(
                    mapControllerFactory = mapControllerFactory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun ShadowMapRoute(
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier,
    pendingLocation: LocationSearchResult? = null,
    pendingProjectId: String? = null,
    onLocationApplied: () -> Unit = {},
    onProjectApplied: () -> Unit = {},
    onOpenLocationSearch: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ShadowMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLocation) {
        val latitude = pendingLocation?.latitude
        val longitude = pendingLocation?.longitude
        if (latitude != null && longitude != null) {
            viewModel.onLocationSelected(
                location = GeoPoint(latitude = latitude, longitude = longitude),
                label = pendingLocation.address.ifBlank { pendingLocation.name }
            )
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
            pendingLocation = pendingLocation,
            onLocationApplied = onLocationApplied,
            onOpenProjects = onOpenProjects,
            onOpenLocationSearch = onOpenLocationSearch,
            onOpenSettings = onOpenSettings
        ),
        modifier = modifier
    )
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun ShadowMapScreen(
    uiState: ShadowMapUiState,
    dependencies: MapScreenDependencies,
    actions: ShadowMapActions,
    navigation: ShadowMapNavigation,
    modifier: Modifier = Modifier
) {
    val noBuildingsFoundMessage = stringResource(R.string.no_buildings_found)
    val loadingBuildingsMessage = stringResource(R.string.loading_buildings)
    val checkingMapAreaMessage = stringResource(R.string.checking_visible_map_area)
    val zoomInLoadMessage = stringResource(R.string.zoom_in_load_buildings)
    val zoomLoadAction = stringResource(R.string.zoom_load)
    val moveFartherMessage = stringResource(R.string.move_farther_previous_point)
    val objectDeletedMessage = stringResource(R.string.object_deleted)
    val undoMessage = stringResource(R.string.undo)
    val sceneClearedMessage = stringResource(R.string.scene_cleared)
    val onDateTimeChanged = actions.map.onDateTimeChanged
    val onNowSelected = actions.map.onNowSelected
    val onLoadStarted = actions.map.onLoadStarted
    val onBuildingsLoaded = actions.map.onBuildingsLoaded
    val onLoadFailed = actions.map.onLoadFailed
    val onViewportChanged = actions.map.onViewportChanged
    val onShadowAppearanceChanged = actions.map.onShadowAppearanceChanged
    val onCurrentLocationReceived = actions.map.onCurrentLocationReceived
    val onSelectDrawMode = actions.drawing.onSelectDrawMode
    val onStopDrawing = actions.drawing.onStopDrawing
    val onAddVertex = actions.drawing.onAddVertex
    val onUndo = actions.drawing.onUndo
    val onDrawingError = actions.drawing.onDrawingError
    val onFinishBuilding = actions.drawing.onFinishBuilding
    val onFinishWall = actions.drawing.onFinishWall
    val onStartTree = actions.drawing.onStartTree
    val onReturnPendingToDrawing = actions.drawing.onReturnPendingToDrawing
    val onCommitPendingDrawing = actions.drawing.onCommitPendingDrawing
    val onUpdateSelectedDrawing = actions.drawing.onUpdateSelectedDrawing
    val onDeleteSelectedDrawing = actions.drawing.onDeleteSelectedDrawing
    val onRestoreDeletedObject = actions.drawing.onRestoreDeletedObject
    val onSelectDrawing = actions.drawing.onSelectDrawing
    val onStartMoving = actions.drawing.onStartMoving
    val onMoveSelectedObject = actions.drawing.onMoveSelectedObject
    val onFinishMoving = actions.drawing.onFinishMoving
    val onCancelMoving = actions.drawing.onCancelMoving
    val onClearScene = actions.scene.onClearScene
    val onRestoreClearedScene = actions.scene.onRestoreClearedScene
    val onSaveProject = actions.project.onSaveProject
    val onSaveProjectAsNew = actions.project.onSaveProjectAsNew
    val mapControllerFactory = dependencies.mapControllerFactory
    val pendingLocation = navigation.pendingLocation
    val onLocationApplied = navigation.onLocationApplied
    val onOpenProjects = navigation.onOpenProjects
    val onOpenLocationSearch = navigation.onOpenLocationSearch
    val onOpenSettings = navigation.onOpenSettings
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = LocalResources.current
    val density = LocalDensity.current
    val dimensions = ShadowMapDesign.dimensions
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(
                Point.fromLngLat(
                    Config.FALLBACK_MAP_CENTER_LONGITUDE,
                    Config.FALLBACK_MAP_CENTER_LATITUDE
                )
            )
            zoom(Config.FALLBACK_MAP_ZOOM)
            bearing(Config.FALLBACK_MAP_BEARING_DEGREES)
        }
    }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val controller = remember(mapView, mapControllerFactory) {
        mapView?.let(mapControllerFactory::create)
    }
    var mapboxSceneMapView by remember { mutableStateOf<MapView?>(null) }
    val mapboxSceneController = remember(mapboxSceneMapView, mapControllerFactory) {
        mapboxSceneMapView?.let(mapControllerFactory::create)
    }
    var satelliteSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var loadRequest by remember { mutableIntStateOf(0) }
    var buildingQueryLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var mapboxSceneMode by remember { mutableStateOf(MapboxSceneMode.VIEW) }
    LaunchedEffect(uiState.projectLoadRevision) {
        if (uiState.projectLoadRevision == 0L) return@LaunchedEffect
        mapboxSceneMode = MapboxSceneMode.VIEW
    }
    var mapboxSceneViewport by remember { mutableStateOf<MapboxScene3DViewport?>(null) }
    LaunchedEffect(pendingLocation) {
        val latitude = pendingLocation?.latitude ?: return@LaunchedEffect
        val longitude = pendingLocation.longitude ?: return@LaunchedEffect
        val center = Point.fromLngLat(longitude, latitude)
        mapViewportState.setCameraOptions {
            center(center)
            zoom(15.0)
        }
        mapboxSceneViewport = (mapboxSceneViewport ?: FALLBACK_MAPBOX_SCENE_VIEWPORT).copy(
            center = GeoPoint(longitude, latitude),
            zoom = 15.0
        )
        onLocationApplied()
    }
    LaunchedEffect(uiState.projectLoadRevision) {
        if (uiState.projectLoadRevision == 0L) return@LaunchedEffect
        val projectViewport = uiState.viewport ?: return@LaunchedEffect
        val center = Point.fromLngLat(
            projectViewport.center.longitude,
            projectViewport.center.latitude
        )
        mapViewportState.setCameraOptions {
            center(center)
            zoom(projectViewport.zoom)
            bearing(projectViewport.bearing)
        }
        mapboxSceneViewport = (mapboxSceneViewport ?: FALLBACK_MAPBOX_SCENE_VIEWPORT).copy(
            center = projectViewport.center,
            zoom = projectViewport.zoom,
            bearing = projectViewport.bearing
        )
    }
    LaunchedEffect(mapView) {
        withFrameNanos { }
        mapboxSceneViewport = mapView?.currentScene3DViewport(
            fallback = FALLBACK_MAPBOX_SCENE_VIEWPORT
        ) ?: FALLBACK_MAPBOX_SCENE_VIEWPORT
    }
    var buildingLoadArea by remember { mutableStateOf<BuildingLoadArea?>(null) }
    var crosshairPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var autoLoadAfterZoom by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDiscardDraftConfirmation by remember { mutableStateOf(false) }
    var exitEditingAfterDiscard by remember { mutableStateOf(false) }
    var currentLocationPoint by remember { mutableStateOf<Point?>(null) }
    var showSelectedLocationSheet by remember { mutableStateOf(false) }
    var showShadowColorSheet by remember { mutableStateOf(false) }
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var projectNameDraft by remember(uiState.activeProjectName) {
        mutableStateOf(uiState.activeProjectName.orEmpty())
    }
    val currentLocationLabel = stringResource(R.string.current_location)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(mapView, mapboxSceneMapView) {
        val currentMapView = mapView
        if (currentMapView == null || mapboxSceneMapView != null) {
            buildingLoadArea = null
            onDispose { }
        } else {
            fun updateBuildingLoadArea() {
                currentMapView.post {
                    buildingLoadArea = currentMapView.toBuildingLoadArea()
                    val center = currentMapView.mapboxMap.cameraState.center
                    currentMapView.toProjectViewport()?.let(onViewportChanged)
                }
            }

            updateBuildingLoadArea()
            val mapIdleSubscription = currentMapView.mapboxMap.subscribeMapIdle {
                updateBuildingLoadArea()
            }
            fun updateCrosshairPoint() {
                val point = currentMapView.mapboxMap.cameraState.center
                crosshairPoint = GeoPoint(point.longitude(), point.latitude())
            }
            fun updateMapboxSceneViewport() {
                mapboxSceneViewport = currentMapView.currentScene3DViewport(
                    fallback = mapboxSceneViewport ?: FALLBACK_MAPBOX_SCENE_VIEWPORT
                ) ?: mapboxSceneViewport ?: FALLBACK_MAPBOX_SCENE_VIEWPORT
            }
            updateCrosshairPoint()
            updateMapboxSceneViewport()
            val cameraSubscription = currentMapView.mapboxMap.subscribeCameraChanged {
                updateCrosshairPoint()
                updateMapboxSceneViewport()
            }
            onDispose {
                mapIdleSubscription.cancel()
                cameraSubscription.cancel()
            }
        }
    }

    DisposableEffect(mapboxSceneMapView, mapboxSceneMode) {
        val currentMapView = mapboxSceneMapView
        if (currentMapView == null) {
            onDispose { }
        } else {
            fun updateEditingCrosshair() {
                if (mapboxSceneMode != MapboxSceneMode.EDIT) return
                currentMapView.post {
                    val camera = currentMapView.mapboxMap.cameraState
                    crosshairPoint = GeoPoint(
                        camera.center.longitude(),
                        camera.center.latitude()
                    )
                }
            }
            fun updateSettledSceneViewport() {
                currentMapView.post {
                    currentMapView.toProjectViewport()?.let(onViewportChanged)
                    if (mapboxSceneMode == MapboxSceneMode.EDIT) {
                        buildingLoadArea = currentMapView.toBuildingLoadArea()
                    }
                }
            }
            updateEditingCrosshair()
            updateSettledSceneViewport()
            val cameraSubscription = currentMapView.mapboxMap.subscribeCameraChanged {
                updateEditingCrosshair()
            }
            val mapIdleSubscription = currentMapView.mapboxMap.subscribeMapIdle {
                updateEditingCrosshair()
                updateSettledSceneViewport()
            }
            onDispose {
                cameraSubscription.cancel()
                mapIdleSubscription.cancel()
            }
        }
    }

    DisposableEffect(mapView, hasLocationPermission) {
        val currentMapView = mapView
        if (currentMapView == null || !hasLocationPermission) {
            onDispose { }
        } else {
            val locationComponent = currentMapView.location
            var firstLocationReceived = false
            val positionListener = OnIndicatorPositionChangedListener { point ->
                currentLocationPoint = point
                if (!firstLocationReceived) {
                    firstLocationReceived = true
                    mapViewportState.setCameraOptions {
                        center(point)
                        zoom(Config.DEVICE_LOCATION_MAP_ZOOM)
                        bearing(Config.FALLBACK_MAP_BEARING_DEGREES)
                    }
                    mapboxSceneViewport = (
                        mapboxSceneViewport ?: FALLBACK_MAPBOX_SCENE_VIEWPORT
                        ).copy(
                        center = GeoPoint(point.longitude(), point.latitude()),
                        zoom = Config.DEVICE_LOCATION_MAP_ZOOM,
                        bearing = Config.FALLBACK_MAP_BEARING_DEGREES
                    )
                    onCurrentLocationReceived(
                        GeoPoint(point.longitude(), point.latitude()),
                        currentLocationLabel
                    )
                }
            }
            locationComponent.updateSettings {
                enabled = true
                locationPuck = LocationPuck2D(opacity = 0f)
                pulsingEnabled = false
                showAccuracyRing = false
            }
            locationComponent.addOnIndicatorPositionChangedListener(positionListener)
            onDispose {
                locationComponent.removeOnIndicatorPositionChangedListener(positionListener)
            }
        }
    }

    DisposableEffect(satelliteSnapshot) {
        val snapshot = satelliteSnapshot
        onDispose {
            if (snapshot != null && !snapshot.isRecycled) snapshot.recycle()
        }
    }

    val buildingController = if (mapboxSceneMode == MapboxSceneMode.EDIT) {
        mapboxSceneController
    } else {
        controller
    }
    LaunchedEffect(loadRequest, buildingController) {
        if (loadRequest == 0 || buildingController == null) return@LaunchedEffect
        val calculationLocation = buildingQueryLocation ?: return@LaunchedEffect
        withFrameNanos { }
        try {
            val result = runCatching {
                buildingController.fetchBuildings(Config.BUILDING_LOAD_TYPE)
            }
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure
            result.fold(
                onSuccess = { buildings ->
                    onBuildingsLoaded(buildings, calculationLocation)
                    snackbarHostState.showSnackbar(
                        message = if (buildings.isEmpty()) {
                            noBuildingsFoundMessage
                        } else {
                            resources.getQuantityString(
                                R.plurals.buildings_loaded,
                                buildings.size,
                                buildings.size
                            )
                        },
                        duration = SnackbarDuration.Short
                    )
                },
                onFailure = { throwable ->
                    onLoadFailed(throwable)
                    snackbarHostState.showSnackbar(
                        message = throwable.message ?: loadingBuildingsMessage,
                        duration = SnackbarDuration.Short
                    )
                }
            )
        } finally {
            satelliteSnapshot = null
        }
    }

    LaunchedEffect(controller, uiState, crosshairPoint, buildingLoadArea) {
        if (controller != null) {
            controller.render(
                loadedBuildings = uiState.visibleLoadedBuildings,
                drawnBuildings = uiState.drawnBuildings,
                drawnWalls = uiState.drawnWalls,
                drawnTrees = uiState.drawnTrees,
                selection = uiState.selectedDrawing,
                activeDrawMode = uiState.activeDrawMode,
                inProgressVertices = uiState.inProgressVertices,
                pendingDrawing = uiState.pendingDrawing,
                crosshairPoint = crosshairPoint,
                shadows = uiState.shadows,
                shadowAppearance = uiState.shadowAppearance
            )
        }
    }

    fun startBuildingLoad() {
        val currentMapView = if (mapboxSceneMode == MapboxSceneMode.EDIT) {
            mapboxSceneMapView
        } else {
            mapView
        } ?: return
        val currentLoadArea = currentMapView.toBuildingLoadArea()
        buildingLoadArea = currentLoadArea
        if (Config.BUILDING_LOAD_TYPE == BuildingLoadType.ALL &&
            currentLoadArea?.isWithinLimit != true
        ) {
            return
        }
        val mapCenter = currentMapView.mapboxMap.cameraState.center
        buildingQueryLocation = GeoPoint(
            longitude = mapCenter.longitude(),
            latitude = mapCenter.latitude()
        )
        onLoadStarted()
        currentMapView.snapshot { bitmap ->
            currentMapView.post {
                satelliteSnapshot = bitmap
                loadRequest++
            }
        }
    }

    fun zoomToValidAreaAndLoad() {
        val currentMapView = if (mapboxSceneMode == MapboxSceneMode.EDIT) {
            mapboxSceneMapView
        } else {
            mapView
        } ?: return
        val area = buildingLoadArea ?: return
        val largestDimension = max(area.widthMeters, area.heightMeters).toDouble()
        val zoomIncrease = if (largestDimension > MAX_AUTO_LOAD_METERS) {
            ln(largestDimension / MAX_AUTO_LOAD_METERS) / ln(2.0) + AUTO_LOAD_ZOOM_PADDING
        } else {
            0.0
        }
        val camera = currentMapView.mapboxMap.cameraState
        if (mapboxSceneMode == MapboxSceneMode.EDIT) {
            currentMapView.mapboxMap.setCamera(
                com.mapbox.maps.CameraOptions.Builder()
                    .center(camera.center)
                    .zoom(camera.zoom + zoomIncrease)
                    .build()
            )
        } else {
            mapViewportState.setCameraOptions {
                center(camera.center)
                zoom(camera.zoom + zoomIncrease)
            }
        }
        autoLoadAfterZoom = true
    }

    fun requestAutoLoad() {
        if (Config.BUILDING_LOAD_TYPE == BuildingLoadType.CENTRE_ONLY ||
            buildingLoadArea?.isWithinLimit == true
        ) {
            startBuildingLoad()
        } else {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val area = buildingLoadArea
                val result = snackbarHostState.showSnackbar(
                    message = if (area == null) {
                        checkingMapAreaMessage
                    } else {
                        "$zoomInLoadMessage · ${area.formattedDimensions}"
                    },
                    actionLabel = if (area == null) null else zoomLoadAction,
                    duration = if (area == null) SnackbarDuration.Short else SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) zoomToValidAreaAndLoad()
            }
        }
    }

    LaunchedEffect(autoLoadAfterZoom, buildingLoadArea) {
        if (autoLoadAfterZoom && buildingLoadArea?.isWithinLimit == true) {
            autoLoadAfterZoom = false
            startBuildingLoad()
        }
    }

    val pendingType = when (uiState.pendingDrawing) {
        is PendingDrawing.Building -> DrawnObjectType.BUILDING
        is PendingDrawing.Wall -> DrawnObjectType.WALL
        is PendingDrawing.Tree -> DrawnObjectType.TREE
        null -> null
    }
    val selectedType = uiState.selectedDrawing?.type
    val propertyType = pendingType ?: selectedType
    val selectionId = uiState.selectedDrawing?.id
    val selectedBuilding = uiState.drawnBuildings.find { it.id == selectionId }
    val selectedLoadedBuilding = uiState.visibleLoadedBuildings.find { building ->
        AutomaticBuildingMatcher.identity(building).selectionId == selectionId
    }
    val selectedOriginalLoadedBuilding = uiState.loadedBuildings.find { building ->
        AutomaticBuildingMatcher.identity(building).selectionId == selectionId
    }
    val selectedWall = uiState.drawnWalls.find { it.id == selectionId }
    val selectedTree = uiState.drawnTrees.find { it.id == selectionId }
    val propertyInitialHeight = propertyType?.let { type ->
        selectedLoadedBuilding?.heightMeters
            ?: selectedBuilding?.heightMeters
            ?: selectedWall?.heightMeters
            ?: selectedTree?.heightMeters
            ?: when (type) {
                DrawnObjectType.BUILDING -> DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
                DrawnObjectType.WALL -> DEFAULT_DRAWN_WALL_HEIGHT_METERS
                DrawnObjectType.TREE -> DEFAULT_DRAWN_TREE_HEIGHT_METERS
            }
    }
    val showPropertiesSheet = uiState.moveSession == null &&
        propertyType != null && propertyInitialHeight != null
    val autoToolState = when {
        uiState.buildingLoadState is BuildingLoadState.Loading -> AutoToolState.LOADING

        Config.BUILDING_LOAD_TYPE == BuildingLoadType.ALL &&
            buildingLoadArea == null -> AutoToolState.CHECKING

        Config.BUILDING_LOAD_TYPE == BuildingLoadType.ALL &&
            buildingLoadArea?.isWithinLimit == false -> AutoToolState.TOO_LARGE

        uiState.buildingLoadState is BuildingLoadState.Error -> AutoToolState.ERROR

        uiState.buildingLoadState is BuildingLoadState.Loaded -> AutoToolState.LOADED

        else -> AutoToolState.READY
    }
    val mode = uiState.activeDrawMode
    fun requestDrawingExit() {
        if (uiState.hasDraft) {
            exitEditingAfterDiscard = false
            showDiscardDraftConfirmation = true
        } else {
            onStopDrawing()
        }
    }

    fun finishEditing() {
        when {
            uiState.moveSession != null -> {
                onFinishMoving()
                mapboxSceneMode = MapboxSceneMode.VIEW
            }

            uiState.pendingDrawing != null ||
                (uiState.activeDrawMode != null && uiState.hasDraft) -> {
                exitEditingAfterDiscard = true
                showDiscardDraftConfirmation = true
            }

            uiState.activeDrawMode != null -> {
                onStopDrawing()
                mapboxSceneMode = MapboxSceneMode.VIEW
            }

            uiState.selectedDrawing != null -> {
                onSelectDrawing(null)
                mapboxSceneMode = MapboxSceneMode.VIEW
            }

            else -> mapboxSceneMode = MapboxSceneMode.VIEW
        }
    }

    BackHandler(
        enabled = mapboxSceneMode == MapboxSceneMode.EDIT
    ) {
        when {
            uiState.moveSession != null -> onCancelMoving()
            pendingType != null -> onReturnPendingToDrawing()
            selectedType != null -> onSelectDrawing(null)
            uiState.activeDrawMode != null -> requestDrawingExit()
            else -> finishEditing()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            onMapClickListener = { point ->
                if (uiState.activeDrawMode == null && uiState.pendingDrawing == null) {
                    controller?.queryDrawing(point, onSelectDrawing)
                }
                false
            },
            compass = { Compass(modifier = Modifier.safeDrawingPadding()) },
            scaleBar = { },
            logo = { Logo(modifier = Modifier.safeDrawingPadding()) },
            attribution = { Attribution(modifier = Modifier.safeDrawingPadding()) },
            style = { MapboxStandardSatelliteStyle() }
        ) {
            MapEffect(Unit) { currentMapView -> mapView = currentMapView }
        }

        mapboxSceneViewport?.let { viewport ->
            ShadowPlannerSceneView(
                uiState = uiState,
                mapSnapshotOverlay = satelliteSnapshot?.asImageBitmap(),
                state = ShadowPlannerSceneState(
                    viewport = viewport,
                    sceneMode = mapboxSceneMode,
                    autoToolState = autoToolState,
                    canRecenterCurrentLocation = currentLocationPoint != null,
                    editingCrosshairPoint = crosshairPoint
                ),
                actions = ShadowPlannerSceneActions(
                    navigation = MapNavigationActions(
                        onOpenSettings = onOpenSettings,
                        onOpenLocationSearch = onOpenLocationSearch,
                        onShowLocationInfo = { showSelectedLocationSheet = true },
                        onRecenterCurrentLocation = {
                            currentLocationPoint?.let { point ->
                                mapViewportState.setCameraOptions { center(point) }
                                mapboxSceneMapView?.mapboxMap?.setCamera(
                                    com.mapbox.maps.CameraOptions.Builder()
                                        .center(point)
                                        .build()
                                )
                            }
                        },
                        onOpenProjects = onOpenProjects,
                        onSaveProject = {
                            projectNameDraft = uiState.activeProjectName.orEmpty()
                            showSaveProjectDialog = true
                        }
                    ),
                    editing = MapEditingActions(
                        onFinishEditing = ::finishEditing,
                        onDrawMode = { selectedMode -> onSelectDrawMode(selectedMode) },
                        onAutoLoad = ::requestAutoLoad,
                        onClear = { showClearConfirmation = true }
                    ),
                    dateTime = MapDateTimeActions(
                        onDateTimeChanged = onDateTimeChanged,
                        onNowSelected = onNowSelected
                    ),
                    onOpenShadowColor = { showShadowColorSheet = true },
                    onSceneModeChanged = { mapboxSceneMode = it },
                    onSceneMapViewReady = { mapboxSceneMapView = it },
                    onSceneMapClick = { point ->
                        if (mapboxSceneMode == MapboxSceneMode.EDIT &&
                            uiState.activeDrawMode == null &&
                            uiState.pendingDrawing == null
                        ) {
                            mapboxSceneController?.queryDrawing(point, onSelectDrawing)
                        }
                        false
                    }
                )
            )
        }

        uiState.moveSession?.let {
            MoveModeOverlay(
                onDrag = { start, current ->
                    val map = mapboxSceneMapView ?: return@MoveModeOverlay
                    val startPoint = map.mapboxMap.coordinateForPixel(
                        ScreenCoordinate(start.x.toDouble(), start.y.toDouble())
                    )
                    val currentPoint = map.mapboxMap.coordinateForPixel(
                        ScreenCoordinate(current.x.toDouble(), current.y.toDouble())
                    )
                    onMoveSelectedObject(
                        currentPoint.longitude() - startPoint.longitude(),
                        currentPoint.latitude() - startPoint.latitude()
                    )
                },
                onDone = onFinishMoving,
                onCancel = onCancelMoving
            )
        }

        if (mapboxSceneMode == MapboxSceneMode.EDIT) {
            DrawingCrosshair(modifier = Modifier.align(Alignment.Center))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = THREE_D_BOTTOM_CONTROL_CLEARANCE
                    )
            )
        }

        if (mapboxSceneMode == MapboxSceneMode.EDIT && mode != null && pendingType == null) {
            ActiveDrawingControls(
                mode = mode,
                vertexCount = uiState.inProgressVertices.size,
                onAdd = {
                    val point = crosshairPoint ?: return@ActiveDrawingControls
                    if (mode == DrawMode.TREE) {
                        onStartTree(point)
                    } else {
                        val last = uiState.inProgressVertices.lastOrNull()
                        val threshold = with(density) { MIN_POINT_SPACING_DP.dp.toPx() }
                        if (last == null ||
                            mapboxSceneMapView?.isFarEnoughFrom(last, point, threshold) != false
                        ) {
                            onAddVertex(point)
                        } else {
                            onDrawingError(moveFartherMessage)
                        }
                    }
                },
                onUndo = onUndo,
                onDone = {
                    val point = crosshairPoint ?: return@ActiveDrawingControls
                    if (mode == DrawMode.BUILDING) onFinishBuilding(point) else onFinishWall(point)
                },
                onCancel = ::requestDrawingExit,
                error = uiState.drawingError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (mapboxSceneMode == MapboxSceneMode.EDIT && showPropertiesSheet) {
            DrawingPropertiesSheet(
                type = propertyType,
                initialHeightMeters = propertyInitialHeight,
                initialRadiusMeters = selectedTree?.radiusMeters
                    ?: if (propertyType == DrawnObjectType.TREE) {
                        DEFAULT_DRAWN_TREE_RADIUS_METERS
                    } else {
                        null
                    },
                isCreating = pendingType != null,
                objectSource = uiState.selectedDrawing?.source ?: SceneObjectSource.MANUAL,
                loadedHeightMeters = selectedOriginalLoadedBuilding?.heightMeters,
                onBack = {
                    if (pendingType != null) onReturnPendingToDrawing() else onSelectDrawing(null)
                },
                onMove = onStartMoving,
                onApply = { height, radius ->
                    if (pendingType != null) {
                        onCommitPendingDrawing(height, radius)
                    } else {
                        onUpdateSelectedDrawing(height, radius)
                    }
                },
                onDelete = {
                    if (onDeleteSelectedDrawing()) {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = objectDeletedMessage,
                                actionLabel = undoMessage,
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onRestoreDeletedObject()
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_scene_question)) },
            text = { Text(stringResource(R.string.clear_scene_details)) },
            confirmButton = {
                Button(onClick = {
                    onClearScene()
                    showClearConfirmation = false
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = withTimeoutOrNull(CLEAR_UNDO_MILLIS) {
                            snackbarHostState.showSnackbar(
                                message = sceneClearedMessage,
                                actionLabel = undoMessage,
                                duration = SnackbarDuration.Indefinite
                            )
                        }
                        if (result == SnackbarResult.ActionPerformed) {
                            onRestoreClearedScene()
                        } else {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    }
                }) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showClearConfirmation = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDiscardDraftConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDraftConfirmation = false
                exitEditingAfterDiscard = false
            },
            title = { Text(stringResource(R.string.discard_drawing_question)) },
            text = { Text(stringResource(R.string.unfinished_points_removed)) },
            confirmButton = {
                Button(onClick = {
                    onStopDrawing()
                    showDiscardDraftConfirmation = false
                    if (exitEditingAfterDiscard) {
                        exitEditingAfterDiscard = false
                        mapboxSceneMode = MapboxSceneMode.VIEW
                    }
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDiscardDraftConfirmation = false
                    exitEditingAfterDiscard = false
                }) {
                    Text(stringResource(R.string.keep_drawing))
                }
            }
        )
    }

    if (showSelectedLocationSheet && uiState.selectedLocationLabel != null) {
        SelectedLocationSheet(
            address = uiState.selectedLocationLabel,
            onDismiss = { showSelectedLocationSheet = false }
        )
    }

    if (showShadowColorSheet) {
        ShadowColorSheet(
            initialAppearance = uiState.shadowAppearance,
            onDismissRequest = { showShadowColorSheet = false },
            onApply = { appearance ->
                onShadowAppearanceChanged(appearance)
                showShadowColorSheet = false
            }
        )
    }

    if (showSaveProjectDialog) {
        SaveProjectDialog(
            projectName = projectNameDraft,
            activeProjectName = uiState.activeProjectName,
            hasActiveProject = uiState.activeProjectId != null,
            onProjectNameChanged = { projectNameDraft = it },
            onDismissRequest = { showSaveProjectDialog = false },
            onSaveUpdate = { name ->
                showSaveProjectDialog = false
                onSaveProject(name)
            },
            onSaveAsNew = { name ->
                showSaveProjectDialog = false
                onSaveProjectAsNew(name)
            }
        )
    }
}

@Composable
private fun MoveModeOverlay(
    onDrag: (androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val dimensions = ShadowMapDesign.dimensions
    var dragStart by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragStart = it },
                    onDragCancel = { dragStart = null },
                    onDragEnd = { dragStart = null },
                    onDrag = { change, amount ->
                        val start = dragStart ?: change.position
                        onDrag(start, change.position)
                    }
                )
            }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = dimensions.spacingLarge,
                    end = dimensions.spacingLarge,
                    bottom = 96.dp
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.move_object_hint),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = dimensions.spacingMedium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

@Preview(name = "Move mode light", showBackground = true, widthDp = 360, heightDp = 180)
@Composable
private fun MoveModeOverlayLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF71856B))) {
            MoveModeOverlay(onDrag = { _, _ -> }, onDone = {}, onCancel = {})
        }
    }
}

@Preview(name = "Move mode dark", showBackground = true, widthDp = 360, heightDp = 180)
@Composable
private fun MoveModeOverlayDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF263326))) {
            MoveModeOverlay(onDrag = { _, _ -> }, onDone = {}, onCancel = {})
        }
    }
}

private val THREE_D_BOTTOM_CONTROL_CLEARANCE = 156.dp

private fun MapView.toSceneViewport(): SceneViewport? {
    if (width <= 0 || height <= 0) return null
    val center = mapboxMap.coordinateForPixel(ScreenCoordinate(width / 2.0, height / 2.0))
    val topLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, 0.0))
    val topRight = mapboxMap.coordinateForPixel(ScreenCoordinate(width.toDouble(), 0.0))
    val bottomLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, height.toDouble()))
    return SceneViewport.fromScreenCoordinates(
        center.longitude(),
        center.latitude(),
        topLeft.longitude(),
        topLeft.latitude(),
        topRight.longitude(),
        topRight.latitude(),
        bottomLeft.longitude(),
        bottomLeft.latitude()
    )
}

private fun MapView.toProjectViewport(): ProjectViewport? {
    if (width <= 0 || height <= 0) return null
    val camera = mapboxMap.cameraState
    val center = camera.center
    val topLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, 0.0))
    val topRight = mapboxMap.coordinateForPixel(ScreenCoordinate(width.toDouble(), 0.0))
    val bottomRight = mapboxMap.coordinateForPixel(
        ScreenCoordinate(width.toDouble(), height.toDouble())
    )
    val bottomLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, height.toDouble()))
    return ProjectViewport(
        center = GeoPoint(center.longitude(), center.latitude()),
        zoom = camera.zoom,
        bearing = camera.bearing,
        pitch = camera.pitch,
        boundary = com.gooludou.shadowplanner.core.model.GeoPolygon(
            listOf(
                listOf(
                    GeoPoint(topLeft.longitude(), topLeft.latitude()),
                    GeoPoint(topRight.longitude(), topRight.latitude()),
                    GeoPoint(bottomRight.longitude(), bottomRight.latitude()),
                    GeoPoint(bottomLeft.longitude(), bottomLeft.latitude()),
                    GeoPoint(topLeft.longitude(), topLeft.latitude())
                )
            )
        )
    )
}

private fun MapView.isFarEnoughFrom(
    first: GeoPoint,
    second: GeoPoint,
    thresholdPixels: Float
): Boolean {
    val firstPixel = mapboxMap.pixelForCoordinate(Point.fromLngLat(first.longitude, first.latitude))
    val secondPixel = mapboxMap.pixelForCoordinate(
        Point.fromLngLat(second.longitude, second.latitude)
    )
    val dx = firstPixel.x - secondPixel.x
    val dy = firstPixel.y - secondPixel.y
    return dx * dx + dy * dy >= thresholdPixels * thresholdPixels
}

private fun MapView.toBuildingLoadArea(): BuildingLoadArea? = toSceneViewport()?.let { viewport ->
    BuildingLoadArea(
        widthMeters = viewport.widthMeters,
        heightMeters = viewport.heightMeters
    )
}

private const val MIN_POINT_SPACING_DP = 12f
private const val MAX_AUTO_LOAD_METERS = 500.0
private const val AUTO_LOAD_ZOOM_PADDING = 0.1
private const val CLEAR_UNDO_MILLIS = 8_000L
