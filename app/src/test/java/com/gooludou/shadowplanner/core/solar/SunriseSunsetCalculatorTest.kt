package com.gooludou.shadowplanner.core.solar

import com.gooludou.shadowplanner.core.model.GeoPoint

import java.time.LocalDate
import java.time.ZoneId
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
}
