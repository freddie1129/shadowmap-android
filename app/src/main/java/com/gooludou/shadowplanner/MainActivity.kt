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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.gooludou.shadowplanner.domain.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.domain.DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
import com.gooludou.shadowplanner.domain.DEFAULT_DRAWN_TREE_HEIGHT_METERS
import com.gooludou.shadowplanner.domain.DEFAULT_DRAWN_TREE_RADIUS_METERS
import com.gooludou.shadowplanner.domain.DEFAULT_DRAWN_WALL_HEIGHT_METERS
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.DrawnObjectType
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.PendingDrawing
import com.gooludou.shadowplanner.domain.SceneObjectSource
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.map.BuildingLoadArea
import com.gooludou.shadowplanner.map.MapboxShadowMapController
import com.gooludou.shadowplanner.navigation.AppNavigation
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
import com.gooludou.shadowplanner.presentation.components.ActiveDrawingControls
import com.gooludou.shadowplanner.presentation.components.AutoToolState
import com.gooludou.shadowplanner.presentation.components.DrawingCrosshair
import com.gooludou.shadowplanner.presentation.components.DrawingPropertiesSheet
import com.gooludou.shadowplanner.presentation.components.Map2DView
import com.gooludou.shadowplanner.presentation.components.Scene3DView
import com.gooludou.shadowplanner.presentation.components.SelectedLocationSheet
import com.gooludou.shadowplanner.presentation.components.ShadowColorSheet
import com.gooludou.shadowplanner.project.ProjectViewport
import com.gooludou.shadowplanner.scene.Scene3DAppearance
import com.gooludou.shadowplanner.scene.SceneCameraView
import com.gooludou.shadowplanner.scene.SceneViewport
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme
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
import java.util.Locale
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
                onShadowAppearanceChanged = viewModel::onShadowAppearanceChanged
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
                onSelectDrawing = viewModel::selectDrawing
            ),
            scene = SceneActions(
                onClearScene = viewModel::clearScene,
                onRestoreClearedScene = viewModel::restoreClearedScene
            ),
            project = ProjectActions(onSaveProject = viewModel::saveProject)
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
    val buildingsLoadedFormat = stringResource(R.string.buildings_loaded)
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
    val onClearScene = actions.scene.onClearScene
    val onRestoreClearedScene = actions.scene.onRestoreClearedScene
    val onSaveProject = actions.project.onSaveProject
    val mapControllerFactory = dependencies.mapControllerFactory
    val pendingLocation = navigation.pendingLocation
    val onLocationApplied = navigation.onLocationApplied
    val onOpenProjects = navigation.onOpenProjects
    val onOpenLocationSearch = navigation.onOpenLocationSearch
    val onOpenSettings = navigation.onOpenSettings
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
    var showSkyIn3d by remember { mutableStateOf(false) }
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
    var showShadowColorSheet by remember { mutableStateOf(false) }
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
                            noBuildingsFoundMessage
                        } else {
                            String.format(Locale.getDefault(), buildingsLoadedFormat, buildings.size)
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

    fun enter3d() {
        sceneViewport = mapView?.toSceneViewport()
        sceneCameraView = SceneCameraView.ORBIT
        showSkyIn3d = false
        if (!show3d) sceneBackStack.add(Scene3DSceneDestination)
    }

    fun exit3d() {
        showSkyIn3d = false
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
    val latestUiState = rememberUpdatedState(uiState)
    val latestAutoToolState = rememberUpdatedState(autoToolState)
    val latestShowDateTime = rememberUpdatedState(showDateTime)
    val latestMode = rememberUpdatedState(mode)
    val latestPropertyType = rememberUpdatedState(propertyType)

    fun requestDrawingExit() {
        if (uiState.hasDraft) {
            showDiscardDraftConfirmation = true
        } else {
            onStopDrawing()
        }
    }

    BackHandler(
        enabled = !show3d &&
            (
                uiState.activeDrawMode != null || uiState.pendingDrawing != null ||
                    selectedType != null
                )
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
                        val currentUiState by latestUiState
                        val currentAutoToolState by latestAutoToolState
                        val currentShowDateTime by latestShowDateTime
                        val currentMode by latestMode
                        val currentPropertyType by latestPropertyType
                        if (currentMode == null && currentPropertyType == null) {
                            Map2DView(
                                uiState = currentUiState,
                                autoToolState = currentAutoToolState,
                                isTimeVisible = currentShowDateTime,
                                onDateTimeChanged = onDateTimeChanged,
                                onNowSelected = onNowSelected,
                                onToggleTime = { showDateTime = !showDateTime },
                                onOpenShadowColor = { showShadowColorSheet = true },
                                onDrawMode = { selectedMode -> onSelectDrawMode(selectedMode) },
                                onAutoLoad = ::requestAutoLoad,
                                onClear = { showClearConfirmation = true },
                                onOpenSettings = onOpenSettings,
                                onOpen3D = {
                                    if (currentUiState.hasDraft) {
                                        showDraft3dConfirmation = true
                                    } else {
                                        enter3d()
                                    }
                                },
                                onOpenProjects = onOpenProjects,
                                onSaveProject = {
                                    projectNameDraft = currentUiState.activeProjectName.orEmpty()
                                    showSaveProjectDialog = true
                                },
                                onOpenLocationSearch = onOpenLocationSearch,
                                onShowLocationInfo = { showSelectedLocationSheet = true }
                            )
                        }
                    }

                    Scene3DSceneDestination -> NavEntry(key) {
                        val currentUiState by latestUiState
                        Scene3DView(
                            buildings = currentUiState.buildings,
                            walls = currentUiState.drawnWalls,
                            trees = currentUiState.drawnTrees,
                            viewport = sceneViewport,
                            azimuth = currentUiState.solarPosition?.azimuthDegrees?.toFloat()
                                ?: Scene3DAppearance.DEFAULT_SUN_AZIMUTH_DEGREES,
                            zenith = currentUiState.solarPosition?.zenithDegrees?.toFloat()
                                ?: Scene3DAppearance.DEFAULT_SUN_ZENITH_DEGREES,
                            sunVisible = currentUiState.solarPosition?.isAboveHorizon == true,
                            sunPath = currentUiState.sunPath,
                            cameraView = sceneCameraView,
                            showSatellite = showSatelliteIn3d,
                            showSky = showSkyIn3d,
                            onCameraViewChanged = { sceneCameraView = it },
                            onToggleSatellite = { showSatelliteIn3d = !showSatelliteIn3d },
                            onToggleSky = { showSkyIn3d = !showSkyIn3d },
                            onBackToMap = ::exit3d,
                            selectedEpochMillis = currentUiState.selectedEpochMillis,
                            timeZoneId = currentUiState.displayTimeZoneId,
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
                        if (last == null ||
                            mapView?.isFarEnoughFrom(last, point, threshold) != false
                        ) {
                            onAddVertex(point)
                        } else {
                            onDrawingError(moveFartherMessage)
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
            text = {
                Text(
                    stringResource(
                        R.string.clear_scene_details,
                        uiState.visibleLoadedBuildings.size,
                        uiState.drawnBuildings.size,
                        uiState.drawnWalls.size,
                        uiState.drawnTrees.size
                    )
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
                OutlinedButton(onClick = { showClearConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDiscardDraftConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardDraftConfirmation = false },
            title = { Text(stringResource(R.string.discard_drawing_question)) },
            text = { Text(stringResource(R.string.unfinished_points_removed)) },
            confirmButton = {
                Button(onClick = {
                    onStopDrawing()
                    showDiscardDraftConfirmation = false
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscardDraftConfirmation = false }) {
                    Text(stringResource(R.string.keep_drawing))
                }
            }
        )
    }

    if (showDraft3dConfirmation) {
        AlertDialog(
            onDismissRequest = { showDraft3dConfirmation = false },
            title = { Text(stringResource(R.string.view_committed_objects_3d_question)) },
            text = { Text(stringResource(R.string.unfinished_drawing_kept)) },
            confirmButton = {
                Button(onClick = {
                    showDraft3dConfirmation = false
                    enter3d()
                }) { Text(stringResource(R.string.view_3d)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDraft3dConfirmation = false }) {
                    Text(stringResource(R.string.continue_drawing))
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
        AlertDialog(
            onDismissRequest = { showSaveProjectDialog = false },
            title = { Text(stringResource(R.string.save_project)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = projectNameDraft,
                    onValueChange = { projectNameDraft = it },
                    label = { Text(stringResource(R.string.project_name)) },
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
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSaveProjectDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
        boundary = com.gooludou.shadowplanner.domain.GeoPolygon(
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
