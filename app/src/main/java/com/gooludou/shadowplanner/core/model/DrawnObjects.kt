package com.gooludou.shadowplanner.core.model

import java.util.UUID

enum class DrawMode {
    BUILDING,
    WALL,
    TREE
}

data class DrawnWall(
    val id: String = UUID.randomUUID().toString(),
    val points: List<GeoPoint>,
    val heightMeters: Double = DEFAULT_DRAWN_WALL_HEIGHT_METERS
)

data class DrawnTree(
    val id: String = UUID.randomUUID().toString(),
    val center: GeoPoint,
    val heightMeters: Double = DEFAULT_DRAWN_TREE_HEIGHT_METERS,
    val radiusMeters: Double = DEFAULT_DRAWN_TREE_RADIUS_METERS,
    val crownShape: TreeCrownShape = TreeCrownShape.CONE
)

enum class TreeCrownShape {
    CYLINDER,
    CONE
}

sealed interface PendingDrawing {
    data class Building(val vertices: List<GeoPoint>) : PendingDrawing

    data class Wall(val points: List<GeoPoint>) : PendingDrawing

    data class Tree(val center: GeoPoint) : PendingDrawing
}

enum class DrawnObjectType {
    BUILDING,
    WALL,
    TREE
}

enum class SceneObjectSource {
    MANUAL,
    AUTOMATIC
}

data class DrawnObjectSelection(
    val id: String,
    val type: DrawnObjectType,
    val source: SceneObjectSource = SceneObjectSource.MANUAL
)

sealed interface SceneObjectGeometry {
    data class Building(val polygon: GeoPolygon) : SceneObjectGeometry

    data class Wall(val points: List<GeoPoint>) : SceneObjectGeometry

    data class Tree(val center: GeoPoint) : SceneObjectGeometry
}

data class MoveSession(
    val selection: DrawnObjectSelection,
    val original: SceneObjectGeometry,
    val current: SceneObjectGeometry
)

fun GeoPoint.translatedBy(longitudeDelta: Double, latitudeDelta: Double): GeoPoint = copy(
    longitude = longitude + longitudeDelta,
    latitude = latitude + latitudeDelta
)

fun GeoPolygon.translatedBy(longitudeDelta: Double, latitudeDelta: Double): GeoPolygon = copy(
    rings = rings.map { ring ->
        ring.map { it.translatedBy(longitudeDelta, latitudeDelta) }
    }
)

const val DEFAULT_DRAWN_BUILDING_HEIGHT_METERS = 6.0
const val DEFAULT_DRAWN_WALL_HEIGHT_METERS = 2.5
const val DEFAULT_DRAWN_TREE_HEIGHT_METERS = 8.0
const val DEFAULT_DRAWN_TREE_RADIUS_METERS = 5.0
