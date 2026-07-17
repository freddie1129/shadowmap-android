package com.example.shadowmap.presentation

import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.AutomaticBuildingIdentity
import com.example.shadowmap.domain.AutomaticBuildingMatcher
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.DrawnObjectSelection
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.domain.LoadedBuildingOverride
import com.example.shadowmap.domain.SolarPosition
import com.example.shadowmap.domain.SceneBuildingMerger
import com.example.shadowmap.project.ProjectViewport

data class ShadowMapUiState(
    val selectedEpochMillis: Long,
    val displayTimeZoneId: String,
    val calculationLocation: GeoPoint? = null,
    val selectedLocationLabel: String? = null,
    val viewport: ProjectViewport? = null,
    val activeProjectId: String? = null,
    val activeProjectName: String? = null,
    val activeProjectCreatedAt: Long? = null,
    val isProjectDirty: Boolean = false,
    val projectError: String? = null,
    val solarPosition: SolarPosition? = null,
    val loadedBuildings: List<BuildingFootprint> = emptyList(),
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride> = emptyMap(),
    val suppressedLoadedBuildings: Map<AutomaticBuildingIdentity, GeoPolygon> = emptyMap(),
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
        get() = loadedBuildings.mapNotNull { building ->
            val identity = AutomaticBuildingMatcher.identity(building)
            if (
                identity in suppressedLoadedBuildings ||
                SceneBuildingMerger.automaticKey(building) in automaticBuildingKeysCoveredByManual
            ) {
                null
            } else {
                loadedBuildingOverrides[identity]?.let { override ->
                    building.copy(heightMeters = override.heightMeters, automaticIdentity = identity)
                } ?: building.copy(automaticIdentity = identity)
            }
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
        get() = visibleLoadedBuildings.isNotEmpty() || hasDrawings

    val hasDraft: Boolean
        get() = inProgressVertices.isNotEmpty() || pendingDrawing != null

    fun isLoadedBuildingEdited(selectionId: String): Boolean =
        loadedBuildingOverrides.keys.any { it.selectionId == selectionId }
}

sealed interface BuildingLoadState {
    data object Idle : BuildingLoadState

    data object Loading : BuildingLoadState

    data object Loaded : BuildingLoadState

    data class Error(val message: String) : BuildingLoadState
}
