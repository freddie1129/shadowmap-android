package com.gooludou.shadowplanner.presentation

import androidx.lifecycle.SavedStateHandle
import com.gooludou.shadowplanner.domain.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.BuildingShadowCalculator
import com.gooludou.shadowplanner.domain.DrawMode
import com.gooludou.shadowplanner.domain.DrawnObjectSelection
import com.gooludou.shadowplanner.domain.DrawnObjectType
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.GeoPolygon
import com.gooludou.shadowplanner.domain.PendingDrawing
import com.gooludou.shadowplanner.domain.SceneObjectSource
import com.gooludou.shadowplanner.domain.SolarPositionCalculator
import com.gooludou.shadowplanner.domain.UserObjectShadowCalculator
import com.gooludou.shadowplanner.location.CurrentLocationResolver
import com.gooludou.shadowplanner.location.LocationSearchResult
import com.gooludou.shadowplanner.project.ProjectRepository
import com.gooludou.shadowplanner.project.ProjectSnapshot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShadowMapViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dateTimeChanges_updateStateAndSavedState() {
        val savedState = SavedStateHandle()
        val viewModel = createViewModel(savedState)
        val selectedTime = Instant.parse("2026-12-21T01:15:00Z").toEpochMilli()

        viewModel.onDateTimeChanged(selectedTime)

        assertEquals(selectedTime, viewModel.uiState.value.selectedEpochMillis)
        assertEquals(selectedTime, savedState.get<Long>("selected_time"))
    }

    @Test
    fun buildingLoadFailure_exposesRetryableErrorState() {
        val viewModel = createViewModel()

        viewModel.onBuildingLoadStarted()
        viewModel.onBuildingLoadFailed(IllegalStateException("Map failed"))

        assertEquals(
            BuildingLoadState.Error("Map failed"),
            viewModel.uiState.value.buildingLoadState
        )
    }

    @Test
    fun loadedBuildings_generateShadows() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        advanceUntilIdle()

        assertEquals(BuildingLoadState.Loaded, viewModel.uiState.value.buildingLoadState)
        assertTrue(
            viewModel.uiState.value.shadows
                .isNotEmpty()
        )
    }

    @Test
    fun nighttimeSelection_removesDirectShadows() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shadows.isNotEmpty())

        viewModel.onDateTimeChanged(Instant.parse("2026-07-13T14:00:00Z").toEpochMilli())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.solarPosition?.isAboveHorizon == false)
        assertTrue(viewModel.uiState.value.shadows.isEmpty())
    }

    @Test
    fun buildingDraft_canReturnFromPropertiesAndCommit() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.BUILDING)
        buildingVertices().forEach(viewModel::addVertex)

        assertTrue(viewModel.finishBuilding(BUILDING_FINISH_POINT))
        val pendingBuilding = viewModel.uiState.value.pendingDrawing as PendingDrawing.Building
        assertEquals(BUILDING_FINISH_POINT, pendingBuilding.vertices.last())

        viewModel.returnPendingToDrawing()
        assertEquals(5, viewModel.uiState.value.inProgressVertices.size)

        assertTrue(viewModel.finishBuilding(BUILDING_FINISH_POINT))
        viewModel.commitPendingDrawing(heightMeters = 7.5)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.drawnBuildings.size)
        assertEquals(7.5, viewModel.uiState.value.drawnBuildings.single().heightMeters, 0.0)
        assertEquals(2, viewModel.uiState.value.buildings.size)
    }

    @Test
    fun drawingBeforeAutomaticLoad_createsRenderableScene() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onMapCenterChanged(TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.BUILDING)
        buildingVertices().forEach(viewModel::addVertex)

        assertTrue(viewModel.finishBuilding(BUILDING_FINISH_POINT))
        viewModel.commitPendingDrawing(heightMeters = 7.5)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.buildings.size)
        assertEquals(null, viewModel.uiState.value.activeDrawMode)
        assertTrue(viewModel.uiState.value.shadows.isNotEmpty())
    }

    @Test
    fun automaticLoadAfterDrawing_mergesWithoutRemovingManualObjects() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onMapCenterChanged(TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.BUILDING)
        buildingVertices().forEach(viewModel::addVertex)
        viewModel.finishBuilding(BUILDING_FINISH_POINT)
        viewModel.commitPendingDrawing(heightMeters = 6.0)

        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.drawnBuildings.size)
        assertEquals(1, viewModel.uiState.value.loadedBuildings.size)
        assertEquals(2, viewModel.uiState.value.buildings.size)
    }

    @Test
    fun repeatedAutomaticLoads_mergeAndDeduplicateBySourceId() {
        val viewModel = createViewModel()

        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        viewModel.onBuildingsLoaded(
            listOf(
                testBuilding(),
                testBuilding().copy(id = "second-building")
            ),
            TEST_LOCATION
        )

        assertEquals(2, viewModel.uiState.value.loadedBuildings.size)
    }

    @Test
    fun switchingTools_requiresDraftDiscardConfirmation() {
        val viewModel = createViewModel()
        viewModel.selectDrawMode(DrawMode.BUILDING)
        viewModel.addVertex(GeoPoint(153.0, -28.0))

        assertTrue(!viewModel.selectDrawMode(DrawMode.WALL))
        assertEquals(DrawMode.BUILDING, viewModel.uiState.value.activeDrawMode)

        viewModel.discardDraftAndSelectDrawMode(DrawMode.WALL)
        assertEquals(DrawMode.WALL, viewModel.uiState.value.activeDrawMode)
        assertTrue(viewModel.uiState.value.inProgressVertices.isEmpty())
    }

    @Test
    fun clearDrawings_keepsLoadedBuildings() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.BUILDING)
        buildingVertices().forEach(viewModel::addVertex)
        viewModel.finishBuilding(BUILDING_FINISH_POINT)
        viewModel.commitPendingDrawing(6.0)

        viewModel.clearDrawings()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.loadedBuildings.size)
        assertEquals(1, viewModel.uiState.value.buildings.size)
        assertTrue(!viewModel.uiState.value.hasDrawings)
    }

    @Test
    fun clearScene_suppressesAutomaticAndRemovesManualObjects() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.TREE)
        viewModel.startTree(TEST_LOCATION)
        viewModel.commitPendingDrawing(heightMeters = 8.0, radiusMeters = 2.5)

        viewModel.clearScene()

        assertTrue(viewModel.uiState.value.buildings.isEmpty())
        assertTrue(viewModel.uiState.value.drawnTrees.isEmpty())
        assertEquals(BuildingLoadState.Loaded, viewModel.uiState.value.buildingLoadState)
        assertEquals(1, viewModel.uiState.value.suppressedLoadedBuildings.size)
    }

    @Test
    fun editedLoadedBuilding_keepsHeightAfterReload() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        val building = viewModel.uiState.value.visibleLoadedBuildings.single()
        viewModel.selectDrawing(
            DrawnObjectSelection(
                id = AutomaticBuildingMatcher.identity(building).selectionId,
                type = DrawnObjectType.BUILDING,
                source = SceneObjectSource.AUTOMATIC
            )
        )

        viewModel.updateSelectedDrawing(heightMeters = 12.5)
        viewModel.onBuildingsLoaded(
            listOf(testBuilding().copy(heightMeters = 31.0)),
            TEST_LOCATION
        )

        assertEquals(
            12.5,
            viewModel.uiState.value.visibleLoadedBuildings.single().heightMeters,
            0.0
        )
        assertEquals(1, viewModel.uiState.value.loadedBuildingOverrides.size)
    }

    @Test
    fun movingLoadedBuilding_keepsAutomaticIdentityAndAdjustedFootprint() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        val building = viewModel.uiState.value.visibleLoadedBuildings.single()
        val selection = DrawnObjectSelection(
            id = AutomaticBuildingMatcher.identity(building).selectionId,
            type = DrawnObjectType.BUILDING,
            source = SceneObjectSource.AUTOMATIC
        )

        viewModel.selectDrawing(selection)
        viewModel.startMoving()
        viewModel.moveSelectedObject(longitudeDelta = 0.001, latitudeDelta = -0.002)

        val moved = viewModel.uiState.value.visibleLoadedBuildings.single()
        assertEquals(selection.id, AutomaticBuildingMatcher.identity(moved).selectionId)
        assertEquals(153.001, moved.polygon.rings.single().first().longitude, 0.0)
        assertEquals(-27.002, moved.polygon.rings.single().first().latitude, 0.0)

        viewModel.cancelMoving()
        assertEquals(
            153.0,
            viewModel.uiState.value.visibleLoadedBuildings.single()
                .polygon.rings.single().first().longitude,
            0.0
        )
        assertEquals(null, viewModel.uiState.value.selectedDrawing)
    }

    @Test
    fun movingManualWall_translatesAllPointsAndCancelRestoresIt() {
        val viewModel = createViewModel()
        viewModel.selectDrawMode(DrawMode.WALL)
        listOf(
            GeoPoint(153.0, -28.0),
            GeoPoint(153.0001, -28.0001)
        ).forEach(viewModel::addVertex)
        viewModel.finishWall(WALL_FINISH_POINT)
        viewModel.commitPendingDrawing(heightMeters = 2.5)
        val wall = viewModel.uiState.value.drawnWalls.single()
        assertEquals(WALL_FINISH_POINT, wall.points.last())
        val selection = DrawnObjectSelection(
            id = wall.id,
            type = DrawnObjectType.WALL,
            source = SceneObjectSource.MANUAL
        )

        viewModel.selectDrawing(selection)
        viewModel.startMoving()
        viewModel.moveSelectedObject(longitudeDelta = 0.01, latitudeDelta = 0.02)

        assertEquals(
            153.01,
            viewModel.uiState.value.drawnWalls.single().points.first().longitude,
            0.0
        )
        assertEquals(
            -27.98,
            viewModel.uiState.value.drawnWalls.single().points.first().latitude,
            0.0
        )
        viewModel.cancelMoving()
        assertEquals(
            153.0,
            viewModel.uiState.value.drawnWalls.single().points.first().longitude,
            0.0
        )
    }

    @Test
    fun resettingLoadedBuildingHeight_removesOverride() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        val building = viewModel.uiState.value.visibleLoadedBuildings.single()
        val selection = DrawnObjectSelection(
            id = AutomaticBuildingMatcher.identity(building).selectionId,
            type = DrawnObjectType.BUILDING,
            source = SceneObjectSource.AUTOMATIC
        )
        viewModel.selectDrawing(selection)
        viewModel.updateSelectedDrawing(heightMeters = 12.5)
        viewModel.selectDrawing(selection)

        viewModel.updateSelectedDrawing(heightMeters = building.heightMeters)

        assertTrue(viewModel.uiState.value.loadedBuildingOverrides.isEmpty())
        assertEquals(
            building.heightMeters,
            viewModel.uiState.value.visibleLoadedBuildings.single().heightMeters,
            0.0
        )
    }

    @Test
    fun editedLoadedBuilding_migratesOverrideWhenSourceIdChanges() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        val building = viewModel.uiState.value.visibleLoadedBuildings.single()
        viewModel.selectDrawing(
            DrawnObjectSelection(
                AutomaticBuildingMatcher.identity(building).selectionId,
                DrawnObjectType.BUILDING,
                SceneObjectSource.AUTOMATIC
            )
        )
        viewModel.updateSelectedDrawing(heightMeters = 9.5)

        viewModel.onBuildingsLoaded(
            listOf(testBuilding().copy(id = "replacement-id", heightMeters = 40.0)),
            TEST_LOCATION
        )

        assertEquals(1, viewModel.uiState.value.loadedBuildings.size)
        assertEquals(9.5, viewModel.uiState.value.visibleLoadedBuildings.single().heightMeters, 0.0)
    }

    @Test
    fun deletedLoadedBuilding_staysHiddenAfterReloadAndCanBeRestored() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        val building = viewModel.uiState.value.visibleLoadedBuildings.single()
        viewModel.selectDrawing(
            DrawnObjectSelection(
                AutomaticBuildingMatcher.identity(building).selectionId,
                DrawnObjectType.BUILDING,
                SceneObjectSource.AUTOMATIC
            )
        )

        assertTrue(viewModel.deleteSelectedDrawing())
        viewModel.onBuildingsLoaded(listOf(testBuilding().copy(heightMeters = 50.0)), TEST_LOCATION)
        assertTrue(viewModel.uiState.value.visibleLoadedBuildings.isEmpty())

        viewModel.restoreLastDeletedObject()
        assertEquals(1, viewModel.uiState.value.visibleLoadedBuildings.size)
    }

    @Test
    fun clearSceneUndo_restoresFullSceneSnapshot() {
        val viewModel = createViewModel()
        viewModel.onBuildingsLoaded(listOf(testBuilding()), TEST_LOCATION)
        viewModel.selectDrawMode(DrawMode.TREE)
        viewModel.startTree(TEST_LOCATION)
        viewModel.commitPendingDrawing(heightMeters = 8.0, radiusMeters = 2.5)

        viewModel.clearScene()
        viewModel.restoreClearedScene()

        assertEquals(1, viewModel.uiState.value.visibleLoadedBuildings.size)
        assertEquals(1, viewModel.uiState.value.drawnTrees.size)
    }

    @Test
    fun successfulProjectLoads_incrementSceneResetRevision() = runTest(dispatcher) {
        val project = testProjectSnapshot()
        val viewModel = createViewModel(
            projectRepository = object : ProjectRepository {
                override fun listProjects() =
                    emptyList<com.gooludou.shadowplanner.project.ProjectSummary>()
                override fun loadProject(id: String) = project
                override fun saveProject(project: ProjectSnapshot) = Unit
                override fun deleteProject(id: String) = Unit
            }
        )

        viewModel.loadProject(project.id)
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.projectLoadRevision)

        viewModel.loadProject(project.id)
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.projectLoadRevision)
    }

    @Test
    fun saveProjectAsNew_createsANewActiveProject() = runTest(dispatcher) {
        val savedProjects = mutableListOf<com.gooludou.shadowplanner.project.ProjectSnapshot>()
        val viewModel = createViewModel(
            projectRepository = object : ProjectRepository {
                override fun listProjects() =
                    emptyList<com.gooludou.shadowplanner.project.ProjectSummary>()

                override fun loadProject(id: String) = error("Not used in this test")

                override fun saveProject(
                    project: com.gooludou.shadowplanner.project.ProjectSnapshot
                ) {
                    savedProjects += project
                }

                override fun deleteProject(id: String) = Unit
            }
        )

        viewModel.saveProject("Original")
        advanceUntilIdle()
        val originalProjectId = viewModel.uiState.value.activeProjectId

        viewModel.saveProjectAsNew("Copy")
        advanceUntilIdle()

        assertEquals(2, savedProjects.size)
        assertNotEquals(originalProjectId, viewModel.uiState.value.activeProjectId)
        assertEquals("Copy", viewModel.uiState.value.activeProjectName)
    }

    @Test
    fun currentLocationReceived_resolvesAndStoresLocationLabel() = runTest(dispatcher) {
        var resolveCalls = 0
        val viewModel = createViewModel(
            currentLocationResolver = object : CurrentLocationResolver {
                override suspend fun resolve(location: GeoPoint): Result<LocationSearchResult> {
                    resolveCalls += 1
                    return Result.success(
                        LocationSearchResult(
                            id = "current-location",
                            name = "Brisbane",
                            address = "Brisbane QLD, Australia",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                }
            }
        )

        viewModel.onCurrentLocationReceived(TEST_LOCATION, "Current location")
        viewModel.onCurrentLocationReceived(TEST_LOCATION, "Current location")
        advanceUntilIdle()

        assertEquals(1, resolveCalls)
        assertEquals(TEST_LOCATION, viewModel.uiState.value.calculationLocation)
        assertEquals("Brisbane QLD, Australia", viewModel.uiState.value.selectedLocationLabel)
    }

    private fun testBuilding(): Building {
        val ring =
            listOf(
                GeoPoint(153.0, -27.0),
                GeoPoint(153.0001, -27.0),
                GeoPoint(153.0001, -27.0001),
                GeoPoint(153.0, -27.0001),
                GeoPoint(153.0, -27.0)
            )
        return Building(
            id = "building",
            polygon = GeoPolygon(listOf(ring)),
            heightMeters = 20.0,
            minHeightMeters = 0.0
        )
    }

    private fun buildingVertices() = listOf(
        GeoPoint(153.0, -28.0),
        GeoPoint(153.0001, -28.0),
        GeoPoint(153.0001, -28.0001),
        GeoPoint(153.0, -28.0001)
    )

    private fun testProjectSnapshot() = ProjectSnapshot(
        id = "project-1",
        name = "Loaded project",
        createdAt = 1L,
        updatedAt = 2L,
        selectedEpochMillis = DEFAULT_TIME.toEpochMilli(),
        displayTimeZoneId = "Australia/Brisbane",
        calculationLocation = TEST_LOCATION,
        selectedLocationLabel = "Brisbane",
        viewport = null,
        drawnBuildings = emptyList(),
        drawnWalls = emptyList(),
        drawnTrees = emptyList(),
        loadedBuildings = emptyList(),
        loadedBuildingOverrides = emptyMap(),
        suppressedLoadedBuildings = emptyMap()
    )

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        projectRepository: ProjectRepository = object : ProjectRepository {
            override fun listProjects() =
                emptyList<com.gooludou.shadowplanner.project.ProjectSummary>()
            override fun loadProject(id: String) = error("Not used in this test")
            override fun saveProject(project: com.gooludou.shadowplanner.project.ProjectSnapshot) =
                Unit
            override fun deleteProject(id: String) = Unit
        },
        currentLocationResolver: CurrentLocationResolver = object : CurrentLocationResolver {
            override suspend fun resolve(location: GeoPoint): Result<LocationSearchResult> =
                Result.failure(IllegalStateException("Not used in this test"))
        }
    ) = ShadowMapViewModel(
        savedStateHandle = savedStateHandle,
        shadowCalculator = BuildingShadowCalculator(),
        userObjectShadowCalculator = UserObjectShadowCalculator(),
        solarPositionCalculator = SolarPositionCalculator(),
        clock = Clock.fixed(DEFAULT_TIME, ZoneOffset.UTC),
        systemZoneId = ZoneId.of("Australia/Brisbane"),
        projectRepository = projectRepository,
        currentLocationResolver = currentLocationResolver,
        computationDispatcher = dispatcher
    )

    private companion object {
        val DEFAULT_TIME: Instant = Instant.parse("2026-07-14T02:00:00Z")
        val TEST_LOCATION = GeoPoint(153.0251, -27.4698)
        val BUILDING_FINISH_POINT = GeoPoint(153.00005, -28.000075)
        val WALL_FINISH_POINT = GeoPoint(153.0002, -28.0002)
    }
}
