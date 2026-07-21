package com.gooludou.shadowplanner.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.SolarPosition
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Preview(name = "Mapbox 3D view - light", showBackground = true)
@Composable
private fun MapboxScene3DViewLightPreview() {
    MapboxScene3DViewPreviewContent(darkTheme = false)
}

@Preview(name = "Mapbox 3D view - dark", showBackground = true)
@Composable
private fun MapboxScene3DViewDarkPreview() {
    MapboxScene3DViewPreviewContent(darkTheme = true)
}

@Composable
private fun MapboxScene3DViewPreviewContent(darkTheme: Boolean) {
    ShadowMapTheme(darkTheme = darkTheme, dynamicColor = false) {
        MapboxScene3DView(
            buildings = emptyList(),
            walls = emptyList(),
            trees = emptyList(),
            viewport = MapboxScene3DViewport(
                center = GeoPoint(153.0251, -27.4698),
                zoom = 17.0,
                bearing = 0.0
            ),
            solarPosition = SolarPosition(azimuthDegrees = 315.0, zenithDegrees = 45.0),
            selectedEpochMillis = 1_752_640_000_000L,
            timeZoneId = "Australia/Brisbane",
            onDateTimeChanged = {},
            onNowSelected = {},
            onBackToMap = {}
        )
    }
}
