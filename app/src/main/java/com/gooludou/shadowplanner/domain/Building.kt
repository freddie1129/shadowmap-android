package com.gooludou.shadowplanner.domain

data class GeoPoint(val longitude: Double, val latitude: Double)

data class GeoPolygon(val rings: List<List<GeoPoint>>)

data class AutomaticBuildingIdentity(
    val featureId: String?,
    val featureNamespace: String?,
    val geometryFingerprint: String
) {
    val selectionId: String
        get() = listOf(featureNamespace.orEmpty(), featureId.orEmpty(), geometryFingerprint)
            .joinToString(IDENTITY_SEPARATOR)

    companion object {
        private const val IDENTITY_SEPARATOR = "|"
    }
}

enum class BuildingSource {
    AUTOMATIC,
    MANUAL
}

data class Building(
    val id: String?,
    val polygon: GeoPolygon,
    val heightMeters: Double,
    val minHeightMeters: Double = 0.0,
    val source: BuildingSource = BuildingSource.AUTOMATIC,
    val automaticIdentity: AutomaticBuildingIdentity? = null
)

data class LoadedBuildingOverride(val heightMeters: Double, val referencePolygon: GeoPolygon)

const val DEFAULT_BUILDING_HEIGHT_METERS = 10.0
