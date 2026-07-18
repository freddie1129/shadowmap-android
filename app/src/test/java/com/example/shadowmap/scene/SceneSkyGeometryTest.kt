package com.example.shadowmap.scene

import com.example.shadowmap.domain.SolarPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SceneSkyGeometryTest {
    private val viewport = SceneViewport(
        centerLongitude = 153.0,
        centerLatitude = -27.0,
        widthMeters = 100f,
        heightMeters = 160f,
        screenRightX = 1f,
        screenRightZ = 0f,
        screenDownX = 0f,
        screenDownZ = 1f
    )

    @Test
    fun frameCoversEveryMapBoundaryCorner() {
        val frame = SceneSkyGeometry.calculateFrame(
            viewport = viewport,
            surfaceWidthPx = 1000,
            surfaceHeightPx = 2000,
            padding = SceneSkyPadding(leftPx = 100, topPx = 100, rightPx = 100, bottomPx = 200),
            visualMarginMeters = 0f
        )

        assertEquals(80f, frame.usableWidthMeters, 0.001f)
        assertEquals(128f, frame.usableHeightMeters, 0.001f)
        assertEquals(sqrt(8_900f), frame.radiusMeters, 0.001f)
        assertEquals(frame.radiusMeters, frame.outerRadiusMeters, 0.001f)
        assertEquals(2.358f, frame.overviewZoom, 0.001f)
    }

    @Test
    fun frameReservesCompassBandOutsideDome() {
        val frame = SceneSkyGeometry.calculateFrame(
            viewport = viewport,
            surfaceWidthPx = 1000,
            surfaceHeightPx = 2000,
            padding = SceneSkyPadding(leftPx = 100, topPx = 100, rightPx = 100, bottomPx = 200),
            compassBandPx = 100f,
            visualMarginMeters = 0f
        )

        assertEquals(sqrt(8_900f), frame.radiusMeters, 0.001f)
        assertEquals(125.786f, frame.outerRadiusMeters, 0.001f)
        assertEquals(3.145f, frame.overviewZoom, 0.001f)
    }

    @Test
    fun frameExpandsToCoverObjectsOutsideMapBoundary() {
        val frame = SceneSkyGeometry.calculateFrame(
            viewport = viewport,
            surfaceWidthPx = 1000,
            surfaceHeightPx = 2000,
            contentRadiusMeters = 120f,
            visualMarginMeters = 0f
        )

        assertEquals(120f, frame.radiusMeters, 0.001f)
    }

    @Test
    fun groundDiskUsesRequestedRadius() {
        val mesh = SceneSkyGeometry.groundDiskMesh(10f, viewport, segments = 12)

        assertEquals(14, mesh.vertices.size)
        assertEquals(36, mesh.indices.size)
        assertTrue(
            mesh.vertices.drop(1).all {
                kotlin.math.abs(sqrt(it.x * it.x + it.z * it.z) - 10f) < 0.001f
            }
        )
    }

    @Test
    fun northAndEastUseMapAlignedAxes() {
        val north = SceneSkyGeometry.pointOnDome(0f, 0f, 10f, viewport)
        val east = SceneSkyGeometry.pointOnDome(90f, 0f, 10f, viewport)

        assertEquals(0f, north.x, 0.001f)
        assertEquals(-10f, north.z, 0.001f)
        assertEquals(10f, east.x, 0.001f)
        assertEquals(0f, east.z, 0.001f)
    }

    @Test
    fun sunPathExcludesBelowHorizonPositions() {
        val mesh = SceneSkyGeometry.sunPathMesh(
            positions = listOf(
                SolarPosition(90.0, 100.0),
                SolarPosition(90.0, 45.0),
                SolarPosition(180.0, 80.0)
            ),
            radiusMeters = 10f,
            viewport = viewport
        )

        assertEquals(2, mesh.vertices.size)
        assertEquals(2, mesh.indices.size)
        assertTrue(mesh.vertices.all { it.y >= 0f })
    }

    @Test
    fun domeMeridiansRemainOnSphereSurface() {
        val radius = 10f
        val mesh = SceneSkyGeometry.domeMesh(radius, viewport)

        assertTrue(
            mesh.vertices.all { vertex ->
                val distance = sqrt(
                    vertex.x * vertex.x + vertex.y * vertex.y + vertex.z * vertex.z
                )
                kotlin.math.abs(distance - radius) < 0.001f
            }
        )
    }

    @Test
    fun domeUsesDenseRingsWithoutIncreasingMeridianCount() {
        val altitudeRings = SceneSkyGeometry.DEFAULT_ALTITUDE_RINGS
        val meridianSteps = (altitudeRings + 1) * 3
        val mesh = SceneSkyGeometry.domeMesh(10f, viewport)
        val ringVertexCount = (altitudeRings + 1) *
            (SceneSkyGeometry.DEFAULT_RING_SEGMENTS + 1)
        val meridianVertexCount = SceneSkyGeometry.DEFAULT_MERIDIAN_COUNT *
            (meridianSteps + 1)

        assertEquals(ringVertexCount + meridianVertexCount, mesh.vertices.size)
        assertEquals(24, SceneSkyGeometry.DEFAULT_MERIDIAN_COUNT)
        assertEquals(180, SceneSkyGeometry.DEFAULT_RING_SEGMENTS)
    }

    @Test
    fun sunBodyVerticesRemainOnMarkerSphere() {
        val center = ScenePoint3(4f, 7f, -3f)
        val radius = 2f
        val mesh = SceneSkyGeometry.sunSphereMesh(center, radius)

        assertTrue(mesh.indices.isNotEmpty())
        assertTrue(
            mesh.vertices.all { vertex ->
                val dx = vertex.x - center.x
                val dy = vertex.y - center.y
                val dz = vertex.z - center.z
                kotlin.math.abs(sqrt(dx * dx + dy * dy + dz * dz) - radius) < 0.001f
            }
        )
    }

    @Test
    fun connectorTubeUsesRequestedDiameter() {
        val mesh = SceneSkyGeometry.connectorTubeMesh(
            start = ScenePoint3(0f, 0f, 0f),
            end = ScenePoint3(0f, 10f, 0f),
            diameterMeters = 2f
        )

        assertEquals(16, mesh.vertices.size)
        assertEquals(48, mesh.indices.size)
        assertTrue(
            mesh.vertices.all {
                kotlin.math.abs(sqrt(it.x * it.x + it.z * it.z) - 1f) < 0.001f
            }
        )
    }

    @Test
    fun sunPathRibbonHasRequestedWidthAndSitsAboveDome() {
        val radius = 10f
        val width = 0.8f
        val offset = 0.1f
        val mesh = SceneSkyGeometry.sunPathRibbonMesh(
            positions = listOf(
                SolarPosition(90.0, 70.0),
                SolarPosition(120.0, 60.0),
                SolarPosition(150.0, 70.0)
            ),
            radiusMeters = radius,
            viewport = viewport,
            widthMeters = width,
            surfaceOffsetMeters = offset
        )

        assertEquals(6, mesh.vertices.size)
        assertEquals(12, mesh.indices.size)
        val firstWidth = mesh.vertices[0].distanceTo(mesh.vertices[1])
        assertEquals(width, firstWidth, 0.001f)
        assertTrue(mesh.vertices.all { it.length() > radius })
    }

    private fun ScenePoint3.distanceTo(other: ScenePoint3): Float = sqrt(
        (x - other.x) * (x - other.x) +
            (y - other.y) * (y - other.y) +
            (z - other.z) * (z - other.z)
    )

    private fun ScenePoint3.length(): Float = sqrt(x * x + y * y + z * z)
}
