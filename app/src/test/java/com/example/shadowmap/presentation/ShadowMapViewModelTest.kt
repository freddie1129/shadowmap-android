package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
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
    fun angleChanges_updateStateAndSavedState() {
        val savedState = SavedStateHandle()
        val viewModel = ShadowMapViewModel(savedState, dispatcher)

        viewModel.onAzimuthChanged(240f)
        viewModel.onZenithChanged(35f)

        assertEquals(240f, viewModel.uiState.value.azimuth)
        assertEquals(35f, viewModel.uiState.value.zenith)
        assertEquals(240f, savedState.get<Float>("azimuth"))
        assertEquals(35f, savedState.get<Float>("zenith"))
    }

    @Test
    fun buildingLoadFailure_exposesRetryableErrorState() {
        val viewModel = ShadowMapViewModel(SavedStateHandle(), dispatcher)

        viewModel.onBuildingLoadStarted()
        viewModel.onBuildingLoadFailed(IllegalStateException("Map failed"))

        assertEquals(BuildingLoadState.Error("Map failed"), viewModel.uiState.value.buildingLoadState)
    }

    @Test
    fun loadedBuildings_generateShadows() = runTest(dispatcher) {
        val viewModel = ShadowMapViewModel(SavedStateHandle(), dispatcher)

        viewModel.onBuildingsLoaded(listOf(testBuilding()))
        advanceUntilIdle()

        assertEquals(BuildingLoadState.Loaded, viewModel.uiState.value.buildingLoadState)
        assertTrue(viewModel.uiState.value.shadows.isNotEmpty())
    }

    private fun testBuilding(): BuildingFootprint {
        val ring = listOf(
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
}
