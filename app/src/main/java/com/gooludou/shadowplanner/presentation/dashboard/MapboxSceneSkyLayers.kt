package com.gooludou.shadowplanner.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
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
    val edgePaddingPixels = with(density) { SceneSkyStyle.DOME_EDGE_PADDING.toPx().toDouble() }
    val pathWidthPixels = with(density) { SceneSkyStyle.SUN_PATH_WIDTH.toPx().toDouble() }
    val connectorWidthPixels = with(density) {
        SceneSkyStyle.SUN_CONNECTOR_WIDTH.toPx().toDouble()
    }
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
            domeSource = GeoJsonSourceState(SceneDomeGlb.SOURCE_ID).apply {
                data = GeoJSONData(listOf(initialFeature))
            },
            segmentSource = GeoJsonSourceState(SceneSunSegmentGlb.SOURCE_ID).apply {
                data = GeoJSONData(emptyList())
            },
            markerSource = GeoJsonSourceState(SceneSunMarkerGlb.SOURCE_ID).apply {
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
    ModelLayer(sourceState = source, layerId = SceneDomeGlb.LAYER_ID) {
        modelId = ModelIdValue(modelId = SceneDomeGlb.MODEL_ID, uri = SceneDomeGlb.MODEL_URI)
        modelType = ModelTypeValue.COMMON_3D
        modelScale = DoubleListValue(listOf(radius, radius, radius))
        applySkyModelAppearance()
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun SceneSunSegmentModelLayer(source: GeoJsonSourceState) {
    ModelLayer(sourceState = source, layerId = SceneSunSegmentGlb.LAYER_ID) {
        modelId = ModelIdValue(
            modelId = SceneSunSegmentGlb.MODEL_ID,
            uri = SceneSunSegmentGlb.MODEL_URI
        )
        modelType = ModelTypeValue.COMMON_3D
        modelTranslation = DoubleListValue(Expression.get(SceneSkyModelProperties.TRANSLATION))
        modelScale = DoubleListValue(Expression.get(SceneSkyModelProperties.SCALE))
        modelRotation = DoubleListValue(Expression.get(SceneSkyModelProperties.ROTATION))
        modelColor = ColorValue(Expression.toColor(Expression.get(SceneSkyModelProperties.COLOR)))
        modelColorMixIntensity = DoubleValue(1.0)
        modelAllowDensityReduction = BooleanValue(false)
        applySkyModelAppearance()
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun SceneSunMarkerModelLayer(source: GeoJsonSourceState, metrics: MapboxSkyMetrics) {
    val radius = metrics.markerRadiusMeters
    ModelLayer(sourceState = source, layerId = SceneSunMarkerGlb.LAYER_ID) {
        modelId = ModelIdValue(
            modelId = SceneSunMarkerGlb.MODEL_ID,
            uri = SceneSunMarkerGlb.MODEL_URI
        )
        modelType = ModelTypeValue.COMMON_3D
        modelTranslation = DoubleListValue(Expression.get(SceneSkyModelProperties.TRANSLATION))
        modelScale = DoubleListValue(listOf(radius, radius, radius))
        modelColor = ColorValue(SceneSunMarkerGlb.COLOR)
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
            SceneSkyModelProperties.TRANSLATION,
            *midpoint.toMapboxModelTranslation()
        )
        addNumberArrayProperty(
            SceneSkyModelProperties.SCALE,
            diameterMeters,
            diameterMeters,
            lengthMeters
        )
        addNumberArrayProperty(
            SceneSkyModelProperties.ROTATION,
            longitudeRotationDegrees,
            latitudeRotationDegrees,
            verticalRotationDegrees
        )
        addStringProperty(
            SceneSkyModelProperties.COLOR,
            when (kind) {
                MapboxSkySegmentKind.PATH -> SceneSunSegmentGlb.PATH_COLOR_HEX
                MapboxSkySegmentKind.CONNECTOR -> SceneSunSegmentGlb.CONNECTOR_COLOR_HEX
            }
        )
    }

private fun MapboxSkyPoint.toMapboxMarkerFeature(center: Point): Feature =
    Feature.fromGeometry(center).apply {
        addNumberArrayProperty(
            SceneSkyModelProperties.TRANSLATION,
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
