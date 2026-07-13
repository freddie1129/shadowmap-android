package com.example.shadowmap

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.map.MapboxShadowMapController
import com.example.shadowmap.presentation.BuildingLoadState
import com.example.shadowmap.presentation.ShadowMapUiState
import com.example.shadowmap.presentation.ShadowMapViewModel
import com.example.shadowmap.scene.FilamentBuildingView
import com.example.shadowmap.scene.Scene3DAppearance
import com.example.shadowmap.scene.SceneViewport
import com.example.shadowmap.ui.theme.ShadowMapTheme
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
        enableEdgeToEdge()
        setContent {
            ShadowMapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShadowMapRoute(
                        mapControllerFactory = mapControllerFactory,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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
        onAzimuthChanged = viewModel::onAzimuthChanged,
        onZenithChanged = viewModel::onZenithChanged,
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
    onAzimuthChanged: (Float) -> Unit,
    onZenithChanged: (Float) -> Unit,
    onLoadStarted: () -> Unit,
    onBuildingsLoaded: (List<BuildingFootprint>) -> Unit,
    onLoadFailed: (Throwable) -> Unit,
    mapControllerFactory: MapboxShadowMapController.Factory,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
        withFrameNanos { }
        try {
            val result = runCatching { controller.fetchBuildings() }
            val failure = result.exceptionOrNull()
            if (failure is CancellationException) throw failure
            result.fold(onSuccess = onBuildingsLoaded, onFailure = onLoadFailed)
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
                azimuth = uiState.azimuth,
                zenith = uiState.zenith,
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

        if (!show3d) {
            BuildingLoadButton(
                loadState = uiState.buildingLoadState,
                onClick = load@{
                    val currentMapView = mapView ?: return@load
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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp)
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
                modifier = Modifier.align(Alignment.TopStart).padding(top = 24.dp, start = 24.dp)
            ) { Text(if (show3d) "Map View" else "3D View") }
        }

        BuildingLoadError(
            loadState = uiState.buildingLoadState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ShadowControls(
            uiState = uiState,
            onAzimuthChanged = onAzimuthChanged,
            onZenithChanged = onZenithChanged,
            modifier = Modifier.align(Alignment.BottomCenter)
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

@Composable
private fun BuildingLoadButton(
    loadState: BuildingLoadState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = loadState !is BuildingLoadState.Loading,
        modifier = modifier.padding(top = 24.dp, end = 24.dp)
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
        Text(
            text = loadState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(24.dp)
        )
    }
}

@Composable
private fun ShadowControls(
    uiState: ShadowMapUiState,
    onAzimuthChanged: (Float) -> Unit,
    onZenithChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Azimuth: ${uiState.azimuth.toInt()}°")
        Slider(
            value = uiState.azimuth,
            valueRange = 0f..360f,
            onValueChange = onAzimuthChanged
        )
        Text(text = "Zenith: ${uiState.zenith.toInt()}°")
        Slider(
            value = uiState.zenith,
            valueRange = 0f..85f,
            onValueChange = onZenithChanged
        )
    }
}
