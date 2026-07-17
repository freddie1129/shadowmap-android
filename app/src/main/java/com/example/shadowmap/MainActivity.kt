package com.example.shadowmap

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.AutomaticBuildingMatcher
import com.example.shadowmap.domain.DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_TREE_HEIGHT_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_TREE_RADIUS_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_WALL_HEIGHT_METERS
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnObjectType
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.domain.SceneObjectSource
import com.example.shadowmap.location.LocationSearchResult
import com.example.shadowmap.map.BuildingLoadArea
import com.example.shadowmap.map.MapboxShadowMapController
import com.example.shadowmap.navigation.AppNavigation
import com.example.shadowmap.presentation.BuildingLoadState
import com.example.shadowmap.presentation.ShadowMapUiState
import com.example.shadowmap.presentation.ShadowMapViewModel
import com.example.shadowmap.presentation.components.DateTimeSpinner
import com.example.shadowmap.presentation.components.ActiveDrawingControls
import com.example.shadowmap.presentation.components.AutoToolState
import com.example.shadowmap.presentation.components.DrawingCrosshair
import com.example.shadowmap.presentation.components.DrawingPropertiesSheet
import com.example.shadowmap.presentation.components.SelectedLocationSheet
import com.example.shadowmap.presentation.components.Scene3DView
import com.example.shadowmap.presentation.components.Map2DView
import com.example.shadowmap.project.ProjectViewport
import com.example.shadowmap.scene.Scene3DAppearance
import com.example.shadowmap.scene.SceneCameraView
import com.example.shadowmap.scene.SceneViewport
import com.example.shadowmap.ui.theme.ShadowMapTheme
import com.example.shadowmap.ui.theme.ShadowMapDesign
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private data object Map2DSceneDestination
private data object Scene3DSceneDestination

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
    pendingLocation: LocationSearchResult? = null,
    pendingProjectId: String? = null,
    onLocationApplied: () -> Unit = {},
    onProjectApplied: () -> Unit = {},
    onOpenLocationSearch: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
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
        onDateTimeChanged = viewModel::onDateTimeChanged,
        onNowSelected = viewModel::onNowSelected,
        onLoadStarted = viewModel::onBuildingLoadStarted,
        onBuildingsLoaded = viewModel::onBuildingsLoaded,
        onLoadFailed = viewModel::onBuildingLoadFailed,
        onViewportChanged = viewModel::onViewportChanged,
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
        onClearScene = viewModel::clearScene,
        onRestoreClearedScene = viewModel::restoreClearedScene,
        mapControllerFactory = mapControllerFactory,
        pendingLocation = pendingLocation,
        onOpenProjects = onOpenProjects,
        onSaveProject = viewModel::saveProject,
        onLocationApplied = onLocationApplied,
        onOpenLocationSearch = onOpenLocationSearch,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun ShadowMapScreen(
    uiState: ShadowMapUiState,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onLoadStarted: () -> Unit,
    onBuildingsLoaded: (List<BuildingFootprint>, GeoPoint) -> Unit,
    onLoadFailed: (Throwable) -> Unit,
    onViewportChanged: (ProjectViewport) -> Unit,
    onSelectDrawMode: (DrawMode) -> Boolean,
    onStopDrawing: () -> Unit,
    onAddVertex: (GeoPoint) -> Unit,
    onUndo: () -> Unit,
    onDrawingError: (String) -> Unit,
    onFinishBuilding: () -> Boolean,
    onFinishWall: () -> Boolean,
    onStartTree: (GeoPoint) -> Unit,
    onReturnPendingToDrawing: () -> Unit,
    onCommitPendingDrawing: (Double, Double?) -> Unit,
    onUpdateSelectedDrawing: (Double, Double?) -> Unit,
    onDeleteSelectedDrawing: () -> Boolean,
    onRestoreDeletedObject: () -> Unit,
    onSelectDrawing: (com.example.shadowmap.domain.DrawnObjectSelection?) -> Unit,
    onClearScene: () -> Unit,
    onRestoreClearedScene: () -> Unit,
    mapControllerFactory: MapboxShadowMapController.Factory,
    pendingLocation: LocationSearchResult?,
    onOpenProjects: () -> Unit,
    onSaveProject: (String?) -> Unit,
    onLocationApplied: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
            center(Point.fromLngLat(153.4038943, -28.0870458))
            zoom(17.0)
        }
    }
    LaunchedEffect(pendingLocation) {
        val latitude = pendingLocation?.latitude ?: return@LaunchedEffect
        val longitude = pendingLocation.longitude ?: return@LaunchedEffect
        mapViewportState.setCameraOptions {
            center(Point.fromLngLat(longitude, latitude))
            zoom(15.0)
        }
        onLocationApplied()
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    val controller = remember(mapView, mapControllerFactory) {
        mapView?.let(mapControllerFactory::create)
    }
    var satelliteSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var loadRequest by remember { mutableIntStateOf(0) }
    var buildingQueryLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val sceneBackStack = remember {
        androidx.compose.runtime.mutableStateListOf<Any>(Map2DSceneDestination)
    }
    val show3d = sceneBackStack.lastOrNull() == Scene3DSceneDestination
    var showSatelliteIn3d by remember { mutableStateOf(true) }
    var sceneCameraView by remember { mutableStateOf(SceneCameraView.ORBIT) }
    var sceneViewport by remember { mutableStateOf<SceneViewport?>(null) }
    var buildingLoadArea by remember { mutableStateOf<BuildingLoadArea?>(null) }
    var crosshairPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showDateTime by remember { mutableStateOf(false) }
    var autoLoadAfterZoom by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showDiscardDraftConfirmation by remember { mutableStateOf(false) }
    var showDraft3dConfirmation by remember { mutableStateOf(false) }
    var showSelectedLocationSheet by remember { mutableStateOf(false) }
    var showSaveProjectDialog by remember { mutableStateOf(false) }
    var projectNameDraft by remember(uiState.activeProjectName) {
        mutableStateOf(uiState.activeProjectName.orEmpty())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(mapView) {
        val currentMapView = mapView
        if (currentMapView == null) {
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
            updateCrosshairPoint()
            val cameraSubscription = currentMapView.mapboxMap.subscribeCameraChanged {
                updateCrosshairPoint()
            }
            onDispose {
                mapIdleSubscription.cancel()
                cameraSubscription.cancel()
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
                if (!firstLocationReceived) {
                    firstLocationReceived = true
                    mapViewportState.setCameraOptions {
                        center(point)
                        zoom(17.0)
                    }
                }
            }
            locationComponent.updateSettings {
                enabled = true
                pulsingEnabled = true
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

    LaunchedEffect(loadRequest, controller) {
        if (loadRequest == 0 || controller == null) return@LaunchedEffect
        val calculationLocation = buildingQueryLocation ?: return@LaunchedEffect
        withFrameNanos { }
        try {
            val result = runCatching { controller.fetchBuildings() }
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure
            result.fold(
                onSuccess = { buildings ->
                    onBuildingsLoaded(buildings, calculationLocation)
                    snackbarHostState.showSnackbar(
                        message = if (buildings.isEmpty()) {
                            "No buildings found in this area"
                        } else {
                            "${buildings.size} buildings loaded"
                        },
                        duration = SnackbarDuration.Short
                    )
                },
                onFailure = { throwable ->
                    onLoadFailed(throwable)
                    snackbarHostState.showSnackbar(
                        message = throwable.message ?: "Unable to load buildings",
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
                shadows = uiState.shadows
            )
        }
    }

    LaunchedEffect(mapView, show3d, showSatelliteIn3d) {
        mapView?.visibility =
            if (show3d &&
                !showSatelliteIn3d
            ) {
                android.view.View.INVISIBLE
            } else {
                android.view.View.VISIBLE
            }
    }

    fun startBuildingLoad() {
        val currentMapView = mapView ?: return
        val currentLoadArea = currentMapView.toBuildingLoadArea()
        buildingLoadArea = currentLoadArea
        if (currentLoadArea?.isWithinLimit != true) return
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
        val currentMapView = mapView ?: return
        val area = buildingLoadArea ?: return
        val largestDimension = max(area.widthMeters, area.heightMeters).toDouble()
        val zoomIncrease = if (largestDimension > MAX_AUTO_LOAD_METERS) {
            ln(largestDimension / MAX_AUTO_LOAD_METERS) / ln(2.0) + AUTO_LOAD_ZOOM_PADDING
        } else {
            0.0
        }
        mapViewportState.setCameraOptions {
            center(currentMapView.mapboxMap.cameraState.center)
            zoom(currentMapView.mapboxMap.cameraState.zoom + zoomIncrease)
        }
        autoLoadAfterZoom = true
    }

    fun requestAutoLoad() {
        if (buildingLoadArea?.isWithinLimit == true) {
            startBuildingLoad()
        } else {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val area = buildingLoadArea
                val result = snackbarHostState.showSnackbar(
                    message = if (area == null) {
                        "Checking the visible map area"
                    } else {
                        "Zoom in to load buildings · ${area.formattedDimensions}"
                    },
                    actionLabel = if (area == null) null else "Zoom & load",
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

    fun enter3d() {
        sceneViewport = mapView?.toSceneViewport()
        sceneCameraView = SceneCameraView.ORBIT
        if (!show3d) sceneBackStack.add(Scene3DSceneDestination)
    }

    fun exit3d() {
        if (show3d) sceneBackStack.removeLastOrNull()
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
    val autoToolState = when {
        uiState.buildingLoadState is BuildingLoadState.Loading -> AutoToolState.LOADING
        buildingLoadArea == null -> AutoToolState.CHECKING
        buildingLoadArea?.isWithinLimit == false -> AutoToolState.TOO_LARGE
        uiState.buildingLoadState is BuildingLoadState.Error -> AutoToolState.ERROR
        uiState.buildingLoadState is BuildingLoadState.Loaded -> AutoToolState.LOADED
        else -> AutoToolState.READY
    }
    val mode = uiState.activeDrawMode

    fun requestDrawingExit() {
        if (uiState.hasDraft) {
            showDiscardDraftConfirmation = true
        } else {
            onStopDrawing()
        }
    }

    BackHandler(
        enabled = !show3d &&
            (uiState.activeDrawMode != null || uiState.pendingDrawing != null || selectedType != null)
    ) {
        when {
            pendingType != null -> onReturnPendingToDrawing()
            selectedType != null -> onSelectDrawing(null)
            else -> requestDrawingExit()
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

        if (!show3d && uiState.activeDrawMode != null && uiState.pendingDrawing == null) {
            DrawingCrosshair(modifier = Modifier.align(Alignment.Center))
        }

        if (show3d && !showSatelliteIn3d) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(Scene3DAppearance.HIDDEN_MAP_BACKDROP_ARGB))
            )
        }

        satelliteSnapshot?.let { snapshot ->
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = sceneBackStack,
            onBack = ::exit3d,
            entryProvider = { key ->
                when (key) {
                    Map2DSceneDestination -> NavEntry(key) {
                        if (mode == null && propertyType == null) {
                            Map2DView(
                                uiState = uiState,
                                autoToolState = autoToolState,
                                isTimeVisible = showDateTime,
                                onDateTimeChanged = onDateTimeChanged,
                                onNowSelected = onNowSelected,
                                onToggleTime = { showDateTime = !showDateTime },
                                onDrawMode = { selectedMode -> onSelectDrawMode(selectedMode) },
                                onAutoLoad = ::requestAutoLoad,
                                onClear = { showClearConfirmation = true },
                                onOpenSettings = onOpenSettings,
                                onOpen3D = {
                                    if (uiState.hasDraft) {
                                        showDraft3dConfirmation = true
                                    } else {
                                        enter3d()
                                    }
                                },
                                onOpenProjects = onOpenProjects,
                                onSaveProject = {
                                    projectNameDraft = uiState.activeProjectName.orEmpty()
                                    showSaveProjectDialog = true
                                },
                                onOpenLocationSearch = onOpenLocationSearch,
                                onShowLocationInfo = { showSelectedLocationSheet = true }
                            )
                        }
                    }
                    Scene3DSceneDestination -> NavEntry(key) {
                        Scene3DView(
                            buildings = uiState.buildings,
                            walls = uiState.drawnWalls,
                            trees = uiState.drawnTrees,
                            viewport = sceneViewport,
                            azimuth = uiState.solarPosition?.azimuthDegrees?.toFloat()
                                ?: Scene3DAppearance.DEFAULT_SUN_AZIMUTH_DEGREES,
                            zenith = uiState.solarPosition?.zenithDegrees?.toFloat()
                                ?: Scene3DAppearance.DEFAULT_SUN_ZENITH_DEGREES,
                            sunVisible = uiState.solarPosition?.isAboveHorizon == true,
                            cameraView = sceneCameraView,
                            showSatellite = showSatelliteIn3d,
                            onCameraViewChanged = { sceneCameraView = it },
                            onToggleSatellite = { showSatelliteIn3d = !showSatelliteIn3d },
                            onBackToMap = ::exit3d,
                            selectedEpochMillis = uiState.selectedEpochMillis,
                            timeZoneId = uiState.displayTimeZoneId,
                            onDateTimeChanged = onDateTimeChanged,
                            onNowSelected = onNowSelected
                        )
                    }
                    else -> error("Unknown scene destination: $key")
                }
            }
        )

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
                        bottom = if (show3d || showDateTime) {
                            THREE_D_BOTTOM_CONTROL_CLEARANCE
                        } else {
                            64.dp
                        }
                    )
            )
        }

        if (!show3d && mode != null && pendingType == null) {
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
                        if (last == null || mapView?.isFarEnoughFrom(last, point, threshold) != false) {
                            onAddVertex(point)
                        } else {
                            onDrawingError("Move farther from the previous point")
                        }
                    }
                },
                onUndo = onUndo,
                onDone = {
                    if (mode == DrawMode.BUILDING) onFinishBuilding() else onFinishWall()
                },
                onCancel = ::requestDrawingExit,
                error = uiState.drawingError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (!show3d && propertyType != null && propertyInitialHeight != null) {
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
                isEditedAutomaticObject = selectionId?.let(uiState::isLoadedBuildingEdited) == true,
                loadedHeightMeters = selectedOriginalLoadedBuilding?.heightMeters,
                onBack = {
                    if (pendingType != null) onReturnPendingToDrawing() else onSelectDrawing(null)
                },
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
                                message = "Object deleted",
                                actionLabel = "Undo",
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
            title = { Text("Clear scene?") },
            text = {
                Text(
                    "Remove ${uiState.visibleLoadedBuildings.size} automatic buildings, " +
                        "${uiState.drawnBuildings.size} manual buildings, " +
                        "${uiState.drawnWalls.size} walls, and ${uiState.drawnTrees.size} trees? " +
                        "Loaded buildings will stay hidden after Auto is used again."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onClearScene()
                    showClearConfirmation = false
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = withTimeoutOrNull(CLEAR_UNDO_MILLIS) {
                            snackbarHostState.showSnackbar(
                                message = "Scene cleared",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Indefinite
                            )
                        }
                        if (result == SnackbarResult.ActionPerformed) {
                            onRestoreClearedScene()
                        } else {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    }
                }) { Text("Clear all") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardDraftConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardDraftConfirmation = false },
            title = { Text("Discard drawing?") },
            text = { Text("Your unfinished points will be removed.") },
            confirmButton = {
                Button(onClick = {
                    onStopDrawing()
                    showDiscardDraftConfirmation = false
                }) { Text("Discard") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardDraftConfirmation = false }) {
                    Text("Keep drawing")
                }
            }
        )
    }

    if (showDraft3dConfirmation) {
        AlertDialog(
            onDismissRequest = { showDraft3dConfirmation = false },
            title = { Text("View committed objects in 3D?") },
            text = { Text("The unfinished drawing will be kept and restored in Map View.") },
            confirmButton = {
                Button(onClick = {
                    showDraft3dConfirmation = false
                    enter3d()
                }) { Text("View 3D") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDraft3dConfirmation = false }) {
                    Text("Continue drawing")
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

    if (showSaveProjectDialog) {
        AlertDialog(
            onDismissRequest = { showSaveProjectDialog = false },
            title = { Text("Save project") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = projectNameDraft,
                    onValueChange = { projectNameDraft = it },
                    label = { Text("Project name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = projectNameDraft.isNotBlank(),
                    onClick = {
                        showSaveProjectDialog = false
                        onSaveProject(projectNameDraft.trim())
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
    toSceneViewport() ?: return null
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
        boundary = com.example.shadowmap.domain.GeoPolygon(
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

private fun MapView.isFarEnoughFrom(first: GeoPoint, second: GeoPoint, thresholdPixels: Float): Boolean {
    val firstPixel = mapboxMap.pixelForCoordinate(Point.fromLngLat(first.longitude, first.latitude))
    val secondPixel = mapboxMap.pixelForCoordinate(Point.fromLngLat(second.longitude, second.latitude))
    val dx = firstPixel.x - secondPixel.x
    val dy = firstPixel.y - secondPixel.y
    return dx * dx + dy * dy >= thresholdPixels * thresholdPixels
}

private fun MapView.toBuildingLoadArea(): BuildingLoadArea? =
    toSceneViewport()?.let { viewport ->
        BuildingLoadArea(
            widthMeters = viewport.widthMeters,
            heightMeters = viewport.heightMeters
        )
    }

private const val MIN_POINT_SPACING_DP = 12f
private const val MAX_AUTO_LOAD_METERS = 500.0
private const val AUTO_LOAD_ZOOM_PADDING = 0.1
private const val CLEAR_UNDO_MILLIS = 8_000L
