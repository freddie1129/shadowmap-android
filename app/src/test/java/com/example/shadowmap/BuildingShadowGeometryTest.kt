package com.example.shadowmap

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingShadowGeometryTest {
    private val origin = Point.fromLngLat(153.0251, -27.4698)
    private val projection = LocalMeterProjection(origin)

    @Test
    fun localProjection_roundTripsCoordinates() {
        val point = Point.fromLngLat(153.026, -27.4689)

        val roundTrip = projection.toPoint(projection.toMeters(point))

        assertEquals(point.longitude(), roundTrip.longitude(), 1e-9)
        assertEquals(point.latitude(), roundTrip.latitude(), 1e-9)
    }

    @Test
    fun overheadSun_keepsShadowAtFootprint() {
        val building = squareBuilding(heightMeters = 20.0)

        val shadow = calculateBuildingShadow(building, azimuthDegrees = 0.0, zenithDegrees = 0.0)

        assertEquals(1, shadow.size)
        assertEquals(building.rings, shadow.single().coordinates())
    }

    @Test
    fun northSun_castsShadowSouthByBuildingHeightAt45Degrees() {
        val building = squareBuilding(heightMeters = 20.0)
        val footprintSouth = building.rings.first().minOf { projection.toMeters(it).y }

        val shadow = calculateBuildingShadow(building, azimuthDegrees = 0.0, zenithDegrees = 45.0)
        val shadowSouth = shadow.flatMap { it.coordinates().flatten() }
            .minOf { projection.toMeters(it).y }

        assertEquals(footprintSouth - 20.0, shadowSouth, 0.05)
    }

    @Test
    fun eastSun_castsShadowWest() {
        val building = squareBuilding(heightMeters = 12.0)
        val footprintWest = building.rings.first().minOf { projection.toMeters(it).x }

        val shadow = calculateBuildingShadow(building, azimuthDegrees = 90.0, zenithDegrees = 45.0)
        val shadowWest = shadow.flatMap { it.coordinates().flatten() }
            .minOf { projection.toMeters(it).x }

        assertEquals(footprintWest - 12.0, shadowWest, 0.05)
    }

    @Test
    fun nearHorizonZenith_isClamped() {
        val building = squareBuilding(heightMeters = 10.0)

        val shadow = calculateBuildingShadow(building, azimuthDegrees = 180.0, zenithDegrees = 90.0)
        val points = shadow.flatMap { it.coordinates().flatten() }

        assertTrue(points.all { it.longitude().isFinite() && it.latitude().isFinite() })
    }

    private fun squareBuilding(heightMeters: Double): BuildingFootprint {
        val ring = listOf(
            projection.toPoint(org.locationtech.jts.geom.Coordinate(-5.0, -5.0)),
            projection.toPoint(org.locationtech.jts.geom.Coordinate(5.0, -5.0)),
            projection.toPoint(org.locationtech.jts.geom.Coordinate(5.0, 5.0)),
            projection.toPoint(org.locationtech.jts.geom.Coordinate(-5.0, 5.0)),
            projection.toPoint(org.locationtech.jts.geom.Coordinate(-5.0, -5.0))
        )
        return BuildingFootprint(
            id = "test-building",
            rings = listOf(ring),
            heightMeters = heightMeters,
            minHeightMeters = 0.0
        )
    }
}
