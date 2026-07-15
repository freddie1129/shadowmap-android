package com.example.shadowmap.domain

import kotlin.math.min
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

object SceneBuildingMerger {
    fun automaticKey(building: BuildingFootprint): String {
        val identity = AutomaticBuildingMatcher.identity(building)
        return identity.selectionId
    }

    fun automaticKeysCoveredByManualBuildings(
        automaticBuildings: List<BuildingFootprint>,
        manualBuildings: List<DrawnBuilding>
    ): Set<String> {
        if (automaticBuildings.isEmpty() || manualBuildings.isEmpty()) return emptySet()
        val manualPolygons = manualBuildings.mapNotNull { it.polygon.toJtsPolygon() }
        return automaticBuildings.mapNotNullTo(mutableSetOf()) { automatic ->
            val polygon = automatic.polygon.toJtsPolygon() ?: return@mapNotNullTo null
            if (manualPolygons.any { manual -> polygon.stronglyOverlaps(manual) }) {
                automaticKey(automatic)
            } else {
                null
            }
        }
    }

    private fun Polygon.stronglyOverlaps(other: Polygon): Boolean {
        val smallerArea = min(area, other.area)
        return envelopeInternal.intersects(other.envelopeInternal) &&
            smallerArea > 0.0 &&
            runCatching { intersection(other).area / smallerArea >= STRONG_OVERLAP_RATIO }
                .getOrDefault(false)
    }

    private fun GeoPolygon.toJtsPolygon(): Polygon? {
        val outerRing = rings.firstOrNull()?.takeIf { it.size >= 3 } ?: return null
        val closedRing = if (outerRing.first() == outerRing.last()) outerRing else outerRing + outerRing.first()
        return runCatching {
            GEOMETRY_FACTORY.createPolygon(
                closedRing.map { Coordinate(it.longitude, it.latitude) }.toTypedArray()
            )
        }.getOrNull()
    }

    private const val STRONG_OVERLAP_RATIO = 0.8
    private val GEOMETRY_FACTORY = GeometryFactory()
}
