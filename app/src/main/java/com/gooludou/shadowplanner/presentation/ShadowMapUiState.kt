package com.gooludou.shadowplanner.presentation

import com.gooludou.shadowplanner.core.model.AutomaticBuildingIdentity
import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.model.LoadedBuildingOverride
import com.gooludou.shadowplanner.core.model.MoveSession
import com.gooludou.shadowplanner.core.model.PendingDrawing
import com.gooludou.shadowplanner.core.geometry.SceneBuildingMerger
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.core.solar.SolarPosition
import com.gooludou.shadowplanner.project.ProjectViewport

data class ShadowMapUiState(
    val selectedEpochMillis: Long,
    val displayTimeZoneId: String,
    val calculationLocation: GeoPoint? = null,
    val selectedLocationLabel: String? = null,
    val viewport: ProjectViewport? = null,
    val activeProjectId: String? = null,
    val activeProjectName: String? = null,
    val activeProjectCreatedAt: Long? = null,
    val projectLoadRevision: Long = 0L,
    val isProjectDirty: Boolean = false,
    val projectError: String? = null,
    val solarPosition: SolarPosition? = null,
    val sunPath: List<SolarPosition> = emptyList(),
    val loadedBuildings: List<Building> = emptyList(),
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride> =
        emptyMap(),
    val suppressedLoadedBuildings: Map<AutomaticBuildingIdentity, GeoPolygon> = emptyMap(),
    val automaticBuildingKeysCoveredByManual: Set<String> = emptySet(),
    val drawnBuildings: List<Building> = emptyList(),
    val drawnWalls: List<DrawnWall> = emptyList(),
    val drawnTrees: List<DrawnTree> = emptyList(),
    val activeDrawMode: DrawMode? = null,
    val inProgressVertices: List<GeoPoint> = emptyList(),
    val pendingDrawing: PendingDrawing? = null,
    val selectedDrawing: DrawnObjectSelection? = null,
    val moveSession: MoveSession? = null,
    val drawingError: String? = null,
    val shadows: List<GeoPolygon> = emptyList(),
    val shadowAppearance: ShadowAppearance = ShadowAppearance.DEFAULT,
    val buildingLoadState: BuildingLoadState = BuildingLoadState.Idle
) {
    val visibleLoadedBuildings: List<Building>
        get() = loadedBuildings.mapNotNull { building ->
            val identity = AutomaticBuildingMatcher.identity(building)
            if (
                identity in suppressedLoadedBuildings ||
                SceneBuildingMerger.automaticKey(building) in automaticBuildingKeysCoveredByManual
            ) {
                null
            } else {
                loadedBuildingOverrides[identity]?.let { override ->
                    building.copy(
                        polygon = override.adjustedPolygon ?: building.polygon,
                        heightMeters = override.heightMeters,
                        automaticIdentity = identity
                    )
                } ?: building.copy(automaticIdentity = identity)
            }
        }

    val buildings: List<Building>
        get() = visibleLoadedBuildings + drawnBuildings

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
