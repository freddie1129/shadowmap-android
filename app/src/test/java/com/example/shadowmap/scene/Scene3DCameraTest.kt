package com.example.shadowmap.scene

import org.junit.Assert.assertEquals
import org.junit.Test

class Scene3DCameraTest {
    @Test
    fun orbitDistance_framesTheLargerOfSceneAndViewport() {
        val distance = Scene3DCamera.orbitDistance(sceneRadius = 20f, viewportRadius = 300f)

        assertEquals(540f, distance, 0f)
    }

    @Test
    fun orthographicPanScale_tracksVisibleMapSpan() {
        val metersPerPixel = Scene3DCamera.orthographicMetersPerPixel(
            viewportSpanMeters = 400f,
            orthographicZoom = 2f,
            viewportPixels = 800
        )

        assertEquals(1f, metersPerPixel, 0f)
    }
}
