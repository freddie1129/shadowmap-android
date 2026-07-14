package com.example.shadowmap.presentation

import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.DrawnObjectSelection
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.domain.SolarPosition
import com.example.shadowmap.domain.SceneBuildingMerger

data class ShadowMapUiState(
    val selectedEpochMillis: Long,
    val displayTimeZoneId: String,
    val calculationLocation: GeoPoint? = null,
    val solarPosition: SolarPosition? = null,
    val loadedBuildings: List<BuildingFootprint> = emptyList(),
    val automaticBuildingKeysCoveredByManual: Set<String> = emptySet(),
    val drawnBuildings: List<DrawnBuilding> = emptyList(),
    val drawnWalls: List<DrawnWall> = emptyList(),
    val drawnTrees: List<DrawnTree> = emptyList(),
    val activeDrawMode: DrawMode? = null,
    val inProgressVertices: List<GeoPoint> = emptyList(),
    val pendingDrawing: PendingDrawing? = null,
    val selectedDrawing: DrawnObjectSelection? = null,
    val drawingError: String? = null,
    val shadows: List<GeoPolygon> = emptyList(),
    val buildingLoadState: BuildingLoadState = BuildingLoadState.Idle
) {
    val visibleLoadedBuildings: List<BuildingFootprint>
        get() = loadedBuildings.filterNot { building ->
            SceneBuildingMerger.automaticKey(building) in automaticBuildingKeysCoveredByManual
        }

    val buildings: List<BuildingFootprint>
        get() = visibleLoadedBuildings + drawnBuildings.map { building ->
            BuildingFootprint(
                id = building.id,
                polygon = building.polygon,
                heightMeters = building.heightMeters,
                minHeightMeters = 0.0
            )
        }

    val hasDrawings: Boolean
        get() = drawnBuildings.isNotEmpty() || drawnWalls.isNotEmpty() || drawnTrees.isNotEmpty()

    val hasSceneObjects: Boolean
        get() = loadedBuildings.isNotEmpty() || hasDrawings

    val hasDraft: Boolean
        get() = inProgressVertices.isNotEmpty() || pendingDrawing != null
}

sealed interface BuildingLoadState {
    data object Idle : BuildingLoadState

    data object Loading : BuildingLoadState

    data object Loaded : BuildingLoadState

    data class Error(val message: String) : BuildingLoadState
}
