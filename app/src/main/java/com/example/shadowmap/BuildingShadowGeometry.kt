package com.example.shadowmap

import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon as JtsPolygon
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.operation.union.UnaryUnionOp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

data class BuildingFootprint(
    val id: String?,
    val rings: List<List<Point>>,
    val heightMeters: Double,
    val minHeightMeters: Double
)

private const val EARTH_RADIUS_METERS = 6_378_137.0
private const val MAX_ZENITH_DEGREES = 85.0
const val DEFAULT_BUILDING_HEIGHT_METERS = 10.0

private val geometryFactory = GeometryFactory()

fun calculateBuildingShadows(
    buildings: List<BuildingFootprint>,
    azimuthDegrees: Double,
    zenithDegrees: Double
): List<Polygon> = buildings.flatMap { building ->
    calculateBuildingShadow(building, azimuthDegrees, zenithDegrees)
}

internal fun calculateBuildingShadow(
    building: BuildingFootprint,
    azimuthDegrees: Double,
    zenithDegrees: Double
): List<Polygon> {
    val outerRing = building.rings.firstOrNull().orEmpty()
    if (outerRing.size < 4) return emptyList()

    val origin = outerRing.dropLast(1).let { points ->
        Point.fromLngLat(
            points.map(Point::longitude).average(),
            points.map(Point::latitude).average()
        )
    }
    val projection = LocalMeterProjection(origin)
    val footprint = building.rings.toJtsPolygon(projection) ?: return emptyList()

    val zenith = zenithDegrees.coerceIn(0.0, MAX_ZENITH_DEGREES) * PI / 180.0
    val shadowLength = max(0.0, building.heightMeters) * tan(zenith)
    val shadowBearing = (azimuthDegrees + 180.0) * PI / 180.0
    val offsetX = shadowLength * sin(shadowBearing)
    val offsetY = shadowLength * cos(shadowBearing)

    if (shadowLength == 0.0) return listOf(Polygon.fromLngLats(building.rings))

    val translatedRoof = footprint.translate(offsetX, offsetY)
    val pieces = mutableListOf<Geometry>(footprint, translatedRoof)
    val shell = footprint.exteriorRing.coordinates
    for (index in 0 until shell.lastIndex) {
        val start = shell[index]
        val end = shell[index + 1]
        pieces += geometryFactory.createPolygon(
            arrayOf(
                start.copy(),
                end.copy(),
                Coordinate(end.x + offsetX, end.y + offsetY),
                Coordinate(start.x + offsetX, start.y + offsetY),
                start.copy()
            )
        )
    }

    return UnaryUnionOp.union(pieces).toMapboxPolygons(projection)
}

private fun List<List<Point>>.toJtsPolygon(projection: LocalMeterProjection): JtsPolygon? {
    val shell = firstOrNull()?.toLinearRing(projection) ?: return null
    val holes = drop(1).mapNotNull { it.toLinearRing(projection) }.toTypedArray()
    return geometryFactory.createPolygon(shell, holes).takeIf { it.isValid }
}

private fun List<Point>.toLinearRing(projection: LocalMeterProjection): LinearRing? {
    if (size < 4) return null
    val coordinates = map(projection::toMeters).toMutableList()
    if (!coordinates.first().equals2D(coordinates.last())) {
        coordinates += coordinates.first().copy()
    }
    return geometryFactory.createLinearRing(coordinates.toTypedArray())
}

private fun JtsPolygon.translate(offsetX: Double, offsetY: Double): JtsPolygon {
    return AffineTransformation.translationInstance(offsetX, offsetY)
        .transform(this) as JtsPolygon
}

private fun Geometry.toMapboxPolygons(projection: LocalMeterProjection): List<Polygon> =
    when (this) {
        is JtsPolygon -> listOf(toMapboxPolygon(projection))
        else -> (0 until numGeometries).flatMap { getGeometryN(it).toMapboxPolygons(projection) }
    }

private fun JtsPolygon.toMapboxPolygon(projection: LocalMeterProjection): Polygon {
    val rings = buildList {
        add(exteriorRing.coordinates.map(projection::toPoint))
        for (index in 0 until numInteriorRing) {
            add(getInteriorRingN(index).coordinates.map(projection::toPoint))
        }
    }
    return Polygon.fromLngLats(rings)
}

internal class LocalMeterProjection(private val origin: Point) {
    private val originLatitudeRadians = origin.latitude() * PI / 180.0
    private val longitudeScale = EARTH_RADIUS_METERS * cos(originLatitudeRadians)
    private val latitudeScale = EARTH_RADIUS_METERS

    fun toMeters(point: Point): Coordinate = Coordinate(
        (point.longitude() - origin.longitude()) * PI / 180.0 * longitudeScale,
        (point.latitude() - origin.latitude()) * PI / 180.0 * latitudeScale
    )

    fun toPoint(coordinate: Coordinate): Point = Point.fromLngLat(
        origin.longitude() + coordinate.x / longitudeScale * 180.0 / PI,
        origin.latitude() + coordinate.y / latitudeScale * 180.0 / PI
    )
}
