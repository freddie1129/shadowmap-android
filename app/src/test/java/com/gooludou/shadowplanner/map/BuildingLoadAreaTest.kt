package com.gooludou.shadowplanner.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingLoadAreaTest {
    @Test
    fun areaAtMaximumDimensionsCanLoadBuildings() {
        val area = BuildingLoadArea(widthMeters = 500f, heightMeters = 500f)

        assertTrue(area.isWithinLimit)
    }

    @Test
    fun areaOverMaximumWidthCannotLoadBuildings() {
        val area = BuildingLoadArea(widthMeters = 501f, heightMeters = 400f)

        assertFalse(area.isWithinLimit)
    }

    @Test
    fun areaOverMaximumHeightCannotLoadBuildings() {
        val area = BuildingLoadArea(widthMeters = 400f, heightMeters = 501f)

        assertFalse(area.isWithinLimit)
    }
}
