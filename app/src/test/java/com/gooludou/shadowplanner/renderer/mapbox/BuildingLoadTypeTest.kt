package com.gooludou.shadowplanner.renderer.mapbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildingLoadTypeTest {
    @Test
    fun allUsesEntireViewport() {
        assertNull(BuildingLoadType.ALL.toQueryGeometry(1080, 1920))
    }

    @Test
    fun centreOnlyUsesScreenCentre() {
        val centre = BuildingLoadType.CENTRE_ONLY
            .toQueryGeometry(1080, 1920)
            ?.screenCoordinate

        assertEquals(540.0, centre?.x ?: Double.NaN, 0.0)
        assertEquals(960.0, centre?.y ?: Double.NaN, 0.0)
    }
}
