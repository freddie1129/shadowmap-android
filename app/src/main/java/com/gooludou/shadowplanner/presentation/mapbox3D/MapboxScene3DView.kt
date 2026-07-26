package com.gooludou.shadowplanner.presentation.mapbox3D

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.DrawnTree
import com.gooludou.shadowplanner.domain.DrawnWall
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.GeoPolygon
import com.gooludou.shadowplanner.domain.SolarPosition
import com.gooludou.shadowplanner.presentation.components.mapboxNative3dConfig
import com.gooludou.shadowplanner.presentation.ShadowMapUiState
import com.gooludou.shadowplanner.presentation.drawview.Map2DTopControls
import com.gooludou.shadowplanner.scene.SceneViewport
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.coroutine.styleLoadedEvents
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.compose.style.layers.generated.VisibilityValue
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardSatelliteStyleState
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.compose.style.terrain.generated.TerrainState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.light.generated.ambientLight
import com.mapbox.maps.extension.style.light.generated.directionalLight
import com.mapbox.maps.extension.style.light.setLight
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.first

/** Hosts the Mapbox 3D scene, drawing layers, sky dome, and scene controls. */
@Composable
@OptIn(MapboxDelicateApi::class, MapboxExperimental::class)
@Suppress("LongMethod")
fun MapboxScene3DView(
    buildings: List<Building>,
    walls: List<DrawnWall>,
    trees: List<DrawnTree>,
    uiState: ShadowMapUiState,
    viewport: MapboxScene3DViewport,
    solarPosition: SolarPosition?,
    sunPath: List<SolarPosition>,
    showDome: Boolean,
    selectedEpochMillis: Long,
    timeZoneId: String,
    calculationLocation: GeoPoint?,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLocationSearch: () -> Unit,
    onShowLocationInfo: () -> Unit,
    onRecenterCurrentLocation: () -> Unit,
    canRecenterCurrentLocation: Boolean,
    onOpenProjects: () -> Unit,
    onSaveProject: () -> Unit,
    onToggleDome: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var basemapStyle by remember { mutableStateOf(MapboxBasemapStyle.SATELLITE) }
    var useMapboxBuildings by remember { mutableStateOf(false) }
    var sceneMapView by remember { mutableStateOf<MapView?>(null) }
    val skyState = rememberMapboxSceneSkyState(viewport, solarPosition, sunPath)
    val mapSources = rememberMapboxScene3DMapSources(buildings, walls, trees)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(viewport.center.longitude, viewport.center.latitude))
            zoom(viewport.zoom)
            bearing(viewport.bearing)
            pitch(Scene3DCamera.ORBIT_PITCH_DEGREES)
        }
    }
    LaunchedEffect(viewport) {
        mapViewportState.setCameraOptions {
            center(Point.fromLngLat(viewport.center.longitude, viewport.center.latitude))
            zoom(viewport.zoom)
            bearing(viewport.bearing)
            pitch(Scene3DCamera.ORBIT_PITCH_DEGREES)
        }
    }
    val buildingColor = MaterialTheme.colorScheme.surfaceVariant
    val wallColor = MaterialTheme.colorScheme.tertiary
    val trunkColor = Color(0xFF75543A)
    val canopyColor = Color(0xFF3F7D48)
    Box(modifier = modifier.fillMaxSize()) {
        MapboxScene3DMap(
            mapViewportState = mapViewportState,
            buildingSource = mapSources.buildingSource,
            wallSource = mapSources.wallSource,
            treeTrunkSource = mapSources.treeTrunkSource,
            treeCanopySource = mapSources.treeCanopySource,
            buildingColor = buildingColor,
            wallColor = wallColor,
            trunkColor = trunkColor,
            canopyColor = canopyColor,
            solarPosition = solarPosition,
            basemapStyle = basemapStyle,
            useMapboxBuildings = useMapboxBuildings,
            onMapViewReady = { sceneMapView = it }
        ) {
            if (showDome) {
                SceneSkyModelLayers(skyState)
            }
        }

        Map2DTopControls(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onOpenLocationSearch = onOpenLocationSearch,
            onShowLocationInfo = onShowLocationInfo,
            onRecenterCurrentLocation = onRecenterCurrentLocation,
            canRecenterCurrentLocation = canRecenterCurrentLocation,
            onOpenProjects = onOpenProjects,
            onSaveProject = onSaveProject,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
        )

        MapboxScene3DControls(
            mapViewportState = mapViewportState,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            location = calculationLocation ?: viewport.center,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            basemapStyle = basemapStyle,
            onToggleBasemapStyle = {
                if (basemapStyle == MapboxBasemapStyle.STANDARD) useMapboxBuildings = false
                basemapStyle = if (basemapStyle == MapboxBasemapStyle.STANDARD) {
                    MapboxBasemapStyle.SATELLITE
                } else {
                    MapboxBasemapStyle.STANDARD
                }
            },
            showDome = showDome,
            useMapboxBuildings = useMapboxBuildings,
            onToggleBuildingSource = {
                useMapboxBuildings = !useMapboxBuildings
                basemapStyle = if (useMapboxBuildings) {
                    MapboxBasemapStyle.STANDARD
                } else {
                    MapboxBasemapStyle.SATELLITE
                }
            },
            onToggleDome = {
                if (!showDome) {
                    val currentViewport = sceneMapView
                        ?.currentScene3DViewport(viewport)
                        ?: viewport.copy(
                            center = mapViewportState.cameraState?.center
                                ?.let { GeoPoint(it.longitude(), it.latitude()) }
                                ?: viewport.center
                        )
                    skyState.updateViewport(currentViewport)
                }
                onToggleDome()
            },
            onBackToMap = onBackToMap
        )
    }
}

