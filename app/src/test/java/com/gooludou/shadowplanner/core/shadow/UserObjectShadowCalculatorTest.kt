package com.gooludou.shadowplanner.core.shadow

import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserObjectShadowCalculatorTest {
    private val calculator = UserObjectShadowCalculator()
    private val origin = GeoPoint(153.0, -28.0)

    @Test
    fun wallCreatesOneShadowPerSegment() {
        val wall = DrawnWall(
            points = listOf(
                origin,
                GeoPoint(153.0001, -28.0),
                GeoPoint(153.0001, -28.0001)
            ),
            heightMeters = 2.5
        )

        val shadows = calculator.calculate(
            walls = listOf(wall),
            trees = emptyList(),
            origin = origin,
            azimuthDegrees = 0.0,
            zenithDegrees = 45.0
        )

        assertEquals(2, shadows.size)
        assertTrue(shadows.all { it.rings.single().size == 5 })
    }

    @Test
    fun treeCreatesClosedProjectedCanopyShadow() {
        val shadows = calculator.calculate(
            walls = emptyList(),
            trees = listOf(DrawnTree(center = origin, heightMeters = 8.0, radiusMeters = 5.0)),
            origin = origin,
            azimuthDegrees = 90.0,
            zenithDegrees = 45.0
        )

        val ring = shadows.single().rings.single()
        assertTrue(ring.size >= 4)
        assertEquals(ring.first(), ring.last())
    }
}
