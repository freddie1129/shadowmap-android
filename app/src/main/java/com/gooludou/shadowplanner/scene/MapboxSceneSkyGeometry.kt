package com.gooludou.shadowplanner.scene

import com.gooludou.shadowplanner.domain.SolarPosition
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class MapboxSkyPoint(
    val eastMeters: Double,
    val northMeters: Double,
    val upMeters: Double
) {
    fun toMapboxModelTranslation(): DoubleArray = doubleArrayOf(
        eastMeters,
        -northMeters,
        upMeters
    )
}

enum class MapboxSkySegmentKind {
    PATH,
    CONNECTOR
}

data class MapboxSkySegment(
    val start: MapboxSkyPoint,
    val end: MapboxSkyPoint,
    val midpoint: MapboxSkyPoint,
    val lengthMeters: Double,
    val diameterMeters: Double,
    val longitudeRotationDegrees: Double,
    val latitudeRotationDegrees: Double,
    val verticalRotationDegrees: Double,
    val kind: MapboxSkySegmentKind
)

data class MapboxSkyMetrics(
    val domeRadiusMeters: Double,
    val outerRadiusMeters: Double,
    val metersPerPixel: Double,
    val pathDiameterMeters: Double,
    val connectorDiameterMeters: Double,
    val markerRadiusMeters: Double,
    val pathSurfaceOffsetMeters: Double
)

object MapboxSceneSkyGeometry {
    const val DOME_MODEL_OUTER_RADIUS = 1.22
    private const val MIN_SCALE_METERS = 0.001
    private const val MARKER_RADIUS_RATIO = 0.025
    private const val MIN_MARKER_RADIUS_METERS = 0.6
    private const val PATH_SURFACE_OFFSET_RATIO = 0.001
    private const val PATH_WIDTH_SURFACE_OFFSET_RATIO = 0.12

    fun calculateMetrics(
        viewportWidthMeters: Double,
        viewportHeightMeters: Double,
        viewportWidthPixels: Int,
        viewportHeightPixels: Int,
        edgePaddingPixels: Double,
        pathWidthPixels: Double,
        connectorWidthPixels: Double
    ): MapboxSkyMetrics {
        val safeWidthPixels = viewportWidthPixels.coerceAtLeast(1)
        val safeHeightPixels = viewportHeightPixels.coerceAtLeast(1)
        val availableWidthMeters = viewportWidthMeters *
            (1.0 - 2.0 * edgePaddingPixels / safeWidthPixels).coerceAtLeast(0.0)
        val availableHeightMeters = viewportHeightMeters *
            (1.0 - 2.0 * edgePaddingPixels / safeHeightPixels).coerceAtLeast(0.0)
        val outerRadiusMeters = minOf(availableWidthMeters, availableHeightMeters) / 2.0
        val domeRadiusMeters = (outerRadiusMeters / DOME_MODEL_OUTER_RADIUS)
            .coerceAtLeast(MIN_SCALE_METERS)
        val metersPerPixel = max(
            viewportWidthMeters / safeWidthPixels,
            viewportHeightMeters / safeHeightPixels
        )
        val pathDiameterMeters = (metersPerPixel * pathWidthPixels)
            .coerceAtLeast(MIN_SCALE_METERS)
        val connectorDiameterMeters = (metersPerPixel * connectorWidthPixels)
            .coerceAtLeast(MIN_SCALE_METERS)
        return MapboxSkyMetrics(
            domeRadiusMeters = domeRadiusMeters,
            outerRadiusMeters = outerRadiusMeters,
            metersPerPixel = metersPerPixel,
            pathDiameterMeters = pathDiameterMeters,
            connectorDiameterMeters = connectorDiameterMeters,
            markerRadiusMeters = max(
                domeRadiusMeters * MARKER_RADIUS_RATIO,
                MIN_MARKER_RADIUS_METERS
            ),
            pathSurfaceOffsetMeters = max(
                pathDiameterMeters * PATH_WIDTH_SURFACE_OFFSET_RATIO,
                domeRadiusMeters * PATH_SURFACE_OFFSET_RATIO
            )
        )
    }

    fun pointOnDome(position: SolarPosition, radiusMeters: Double): MapboxSkyPoint {
        val azimuthRadians = Math.toRadians(position.azimuthDegrees)
        val altitudeRadians = Math.toRadians(
            (90.0 - position.zenithDegrees).coerceIn(0.0, 90.0)
        )
        val horizontalMeters = cos(altitudeRadians) * radiusMeters
        return MapboxSkyPoint(
            eastMeters = sin(azimuthRadians) * horizontalMeters,
            northMeters = cos(azimuthRadians) * horizontalMeters,
            upMeters = sin(altitudeRadians) * radiusMeters
        )
    }

    fun pathSegments(
        positions: List<SolarPosition>,
        metrics: MapboxSkyMetrics
    ): List<MapboxSkySegment> {
        val pathRadius = metrics.domeRadiusMeters + metrics.pathSurfaceOffsetMeters
        return positions
            .filter(SolarPosition::isAboveHorizon)
            .map { pointOnDome(it, pathRadius) }
            .zipWithNext { start, end ->
                segmentBetween(
                    start = start,
                    end = end,
                    diameterMeters = metrics.pathDiameterMeters,
                    kind = MapboxSkySegmentKind.PATH
                )
            }
    }

    fun connector(
        position: SolarPosition?,
        metrics: MapboxSkyMetrics
    ): MapboxSkySegment? {
        if (position?.isAboveHorizon != true) return null
        return segmentBetween(
            start = MapboxSkyPoint(0.0, 0.0, 0.0),
            end = pointOnDome(position, metrics.domeRadiusMeters),
            diameterMeters = metrics.connectorDiameterMeters,
            kind = MapboxSkySegmentKind.CONNECTOR
        )
    }

    fun marker(position: SolarPosition?, metrics: MapboxSkyMetrics): MapboxSkyPoint? =
        position?.takeIf(SolarPosition::isAboveHorizon)?.let {
            pointOnDome(it, metrics.domeRadiusMeters)
        }

    private fun segmentBetween(
        start: MapboxSkyPoint,
        end: MapboxSkyPoint,
        diameterMeters: Double,
        kind: MapboxSkySegmentKind
    ): MapboxSkySegment {
        val east = end.eastMeters - start.eastMeters
        val north = end.northMeters - start.northMeters
        val up = end.upMeters - start.upMeters
        val horizontal = hypot(east, north)
        val tiltDegrees = Math.toDegrees(atan2(horizontal, up))
        val azimuthDegrees = Math.toDegrees(atan2(east, north))
        return MapboxSkySegment(
            start = start,
            end = end,
            midpoint = MapboxSkyPoint(
                eastMeters = (start.eastMeters + end.eastMeters) / 2.0,
                northMeters = (start.northMeters + end.northMeters) / 2.0,
                upMeters = (start.upMeters + end.upMeters) / 2.0
            ),
            lengthMeters = sqrt(east * east + north * north + up * up)
                .coerceAtLeast(MIN_SCALE_METERS),
            diameterMeters = diameterMeters.coerceAtLeast(MIN_SCALE_METERS),
            // Mapbox converts the glTF +Y axis to map +Z, then applies model rotations in
            // Z/X/Y order. Tilting around map X and rotating around map Z therefore aligns
            // the cylinder with the segment's east/north/up direction.
            longitudeRotationDegrees = tiltDegrees,
            latitudeRotationDegrees = 0.0,
            verticalRotationDegrees = azimuthDegrees,
            kind = kind
        )
    }
}