/** Creates the Mapbox map, applies the selected style, and installs scene layers. */
@Composable
@OptIn(MapboxExperimental::class)
@Suppress("LongMethod")
private fun MapboxScene3DMap(
    mapViewportState: MapViewportState,
    buildingSource: GeoJsonSourceState,
    wallSource: GeoJsonSourceState,
    treeTrunkSource: GeoJsonSourceState,
    treeCanopySource: GeoJsonSourceState,
    buildingColor: Color,
    wallColor: Color,
    trunkColor: Color,
    canopyColor: Color,
    solarPosition: SolarPosition?,
    basemapStyle: MapboxBasemapStyle,
    useMapboxBuildings: Boolean,
    onMapViewReady: (MapView) -> Unit,
    domeContent: @Composable () -> Unit
) {
    // Compose Preview cannot initialize Mapbox's native renderer, so show a safe placeholder.
    if (LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.mapbox_3d_view))
        }
        return
    }
    val mapState = rememberMapState {
        gesturesSettings = GesturesSettings {
            pitchEnabled = true
        }
    }
    val sunVisible = solarPosition?.isAboveHorizon == true
    val sunAzimuth = solarPosition?.azimuthDegrees ?: Scene3DLighting.DEFAULT_AZIMUTH_DEGREES
    val sunZenith = solarPosition?.zenithDegrees
        ?.coerceIn(Scene3DLighting.MIN_POLAR_ANGLE_DEGREES, Scene3DLighting.MAX_POLAR_ANGLE_DEGREES)
        ?: Scene3DLighting.DEFAULT_POLAR_ANGLE_DEGREES
    val standardSatelliteStyleState = rememberStandardSatelliteStyleState {
        terrainState = TerrainState.DISABLED
    }
    val standardStyleState = rememberStandardStyleState {
        terrainState = TerrainState.DISABLED
    }
    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        mapState = mapState,
        scaleBar = { },
        style = {
            when (basemapStyle) {
                MapboxBasemapStyle.STANDARD -> {
                    MapboxStandardStyle(standardStyleState = standardStyleState)
                }

                MapboxBasemapStyle.SATELLITE -> {
                    MapboxStandardSatelliteStyle(
                        standardSatelliteStyleState = standardSatelliteStyleState
                    )
                }
            }
        }
    ) {
        BuildingExtrusionLayer(buildingSource, buildingColor, !useMapboxBuildings)
        WallExtrusionLayer(wallSource, wallColor)
        TreeExtrusionLayers(treeTrunkSource, treeCanopySource, trunkColor, canopyColor)
        domeContent()
        MapEffect(sunVisible, sunAzimuth, sunZenith, basemapStyle) { mapView ->
            if (!mapView.mapboxMap.isStyleLoaded()) {
                mapView.mapboxMap.styleLoadedEvents.first()
            }
            val ambientLight = ambientLight(Scene3DMapIds.AMBIENT_LIGHT) {
                intensity(Scene3DLighting.AMBIENT_INTENSITY)
            }
            val directionalLight = directionalLight(Scene3DMapIds.SUN_LIGHT) {
                castShadows(sunVisible)
                direction(listOf(sunAzimuth, sunZenith))
                intensity(if (sunVisible) Scene3DLighting.SUN_INTENSITY else 0.0)
                shadowIntensity(Scene3DLighting.SHADOW_INTENSITY)
            }
            mapView.mapboxMap.setLight(ambientLight, directionalLight)
        }
        MapEffect(basemapStyle, useMapboxBuildings) { mapView ->
            onMapViewReady(mapView)
            if (!mapView.mapboxMap.isStyleLoaded()) {
                mapView.mapboxMap.styleLoadedEvents.first()
            }
            mapboxNative3dConfig(
                useMapboxBuildings
            ).forEach { (key, enabled) ->
                mapView.mapboxMap.setStyleImportConfigProperty(
                    Scene3DMapIds.STANDARD_STYLE_IMPORT,
                    key,
                    Value(enabled)
                )
            }
        }
    }
}

