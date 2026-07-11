package com.example.shadowmap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import com.example.shadowmap.ui.theme.ShadowMapTheme
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature
import com.mapbox.maps.extension.style.light.generated.ambientLight
import com.mapbox.maps.extension.style.light.generated.directionalLight
import com.mapbox.maps.extension.style.light.setLight
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShadowMapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Matches the Standard style's built-in "day" light preset: near-overhead sun from the south.
private const val DEFAULT_AZIMUTH = 180f
private const val DEFAULT_ZENITH = 20f
private const val BUILDINGS_SOURCE_ID = "queried-buildings-source"
private const val BUILDINGS_FILL_LAYER_ID = "queried-buildings-fill"
private const val BUILDINGS_LINE_LAYER_ID = "queried-buildings-outline"
private const val SHADOWS_SOURCE_ID = "calculated-building-shadows-source"
private const val SHADOWS_FILL_LAYER_ID = "calculated-building-shadows-fill"

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(153.4038943, -28.0870458)) // Brisbane CBD
            zoom(17.0)
           // pitch(60.0)
        }
    }

    var azimuth by remember { mutableFloatStateOf(DEFAULT_AZIMUTH) }
    var zenith by remember { mutableFloatStateOf(DEFAULT_ZENITH) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isFetchingBuildings by remember { mutableStateOf(false) }
    var satelliteSnapshot by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var startBuildingFetch by remember { mutableStateOf(false) }
    var buildings by remember { mutableStateOf<List<BuildingFootprint>>(emptyList()) }

    LaunchedEffect(startBuildingFetch) {
        if (!startBuildingFetch) return@LaunchedEffect
        val currentMapView = mapView ?: return@LaunchedEffect

        // Let Compose display the snapshot before replacing the live map style.
        withFrameNanos { }
        startBuildingFetch = false
        fetchBuildingFootprints(currentMapView, azimuth, zenith) { fetchedBuildings ->
            buildings = fetchedBuildings
            satelliteSnapshot = null
            isFetchingBuildings = false
        }
    }

    LaunchedEffect(buildings, azimuth, zenith, mapView) {
        if (buildings.isEmpty()) return@LaunchedEffect

        delay(50)
        val shadows = withContext(Dispatchers.Default) {
            calculateBuildingShadows(buildings, azimuth.toDouble(), zenith.toDouble())
        }
        mapView?.mapboxMap?.getStyle { style ->
            style.getSourceAs<GeoJsonSource>(SHADOWS_SOURCE_ID)?.featureCollection(
                shadows.toFeatureCollection()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapboxStandardSatelliteStyle() }
        ) {
            MapEffect(Unit) { currentMapView ->
                mapView = currentMapView
                currentMapView.mapboxMap.getStyle { style ->
                    val ambient = ambientLight {
                        color(Color.WHITE)
                        intensity(0.5)
                    }
                    val directional = directionalLight {
                        castShadows(true)
                        direction(listOf(azimuth.toDouble(), zenith.toDouble()))
                    }
                    style.setLight(ambient, directional)
                }
            }
        }

        satelliteSnapshot?.let { snapshot ->
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        Button(
            onClick = {
                val currentMapView = mapView ?: return@Button
                isFetchingBuildings = true
                currentMapView.snapshot { bitmap ->
                    currentMapView.post {
                        satelliteSnapshot = bitmap
                        startBuildingFetch = true
                    }
                }
            },
            enabled = !isFetchingBuildings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        ) {
            Text(text = if (isFetchingBuildings) "Loading buildings..." else "Show buildings")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Azimuth: ${azimuth.toInt()}°")
            Slider(
                value = azimuth,
                valueRange = 0f..360f,
                onValueChange = {
                    azimuth = it
                }
            )
            Text(text = "Zenith: ${zenith.toInt()}°")
            Slider(
                value = zenith,
                valueRange = 0f..85f,
                onValueChange = {
                    zenith = it
                }
            )
        }
    }
}

private fun StandardBuildingsFeature.toBuildingFootprints(): List<BuildingFootprint> {
    val featureHeight = height?.takeIf { it > 0.0 } ?: DEFAULT_BUILDING_HEIGHT_METERS
    return geometry.toPolygonRings().map { rings ->
        BuildingFootprint(
            id = originalFeature.id(),
            rings = rings,
            heightMeters = featureHeight,
            minHeightMeters = minHeight ?: 0.0
        )
    }
}

@OptIn(MapboxExperimental::class)
private fun fetchBuildingFootprints(
    mapView: MapView,
    azimuth: Float,
    zenith: Float,
    onComplete: (List<BuildingFootprint>) -> Unit
) {
    val mapboxMap = mapView.mapboxMap
    mapboxMap.loadStyle(Style.STANDARD) {
        var idleSubscription: Cancelable? = null
        idleSubscription = mapboxMap.subscribeMapIdle {
            idleSubscription?.cancel()
            mapboxMap.queryRenderedFeatures(StandardBuildings(), null) { features ->
                val buildings = features.flatMap { it.toBuildingFootprints() }
                mapboxMap.loadStyle(Style.STANDARD_SATELLITE) { satelliteStyle ->
                    addBuildingLayers(
                        style = satelliteStyle,
                        buildings = buildings,
                        azimuth = azimuth.toDouble(),
                        zenith = zenith.toDouble()
                    )
                    var satelliteIdleSubscription: Cancelable? = null
                    satelliteIdleSubscription = mapboxMap.subscribeMapIdle {
                        satelliteIdleSubscription?.cancel()
                        onComplete(buildings)
                    }
                }
            }
        }
    }
}

private fun addBuildingLayers(
    style: Style,
    buildings: List<BuildingFootprint>,
    azimuth: Double,
    zenith: Double
) {
    val buildingFeatures = buildings.map { building ->
        Feature.fromGeometry(Polygon.fromLngLats(building.rings))
    }
    val shadowFeatures = calculateBuildingShadows(buildings, azimuth, zenith)

    style.addSource(
        geoJsonSource(SHADOWS_SOURCE_ID) {
            featureCollection(shadowFeatures.toFeatureCollection())
        }
    )
    style.addLayer(
        fillLayer(SHADOWS_FILL_LAYER_ID, SHADOWS_SOURCE_ID) {
            fillColor("#111820")
            fillOpacity(0.55)
        }
    )

    style.addSource(
        geoJsonSource(BUILDINGS_SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(buildingFeatures))
        }
    )
    style.addLayer(
        fillLayer(BUILDINGS_FILL_LAYER_ID, BUILDINGS_SOURCE_ID) {
            fillColor("#4CAF50")
            fillOpacity(0.25)
        }
    )
    style.addLayer(
        lineLayer(BUILDINGS_LINE_LAYER_ID, BUILDINGS_SOURCE_ID) {
            lineColor("#0DFF72")
            lineOpacity(0.95)
            lineWidth(2.5)
        }
    )
}

private fun List<Polygon>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(map { Feature.fromGeometry(it) })

private fun Geometry?.toPolygonRings(): List<List<List<Point>>> {
    return when (this) {
        is Polygon -> listOf(coordinates())
        is MultiPolygon -> coordinates()
        else -> emptyList()
    }
}
