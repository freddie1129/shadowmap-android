package com.gooludou.shadowplanner.feature.shadowmap

import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.project.ProjectSnapshot
import java.time.Clock

internal class ShadowMapProjectMapper(private val clock: Clock) {
    fun toSnapshot(state: ShadowMapUiState, id: String, name: String, createdAt: Long) =
        ProjectSnapshot(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = clock.millis(),
            selectedEpochMillis = state.selectedEpochMillis,
            displayTimeZoneId = state.displayTimeZoneId,
            calculationLocation = state.calculationLocation,
            selectedLocationLabel = state.selectedLocationLabel,
            viewport = state.viewport,
            drawnBuildings = state.drawnBuildings,
            drawnWalls = state.drawnWalls,
            drawnTrees = state.drawnTrees,
            loadedBuildings = state.loadedBuildings,
            loadedBuildingOverrides = state.loadedBuildingOverrides,
            suppressedLoadedBuildings = state.suppressedLoadedBuildings,
            shadowAppearance = state.shadowAppearance
        )

    fun toUiState(project: ProjectSnapshot, previous: ShadowMapUiState): ShadowMapUiState {
        val state = previous.copy(
            selectedEpochMillis = project.selectedEpochMillis,
            displayTimeZoneId = project.displayTimeZoneId,
            calculationLocation = project.calculationLocation,
            selectedLocationLabel = project.selectedLocationLabel,
            viewport = project.viewport,
            activeProjectId = project.id,
            activeProjectName = project.name,
            activeProjectCreatedAt = project.createdAt,
            projectLoadRevision = previous.projectLoadRevision + 1L,
            isProjectDirty = false,
            projectError = null,
            loadedBuildings = project.loadedBuildings,
            loadedBuildingOverrides = project.loadedBuildingOverrides,
            suppressedLoadedBuildings = project.suppressedLoadedBuildings,
            shadowAppearance = project.shadowAppearance ?: ShadowAppearance.DEFAULT,
            automaticBuildingKeysCoveredByManual = emptySet(),
            drawnBuildings = project.drawnBuildings,
            drawnWalls = project.drawnWalls,
            drawnTrees = project.drawnTrees,
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            shadows = emptyList(),
            buildingLoadState = if (project.loadedBuildings.isEmpty()) {
                BuildingLoadState.Idle
            } else {
                BuildingLoadState.Loaded
            }
        )
        return state.withRefreshedAutomaticOverlapSuppression()
    }
}