/** Renders user-drawn building polygons as height-aware Mapbox extrusions. */
@Composable
private fun BuildingExtrusionLayer(source: GeoJsonSourceState, color: Color, visible: Boolean) {
    FillExtrusionLayer(sourceState = source, layerId = Scene3DDrawingLayers.BUILDING_LAYER) {
        fillExtrusionBase = DoubleValue(Expression.get(Scene3DFeatureProperties.BASE_HEIGHT))
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionColor = ColorValue(color)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionAmbientOcclusionIntensity = DoubleValue(Scene3DObjectStyle.AMBIENT_OCCLUSION)
        fillExtrusionCastShadows = BooleanValue(true)
        visibility = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
    }
}

/** Renders user-drawn walls as shadow-casting extruded lines. */
@Composable
@OptIn(MapboxExperimental::class)
private fun WallExtrusionLayer(source: GeoJsonSourceState, color: Color) {
    FillExtrusionLayer(sourceState = source, layerId = Scene3DDrawingLayers.WALL_LAYER) {
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionLineWidth = DoubleValue(Scene3DObjectStyle.WALL_WIDTH_METERS)
        fillExtrusionColor = ColorValue(color)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
    }
}

/** Renders the trunk and canopy extrusions for user-drawn trees. */
@Composable
@OptIn(MapboxExperimental::class)
private fun TreeExtrusionLayers(
    trunkSource: GeoJsonSourceState,
    canopySource: GeoJsonSourceState,
    trunkColor: Color,
    canopyColor: Color
) {
    FillExtrusionLayer(sourceState = trunkSource, layerId = Scene3DDrawingLayers.TREE_TRUNK_LAYER) {
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionColor = ColorValue(trunkColor)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
    }
    FillExtrusionLayer(
        sourceState = canopySource,
        layerId = Scene3DDrawingLayers.TREE_CANOPY_LAYER
    ) {
        fillExtrusionBase = DoubleValue(Expression.get(Scene3DFeatureProperties.BASE_HEIGHT))
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionColor = ColorValue(canopyColor)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionEdgeRadius = DoubleValue(Scene3DTreeGeometry.EDGE_RADIUS_METERS)
        fillExtrusionRoundedRoof = BooleanValue(true)
        fillExtrusionCastShadows = BooleanValue(true)
    }
}

/** Converts a domain building into a Mapbox polygon feature with height properties. */
private fun Building.toMapboxFeature(): Feature? {
    if (polygon.rings.firstOrNull().orEmpty().distinct().size < 3) return null
    return Feature.fromGeometry(polygon.toMapboxPolygon()).apply {
        addNumberProperty(Scene3DFeatureProperties.BASE_HEIGHT, minHeightMeters.coerceAtLeast(0.0))
        addNumberProperty(
            Scene3DFeatureProperties.HEIGHT,
            heightMeters.coerceAtLeast(minHeightMeters)
        )
    }
}

