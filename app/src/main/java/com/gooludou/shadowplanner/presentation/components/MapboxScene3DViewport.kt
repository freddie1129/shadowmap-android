package com.gooludou.shadowplanner.presentation.components

import com.gooludou.shadowplanner.domain.GeoPoint

data class MapboxScene3DViewport(
    val center: GeoPoint,
    val zoom: Double,
    val bearing: Double,
    val widthMeters: Double,
    val heightMeters: Double,
    val widthPixels: Int,
    val heightPixels: Int
)
