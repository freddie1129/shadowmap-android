package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.renderer.filament.SceneViewport
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun Building.toMapboxFeature(): Feature? {
    if (polygon.rings.firstOrNull().orEmpty().distinct().size < 3) return null
    return Feature.fromGeometry(polygon.toMapboxPolygon()).apply {
        addNumberProperty(Scene3DFeatureProperties.BASE_HEIGHT, minHeightMeters.coerceAtLeast(0.0))
        addNumberProperty(
            Scene3DFeatureProperties.HEIGHT,
            heightMeters.coerceAtLeast(minHeightMeters)
        )
    }
}

internal fun DrawnWall.toMapboxFeature(): Feature? {
    if (points.size < 2) return null
    return Feature.fromGeometry(LineString.fromLngLats(points.map(GeoPoint::toMapboxPoint))).apply {
        addNumberProperty(Scene3DFeatureProperties.HEIGHT, heightMeters.coerceAtLeast(0.0))
    }
}

internal fun DrawnTree.toTrunkFeature(): Feature = Feature.fromGeometry(
    circlePolygon(
        center,
        (radiusMeters * Scene3DTreeGeometry.TRUNK_RADIUS_RATIO)
            .coerceAtLeast(Scene3DTreeGeometry.MIN_TRUNK_RADIUS_METERS)
    )
).apply {
    addNumberProperty(
        Scene3DFeatureProperties.HEIGHT,
        heightMeters.coerceAtLeast(Scene3DTreeGeometry.MIN_TREE_HEIGHT_METERS) *
            Scene3DTreeGeometry.TRUNK_HEIGHT_RATIO
    )
}

internal fun DrawnTree.toCanopyFeature(): Feature = Feature.fromGeometry(
    circlePolygon(center, radiusMeters.coerceAtLeast(Scene3DTreeGeometry.MIN_CANOPY_RADIUS_METERS))
).apply {
    val height = heightMeters.coerceAtLeast(Scene3DTreeGeometry.MIN_TREE_HEIGHT_METERS)
    addNumberProperty(
        Scene3DFeatureProperties.BASE_HEIGHT,
        height * Scene3DTreeGeometry.CANOPY_BASE_RATIO
    )
    addNumberProperty(Scene3DFeatureProperties.HEIGHT, height)
}

private fun GeoPolygon.toMapboxPolygon(): Polygon = Polygon.fromLngLats(
    rings.map { ring ->
        val closedRing = when {
            ring.isEmpty() -> emptyList()
            ring.first() == ring.last() -> ring
            else -> ring + ring.first()
        }
        closedRing.map(GeoPoint::toMapboxPoint)
    }
)

private fun circlePolygon(center: GeoPoint, radiusMeters: Double): Polygon {
    val latitudeRadians = center.latitude * PI / 180.0
    val latitudeDegreesPerMeter = 180.0 / (PI * Scene3DGeography.EARTH_RADIUS_METERS)
    val longitudeDegreesPerMeter = latitudeDegreesPerMeter / cos(latitudeRadians)
    val ring = (0..Scene3DTreeGeometry.SEGMENTS).map { index ->
        val angle = index.toDouble() / Scene3DTreeGeometry.SEGMENTS * 2.0 * PI
        Point.fromLngLat(
            center.longitude + sin(angle) * radiusMeters * longitudeDegreesPerMeter,
            center.latitude + cos(angle) * radiusMeters * latitudeDegreesPerMeter
        )
    }
    return Polygon.fromLngLats(listOf(ring))
}

private fun GeoPoint.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

internal fun MapView.currentScene3DViewport(
    fallback: MapboxScene3DViewport
): MapboxScene3DViewport? {
    if (width <= 0 || height <= 0) return null
    val camera = mapboxMap.cameraState
    val center = camera.center
    val topDownBounds = mapboxMap.coordinateBoundsForCamera(
        CameraOptions.Builder()
            .center(center)
            .zoom(camera.zoom)
            .bearing(0.0)
            .pitch(0.0)
            .build()
    )
    val topLeft = topDownBounds.northwest()
    val topRight = topDownBounds.northeast
    val bottomLeft = topDownBounds.southwest
    val boundary = SceneViewport.fromScreenCoordinates(
        centerLongitude = center.longitude(),
        centerLatitude = center.latitude(),
        topLeftLongitude = topLeft.longitude(),
        topLeftLatitude = topLeft.latitude(),
        topRightLongitude = topRight.longitude(),
        topRightLatitude = topRight.latitude(),
        bottomLeftLongitude = bottomLeft.longitude(),
        bottomLeftLatitude = bottomLeft.latitude()
    )
    return fallback.copy(
        center = GeoPoint(center.longitude(), center.latitude()),
        zoom = camera.zoom,
        bearing = camera.bearing,
        widthMeters = boundary.widthMeters.toDouble(),
        heightMeters = boundary.heightMeters.toDouble(),
        widthPixels = width,
        heightPixels = height
    )
}
