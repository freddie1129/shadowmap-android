package com.example.shadowmap.scene

import kotlin.math.max

data class SceneRgb(val red: Float, val green: Float, val blue: Float)

data class SceneRgba(val red: Float, val green: Float, val blue: Float, val alpha: Float)

internal fun SceneRgba.toShaderFloat4(): String = "float4($red, $green, $blue, $alpha)"

object Scene3DAppearance {
    /** Opaque base color used by the lit material on building roofs. */
    val ROOF_COLOR = SceneRgba(0.82f, 0.84f, 0.83f, 1f)

    /** Roof microsurface roughness, where 0 is glossy and 1 is fully rough. */
    const val ROOF_ROUGHNESS = 0.72f

    /** Opaque base color used by the lit material on building walls. */
    val WALL_COLOR = SceneRgba(0.66f, 0.70f, 0.73f, 1f)

    /** Wall microsurface roughness, where 0 is glossy and 1 is fully rough. */
    const val WALL_ROUGHNESS = 0.82f

    /** Unpremultiplied ground RGB color; opacity is applied when creating the shader color. */
    val GROUND_COLOR = SceneRgb(0.82f, 0.84f, 0.85f)

    /** Translucent linear-space amber used for the dome meridians, altitude rings, and compass. */
    val SKY_GUIDE_COLOR = SceneRgba(0.96f, 0.70f, 0.18f, 0.28f)

    /** High-contrast linear-space orange used for the selected day's sun-path ribbon. */
    val SUN_PATH_COLOR = SceneRgba(1f, 0.42f, 0.08f, 0.9f)

    /** Saturated linear-space orange used by the current-sun centre guide. */
    val CURRENT_SUN_COLOR = SceneRgba(1f, 0.30f, 0.002f, 0.82f)

    /** Saturated linear-space yellow used by the current-sun body. */
    val SUN_BODY_COLOR = SceneRgba(1f, 0.50f, 0.002f, 1f)

    /** Ground alpha, where 0 is transparent and 1 is opaque. */
    const val GROUND_OPACITY = 0.30f

    /** Ground microsurface roughness, where 0 is glossy and 1 is fully rough. */
    const val GROUND_ROUGHNESS = 1f

    /** Warm RGB tint of the primary directional sunlight. */
    val SUN_COLOR = SceneRgb(1f, 0.97f, 0.91f)

    /** Primary sunlight illuminance in lux. */
    const val SUN_INTENSITY = 85_000f

    /** Apparent angular radius of the sun in degrees, controlling shadow softness. */
    const val SUN_ANGULAR_RADIUS = 1f

    /** Initial clockwise-from-north sun azimuth used before UI state is applied. */
    const val DEFAULT_SUN_AZIMUTH_DEGREES = 135f

    /** Initial angle down from vertical used before UI state is applied. */
    const val DEFAULT_SUN_ZENITH_DEGREES = 45f

    /** Cool RGB tint of the secondary light that softens unlit building faces. */
    val FILL_LIGHT_COLOR = SceneRgb(0.76f, 0.84f, 1f)

    /** Secondary directional-light illuminance in lux. */
    const val FILL_LIGHT_INTENSITY = 22_000f

    /** Fixed world-space direction of the shadow-free secondary light. */
    val FILL_LIGHT_DIRECTION = Direction3(0.45f, -0.65f, -0.6f)

    /** Compose ARGB color displayed behind the Filament scene when the map is hidden. */
    const val HIDDEN_MAP_BACKDROP_ARGB = 0xFFDDE2E6

    fun groundShaderColor(): String {
        val alpha = GROUND_OPACITY
        return SceneRgba(
            red = GROUND_COLOR.red * alpha,
            green = GROUND_COLOR.green * alpha,
            blue = GROUND_COLOR.blue * alpha,
            alpha = alpha
        ).toShaderFloat4()
    }
}

object Scene3DCamera {
    /** Initial scene radius in meters before building geometry is loaded. */
    const val DEFAULT_SCENE_RADIUS_METERS = 40f

    /** Minimum radius used to frame small or empty building meshes. */
    const val MIN_SCENE_RADIUS_METERS = 20f

    /** Default orthographic-camera rotation around the vertical axis. */
    const val DEFAULT_YAW_DEGREES = 0f

    /** Default orthographic-camera elevation angle above the ground plane. */
    const val DEFAULT_PITCH_DEGREES = 42f

    /** Initial orbit-camera distance in meters before geometry is loaded. */
    const val DEFAULT_DISTANCE_METERS = 72f

    /** Radius multiplier used to position the orbiting orthographic camera after a reset. */
    const val RESET_DISTANCE_MULTIPLIER = 1.8f

    /** Default scale of the map-aligned orthographic projection. */
    const val DEFAULT_ORTHOGRAPHIC_ZOOM = 1f

    /** Scene-radius multiplier used to place the top-down camera above the ground. */
    const val TOP_DOWN_HEIGHT_MULTIPLIER = 4f

    /** Minimum top-down camera height in meters, ensuring it clears tall buildings. */
    const val MIN_TOP_DOWN_HEIGHT_METERS = 500f

    /** Distance in meters to the nearest rendered camera plane. */
    const val NEAR_CLIP_METERS = 0.1

    /** Distance in meters to the farthest rendered camera plane. */
    const val FAR_CLIP_METERS = 10_000.0

    /** One-finger orbit sensitivity in degrees of rotation per dragged screen pixel. */
    const val ORBIT_DEGREES_PER_PIXEL = 0.25f

    /** Lowest allowed orthographic orbit pitch in degrees. */
    const val MIN_PITCH_DEGREES = 8f

    /** Highest allowed orthographic orbit pitch in degrees. */
    const val MAX_PITCH_DEGREES = 85f

    /** Closest allowed orthographic zoom scale. */
    const val MIN_ORTHOGRAPHIC_ZOOM = 0.25f

    /** Farthest allowed orthographic zoom scale. */
    const val MAX_ORTHOGRAPHIC_ZOOM = 8f

    /** Maximum pan offset from the origin expressed as a multiplier of scene radius. */
    const val TARGET_LIMIT_MULTIPLIER = 2f

    fun orbitDistance(sceneRadius: Float, viewportRadius: Float): Float =
        max(sceneRadius, viewportRadius).coerceAtLeast(MIN_SCENE_RADIUS_METERS) *
            RESET_DISTANCE_MULTIPLIER

    fun orthographicMetersPerPixel(
        viewportSpanMeters: Float,
        orthographicZoom: Float,
        viewportPixels: Int
    ): Float = viewportSpanMeters * orthographicZoom / max(viewportPixels, 1)
}

object Scene3DGeometry {
    /** WGS84 semi-major Earth radius in meters, used for local map projection. */
    const val WGS84_EARTH_RADIUS_METERS = 6_378_137.0

    /** Mesh-radius multiplier used for ground size when map viewport bounds are unavailable. */
    const val FALLBACK_GROUND_EXTENT_MULTIPLIER = 1.35f

    /** Minimum supported angle in degrees down from the vertical light direction. */
    const val MIN_LIGHT_ZENITH_DEGREES = 0f

    /** Maximum zenith in degrees, kept below the degenerate horizontal-light position. */
    const val MAX_LIGHT_ZENITH_DEGREES = 89.9f
}
