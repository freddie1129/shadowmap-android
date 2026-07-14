package com.example.shadowmap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_TREE_HEIGHT_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_TREE_RADIUS_METERS
import com.example.shadowmap.domain.DEFAULT_DRAWN_WALL_HEIGHT_METERS
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnObjectType
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.map.BuildingLoadArea
import com.example.shadowmap.map.MAX_BUILDING_LOAD_DIMENSION_METERS
import com.example.shadowmap.map.MapboxShadowMapController
import com.example.shadowmap.presentation.BuildingLoadState
import com.example.shadowmap.presentation.ShadowMapUiState
import com.example.shadowmap.presentation.ShadowMapViewModel
import com.example.shadowmap.presentation.components.DateTimeSpinner
import com.example.shadowmap.presentation.components.ActiveDrawingControls
import com.example.shadowmap.presentation.components.DrawingCrosshair
import com.example.shadowmap.presentation.components.DrawingPropertiesSheet
import com.example.shadowmap.presentation.components.DrawingToolChooser
import com.example.shadowmap.scene.FilamentBuildingView
import com.example.shadowmap.scene.Scene3DAppearance
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
import kotlinx.coroutines.CancellationException

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mapControllerFactory: MapboxShadowMapController.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            ShadowMapTheme {
                ShadowMapRoute(
                    mapControllerFactory = mapControllerFactory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ShadowMapRoute(
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier,
    viewModel: ShadowMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShadowMapScreen(
        uiState = uiState,
        onDateTimeChanged = viewModel::onDateTimeChanged,
        onNowSelected = viewModel::onNowSelected,
        onLoadStarted = viewModel::onBuildingLoadStarted,
        onBuildingsLoaded = viewModel::onBuildingsLoaded,
        onLoadFailed = viewModel::onBuildingLoadFailed,
        onSelectDrawMode = viewModel::selectDrawMode,
        onDiscardDraftAndSelectMode = viewModel::discardDraftAndSelectDrawMode,
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
        onSelectDrawing = viewModel::selectDrawing,
        onClearDrawings = viewModel::clearDrawings,
        mapControllerFactory = mapControllerFactory,
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
    onSelectDrawMode: (DrawMode) -> Boolean,
    onDiscardDraftAndSelectMode: (DrawMode) -> Unit,
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
    onDeleteSelectedDrawing: () -> Unit,
    onSelectDrawing: (com.example.shadowmap.domain.DrawnObjectSelection?) -> Unit,
    onClearDrawings: () -> Unit,
    mapControllerFactory: MapboxShadowMapController.Factory,
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
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val controller = remember(mapView, mapControllerFactory) {
        mapView?.let(mapControllerFactory::create)
    }
    var satelliteSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var loadRequest by remember { mutableIntStateOf(0) }
    var buildingQueryLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var show3d by remember { mutableStateOf(false) }
    var showSatelliteIn3d by remember { mutableStateOf(true) }
    var sceneViewport by remember { mutableStateOf<SceneViewport?>(null) }
    var buildingLoadArea by remember { mutableStateOf<BuildingLoadArea?>(null) }
    var crosshairPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var showToolChooser by remember { mutableStateOf(false) }
    var pendingModeSwitch by remember { mutableStateOf<DrawMode?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showReloadConfirmation by remember { mutableStateOf(false) }
    var showDraft3dConfirmation by remember { mutableStateOf(false) }

    DisposableEffect(mapView) {
        val currentMapView = mapView
        if (currentMapView == null) {
            buildingLoadArea = null
            onDispose { }
        } else {
            fun updateBuildingLoadArea() {
                currentMapView.post {
                    buildingLoadArea = currentMapView.toBuildingLoadArea()
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
                onSuccess = { buildings -> onBuildingsLoaded(buildings, calculationLocation) },
                onFailure = onLoadFailed
            )
        } finally {
            satelliteSnapshot = null
        }
    }

    LaunchedEffect(controller, uiState, crosshairPoint) {
        if (controller != null && uiState.buildingLoadState is BuildingLoadState.Loaded) {
            controller.render(
                loadedBuildings = uiState.loadedBuildings,
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

    fun enter3d() {
        sceneViewport = mapView?.toSceneViewport()
        show3d = true
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
            scaleBar = { ScaleBar(modifier = Modifier.safeDrawingPadding()) },
            logo = { Logo(modifier = Modifier.safeDrawingPadding()) },
            attribution = { Attribution(modifier = Modifier.safeDrawingPadding()) },
            style = { MapboxStandardSatelliteStyle() }
        ) {
            MapEffect(Unit) { currentMapView -> mapView = currentMapView }
        }

        if (!show3d && uiState.activeDrawMode != null) {
            DrawingCrosshair(modifier = Modifier.align(Alignment.Center))
        }

        if (show3d && !showSatelliteIn3d) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(Scene3DAppearance.HIDDEN_MAP_BACKDROP_ARGB))
            )
        }

        if (show3d) {
            FilamentBuildingView(
                buildings = uiState.buildings,
                walls = uiState.drawnWalls,
                trees = uiState.drawnTrees,
                viewport = sceneViewport,
                azimuth = uiState.solarPosition?.azimuthDegrees?.toFloat()
                    ?: Scene3DAppearance.DEFAULT_SUN_AZIMUTH_DEGREES,
                zenith = uiState.solarPosition?.zenithDegrees?.toFloat()
                    ?: Scene3DAppearance.DEFAULT_SUN_ZENITH_DEGREES,
                sunVisible = uiState.solarPosition?.isAboveHorizon == true,
                modifier = Modifier.fillMaxSize()
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (!show3d) {
                BuildingLoadButton(
                    loadState = uiState.buildingLoadState,
                    loadArea = buildingLoadArea,
                    onClick = {
                        if (uiState.hasDrawings || uiState.hasDraft) {
                            showReloadConfirmation = true
                        } else {
                            startBuildingLoad()
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            } else {
                Button(
                    onClick = { showSatelliteIn3d = !showSatelliteIn3d },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = dimensions.screenPadding, end = dimensions.screenPadding)
                ) {
                    Text(if (showSatelliteIn3d) "Hide satellite" else "Show satellite")
                }
            }
            if (uiState.buildings.isNotEmpty() || uiState.drawnWalls.isNotEmpty() || uiState.drawnTrees.isNotEmpty()) {
                Button(
                    onClick = {
                        if (show3d) {
                            show3d = false
                        } else if (uiState.hasDraft) {
                            showDraft3dConfirmation = true
                        } else {
                            enter3d()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = dimensions.screenPadding, start = dimensions.screenPadding)
                ) { Text(if (show3d) "Map View" else "3D View") }
            }

            BuildingLoadError(
                loadState = uiState.buildingLoadState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            DateTimeSpinner(
                selectedEpochMillis = uiState.selectedEpochMillis,
                timeZoneId = uiState.displayTimeZoneId,
                onDateTimeChanged = onDateTimeChanged,
                onNowSelected = onNowSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (!show3d && uiState.buildingLoadState is BuildingLoadState.Loaded) {
                val mode = uiState.activeDrawMode
                if (mode == null) {
                    DrawingToolChooser(
                        expanded = showToolChooser,
                        onExpand = { showToolChooser = true },
                        onSelect = { selectedMode ->
                            if (!onSelectDrawMode(selectedMode)) {
                                pendingModeSwitch = selectedMode
                            }
                            showToolChooser = false
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = dimensions.screenPadding, bottom = 132.dp)
                    )
                    if (uiState.hasDrawings) {
                        OutlinedButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = dimensions.screenPadding, bottom = 132.dp)
                        ) { Text("Clear drawings") }
                    }
                } else {
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
                        onCancel = onStopDrawing,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 132.dp)
                    )
                }
            }

            uiState.drawingError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                        .padding(12.dp)
                )
            }
        }
    }

    pendingModeSwitch?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingModeSwitch = null },
            title = { Text("Discard drawing?") },
            text = { Text("Switching tools will discard the current unfinished drawing.") },
            confirmButton = {
                Button(onClick = {
                    onDiscardDraftAndSelectMode(mode)
                    pendingModeSwitch = null
                }) { Text("Discard and switch") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingModeSwitch = null }) { Text("Keep drawing") }
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear all drawings?") },
            text = { Text("Loaded buildings will remain on the map.") },
            confirmButton = {
                Button(onClick = {
                    onClearDrawings()
                    showClearConfirmation = false
                }) { Text("Clear") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showReloadConfirmation) {
        AlertDialog(
            onDismissRequest = { showReloadConfirmation = false },
            title = { Text("Clear drawings and reload?") },
            text = { Text("Loading buildings for a new area will remove all user drawings.") },
            confirmButton = {
                Button(onClick = {
                    onClearDrawings()
                    showReloadConfirmation = false
                    startBuildingLoad()
                }) { Text("Clear and load") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReloadConfirmation = false }) { Text("Cancel") }
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

    val pendingType = when (uiState.pendingDrawing) {
        is PendingDrawing.Building -> DrawnObjectType.BUILDING
        is PendingDrawing.Wall -> DrawnObjectType.WALL
        is PendingDrawing.Tree -> DrawnObjectType.TREE
        null -> null
    }
    val selectedType = uiState.selectedDrawing?.type
    val propertyType = pendingType ?: selectedType
    if (propertyType != null) {
        val selectionId = uiState.selectedDrawing?.id
        val selectedBuilding = uiState.drawnBuildings.find { it.id == selectionId }
        val selectedWall = uiState.drawnWalls.find { it.id == selectionId }
        val selectedTree = uiState.drawnTrees.find { it.id == selectionId }
        val initialHeight = selectedBuilding?.heightMeters
            ?: selectedWall?.heightMeters
            ?: selectedTree?.heightMeters
            ?: when (propertyType) {
                DrawnObjectType.BUILDING -> DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
                DrawnObjectType.WALL -> DEFAULT_DRAWN_WALL_HEIGHT_METERS
                DrawnObjectType.TREE -> DEFAULT_DRAWN_TREE_HEIGHT_METERS
            }
        DrawingPropertiesSheet(
            type = propertyType,
            initialHeightMeters = initialHeight,
            initialRadiusMeters = selectedTree?.radiusMeters
                ?: if (propertyType == DrawnObjectType.TREE) DEFAULT_DRAWN_TREE_RADIUS_METERS else null,
            isCreating = pendingType != null,
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
            onDelete = onDeleteSelectedDrawing
        )
    }
}

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

@Composable
private fun BuildingLoadButton(
    loadState: BuildingLoadState,
    loadArea: BuildingLoadArea?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    val isLoading = loadState is BuildingLoadState.Loading
    val isWithinLimit = loadArea?.isWithinLimit == true
    Surface(
        modifier = modifier
            .padding(top = dimensions.screenPadding, end = dimensions.screenPadding)
            .widthIn(max = dimensions.floatingControlMaxWidth),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = dimensions.spacingXxs
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(dimensions.spacingMedium)
        ) {
            Button(
                onClick = onClick,
                enabled = isWithinLimit && !isLoading
            ) {
                Text(
                    text = when {
                        isLoading -> "Loading buildings…"
                        isWithinLimit -> "Load buildings"
                        else -> "Zoom in to load buildings"
                    }
                )
            }
            Text(
                text = when {
                    loadArea == null -> "Checking visible map area…"
                    isWithinLimit -> "Area ready: ${loadArea.formattedDimensions}"
                    else -> "Current area: ${loadArea.formattedDimensions}\n" +
                        "Maximum: ${MAX_BUILDING_LOAD_DIMENSION_METERS.toInt()} m × " +
                        "${MAX_BUILDING_LOAD_DIMENSION_METERS.toInt()} m"
                },
                color = if (loadArea != null && !isWithinLimit) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(top = dimensions.spacingSmall)
            )
        }
    }
}

@Composable
private fun BuildingLoadError(loadState: BuildingLoadState, modifier: Modifier = Modifier) {
    if (loadState is BuildingLoadState.Error) {
        val dimensions = ShadowMapDesign.dimensions
        Text(
            text = loadState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(dimensions.screenPadding)
        )
    }
}

private const val MIN_POINT_SPACING_DP = 12f
