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
        var wasAboveHorizon = isAboveHorizon(previous, location)
        var sunrise: Instant? = null
        var sunset: Instant? = null
        var current = previous.plusSeconds(SAMPLE_INTERVAL_SECONDS)

        while (current <= dayEnd) {
            val isAboveHorizon = isAboveHorizon(current, location)
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
        val lowerIsAboveHorizon = isAboveHorizon(lower, location)
        while (upper.toEpochMilli() - lower.toEpochMilli() > MILLIS_PER_MINUTE) {
            val midpoint = Instant.ofEpochMilli(
                (lower.toEpochMilli() + upper.toEpochMilli()) / 2
            )
            if (isAboveHorizon(midpoint, location) == lowerIsAboveHorizon) {
                lower = midpoint
            } else {
                upper = midpoint
            }
        }
        return upper
    }

    private fun isAboveHorizon(instant: Instant, location: GeoPoint): Boolean =
        solarPositionCalculator.calculate(
            epochMillis = instant.toEpochMilli(),
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude
        ).isAboveHorizon

    private companion object {
        const val SAMPLE_INTERVAL_SECONDS = 5 * 60L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
