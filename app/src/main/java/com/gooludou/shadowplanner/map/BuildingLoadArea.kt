package com.gooludou.shadowplanner.map

import kotlin.math.roundToInt

data class BuildingLoadArea(val widthMeters: Float, val heightMeters: Float) {
    val isWithinLimit: Boolean
        get() = widthMeters <= MAX_BUILDING_LOAD_DIMENSION_METERS &&
            heightMeters <= MAX_BUILDING_LOAD_DIMENSION_METERS

    val formattedDimensions: String
        get() = "${widthMeters.roundToInt()} m × ${heightMeters.roundToInt()} m"
}

const val MAX_BUILDING_LOAD_DIMENSION_METERS = 500f
