package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.ViewInAr
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
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.domain.Building
import com.gooludou.shadowplanner.domain.DrawnTree
import com.gooludou.shadowplanner.domain.DrawnWall
import com.gooludou.shadowplanner.domain.GeoPoint
import com.gooludou.shadowplanner.domain.GeoPolygon
import com.gooludou.shadowplanner.domain.SolarPosition
import com.gooludou.shadowplanner.scene.SceneViewport
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapView
import com.mapbox.maps.coroutine.styleLoadedEvents
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
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

private enum class MapboxBasemapStyle {
    STANDARD,
    SATELLITE
}

@Composable
@OptIn(MapboxDelicateApi::class, MapboxExperimental::class)
fun MapboxScene3DView(
    buildings: List<Building>,
    walls: List<DrawnWall>,
    trees: List<DrawnTree>,
    viewport: MapboxScene3DViewport,
    solarPosition: SolarPosition?,
    sunPath: List<SolarPosition>,
    showDome: Boolean,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    onToggleDome: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var basemapStyle by remember { mutableStateOf(MapboxBasemapStyle.SATELLITE) }
    var useMapboxBuildings by remember { mutableStateOf(false) }
    var sceneMapView by remember { mutableStateOf<MapView?>(null) }
    val skyState = rememberMapboxSceneSkyState(viewport, solarPosition, sunPath)
    val buildingFeatures = remember(buildings) { buildings.mapNotNull(Building::toMapboxFeature) }
    val wallFeatures = remember(walls) { walls.mapNotNull(DrawnWall::toMapboxFeature) }
    val treeTrunkFeatures = remember(trees) { trees.map(DrawnTree::toTrunkFeature) }
    val treeCanopyFeatures = remember(trees) { trees.map(DrawnTree::toCanopyFeature) }
    val buildingSource = rememberFeatureSource(BUILDING_SOURCE_ID, buildingFeatures)
    val wallSource = rememberFeatureSource(WALL_SOURCE_ID, wallFeatures)
    val treeTrunkSource = rememberFeatureSource(TREE_TRUNK_SOURCE_ID, treeTrunkFeatures)
    val treeCanopySource = rememberFeatureSource(TREE_CANOPY_SOURCE_ID, treeCanopyFeatures)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(viewport.center.longitude, viewport.center.latitude))
            zoom(viewport.zoom)
            bearing(viewport.bearing)
            pitch(MAPBOX_3D_PITCH_DEGREES)
        }
    }
    val buildingColor = MaterialTheme.colorScheme.surfaceVariant
    val wallColor = MaterialTheme.colorScheme.tertiary
    val trunkColor = Color(0xFF75543A)
    val canopyColor = Color(0xFF3F7D48)
    Box(modifier = modifier.fillMaxSize()) {
        MapboxScene3DMap(
            mapViewportState = mapViewportState,
            buildingSource = buildingSource,
            wallSource = wallSource,
            treeTrunkSource = treeTrunkSource,
            treeCanopySource = treeCanopySource,
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

        MapboxScene3DControls(
            mapViewportState = mapViewportState,
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            basemapStyle = basemapStyle,
            onToggleBasemapStyle = {
                if (basemapStyle == MapboxBasemapStyle.STANDARD) useMapboxBuildings = false
                basemapStyle = if (basemapStyle == MapboxBasemapStyle.STANDARD) {
                    MapboxBasemapStyle.SATELLITE
                } else MapboxBasemapStyle.STANDARD
            },
            showDome = showDome,
            useMapboxBuildings = useMapboxBuildings,
            onToggleBuildingSource = {
                useMapboxBuildings = !useMapboxBuildings
                basemapStyle = if (useMapboxBuildings) MapboxBasemapStyle.STANDARD else MapboxBasemapStyle.SATELLITE
            },
            onToggleDome = {
                if (!showDome) {
                    val currentViewport = sceneMapView?.currentScene3DViewport(viewport)
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

@Composable
private fun MapboxScene3DControls(
    mapViewportState: MapViewportState,
    selectedEpochMillis: Long,
    timeZoneId: String,
    onDateTimeChanged: (Long) -> Unit,
    onNowSelected: () -> Unit,
    basemapStyle: MapboxBasemapStyle,
    onToggleBasemapStyle: () -> Unit,
    showDome: Boolean,
    useMapboxBuildings: Boolean,
    onToggleBuildingSource: () -> Unit,
    onToggleDome: () -> Unit,
    onBackToMap: () -> Unit
) {
    val currentPitch = mapViewportState.cameraState?.pitch ?: MAPBOX_3D_PITCH_DEGREES
    val isTopDown = currentPitch <=
        TOP_DOWN_PITCH_THRESHOLD_DEGREES
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        SceneViewSwitchButton(
            label = stringResource(R.string.map_view),
            icon = Icons.Outlined.Map,
            onClick = onBackToMap,
            modifier = Modifier.align(Alignment.TopStart)
        )
        SceneViewSwitchButton(
            label = stringResource(
                if (basemapStyle == MapboxBasemapStyle.SATELLITE) {
                    R.string.standard_map_style
                } else {
                    R.string.satellite_map_style
                }
            ),
            icon = if (basemapStyle == MapboxBasemapStyle.SATELLITE) {
                Icons.Outlined.Map
            } else {
                Icons.Outlined.SatelliteAlt
            },
            onClick = onToggleBasemapStyle,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        MapboxDomeToggle(
            showDome = showDome,
            onClick = onToggleDome,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = MAPBOX_DOME_CONTROL_OFFSET)
        )
        MapboxBuildingSourceToggle(
            useMapboxBuildings = useMapboxBuildings,
            onClick = onToggleBuildingSource,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = MAPBOX_BUILDING_CONTROL_OFFSET)
        )
        SceneViewSwitchButton(
            label = stringResource(
                if (isTopDown) R.string.view_3d_button else R.string.top_down_view
            ),
            icon = if (isTopDown) Icons.Outlined.ViewInAr else Icons.Outlined.Map,
            onClick = {
                mapViewportState.setCameraOptions {
                    pitch(if (isTopDown) MAPBOX_3D_PITCH_DEGREES else TOP_DOWN_PITCH_DEGREES)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = ShadowMapDesign.dimensions.screenPadding,
                    bottom = MAPBOX_BOTTOM_CONTROL_CLEARANCE
                )
        )
        PitchSlider(
            pitch = currentPitch.toFloat(),
            onPitchChange = { pitch ->
                mapViewportState.setCameraOptions {
                    pitch(pitch.toDouble())
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        DateTimeSpinner(
            selectedEpochMillis = selectedEpochMillis,
            timeZoneId = timeZoneId,
            onDateTimeChanged = onDateTimeChanged,
            onNowSelected = onNowSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
@OptIn(MapboxExperimental::class)
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
    val sunAzimuth = solarPosition?.azimuthDegrees ?: DEFAULT_LIGHT_AZIMUTH_DEGREES
    val sunZenith = solarPosition?.zenithDegrees
        ?.coerceIn(MIN_LIGHT_POLAR_ANGLE_DEGREES, MAX_LIGHT_POLAR_ANGLE_DEGREES)
        ?: DEFAULT_LIGHT_POLAR_ANGLE_DEGREES
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
            val ambientLight = ambientLight(AMBIENT_LIGHT_ID) {
                intensity(AMBIENT_LIGHT_INTENSITY)
            }
            val directionalLight = directionalLight(SUN_LIGHT_ID) {
                castShadows(sunVisible)
                direction(listOf(sunAzimuth, sunZenith))
                intensity(if (sunVisible) SUN_LIGHT_INTENSITY else 0.0)
                shadowIntensity(SHADOW_INTENSITY)
            }
            mapView.mapboxMap.setLight(ambientLight, directionalLight)
        }
        MapEffect(basemapStyle, useMapboxBuildings) { mapView ->
            onMapViewReady(mapView)
            if (!mapView.mapboxMap.isStyleLoaded()) {
                mapView.mapboxMap.styleLoadedEvents.first()
            }
            mapboxNative3dConfig(useMapboxBuildings).forEach { (key, enabled) ->
                mapView.mapboxMap.setStyleImportConfigProperty(
                    STANDARD_STYLE_IMPORT_ID,
                    key,
                    Value(enabled)
                )
            }
        }
    }
}

@Composable
private fun BuildingExtrusionLayer(
    source: GeoJsonSourceState,
    color: Color,
    visible: Boolean
) {
    FillExtrusionLayer(sourceState = source, layerId = BUILDING_LAYER_ID) {
        fillExtrusionBase = DoubleValue(Expression.get(PROPERTY_BASE_HEIGHT))
        fillExtrusionHeight = DoubleValue(Expression.get(PROPERTY_HEIGHT))
        fillExtrusionColor = ColorValue(color)
        fillExtrusionOpacity = DoubleValue(OBJECT_OPACITY)
        fillExtrusionAmbientOcclusionIntensity = DoubleValue(AMBIENT_OCCLUSION)
        fillExtrusionCastShadows = BooleanValue(true)
        visibility = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun WallExtrusionLayer(source: GeoJsonSourceState, color: Color) {
    FillExtrusionLayer(sourceState = source, layerId = WALL_LAYER_ID) {
        fillExtrusionHeight = DoubleValue(Expression.get(PROPERTY_HEIGHT))
        fillExtrusionLineWidth = DoubleValue(WALL_WIDTH_METERS)
        fillExtrusionColor = ColorValue(color)
        fillExtrusionOpacity = DoubleValue(OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun TreeExtrusionLayers(
    trunkSource: GeoJsonSourceState,
    canopySource: GeoJsonSourceState,
    trunkColor: Color,
    canopyColor: Color
) {
    FillExtrusionLayer(sourceState = trunkSource, layerId = TREE_TRUNK_LAYER_ID) {
        fillExtrusionHeight = DoubleValue(Expression.get(PROPERTY_HEIGHT))
        fillExtrusionColor = ColorValue(trunkColor)
        fillExtrusionOpacity = DoubleValue(OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
    }
    FillExtrusionLayer(sourceState = canopySource, layerId = TREE_CANOPY_LAYER_ID) {
        fillExtrusionBase = DoubleValue(Expression.get(PROPERTY_BASE_HEIGHT))
        fillExtrusionHeight = DoubleValue(Expression.get(PROPERTY_HEIGHT))
        fillExtrusionColor = ColorValue(canopyColor)
        fillExtrusionOpacity = DoubleValue(OBJECT_OPACITY)
        fillExtrusionEdgeRadius = DoubleValue(TREE_EDGE_RADIUS_METERS)
        fillExtrusionRoundedRoof = BooleanValue(true)
        fillExtrusionCastShadows = BooleanValue(true)
    }
}

private fun Building.toMapboxFeature(): Feature? {
    if (polygon.rings.firstOrNull().orEmpty().distinct().size < 3) return null
    return Feature.fromGeometry(polygon.toMapboxPolygon()).apply {
        addNumberProperty(PROPERTY_BASE_HEIGHT, minHeightMeters.coerceAtLeast(0.0))
        addNumberProperty(PROPERTY_HEIGHT, heightMeters.coerceAtLeast(minHeightMeters))
    }
}

private fun DrawnWall.toMapboxFeature(): Feature? {
    if (points.size < 2) return null
    return Feature.fromGeometry(LineString.fromLngLats(points.map(GeoPoint::toMapboxPoint))).apply {
        addNumberProperty(PROPERTY_HEIGHT, heightMeters.coerceAtLeast(0.0))
    }
}

private fun DrawnTree.toTrunkFeature(): Feature = Feature.fromGeometry(
    circlePolygon(center, (radiusMeters * TREE_TRUNK_RADIUS_RATIO).coerceAtLeast(MIN_TRUNK_RADIUS_METERS))
).apply {
    addNumberProperty(PROPERTY_HEIGHT, heightMeters.coerceAtLeast(MIN_TREE_HEIGHT_METERS) * TREE_TRUNK_HEIGHT_RATIO)
}

private fun DrawnTree.toCanopyFeature(): Feature = Feature.fromGeometry(
    circlePolygon(center, radiusMeters.coerceAtLeast(MIN_CANOPY_RADIUS_METERS))
).apply {
    val height = heightMeters.coerceAtLeast(MIN_TREE_HEIGHT_METERS)
    addNumberProperty(PROPERTY_BASE_HEIGHT, height * TREE_CANOPY_BASE_RATIO)
    addNumberProperty(PROPERTY_HEIGHT, height)
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
    val latitudeDegreesPerMeter = 180.0 / (PI * EARTH_RADIUS_METERS)
    val longitudeDegreesPerMeter = latitudeDegreesPerMeter / cos(latitudeRadians)
    val ring = (0..TREE_SEGMENTS).map { index ->
        val angle = index.toDouble() / TREE_SEGMENTS * 2.0 * PI
        Point.fromLngLat(
            center.longitude + sin(angle) * radiusMeters * longitudeDegreesPerMeter,
            center.latitude + cos(angle) * radiusMeters * latitudeDegreesPerMeter
        )
    }
    return Polygon.fromLngLats(listOf(ring))
}

private fun GeoPoint.toMapboxPoint(): Point = Point.fromLngLat(longitude, latitude)

private fun MapView.currentScene3DViewport(
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

private const val STANDARD_STYLE_IMPORT_ID = "basemap"
private const val SUN_LIGHT_ID = "shadow-planner-sun"
private const val AMBIENT_LIGHT_ID = "shadow-planner-ambient"
private const val BUILDING_SOURCE_ID = "custom-3d-buildings-source"
private const val WALL_SOURCE_ID = "custom-3d-walls-source"
private const val TREE_TRUNK_SOURCE_ID = "custom-3d-tree-trunks-source"
private const val TREE_CANOPY_SOURCE_ID = "custom-3d-tree-canopies-source"
private const val BUILDING_LAYER_ID = "custom-3d-buildings-layer"
private const val WALL_LAYER_ID = "custom-3d-walls-layer"
private const val TREE_TRUNK_LAYER_ID = "custom-3d-tree-trunks-layer"
private const val TREE_CANOPY_LAYER_ID = "custom-3d-tree-canopies-layer"
private const val PROPERTY_HEIGHT = "height"
private const val PROPERTY_BASE_HEIGHT = "base_height"
private const val MAPBOX_3D_PITCH_DEGREES = 60.0
private const val TOP_DOWN_PITCH_DEGREES = 0.0
private const val TOP_DOWN_PITCH_THRESHOLD_DEGREES = 1.0
private const val DEFAULT_LIGHT_AZIMUTH_DEGREES = 210.0
private const val DEFAULT_LIGHT_POLAR_ANGLE_DEGREES = 30.0
private const val MIN_LIGHT_POLAR_ANGLE_DEGREES = 0.0
private const val MAX_LIGHT_POLAR_ANGLE_DEGREES = 90.0
private const val SUN_LIGHT_INTENSITY = 0.8
private const val AMBIENT_LIGHT_INTENSITY = 0.35
private const val SHADOW_INTENSITY = 0.85
private const val WALL_WIDTH_METERS = 0.2
private const val OBJECT_OPACITY = 1.0
private const val AMBIENT_OCCLUSION = 0.3
private const val TREE_TRUNK_HEIGHT_RATIO = 0.45
private const val TREE_CANOPY_BASE_RATIO = 0.35
private const val TREE_TRUNK_RADIUS_RATIO = 0.18
private const val TREE_EDGE_RADIUS_METERS = 0.5
private const val MIN_TREE_HEIGHT_METERS = 0.1
private const val MIN_TRUNK_RADIUS_METERS = 0.15
private const val MIN_CANOPY_RADIUS_METERS = 0.5
private const val TREE_SEGMENTS = 12
private const val EARTH_RADIUS_METERS = 6_378_137.0
private val MAPBOX_BOTTOM_CONTROL_CLEARANCE = 156.dp
private val MAPBOX_DOME_CONTROL_OFFSET = 52.dp
private val MAPBOX_BUILDING_CONTROL_OFFSET = 104.dp
