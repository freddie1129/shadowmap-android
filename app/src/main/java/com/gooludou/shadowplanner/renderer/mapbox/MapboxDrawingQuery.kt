package com.gooludou.shadowplanner.renderer.mapbox

import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.DrawnObjectType
import com.gooludou.shadowplanner.core.model.SceneObjectSource
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions

internal class MapboxDrawingQuery(private val mapView: MapView) {
    fun query(point: Point, callback: (DrawnObjectSelection?) -> Unit) {
        val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)
        mapView.mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenCoordinate),
            RenderedQueryOptions(QUERY_LAYER_IDS, null)
        ) { result ->
            val feature = result.value?.firstOrNull()?.queriedFeature?.feature
            val objectType = feature?.getStringProperty(PROPERTY_TYPE)
            val id = feature?.getStringProperty(PROPERTY_ID)
            val type = when (objectType) {
                TYPE_LOADED_BUILDING, TYPE_DRAWN_BUILDING -> DrawnObjectType.BUILDING
                TYPE_WALL -> DrawnObjectType.WALL
                TYPE_TREE, TYPE_TREE_CENTER -> DrawnObjectType.TREE
                else -> null
            }
            val source = if (objectType == TYPE_LOADED_BUILDING) {
                SceneObjectSource.AUTOMATIC
            } else {
                SceneObjectSource.MANUAL
            }
            callback(
                if (id != null && type != null) {
                    DrawnObjectSelection(id, type, source)
                } else {
                    null
                }
            )
        }
    }

    private companion object {
        val QUERY_LAYER_IDS = listOf(
            "selected-object-outline",
            "selected-object-fill",
            "loaded-buildings-outline",
            "loaded-buildings-fill",
            "drawn-trees-center",
            "drawn-trees-outline",
            "drawn-trees-fill",
            "drawn-walls",
            "drawn-buildings-outline",
            "drawn-buildings-fill"
        )
        const val PROPERTY_ID = "id"
        const val PROPERTY_TYPE = "object_type"
        const val TYPE_LOADED_BUILDING = "loaded_building"
        const val TYPE_DRAWN_BUILDING = "drawn_building"
        const val TYPE_WALL = "wall"
        const val TYPE_TREE = "tree"
        const val TYPE_TREE_CENTER = "tree_center"
    }
}
