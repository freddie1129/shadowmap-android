package com.gooludou.shadowplanner.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarPositionCalculatorTest {
    private val calculator = SolarPositionCalculator()

    @Test
    fun nrelReferencePosition_matchesPublishedDirection() {
        val position = calculator.calculate(
            epochMillis = Instant.parse("2003-10-17T19:30:30Z").toEpochMilli(),
            latitudeDegrees = 39.742476,
            longitudeDegrees = -105.1786
        )

        assertEquals(194.34, position.azimuthDegrees, 0.5)
        assertEquals(50.11, position.zenithDegrees, 0.5)
        assertTrue(position.isAboveHorizon)
    }

    @Test
    fun brisbaneMorning_placesSunInEasternSky() {
        val position = calculator.calculate(
            epochMillis = Instant.parse("2026-07-13T22:00:00Z").toEpochMilli(),
            latitudeDegrees = -27.4698,
            longitudeDegrees = 153.0251
        )

        assertTrue(position.azimuthDegrees in 0.0..90.0)
        assertTrue(position.isAboveHorizon)
    }

    @Test
    fun brisbaneMidnight_isBelowHorizon() {
        val position = calculator.calculate(
            epochMillis = Instant.parse("2026-07-13T14:00:00Z").toEpochMilli(),
            latitudeDegrees = -27.4698,
            longitudeDegrees = 153.0251
        )

        assertTrue(position.zenithDegrees > 90.0)
        assertFalse(position.isAboveHorizon)
    }
}
