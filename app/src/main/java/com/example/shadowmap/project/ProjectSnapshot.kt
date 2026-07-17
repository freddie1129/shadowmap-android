package com.example.shadowmap.project

import com.example.shadowmap.domain.AutomaticBuildingIdentity
import com.example.shadowmap.domain.BuildingFootprint
import com.example.shadowmap.domain.DrawnBuilding
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.GeoPoint
import com.example.shadowmap.domain.GeoPolygon
import com.example.shadowmap.domain.LoadedBuildingOverride

data class ProjectViewport(
    val center: GeoPoint,
    val zoom: Double,
    val bearing: Double,
    val pitch: Double,
    val boundary: GeoPolygon
)

data class ProjectSnapshot(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val selectedEpochMillis: Long,
    val displayTimeZoneId: String,
    val calculationLocation: GeoPoint?,
    val selectedLocationLabel: String?,
    val viewport: ProjectViewport?,
    val drawnBuildings: List<DrawnBuilding>,
    val drawnWalls: List<DrawnWall>,
    val drawnTrees: List<DrawnTree>,
    val loadedBuildings: List<BuildingFootprint>,
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>,
    val suppressedLoadedBuildings: Map<AutomaticBuildingIdentity, GeoPolygon>
)

data class ProjectSummary(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val fileName: String
)

const val CURRENT_SCHEMA_VERSION = 1
