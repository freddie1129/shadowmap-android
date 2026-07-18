package com.example.shadowmap.domain

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.min
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon

object AutomaticBuildingMatcher {
    fun identity(building: Building): AutomaticBuildingIdentity =
        building.automaticIdentity ?: identity(
            featureId = building.id,
            featureNamespace = null,
            polygon = building.polygon
        )

    fun identity(
        featureId: String?,
        featureNamespace: String?,
        polygon: GeoPolygon
    ): AutomaticBuildingIdentity = AutomaticBuildingIdentity(
        featureId = featureId,
        featureNamespace = featureNamespace,
        geometryFingerprint = geometryFingerprint(polygon)
    )

    fun findMatch(
        building: Building,
        candidates: Collection<Pair<AutomaticBuildingIdentity, GeoPolygon>>
    ): AutomaticBuildingIdentity? {
        val identity = identity(building)
        val sameSourceCandidates = candidates.filter { (candidate, _) ->
            identity.hasSameSourceFeature(candidate)
        }
        return candidates.firstOrNull { (candidate, _) -> candidate == identity }?.first
            ?: sameSourceCandidates.firstOrNull { (_, polygon) ->
                building.polygon.stronglyOverlaps(polygon)
            }?.first
            ?: sameSourceCandidates.singleOrNull()?.first
            ?: candidates.firstOrNull { (candidate, _) ->
            candidate.geometryFingerprint == identity.geometryFingerprint
            }?.first
            ?: candidates.firstOrNull { (_, polygon) ->
                building.polygon.stronglyOverlaps(polygon)
            }?.first
    }

    fun geometryFingerprint(polygon: GeoPolygon): String {
        val canonical = polygon.rings.mapNotNull(::canonicalRing).sorted().joinToString(";")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun AutomaticBuildingIdentity.hasSameSourceFeature(
        other: AutomaticBuildingIdentity
    ): Boolean = featureId != null &&
        featureId == other.featureId &&
        featureNamespace == other.featureNamespace

    private fun canonicalRing(ring: List<GeoPoint>): String? {
        val points = ring.dropLastWhileClosingPoint().map { point ->
            String.format(Locale.US, "%.7f,%.7f", point.longitude, point.latitude)
        }
        if (points.size < 3) return null
        val forward = points.rotations().minOrNull().orEmpty()
        val reverse = points.reversed().rotations().minOrNull().orEmpty()
        return minOf(forward, reverse)
    }

    private fun List<GeoPoint>.dropLastWhileClosingPoint(): List<GeoPoint> =
        if (size > 1 && first() == last()) dropLast(1) else this

    private fun List<String>.rotations(): List<String> = indices.map { start ->
        (indices.map { offset -> this[(start + offset) % size] }).joinToString("|")
    }

    private fun GeoPolygon.stronglyOverlaps(other: GeoPolygon): Boolean {
        val first = toJtsPolygon()
        val second = other.toJtsPolygon()
        return if (first == null || second == null) {
            false
        } else {
            val smallerArea = min(first.area, second.area)
            first.envelopeInternal.intersects(second.envelopeInternal) &&
                smallerArea > 0.0 &&
                runCatching { first.intersection(second).area / smallerArea >= STRONG_OVERLAP_RATIO }
                    .getOrDefault(false)
        }
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
