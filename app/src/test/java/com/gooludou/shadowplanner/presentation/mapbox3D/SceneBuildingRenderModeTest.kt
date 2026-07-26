package com.gooludou.shadowplanner.presentation.mapbox3D

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneBuildingRenderModeTest {
    @Test
    fun `Mapbox buildings use Mapbox rendering regardless of pitch`() {
        assertEquals(
            SceneBuildingRenderMode.MAPBOX,
            sceneBuildingRenderMode(
                useMapboxBuildings = true,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `drawn buildings use 2D rendering in top-down view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_TOP_DOWN,
            sceneBuildingRenderMode(
                useMapboxBuildings = false,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `drawn buildings use extrusion rendering in 3D view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_3D,
            sceneBuildingRenderMode(
                useMapboxBuildings = false,
                cameraPitchDegrees = Scene3DCamera.ORBIT_PITCH_DEGREES
            )
        )
    }
}
