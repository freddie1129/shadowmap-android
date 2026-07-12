package com.example.shadowmap.map

import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.DEFAULT_BUILDING_HEIGHT_METERS
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
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
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class MapboxShadowMapController
@AssistedInject
constructor(
    @Assisted private val mapView: MapView
) {
    @OptIn(MapboxExperimental::class)
    suspend fun fetchBuildings(): List<BuildingFootprint> =
        withContext(Dispatchers.Main.immediate) {
            var switchedToStandard = false
            try {
                withTimeout(STYLE_OPERATION_TIMEOUT_MILLIS) {
                    awaitStyle(Style.STANDARD)
                    switchedToStandard = true
                    awaitMapIdle()
                    queryBuildings()
                }
            } finally {
                if (switchedToStandard) {
                    withContext(NonCancellable) {
                        val restored =
                            withTimeoutOrNull(STYLE_OPERATION_TIMEOUT_MILLIS) {
                                awaitStyle(Style.STANDARD_SATELLITE)
                                awaitMapIdle()
                            }
                        checkNotNull(restored) { "Timed out restoring the satellite style" }
                    }
                }
            }
        }

    fun render(buildings: List<BuildingFootprint>, shadows: List<GeoPolygon>) {
        mapView.mapboxMap.getStyle { style ->
            val shadowData = shadows.toFeatureCollection()
            val buildingData = buildings.map(BuildingFootprint::polygon).toFeatureCollection()

            val shadowSource = style.getSourceAs<GeoJsonSource>(SHADOWS_SOURCE_ID)
            if (shadowSource == null) {
                style.addSource(
                    geoJsonSource(SHADOWS_SOURCE_ID) { featureCollection(shadowData) }
                )
                style.addLayer(
                    fillLayer(SHADOWS_FILL_LAYER_ID, SHADOWS_SOURCE_ID) {
                        fillColor("#111820")
                        fillOpacity(0.55)
                    }
                )
            } else {
                shadowSource.featureCollection(shadowData)
            }

            val buildingSource = style.getSourceAs<GeoJsonSource>(BUILDINGS_SOURCE_ID)
            if (buildingSource == null) {
                style.addSource(
                    geoJsonSource(BUILDINGS_SOURCE_ID) { featureCollection(buildingData) }
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
            } else {
                buildingSource.featureCollection(buildingData)
            }
        }
    }

    private suspend fun awaitStyle(styleUri: String) {
        suspendCancellableCoroutine { continuation ->
            mapView.mapboxMap.loadStyle(styleUri) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun awaitMapIdle() {
        suspendCancellableCoroutine { continuation ->
            var subscription: Cancelable? = null
            subscription =
                mapView.mapboxMap.subscribeMapIdle {
                    subscription?.cancel()
                    if (continuation.isActive) continuation.resume(Unit)
                }
            continuation.invokeOnCancellation { subscription?.cancel() }
        }
    }

    @OptIn(MapboxExperimental::class)
    private suspend fun queryBuildings(): List<BuildingFootprint> =
        suspendCancellableCoroutine { continuation ->
            mapView.mapboxMap.queryRenderedFeatures(StandardBuildings(), null) { features ->
                if (continuation.isActive) {
                    continuation.resume(features.flatMap { it.toDomainFootprints() })
                }
            }
        }

    private fun StandardBuildingsFeature.toDomainFootprints(): List<BuildingFootprint> {
        val featureHeight = height?.takeIf { it > 0.0 } ?: DEFAULT_BUILDING_HEIGHT_METERS
        return geometry.toGeoPolygons().map { polygon ->
            BuildingFootprint(
                id = originalFeature.id(),
                polygon = polygon,
                heightMeters = featureHeight,
                minHeightMeters = minHeight ?: 0.0
            )
        }
    }

    private fun Geometry.toGeoPolygons(): List<GeoPolygon> = when (this) {
        is Polygon -> listOf(coordinates().toDomainPolygon())
        is MultiPolygon -> coordinates().map { it.toDomainPolygon() }
        else -> emptyList()
    }

    private fun List<List<Point>>.toDomainPolygon(): GeoPolygon = GeoPolygon(
        rings =
            map { ring ->
                ring.map { point -> GeoPoint(point.longitude(), point.latitude()) }
            }
    )

    private fun List<GeoPolygon>.toFeatureCollection(): FeatureCollection =
        FeatureCollection.fromFeatures(
            map { polygon -> Feature.fromGeometry(polygon.toMapboxPolygon()) }
        )

    private fun GeoPolygon.toMapboxPolygon(): Polygon = Polygon.fromLngLats(
        rings.map { ring ->
            ring.map { point -> Point.fromLngLat(point.longitude, point.latitude) }
        }
    )

    companion object {
        private const val STYLE_OPERATION_TIMEOUT_MILLIS = 20_000L
        private const val BUILDINGS_SOURCE_ID = "queried-buildings-source"
        private const val BUILDINGS_FILL_LAYER_ID = "queried-buildings-fill"
        private const val BUILDINGS_LINE_LAYER_ID = "queried-buildings-outline"
        private const val SHADOWS_SOURCE_ID = "calculated-building-shadows-source"
        private const val SHADOWS_FILL_LAYER_ID = "calculated-building-shadows-fill"
    }

    @AssistedFactory
    interface Factory {
        fun create(mapView: MapView): MapboxShadowMapController
    }
}
