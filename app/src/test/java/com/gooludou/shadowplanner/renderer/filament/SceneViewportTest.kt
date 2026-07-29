package com.gooludou.shadowplanner.renderer.filament

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneViewportTest {
    @Test
    fun screenCoordinatesProduceViewportDimensionsInMeters() {
        val viewport = SceneViewport.fromScreenCoordinates(
            centerLongitude = 153.0,
            centerLatitude = -28.0,
            topLeftLongitude = 152.999,
            topLeftLatitude = -27.999,
            topRightLongitude = 153.001,
            topRightLatitude = -27.999,
            bottomLeftLongitude = 152.999,
            bottomLeftLatitude = -28.001
        )

        assertEquals(196.6f, viewport.widthMeters, 1f)
        assertEquals(222.6f, viewport.heightMeters, 1f)
        assertTrue(viewport.widthMeters > 0f)
        assertTrue(viewport.heightMeters > 0f)
    }
}
