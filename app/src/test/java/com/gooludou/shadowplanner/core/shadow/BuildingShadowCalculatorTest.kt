package com.gooludou.shadowplanner.core.shadow

import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.locationtech.jts.geom.Coordinate

class BuildingShadowCalculatorTest {
    private val origin = GeoPoint(153.0251, -27.4698)
    private val projection = LocalMeterProjection(origin)
    private val calculator = BuildingShadowCalculator()

    @Test
    fun localProjection_roundTripsCoordinates() {
        val point = GeoPoint(153.026, -27.4689)

        val roundTrip = projection.toPoint(projection.toMeters(point))

        assertEquals(point.longitude, roundTrip.longitude, 1e-9)
        assertEquals(point.latitude, roundTrip.latitude, 1e-9)
    }

    @Test
    fun overheadSun_keepsShadowAtFootprint() {
        val building = squareBuilding(heightMeters = 20.0)

        val shadow = calculator.calculateBuilding(building, 0.0, 0.0)

        assertEquals(listOf(building.polygon), shadow)
    }

    @Test
    fun northSun_castsShadowSouthByBuildingHeightAt45Degrees() {
        val building = squareBuilding(heightMeters = 20.0)
        val footprintSouth =
            building.polygon.rings
                .first()
                .minOf { projection.toMeters(it).y }

        val shadow = calculator.calculateBuilding(building, 0.0, 45.0)
        val shadowSouth = shadow.flatMap { it.rings.flatten() }.minOf { projection.toMeters(it).y }

        assertEquals(footprintSouth - 20.0, shadowSouth, 0.05)
    }

    @Test
    fun eastSun_castsShadowWest() {
        val building = squareBuilding(heightMeters = 12.0)
        val footprintWest =
            building.polygon.rings
                .first()
                .minOf { projection.toMeters(it).x }

        val shadow = calculator.calculateBuilding(building, 90.0, 45.0)
        val shadowWest = shadow.flatMap { it.rings.flatten() }.minOf { projection.toMeters(it).x }

        assertEquals(footprintWest - 12.0, shadowWest, 0.05)
    }

    @Test
    fun nearHorizonZenith_isClamped() {
        val shadow = calculator.calculateBuilding(squareBuilding(10.0), 180.0, 90.0)

        assertTrue(
            shadow
                .flatMap { it.rings.flatten() }
                .all { it.longitude.isFinite() && it.latitude.isFinite() }
        )
    }

    private fun squareBuilding(heightMeters: Double): Building {
        val ring =
            listOf(
                projection.toPoint(Coordinate(-5.0, -5.0)),
                projection.toPoint(Coordinate(5.0, -5.0)),
                projection.toPoint(Coordinate(5.0, 5.0)),
                projection.toPoint(Coordinate(-5.0, 5.0)),
                projection.toPoint(Coordinate(-5.0, -5.0))
            )
        return Building(
            id = "test-building",
            polygon = GeoPolygon(listOf(ring)),
            heightMeters = heightMeters,
            minHeightMeters = 0.0
        )
    }
}
