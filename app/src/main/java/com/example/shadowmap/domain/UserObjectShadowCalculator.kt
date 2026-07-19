package com.example.shadowmap.domain

import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

class UserObjectShadowCalculator @Inject constructor() {
    fun calculate(
        walls: List<DrawnWall>,
        trees: List<DrawnTree>,
        origin: GeoPoint,
        azimuthDegrees: Double,
        zenithDegrees: Double
    ): List<GeoPolygon> {
        val zenith = zenithDegrees.coerceIn(0.0, MAX_ZENITH_DEGREES) * PI / 180.0
        val shadowBearing = (azimuthDegrees + 180.0) * PI / 180.0
        val projection = LocalMeterProjection(origin)
        return walls.flatMap { wall ->
            val distance = wall.heightMeters.coerceAtLeast(0.0) * tan(zenith)
            val offsetX = distance * sin(shadowBearing)
            val offsetY = distance * cos(shadowBearing)
            wall.points.zipWithNext().map { (start, end) ->
                val a = projection.toMeters(start)
                val b = projection.toMeters(end)
                GeoPolygon(
                    listOf(
                        listOf(
                            projection.toPoint(a),
                            projection.toPoint(b),
                            projection.toPoint(Coordinate(b.x + offsetX, b.y + offsetY)),
                            projection.toPoint(Coordinate(a.x + offsetX, a.y + offsetY)),
                            projection.toPoint(a)
                        )
                    )
                )
            }
        } + trees.flatMap { tree ->
            treeShadow(tree, projection, shadowBearing, zenith)
        }
    }

    private fun treeShadow(
        tree: DrawnTree,
        projection: LocalMeterProjection,
        shadowBearing: Double,
        zenith: Double
    ): List<GeoPolygon> {
        val center = projection.toMeters(tree.center)
        val distance = tree.heightMeters.coerceAtLeast(0.0) * tan(zenith)
        val offsetX = distance * sin(shadowBearing)
        val offsetY = distance * cos(shadowBearing)
        val radius = tree.radiusMeters.coerceAtLeast(MIN_TREE_RADIUS_METERS)
        val coordinates = mutableListOf<Coordinate>()
        repeat(TREE_SHADOW_SEGMENTS) { index ->
            val angle = index.toDouble() / TREE_SHADOW_SEGMENTS * 2.0 * PI
            val dx = cos(angle) * radius
            val dy = sin(angle) * radius
            coordinates += Coordinate(center.x + dx, center.y + dy)
            coordinates += Coordinate(center.x + offsetX + dx, center.y + offsetY + dy)
        }
        return geometryFactory.createMultiPointFromCoords(coordinates.toTypedArray())
            .convexHull()
            .toGeoPolygons(projection)
    }

    private fun Geometry.toGeoPolygons(projection: LocalMeterProjection): List<GeoPolygon> =
        when (this) {
            is Polygon -> listOf(
                GeoPolygon(
                    buildList {
                        add(exteriorRing.coordinates.map(projection::toPoint))
                        repeat(numInteriorRing) { index ->
                            add(getInteriorRingN(index).coordinates.map(projection::toPoint))
                        }
                    }
                )
            )

            else -> (0 until numGeometries).flatMap { getGeometryN(it).toGeoPolygons(projection) }
        }

    private companion object {
        const val MAX_ZENITH_DEGREES = 85.0
        const val MIN_TREE_RADIUS_METERS = 0.5
        const val TREE_SHADOW_SEGMENTS = 16
        val geometryFactory = GeometryFactory()
    }
}
