package com.example.shadowmap.domain

import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

data class SolarPosition(val azimuthDegrees: Double, val zenithDegrees: Double) {
    val isAboveHorizon: Boolean
        get() = zenithDegrees < HORIZON_ZENITH_DEGREES

    private companion object {
        const val HORIZON_ZENITH_DEGREES = 90.0
    }
}

/** Calculates the direction of the sun using NOAA's solar-position equations. */
class SolarPositionCalculator
@Inject
constructor() {
    fun calculate(
        epochMillis: Long,
        latitudeDegrees: Double,
        longitudeDegrees: Double
    ): SolarPosition {
        require(latitudeDegrees in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitudeDegrees in -180.0..180.0) {
            "Longitude must be between -180 and 180"
        }

        val julianDay = epochMillis / MILLIS_PER_DAY + UNIX_EPOCH_JULIAN_DAY
        val julianCentury = (julianDay - J2000_JULIAN_DAY) / DAYS_PER_JULIAN_CENTURY
        val geometricMeanLongitude = normalizeDegrees(
            280.46646 + julianCentury * (36_000.76983 + julianCentury * 0.0003032)
        )
        val geometricMeanAnomaly =
            357.52911 + julianCentury * (35_999.05029 - 0.0001537 * julianCentury)
        val orbitEccentricity =
            0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury)
        val equationOfCenter =
            sinDegrees(geometricMeanAnomaly) *
                (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) +
                sinDegrees(2.0 * geometricMeanAnomaly) *
                (0.019993 - 0.000101 * julianCentury) +
                sinDegrees(3.0 * geometricMeanAnomaly) * 0.000289
        val trueLongitude = geometricMeanLongitude + equationOfCenter
        val omega = 125.04 - 1934.136 * julianCentury
        val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sinDegrees(omega)
        val meanObliquity =
            23.0 +
                (
                    26.0 +
                        (
                            21.448 -
                                julianCentury *
                                (46.815 + julianCentury * (0.00059 - 0.001813 * julianCentury))
                            ) /
                        60.0
                    ) /
                60.0
        val correctedObliquity = meanObliquity + 0.00256 * cosDegrees(omega)
        val declination = asin(
            sinDegrees(correctedObliquity) * sinDegrees(apparentLongitude)
        )
        val equationOfTimeMinutes = equationOfTime(
            geometricMeanLongitude = geometricMeanLongitude,
            geometricMeanAnomaly = geometricMeanAnomaly,
            orbitEccentricity = orbitEccentricity,
            correctedObliquity = correctedObliquity
        )

        val utcMinutes = Math.floorMod(epochMillis, MILLIS_PER_DAY_LONG) / MILLIS_PER_MINUTE
        val trueSolarMinutes = normalizeMinutes(
            utcMinutes + equationOfTimeMinutes + 4.0 * longitudeDegrees
        )
        val hourAngle = Math.toRadians(trueSolarMinutes / 4.0 - 180.0)
        val latitude = Math.toRadians(latitudeDegrees)
        val cosZenith = (
            sin(latitude) * sin(declination) +
                cos(latitude) * cos(declination) * cos(hourAngle)
            ).coerceIn(-1.0, 1.0)
        val zenith = Math.toDegrees(acos(cosZenith))
        val azimuth = normalizeDegrees(
            Math.toDegrees(
                atan2(
                    sin(hourAngle),
                    cos(hourAngle) * sin(latitude) - tan(declination) * cos(latitude)
                )
            ) + 180.0
        )

        return SolarPosition(azimuthDegrees = azimuth, zenithDegrees = zenith)
    }

    private fun equationOfTime(
        geometricMeanLongitude: Double,
        geometricMeanAnomaly: Double,
        orbitEccentricity: Double,
        correctedObliquity: Double
    ): Double {
        val y = tan(Math.toRadians(correctedObliquity) / 2.0).let { it * it }
        val longitude = Math.toRadians(geometricMeanLongitude)
        val anomaly = Math.toRadians(geometricMeanAnomaly)
        val radians =
            y * sin(2.0 * longitude) -
                2.0 * orbitEccentricity * sin(anomaly) +
                4.0 * orbitEccentricity * y * sin(anomaly) * cos(2.0 * longitude) -
                0.5 * y * y * sin(4.0 * longitude) -
                1.25 * orbitEccentricity * orbitEccentricity * sin(2.0 * anomaly)
        return Math.toDegrees(radians) * 4.0
    }

    private fun sinDegrees(degrees: Double): Double = sin(degrees * PI / 180.0)

    private fun cosDegrees(degrees: Double): Double = cos(degrees * PI / 180.0)

    private fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    private fun normalizeMinutes(minutes: Double): Double = ((minutes % 1440.0) + 1440.0) % 1440.0

    private companion object {
        const val UNIX_EPOCH_JULIAN_DAY = 2_440_587.5
        const val J2000_JULIAN_DAY = 2_451_545.0
        const val DAYS_PER_JULIAN_CENTURY = 36_525.0
        const val MILLIS_PER_DAY = 86_400_000.0
        const val MILLIS_PER_DAY_LONG = 86_400_000L
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
