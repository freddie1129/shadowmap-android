package com.example.shadowmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneBuildingMergerTest {
    @Test
    fun automaticBuildingCoveredByManual_isSuppressed() {
        val polygon = square(153.0, -28.0)
        val automatic = Building("automatic", polygon, 10.0, 0.0)
        val manual = Building("manual", polygon, 6.0, source = BuildingSource.MANUAL)

        val suppressed = SceneBuildingMerger.automaticKeysCoveredByManualBuildings(
            automaticBuildings = listOf(automatic),
            manualBuildings = listOf(manual)
        )

        assertEquals(setOf(SceneBuildingMerger.automaticKey(automatic)), suppressed)
    }

    @Test
    fun separateManualBuilding_keepsAutomaticBuildingVisible() {
        val automatic = Building("automatic", square(153.0, -28.0), 10.0, 0.0)
        val manual = Building("manual", square(154.0, -27.0), 6.0, source = BuildingSource.MANUAL)

        val suppressed = SceneBuildingMerger.automaticKeysCoveredByManualBuildings(
            automaticBuildings = listOf(automatic),
            manualBuildings = listOf(manual)
        )

        assertTrue(suppressed.isEmpty())
    }

    private fun square(longitude: Double, latitude: Double): GeoPolygon {
        val ring = listOf(
            GeoPoint(longitude, latitude),
            GeoPoint(longitude + 0.0001, latitude),
            GeoPoint(longitude + 0.0001, latitude - 0.0001),
            GeoPoint(longitude, latitude - 0.0001),
            GeoPoint(longitude, latitude)
        )
        return GeoPolygon(listOf(ring))
    }
}
