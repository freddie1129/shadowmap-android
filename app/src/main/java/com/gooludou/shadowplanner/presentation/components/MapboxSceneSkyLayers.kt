package com.gooludou.shadowplanner.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.SolarPosition
import com.gooludou.shadowplanner.scene.MapboxSceneSkyGeometry
import com.gooludou.shadowplanner.scene.MapboxSkyMetrics
import com.gooludou.shadowplanner.scene.MapboxSkyPoint
import com.gooludou.shadowplanner.scene.MapboxSkySegment
import com.gooludou.shadowplanner.scene.MapboxSkySegmentKind
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.ModelIdValue
import com.mapbox.maps.extension.compose.style.layers.generated.ModelLayer
import com.mapbox.maps.extension.compose.style.layers.generated.ModelLayerState
import com.mapbox.maps.extension.compose.style.layers.generated.ModelTypeValue
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.generated.Expression

internal class MapboxSceneSkyLayers(
    initialViewport: MapboxScene3DViewport,
    private val edgePaddingPixels: Double,
    private val pathWidthPixels: Double,
    private val connectorWidthPixels: Double,
    val domeSource: GeoJsonSourceState,
    val segmentSource: GeoJsonSourceState,
    val markerSource: GeoJsonSourceState
) {
    var center by mutableStateOf(initialViewport.center)
        private set

    var metrics by mutableStateOf(calculateMetrics(initialViewport))
        private set

    fun updateViewport(viewport: MapboxScene3DViewport) {
        metrics = calculateMetrics(viewport)
        center = viewport.center
    }

    private fun calculateMetrics(viewport: MapboxScene3DViewport): MapboxSkyMetrics =
        MapboxSceneSkyGeometry.calculateMetrics(
            viewportWidthMeters = viewport.widthMeters,
            viewportHeightMeters = viewport.heightMeters,
            viewportWidthPixels = viewport.widthPixels,
            viewportHeightPixels = viewport.heightPixels,
            edgePaddingPixels = edgePaddingPixels,
            pathWidthPixels = pathWidthPixels,
            connectorWidthPixels = connectorWidthPixels
        )
}

@Composable
internal fun rememberMapboxSceneSkyState(
    viewport: MapboxScene3DViewport,
    solarPosition: SolarPosition?,
    sunPath: List<SolarPosition>
): MapboxSceneSkyLayers {
    val density = LocalDensity.current
    val edgePaddingPixels = with(density) { DOME_EDGE_PADDING.toPx().toDouble() }
    val pathWidthPixels = with(density) { SUN_PATH_WIDTH.toPx().toDouble() }
    val connectorWidthPixels = with(density) { SUN_CONNECTOR_WIDTH.toPx().toDouble() }
    val state = remember(
        viewport,
        edgePaddingPixels,
        pathWidthPixels,
        connectorWidthPixels
    ) {
        val initialFeature = Feature.fromGeometry(viewport.center.toMapboxPoint())
        MapboxSceneSkyLayers(
            initialViewport = viewport,
            edgePaddingPixels = edgePaddingPixels,
            pathWidthPixels = pathWidthPixels,
            connectorWidthPixels = connectorWidthPixels,
            domeSource = GeoJsonSourceState(DOME_SOURCE_ID).apply {
                data = GeoJSONData(listOf(initialFeature))
            },
            segmentSource = GeoJsonSourceState(SUN_SEGMENT_SOURCE_ID).apply {
                data = GeoJSONData(emptyList())
            },
            markerSource = GeoJsonSourceState(SUN_MARKER_SOURCE_ID).apply {
                data = GeoJSONData(emptyList())
            }
        )
    }
    val metrics = state.metrics
    val segmentFeatures = remember(state.center, metrics, sunPath, solarPosition) {
        val center = state.center.toMapboxPoint()
        MapboxSceneSkyGeometry.pathSegments(sunPath, metrics)
            .map { it.toMapboxFeature(center) } +
            listOfNotNull(
                MapboxSceneSkyGeometry.connector(solarPosition, metrics)
                    ?.toMapboxFeature(center)
            )
    }
    val markerFeatures = remember(state.center, metrics, solarPosition) {
        val marker = MapboxSceneSkyGeometry.marker(solarPosition, metrics)
        listOfNotNull(marker?.toMapboxMarkerFeature(state.center.toMapboxPoint()))
    }
    LaunchedEffect(state.center) {
        state.domeSource.data = GeoJSONData(
            listOf(Feature.fromGeometry(state.center.toMapboxPoint()))
        )
    }
    LaunchedEffect(segmentFeatures) {
        state.segmentSource.data = GeoJSONData(segmentFeatures)
    }
    LaunchedEffect(markerFeatures) {
        state.markerSource.data = GeoJSONData(markerFeatures)
    }
    return state
}

@Composable
@OptIn(MapboxExperimental::class)
internal fun SceneSkyModelLayers(state: MapboxSceneSkyLayers) {
    SceneDomeModelLayer(state.domeSource, state.metrics)
    SceneSunSegmentModelLayer(state.segmentSource)
    SceneSunMarkerModelLayer(state.markerSource, state.metrics)
}

