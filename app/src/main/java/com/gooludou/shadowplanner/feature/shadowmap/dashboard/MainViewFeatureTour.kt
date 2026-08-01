package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

internal enum class MainViewTooltipTarget {
    LOCATION_SEARCH,
    CURRENT_LOCATION,
    OPEN_PROJECT,
    SAVE_PROJECT,
    DATE_TIME,
    SHADOW_COLOUR,
    SKY_DISPLAY,
    REFRESH_SKY,
    MAP_DISPLAY,
    BASEMAP,
    CAMERA,
    EDIT
}

private val mainViewTooltipSteps = listOf(
    FeatureTourStep(
        MainViewTooltipTarget.LOCATION_SEARCH.name,
        R.string.main_view_tooltip_location_title,
        R.string.main_view_tooltip_location_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.CURRENT_LOCATION.name,
        R.string.main_view_tooltip_current_location_title,
        R.string.main_view_tooltip_current_location_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.OPEN_PROJECT.name,
        R.string.main_view_tooltip_open_project_title,
        R.string.main_view_tooltip_open_project_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.SAVE_PROJECT.name,
        R.string.main_view_tooltip_save_project_title,
        R.string.main_view_tooltip_save_project_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.DATE_TIME.name,
        R.string.main_view_tooltip_date_time_title,
        R.string.main_view_tooltip_date_time_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.SHADOW_COLOUR.name,
        R.string.main_view_tooltip_shadow_colour_title,
        R.string.main_view_tooltip_shadow_colour_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.BASEMAP.name,
        R.string.main_view_tooltip_basemap_title,
        R.string.main_view_tooltip_basemap_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.MAP_DISPLAY.name,
        R.string.main_view_tooltip_map_display_title,
        R.string.main_view_tooltip_map_display_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.CAMERA.name,
        R.string.main_view_tooltip_camera_title,
        R.string.main_view_tooltip_camera_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.SKY_DISPLAY.name,
        R.string.main_view_tooltip_sky_title,
        R.string.main_view_tooltip_sky_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.REFRESH_SKY.name,
        R.string.main_view_tooltip_refresh_sky_title,
        R.string.main_view_tooltip_refresh_sky_description
    ),
    FeatureTourStep(
        MainViewTooltipTarget.EDIT.name,
        R.string.main_view_tooltip_edit_title,
        R.string.main_view_tooltip_edit_description
    )
)

@Composable
internal fun MainViewFeatureTour(
    targetBounds: Map<MainViewTooltipTarget, Rect>,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    FeatureTourOverlay(
        targetBounds = targetBounds.mapKeys { it.key.name },
        steps = mainViewTooltipSteps,
        onCompleted = onCompleted,
        modifier = modifier
    )
}

@Preview(name = "Main view feature tour - light", widthDp = 400, heightDp = 800)
@Composable
private fun MainViewFeatureTourLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        MainViewFeatureTourPreviewContent()
    }
}

@Preview(name = "Main view feature tour - dark", widthDp = 400, heightDp = 800)
@Composable
private fun MainViewFeatureTourDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        MainViewFeatureTourPreviewContent()
    }
}

@Composable
private fun MainViewFeatureTourPreviewContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MainViewFeatureTour(
            targetBounds = mapOf(
                MainViewTooltipTarget.LOCATION_SEARCH to Rect(96f, 28f, 304f, 84f)
            ),
            onCompleted = {}
        )
    }
}
