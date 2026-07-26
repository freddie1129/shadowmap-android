package com.gooludou.shadowplanner.presentation.mapbox3D

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** IDs used by the Standard style import and the scene's Mapbox light objects. */
internal object Scene3DMapIds {
    /** Standard style import containing the basemap configuration. */
    const val STANDARD_STYLE_IMPORT = "basemap"

    /** ID of the directional light controlled by the current sun position. */
    const val SUN_LIGHT = "shadow-planner-sun"

    /** ID of the ambient light shared by the 3D scene. */
    const val AMBIENT_LIGHT = "shadow-planner-ambient"
}

/** Source and layer IDs for the user-drawn 3D objects. */
internal object Scene3DDrawingLayers {
    /** GeoJSON source containing drawn building footprints. */
    const val BUILDING_SOURCE = "custom-3d-buildings-source"

    /** Extrusion layer rendering drawn buildings. */
    const val BUILDING_LAYER = "custom-3d-buildings-layer"

    /** GeoJSON source containing drawn wall lines. */
    const val WALL_SOURCE = "custom-3d-walls-source"

    /** Extrusion layer rendering drawn walls. */
    const val WALL_LAYER = "custom-3d-walls-layer"

    /** GeoJSON source containing drawn tree trunks. */
    const val TREE_TRUNK_SOURCE = "custom-3d-tree-trunks-source"

    /** Extrusion layer rendering drawn tree trunks. */
    const val TREE_TRUNK_LAYER = "custom-3d-tree-trunks-layer"

    /** GeoJSON source containing drawn tree canopies. */
    const val TREE_CANOPY_SOURCE = "custom-3d-tree-canopies-source"

    /** Extrusion layer rendering drawn tree canopies. */
    const val TREE_CANOPY_LAYER = "custom-3d-tree-canopies-layer"
}

/** Property names shared by the GeoJSON features used by drawn objects. */
internal object Scene3DFeatureProperties {
    /** Absolute top height of an extruded object. */
    const val HEIGHT = "height"

    /** Base height used by buildings and tree canopies. */
    const val BASE_HEIGHT = "base_height"
}

/** Camera values shared by the 3D map and its camera controls. */
internal object Scene3DCamera {
    /** Default pitched angle used for the orbit-style 3D view. */
    const val ORBIT_PITCH_DEGREES = 60.0

    /** Camera pitch used for the top-down map view. */
    const val TOP_DOWN_PITCH_DEGREES = 0.0

    /** Maximum pitch treated as top-down for control-label purposes. */
    const val TOP_DOWN_THRESHOLD_DEGREES = 1.0
}

/** Rendering selected from the building source and current camera pitch. */
internal enum class SceneBuildingRenderMode {
    /** Mapbox Standard buildings supplied by the basemap. */
    MAPBOX,

    /** App buildings rendered with the same outlines and shadows as the 2D map. */
    DRAWN_TOP_DOWN,

    /** App buildings rendered as the existing height-aware 3D extrusions. */
    DRAWN_3D
}

/** Resolves the building renderer without storing a second camera-mode state. */
internal fun sceneBuildingRenderMode(
    useMapboxBuildings: Boolean,
    cameraPitchDegrees: Double
): SceneBuildingRenderMode = when {
    useMapboxBuildings -> SceneBuildingRenderMode.MAPBOX
    cameraPitchDegrees <= Scene3DCamera.TOP_DOWN_THRESHOLD_DEGREES -> {
        SceneBuildingRenderMode.DRAWN_TOP_DOWN
    }
    else -> SceneBuildingRenderMode.DRAWN_3D
}

/** Screen offsets used to keep the 3D controls clear of one another and system UI. */
internal object Scene3DControlLayout {
    /** Bottom clearance reserved for the date/time control. */
    val BOTTOM_CONTROL_CLEARANCE = 156.dp

    /** Top offset for the sky overview control. */
    val DOME_CONTROL_OFFSET = 52.dp

    /** Top offset for the building-source control below the dome control. */
    val BUILDING_CONTROL_OFFSET = 104.dp
}

/** Lighting defaults and limits applied to the Mapbox 3D scene. */
internal object Scene3DLighting {
    /** Fallback sun azimuth used before a solar position is available. */
    const val DEFAULT_AZIMUTH_DEGREES = 210.0

    /** Fallback sun polar angle used before a solar position is available. */
    const val DEFAULT_POLAR_ANGLE_DEGREES = 30.0

    /** Lowest accepted polar angle for Mapbox's directional light. */
    const val MIN_POLAR_ANGLE_DEGREES = 0.0

    /** Highest accepted polar angle for Mapbox's directional light. */
    const val MAX_POLAR_ANGLE_DEGREES = 90.0

    /** Directional-light intensity when the sun is above the horizon. */
    const val SUN_INTENSITY = 0.8

    /** Ambient fill-light intensity for the scene. */
    const val AMBIENT_INTENSITY = 0.35

    /** Shadow strength applied to the directional light. */
    const val SHADOW_INTENSITY = 0.85
}

/** Shared material and extrusion values for user-drawn scene objects. */
internal object Scene3DObjectStyle {
    /** Width in meters used when rendering drawn wall lines. */
    const val WALL_WIDTH_METERS = 0.2

    /** Opacity applied to drawn extrusions. */
    const val OBJECT_OPACITY = 1.0

    /** Ambient-occlusion strength applied to drawn buildings. */
    const val AMBIENT_OCCLUSION = 0.3
}

/** Geometry ratios and minimum dimensions used to render drawn trees. */
internal object Scene3DTreeGeometry {
    /** Fraction of tree height occupied by the trunk. */
    const val TRUNK_HEIGHT_RATIO = 0.45

    /** Fraction of tree height used as the canopy base. */
    const val CANOPY_BASE_RATIO = 0.35

    /** Ratio converting canopy radius into trunk radius. */
    const val TRUNK_RADIUS_RATIO = 0.18

    /** Rounded-roof edge radius for tree canopies. */
    const val EDGE_RADIUS_METERS = 0.5

    /** Minimum height allowed for a drawn tree. */
    const val MIN_TREE_HEIGHT_METERS = 0.1

    /** Minimum radius allowed for a drawn tree trunk. */
    const val MIN_TRUNK_RADIUS_METERS = 0.15

    /** Minimum radius allowed for a drawn tree canopy. */
    const val MIN_CANOPY_RADIUS_METERS = 0.5

    /** Number of points used to approximate circular tree footprints. */
    const val SEGMENTS = 12
}

/** Geographic constants used when converting tree radii into longitude/latitude offsets. */
internal object Scene3DGeography {
    /** Mean Earth radius used by the local spherical conversion. */
    const val EARTH_RADIUS_METERS = 6_378_137.0
}
