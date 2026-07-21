package com.gooludou.shadowplanner.presentation.components

import com.gooludou.shadowplanner.domain.GeoPoint

data class MapboxScene3DViewport(
    val center: GeoPoint,
    val zoom: Double,
    val bearing: Double
)
