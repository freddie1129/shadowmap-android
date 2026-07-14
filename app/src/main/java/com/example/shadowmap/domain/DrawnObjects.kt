package com.example.shadowmap.domain

import java.util.UUID

enum class DrawMode {
    BUILDING,
    WALL,
    TREE
}

data class DrawnBuilding(
    val id: String = UUID.randomUUID().toString(),
    val polygon: GeoPolygon,
    val heightMeters: Double = DEFAULT_DRAWN_BUILDING_HEIGHT_METERS
)

data class DrawnWall(
    val id: String = UUID.randomUUID().toString(),
    val points: List<GeoPoint>,
    val heightMeters: Double = DEFAULT_DRAWN_WALL_HEIGHT_METERS
)

data class DrawnTree(
    val id: String = UUID.randomUUID().toString(),
    val center: GeoPoint,
    val heightMeters: Double = DEFAULT_DRAWN_TREE_HEIGHT_METERS,
    val radiusMeters: Double = DEFAULT_DRAWN_TREE_RADIUS_METERS
)

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

data class DrawnObjectSelection(val id: String, val type: DrawnObjectType)

const val DEFAULT_DRAWN_BUILDING_HEIGHT_METERS = 6.0
const val DEFAULT_DRAWN_WALL_HEIGHT_METERS = 2.5
const val DEFAULT_DRAWN_TREE_HEIGHT_METERS = 8.0
const val DEFAULT_DRAWN_TREE_RADIUS_METERS = 5.0
