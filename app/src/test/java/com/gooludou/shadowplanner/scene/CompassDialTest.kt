package com.gooludou.shadowplanner.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassDialTest {
    @Test
    fun marksUseFiveDegreeSpacing() {
        val marks = CompassDial.marks()

        assertEquals(72, marks.size)
        assertTrue(
            marks.zipWithNext().all { (first, second) ->
                second.azimuthDegrees - first.azimuthDegrees == 5
            }
        )
    }

    @Test
    fun cardinalDirectionsReplaceDegreeLabels() {
        assertEquals("N", CompassDial.cardinalLabel(0))
        assertEquals("E", CompassDial.cardinalLabel(90))
        assertEquals("S", CompassDial.cardinalLabel(180))
        assertEquals("W", CompassDial.cardinalLabel(270))
        assertNull(CompassDial.cardinalLabel(45))
    }

    @Test
    fun majorMarksAreLabelledEveryFifteenDegrees() {
        val marks = CompassDial.marks().associateBy(CompassMark::azimuthDegrees)

        assertEquals(CompassTickType.MAJOR, marks.getValue(15).type)
        assertEquals("15°", marks.getValue(15).label)
        assertEquals(CompassTickType.MEDIUM, marks.getValue(5).type)
        assertNull(marks.getValue(5).label)
    }
}
