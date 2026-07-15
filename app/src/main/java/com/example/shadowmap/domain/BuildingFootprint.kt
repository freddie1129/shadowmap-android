package com.example.shadowmap.domain

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

data class BuildingFootprint(
    val id: String?,
    val polygon: GeoPolygon,
    val heightMeters: Double,
    val minHeightMeters: Double,
    val automaticIdentity: AutomaticBuildingIdentity? = null
)

data class LoadedBuildingOverride(
    val heightMeters: Double,
    val referencePolygon: GeoPolygon
)

const val DEFAULT_BUILDING_HEIGHT_METERS = 10.0
