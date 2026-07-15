package com.example.shadowmap.scene

import org.junit.Assert.assertEquals
import org.junit.Test

class Scene3DCameraTest {
    @Test
    fun perspectiveDistance_preservesOrthographicVerticalSpan() {
        val distance = Scene3DCamera.perspectiveDistanceForOrthographicHeight(
            viewportHeightMeters = 200f,
            orthographicZoom = 1f
        )

        assertEquals(241.42f, distance, 0.01f)
    }

    @Test
    fun perspectiveBounds_useViewportWhenSceneGeometryIsSparse() {
        assertEquals(
            5f,
            Scene3DCamera.minimumPerspectiveDistance(
                sceneRadius = 20f,
                viewportRadius = 300f
            ),
            0f
        )
        assertEquals(
            2_400f,
            Scene3DCamera.maximumPerspectiveDistance(
                sceneRadius = 20f,
                viewportRadius = 300f
            ),
            0f
        )
    }

    @Test
    fun perspectiveBounds_allowCloseViewWhenSceneContainsDistantGeometry() {
        assertEquals(
            75f,
            Scene3DCamera.minimumPerspectiveDistance(
                sceneRadius = 2_000f,
                viewportRadius = 300f
            ),
            0f
        )
        assertEquals(
            16_000f,
            Scene3DCamera.maximumPerspectiveDistance(
                sceneRadius = 2_000f,
                viewportRadius = 300f
            ),
            0f
        )
    }
}
