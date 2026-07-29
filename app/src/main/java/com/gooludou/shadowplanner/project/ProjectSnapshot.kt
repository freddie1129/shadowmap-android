package com.gooludou.shadowplanner.project

import com.gooludou.shadowplanner.core.model.AutomaticBuildingIdentity
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.model.LoadedBuildingOverride
import com.gooludou.shadowplanner.core.model.ShadowAppearance

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
    val drawnBuildings: List<Building>,
    val drawnWalls: List<DrawnWall>,
    val drawnTrees: List<DrawnTree>,
    val loadedBuildings: List<Building>,
    val loadedBuildingOverrides: Map<AutomaticBuildingIdentity, LoadedBuildingOverride>,
    val suppressedLoadedBuildings: Map<AutomaticBuildingIdentity, GeoPolygon>,
    val shadowAppearance: ShadowAppearance = ShadowAppearance.DEFAULT
)

data class ProjectSummary(
    val id: String,
    val name: String,
    val locationLabel: String?,
    val updatedAt: Long,
    val fileName: String
)

const val CURRENT_SCHEMA_VERSION = 1
