package com.gooludou.shadowplanner.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Direction3(val x: Float, val y: Float, val z: Float)

/** Converts clockwise-from-north azimuth and down-from-vertical zenith to light-ray direction. */
fun sunLightDirection(azimuthDegrees: Float, zenithDegrees: Float): Direction3 {
    val azimuth = azimuthDegrees * PI / 180.0
    val zenith = zenithDegrees.coerceIn(
        Scene3DGeometry.MIN_LIGHT_ZENITH_DEGREES,
        Scene3DGeometry.MAX_LIGHT_ZENITH_DEGREES
    ) * PI / 180.0
    return Direction3(
        x = (-sin(zenith) * sin(azimuth)).toFloat(),
        y = (-cos(zenith)).toFloat(),
        z = (sin(zenith) * cos(azimuth)).toFloat()
    )
}
