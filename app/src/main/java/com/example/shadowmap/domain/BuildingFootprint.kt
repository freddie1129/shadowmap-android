package com.example.shadowmap.domain

data class GeoPoint(val longitude: Double, val latitude: Double)

data class GeoPolygon(val rings: List<List<GeoPoint>>)

data class BuildingFootprint(
    val id: String?,
    val polygon: GeoPolygon,
    val heightMeters: Double,
    val minHeightMeters: Double
)

const val DEFAULT_BUILDING_HEIGHT_METERS = 10.0
