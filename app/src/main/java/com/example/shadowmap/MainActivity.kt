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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.map.MapboxShadowMapController
import com.example.shadowmap.presentation.BuildingLoadState
import com.example.shadowmap.presentation.ShadowMapUiState
import com.example.shadowmap.presentation.ShadowMapViewModel
import com.example.shadowmap.presentation.components.DateTimeSpinner
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
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    LaunchedEffect(controller, uiState.buildings, uiState.shadows) {
        if (controller != null && uiState.buildings.isNotEmpty()) {
            controller.render(uiState.buildings, uiState.shadows)
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

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            compass = { Compass(modifier = Modifier.safeDrawingPadding()) },
            scaleBar = { ScaleBar(modifier = Modifier.safeDrawingPadding()) },
            logo = { Logo(modifier = Modifier.safeDrawingPadding()) },
            attribution = { Attribution(modifier = Modifier.safeDrawingPadding()) },
            style = { MapboxStandardSatelliteStyle() }
        ) {
            MapEffect(Unit) { currentMapView -> mapView = currentMapView }
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
                    onClick = load@{
                        val currentMapView = mapView ?: return@load
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
            if (uiState.buildings.isNotEmpty()) {
                Button(
                    onClick = {
                        if (!show3d) {
                            sceneViewport = mapView?.toSceneViewport()
                        }
                        show3d = !show3d
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
        }
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

@Composable
private fun BuildingLoadButton(
    loadState: BuildingLoadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = ShadowMapDesign.dimensions
    Button(
        onClick = onClick,
        enabled = loadState !is BuildingLoadState.Loading,
        modifier = modifier.padding(
            top = dimensions.screenPadding,
            end = dimensions.screenPadding,
        )
    ) {
        Text(
            text = if (loadState is BuildingLoadState.Loading) {
                "Loading buildings..."
            } else {
                "Show buildings"
            }
        )
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
