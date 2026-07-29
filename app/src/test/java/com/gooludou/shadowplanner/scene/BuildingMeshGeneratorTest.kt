package com.gooludou.shadowplanner.scene

import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildingMeshGeneratorTest {
    @Test
    fun squareFootprintCreatesRoofWallsAndGround() {
        val building = Building(
            id = "one",
            polygon = GeoPolygon(
                listOf(
                    listOf(
                        GeoPoint(153.0, -28.0),
                        GeoPoint(153.0001, -28.0),
                        GeoPoint(153.0001, -28.0001),
                        GeoPoint(153.0, -28.0001),
                        GeoPoint(153.0, -28.0)
                    )
                )
            ),
            heightMeters = 12.0,
            minHeightMeters = 2.0
        )

        val mesh = BuildingMeshGenerator.generate(listOf(building))

        assertEquals(24, mesh.vertices.size)
        assertEquals(36, mesh.indices.size)
        assertTrue(mesh.vertices.any { it.y == 12f })
        assertTrue(mesh.vertices.any { it.y == 2f })
        assertTrue(mesh.radiusMeters > 1f)
        assertTrue(mesh.coverageRadiusMeters > mesh.radiusMeters)
        assertTrue(mesh.coverageRadiusMeters >= 12f)
        assertTrue(triangleNormalY(mesh, 0) > 0f)
        assertTrue(wallNormalDotProduct(mesh) > 0f)
    }

    @Test
    fun emptyInputProducesNoGpuGeometry() {
        val mesh = BuildingMeshGenerator.generate(emptyList())

        assertTrue(mesh.vertices.isEmpty())
        assertTrue(mesh.indices.isEmpty())
    }

    @Test
    fun wallsAndTreesAddRenderableGeometry() {
        val origin = GeoPoint(153.0, -28.0)
        val wall = DrawnWall(
            points = listOf(origin, GeoPoint(153.0001, -28.0)),
            heightMeters = 2.5
        )
        val tree = DrawnTree(
            center = GeoPoint(153.00005, -28.00005),
            heightMeters = 8.0,
            radiusMeters = 5.0
        )

        val mesh = BuildingMeshGenerator.generate(emptyList(), listOf(wall), listOf(tree))

        assertTrue(mesh.vertices.isNotEmpty())
        assertTrue(mesh.indices.isNotEmpty())
        assertTrue(mesh.vertices.any { it.y == 8f })
        assertTrue(mesh.vertices.any { it.y == 2.5f })
    }

    private fun triangleNormalY(mesh: BuildingMesh, indexOffset: Int): Float {
        val a = mesh.vertices[mesh.indices[indexOffset]]
        val b = mesh.vertices[mesh.indices[indexOffset + 1]]
        val c = mesh.vertices[mesh.indices[indexOffset + 2]]
        return (b.z - a.z) * (c.x - a.x) - (b.x - a.x) * (c.z - a.z)
    }

    private fun wallNormalDotProduct(mesh: BuildingMesh): Float {
        val a = mesh.vertices[mesh.indices[mesh.wallIndexOffset]]
        val b = mesh.vertices[mesh.indices[mesh.wallIndexOffset + 1]]
        val c = mesh.vertices[mesh.indices[mesh.wallIndexOffset + 2]]
        val edgeOneX = b.x - a.x
        val edgeOneY = b.y - a.y
        val edgeOneZ = b.z - a.z
        val edgeTwoX = c.x - a.x
        val edgeTwoY = c.y - a.y
        val edgeTwoZ = c.z - a.z
        val normalX = edgeOneY * edgeTwoZ - edgeOneZ * edgeTwoY
        val normalY = edgeOneZ * edgeTwoX - edgeOneX * edgeTwoZ
        val normalZ = edgeOneX * edgeTwoY - edgeOneY * edgeTwoX
        return normalX * a.normalX + normalY * a.normalY + normalZ * a.normalZ
    }
}
