package com.gooludou.shadowplanner.presentation.dashboard

import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.map.MapboxShadowMapController
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.mapbox.maps.MapView

/** Reuses the 2D map renderer so outlines and calculated shadows stay visually identical. */
internal fun renderMapboxDrawingOverlay(
    mapView: MapView,
    uiState: ShadowMapUiState,
    crosshairPoint: GeoPoint?,
    visible: Boolean
) {
    MapboxShadowMapController(mapView).render(
        loadedBuildings = if (visible) uiState.visibleLoadedBuildings else emptyList(),
        drawnBuildings = if (visible) uiState.drawnBuildings else emptyList(),
        drawnWalls = if (visible) uiState.drawnWalls else emptyList(),
        drawnTrees = if (visible) uiState.drawnTrees else emptyList(),
        selection = if (visible) uiState.selectedDrawing else null,
        activeDrawMode = if (visible) uiState.activeDrawMode else null,
        inProgressVertices = if (visible) uiState.inProgressVertices else emptyList(),
        pendingDrawing = if (visible) uiState.pendingDrawing else null,
        crosshairPoint = if (visible) crosshairPoint else null,
        shadows = if (visible) uiState.shadows else emptyList(),
        shadowAppearance = uiState.shadowAppearance
    )
}
