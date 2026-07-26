package com.gooludou.shadowplanner

/** App-wide feature configuration. */
object Config {
    const val ALLOW_LOAD_BUILDING = false

    /** Neutral longitude used while permission is pending or location is unavailable. */
    const val FALLBACK_MAP_CENTER_LONGITUDE = 0.0

    /** Neutral latitude used while permission is pending or location is unavailable. */
    const val FALLBACK_MAP_CENTER_LATITUDE = 0.0

    /** World-level zoom used until a useful local location is available. */
    const val FALLBACK_MAP_ZOOM = 1.5

    /** Local zoom used after the device provides a current location. */
    const val DEVICE_LOCATION_MAP_ZOOM = 17.0

    /** Neutral camera bearing used for the world fallback. */
    const val FALLBACK_MAP_BEARING_DEGREES = 0.0

    /** Temporary fallback width until the Mapbox view reports its measured footprint. */
    const val MAPBOX_3D_FALLBACK_WIDTH_METERS = 1_000.0

    /** Temporary fallback height until the Mapbox view reports its measured footprint. */
    const val MAPBOX_3D_FALLBACK_HEIGHT_METERS = 1_000.0
}
