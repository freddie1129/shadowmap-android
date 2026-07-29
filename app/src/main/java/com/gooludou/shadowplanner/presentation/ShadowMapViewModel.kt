package com.gooludou.shadowplanner.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gooludou.shadowplanner.di.DefaultDispatcher
import com.gooludou.shadowplanner.core.model.AutomaticBuildingIdentity
import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.shadow.BuildingShadowCalculator
import com.gooludou.shadowplanner.core.model.BuildingSource
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.geometry.DrawingGeometryValidator
import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.DrawnObjectType
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.LoadedBuildingOverride
import com.gooludou.shadowplanner.core.model.MoveSession
import com.gooludou.shadowplanner.core.model.PendingDrawing
import com.gooludou.shadowplanner.core.geometry.SceneBuildingMerger
import com.gooludou.shadowplanner.core.model.SceneObjectGeometry
import com.gooludou.shadowplanner.core.model.SceneObjectSource
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.gooludou.shadowplanner.core.solar.SolarPosition
import com.gooludou.shadowplanner.core.solar.SolarPositionCalculator
import com.gooludou.shadowplanner.core.shadow.UserObjectShadowCalculator
import com.gooludou.shadowplanner.core.model.translatedBy
import com.gooludou.shadowplanner.location.CurrentLocationResolver
import com.gooludou.shadowplanner.project.ProjectRepository
import com.gooludou.shadowplanner.project.ProjectSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
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
@Suppress("TooManyFunctions", "LargeClass")
class ShadowMapViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val shadowCalculator: BuildingShadowCalculator,
    private val userObjectShadowCalculator: UserObjectShadowCalculator,
    private val solarPositionCalculator: SolarPositionCalculator,
    private val clock: Clock,
    systemZoneId: ZoneId,
    private val projectRepository: ProjectRepository,
    private val currentLocationResolver: CurrentLocationResolver,
    @param:DefaultDispatcher
    private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var shadowJob: Job? = null
    private var lastDeletedObject: DeletedSceneObject? = null
    private var lastClearedScene: ClearedSceneSnapshot? = null
    private var moveSnapshot: ShadowMapUiState? = null
    private var hasResolvedCurrentLocation = false

    private val _uiState =
        MutableStateFlow(
            ShadowMapUiState(
                selectedEpochMillis =
                    savedStateHandle[SELECTED_TIME_KEY] ?: clock.millis().roundToTimeStep(),
                displayTimeZoneId = savedStateHandle[TIME_ZONE_KEY] ?: systemZoneId.id,
                calculationLocation = restoredCalculationLocation(),
                selectedLocationLabel = savedStateHandle[LOCATION_LABEL_KEY]
            )
        )
    val uiState: StateFlow<ShadowMapUiState> = _uiState.asStateFlow()

    init {
        savedStateHandle[TIME_ZONE_KEY] = _uiState.value.displayTimeZoneId
        recalculateSunAndShadows()
    }

    fun onDateTimeChanged(epochMillis: Long) {
        savedStateHandle[SELECTED_TIME_KEY] = epochMillis
        _uiState.value =
            _uiState.value.copy(selectedEpochMillis = epochMillis, isProjectDirty = true)
        recalculateSunAndShadows()
    }

    fun onViewportChanged(viewport: com.gooludou.shadowplanner.project.ProjectViewport) {
        savedStateHandle[LOCATION_LATITUDE_KEY] = viewport.center.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = viewport.center.longitude
        _uiState.value = _uiState.value.copy(
            viewport = viewport,
            calculationLocation = viewport.center,
            isProjectDirty = true
        )
        recalculateSunAndShadows()
    }

    fun saveProject(name: String? = null) {
        val state = _uiState.value
        saveProject(
            state = state,
            projectId = state.activeProjectId ?: UUID.randomUUID().toString(),
            name = name ?: state.activeProjectName ?: "Untitled project",
            createdAt = state.activeProjectCreatedAt ?: clock.millis()
        )
    }

    fun saveProjectAsNew(name: String) {
        val state = _uiState.value
        saveProject(
            state = state,
            projectId = UUID.randomUUID().toString(),
            name = name,
            createdAt = clock.millis()
        )
    }

    private fun saveProject(
        state: ShadowMapUiState,
        projectId: String,
        name: String,
        createdAt: Long
    ) {
        val snapshot = state.toProjectSnapshot(
            id = projectId,
            name = name,
            createdAt = createdAt
        )
        viewModelScope.launch(computationDispatcher) {
            runCatching { projectRepository.saveProject(snapshot) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        activeProjectId = snapshot.id,
                        activeProjectName = snapshot.name,
                        activeProjectCreatedAt = snapshot.createdAt,
                        isProjectDirty = false,
                        projectError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(projectError = error.message)
                }
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch(computationDispatcher) {
            runCatching { projectRepository.loadProject(id) }
                .onSuccess { project ->
                    val restored = project.toUiState(_uiState.value)
                    _uiState.value = restored
                    savedStateHandle[SELECTED_TIME_KEY] = project.selectedEpochMillis
                    savedStateHandle[TIME_ZONE_KEY] = project.displayTimeZoneId
                    project.calculationLocation?.let { location ->
                        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
                        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
                    }
                    recalculateSunAndShadows()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(projectError = error.message)
                }
        }
    }

    private fun ShadowMapUiState.toProjectSnapshot(id: String, name: String, createdAt: Long) =
        ProjectSnapshot(
            id = id,
            name = name,
            createdAt = createdAt,
            updatedAt = clock.millis(),
            selectedEpochMillis = selectedEpochMillis,
            displayTimeZoneId = displayTimeZoneId,
            calculationLocation = calculationLocation,
            selectedLocationLabel = selectedLocationLabel,
            viewport = viewport,
            drawnBuildings = drawnBuildings,
            drawnWalls = drawnWalls,
            drawnTrees = drawnTrees,
            loadedBuildings = loadedBuildings,
            loadedBuildingOverrides = loadedBuildingOverrides,
            suppressedLoadedBuildings = suppressedLoadedBuildings,
            shadowAppearance = shadowAppearance
        )

    private fun ProjectSnapshot.toUiState(previous: ShadowMapUiState): ShadowMapUiState {
        val state = previous.copy(
            selectedEpochMillis = selectedEpochMillis,
            displayTimeZoneId = displayTimeZoneId,
            calculationLocation = calculationLocation,
            selectedLocationLabel = selectedLocationLabel,
            viewport = viewport,
            activeProjectId = id,
            activeProjectName = name,
            activeProjectCreatedAt = createdAt,
            projectLoadRevision = previous.projectLoadRevision + 1L,
            isProjectDirty = false,
            projectError = null,
            loadedBuildings = loadedBuildings,
            loadedBuildingOverrides = loadedBuildingOverrides,
            suppressedLoadedBuildings = suppressedLoadedBuildings,
            shadowAppearance = shadowAppearance ?: ShadowAppearance.DEFAULT,
            automaticBuildingKeysCoveredByManual = emptySet(),
            drawnBuildings = drawnBuildings,
            drawnWalls = drawnWalls,
            drawnTrees = drawnTrees,
            activeDrawMode = null,
            inProgressVertices = emptyList(),
            pendingDrawing = null,
            selectedDrawing = null,
            shadows = emptyList(),
            buildingLoadState = if (loadedBuildings.isEmpty()) {
                BuildingLoadState.Idle
            } else {
                BuildingLoadState.Loaded
            }
        )
        return state.withRefreshedAutomaticOverlapSuppression()
    }

    fun onNowSelected() {
        val currentTimeMillis = clock.millis()
        val selectedTime = currentTimeMillis.roundToTimeStep()
        onDateTimeChanged(selectedTime)
    }

    fun onBuildingLoadStarted() {
        _uiState.value = _uiState.value.copy(buildingLoadState = BuildingLoadState.Loading)
    }

    fun onShadowAppearanceChanged(appearance: ShadowAppearance) {
        if (_uiState.value.shadowAppearance == appearance) return
        _uiState.value = _uiState.value.copy(
            shadowAppearance = appearance,
            isProjectDirty = true
        )
    }

    fun onMapCenterChanged(location: GeoPoint) {
        val previous = _uiState.value.calculationLocation
        if (previous == location) return
        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
        _uiState.value = _uiState.value.copy(calculationLocation = location)
        recalculateSunAndShadows()
    }

    fun onLocationSelected(location: GeoPoint, label: String) {
        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
        savedStateHandle[LOCATION_LABEL_KEY] = label
        _uiState.value = _uiState.value.copy(
            calculationLocation = location,
            selectedLocationLabel = label
        )
        recalculateSunAndShadows()
    }

    fun onCurrentLocationReceived(location: GeoPoint, fallbackLabel: String) {
        if (hasResolvedCurrentLocation) return
        hasResolvedCurrentLocation = true
        viewModelScope.launch(computationDispatcher) {
            val resolvedLocation = currentLocationResolver.resolve(location)
                .getOrElse {
                    com.gooludou.shadowplanner.location.LocationSearchResult(
                        id = "current:${location.longitude},${location.latitude}",
                        name = fallbackLabel,
                        address = fallbackLabel,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            onLocationSelected(
                location = GeoPoint(
                    longitude = resolvedLocation.longitude ?: location.longitude,
                    latitude = resolvedLocation.latitude ?: location.latitude
                ),
                label = resolvedLocation.address.ifBlank { resolvedLocation.name }
            )
        }
    }

    fun onBuildingsLoaded(buildings: List<Building>, location: GeoPoint) {
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
        _uiState.value =
            state.copy(activeDrawMode = mode, selectedDrawing = null, drawingError = null)
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
        if (state.activeDrawMode != DrawMode.BUILDING &&
            state.activeDrawMode != DrawMode.WALL
        ) {
            return
        }
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

    fun finishBuilding(finalPoint: GeoPoint): Boolean {
        val vertices = _uiState.value.inProgressVertices + finalPoint
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

    fun finishWall(finalPoint: GeoPoint): Boolean {
        val points = _uiState.value.inProgressVertices + finalPoint
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
                drawnBuildings = state.drawnBuildings + Building(
                    id = UUID.randomUUID().toString(),
                    polygon = DrawingGeometryValidator.closedPolygon(pending.vertices),
                    heightMeters = heightMeters,
                    source = BuildingSource.MANUAL
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
        moveSnapshot = null
        _uiState.value = _uiState.value.copy(selectedDrawing = selection, moveSession = null)
    }

    fun startMoving() {
        val state = _uiState.value
        val selection = state.selectedDrawing ?: return
        val geometry = state.geometryFor(selection) ?: return
        moveSnapshot = state
        _uiState.value = state.copy(
            moveSession = MoveSession(selection, geometry, geometry)
        )
    }

    fun moveSelectedObject(longitudeDelta: Double, latitudeDelta: Double) {
        val state = _uiState.value
        val session = state.moveSession ?: return
        val moved = when (val geometry = session.original) {
            is SceneObjectGeometry.Building ->
                SceneObjectGeometry.Building(
                    geometry.polygon.translatedBy(longitudeDelta, latitudeDelta)
                )

            is SceneObjectGeometry.Wall ->
                SceneObjectGeometry.Wall(
                    geometry.points.map { it.translatedBy(longitudeDelta, latitudeDelta) }
                )

            is SceneObjectGeometry.Tree ->
                SceneObjectGeometry.Tree(
                    geometry.center.translatedBy(longitudeDelta, latitudeDelta)
                )
        }
        val updated = state.applyGeometry(session.selection, moved) ?: return
        _uiState.value = updated.copy(
            moveSession = session.copy(current = moved),
            isProjectDirty = true
        )
        recalculateSunAndShadows()
    }

    fun finishMoving() {
        if (_uiState.value.moveSession == null) return
        moveSnapshot = null
        _uiState.value = _uiState.value.copy(
            moveSession = null,
            selectedDrawing = null
        )
    }

    fun cancelMoving() {
        val state = _uiState.value
        val session = state.moveSession ?: return
        val restored = moveSnapshot
        moveSnapshot = null
        if (restored != null) {
            _uiState.value = restored.copy(selectedDrawing = null)
            recalculateSunAndShadows()
        } else {
            val restoredGeometry =
                state.applyGeometry(session.selection, session.original) ?: return
            _uiState.value = restoredGeometry.copy(
                moveSession = null,
                selectedDrawing = null
            )
            recalculateSunAndShadows()
        }
    }

    fun updateSelectedDrawing(heightMeters: Double, radiusMeters: Double? = null) {
        val state = _uiState.value
        val selection = state.selectedDrawing ?: return
        _uiState.value = if (selection.source == SceneObjectSource.AUTOMATIC) {
            val building = state.loadedBuildings.firstOrNull { loaded ->
                AutomaticBuildingMatcher.identity(loaded).selectionId == selection.id
            } ?: return
            val identity = AutomaticBuildingMatcher.identity(building)
            val existingOverride = state.loadedBuildingOverrides[identity]
            state.copy(
                loadedBuildingOverrides = if (
                    abs(heightMeters - building.heightMeters) < HEIGHT_EQUALITY_TOLERANCE_METERS
                ) {
                    if (existingOverride?.adjustedPolygon == null) {
                        state.loadedBuildingOverrides - identity
                    } else {
                        state.loadedBuildingOverrides + (
                            identity to existingOverride.copy(heightMeters = building.heightMeters)
                            )
                    }
                } else {
                    state.loadedBuildingOverrides + (
                        identity to LoadedBuildingOverride(
                            heightMeters = heightMeters,
                            referencePolygon =
                                existingOverride?.referencePolygon ?: building.polygon,
                            adjustedPolygon = existingOverride?.adjustedPolygon
                        )
                        )
                },
                selectedDrawing = null
            )
        } else {
            when (selection.type) {
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
                            it.copy(
                                heightMeters = heightMeters,
                                radiusMeters =
                                    radiusMeters ?: it.radiusMeters
                            )
                        } else {
                            it
                        }
                    },
                    selectedDrawing = null
                )
            }
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
            _uiState.value =
                state.copy(solarPosition = null, sunPath = emptyList(), shadows = emptyList())
            return
        }
        val solarPosition = solarPositionCalculator.calculate(
            epochMillis = state.selectedEpochMillis,
            latitudeDegrees = location.latitude,
            longitudeDegrees = location.longitude
        )
        _uiState.value = state.copy(
            solarPosition = solarPosition,
            sunPath = calculateSunPath(state.selectedEpochMillis, location)
        )
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

    private fun calculateSunPath(epochMillis: Long, location: GeoPoint): List<SolarPosition> {
        val zone = ZoneId.of(_uiState.value.displayTimeZoneId)
        val dayStart = Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
        return (0..96).map { step ->
            solarPositionCalculator.calculate(
                epochMillis = dayStart.plus(step * 15L, ChronoUnit.MINUTES).toEpochMilli(),
                latitudeDegrees = location.latitude,
                longitudeDegrees = location.longitude
            )
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

    private fun ShadowMapUiState.withRefreshedAutomaticOverlapSuppression(): ShadowMapUiState =
        copy(
            automaticBuildingKeysCoveredByManual =
                SceneBuildingMerger.automaticKeysCoveredByManualBuildings(
                    automaticBuildings = loadedBuildings,
                    manualBuildings = drawnBuildings
                )
        )

    private fun ShadowMapUiState.geometryFor(
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
    private fun ShadowMapUiState.applyGeometry(
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

    private fun reconcileLoadedOverrides(
        incoming: List<Building>,
        overrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>
    ): Map<AutomaticBuildingIdentity, LoadedBuildingOverride> {
        if (overrides.isEmpty()) return emptyMap()
        val result = overrides.toMutableMap()
        val candidates = overrides.map { (identity, override) ->
            identity to
                override.referencePolygon
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

    private fun mergeLoadedBuildings(
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

    private fun reconcileLoadedSuppressions(
        incoming: List<Building>,
        suppressions: Map<AutomaticBuildingIdentity, com.gooludou.shadowplanner.core.model.GeoPolygon>
    ): Map<AutomaticBuildingIdentity, com.gooludou.shadowplanner.core.model.GeoPolygon> {
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
        private const val LOCATION_LABEL_KEY = "location_label"
        private const val TIME_STEP_MILLIS = 5 * 60 * 1000L
        private const val SHADOW_DEBOUNCE_MILLIS = 50L

        // Property fields display one decimal place, so half a tenth represents the loaded value.
        private const val HEIGHT_EQUALITY_TOLERANCE_METERS = 0.051
    }
}

private sealed interface DeletedSceneObject {
    data class AutomaticBuilding(val building: Building) : DeletedSceneObject
    data class ManualBuilding(val building: Building) : DeletedSceneObject
    data class Wall(val wall: DrawnWall) : DeletedSceneObject
    data class Tree(val tree: DrawnTree) : DeletedSceneObject
}

private data class ClearedSceneSnapshot(
    val loadedBuildings: List<Building>,
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>,
    val drawnBuildings: List<Building>,
    val drawnWalls: List<DrawnWall>,
    val drawnTrees: List<DrawnTree>,
    val suppressedLoadedBuildings:
    Map<AutomaticBuildingIdentity, com.gooludou.shadowplanner.core.model.GeoPolygon>,
    val selectedDrawing: DrawnObjectSelection?
)
