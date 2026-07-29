package com.gooludou.shadowplanner.core.geometry

import com.gooludou.shadowplanner.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DrawingGeometryValidatorTest {
    @Test
    fun validBuilding_isAcceptedAndClosedAtCommit() {
        val vertices = listOf(
            GeoPoint(153.0, -28.0),
            GeoPoint(153.0001, -28.0),
            GeoPoint(153.0001, -28.0001),
            GeoPoint(153.0, -28.0001)
        )

        assertNull(DrawingGeometryValidator.validateBuilding(vertices))
        val ring = DrawingGeometryValidator.closedPolygon(vertices).rings.single()
        assertEquals(ring.first(), ring.last())
    }

    @Test
    fun selfIntersectingBuilding_isRejected() {
        val vertices = listOf(
            GeoPoint(153.0, -28.0),
            GeoPoint(153.0001, -28.0001),
            GeoPoint(153.0001, -28.0),
            GeoPoint(153.0, -28.0001)
        )

        assertNotNull(DrawingGeometryValidator.validateBuilding(vertices))
    }

    @Test
    fun wallNeedsTwoDistinctPoints() {
        val point = GeoPoint(153.0, -28.0)

        assertNotNull(DrawingGeometryValidator.validateWall(listOf(point, point)))
        assertNull(
            DrawingGeometryValidator.validateWall(
                listOf(point, GeoPoint(153.0001, -28.0))
            )
        )
    }
}
