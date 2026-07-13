package com.example.shadowmap.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

data class SceneViewport(
    val centerLongitude: Double,
    val centerLatitude: Double,
    val widthMeters: Float,
    val heightMeters: Float,
    val screenRightX: Float,
    val screenRightZ: Float,
    val screenDownX: Float,
    val screenDownZ: Float
) {
    companion object {
        private const val EARTH_RADIUS_METERS = 6_378_137.0

        fun fromScreenCoordinates(
            centerLongitude: Double,
            centerLatitude: Double,
            topLeftLongitude: Double,
            topLeftLatitude: Double,
            topRightLongitude: Double,
            topRightLatitude: Double,
            bottomLeftLongitude: Double,
            bottomLeftLatitude: Double
        ): SceneViewport {
            val latitudeScale = EARTH_RADIUS_METERS * PI / 180.0
            val longitudeScale = latitudeScale * cos(centerLatitude * PI / 180.0)
            val rightX = ((topRightLongitude - topLeftLongitude) * longitudeScale).toFloat()
            val rightZ = (-(topRightLatitude - topLeftLatitude) * latitudeScale).toFloat()
            val downX = ((bottomLeftLongitude - topLeftLongitude) * longitudeScale).toFloat()
            val downZ = (-(bottomLeftLatitude - topLeftLatitude) * latitudeScale).toFloat()
            val width = sqrt(rightX * rightX + rightZ * rightZ).coerceAtLeast(1f)
            val height = sqrt(downX * downX + downZ * downZ).coerceAtLeast(1f)
            return SceneViewport(
                centerLongitude = centerLongitude,
                centerLatitude = centerLatitude,
                widthMeters = width,
                heightMeters = height,
                screenRightX = rightX / width,
                screenRightZ = rightZ / width,
                screenDownX = downX / height,
                screenDownZ = downZ / height
            )
        }
    }
}
