@file:Suppress("MatchingDeclarationName")

package com.example.shadowmap.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Direction3(val x: Float, val y: Float, val z: Float)

/** Converts clockwise-from-north azimuth and down-from-vertical zenith to light-ray direction. */
fun sunLightDirection(azimuthDegrees: Float, zenithDegrees: Float): Direction3 {
    val azimuth = azimuthDegrees * PI / 180.0
    val zenith = zenithDegrees.coerceIn(0f, 89.9f) * PI / 180.0
    return Direction3(
        x = (-sin(zenith) * sin(azimuth)).toFloat(),
        y = (-cos(zenith)).toFloat(),
        z = (sin(zenith) * cos(azimuth)).toFloat()
    )
}
