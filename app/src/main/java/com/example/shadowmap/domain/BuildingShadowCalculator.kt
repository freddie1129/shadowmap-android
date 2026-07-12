package com.example.shadowmap.domain

import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.operation.union.UnaryUnionOp

class BuildingShadowCalculator
@Inject
constructor() {
    fun calculate(
        buildings: List<BuildingFootprint>,
        azimuthDegrees: Double,
        zenithDegrees: Double
    ): List<GeoPolygon> = buildings.flatMap { building ->
        calculateBuilding(building, azimuthDegrees, zenithDegrees)
    }

    internal fun calculateBuilding(
        building: BuildingFootprint,
        azimuthDegrees: Double,
        zenithDegrees: Double
    ): List<GeoPolygon> {
        val outerRing =
            building.polygon.rings
                .firstOrNull()
                .orEmpty()
        return if (outerRing.size < 4) {
            emptyList()
        } else {
            calculateProjectedShadow(building, outerRing, azimuthDegrees, zenithDegrees)
        }
    }

    private fun calculateProjectedShadow(
        building: BuildingFootprint,
        outerRing: List<GeoPoint>,
        azimuthDegrees: Double,
        zenithDegrees: Double
    ): List<GeoPolygon> {
        val originPoints = if (outerRing.first() ==
            outerRing.last()
        ) {
            outerRing.dropLast(1)
        } else {
            outerRing
        }
        val origin =
            GeoPoint(
                longitude = originPoints.map(GeoPoint::longitude).average(),
                latitude = originPoints.map(GeoPoint::latitude).average()
            )
        val projection = LocalMeterProjection(origin)
        val footprint = building.polygon.rings.toJtsPolygon(projection)
        return if (footprint == null) {
            emptyList()
        } else {
            calculateValidShadow(building, footprint, projection, azimuthDegrees, zenithDegrees)
        }
    }

    private fun calculateValidShadow(
        building: BuildingFootprint,
        footprint: Polygon,
        projection: LocalMeterProjection,
        azimuthDegrees: Double,
        zenithDegrees: Double
    ): List<GeoPolygon> {
        val zenith = zenithDegrees.coerceIn(0.0, MAX_ZENITH_DEGREES) * PI / 180.0
        val shadowLength = max(0.0, building.heightMeters) * tan(zenith)
        val shadowBearing = (azimuthDegrees + 180.0) * PI / 180.0
        val offsetX = shadowLength * sin(shadowBearing)
        val offsetY = shadowLength * cos(shadowBearing)

        return if (shadowLength == 0.0) {
            listOf(building.polygon)
        } else {
            val pieces =
                mutableListOf<Geometry>(
                    footprint,
                    AffineTransformation.translationInstance(offsetX, offsetY).transform(footprint)
                )
            val shell = footprint.exteriorRing.coordinates
            for (index in 0 until shell.lastIndex) {
                val start = shell[index]
                val end = shell[index + 1]
                pieces +=
                    geometryFactory.createPolygon(
                        arrayOf(
                            start.copy(),
                            end.copy(),
                            Coordinate(end.x + offsetX, end.y + offsetY),
                            Coordinate(start.x + offsetX, start.y + offsetY),
                            start.copy()
                        )
                    )
            }
            UnaryUnionOp.union(pieces).toGeoPolygons(projection)
        }
    }

    private fun List<List<GeoPoint>>.toJtsPolygon(projection: LocalMeterProjection): Polygon? {
        val shell = firstOrNull()?.toLinearRing(projection) ?: return null
        val holes = drop(1).mapNotNull { it.toLinearRing(projection) }.toTypedArray()
        return geometryFactory.createPolygon(shell, holes).takeIf { it.isValid }
    }

    private fun List<GeoPoint>.toLinearRing(projection: LocalMeterProjection): LinearRing? {
        if (size < 4) return null
        val coordinates = map(projection::toMeters).toMutableList()
        if (!coordinates.first().equals2D(coordinates.last())) {
            coordinates += coordinates.first().copy()
        }
        return geometryFactory.createLinearRing(coordinates.toTypedArray())
    }

    private fun Geometry.toGeoPolygons(projection: LocalMeterProjection): List<GeoPolygon> =
        when (this) {
            is Polygon -> listOf(toGeoPolygon(projection))

            else -> (0 until numGeometries).flatMap {
                getGeometryN(it).toGeoPolygons(projection)
            }
        }

    private fun Polygon.toGeoPolygon(projection: LocalMeterProjection): GeoPolygon = GeoPolygon(
        rings =
            buildList {
                add(exteriorRing.coordinates.map(projection::toPoint))
                for (index in 0 until numInteriorRing) {
                    add(getInteriorRingN(index).coordinates.map(projection::toPoint))
                }
            }
    )

    companion object {
        private const val MAX_ZENITH_DEGREES = 85.0
        private val geometryFactory = GeometryFactory()
    }
}

internal class LocalMeterProjection(private val origin: GeoPoint) {
    private val originLatitudeRadians = origin.latitude * PI / 180.0
    private val longitudeScale = EARTH_RADIUS_METERS * cos(originLatitudeRadians)

    fun toMeters(point: GeoPoint): Coordinate = Coordinate(
        (point.longitude - origin.longitude) * PI / 180.0 * longitudeScale,
        (point.latitude - origin.latitude) * PI / 180.0 * EARTH_RADIUS_METERS
    )

    fun toPoint(coordinate: Coordinate): GeoPoint = GeoPoint(
        longitude = origin.longitude + coordinate.x / longitudeScale * 180.0 / PI,
        latitude = origin.latitude + coordinate.y / EARTH_RADIUS_METERS * 180.0 / PI
    )

    companion object {
        private const val EARTH_RADIUS_METERS = 6_378_137.0
    }
}