/** Converts a domain wall into a Mapbox line feature with a height property. */
private fun DrawnWall.toMapboxFeature(): Feature? {
    if (points.size < 2) return null
    return Feature.fromGeometry(LineString.fromLngLats(points.map(GeoPoint::toMapboxPoint))).apply {
        addNumberProperty(Scene3DFeatureProperties.HEIGHT, heightMeters.coerceAtLeast(0.0))
    }
}

/** Converts a drawn tree into its circular trunk footprint feature. */
private fun DrawnTree.toTrunkFeature(): Feature = Feature.fromGeometry(
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

/** Converts a drawn tree into its circular canopy footprint feature. */
private fun DrawnTree.toCanopyFeature(): Feature = Feature.fromGeometry(
    circlePolygon(center, radiusMeters.coerceAtLeast(Scene3DTreeGeometry.MIN_CANOPY_RADIUS_METERS))
).apply {
    val height = heightMeters.coerceAtLeast(Scene3DTreeGeometry.MIN_TREE_HEIGHT_METERS)
    addNumberProperty(
        Scene3DFeatureProperties.BASE_HEIGHT,
        height * Scene3DTreeGeometry.CANOPY_BASE_RATIO
    )
    addNumberProperty(Scene3DFeatureProperties.HEIGHT, height)
}

/** Converts polygon rings into a Mapbox polygon, closing open rings when necessary. */
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

/** Builds a geodesic-enough local circle polygon around a geographic point. */
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

/** Converts the app's geographic point type to a Mapbox point. */
private fun GeoPoint.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

/** Reads the current Mapbox camera and screen footprint for dome sizing. */
internal fun MapView.currentScene3DViewport(
    fallback: MapboxScene3DViewport
): MapboxScene3DViewport? {
    if (width <= 0 || height <= 0) return null
    val camera = mapboxMap.cameraState
    val center = camera.center
    // Pitched screen corners include distant ground and inflate the boundary. Measure the
    // same center and zoom with a virtual top-down camera so screen-fit sizing is pitch-neutral.
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

private data class MapboxScene3DMapSources(
    val buildingSource: GeoJsonSourceState,
    val wallSource: GeoJsonSourceState,
    val treeTrunkSource: GeoJsonSourceState,
    val treeCanopySource: GeoJsonSourceState
)

@Composable
private fun rememberMapboxScene3DMapSources(
    buildings: List<Building>,
    walls: List<DrawnWall>,
    trees: List<DrawnTree>
): MapboxScene3DMapSources {
    val buildingFeatures = remember(buildings) { buildings.mapNotNull(Building::toMapboxFeature) }
    val wallFeatures = remember(walls) { walls.mapNotNull(DrawnWall::toMapboxFeature) }
    val treeTrunkFeatures = remember(trees) { trees.map(DrawnTree::toTrunkFeature) }
    val treeCanopyFeatures = remember(trees) { trees.map(DrawnTree::toCanopyFeature) }
    return MapboxScene3DMapSources(
        buildingSource = rememberFeatureSource(
            Scene3DDrawingLayers.BUILDING_SOURCE,
            buildingFeatures
        ),
        wallSource = rememberFeatureSource(Scene3DDrawingLayers.WALL_SOURCE, wallFeatures),
        treeTrunkSource = rememberFeatureSource(
            Scene3DDrawingLayers.TREE_TRUNK_SOURCE,
            treeTrunkFeatures
        ),
        treeCanopySource = rememberFeatureSource(
            Scene3DDrawingLayers.TREE_CANOPY_SOURCE,
            treeCanopyFeatures
        )
    )
}

/** Remembers a GeoJSON source and refreshes its data when features change. */
@Composable
private fun rememberFeatureSource(id: String, features: List<Feature>): GeoJsonSourceState {
    val source = remember(id) {
        GeoJsonSourceState(id).apply { data = GeoJSONData(features) }
    }
    LaunchedEffect(features) {
        source.data = GeoJSONData(features)
    }
    return source
}
