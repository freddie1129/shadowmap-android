package com.gooludou.shadowplanner

import com.gooludou.shadowplanner.renderer.mapbox.BuildingLoadType

/** App-wide feature configuration. */
object Config {
    const val ALLOW_LOAD_BUILDING = true

    /** Loads only the building under the crosshair; use ALL to load the visible viewport. */
    val BUILDING_LOAD_TYPE = BuildingLoadType.CENTRE_ONLY

    /** Neutral longitude used while permission is pending or location is unavailable. */
    const val FALLBACK_MAP_CENTER_LONGITUDE = 0.0

    /** Neutral latitude used while permission is pending or location is unavailable. */
    const val FALLBACK_MAP_CENTER_LATITUDE = 0.0

    /** Zoom shared by the initial camera and all location-based camera moves. */
    const val DEFAULT_MAP_ZOOM = 17.0

    /** Neutral camera bearing used for the world fallback. */
    const val FALLBACK_MAP_BEARING_DEGREES = 0.0

    /** Temporary fallback width until the Mapbox view reports its measured footprint. */
    const val MAPBOX_3D_FALLBACK_WIDTH_METERS = 1_000.0

    /** Temporary fallback height until the Mapbox view reports its measured footprint. */
    const val MAPBOX_3D_FALLBACK_HEIGHT_METERS = 1_000.0
}
