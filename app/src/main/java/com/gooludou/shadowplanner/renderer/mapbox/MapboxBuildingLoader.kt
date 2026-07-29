package com.gooludou.shadowplanner.renderer.mapbox

import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.BuildingSource
import com.gooludou.shadowplanner.core.model.DEFAULT_BUILDING_HEIGHT_METERS
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

internal class MapboxBuildingLoader(private val mapView: MapView) {
    @OptIn(MapboxExperimental::class)
    suspend fun fetch(loadType: BuildingLoadType): List<Building> =
        withContext(Dispatchers.Main.immediate) {
            var switchedToStandard = false
            try {
                withTimeout(STYLE_OPERATION_TIMEOUT_MILLIS) {
                    awaitStyle(Style.STANDARD)
                    switchedToStandard = true
                    awaitMapIdle()
                    queryBuildings(loadType)
                }
            } finally {
                if (switchedToStandard) {
                    withContext(NonCancellable) {
                        val restored = withTimeoutOrNull(STYLE_OPERATION_TIMEOUT_MILLIS) {
                            awaitStyle(Style.STANDARD_SATELLITE)
                            awaitMapIdle()
                        }
                        checkNotNull(restored) { "Timed out restoring the satellite style" }
                    }
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
            subscription = mapView.mapboxMap.subscribeMapIdle {
                subscription?.cancel()
                if (continuation.isActive) continuation.resume(Unit)
            }
            continuation.invokeOnCancellation { subscription?.cancel() }
        }
    }

    @OptIn(MapboxExperimental::class)
    private suspend fun queryBuildings(loadType: BuildingLoadType): List<Building> =
        suspendCancellableCoroutine { continuation ->
            val queryGeometry = loadType.toQueryGeometry(mapView.width, mapView.height)
            mapView.mapboxMap.queryRenderedFeatures(StandardBuildings(), queryGeometry) { features ->
                if (continuation.isActive) {
                    continuation.resume(features.flatMap { it.toDomainFootprints() })
                }
            }
        }

    private fun StandardBuildingsFeature.toDomainFootprints(): List<Building> {
        val featureHeight = height?.takeIf { it > 0.0 } ?: DEFAULT_BUILDING_HEIGHT_METERS
        val featureId = id?.featureId ?: originalFeature.id()
        val featureNamespace = id?.featureNamespace
        return geometry.toGeoPolygons().map { polygon ->
            Building(
                id = featureId,
                polygon = polygon,
                heightMeters = featureHeight,
                minHeightMeters = minHeight ?: 0.0,
                source = BuildingSource.AUTOMATIC,
                automaticIdentity = AutomaticBuildingMatcher.identity(
                    featureId = featureId,
                    featureNamespace = featureNamespace,
                    polygon = polygon
                )
            )
        }
    }

    private fun Geometry.toGeoPolygons(): List<GeoPolygon> = when (this) {
        is Polygon -> listOf(coordinates().toDomainPolygon())
        is MultiPolygon -> coordinates().map { it.toDomainPolygon() }
        else -> emptyList()
    }

    private fun List<List<Point>>.toDomainPolygon(): GeoPolygon = GeoPolygon(
        rings = map { ring ->
            ring.map { point -> GeoPoint(point.longitude(), point.latitude()) }
        }
    )

    private companion object {
        const val STYLE_OPERATION_TIMEOUT_MILLIS = 20_000L
    }
}
