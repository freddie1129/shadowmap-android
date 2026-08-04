package com.gooludou.shadowplanner.core.geometry

import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import kotlin.math.PI
import kotlin.math.cos
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

object DrawingGeometryValidator {
    @Suppress("ReturnCount")
    fun validateBuilding(vertices: List<GeoPoint>): DrawingValidationError? {
        val unique = vertices.distinctBy { it.longitude to it.latitude }
        if (unique.size < MIN_BUILDING_VERTEX_COUNT) {
            return DrawingValidationError.TOO_FEW_BUILDING_CORNERS
        }
        val origin = unique.first()
        val latitudeScale = EARTH_RADIUS_METERS * PI / 180.0
        val longitudeScale = latitudeScale * cos(origin.latitude * PI / 180.0)
        val coordinates = unique.map { point ->
            Coordinate(
                (point.longitude - origin.longitude) * longitudeScale,
                (point.latitude - origin.latitude) * latitudeScale
            )
        }.toMutableList()
        coordinates += coordinates.first().copy()
        val polygon = geometryFactory.createPolygon(coordinates.toTypedArray())
        if (!polygon.isValid) return DrawingValidationError.BUILDING_EDGES_CROSS
        if (polygon.area < MIN_BUILDING_AREA_SQUARE_METERS) {
            return DrawingValidationError.BUILDING_TOO_SMALL
        }
        return null
    }

    fun validateWall(points: List<GeoPoint>): DrawingValidationError? =
        if (points.distinctBy { it.longitude to it.latitude }.size < MIN_WALL_POINT_COUNT) {
            DrawingValidationError.TOO_FEW_WALL_POINTS
        } else {
            null
        }

    fun closedPolygon(vertices: List<GeoPoint>): GeoPolygon {
        val open = if (vertices.size > 1 && vertices.first() == vertices.last()) {
            vertices.dropLast(1)
        } else {
            vertices
        }
        return GeoPolygon(listOf(open + open.first()))
    }

    private const val MIN_BUILDING_VERTEX_COUNT = 3
    private const val MIN_WALL_POINT_COUNT = 2
    private const val MIN_BUILDING_AREA_SQUARE_METERS = 1.0
    private const val EARTH_RADIUS_METERS = 6_378_137.0
    private val geometryFactory = GeometryFactory()
}
