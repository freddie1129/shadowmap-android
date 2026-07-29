package com.gooludou.shadowplanner.feature.shadowmap

import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.project.ProjectViewport
import com.gooludou.shadowplanner.renderer.filament.SceneViewport
import com.gooludou.shadowplanner.renderer.mapbox.BuildingLoadArea
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate

internal fun MapView.toSceneViewport(): SceneViewport? {
    if (width <= 0 || height <= 0) return null
    val center = mapboxMap.coordinateForPixel(ScreenCoordinate(width / 2.0, height / 2.0))
    val topLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, 0.0))
    val topRight = mapboxMap.coordinateForPixel(ScreenCoordinate(width.toDouble(), 0.0))
    val bottomLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, height.toDouble()))
    return SceneViewport.fromScreenCoordinates(
        center.longitude(), center.latitude(),
        topLeft.longitude(), topLeft.latitude(),
        topRight.longitude(), topRight.latitude(),
        bottomLeft.longitude(), bottomLeft.latitude()
    )
}

internal fun MapView.toProjectViewport(): ProjectViewport? {
    if (width <= 0 || height <= 0) return null
    val camera = mapboxMap.cameraState
    val center = camera.center
    val topLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, 0.0))
    val topRight = mapboxMap.coordinateForPixel(ScreenCoordinate(width.toDouble(), 0.0))
    val bottomRight = mapboxMap.coordinateForPixel(
        ScreenCoordinate(width.toDouble(), height.toDouble())
    )
    val bottomLeft = mapboxMap.coordinateForPixel(ScreenCoordinate(0.0, height.toDouble()))
    return ProjectViewport(
        center = GeoPoint(center.longitude(), center.latitude()),
        zoom = camera.zoom,
        bearing = camera.bearing,
        pitch = camera.pitch,
        boundary = GeoPolygon(
            listOf(
                listOf(
                    GeoPoint(topLeft.longitude(), topLeft.latitude()),
                    GeoPoint(topRight.longitude(), topRight.latitude()),
                    GeoPoint(bottomRight.longitude(), bottomRight.latitude()),
                    GeoPoint(bottomLeft.longitude(), bottomLeft.latitude()),
                    GeoPoint(topLeft.longitude(), topLeft.latitude())
                )
            )
        )
    )
}

internal fun MapView.isFarEnoughFrom(
    first: GeoPoint,
    second: GeoPoint,
    thresholdPixels: Float
): Boolean {
    val firstPixel = mapboxMap.pixelForCoordinate(Point.fromLngLat(first.longitude, first.latitude))
    val secondPixel = mapboxMap.pixelForCoordinate(
        Point.fromLngLat(second.longitude, second.latitude)
    )
    val dx = firstPixel.x - secondPixel.x
    val dy = firstPixel.y - secondPixel.y
    return dx * dx + dy * dy >= thresholdPixels * thresholdPixels
}

internal fun MapView.toBuildingLoadArea(): BuildingLoadArea? = toSceneViewport()?.let { viewport ->
    BuildingLoadArea(viewport.widthMeters, viewport.heightMeters)
}
