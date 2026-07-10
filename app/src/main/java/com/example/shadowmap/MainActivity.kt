package com.example.shadowmap

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadowmap.ui.theme.ShadowMapTheme
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.style.light.generated.DirectionalLight
import com.mapbox.maps.extension.style.light.generated.ambientLight
import com.mapbox.maps.extension.style.light.generated.directionalLight
import com.mapbox.maps.extension.style.light.setLight

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

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(153.0251, -27.4698)) // Brisbane CBD
            zoom(16.5)
            pitch(60.0)
        }
    }

    var azimuth by remember { mutableFloatStateOf(DEFAULT_AZIMUTH) }
    var zenith by remember { mutableFloatStateOf(DEFAULT_ZENITH) }

    // Set once the style finishes loading; slider callbacks push updates straight through it,
    // bypassing the Compose LightsState wrapper (its setStyleLights effect never reliably fires).
    val directionalLightRef = remember { mutableStateOf<DirectionalLight?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.getStyle { style ->
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
