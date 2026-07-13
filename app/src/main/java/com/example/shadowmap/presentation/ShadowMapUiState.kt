package com.example.shadowmap.presentation

import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.SolarPosition

data class ShadowMapUiState(
    val selectedEpochMillis: Long,
    val displayTimeZoneId: String,
    val calculationLocation: GeoPoint? = null,
    val solarPosition: SolarPosition? = null,
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
