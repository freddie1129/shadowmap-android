package com.gooludou.shadowplanner.renderer.mapbox

import com.gooludou.shadowplanner.core.geometry.AutomaticBuildingMatcher
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.BuildingSource
import com.gooludou.shadowplanner.core.model.DEFAULT_BUILDING_HEIGHT_METERS
import com.gooludou.shadowplanner.core.model.DrawMode
import com.gooludou.shadowplanner.core.model.DrawnObjectSelection
import com.gooludou.shadowplanner.core.model.DrawnObjectType
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.model.GeoPolygon
import com.gooludou.shadowplanner.core.model.PendingDrawing
import com.gooludou.shadowplanner.core.model.SceneObjectSource
import com.gooludou.shadowplanner.core.model.ShadowAppearance
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.interactions.standard.generated.StandardBuildings
import com.mapbox.maps.interactions.standard.generated.StandardBuildingsFeature
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("TooManyFunctions")
class MapboxShadowMapController
@AssistedInject
constructor(
    @Assisted private val mapView: MapView
) {
    fun queryDrawing(point: Point, callback: (DrawnObjectSelection?) -> Unit) {
        val screenCoordinate = mapView.mapboxMap.pixelForCoordinate(point)
        mapView.mapboxMap.queryRenderedFeatures(
            RenderedQueryGeometry(screenCoordinate),
            RenderedQueryOptions(
                listOf(
                    SELECTED_LINE_LAYER_ID,
                    SELECTED_FILL_LAYER_ID,
                    LOADED_BUILDINGS_LINE_LAYER_ID,
                    LOADED_BUILDINGS_FILL_LAYER_ID,
                    DRAWN_TREES_CENTER_LAYER_ID,
                    DRAWN_TREES_LINE_LAYER_ID,
                    DRAWN_TREES_FILL_LAYER_ID,
                    DRAWN_WALLS_LAYER_ID,
                    DRAWN_BUILDINGS_LINE_LAYER_ID,
                    DRAWN_BUILDINGS_FILL_LAYER_ID
                ),
                null
            )
        ) { result ->
            val feature = result.value?.firstOrNull()?.queriedFeature?.feature
            val id = feature?.getStringProperty(PROPERTY_ID)
            val type = when (feature?.getStringProperty(PROPERTY_TYPE)) {
                TYPE_LOADED_BUILDING, TYPE_DRAWN_BUILDING -> DrawnObjectType.BUILDING
                TYPE_WALL -> DrawnObjectType.WALL
                TYPE_TREE, TYPE_TREE_CENTER -> DrawnObjectType.TREE
                else -> null
            }
            val source = if (feature?.getStringProperty(PROPERTY_TYPE) == TYPE_LOADED_BUILDING) {
                SceneObjectSource.AUTOMATIC
            } else {
                SceneObjectSource.MANUAL
            }
            callback(
                if (id != null &&
                    type != null
                ) {
                    DrawnObjectSelection(id, type, source)
                } else {
                    null
                }
            )
        }
    }

    @OptIn(MapboxExperimental::class)
    suspend fun fetchBuildings(loadType: BuildingLoadType): List<Building> =
        withContext(Dispatchers.Main.immediate) {
            var switchedToStandard = false
            try {
                withTimeout(STYLE_OPERATION_TIMEOUT_MILLIS) {
                    awaitStyle(Style.STANDARD)
                    switchedToStandard = true
                    awaitMapIdle()
                    queryBuildings(loadType)
                }
            } finally {
                if (switchedToStandard) {
                    withContext(NonCancellable) {
                        val restored =
                            withTimeoutOrNull(STYLE_OPERATION_TIMEOUT_MILLIS) {
                                awaitStyle(Style.STANDARD_SATELLITE)
                                awaitMapIdle()
                            }
                        checkNotNull(restored) { "Timed out restoring the satellite style" }
                    }
                }
            }
        }

    @Suppress("LongMethod", "LongParameterList")
    fun render(
        loadedBuildings: List<Building>,
        drawnBuildings: List<Building>,
        drawnWalls: List<DrawnWall>,
        drawnTrees: List<DrawnTree>,
        selection: DrawnObjectSelection?,
        activeDrawMode: DrawMode?,
        inProgressVertices: List<GeoPoint>,
        pendingDrawing: PendingDrawing?,
        crosshairPoint: GeoPoint?,
        shadows: List<GeoPolygon>,
        shadowAppearance: ShadowAppearance
    ) {
        mapView.mapboxMap.getStyle { style ->
            val shadowData = shadows.toFeatureCollection()
            val sceneData = sceneFeatureCollection(
                loadedBuildings,
                drawnBuildings,
                drawnWalls,
                drawnTrees,
                selection
            )
            val previewData = previewFeatureCollection(
                activeDrawMode,
                inProgressVertices,
                pendingDrawing,
                crosshairPoint
            )

            val shadowSource = style.getSourceAs<GeoJsonSource>(SHADOWS_SOURCE_ID)
            if (shadowSource == null) {
                style.addSource(
                    geoJsonSource(SHADOWS_SOURCE_ID) { featureCollection(shadowData) }
                )
                style.addLayer(
                    fillLayer(SHADOWS_FILL_LAYER_ID, SHADOWS_SOURCE_ID) {
                        fillColor(shadowAppearance.mapboxColor)
                        fillOpacity(shadowAppearance.opacity.toDouble())
                    }
                )
            } else {
                shadowSource.featureCollection(shadowData)
                style.getLayerAs<com.mapbox.maps.extension.style.layers.generated.FillLayer>(
                    SHADOWS_FILL_LAYER_ID
                )?.apply {
                    fillColor(shadowAppearance.mapboxColor)
                    fillOpacity(shadowAppearance.opacity.toDouble())
                }
            }

            val sceneSource = style.getSourceAs<GeoJsonSource>(SCENE_SOURCE_ID)
            if (sceneSource == null) {
                style.addSource(
                    geoJsonSource(SCENE_SOURCE_ID) { featureCollection(sceneData) }
                )
                style.addLayer(
                    fillLayer(LOADED_BUILDINGS_FILL_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_LOADED_BUILDING))
                        fillColor("#4CAF50")
                        fillOpacity(0.25)
                    }
                )
                style.addLayer(
                    lineLayer(LOADED_BUILDINGS_LINE_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_LOADED_BUILDING))
                        lineColor("#0DFF72")
                        lineOpacity(0.95)
                        lineWidth(2.5)
                    }
                )
                style.addLayer(
                    fillLayer(DRAWN_BUILDINGS_FILL_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_DRAWN_BUILDING))
                        fillColor("#E8EAED")
                        fillOpacity(0.42)
                    }
                )
                style.addLayer(
                    lineLayer(DRAWN_BUILDINGS_LINE_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_DRAWN_BUILDING))
                        lineColor("#FFFFFF")
                        lineWidth(3.0)
                    }
                )
                style.addLayer(
                    lineLayer(DRAWN_WALLS_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_WALL))
                        lineColor("#00E5FF")
                        lineWidth(5.0)
                    }
                )
                style.addLayer(
                    fillLayer(DRAWN_TREES_FILL_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_TREE))
                        fillColor("#32C864")
                        fillOpacity(0.4)
                    }
                )
                style.addLayer(
                    lineLayer(DRAWN_TREES_LINE_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_TREE))
                        lineColor("#1E963C")
                        lineWidth(2.0)
                    }
                )
                style.addLayer(
                    circleLayer(DRAWN_TREES_CENTER_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(typeFilter(TYPE_TREE_CENTER))
                        circleColor("#FFFFFF")
                        circleStrokeColor("#1E963C")
                        circleStrokeWidth(2.0)
                        circleRadius(5.0)
                    }
                )
                style.addLayer(
                    fillLayer(SELECTED_FILL_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(selectedFilter())
                        fillColor("#FFC832")
                        fillOpacity(0.48)
                    }
                )
                style.addLayer(
                    lineLayer(SELECTED_LINE_LAYER_ID, SCENE_SOURCE_ID) {
                        filter(selectedFilter())
                        lineColor("#FFC832")
                        lineWidth(6.0)
                    }
                )
            } else {
                sceneSource.featureCollection(sceneData)
            }

            val previewSource = style.getSourceAs<GeoJsonSource>(PREVIEW_SOURCE_ID)
            if (previewSource == null) {
                style.addSource(
                    geoJsonSource(PREVIEW_SOURCE_ID) { featureCollection(previewData) }
                )
                style.addLayer(
                    lineLayer(PREVIEW_SOLID_LINE_LAYER_ID, PREVIEW_SOURCE_ID) {
                        filter(kindFilter(KIND_DRAFT_SOLID))
                        lineColor("#FFFFFF")
                        lineWidth(3.0)
                    }
                )
                style.addLayer(
                    lineLayer(PREVIEW_DASHED_LINE_LAYER_ID, PREVIEW_SOURCE_ID) {
                        filter(kindFilter(KIND_DRAFT_DASHED))
                        lineColor("#FFFFFF")
                        lineWidth(3.0)
                        lineDasharray(listOf(2.0, 2.0))
                    }
                )
                style.addLayer(
                    fillLayer(PREVIEW_FILL_LAYER_ID, PREVIEW_SOURCE_ID) {
                        filter(kindFilter(KIND_PENDING_FILL))
                        fillColor("#FFFFFF")
                        fillOpacity(0.32)
                    }
                )
                style.addLayer(
                    circleLayer(PREVIEW_VERTEX_LAYER_ID, PREVIEW_SOURCE_ID) {
                        filter(kindFilter(KIND_VERTEX))
                        circleColor("#FFD600")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                        circleRadius(6.0)
                    }
                )
            } else {
                previewSource.featureCollection(previewData)
            }
        }
    }

    private fun sceneFeatureCollection(
        loadedBuildings: List<Building>,
        drawnBuildings: List<Building>,
        drawnWalls: List<DrawnWall>,
        drawnTrees: List<DrawnTree>,
        selection: DrawnObjectSelection?
    ): FeatureCollection {
        val features = mutableListOf<Feature>()
        loadedBuildings.forEach { building ->
            val selectionId = AutomaticBuildingMatcher.identity(building).selectionId
            features += building.polygon.toFeature(
                selectionId,
                TYPE_LOADED_BUILDING,
                selection?.source == SceneObjectSource.AUTOMATIC && selection.id == selectionId
            )
        }
        drawnBuildings.forEach { building ->
            features += building.polygon.toFeature(
                building.id,
                TYPE_DRAWN_BUILDING,
                selection?.source == SceneObjectSource.MANUAL && selection.id == building.id
            )
        }
        drawnWalls.forEach { wall ->
            features += Feature.fromGeometry(
                LineString.fromLngLats(wall.points.map { it.toMapboxPoint() })
            ).withProperties(
                wall.id,
                TYPE_WALL,
                selection?.source == SceneObjectSource.MANUAL && selection.id == wall.id
            )
        }
        drawnTrees.forEach { tree ->
            features += tree.toCanopyPolygon().toFeature(
                tree.id,
                TYPE_TREE,
                selection?.source == SceneObjectSource.MANUAL && selection.id == tree.id
            )
            features += Feature.fromGeometry(tree.center.toMapboxPoint())
                .withProperties(tree.id, TYPE_TREE_CENTER, false)
        }
        return FeatureCollection.fromFeatures(features)
    }

    private fun previewFeatureCollection(
        activeDrawMode: DrawMode?,
        vertices: List<GeoPoint>,
        pendingDrawing: PendingDrawing?,
        crosshairPoint: GeoPoint?
    ): FeatureCollection {
        val features = mutableListOf<Feature>()
        vertices.forEach { vertex ->
            features += Feature.fromGeometry(vertex.toMapboxPoint()).apply {
                addStringProperty(PROPERTY_KIND, KIND_VERTEX)
            }
        }
        if (vertices.size >= 2) {
            features += previewLine(vertices, KIND_DRAFT_SOLID)
        }
        if (crosshairPoint != null && vertices.isNotEmpty()) {
            features += previewLine(listOf(vertices.last(), crosshairPoint), KIND_DRAFT_DASHED)
            if (activeDrawMode == DrawMode.BUILDING && vertices.size >= 2) {
                features += previewLine(listOf(crosshairPoint, vertices.first()), KIND_DRAFT_DASHED)
            }
        }
        when (pendingDrawing) {
            is PendingDrawing.Building -> {
                val ring = pendingDrawing.vertices + pendingDrawing.vertices.first()
                features +=
                    GeoPolygon(listOf(ring)).toFeature(null, TYPE_DRAWN_BUILDING, false).apply {
                        addStringProperty(PROPERTY_KIND, KIND_PENDING_FILL)
                    }
            }

            is PendingDrawing.Wall ->
                features +=
                    previewLine(pendingDrawing.points, KIND_DRAFT_SOLID)

            is PendingDrawing.Tree -> {
                features += DrawnTree(center = pendingDrawing.center).toCanopyPolygon()
                    .toFeature(null, TYPE_TREE, false).apply {
                        addStringProperty(PROPERTY_KIND, KIND_PENDING_FILL)
                    }
            }

            null -> Unit
        }
        return FeatureCollection.fromFeatures(features)
    }

    private fun previewLine(points: List<GeoPoint>, kind: String): Feature =
        Feature.fromGeometry(LineString.fromLngLats(points.map { it.toMapboxPoint() })).apply {
            addStringProperty(PROPERTY_KIND, kind)
        }

    private fun GeoPolygon.toFeature(id: String?, type: String, selected: Boolean): Feature =
        Feature.fromGeometry(toMapboxPolygon()).withProperties(id, type, selected)

    private fun Feature.withProperties(id: String?, type: String, selected: Boolean): Feature =
        apply {
            id?.let { addStringProperty(PROPERTY_ID, it) }
            addStringProperty(PROPERTY_TYPE, type)
            addBooleanProperty(PROPERTY_SELECTED, selected)
        }

    private fun DrawnTree.toCanopyPolygon(): GeoPolygon {
        val latitudeRadians = center.latitude * PI / 180.0
        val latitudeDegreesPerMeter = 180.0 / (PI * EARTH_RADIUS_METERS)
        val longitudeDegreesPerMeter = latitudeDegreesPerMeter / cos(latitudeRadians)
        val ring = (0..TREE_SEGMENTS).map { index ->
            val angle = index.toDouble() / TREE_SEGMENTS * 2.0 * PI
            GeoPoint(
                longitude = center.longitude + sin(angle) * radiusMeters * longitudeDegreesPerMeter,
                latitude = center.latitude + cos(angle) * radiusMeters * latitudeDegreesPerMeter
            )
        }
        return GeoPolygon(listOf(ring))
    }

    private fun GeoPoint.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

    private fun typeFilter(type: String): Expression = Expression.eq(
        Expression.get(PROPERTY_TYPE),
        Expression.literal(type)
    )

    private fun kindFilter(kind: String): Expression = Expression.eq(
        Expression.get(PROPERTY_KIND),
        Expression.literal(kind)
    )

    private fun selectedFilter(): Expression = Expression.eq(
        Expression.get(PROPERTY_SELECTED),
        Expression.literal(true)
    )

    private suspend fun awaitStyle(styleUri: String) {
        suspendCancellableCoroutine { continuation ->
            mapView.mapboxMap.loadStyle(styleUri) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private suspend fun awaitMapIdle() {
        suspendCancellableCoroutine { continuation ->
            var subscription: Cancelable? = null
            subscription =
                mapView.mapboxMap.subscribeMapIdle {
                    subscription?.cancel()
                    if (continuation.isActive) continuation.resume(Unit)
                }
            continuation.invokeOnCancellation { subscription?.cancel() }
        }
    }

    @OptIn(MapboxExperimental::class)
    private suspend fun queryBuildings(loadType: BuildingLoadType): List<Building> =
        suspendCancellableCoroutine { continuation ->
            val queryGeometry = loadType.toQueryGeometry(mapView.width, mapView.height)
            mapView.mapboxMap.queryRenderedFeatures(
                StandardBuildings(),
                queryGeometry
            ) { features ->
                if (continuation.isActive) {
                    continuation.resume(features.flatMap { it.toDomainFootprints() })
                }
            }
        }

    private fun StandardBuildingsFeature.toDomainFootprints(): List<Building> {
        val featureHeight = height?.takeIf { it > 0.0 } ?: DEFAULT_BUILDING_HEIGHT_METERS
        val featureId = this.id?.featureId ?: originalFeature.id()
        val featureNamespace = this.id?.featureNamespace
        return geometry.toGeoPolygons().map { polygon ->
            Building(
                id = featureId,
                polygon = polygon,
                heightMeters = featureHeight,
                minHeightMeters = minHeight ?: 0.0,
                source = BuildingSource.AUTOMATIC,
                automaticIdentity = AutomaticBuildingMatcher.identity(
                    featureId = featureId,
                    featureNamespace = featureNamespace,
                    polygon = polygon
                )
            )
        }
    }

    private fun Geometry.toGeoPolygons(): List<GeoPolygon> = when (this) {
        is Polygon -> listOf(coordinates().toDomainPolygon())
        is MultiPolygon -> coordinates().map { it.toDomainPolygon() }
        else -> emptyList()
    }

    private fun List<List<Point>>.toDomainPolygon(): GeoPolygon = GeoPolygon(
        rings =
            map { ring ->
                ring.map { point -> GeoPoint(point.longitude(), point.latitude()) }
            }
    )

    private fun List<GeoPolygon>.toFeatureCollection(): FeatureCollection =
        FeatureCollection.fromFeatures(
            map { polygon -> Feature.fromGeometry(polygon.toMapboxPolygon()) }
        )

    private fun GeoPolygon.toMapboxPolygon(): Polygon = Polygon.fromLngLats(
        rings.map { ring ->
            ring.map { point -> Point.fromLngLat(point.longitude, point.latitude) }
        }
    )

    companion object {
        private const val STYLE_OPERATION_TIMEOUT_MILLIS = 20_000L
        private const val SCENE_SOURCE_ID = "scene-objects-source"
        private const val PREVIEW_SOURCE_ID = "drawing-preview-source"
        private const val LOADED_BUILDINGS_FILL_LAYER_ID = "loaded-buildings-fill"
        private const val LOADED_BUILDINGS_LINE_LAYER_ID = "loaded-buildings-outline"
        private const val DRAWN_BUILDINGS_FILL_LAYER_ID = "drawn-buildings-fill"
        private const val DRAWN_BUILDINGS_LINE_LAYER_ID = "drawn-buildings-outline"
        private const val DRAWN_WALLS_LAYER_ID = "drawn-walls"
        private const val DRAWN_TREES_FILL_LAYER_ID = "drawn-trees-fill"
        private const val DRAWN_TREES_LINE_LAYER_ID = "drawn-trees-outline"
        private const val DRAWN_TREES_CENTER_LAYER_ID = "drawn-trees-center"
        private const val SELECTED_FILL_LAYER_ID = "selected-object-fill"
        private const val SELECTED_LINE_LAYER_ID = "selected-object-outline"
        private const val PREVIEW_SOLID_LINE_LAYER_ID = "drawing-preview-solid"
        private const val PREVIEW_DASHED_LINE_LAYER_ID = "drawing-preview-dashed"
        private const val PREVIEW_FILL_LAYER_ID = "drawing-preview-fill"
        private const val PREVIEW_VERTEX_LAYER_ID = "drawing-preview-vertices"
        private const val SHADOWS_SOURCE_ID = "calculated-building-shadows-source"
        private const val SHADOWS_FILL_LAYER_ID = "calculated-building-shadows-fill"
        private const val PROPERTY_ID = "id"
        private const val PROPERTY_TYPE = "object_type"
        private const val PROPERTY_SELECTED = "selected"
        private const val PROPERTY_KIND = "kind"
        private const val TYPE_LOADED_BUILDING = "loaded_building"
        private const val TYPE_DRAWN_BUILDING = "drawn_building"
        private const val TYPE_WALL = "wall"
        private const val TYPE_TREE = "tree"
        private const val TYPE_TREE_CENTER = "tree_center"
        private const val KIND_VERTEX = "vertex"
        private const val KIND_DRAFT_SOLID = "draft_solid"
        private const val KIND_DRAFT_DASHED = "draft_dashed"
        private const val KIND_PENDING_FILL = "pending_fill"
        private const val TREE_SEGMENTS = 24
        private const val EARTH_RADIUS_METERS = 6_378_137.0
    }

    @AssistedFactory
    interface Factory {
        fun create(mapView: MapView): MapboxShadowMapController
    }
}
