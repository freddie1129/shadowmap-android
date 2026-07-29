package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `explicitly selecting Mapbox buildings starts on standard basemap`() {
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

    @Test
    fun `loaded projects use centralized Mapbox 3D defaults`() {
        val displayMode = initialMapDisplayMode(projectLoadRevision = 1L)

        assertEquals(
            Scene3DCamera.TOP_DOWN_PITCH_DEGREES,
            displayMode.cameraPitchDegrees,
            0.0
        )
        assertEquals(
            SceneBuildingSelection.DRAWN,
            displayMode.content
        )
        assertEquals(
            MapboxBasemapStyle.SATELLITE,
            displayMode.basemapStyle
        )
        assertFalse(displayMode.isDomeVisible)
        assertEquals(MapCameraMode.TOP_DOWN, displayMode.cameraMode)
    }

    @Test
    fun `editing and post-edit view use centralized top-down drawing defaults`() {
        val displayMode = MapDisplayDefaults.EDITING

        assertEquals(
            Scene3DCamera.TOP_DOWN_PITCH_DEGREES,
            displayMode.cameraPitchDegrees,
            0.0
        )
        assertEquals(
            SceneBuildingSelection.DRAWN,
            displayMode.content
        )
        assertEquals(
            MapboxBasemapStyle.SATELLITE,
            displayMode.basemapStyle
        )
        assertFalse(displayMode.isDomeVisible)
        assertEquals(MapCameraMode.TOP_DOWN, displayMode.cameraMode)
    }

    @Test
    fun `app launch display mode starts with standard Mapbox 3D and dome`() {
        val displayMode = initialMapDisplayMode(projectLoadRevision = 0L)

        assertEquals(MapboxBasemapStyle.STANDARD, displayMode.basemapStyle)
        assertEquals(SceneBuildingSelection.MAPBOX, displayMode.content)
        assertEquals(MapCameraMode.THREE_DIMENSIONAL, displayMode.cameraMode)
        assertEquals(true, displayMode.isDomeVisible)
    }

    @Test
    fun `editing forces satellite basemap`() {
        val displayMode = MapDisplayDefaults.APP_LAUNCH.forSceneMode(MapboxSceneMode.EDIT)

        assertEquals(MapboxBasemapStyle.SATELLITE, displayMode.basemapStyle)
    }

    @Test
    fun `view mode preserves selected basemap`() {
        val displayMode = MapDisplayDefaults.APP_LAUNCH.forSceneMode(MapboxSceneMode.VIEW)

        assertEquals(MapboxBasemapStyle.STANDARD, displayMode.basemapStyle)
    }
}
