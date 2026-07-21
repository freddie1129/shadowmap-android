package com.gooludou.shadowplanner.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShadowAppearanceTest {
    @Test
    fun presets_matchAndroidShadowMap() {
        assertEquals(
            listOf(
                0xFF1E1E1EL,
                0xFF0D47A1L,
                0xFF4A148CL,
                0xFFE65100L,
                0xFF004D40L
            ),
            ShadowAppearance.PRESET_COLORS
        )
    }

    @Test
    fun default_matchesAndroidShadowMap() {
        assertEquals(0xFF1E1E1EL, ShadowAppearance.DEFAULT.colorArgb)
        assertEquals(128f / 255f, ShadowAppearance.DEFAULT.opacity, 0.000001f)
    }
}
