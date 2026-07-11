package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.BuildingShadowCalculator
import com.example.shadowmap.di.DefaultDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShadowMapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val shadowCalculator: BuildingShadowCalculator,
    @param:DefaultDispatcher
    private val computationDispatcher: CoroutineDispatcher
) : ViewModel() {
    private var shadowJob: Job? = null

    private val _uiState = MutableStateFlow(
        ShadowMapUiState(
            azimuth = savedStateHandle[AZIMUTH_KEY] ?: DEFAULT_AZIMUTH,
            zenith = savedStateHandle[ZENITH_KEY] ?: DEFAULT_ZENITH
        )
    )
    val uiState: StateFlow<ShadowMapUiState> = _uiState.asStateFlow()

    fun onAzimuthChanged(value: Float) {
        savedStateHandle[AZIMUTH_KEY] = value
        _uiState.value = _uiState.value.copy(azimuth = value)
        regenerateShadows()
    }

    fun onZenithChanged(value: Float) {
        savedStateHandle[ZENITH_KEY] = value
        _uiState.value = _uiState.value.copy(zenith = value)
        regenerateShadows()
    }

    fun onBuildingLoadStarted() {
        _uiState.value = _uiState.value.copy(buildingLoadState = BuildingLoadState.Loading)
    }

    fun onBuildingsLoaded(buildings: List<BuildingFootprint>) {
        _uiState.value = _uiState.value.copy(
            buildings = buildings,
            shadows = emptyList(),
            buildingLoadState = BuildingLoadState.Loaded
        )
        regenerateShadows()
    }

    fun onBuildingLoadFailed(throwable: Throwable) {
        _uiState.value = _uiState.value.copy(
            buildingLoadState = BuildingLoadState.Error(
                throwable.message ?: "Unable to load buildings"
            )
        )
    }

    private fun regenerateShadows() {
        shadowJob?.cancel()
        val state = _uiState.value
        if (state.buildings.isEmpty()) {
            _uiState.value = state.copy(shadows = emptyList())
            return
        }

        shadowJob = viewModelScope.launch(computationDispatcher) {
            delay(SHADOW_DEBOUNCE_MILLIS)
            val shadows = shadowCalculator.calculate(
                buildings = state.buildings,
                azimuthDegrees = state.azimuth.toDouble(),
                zenithDegrees = state.zenith.toDouble()
            )
            ensureActive()
            _uiState.value = _uiState.value.copy(shadows = shadows)
        }
    }

    companion object {
        private const val AZIMUTH_KEY = "azimuth"
        private const val ZENITH_KEY = "zenith"
        private const val SHADOW_DEBOUNCE_MILLIS = 50L
    }
}
