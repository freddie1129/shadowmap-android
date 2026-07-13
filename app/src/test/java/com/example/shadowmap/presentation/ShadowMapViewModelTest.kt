package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.BuildingShadowCalculator
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.SolarPositionCalculator
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

    private fun testBuilding(): BuildingFootprint {
        val ring =
            listOf(
                GeoPoint(153.0, -27.0),
                GeoPoint(153.0001, -27.0),
                GeoPoint(153.0001, -27.0001),
                GeoPoint(153.0, -27.0001),
                GeoPoint(153.0, -27.0)
            )
        return BuildingFootprint(
            id = "building",
            polygon = GeoPolygon(listOf(ring)),
            heightMeters = 20.0,
            minHeightMeters = 0.0
        )
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        ShadowMapViewModel(
            savedStateHandle = savedStateHandle,
            shadowCalculator = BuildingShadowCalculator(),
            solarPositionCalculator = SolarPositionCalculator(),
            clock = Clock.fixed(DEFAULT_TIME, ZoneOffset.UTC),
            systemZoneId = ZoneId.of("Australia/Brisbane"),
            computationDispatcher = dispatcher
        )

    private companion object {
        val DEFAULT_TIME: Instant = Instant.parse("2026-07-14T02:00:00Z")
        val TEST_LOCATION = GeoPoint(153.0251, -27.4698)
    }
}
