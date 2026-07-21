package com.gooludou.shadowplanner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared layout tokens for screens and reusable components. */
@Immutable
data class AppDimensions(
    val spacingXxs: Dp = 2.dp,
    val spacingXs: Dp = 4.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 12.dp,
    val spacingLarge: Dp = 16.dp,
    val spacingExtraLarge: Dp = 24.dp,
    val spacingHuge: Dp = 32.dp,
    val screenPadding: Dp = spacingExtraLarge,
    val floatingControlMargin: Dp = spacingMedium,
    val floatingControlMaxWidth: Dp = 400.dp,
    val floatingControlElevation: Dp = 6.dp,
    val minimumTouchTarget: Dp = 48.dp,
    val iconSize: Dp = spacingExtraLarge
)

internal val DefaultAppDimensions = AppDimensions()
internal val LocalAppDimensions = staticCompositionLocalOf { DefaultAppDimensions }

/** Entry point for ShadowMap design-system tokens not covered by MaterialTheme. */
object ShadowMapDesign {
    val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimensions.current
}
