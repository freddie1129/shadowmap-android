package com.gooludou.shadowplanner.core.solar

import com.gooludou.shadowplanner.core.model.GeoPoint
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertTrue
import org.junit.Test

class SunriseSunsetCalculatorTest {
    @Test
    fun brisbaneDay_returnsSunriseBeforeSunsetWithinTheSelectedDate() {
        val zoneId = ZoneId.of("Australia/Brisbane")
        val date = LocalDate.of(2026, 7, 24)

        val result = SunriseSunsetCalculator().calculate(
            date = date,
            zoneId = zoneId,
            location = GeoPoint(longitude = 153.0251, latitude = -27.4698)
        )

        requireNotNull(result)
        assertTrue(result.sunrise < result.sunset)
        assertTrue(result.sunrise.atZone(zoneId).toLocalDate() == date)
        assertTrue(result.sunset.atZone(zoneId).toLocalDate() == date)
    }

    @Test
    fun goldCoastWinterDay_matchesPublishedSunriseAndSunset() {
        val zoneId = ZoneId.of("Australia/Brisbane")
        val date = LocalDate.of(2026, 7, 30)

        val result = SunriseSunsetCalculator().calculate(
            date = date,
            zoneId = zoneId,
            location = GeoPoint(longitude = 153.4, latitude = -28.0167)
        )

        requireNotNull(result)
        assertWithinTwoMinutes(
            actual = result.sunrise.atZone(zoneId),
            expected = date.atTime(LocalTime.of(6, 29)).atZone(zoneId)
        )
        assertWithinTwoMinutes(
            actual = result.sunset.atZone(zoneId),
            expected = date.atTime(LocalTime.of(17, 15)).atZone(zoneId)
        )
    }

    private fun assertWithinTwoMinutes(actual: ZonedDateTime, expected: ZonedDateTime) {
        assertTrue(
            "Expected $actual to be within two minutes of $expected",
            Duration.between(expected, actual).abs() <= Duration.ofMinutes(2)
        )
    }
}
