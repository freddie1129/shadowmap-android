package com.gooludou.shadowplanner.presentation.components

import com.gooludou.shadowplanner.domain.GeoPoint

/**
 * Snapshot of the Mapbox camera and the visible ground footprint used by the 3D scene.
 *
 * The meter dimensions describe the map boundary around [center]. They are kept separately
 * from the pixel dimensions because the sky dome is sized in world units but rendered into a
 * screen whose dimensions can change with device orientation or window size.
 */
data class MapboxScene3DViewport(
    /** Geographic center of the current Mapbox camera. */
    val center: GeoPoint,

    /** Mapbox camera zoom level. */
    val zoom: Double,

    /** Camera bearing in degrees clockwise from true north. */
    val bearing: Double,

    /** Width of the visible ground boundary in meters. */
    val widthMeters: Double,

    /** Height of the visible ground boundary in meters. */
    val heightMeters: Double,

    /** Width of the Mapbox view in physical pixels. */
    val widthPixels: Int,

    /** Height of the Mapbox view in physical pixels. */
    val heightPixels: Int
)
