package com.gooludou.shadowplanner.presentation.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Mapbox resources associated with the static compass dome GLB. */
internal object SceneDomeGlb {
    const val SOURCE_ID = "scene-dome-source"
    const val LAYER_ID = "scene-dome-layer"
    const val MODEL_ID = "scene-dome-model"
    const val MODEL_URI = "asset://scene_dome.glb"
}

/** Mapbox resources and colors associated with the shared sun-segment GLB. */
internal object SceneSunSegmentGlb {
    const val SOURCE_ID = "scene-sun-segment-source"
    const val LAYER_ID = "scene-sun-segment-layer"
    const val MODEL_ID = "scene-sun-segment-model"
    const val MODEL_URI = "asset://scene_sun_segment.glb"
    const val PATH_COLOR_HEX = "#FFFFB547"
    const val CONNECTOR_COLOR_HEX = "#FFFFE082"
}

/** Mapbox resources and appearance associated with the dynamic sun marker GLB. */
internal object SceneSunMarkerGlb {
    const val SOURCE_ID = "scene-sun-marker-source"
    const val LAYER_ID = "scene-sun-marker-layer"
    const val MODEL_ID = "scene-sun-marker-model"
    const val MODEL_URI = "asset://scene_sun_sphere.glb"
    val COLOR = Color(0xFFFFD54F)
}

/** Property keys shared by the segment and marker model features. */
internal object SceneSkyModelProperties {
    const val TRANSLATION = "model_translation"
    const val SCALE = "model_scale"
    const val ROTATION = "model_rotation"
    const val COLOR = "model_color"
}

/** Shared screen-space sizing values for the sky models. */
internal object SceneSkyStyle {
    val DOME_EDGE_PADDING = 32.dp
    val SUN_PATH_WIDTH = 2.dp
    val SUN_CONNECTOR_WIDTH = 1.dp
}
