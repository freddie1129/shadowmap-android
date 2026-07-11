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
import com.mapbox.maps.extension.style.light.generated.DirectionalLight
import com.mapbox.maps.extension.style.light.generated.ambientLight
import com.mapbox.maps.extension.style.light.generated.directionalLight
import com.mapbox.maps.extension.style.light.setLight
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource

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

    // Set once the style finishes loading; slider callbacks push updates straight through it,
    // bypassing the Compose LightsState wrapper (its setStyleLights effect never reliably fires).
    val directionalLightRef = remember { mutableStateOf<DirectionalLight?>(null) }

    LaunchedEffect(startBuildingFetch) {
        if (!startBuildingFetch) return@LaunchedEffect
        val currentMapView = mapView ?: return@LaunchedEffect

        // Let Compose display the snapshot before replacing the live map style.
        withFrameNanos { }
        startBuildingFetch = false
        fetchBuildingFootprints(currentMapView) {
            satelliteSnapshot = null
            isFetchingBuildings = false
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
                    directionalLightRef.value = directional
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
                    directionalLightRef.value?.direction(listOf(azimuth.toDouble(), zenith.toDouble()))
                }
            )
            Text(text = "Zenith: ${zenith.toInt()}°")
            Slider(
                value = zenith,
                valueRange = 0f..90f,
                onValueChange = {
                    zenith = it
                    directionalLightRef.value?.direction(listOf(azimuth.toDouble(), zenith.toDouble()))
                }
            )
        }
    }
}

private fun StandardBuildingsFeature.toPolygonRings(): List<List<List<Point>>> {
    return geometry.toPolygonRings()
}

@OptIn(MapboxExperimental::class)
private fun fetchBuildingFootprints(
    mapView: MapView,
    onComplete: () -> Unit
) {
    val mapboxMap = mapView.mapboxMap
    mapboxMap.loadStyle(Style.STANDARD) {
        var idleSubscription: Cancelable? = null
        idleSubscription = mapboxMap.subscribeMapIdle {
            idleSubscription?.cancel()
            mapboxMap.queryRenderedFeatures(StandardBuildings(), null) { features ->
                val footprints = features.flatMap { it.toPolygonRings() }
                mapboxMap.loadStyle(Style.STANDARD_SATELLITE) { satelliteStyle ->
                    addBuildingFootprintLayers(satelliteStyle, footprints)
                    var satelliteIdleSubscription: Cancelable? = null
                    satelliteIdleSubscription = mapboxMap.subscribeMapIdle {
                        satelliteIdleSubscription?.cancel()
                        onComplete()
                    }
                }
            }
        }
    }
}

private fun addBuildingFootprintLayers(
    style: Style,
    footprints: List<List<List<Point>>>
) {
    val features = footprints.map { rings ->
        Feature.fromGeometry(Polygon.fromLngLats(rings))
    }

    style.addSource(
        geoJsonSource(BUILDINGS_SOURCE_ID) {
            featureCollection(FeatureCollection.fromFeatures(features))
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

private fun Geometry?.toPolygonRings(): List<List<List<Point>>> {
    return when (this) {
        is Polygon -> listOf(coordinates())
        is MultiPolygon -> coordinates()
        else -> emptyList()
    }
}
