package com.example.shadowmap.domain

import java.util.Locale

data class ShadowAppearance(
    val colorArgb: Long = DEFAULT_SHADOW_COLOR_ARGB,
    val opacity: Float = DEFAULT_SHADOW_OPACITY
) {
    val mapboxColor: String
        get() = String.format(Locale.US, "#%06X", colorArgb and RGB_MASK)

    companion object {
        val DEFAULT = ShadowAppearance()

        // Opaque RGB presets; shadow opacity is configured independently.
        val PRESET_COLORS = listOf(
            0xFF1E1E1EL, // Charcoal (default)
            0xFF0D47A1L, // Navy
            0xFF4A148CL, // Purple
            0xFFE65100L, // Amber
            0xFF004D40L // Teal
        )

        private const val RGB_MASK = 0xFFFFFFL
    }
}

const val DEFAULT_SHADOW_COLOR_ARGB: Long = 0xFF1E1E1EL
const val DEFAULT_SHADOW_OPACITY: Float = 0.5019608f
