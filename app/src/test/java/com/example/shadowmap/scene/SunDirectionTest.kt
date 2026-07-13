package com.example.shadowmap.scene

import org.junit.Assert.assertEquals
import org.junit.Test

class SunDirectionTest {
    @Test
    fun overheadSunPointsLightRaysDown() {
        val direction = sunLightDirection(123f, 0f)

        assertEquals(0f, direction.x, 0.0001f)
        assertEquals(-1f, direction.y, 0.0001f)
        assertEquals(0f, direction.z, 0.0001f)
    }

    @Test
    fun easternLowSunPointsRaysWestAndDown() {
        val direction = sunLightDirection(90f, 60f)

        assertEquals(-0.866f, direction.x, 0.001f)
        assertEquals(-0.5f, direction.y, 0.001f)
        assertEquals(0f, direction.z, 0.001f)
    }
}
