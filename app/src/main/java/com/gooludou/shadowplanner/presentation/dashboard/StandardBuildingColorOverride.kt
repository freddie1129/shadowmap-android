package com.gooludou.shadowplanner.presentation.dashboard

import com.mapbox.bindgen.Value
import com.mapbox.maps.MapView

/** Holds Standard's resolved building colour while its native footprints are transparent. */
internal class StandardBuildingColorOverride {
    var originalColor: Value? = null
}

/** Hides Standard's 2D footprints for drawings and restores its exact themed colour afterwards. */
internal fun updateStandardBuildingFootprints(
    mapView: MapView,
    basemapStyle: MapboxBasemapStyle,
    buildingRenderMode: SceneBuildingRenderMode,
    colorOverride: StandardBuildingColorOverride
) {
    if (basemapStyle != MapboxBasemapStyle.STANDARD) {
        colorOverride.originalColor = null
        return
    }
    if (buildingRenderMode == SceneBuildingRenderMode.MAPBOX) {
        colorOverride.originalColor?.let { originalColor ->
            mapView.mapboxMap.setStyleImportConfigProperty(
                Scene3DMapIds.STANDARD_STYLE_IMPORT,
                Scene3DMapConfig.COLOR_BUILDINGS,
                originalColor
            )
        }
        colorOverride.originalColor = null
        return
    }
    if (colorOverride.originalColor == null) {
        colorOverride.originalColor = mapView.mapboxMap.getStyleImportConfigProperty(
            Scene3DMapIds.STANDARD_STYLE_IMPORT,
            Scene3DMapConfig.COLOR_BUILDINGS
        ).value?.value
    }
    mapView.mapboxMap.setStyleImportConfigProperty(
        Scene3DMapIds.STANDARD_STYLE_IMPORT,
        Scene3DMapConfig.COLOR_BUILDINGS,
        Value(Scene3DMapConfig.TRANSPARENT_COLOR)
    )
}
