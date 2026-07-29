package com.gooludou.shadowplanner.feature.shadowmap

import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.geometry.SceneBuildingMerger
import com.gooludou.shadowplanner.core.model.AutomaticBuildingIdentity
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.DrawnObjectType
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.model.LoadedBuildingOverride
import com.gooludou.shadowplanner.core.model.SceneObjectGeometry
import com.gooludou.shadowplanner.core.model.SceneObjectSource

internal fun ShadowMapUiState.withRefreshedAutomaticOverlapSuppression(): ShadowMapUiState =
    copy(
        automaticBuildingKeysCoveredByManual =
            SceneBuildingMerger.automaticKeysCoveredByManualBuildings(
                automaticBuildings = loadedBuildings,
                manualBuildings = drawnBuildings
            )
    )

internal fun ShadowMapUiState.geometryFor(
    selection: DrawnObjectSelection
): SceneObjectGeometry? = if (selection.source == SceneObjectSource.AUTOMATIC) {
    visibleLoadedBuildings.firstOrNull { building ->
        AutomaticBuildingMatcher.identity(building).selectionId == selection.id
    }?.let { SceneObjectGeometry.Building(it.polygon) }
} else {
    when (selection.type) {
        DrawnObjectType.BUILDING -> drawnBuildings.firstOrNull { it.id == selection.id }
            ?.let { SceneObjectGeometry.Building(it.polygon) }

        DrawnObjectType.WALL -> drawnWalls.firstOrNull { it.id == selection.id }
            ?.let { SceneObjectGeometry.Wall(it.points) }

        DrawnObjectType.TREE -> drawnTrees.firstOrNull { it.id == selection.id }
            ?.let { SceneObjectGeometry.Tree(it.center) }
    }
}

@Suppress("ReturnCount")
internal fun ShadowMapUiState.applyGeometry(
    selection: DrawnObjectSelection,
    geometry: SceneObjectGeometry
): ShadowMapUiState? {
    if (selection.source == SceneObjectSource.AUTOMATIC) {
        val building = loadedBuildings.firstOrNull { loaded ->
            AutomaticBuildingMatcher.identity(loaded).selectionId == selection.id
        } ?: return null
        val identity = AutomaticBuildingMatcher.identity(building)
        val existing = loadedBuildingOverrides[identity]
        val polygon = (geometry as? SceneObjectGeometry.Building)?.polygon ?: return null
        return copy(
            loadedBuildingOverrides = loadedBuildingOverrides + (
                identity to LoadedBuildingOverride(
                    heightMeters = existing?.heightMeters ?: building.heightMeters,
                    referencePolygon = existing?.referencePolygon ?: building.polygon,
                    adjustedPolygon = polygon
                )
                )
        )
    }
    return when (geometry) {
        is SceneObjectGeometry.Building -> copy(
            drawnBuildings = drawnBuildings.map {
                if (it.id == selection.id) it.copy(polygon = geometry.polygon) else it
            }
        )

        is SceneObjectGeometry.Wall -> copy(
            drawnWalls = drawnWalls.map {
                if (it.id == selection.id) it.copy(points = geometry.points) else it
            }
        )

        is SceneObjectGeometry.Tree -> copy(
            drawnTrees = drawnTrees.map {
                if (it.id == selection.id) it.copy(center = geometry.center) else it
            }
        )
    }
}

internal fun reconcileLoadedOverrides(
    incoming: List<Building>,
    overrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>
): Map<AutomaticBuildingIdentity, LoadedBuildingOverride> {
    if (overrides.isEmpty()) return emptyMap()
    val result = overrides.toMutableMap()
    val candidates = overrides.map { (identity, override) ->
        identity to override.referencePolygon
    }
    incoming.forEach { building ->
        val newIdentity = AutomaticBuildingMatcher.identity(building)
        val oldIdentity = AutomaticBuildingMatcher.findMatch(building, candidates)
        if (oldIdentity != null && oldIdentity != newIdentity) {
            val override = result.remove(oldIdentity) ?: overrides.getValue(oldIdentity)
            result[newIdentity] = override.copy(referencePolygon = building.polygon)
        }
    }
    return result
}

internal fun mergeLoadedBuildings(
    existing: List<Building>,
    incoming: List<Building>
): List<Building> {
    val remainingExisting = existing.toMutableList()
    incoming.forEach { building ->
        val candidates = remainingExisting.map { candidate ->
            AutomaticBuildingMatcher.identity(candidate) to candidate.polygon
        }
        val matchedIdentity = AutomaticBuildingMatcher.findMatch(building, candidates)
        remainingExisting.removeAll { candidate ->
            val sameMatchedObject = matchedIdentity != null &&
                AutomaticBuildingMatcher.identity(candidate) == matchedIdentity
            sameMatchedObject || SceneBuildingMerger.automaticKey(candidate) ==
                SceneBuildingMerger.automaticKey(building)
        }
    }
    return (remainingExisting + incoming)
        .associateBy(SceneBuildingMerger::automaticKey)
        .values
        .toList()
}

internal fun reconcileLoadedSuppressions(
    incoming: List<Building>,
    suppressions: Map<AutomaticBuildingIdentity, GeoPolygon>
): Map<AutomaticBuildingIdentity, GeoPolygon> {
    if (suppressions.isEmpty()) return emptyMap()
    val result = suppressions.toMutableMap()
    val candidates = suppressions.toList()
    incoming.forEach { building ->
        val newIdentity = AutomaticBuildingMatcher.identity(building)
        val oldIdentity = AutomaticBuildingMatcher.findMatch(building, candidates)
        if (oldIdentity != null && oldIdentity != newIdentity) {
            result.remove(oldIdentity)
            result[newIdentity] = building.polygon
        }
    }
    return result
}

internal fun ShadowMapUiState.deletedObject(
    selection: DrawnObjectSelection
): DeletedSceneObject? = if (selection.source == SceneObjectSource.AUTOMATIC) {
    visibleLoadedBuildings.firstOrNull { building ->
        AutomaticBuildingMatcher.identity(building).selectionId == selection.id
    }?.let(DeletedSceneObject::AutomaticBuilding)
} else {
    when (selection.type) {
        DrawnObjectType.BUILDING -> drawnBuildings.firstOrNull { it.id == selection.id }
            ?.let(DeletedSceneObject::ManualBuilding)

        DrawnObjectType.WALL -> drawnWalls.firstOrNull { it.id == selection.id }
            ?.let(DeletedSceneObject::Wall)

        DrawnObjectType.TREE -> drawnTrees.firstOrNull { it.id == selection.id }
            ?.let(DeletedSceneObject::Tree)
    }
}

internal sealed interface DeletedSceneObject {
    data class AutomaticBuilding(val building: Building) : DeletedSceneObject
    data class ManualBuilding(val building: Building) : DeletedSceneObject
    data class Wall(val wall: DrawnWall) : DeletedSceneObject
    data class Tree(val tree: DrawnTree) : DeletedSceneObject
}
