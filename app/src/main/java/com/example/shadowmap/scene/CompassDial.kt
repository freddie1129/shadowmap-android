package com.example.shadowmap.scene

enum class CompassTickType {
    MEDIUM,
    MAJOR,
    CARDINAL
}

data class CompassMark(val azimuthDegrees: Int, val type: CompassTickType, val label: String?)

object CompassDial {
    const val TICK_INTERVAL_DEGREES = 5
    const val MAJOR_INTERVAL_DEGREES = 15

    fun marks(): List<CompassMark> = (0 until 360 step TICK_INTERVAL_DEGREES).map { azimuth ->
        val cardinal = cardinalLabel(azimuth)
        when {
            cardinal != null -> CompassMark(azimuth, CompassTickType.CARDINAL, cardinal)

            azimuth % MAJOR_INTERVAL_DEGREES == 0 ->
                CompassMark(azimuth, CompassTickType.MAJOR, "$azimuth°")

            else -> CompassMark(azimuth, CompassTickType.MEDIUM, null)
        }
    }

    fun cardinalLabel(azimuthDegrees: Int): String? = when (azimuthDegrees.mod(360)) {
        0 -> "N"
        90 -> "E"
        180 -> "S"
        270 -> "W"
        else -> null
    }
}
