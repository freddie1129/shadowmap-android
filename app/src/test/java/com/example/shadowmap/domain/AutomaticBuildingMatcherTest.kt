package com.example.shadowmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AutomaticBuildingMatcherTest {
    @Test
    fun geometryFingerprint_isStableAcrossRingStartAndDirection() {
        val points = listOf(
            GeoPoint(153.0, -27.0),
            GeoPoint(153.001, -27.0),
            GeoPoint(153.001, -27.001),
            GeoPoint(153.0, -27.001)
        )
        val first = GeoPolygon(listOf(points + points.first()))
        val rotated = points.drop(2) + points.take(2)
        val second = GeoPolygon(listOf(rotated.reversed() + rotated.last()))

        assertEquals(
            AutomaticBuildingMatcher.geometryFingerprint(first),
            AutomaticBuildingMatcher.geometryFingerprint(second)
        )
    }

    @Test
    fun identity_includesFeatureNamespace() {
        val polygon = square()
        val first = AutomaticBuildingMatcher.identity("123", "buildings-a", polygon)
        val second = AutomaticBuildingMatcher.identity("123", "buildings-b", polygon)

        assertNotEquals(first.selectionId, second.selectionId)
    }

    private fun square(): GeoPolygon {
        val points = listOf(
            GeoPoint(153.0, -27.0),
            GeoPoint(153.001, -27.0),
            GeoPoint(153.001, -27.001),
            GeoPoint(153.0, -27.001)
        )
        return GeoPolygon(listOf(points + points.first()))
    }
}
