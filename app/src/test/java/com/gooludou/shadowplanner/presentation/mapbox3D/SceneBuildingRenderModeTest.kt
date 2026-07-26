package com.gooludou.shadowplanner.presentation.mapbox3D

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SceneBuildingRenderModeTest {
    @Test
    fun `Mapbox buildings use Mapbox rendering regardless of pitch`() {
        assertEquals(
            SceneBuildingRenderMode.MAPBOX,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.MAPBOX,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `drawn buildings use 2D rendering in top-down view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_TOP_DOWN,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.DRAWN,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `drawn buildings use extrusion rendering in 3D view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_3D,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.DRAWN,
                cameraPitchDegrees = Scene3DCamera.ORBIT_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `forced 3D drawn buildings use extrusion rendering in top-down view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_3D,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.DRAWN_FORCE_3D,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `forced 3D menu option uses satellite without changing pitch`() {
        val selection = SceneBuildingSelection.fromMenuIndex(1)

        assertEquals(SceneBuildingSelection.DRAWN_FORCE_3D, selection)
        assertEquals(MapboxBasemapStyle.SATELLITE, selection.basemapStyle)
        assertNull(selection.pitchOnSelection)
    }
}