@Composable
@OptIn(MapboxExperimental::class)
private fun SceneDomeModelLayer(source: GeoJsonSourceState, metrics: MapboxSkyMetrics) {
    val radius = metrics.domeRadiusMeters
    ModelLayer(sourceState = source, layerId = DOME_LAYER_ID) {
        modelId = ModelIdValue(modelId = DOME_MODEL_ID, uri = DOME_MODEL_URI)
        modelType = ModelTypeValue.COMMON_3D
        modelScale = DoubleListValue(listOf(radius, radius, radius))
        applySkyModelAppearance()
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun SceneSunSegmentModelLayer(source: GeoJsonSourceState) {
    ModelLayer(sourceState = source, layerId = SUN_SEGMENT_LAYER_ID) {
        modelId = ModelIdValue(modelId = SUN_SEGMENT_MODEL_ID, uri = SUN_SEGMENT_MODEL_URI)
        modelType = ModelTypeValue.COMMON_3D
        modelTranslation = DoubleListValue(Expression.get(PROPERTY_MODEL_TRANSLATION))
        modelScale = DoubleListValue(Expression.get(PROPERTY_MODEL_SCALE))
        modelRotation = DoubleListValue(Expression.get(PROPERTY_MODEL_ROTATION))
        modelColor = ColorValue(Expression.toColor(Expression.get(PROPERTY_MODEL_COLOR)))
        modelColorMixIntensity = DoubleValue(1.0)
        modelAllowDensityReduction = BooleanValue(false)
        applySkyModelAppearance()
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun SceneSunMarkerModelLayer(
    source: GeoJsonSourceState,
    metrics: MapboxSkyMetrics
) {
    val radius = metrics.markerRadiusMeters
    ModelLayer(sourceState = source, layerId = SUN_MARKER_LAYER_ID) {
        modelId = ModelIdValue(modelId = SUN_MARKER_MODEL_ID, uri = SUN_MARKER_MODEL_URI)
        modelType = ModelTypeValue.COMMON_3D
        modelTranslation = DoubleListValue(Expression.get(PROPERTY_MODEL_TRANSLATION))
        modelScale = DoubleListValue(listOf(radius, radius, radius))
        modelColor = ColorValue(SUN_MARKER_COLOR)
        modelColorMixIntensity = DoubleValue(1.0)
        modelAllowDensityReduction = BooleanValue(false)
        applySkyModelAppearance()
    }
}

private fun ModelLayerState.applySkyModelAppearance() {
    modelCastShadows = BooleanValue(false)
    modelReceiveShadows = BooleanValue(false)
    modelAmbientOcclusionIntensity = DoubleValue(0.0)
    modelEmissiveStrength = DoubleValue(1.0)
}

private fun MapboxSkySegment.toMapboxFeature(center: Point): Feature =
    Feature.fromGeometry(center).apply {
        addNumberArrayProperty(
            PROPERTY_MODEL_TRANSLATION,
            *midpoint.toMapboxModelTranslation()
        )
        addNumberArrayProperty(
            PROPERTY_MODEL_SCALE,
            diameterMeters,
            diameterMeters,
            lengthMeters
        )
        addNumberArrayProperty(
            PROPERTY_MODEL_ROTATION,
            longitudeRotationDegrees,
            latitudeRotationDegrees,
            verticalRotationDegrees
        )
        addStringProperty(
            PROPERTY_MODEL_COLOR,
            when (kind) {
                MapboxSkySegmentKind.PATH -> SUN_PATH_COLOR_HEX
                MapboxSkySegmentKind.CONNECTOR -> SUN_CONNECTOR_COLOR_HEX
            }
        )
    }

private fun MapboxSkyPoint.toMapboxMarkerFeature(center: Point): Feature =
    Feature.fromGeometry(center).apply {
        addNumberArrayProperty(
            PROPERTY_MODEL_TRANSLATION,
            *toMapboxModelTranslation()
        )
    }

private fun Feature.addNumberArrayProperty(name: String, vararg values: Double) {
    properties()?.add(
        name,
        JsonArray().apply { values.forEach(::add) }
    )
}

private fun GeoPoint.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

private const val DOME_SOURCE_ID = "scene-dome-source"
private const val DOME_LAYER_ID = "scene-dome-layer"
private const val DOME_MODEL_ID = "scene-dome-model"
private const val DOME_MODEL_URI = "asset://scene_dome.glb"
private const val SUN_SEGMENT_SOURCE_ID = "scene-sun-segment-source"
private const val SUN_MARKER_SOURCE_ID = "scene-sun-marker-source"
private const val SUN_SEGMENT_LAYER_ID = "scene-sun-segment-layer"
private const val SUN_MARKER_LAYER_ID = "scene-sun-marker-layer"
private const val SUN_SEGMENT_MODEL_ID = "scene-sun-segment-model"
private const val SUN_MARKER_MODEL_ID = "scene-sun-marker-model"
private const val SUN_SEGMENT_MODEL_URI = "asset://scene_sun_segment.glb"
private const val SUN_MARKER_MODEL_URI = "asset://scene_sun_sphere.glb"
private const val PROPERTY_MODEL_TRANSLATION = "model_translation"
private const val PROPERTY_MODEL_SCALE = "model_scale"
private const val PROPERTY_MODEL_ROTATION = "model_rotation"
private const val PROPERTY_MODEL_COLOR = "model_color"
private const val SUN_PATH_COLOR_HEX = "#FFFFB547"
private const val SUN_CONNECTOR_COLOR_HEX = "#FFFFE082"
private val SUN_MARKER_COLOR = Color(0xFFFFD54F)
private val DOME_EDGE_PADDING = 32.dp
private val SUN_PATH_WIDTH = 2.dp
private val SUN_CONNECTOR_WIDTH = 1.dp
