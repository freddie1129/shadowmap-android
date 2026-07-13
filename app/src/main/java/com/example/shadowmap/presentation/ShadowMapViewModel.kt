package com.example.shadowmap.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shadowmap.di.DefaultDispatcher
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.BuildingShadowCalculator
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.SolarPositionCalculator
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
class ShadowMapViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val shadowCalculator: BuildingShadowCalculator,
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

    fun onBuildingsLoaded(buildings: List<BuildingFootprint>, location: GeoPoint) {
        savedStateHandle[LOCATION_LATITUDE_KEY] = location.latitude
        savedStateHandle[LOCATION_LONGITUDE_KEY] = location.longitude
        _uiState.value =
            _uiState.value.copy(
                calculationLocation = location,
                buildings = buildings,
                shadows = emptyList(),
                buildingLoadState = BuildingLoadState.Loaded
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
        if (state.buildings.isEmpty() || !solarPosition.isAboveHorizon) {
            _uiState.value = _uiState.value.copy(shadows = emptyList())
            return
        }

        shadowJob =
            viewModelScope.launch(computationDispatcher) {
                delay(SHADOW_DEBOUNCE_MILLIS)
                val shadows =
                    shadowCalculator.calculate(
                        buildings = state.buildings,
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

    companion object {
        private const val SELECTED_TIME_KEY = "selected_time"
        private const val TIME_ZONE_KEY = "time_zone"
        private const val LOCATION_LATITUDE_KEY = "location_latitude"
        private const val LOCATION_LONGITUDE_KEY = "location_longitude"
        private const val TIME_STEP_MILLIS = 5 * 60 * 1000L
        private const val SHADOW_DEBOUNCE_MILLIS = 50L
    }
}
