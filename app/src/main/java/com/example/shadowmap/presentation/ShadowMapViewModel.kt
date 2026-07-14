package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shadowmap.di.DefaultDispatcher
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.BuildingShadowCalculator
import com.example.shadowmap.domain.DrawMode
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.DrawnObjectSelection
import com.example.shadowmap.domain.DrawnObjectType
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.DrawingGeometryValidator
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.PendingDrawing
import com.example.shadowmap.domain.SolarPositionCalculator
import com.example.shadowmap.domain.SceneBuildingMerger
import com.example.shadowmap.domain.UserObjectShadowCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
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
        val mergedBuildings = (_uiState.value.loadedBuildings + buildings)
            .distinctBy(SceneBuildingMerger::automaticKey)
        _uiState.value =
            _uiState.value.copy(
                calculationLocation = location,
                loadedBuildings = mergedBuildings,
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
        _uiState.value = when (selection.type) {
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

    fun deleteSelectedDrawing() {
        val state = _uiState.value
        val selection = state.selectedDrawing ?: return
        _uiState.value = state.copy(
            drawnBuildings = state.drawnBuildings.filterNot { it.id == selection.id },
            drawnWalls = state.drawnWalls.filterNot { it.id == selection.id },
            drawnTrees = state.drawnTrees.filterNot { it.id == selection.id },
            selectedDrawing = null
        ).withRefreshedAutomaticOverlapSuppression()
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
        _uiState.value = _uiState.value.copy(
            loadedBuildings = emptyList(),
            automaticBuildingKeysCoveredByManual = emptySet(),
            drawnBuildings = emptyList(),
            drawnWalls = emptyList(),
            drawnTrees = emptyList(),
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            drawingError = null,
            shadows = emptyList(),
            buildingLoadState = BuildingLoadState.Idle
        )
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

    companion object {
        private const val SELECTED_TIME_KEY = "selected_time"
        private const val TIME_ZONE_KEY = "time_zone"
        private const val LOCATION_LATITUDE_KEY = "location_latitude"
        private const val LOCATION_LONGITUDE_KEY = "location_longitude"
        private const val TIME_STEP_MILLIS = 5 * 60 * 1000L
        private const val SHADOW_DEBOUNCE_MILLIS = 50L
    }
}
