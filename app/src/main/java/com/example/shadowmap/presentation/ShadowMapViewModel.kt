package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shadowmap.di.DefaultDispatcher
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.BuildingShadowCalculator
import com.example.shadowmap.domain.AutomaticBuildingIdentity
import com.example.shadowmap.domain.AutomaticBuildingMatcher
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.DrawnObjectSelection
import com.example.shadowmap.domain.DrawnObjectType
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.DrawingGeometryValidator
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.domain.LoadedBuildingOverride
import com.example.shadowmap.domain.SceneObjectSource
import com.example.shadowmap.domain.SolarPositionCalculator
import com.example.shadowmap.domain.SceneBuildingMerger
import com.example.shadowmap.domain.UserObjectShadowCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
@Suppress("TooManyFunctions")
class ShadowMapViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val shadowCalculator: BuildingShadowCalculator,
    private val userObjectShadowCalculator: UserObjectShadowCalculator,
    private val solarPositionCalculator: SolarPositionCalculator,
    private val clock: Clock,
    systemZoneId: ZoneId,
    @param:DefaultDispatcher
    private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var shadowJob: Job? = null
    private var lastDeletedObject: DeletedSceneObject? = null
    private var lastClearedScene: ClearedSceneSnapshot? = null

    private val _uiState =
        MutableStateFlow(
            ShadowMapUiState(
                selectedEpochMillis =
                    savedStateHandle[SELECTED_TIME_KEY] ?: clock.millis().roundToTimeStep(),
                displayTimeZoneId = savedStateHandle[TIME_ZONE_KEY] ?: systemZoneId.id,
                calculationLocation = restoredCalculationLocation()
            )
        )
    val uiState: StateFlow<ShadowMapUiState> = _uiState.asStateFlow()

    init {
        savedStateHandle[TIME_ZONE_KEY] = _uiState.value.displayTimeZoneId
        recalculateSunAndShadows()
    }

    fun onDateTimeChanged(epochMillis: Long) {
        savedStateHandle[SELECTED_TIME_KEY] = epochMillis
        _uiState.value = _uiState.value.copy(selectedEpochMillis = epochMillis)
        recalculateSunAndShadows()
    }

    fun onNowSelected() {
        onDateTimeChanged(clock.millis().roundToTimeStep())
    }

    fun onBuildingLoadStarted() {
        _uiState.value = _uiState.value.copy(buildingLoadState = BuildingLoadState.Loading)
    }

    fun onMapCenterChanged(location: GeoPoint) {
        val previous = _uiState.value.calculationLocation
        if (previous == location) return
        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
        _uiState.value = _uiState.value.copy(calculationLocation = location)
        recalculateSunAndShadows()
    }

    fun onBuildingsLoaded(buildings: List<BuildingFootprint>, location: GeoPoint) {
        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
        val state = _uiState.value
        val normalizedBuildings = buildings.map { building ->
            building.copy(automaticIdentity = AutomaticBuildingMatcher.identity(building))
        }
        val reconciledOverrides = reconcileLoadedOverrides(
            incoming = normalizedBuildings,
            overrides = state.loadedBuildingOverrides
        )
        val reconciledSuppressions = reconcileLoadedSuppressions(
            incoming = normalizedBuildings,
            suppressions = state.suppressedLoadedBuildings
        )
        val mergedBuildings = mergeLoadedBuildings(state.loadedBuildings, normalizedBuildings)
        _uiState.value =
            state.copy(
                calculationLocation = location,
                loadedBuildings = mergedBuildings,
                loadedBuildingOverrides = reconciledOverrides,
                suppressedLoadedBuildings = reconciledSuppressions,
                shadows = emptyList(),
                buildingLoadState = BuildingLoadState.Loaded
            ).withRefreshedAutomaticOverlapSuppression()
        recalculateSunAndShadows()
    }

    /** Returns false when switching would discard an unfinished building or wall. */
    @Suppress("ReturnCount")
    fun selectDrawMode(mode: DrawMode): Boolean {
        val state = _uiState.value
        if (state.activeDrawMode == mode) return true
        if (state.hasDraft) return false
        _uiState.value = state.copy(activeDrawMode = mode, selectedDrawing = null, drawingError = null)
        return true
    }

    fun discardDraftAndSelectDrawMode(mode: DrawMode) {
        _uiState.value = _uiState.value.copy(
            activeDrawMode = mode,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            drawingError = null
        )
    }

    fun stopDrawing() {
        _uiState.value = _uiState.value.copy(
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            drawingError = null
        )
    }

    fun addVertex(point: GeoPoint) {
        val state = _uiState.value
        if (state.activeDrawMode != DrawMode.BUILDING && state.activeDrawMode != DrawMode.WALL) return
        _uiState.value = state.copy(
            inProgressVertices = state.inProgressVertices + point,
            drawingError = null
        )
    }

    fun undoLastVertex() {
        val vertices = _uiState.value.inProgressVertices
        if (vertices.isEmpty()) return
        _uiState.value = _uiState.value.copy(
            inProgressVertices = vertices.dropLast(1),
            drawingError = null
        )
    }

    fun setDrawingError(message: String) {
        _uiState.value = _uiState.value.copy(drawingError = message)
    }

    fun finishBuilding(): Boolean {
        val vertices = _uiState.value.inProgressVertices
        val error = DrawingGeometryValidator.validateBuilding(vertices)
        if (error != null) {
            _uiState.value = _uiState.value.copy(drawingError = error)
            return false
        }
        _uiState.value = _uiState.value.copy(
            inProgressVertices = emptyList(),
            pendingDrawing = PendingDrawing.Building(vertices),
            drawingError = null
        )
        return true
    }

    fun finishWall(): Boolean {
        val points = _uiState.value.inProgressVertices
        val error = DrawingGeometryValidator.validateWall(points)
        if (error != null) {
            _uiState.value = _uiState.value.copy(drawingError = error)
            return false
        }
        _uiState.value = _uiState.value.copy(
            inProgressVertices = emptyList(),
            pendingDrawing = PendingDrawing.Wall(points),
            drawingError = null
        )
        return true
    }

    fun startTree(point: GeoPoint) {
        if (_uiState.value.activeDrawMode != DrawMode.TREE) return
        _uiState.value = _uiState.value.copy(
            pendingDrawing = PendingDrawing.Tree(point),
            selectedDrawing = null,
            drawingError = null
        )
    }

    fun returnPendingToDrawing() {
        val pending = _uiState.value.pendingDrawing ?: return
        _uiState.value = when (pending) {
            is PendingDrawing.Building -> _uiState.value.copy(
                inProgressVertices = pending.vertices,
                pendingDrawing = null
            )
            is PendingDrawing.Wall -> _uiState.value.copy(
                inProgressVertices = pending.points,
                pendingDrawing = null
            )
            is PendingDrawing.Tree -> _uiState.value.copy(pendingDrawing = null)
        }
    }

    fun commitPendingDrawing(heightMeters: Double, radiusMeters: Double? = null) {
        val state = _uiState.value
        val pending = state.pendingDrawing ?: return
        _uiState.value = when (pending) {
            is PendingDrawing.Building -> state.copy(
                drawnBuildings = state.drawnBuildings + DrawnBuilding(
                    polygon = DrawingGeometryValidator.closedPolygon(pending.vertices),
                    heightMeters = heightMeters
                ),
                activeDrawMode = null,
                pendingDrawing = null
            )
            is PendingDrawing.Wall -> state.copy(
                drawnWalls = state.drawnWalls + DrawnWall(
                    points = pending.points,
                    heightMeters = heightMeters
                ),
                activeDrawMode = null,
                pendingDrawing = null
            )
            is PendingDrawing.Tree -> state.copy(
                drawnTrees = state.drawnTrees + DrawnTree(
                    center = pending.center,
                    heightMeters = heightMeters,
                    radiusMeters = radiusMeters ?: return
                ),
                activeDrawMode = null,
                pendingDrawing = null
            )
        }.withRefreshedAutomaticOverlapSuppression()
        recalculateSunAndShadows()
    }

    fun selectDrawing(selection: DrawnObjectSelection?) {
        _uiState.value = _uiState.value.copy(selectedDrawing = selection)
    }

    fun updateSelectedDrawing(heightMeters: Double, radiusMeters: Double? = null) {
        val state = _uiState.value
        val selection = state.selectedDrawing ?: return
        _uiState.value = if (selection.source == SceneObjectSource.AUTOMATIC) {
            val building = state.loadedBuildings.firstOrNull { loaded ->
                AutomaticBuildingMatcher.identity(loaded).selectionId == selection.id
            } ?: return
            val identity = AutomaticBuildingMatcher.identity(building)
            state.copy(
                loadedBuildingOverrides = if (
                    abs(heightMeters - building.heightMeters) < HEIGHT_EQUALITY_TOLERANCE_METERS
                ) {
                    state.loadedBuildingOverrides - identity
                } else {
                    state.loadedBuildingOverrides + (
                        identity to LoadedBuildingOverride(heightMeters, building.polygon)
                        )
                },
                selectedDrawing = null
            )
        } else when (selection.type) {
            DrawnObjectType.BUILDING -> state.copy(
                drawnBuildings = state.drawnBuildings.map {
                    if (it.id == selection.id) it.copy(heightMeters = heightMeters) else it
                },
                selectedDrawing = null
            )
            DrawnObjectType.WALL -> state.copy(
                drawnWalls = state.drawnWalls.map {
                    if (it.id == selection.id) it.copy(heightMeters = heightMeters) else it
                },
                selectedDrawing = null
            )
            DrawnObjectType.TREE -> state.copy(
                drawnTrees = state.drawnTrees.map {
                    if (it.id == selection.id) {
                        it.copy(heightMeters = heightMeters, radiusMeters = radiusMeters ?: it.radiusMeters)
                    } else {
                        it
                    }
                },
                selectedDrawing = null
            )
        }
        recalculateSunAndShadows()
    }

    fun deleteSelectedDrawing(): Boolean {
        val state = _uiState.value
        val deleted = state.selectedDrawing?.let { state.deletedObject(it) } ?: return false
        lastDeletedObject = deleted
        _uiState.value = when (deleted) {
            is DeletedSceneObject.AutomaticBuilding -> {
                val identity = AutomaticBuildingMatcher.identity(deleted.building)
                state.copy(
                    suppressedLoadedBuildings = state.suppressedLoadedBuildings +
                        (identity to deleted.building.polygon),
                    selectedDrawing = null
                )
            }
            is DeletedSceneObject.ManualBuilding -> state.copy(
                drawnBuildings = state.drawnBuildings.filterNot { it.id == deleted.building.id },
                selectedDrawing = null
            )
            is DeletedSceneObject.Wall -> state.copy(
                drawnWalls = state.drawnWalls.filterNot { it.id == deleted.wall.id },
                selectedDrawing = null
            )
            is DeletedSceneObject.Tree -> state.copy(
                drawnTrees = state.drawnTrees.filterNot { it.id == deleted.tree.id },
                selectedDrawing = null
            )
        }.withRefreshedAutomaticOverlapSuppression()
        recalculateSunAndShadows()
        return true
    }

    fun restoreLastDeletedObject() {
        val deleted = lastDeletedObject ?: return
        val state = _uiState.value
        _uiState.value = when (deleted) {
            is DeletedSceneObject.AutomaticBuilding -> {
                val identity = AutomaticBuildingMatcher.identity(deleted.building)
                state.copy(
                    suppressedLoadedBuildings = state.suppressedLoadedBuildings - identity
                )
            }
            is DeletedSceneObject.ManualBuilding -> state.copy(
                drawnBuildings = (state.drawnBuildings + deleted.building).distinctBy { it.id }
            )
            is DeletedSceneObject.Wall -> state.copy(
                drawnWalls = (state.drawnWalls + deleted.wall).distinctBy { it.id }
            )
            is DeletedSceneObject.Tree -> state.copy(
                drawnTrees = (state.drawnTrees + deleted.tree).distinctBy { it.id }
            )
        }.withRefreshedAutomaticOverlapSuppression()
        lastDeletedObject = null
        recalculateSunAndShadows()
    }

    fun clearDrawings() {
        _uiState.value = _uiState.value.copy(
            drawnBuildings = emptyList(),
            drawnWalls = emptyList(),
            drawnTrees = emptyList(),
            automaticBuildingKeysCoveredByManual = emptySet(),
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            drawingError = null
        )
        recalculateSunAndShadows()
    }

    fun clearScene() {
        val state = _uiState.value
        lastClearedScene = ClearedSceneSnapshot(
            loadedBuildings = state.loadedBuildings,
            loadedBuildingOverrides = state.loadedBuildingOverrides,
            drawnBuildings = state.drawnBuildings,
            drawnWalls = state.drawnWalls,
            drawnTrees = state.drawnTrees,
            suppressedLoadedBuildings = state.suppressedLoadedBuildings,
            selectedDrawing = state.selectedDrawing
        )
        val allLoadedSuppressions = state.loadedBuildings.associate { building ->
            AutomaticBuildingMatcher.identity(building) to building.polygon
        }
        _uiState.value = state.copy(
            suppressedLoadedBuildings = state.suppressedLoadedBuildings + allLoadedSuppressions,
            automaticBuildingKeysCoveredByManual = emptySet(),
            drawnBuildings = emptyList(),
            drawnWalls = emptyList(),
            drawnTrees = emptyList(),
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            drawingError = null,
            shadows = emptyList()
        )
        recalculateSunAndShadows()
    }

    fun restoreClearedScene() {
        val snapshot = lastClearedScene ?: return
        _uiState.value = _uiState.value.copy(
            loadedBuildings = snapshot.loadedBuildings,
            loadedBuildingOverrides = snapshot.loadedBuildingOverrides,
            drawnBuildings = snapshot.drawnBuildings,
            drawnWalls = snapshot.drawnWalls,
            drawnTrees = snapshot.drawnTrees,
            suppressedLoadedBuildings = snapshot.suppressedLoadedBuildings,
            selectedDrawing = snapshot.selectedDrawing
        ).withRefreshedAutomaticOverlapSuppression()
        lastClearedScene = null
        recalculateSunAndShadows()
    }

    fun onBuildingLoadFailed(throwable: Throwable) {
        _uiState.value =
            _uiState.value.copy(
                buildingLoadState =
                    BuildingLoadState.Error(
                        throwable.message ?: "Unable to load buildings"
                    )
            )
    }

    private fun recalculateSunAndShadows() {
        shadowJob?.cancel()
        val state = _uiState.value
        val location = state.calculationLocation
        if (location == null) {
            _uiState.value = state.copy(solarPosition = null, shadows = emptyList())
            return
        }
        val solarPosition = solarPositionCalculator.calculate(
            epochMillis = state.selectedEpochMillis,
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude
        )
        _uiState.value = state.copy(solarPosition = solarPosition)
        val hasShadowCasters = state.buildings.isNotEmpty() ||
            state.drawnWalls.isNotEmpty() || state.drawnTrees.isNotEmpty()
        if (!hasShadowCasters || !solarPosition.isAboveHorizon) {
            _uiState.value = _uiState.value.copy(shadows = emptyList())
            return
        }

        shadowJob =
            viewModelScope.launch(computationDispatcher) {
                delay(SHADOW_DEBOUNCE_MILLIS)
                val shadows = shadowCalculator.calculate(
                        buildings = state.buildings,
                        azimuthDegrees = solarPosition.azimuthDegrees,
                        zenithDegrees = solarPosition.zenithDegrees
                    ) + userObjectShadowCalculator.calculate(
                        walls = state.drawnWalls,
                        trees = state.drawnTrees,
                        origin = location,
                        azimuthDegrees = solarPosition.azimuthDegrees,
                        zenithDegrees = solarPosition.zenithDegrees
                    )
                ensureActive()
                _uiState.value = _uiState.value.copy(shadows = shadows)
            }
    }

    private fun restoredCalculationLocation(): GeoPoint? {
        val latitude = savedStateHandle.get<Double>(LOCATION_LATITUDE_KEY)
        val longitude = savedStateHandle.get<Double>(LOCATION_LONGITUDE_KEY)
        return if (latitude != null && longitude != null) {
            GeoPoint(longitude = longitude, latitude = latitude)
        } else {
            null
        }
    }

    private fun Long.roundToTimeStep(): Long =
        ((this + TIME_STEP_MILLIS / 2) / TIME_STEP_MILLIS) * TIME_STEP_MILLIS

    private fun ShadowMapUiState.withRefreshedAutomaticOverlapSuppression(): ShadowMapUiState = copy(
        automaticBuildingKeysCoveredByManual =
            SceneBuildingMerger.automaticKeysCoveredByManualBuildings(
                automaticBuildings = loadedBuildings,
                manualBuildings = drawnBuildings
            )
    )

    private fun reconcileLoadedOverrides(
        incoming: List<BuildingFootprint>,
        overrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>
    ): Map<AutomaticBuildingIdentity, LoadedBuildingOverride> {
        if (overrides.isEmpty()) return emptyMap()
        val result = overrides.toMutableMap()
        val candidates = overrides.map { (identity, override) -> identity to override.referencePolygon }
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

    private fun mergeLoadedBuildings(
        existing: List<BuildingFootprint>,
        incoming: List<BuildingFootprint>
    ): List<BuildingFootprint> {
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

    private fun reconcileLoadedSuppressions(
        incoming: List<BuildingFootprint>,
        suppressions: Map<AutomaticBuildingIdentity, com.example.shadowmap.domain.GeoPolygon>
    ): Map<AutomaticBuildingIdentity, com.example.shadowmap.domain.GeoPolygon> {
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

    private fun ShadowMapUiState.deletedObject(
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

    companion object {
        private const val SELECTED_TIME_KEY = "selected_time"
        private const val TIME_ZONE_KEY = "time_zone"
        private const val LOCATION_LATITUDE_KEY = "location_latitude"
        private const val LOCATION_LONGITUDE_KEY = "location_longitude"
        private const val TIME_STEP_MILLIS = 5 * 60 * 1000L
        private const val SHADOW_DEBOUNCE_MILLIS = 50L
        // Property fields display one decimal place, so half a tenth represents the loaded value.
        private const val HEIGHT_EQUALITY_TOLERANCE_METERS = 0.051
    }
}

private sealed interface DeletedSceneObject {
    data class AutomaticBuilding(val building: BuildingFootprint) : DeletedSceneObject
    data class ManualBuilding(val building: DrawnBuilding) : DeletedSceneObject
    data class Wall(val wall: DrawnWall) : DeletedSceneObject
    data class Tree(val tree: DrawnTree) : DeletedSceneObject
}

private data class ClearedSceneSnapshot(
    val loadedBuildings: List<BuildingFootprint>,
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>,
    val drawnBuildings: List<DrawnBuilding>,
    val drawnWalls: List<DrawnWall>,
    val drawnTrees: List<DrawnTree>,
    val suppressedLoadedBuildings: Map<AutomaticBuildingIdentity, com.example.shadowmap.domain.GeoPolygon>,
    val selectedDrawing: DrawnObjectSelection?
)
