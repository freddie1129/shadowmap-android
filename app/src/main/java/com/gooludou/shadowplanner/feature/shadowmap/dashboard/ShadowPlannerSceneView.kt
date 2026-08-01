package com.gooludou.shadowplanner.feature.shadowmap.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.Config
import com.gooludou.shadowplanner.R
import com.gooludou.shadowplanner.core.model.Building
import com.gooludou.shadowplanner.core.model.DrawnTree
import com.gooludou.shadowplanner.core.model.DrawnWall
import com.gooludou.shadowplanner.core.model.GeoPoint
import com.gooludou.shadowplanner.core.solar.SolarPosition
import com.gooludou.shadowplanner.core.ui.theme.LightSurfaceVariant
import com.gooludou.shadowplanner.feature.shadowmap.ShadowMapUiState
import com.gooludou.shadowplanner.feature.shadowmap.components.MapCenterPlus
import com.gooludou.shadowplanner.feature.shadowmap.components.mapboxNative3dConfig
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
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
import kotlinx.coroutines.flow.first

/** Hosts the Mapbox 3D scene, drawing layers, sky dome, and scene controls. */
@Composable
@OptIn(MapboxDelicateApi::class, MapboxExperimental::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun ShadowPlannerSceneView(
    uiState: ShadowMapUiState,
    state: ShadowPlannerSceneState,
    actions: ShadowPlannerSceneActions,
    modifier: Modifier = Modifier,
    mapSnapshotOverlay: ImageBitmap? = null
) {
    val viewport = state.viewport
    val solarPosition = uiState.solarPosition
    val initialDisplayMode = remember {
        initialMapDisplayMode(uiState.projectLoadRevision, uiState.hasDrawings)
    }
    var displayMode by remember {
        mutableStateOf(initialDisplayMode)
    }
    val activeDisplayMode = displayMode.forSceneMode(state.sceneMode)
    var hasEnteredEditing by remember { mutableStateOf(false) }
    var sceneMapView by remember { mutableStateOf<MapView?>(null) }
    var isSkyViewportReady by remember { mutableStateOf(false) }
    var skyRefreshRequest by remember { mutableStateOf(0) }
    val skyState = rememberMapboxSceneSkyState(viewport, solarPosition, uiState.sunPath)
    val mapSources = rememberMapboxScene3DMapSources(
        uiState.buildings,
        uiState.drawnWalls,
        uiState.drawnTrees
    )

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(viewport.center.longitude, viewport.center.latitude))
            zoom(viewport.zoom)
            bearing(viewport.bearing)
            pitch(initialDisplayMode.cameraPitchDegrees)
        }
    }
    LaunchedEffect(viewport) {
        mapViewportState.setCameraOptions {
            center(Point.fromLngLat(viewport.center.longitude, viewport.center.latitude))
            zoom(viewport.zoom)
            bearing(viewport.bearing)
        }
    }
    LaunchedEffect(uiState.projectLoadRevision) {
        if (uiState.projectLoadRevision == 0L) return@LaunchedEffect
        hasEnteredEditing = false
        val projectDisplayMode = if (uiState.hasDrawings) {
            MapDisplayDefaults.PROJECT_WITH_DRAWINGS
        } else {
            MapDisplayDefaults.PROJECT_WITHOUT_DRAWINGS
        }
        displayMode = projectDisplayMode
        mapViewportState.setCameraOptions {
            pitch(projectDisplayMode.cameraPitchDegrees)
        }
    }
    LaunchedEffect(state.sceneMode) {
        if (state.sceneMode == MapboxSceneMode.EDIT) {
            hasEnteredEditing = true
            displayMode = MapDisplayDefaults.EDITING
            mapViewportState.setCameraOptions {
                pitch(MapDisplayDefaults.EDITING.cameraPitchDegrees)
            }
        } else if (hasEnteredEditing) {
            val postEditingMode = postEditingDisplayMode(uiState.hasDrawings)
            displayMode = postEditingMode
            mapViewportState.setCameraOptions {
                pitch(postEditingMode.cameraPitchDegrees)
            }
            hasEnteredEditing = false
        }
    }
    fun refreshSkyViewport() {
        val currentViewport = sceneMapView
            ?.currentScene3DViewport(viewport)
            ?: viewport.copy(
                center = mapViewportState.cameraState?.center
                    ?.let { GeoPoint(it.longitude(), it.latitude()) }
                    ?: viewport.center
            )
        skyState.updateViewport(currentViewport)
    }
    LaunchedEffect(sceneMapView, skyRefreshRequest) {
        if (sceneMapView == null) {
            isSkyViewportReady = false
            return@LaunchedEffect
        }
        // Match the working toggle flow: keep the sky layers detached while their sources are
        // rebuilt from the measured 3D viewport, then attach them on the following frame.
        isSkyViewportReady = false
        withFrameNanos { }
        refreshSkyViewport()
        withFrameNanos { }
        isSkyViewportReady = true
    }
    val buildingColor = LightSurfaceVariant
    val wallColor = MaterialTheme.colorScheme.tertiary
    val trunkColor = Color(0xFF75543A)
    val canopyColor = Color(0xFF3F7D48)
    val buildingRenderMode = sceneBuildingRenderMode(
        buildingSelection = activeDisplayMode.content,
        basemapStyle = activeDisplayMode.basemapStyle,
        cameraPitchDegrees = activeDisplayMode.cameraPitchDegrees
    )
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
            basemapStyle = activeDisplayMode.basemapStyle,
            buildingRenderMode = buildingRenderMode,
            sceneMode = state.sceneMode,
            uiState = uiState,
            editingCrosshairPoint = state.editingCrosshairPoint,
            onMapViewReady = {
                sceneMapView = it
                actions.onSceneMapViewReady(it)
            },
            onMapClick = actions.onSceneMapClick
        ) {
            if (activeDisplayMode.skyDisplayMode.isVisible && isSkyViewportReady) {
                SceneSkyModelLayers(skyState, activeDisplayMode.skyDisplayMode)
            }
        }

        mapSnapshotOverlay?.let { snapshot ->
            Image(
                bitmap = snapshot,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        if (
            state.sceneMode == MapboxSceneMode.VIEW &&
            activeDisplayMode.cameraMode == MapCameraMode.TOP_DOWN
        ) {
            MapCenterPlus(modifier = Modifier.align(Alignment.Center))
        }

        MapControls(
            mapViewportState = mapViewportState,
            uiState = uiState,
            state = MapControlsState(
                dateTimeLocation = uiState.calculationLocation ?: viewport.center,
                canRecenterCurrentLocation = state.canRecenterCurrentLocation,
                displayMode = activeDisplayMode,
                sceneMode = state.sceneMode,
                autoToolState = state.autoToolState,
                hasCompletedEditingTooltips = state.hasCompletedEditingTooltips,
                hasCompletedMainViewTooltips = state.hasCompletedMainViewTooltips
            ),
            actions = MapControlsActions(
                navigation = actions.navigation,
                display = MapDisplayActions(
                    onDisplayModeChanged = { updatedMode ->
                        val constrainedMode = updatedMode.forSceneMode(state.sceneMode)
                        if (activeDisplayMode.cameraPitchDegrees !=
                            constrainedMode.cameraPitchDegrees
                        ) {
                            mapViewportState.setCameraOptions {
                                pitch(constrainedMode.cameraPitchDegrees)
                            }
                        }
                        displayMode = constrainedMode
                    },
                    onRefreshSky = {
                        displayMode = displayMode.copy(skyDisplayMode = SkyDisplayMode.FULL)
                        isSkyViewportReady = false
                        skyRefreshRequest++
                    },
                    onOpenShadowColor = actions.onOpenShadowColor,
                    onStartEditing = {
                        actions.onSceneModeChanged(MapboxSceneMode.EDIT)
                    }
                ),
                editing = actions.editing,
                dateTime = actions.dateTime,
                onEditingTooltipsCompleted = actions.onEditingTooltipsCompleted,
                onMainViewTooltipsCompleted = actions.onMainViewTooltipsCompleted
            )
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
    buildingRenderMode: SceneBuildingRenderMode,
    sceneMode: MapboxSceneMode,
    uiState: ShadowMapUiState,
    editingCrosshairPoint: GeoPoint?,
    onMapViewReady: (MapView) -> Unit,
    onMapClick: (Point) -> Boolean,
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
    LaunchedEffect(sceneMode) {
        mapState.gesturesSettings = GesturesSettings {
            pitchEnabled = sceneMode == MapboxSceneMode.VIEW
        }
    }
    val sunVisible = solarPosition?.isAboveHorizon == true
    val sunAzimuth = solarPosition?.azimuthDegrees ?: Scene3DLighting.DEFAULT_AZIMUTH_DEGREES
    val sunZenith = solarPosition?.zenithDegrees
        ?.coerceIn(Scene3DLighting.MIN_POLAR_ANGLE_DEGREES, Scene3DLighting.MAX_POLAR_ANGLE_DEGREES)
        ?: Scene3DLighting.DEFAULT_POLAR_ANGLE_DEGREES
    val standardSatelliteStyleState = rememberStandardSatelliteStyleState {
        terrainState = TerrainState.DISABLED
        configurationsState.apply {
            val labelVisibility = BooleanValue(Config.SHOW_MAP_LABELS)
            showPlaceLabels = labelVisibility
            showPointOfInterestLabels = labelVisibility
            showRoadLabels = labelVisibility
            showTransitLabels = labelVisibility
        }
    }
    val standardStyleState = rememberStandardStyleState {
        terrainState = TerrainState.DISABLED
        configurationsState.apply {
            val labelVisibility = BooleanValue(Config.SHOW_MAP_LABELS)
            showPlaceLabels = labelVisibility
            showPointOfInterestLabels = labelVisibility
            showRoadLabels = labelVisibility
            showTransitLabels = labelVisibility
        }
    }
    val standardBuildingColorOverride = remember { StandardBuildingColorOverride() }
    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        mapState = mapState,
        onMapClickListener = onMapClick,
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
        val showExtrudedDrawings = buildingRenderMode != SceneBuildingRenderMode.DRAWN_TOP_DOWN
        BuildingExtrusionLayer(
            buildingSource,
            buildingColor,
            buildingRenderMode == SceneBuildingRenderMode.DRAWN_3D
        )
        WallExtrusionLayer(wallSource, wallColor, showExtrudedDrawings)
        TreeExtrusionLayers(
            treeTrunkSource,
            treeCanopySource,
            trunkColor,
            canopyColor,
            showExtrudedDrawings
        )
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
        MapEffect(basemapStyle, buildingRenderMode) { mapView ->
            onMapViewReady(mapView)
            if (!mapView.mapboxMap.isStyleLoaded()) {
                mapView.mapboxMap.styleLoadedEvents.first()
            }
            updateStandardBuildingFootprints(
                mapView = mapView,
                basemapStyle = basemapStyle,
                buildingRenderMode = buildingRenderMode,
                colorOverride = standardBuildingColorOverride
            )
            mapboxNative3dConfig(
                buildingRenderMode == SceneBuildingRenderMode.MAPBOX
            ).forEach { (key, enabled) ->
                mapView.mapboxMap.setStyleImportConfigProperty(
                    Scene3DMapIds.STANDARD_STYLE_IMPORT,
                    key,
                    Value(enabled)
                )
            }
        }
        MapEffect(
            basemapStyle,
            buildingRenderMode,
            sceneMode,
            uiState,
            editingCrosshairPoint
        ) { mapView ->
            if (!mapView.mapboxMap.isStyleLoaded()) {
                mapView.mapboxMap.styleLoadedEvents.first()
            }
            renderMapboxDrawingOverlay(
                mapView = mapView,
                uiState = uiState,
                crosshairPoint = editingCrosshairPoint,
                visible = sceneMode == MapboxSceneMode.EDIT ||
                    buildingRenderMode == SceneBuildingRenderMode.DRAWN_TOP_DOWN
            )
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
private fun WallExtrusionLayer(source: GeoJsonSourceState, color: Color, visible: Boolean) {
    FillExtrusionLayer(sourceState = source, layerId = Scene3DDrawingLayers.WALL_LAYER) {
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionLineWidth = DoubleValue(Scene3DObjectStyle.WALL_WIDTH_METERS)
        fillExtrusionColor = ColorValue(color)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
        visibility = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
    }
}

/** Renders the trunk and canopy extrusions for user-drawn trees. */
@Composable
@OptIn(MapboxExperimental::class)
private fun TreeExtrusionLayers(
    trunkSource: GeoJsonSourceState,
    canopySource: GeoJsonSourceState,
    trunkColor: Color,
    canopyColor: Color,
    visible: Boolean
) {
    FillExtrusionLayer(sourceState = trunkSource, layerId = Scene3DDrawingLayers.TREE_TRUNK_LAYER) {
        fillExtrusionHeight = DoubleValue(Expression.get(Scene3DFeatureProperties.HEIGHT))
        fillExtrusionColor = ColorValue(trunkColor)
        fillExtrusionOpacity = DoubleValue(Scene3DObjectStyle.OBJECT_OPACITY)
        fillExtrusionCastShadows = BooleanValue(true)
        visibility = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
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
        visibility = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
    }
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
    val treeCanopyFeatures = remember(trees) { trees.flatMap(DrawnTree::toCanopyFeatures) }
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
