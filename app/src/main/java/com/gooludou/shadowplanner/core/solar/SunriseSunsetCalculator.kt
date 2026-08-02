package com.gooludou.shadowplanner.core.solar

import com.gooludou.shadowplanner.core.model.GeoPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SunriseSunset(val sunrise: Instant, val sunset: Instant)

/** Finds local sunrise and sunset by detecting horizon crossings for a calendar day. */
class SunriseSunsetCalculator(
    private val solarPositionCalculator: SolarPositionCalculator = SolarPositionCalculator()
) {
    fun calculate(date: LocalDate, zoneId: ZoneId, location: GeoPoint): SunriseSunset? {
        val dayStart = date.atStartOfDay(zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        var previous = dayStart
        var wasAboveHorizon = isAboveApparentHorizon(previous, location)
        var sunrise: Instant? = null
        var sunset: Instant? = null
        var current = previous.plusSeconds(SAMPLE_INTERVAL_SECONDS)

        while (current <= dayEnd) {
            val isAboveHorizon = isAboveApparentHorizon(current, location)
            if (!wasAboveHorizon && isAboveHorizon && sunrise == null) {
                sunrise = horizonCrossing(previous, current, location)
            } else if (wasAboveHorizon && !isAboveHorizon && sunset == null) {
                sunset = horizonCrossing(previous, current, location)
            }
            if (sunrise != null && sunset != null) return SunriseSunset(sunrise, sunset)
            previous = current
            wasAboveHorizon = isAboveHorizon
            current = current.plusSeconds(SAMPLE_INTERVAL_SECONDS)
        }
        return null
    }

    private fun horizonCrossing(start: Instant, end: Instant, location: GeoPoint): Instant {
        var lower = start
        var upper = end
        val lowerIsAboveHorizon = isAboveApparentHorizon(lower, location)
        while (upper.toEpochMilli() - lower.toEpochMilli() > CROSSING_PRECISION_MILLIS) {
            val midpoint = Instant.ofEpochMilli(
                (lower.toEpochMilli() + upper.toEpochMilli()) / 2
            )
            if (isAboveApparentHorizon(midpoint, location) == lowerIsAboveHorizon) {
                lower = midpoint
            } else {
                upper = midpoint
            }
        }
        return upper
    }

    private fun isAboveApparentHorizon(instant: Instant, location: GeoPoint): Boolean =
        solarPositionCalculator.calculate(
            epochMillis = instant.toEpochMilli(),
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude
        ).zenithDegrees < APPARENT_HORIZON_ZENITH_DEGREES

    private companion object {
        // Includes the Sun's apparent radius and atmospheric refraction at the horizon.
        const val APPARENT_HORIZON_ZENITH_DEGREES = 90.833
        const val SAMPLE_INTERVAL_SECONDS = 5 * 60L
        const val CROSSING_PRECISION_MILLIS = 1_000L
    }
}
