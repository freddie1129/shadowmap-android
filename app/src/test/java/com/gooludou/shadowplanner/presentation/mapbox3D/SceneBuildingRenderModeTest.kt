package com.gooludou.shadowplanner.presentation.mapbox3D

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneBuildingRenderModeTest {
    @Test
    fun `Mapbox buildings use Mapbox rendering regardless of pitch`() {
        assertEquals(
            SceneBuildingRenderMode.MAPBOX,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.MAPBOX,
                basemapStyle = MapboxBasemapStyle.STANDARD,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `satellite drawn buildings use 2D rendering in top-down view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_TOP_DOWN,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.DRAWN,
                basemapStyle = MapboxBasemapStyle.SATELLITE,
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
                basemapStyle = MapboxBasemapStyle.SATELLITE,
                cameraPitchDegrees = Scene3DCamera.ORBIT_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `standard drawn buildings remain extruded in top-down view`() {
        assertEquals(
            SceneBuildingRenderMode.DRAWN_3D,
            sceneBuildingRenderMode(
                buildingSelection = SceneBuildingSelection.DRAWN,
                basemapStyle = MapboxBasemapStyle.STANDARD,
                cameraPitchDegrees = Scene3DCamera.TOP_DOWN_PITCH_DEGREES
            )
        )
    }

    @Test
    fun `building menu contains only drawn and Mapbox options`() {
        assertEquals(2, SceneBuildingSelection.entries.size)
        assertEquals(SceneBuildingSelection.DRAWN, SceneBuildingSelection.fromMenuIndex(0))
        assertEquals(SceneBuildingSelection.MAPBOX, SceneBuildingSelection.fromMenuIndex(1))
    }

    @Test
    fun `only drawn buildings allow basemap selection`() {
        assertTrue(SceneBuildingSelection.DRAWN.allowsBasemapSelection)
        assertFalse(SceneBuildingSelection.MAPBOX.allowsBasemapSelection)
    }

    @Test
    fun `selecting Mapbox buildings switches to standard basemap`() {
        assertEquals(
            MapboxBasemapStyle.STANDARD,
            basemapStyleAfterBuildingSelection(
                buildingSelection = SceneBuildingSelection.MAPBOX,
                currentBasemapStyle = MapboxBasemapStyle.SATELLITE
            )
        )
    }

    @Test
    fun `selecting drawn buildings preserves the current basemap`() {
        assertEquals(
            MapboxBasemapStyle.SATELLITE,
            basemapStyleAfterBuildingSelection(
                buildingSelection = SceneBuildingSelection.DRAWN,
                currentBasemapStyle = MapboxBasemapStyle.SATELLITE
            )
        )
    }
}
