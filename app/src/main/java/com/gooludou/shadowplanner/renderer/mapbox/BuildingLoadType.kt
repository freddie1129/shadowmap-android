package com.gooludou.shadowplanner.renderer.mapbox

import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.ScreenCoordinate

/** Controls which rendered Mapbox building footprints are requested. */
enum class BuildingLoadType {
    ALL,
    CENTRE_ONLY
}

/** Converts the configured load scope into Mapbox screen-space query geometry. */
internal fun BuildingLoadType.toQueryGeometry(
    widthPixels: Int,
    heightPixels: Int
): RenderedQueryGeometry? = when (this) {
    BuildingLoadType.ALL -> null

    BuildingLoadType.CENTRE_ONLY -> RenderedQueryGeometry(
        ScreenCoordinate(
            widthPixels / 2.0,
            heightPixels / 2.0
        )
    )
}
