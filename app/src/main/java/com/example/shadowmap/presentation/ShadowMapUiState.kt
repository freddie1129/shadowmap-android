package com.example.shadowmap.presentation

import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.GeoPolygon

data class ShadowMapUiState(
    val azimuth: Float = DEFAULT_AZIMUTH,
    val zenith: Float = DEFAULT_ZENITH,
    val buildings: List<BuildingFootprint> = emptyList(),
    val shadows: List<GeoPolygon> = emptyList(),
    val buildingLoadState: BuildingLoadState = BuildingLoadState.Idle
)

sealed interface BuildingLoadState {
    data object Idle : BuildingLoadState

    data object Loading : BuildingLoadState

    data object Loaded : BuildingLoadState

    data class Error(val message: String) : BuildingLoadState
}

const val DEFAULT_AZIMUTH = 180f
const val DEFAULT_ZENITH = 20f
