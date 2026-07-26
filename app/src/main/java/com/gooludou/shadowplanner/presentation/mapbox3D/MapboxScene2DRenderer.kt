package com.gooludou.shadowplanner.presentation.mapbox3D

import com.gooludou.shadowplanner.map.MapboxShadowMapController
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.mapbox.maps.MapView

/** Reuses the 2D map renderer so outlines and calculated shadows stay visually identical. */
internal fun renderMapboxTopDownScene(
    mapView: MapView,
    uiState: ShadowMapUiState,
    visible: Boolean
) {
    MapboxShadowMapController(mapView).render(
        loadedBuildings = if (visible) uiState.visibleLoadedBuildings else emptyList(),
        drawnBuildings = if (visible) uiState.drawnBuildings else emptyList(),
        drawnWalls = if (visible) uiState.drawnWalls else emptyList(),
        drawnTrees = if (visible) uiState.drawnTrees else emptyList(),
        selection = if (visible) uiState.selectedDrawing else null,
        activeDrawMode = null,
        inProgressVertices = emptyList(),
        pendingDrawing = null,
        crosshairPoint = null,
        shadows = if (visible) uiState.shadows else emptyList(),
        shadowAppearance = uiState.shadowAppearance
    )
}
