package com.example.shadowmap.scene

import android.content.Context
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.View.OnAttachStateChangeListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shadowmap.domain.Building
import com.example.shadowmap.domain.DrawnTree
import com.example.shadowmap.domain.DrawnWall
import com.example.shadowmap.domain.SolarPosition
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.android.TextureHelper
import com.google.android.filament.filamat.MaterialBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FilamentBuildingView(
    buildings: List<Building>,
    modifier: Modifier = Modifier,
    walls: List<DrawnWall> = emptyList(),
    trees: List<DrawnTree> = emptyList(),
    viewport: SceneViewport?,
    azimuth: Float,
    zenith: Float,
    sunVisible: Boolean,
    sunPath: List<SolarPosition> = emptyList(),
    skyVisible: Boolean = true,
    skyPadding: SceneSkyPadding = SceneSkyPadding(),
    sunPathWidthPx: Float = 4f,
    sunConnectorWidthPx: Float = 2f,
    compassBandPx: Float = 40f,
    cameraView: SceneCameraView,
    onCameraViewChanged: (SceneCameraView) -> Unit
) {
    val currentOnCameraViewChanged = rememberUpdatedState(onCameraViewChanged)
    val renderer = remember {
        FilamentBuildingRenderer { view -> currentOnCameraViewChanged.value(view) }
    }
    AndroidView(
        factory = { context -> renderer.createSurface(context) },
        modifier = modifier
    )
    LaunchedEffect(buildings, walls, trees, viewport) {
        renderer.setBuildings(buildings, walls, trees, viewport, cameraView)
    }
    LaunchedEffect(cameraView) {
        renderer.setCameraView(cameraView)
    }
    LaunchedEffect(azimuth, zenith, sunVisible) {
        renderer.setSun(azimuth, zenith, sunVisible)
    }
    LaunchedEffect(sunPath, skyVisible) {
        renderer.setSky(sunPath, skyVisible)
    }
    LaunchedEffect(skyPadding) {
        renderer.setSkyPadding(skyPadding)
    }
    LaunchedEffect(sunPathWidthPx) {
        renderer.setSunPathWidth(sunPathWidthPx)
    }
    LaunchedEffect(sunConnectorWidthPx) {
        renderer.setSunConnectorWidth(sunConnectorWidthPx)
    }
    LaunchedEffect(compassBandPx) {
        renderer.setCompassBand(compassBandPx)
    }
    DisposableEffect(renderer) { onDispose(renderer::destroy) }
}

@Suppress("TooManyFunctions", "LargeClass")
private class FilamentBuildingRenderer(
    private val onCameraViewChanged: (SceneCameraView) -> Unit
) : Choreographer.FrameCallback {
    private val engine = Engine.create()
    private val filamentRenderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity = EntityManager.get().create()
    private val camera = engine.createCamera(cameraEntity)
    private val sunEntity = EntityManager.get().create()
    private val fillLightEntity = EntityManager.get().create()
    private val choreographer = Choreographer.getInstance()
    private var swapChain: SwapChain? = null
    private var displayHelper: DisplayHelper? = null
    private var renderableEntity = 0
    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var roofMaterial: Material? = null
    private var wallMaterial: Material? = null
    private var groundMaterial: Material? = null
    private var skyMaterial: Material? = null
    private var pathMaterial: Material? = null
    private var markerMaterial: Material? = null
    private var sunBodyMaterial: Material? = null
    private var compassMaterial: Material? = null
    private var compassTexture: Texture? = null
    private var skyRenderableEntity = 0
    private var pathRenderableEntity = 0
    private var markerRenderableEntity = 0
    private var sunBodyRenderableEntity = 0
    private var compassRenderableEntity = 0
    private var skyVertexBuffer: VertexBuffer? = null
    private var skyIndexBuffer: IndexBuffer? = null
    private var pathVertexBuffer: VertexBuffer? = null
    private var pathIndexBuffer: IndexBuffer? = null
    private var markerVertexBuffer: VertexBuffer? = null
    private var markerIndexBuffer: IndexBuffer? = null
    private var sunBodyVertexBuffer: VertexBuffer? = null
    private var sunBodyIndexBuffer: IndexBuffer? = null
    private var compassVertexBuffer: VertexBuffer? = null
    private var compassIndexBuffer: IndexBuffer? = null
    private var destroyed = false
    private var sceneRadius = Scene3DCamera.DEFAULT_SCENE_RADIUS_METERS
    private var cameraYaw = Scene3DCamera.DEFAULT_YAW_DEGREES
    private var cameraPitch = Scene3DCamera.DEFAULT_PITCH_DEGREES
    private var cameraDistance = Scene3DCamera.DEFAULT_DISTANCE_METERS
    private var cameraTargetX = 0f
    private var cameraTargetZ = 0f
    private var alignedTopDown = true
    private var orthographicZoom = Scene3DCamera.DEFAULT_ORTHOGRAPHIC_ZOOM
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var sceneViewport: SceneViewport? = null
    private var currentAzimuth = Scene3DAppearance.DEFAULT_SUN_AZIMUTH_DEGREES
    private var currentZenith = Scene3DAppearance.DEFAULT_SUN_ZENITH_DEGREES
    private var currentSunVisible = true
    private var currentSunPath = emptyList<SolarPosition>()
    private var skyVisible = true
    private var skyPadding = SceneSkyPadding()
    private var sunPathWidthPx = 4f
    private var sunConnectorWidthPx = 2f
    private var compassBandPx = 40f

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        isOpaque = false
        isMediaOverlay = true
        renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: android.view.Surface) {
                swapChain?.let(engine::destroySwapChain)
                swapChain = engine.createSwapChain(surface, swapChainFlags)
            }

            override fun onDetachedFromSurface() {
                swapChain?.let(engine::destroySwapChain)
                swapChain = null
                engine.flushAndWait()
            }

            override fun onResized(width: Int, height: Int) {
                viewportWidth = width
                viewportHeight = height
                view.viewport = Viewport(0, 0, width, height)
                updateProjection()
                rebuildSky()
            }
        }
    }

    init {
        view.scene = scene
        view.camera = camera
        view.blendMode = View.BlendMode.TRANSLUCENT
        view.isPostProcessingEnabled = true
        filamentRenderer.clearOptions = filamentRenderer.clearOptions.apply {
            clear = true
            clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        }
        val sunColor = Scene3DAppearance.SUN_COLOR
        LightManager.Builder(LightManager.Type.SUN)
            .color(sunColor.red, sunColor.green, sunColor.blue)
            .intensity(Scene3DAppearance.SUN_INTENSITY)
            .castShadows(true)
            .sunAngularRadius(Scene3DAppearance.SUN_ANGULAR_RADIUS)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)
        val fillColor = Scene3DAppearance.FILL_LIGHT_COLOR
        val fillDirection = Scene3DAppearance.FILL_LIGHT_DIRECTION
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(fillColor.red, fillColor.green, fillColor.blue)
            .intensity(Scene3DAppearance.FILL_LIGHT_INTENSITY)
            .direction(fillDirection.x, fillDirection.y, fillDirection.z)
            .castShadows(false)
            .build(engine, fillLightEntity)
        scene.addEntity(fillLightEntity)
        setSun(
            Scene3DAppearance.DEFAULT_SUN_AZIMUTH_DEGREES,
            Scene3DAppearance.DEFAULT_SUN_ZENITH_DEGREES,
            visible = true
        )
        choreographer.postFrameCallback(this)
    }

    fun createSurface(context: Context): SurfaceView = GestureSurfaceView(context).also { surface ->
        displayHelper = DisplayHelper(context)
        surface.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: android.view.View) {
                    view.display?.let { display ->
                        displayHelper?.attach(filamentRenderer, display)
                    }
                }

                override fun onViewDetachedFromWindow(view: android.view.View) {
                    displayHelper?.detach()
                }
            }
        )
        val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    orthographicZoom = (orthographicZoom / detector.scaleFactor)
                        .coerceIn(
                            Scene3DCamera.MIN_ORTHOGRAPHIC_ZOOM,
                            Scene3DCamera.MAX_ORTHOGRAPHIC_ZOOM
                        )
                    updateProjection()
                    rebuildSunPath()
                    updateCamera()
                    return true
                }
            }
        )
        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onScroll(
                    first: MotionEvent?,
                    current: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    if (scaleDetector.isInProgress) {
                        return true
                    } else if (current.pointerCount >= 2) {
                        panCamera(distanceX, distanceY)
                    } else {
                        if (alignedTopDown) {
                            alignedTopDown = false
                            onCameraViewChanged(SceneCameraView.ORBIT)
                        }
                        cameraYaw =
                            (cameraYaw - distanceX * Scene3DCamera.ORBIT_DEGREES_PER_PIXEL) % 360f
                        cameraPitch =
                            (cameraPitch + distanceY * Scene3DCamera.ORBIT_DEGREES_PER_PIXEL)
                                .coerceIn(
                                    Scene3DCamera.MIN_PITCH_DEGREES,
                                    Scene3DCamera.MAX_PITCH_DEGREES
                                )
                        updateCamera()
                    }
                    return true
                }

                override fun onDoubleTap(event: MotionEvent): Boolean {
                    resetToInitialOrbit()
                    onCameraViewChanged(SceneCameraView.ORBIT)
                    return true
                }
            }
        )
        surface.isClickable = true
        surface.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP) surface.performClick()
            true
        }
        uiHelper.attachTo(surface)
    }

    @Suppress("LongMethod")
    fun setBuildings(
        buildings: List<Building>,
        walls: List<DrawnWall>,
        trees: List<DrawnTree>,
        viewport: SceneViewport?,
        cameraView: SceneCameraView
    ) {
        if (destroyed) return
        clearMesh()
        sceneViewport = viewport
        val mesh = BuildingMeshGenerator.generate(buildings, walls, trees, viewport)
        if (mesh.indices.isEmpty()) return
        val floatsPerVertex = 7
        val vertexBytes = ByteBuffer.allocateDirect(
            mesh.vertices.size * floatsPerVertex * Float.SIZE_BYTES
        )
            .order(ByteOrder.nativeOrder())
        mesh.vertices.forEach { vertex ->
            val tangent = normalToQuaternion(vertex.normalX, vertex.normalY, vertex.normalZ)
            vertexBytes.putFloat(vertex.x).putFloat(vertex.y).putFloat(vertex.z)
            tangent.forEach(vertexBytes::putFloat)
        }
        vertexBytes.flip()
        val indexBytes = ByteBuffer.allocateDirect(mesh.indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        mesh.indices.forEach(indexBytes::putInt)
        indexBytes.flip()
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(mesh.vertices.size)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .attribute(
                VertexBuffer.VertexAttribute.TANGENTS,
                0,
                VertexBuffer.AttributeType.FLOAT4,
                3 * Float.SIZE_BYTES,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .build(engine)
            .also { it.setBufferAt(engine, 0, vertexBytes) }
        indexBuffer = IndexBuffer.Builder()
            .indexCount(mesh.indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)
            .also { it.setBuffer(engine, indexBytes) }
        roofMaterial = createRoofMaterial()
        wallMaterial = createWallMaterial()
        groundMaterial = createGroundMaterial()
        renderableEntity = EntityManager.get().create()
        RenderableManager.Builder(3)
            .boundingBox(Box(0f, 50f, 0f, mesh.radiusMeters, 100f, mesh.radiusMeters))
            .material(0, checkNotNull(roofMaterial).defaultInstance)
            .material(1, checkNotNull(wallMaterial).defaultInstance)
            .material(2, checkNotNull(groundMaterial).defaultInstance)
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                checkNotNull(vertexBuffer),
                checkNotNull(indexBuffer),
                0,
                mesh.wallIndexOffset
            )
            .geometry(
                1,
                RenderableManager.PrimitiveType.TRIANGLES,
                checkNotNull(vertexBuffer),
                checkNotNull(indexBuffer),
                mesh.wallIndexOffset,
                mesh.groundIndexOffset - mesh.wallIndexOffset
            )
            .geometry(
                2,
                RenderableManager.PrimitiveType.TRIANGLES,
                checkNotNull(vertexBuffer),
                checkNotNull(indexBuffer),
                mesh.groundIndexOffset,
                mesh.indices.size - mesh.groundIndexOffset
            )
            .castShadows(true)
            .receiveShadows(true)
            .culling(false)
            .build(engine, renderableEntity)
        scene.addEntity(renderableEntity)
        sceneRadius = max(mesh.radiusMeters, Scene3DCamera.MIN_SCENE_RADIUS_METERS)
        rebuildSky()
        when (cameraView) {
            SceneCameraView.ORBIT -> resetToInitialOrbit()
            SceneCameraView.TOP_DOWN -> resetToTopDown()
        }
    }

    fun setSun(azimuth: Float, zenith: Float, visible: Boolean) {
        if (destroyed) return
        currentAzimuth = azimuth
        currentZenith = zenith
        currentSunVisible = visible
        val direction = sunLightDirection(azimuth, zenith)
        val instance = engine.lightManager.getInstance(sunEntity)
        engine.lightManager.setDirection(instance, direction.x, direction.y, direction.z)
        engine.lightManager.setIntensity(
            instance,
            if (visible) Scene3DAppearance.SUN_INTENSITY else 0f
        )
        rebuildSunMarker()
    }

    fun setSky(sunPath: List<SolarPosition>, visible: Boolean) {
        val visibilityChanged = skyVisible != visible
        currentSunPath = sunPath
        skyVisible = visible
        if (visibilityChanged) rebuildSky() else rebuildSunPath()
    }

    fun setSkyPadding(padding: SceneSkyPadding) {
        skyPadding = padding
        rebuildSky()
    }

    fun setSunPathWidth(widthPx: Float) {
        sunPathWidthPx = widthPx.coerceAtLeast(1f)
        rebuildSunPath()
    }

    fun setSunConnectorWidth(widthPx: Float) {
        sunConnectorWidthPx = widthPx.coerceAtLeast(1f)
        rebuildSunMarker()
    }

    fun setCompassBand(widthPx: Float) {
        compassBandPx = widthPx.coerceAtLeast(0f)
        rebuildSky()
    }

    private fun rebuildSky() {
        val viewport = sceneViewport ?: return
        if (!skyVisible || viewportWidth <= 1 || viewportHeight <= 1) {
            clearSky()
            return
        }
        val frame = SceneSkyGeometry.calculateFrame(
            viewport,
            viewportWidth,
            viewportHeight,
            skyPadding,
            compassBandPx = compassBandPx
        )
        val dome = SceneSkyGeometry.domeMesh(frame.radiusMeters, viewport)
        val compass = SceneSkyGeometry.compassMesh(frame.radiusMeters, viewport)
        clearSky()
        skyMaterial = createLineMaterial("sky_guides", Scene3DAppearance.SKY_GUIDE_COLOR)
        pathMaterial = createLineMaterial("sun_path", Scene3DAppearance.SUN_PATH_COLOR)
        markerMaterial = createLineMaterial("current_sun", Scene3DAppearance.CURRENT_SUN_COLOR)
        sunBodyMaterial = createSunBodyMaterial()
        createLineRenderable(dome + compass, skyMaterial!!).also {
            skyRenderableEntity = it.entity
            skyVertexBuffer = it.vertexBuffer
            skyIndexBuffer = it.indexBuffer
        }
        rebuildCompass(frame)
        rebuildSunPath(frame.radiusMeters)
        rebuildSunMarker(frame.radiusMeters)
    }

    private fun rebuildCompass(frame: SceneSkyFrame) {
        clearCompass()
        val ratio = frame.radiusMeters / frame.outerRadiusMeters
        val bitmap = CompassDialBitmap.create(ratio)
        compassTexture = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(0xff)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)
            .usage(Texture.Usage.DEFAULT or Texture.Usage.GEN_MIPMAPPABLE)
            .build(engine)
            .also { texture ->
                TextureHelper.setBitmap(engine, texture, 0, bitmap)
                texture.generateMipmaps(engine)
            }
        compassMaterial = createCompassMaterial().also { material ->
            material.defaultInstance.setParameter(
                "compassTexture",
                checkNotNull(compassTexture),
                TextureSampler(
                    TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
                    TextureSampler.MagFilter.LINEAR,
                    TextureSampler.WrapMode.CLAMP_TO_EDGE
                )
            )
        }
        createCompassRenderable(
            outerRadiusMeters = frame.outerRadiusMeters,
            viewport = checkNotNull(sceneViewport),
            material = checkNotNull(compassMaterial)
        )
    }

    private fun createCompassMaterial(): Material {
        val materialPackage = MaterialBuilder()
            .name("compass_dial")
            .platform(MaterialBuilder.Platform.MOBILE)
            .targetApi(MaterialBuilder.TargetApi.OPENGL)
            .shading(MaterialBuilder.Shading.UNLIT)
            .require(MaterialBuilder.VertexAttribute.UV0)
            .doubleSided(true)
            .samplerParameter(
                MaterialBuilder.SamplerType.SAMPLER_2D,
                MaterialBuilder.SamplerFormat.FLOAT,
                MaterialBuilder.ParameterPrecision.DEFAULT,
                "compassTexture"
            )
            .blending(MaterialBuilder.BlendingMode.TRANSPARENT)
            .depthWrite(false)
            .material(
                """
                void material(inout MaterialInputs material) {
                    prepareMaterial(material);
                    material.baseColor = texture(materialParams_compassTexture, getUV0());
                }
                """.trimIndent()
            )
            .build(engine)
        check(materialPackage.isValid) { "Filament could not compile the compass material" }
        val packageBuffer = materialPackage.buffer
        return Material.Builder().payload(packageBuffer, packageBuffer.remaining()).build(engine)
    }

    private fun createCompassRenderable(
        outerRadiusMeters: Float,
        viewport: SceneViewport,
        material: Material
    ) {
        val rightX = viewport.screenRightX
        val rightZ = viewport.screenRightZ
        val downX = viewport.screenDownX
        val downZ = viewport.screenDownZ
        fun point(right: Float, down: Float): ScenePoint3 = ScenePoint3(
            x = rightX * right + downX * down,
            y = COMPASS_GROUND_OFFSET_METERS,
            z = rightZ * right + downZ * down
        )
        val points = listOf(
            point(-outerRadiusMeters, -outerRadiusMeters),
            point(outerRadiusMeters, -outerRadiusMeters),
            point(outerRadiusMeters, outerRadiusMeters),
            point(-outerRadiusMeters, outerRadiusMeters)
        )
        // Android bitmaps use a top-left origin, while Filament samples V from the bottom.
        val textureCoordinates = listOf(0f to 1f, 1f to 1f, 1f to 0f, 0f to 0f)
        val floatsPerVertex = 5
        val vertexBytes = ByteBuffer.allocateDirect(
            points.size * floatsPerVertex * Float.SIZE_BYTES
        ).order(ByteOrder.nativeOrder())
        points.zip(textureCoordinates).forEach { (point, uv) ->
            vertexBytes.putFloat(point.x).putFloat(point.y).putFloat(point.z)
            vertexBytes.putFloat(uv.first).putFloat(uv.second)
        }
        vertexBytes.flip()
        val indices = listOf(0, 2, 1, 0, 3, 2)
        val indexBytes = ByteBuffer.allocateDirect(indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        indices.forEach(indexBytes::putInt)
        indexBytes.flip()
        compassVertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(points.size)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .attribute(
                VertexBuffer.VertexAttribute.UV0,
                0,
                VertexBuffer.AttributeType.FLOAT2,
                3 * Float.SIZE_BYTES,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .build(engine)
            .also { it.setBufferAt(engine, 0, vertexBytes) }
        compassIndexBuffer = IndexBuffer.Builder()
            .indexCount(indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)
            .also { it.setBuffer(engine, indexBytes) }
        compassRenderableEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(
                Box(
                    0f,
                    COMPASS_GROUND_OFFSET_METERS,
                    0f,
                    outerRadiusMeters,
                    0.1f,
                    outerRadiusMeters
                )
            )
            .material(0, material.defaultInstance)
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                checkNotNull(compassVertexBuffer),
                checkNotNull(compassIndexBuffer),
                0,
                indices.size
            )
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .build(engine, compassRenderableEntity)
        scene.addEntity(compassRenderableEntity)
    }

    private fun rebuildSunPath(radiusMeters: Float? = null) {
        clearLine(pathRenderableEntity, pathVertexBuffer, pathIndexBuffer)
        pathRenderableEntity = 0
        pathVertexBuffer = null
        pathIndexBuffer = null
        val hasRenderableSurface = viewportWidth > 1 && viewportHeight > 1
        val canRenderPath = skyVisible && pathMaterial != null && hasRenderableSurface
        if (!canRenderPath) return
        val viewport = sceneViewport ?: return
        val radius = radiusMeters ?: SceneSkyGeometry.calculateFrame(
            viewport,
            viewportWidth,
            viewportHeight,
            skyPadding,
            compassBandPx = compassBandPx
        ).radiusMeters
        val horizontalMetersPerPixel =
            viewport.widthMeters * orthographicZoom / viewportWidth.coerceAtLeast(1)
        val verticalMetersPerPixel =
            viewport.heightMeters * orthographicZoom / viewportHeight.coerceAtLeast(1)
        val pathWidthMeters = max(horizontalMetersPerPixel, verticalMetersPerPixel) * sunPathWidthPx
        val path = SceneSkyGeometry.sunPathRibbonMesh(
            positions = currentSunPath,
            radiusMeters = radius,
            viewport = viewport,
            widthMeters = pathWidthMeters,
            surfaceOffsetMeters = max(pathWidthMeters * 0.12f, radius * 0.001f)
        )
        if (path.indices.isNotEmpty()) {
            createTriangleRenderable(path, pathMaterial!!).also {
                pathRenderableEntity = it.entity
                pathVertexBuffer = it.vertexBuffer
                pathIndexBuffer = it.indexBuffer
            }
        }
    }

    private fun rebuildSunMarker(radiusMeters: Float? = null) {
        if (!skyVisible) return
        val viewport = sceneViewport ?: return
        val radius = radiusMeters ?: SceneSkyGeometry.calculateFrame(
            viewport,
            viewportWidth,
            viewportHeight,
            skyPadding,
            compassBandPx = compassBandPx
        ).radiusMeters
        clearLine(markerRenderableEntity, markerVertexBuffer, markerIndexBuffer)
        clearLine(sunBodyRenderableEntity, sunBodyVertexBuffer, sunBodyIndexBuffer)
        markerRenderableEntity = 0
        sunBodyRenderableEntity = 0
        markerVertexBuffer = null
        markerIndexBuffer = null
        sunBodyVertexBuffer = null
        sunBodyIndexBuffer = null
        if (currentSunVisible && markerMaterial != null && sunBodyMaterial != null) {
            val center = SceneSkyGeometry.pointOnDome(
                currentAzimuth,
                90f - currentZenith,
                radius,
                viewport
            )
            val bodyRadius = max(radius * 0.025f, 0.6f)
            val metersPerPixel = max(
                viewport.widthMeters * orthographicZoom / viewportWidth.coerceAtLeast(1),
                viewport.heightMeters * orthographicZoom / viewportHeight.coerceAtLeast(1)
            )
            val marker = SceneSkyGeometry.connectorTubeMesh(
                start = ScenePoint3(0f, 0f, 0f),
                end = center,
                diameterMeters = metersPerPixel * sunConnectorWidthPx
            )
            createTriangleRenderable(marker, markerMaterial!!).also {
                markerRenderableEntity = it.entity
                markerVertexBuffer = it.vertexBuffer
                markerIndexBuffer = it.indexBuffer
            }
            createTriangleRenderable(
                SceneSkyGeometry.sunSphereMesh(center, bodyRadius),
                sunBodyMaterial!!
            ).also {
                sunBodyRenderableEntity = it.entity
                sunBodyVertexBuffer = it.vertexBuffer
                sunBodyIndexBuffer = it.indexBuffer
            }
        }
    }

    private data class LineResources(
        val entity: Int,
        val vertexBuffer: VertexBuffer,
        val indexBuffer: IndexBuffer
    )

    private fun createLineRenderable(
        mesh: SceneLineMesh,
        material: Material
    ): LineResources {
        val floatsPerVertex = 7
        val vertexBytes = ByteBuffer.allocateDirect(
            mesh.vertices.size * floatsPerVertex * Float.SIZE_BYTES
        ).order(ByteOrder.nativeOrder())
        mesh.vertices.forEach { point ->
            vertexBytes.putFloat(point.x).putFloat(point.y).putFloat(point.z)
            normalToQuaternion(0f, 1f, 0f).forEach(vertexBytes::putFloat)
        }
        vertexBytes.flip()
        val indexBytes = ByteBuffer.allocateDirect(mesh.indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        mesh.indices.forEach(indexBytes::putInt)
        indexBytes.flip()
        val vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(mesh.vertices.size)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .attribute(
                VertexBuffer.VertexAttribute.TANGENTS,
                0,
                VertexBuffer.AttributeType.FLOAT4,
                3 * Float.SIZE_BYTES,
                floatsPerVertex * Float.SIZE_BYTES
            )
            .build(engine)
            .also { it.setBufferAt(engine, 0, vertexBytes) }
        val indexBuffer = IndexBuffer.Builder()
            .indexCount(mesh.indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)
            .also { it.setBuffer(engine, indexBytes) }
        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-100f, -1f, -100f, 100f, 100f, 100f))
            .material(0, material.defaultInstance)
            .geometry(0, RenderableManager.PrimitiveType.LINES, vertexBuffer, indexBuffer, 0, mesh.indices.size)
            .culling(false)
            .build(engine, entity)
        scene.addEntity(entity)
        return LineResources(entity, vertexBuffer, indexBuffer)
    }

    private fun createTriangleRenderable(
        mesh: SceneTriangleMesh,
        material: Material
    ): LineResources {
        val vertexBytes = ByteBuffer.allocateDirect(
            mesh.vertices.size * 3 * Float.SIZE_BYTES
        ).order(ByteOrder.nativeOrder())
        mesh.vertices.forEach { point ->
            vertexBytes.putFloat(point.x).putFloat(point.y).putFloat(point.z)
        }
        vertexBytes.flip()
        val indexBytes = ByteBuffer.allocateDirect(mesh.indices.size * Int.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        mesh.indices.forEach(indexBytes::putInt)
        indexBytes.flip()
        val vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(mesh.vertices.size)
            .attribute(
                VertexBuffer.VertexAttribute.POSITION,
                0,
                VertexBuffer.AttributeType.FLOAT3,
                0,
                3 * Float.SIZE_BYTES
            )
            .build(engine)
            .also { it.setBufferAt(engine, 0, vertexBytes) }
        val indexBuffer = IndexBuffer.Builder()
            .indexCount(mesh.indices.size)
            .bufferType(IndexBuffer.Builder.IndexType.UINT)
            .build(engine)
            .also { it.setBuffer(engine, indexBytes) }
        val entity = EntityManager.get().create()
        val minX = mesh.vertices.minOf(ScenePoint3::x)
        val minY = mesh.vertices.minOf(ScenePoint3::y)
        val minZ = mesh.vertices.minOf(ScenePoint3::z)
        val maxX = mesh.vertices.maxOf(ScenePoint3::x)
        val maxY = mesh.vertices.maxOf(ScenePoint3::y)
        val maxZ = mesh.vertices.maxOf(ScenePoint3::z)
        RenderableManager.Builder(1)
            .boundingBox(
                Box(
                    (minX + maxX) / 2f,
                    (minY + maxY) / 2f,
                    (minZ + maxZ) / 2f,
                    max((maxX - minX) / 2f, 0.01f),
                    max((maxY - minY) / 2f, 0.01f),
                    max((maxZ - minZ) / 2f, 0.01f)
                )
            )
            .material(0, material.defaultInstance)
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                indexBuffer,
                0,
                mesh.indices.size
            )
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .build(engine, entity)
        scene.addEntity(entity)
        return LineResources(entity, vertexBuffer, indexBuffer)
    }

    private fun createLineMaterial(name: String, color: SceneRgba): Material = createMaterial(
        name = name,
        source =
            """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                material.baseColor = ${color.toShaderFloat4()};
            }
            """.trimIndent(),
        transparent = true,
        unlit = true
    )

    private fun createSunBodyMaterial(): Material = createMaterial(
        name = "sun_body",
        source =
            """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                material.baseColor = ${Scene3DAppearance.SUN_BODY_COLOR.toShaderFloat4()};
            }
            """.trimIndent(),
        unlit = true
    )

    private fun clearSky() {
        clearCompass()
        clearLine(skyRenderableEntity, skyVertexBuffer, skyIndexBuffer)
        clearLine(pathRenderableEntity, pathVertexBuffer, pathIndexBuffer)
        clearLine(markerRenderableEntity, markerVertexBuffer, markerIndexBuffer)
        clearLine(sunBodyRenderableEntity, sunBodyVertexBuffer, sunBodyIndexBuffer)
        skyRenderableEntity = 0
        pathRenderableEntity = 0
        markerRenderableEntity = 0
        sunBodyRenderableEntity = 0
        skyVertexBuffer = null
        skyIndexBuffer = null
        pathVertexBuffer = null
        pathIndexBuffer = null
        markerVertexBuffer = null
        markerIndexBuffer = null
        sunBodyVertexBuffer = null
        sunBodyIndexBuffer = null
        skyMaterial?.let(engine::destroyMaterial)
        pathMaterial?.let(engine::destroyMaterial)
        markerMaterial?.let(engine::destroyMaterial)
        sunBodyMaterial?.let(engine::destroyMaterial)
        skyMaterial = null
        pathMaterial = null
        markerMaterial = null
        sunBodyMaterial = null
    }

    private fun clearCompass() {
        clearLine(compassRenderableEntity, compassVertexBuffer, compassIndexBuffer)
        compassRenderableEntity = 0
        compassVertexBuffer = null
        compassIndexBuffer = null
        compassMaterial?.let(engine::destroyMaterial)
        compassTexture?.let(engine::destroyTexture)
        compassMaterial = null
        compassTexture = null
    }

    private fun clearLine(entity: Int, vertexBuffer: VertexBuffer?, indexBuffer: IndexBuffer?) {
        if (entity != 0) {
            scene.removeEntity(entity)
            engine.destroyEntity(entity)
            EntityManager.get().destroy(entity)
        }
        vertexBuffer?.let(engine::destroyVertexBuffer)
        indexBuffer?.let(engine::destroyIndexBuffer)
    }

    private fun createRoofMaterial(): Material = createMaterial(
        name = "building_roofs",
        source =
            """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                material.baseColor = ${Scene3DAppearance.ROOF_COLOR.toShaderFloat4()};
                material.roughness = ${Scene3DAppearance.ROOF_ROUGHNESS};
            }
            """.trimIndent()
    )

    private fun createWallMaterial(): Material = createMaterial(
        name = "building_walls",
        source =
            """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                material.baseColor = ${Scene3DAppearance.WALL_COLOR.toShaderFloat4()};
                material.roughness = ${Scene3DAppearance.WALL_ROUGHNESS};
            }
            """.trimIndent()
    )

    private fun createGroundMaterial(): Material = createMaterial(
        name = "ground",
        source =
            """
            void material(inout MaterialInputs material) {
                prepareMaterial(material);
                material.baseColor = ${Scene3DAppearance.groundShaderColor()};
                material.roughness = ${Scene3DAppearance.GROUND_ROUGHNESS};
            }
            """.trimIndent(),
        transparent = true
    )

    private fun createMaterial(
        name: String,
        source: String,
        transparent: Boolean = false,
        unlit: Boolean = false
    ): Material {
        val builder = MaterialBuilder()
            .name(name)
            .platform(MaterialBuilder.Platform.MOBILE)
            .targetApi(MaterialBuilder.TargetApi.OPENGL)
            .shading(if (unlit) MaterialBuilder.Shading.UNLIT else MaterialBuilder.Shading.LIT)
            .doubleSided(true)
            .material(source)
        if (transparent) {
            builder.blending(MaterialBuilder.BlendingMode.TRANSPARENT).depthWrite(false)
        }
        val materialPackage = builder.build(engine)
        check(materialPackage.isValid) { "Filament could not compile the building material" }
        val packageBuffer = materialPackage.buffer
        return Material.Builder().payload(packageBuffer, packageBuffer.remaining()).build(engine)
    }

    fun setCameraView(cameraView: SceneCameraView) {
        when (cameraView) {
            SceneCameraView.ORBIT -> if (alignedTopDown) resetToInitialOrbit()
            SceneCameraView.TOP_DOWN -> if (!alignedTopDown) resetToTopDown()
        }
    }

    private fun resetToInitialOrbit() {
        alignedTopDown = false
        orthographicZoom = Scene3DCamera.DEFAULT_ORTHOGRAPHIC_ZOOM
        cameraYaw = Scene3DCamera.DEFAULT_YAW_DEGREES
        cameraPitch = Scene3DCamera.DEFAULT_PITCH_DEGREES
        cameraDistance = Scene3DCamera.orbitDistance(sceneRadius, viewportRadius())
        cameraTargetX = 0f
        cameraTargetZ = 0f
        updateProjection()
        rebuildSunPath()
        updateCamera()
    }

    private fun resetToTopDown() {
        alignedTopDown = true
        orthographicZoom = Scene3DCamera.DEFAULT_ORTHOGRAPHIC_ZOOM
        cameraYaw = Scene3DCamera.DEFAULT_YAW_DEGREES
        cameraPitch = Scene3DCamera.DEFAULT_PITCH_DEGREES
        cameraDistance = Scene3DCamera.orbitDistance(sceneRadius, viewportRadius())
        cameraTargetX = 0f
        cameraTargetZ = 0f
        updateProjection()
        rebuildSunPath()
        updateCamera()
    }

    private fun viewportRadius(): Float {
        val viewport = sceneViewport ?: return sceneRadius
        val halfWidth = viewport.widthMeters / 2f
        val halfHeight = viewport.heightMeters / 2f
        return sqrt(halfWidth * halfWidth + halfHeight * halfHeight)
    }

    private fun panCamera(distanceX: Float, distanceY: Float) {
        val viewport = sceneViewport ?: return
        if (alignedTopDown) {
            val screenX = distanceX * viewport.widthMeters * orthographicZoom / viewportWidth
            val screenY = distanceY * viewport.heightMeters * orthographicZoom / viewportHeight
            cameraTargetX += viewport.screenRightX * screenX + viewport.screenDownX * screenY
            cameraTargetZ += viewport.screenRightZ * screenX + viewport.screenDownZ * screenY
        } else {
            val yawRadians = Math.toRadians(cameraYaw.toDouble())
            val horizontalMetersPerPixel = Scene3DCamera.orthographicMetersPerPixel(
                viewport.widthMeters,
                orthographicZoom,
                viewportWidth
            )
            val verticalMetersPerPixel = Scene3DCamera.orthographicMetersPerPixel(
                viewport.heightMeters,
                orthographicZoom,
                viewportHeight
            )
            val rightX = cos(yawRadians).toFloat()
            val rightZ = -sin(yawRadians).toFloat()
            val forwardX = sin(yawRadians).toFloat()
            val forwardZ = cos(yawRadians).toFloat()
            cameraTargetX += rightX * distanceX * horizontalMetersPerPixel +
                forwardX * distanceY * verticalMetersPerPixel
            cameraTargetZ += rightZ * distanceX * horizontalMetersPerPixel +
                forwardZ * distanceY * verticalMetersPerPixel
            val targetLimit =
                max(sceneRadius, viewportRadius()) * Scene3DCamera.TARGET_LIMIT_MULTIPLIER
            cameraTargetX = cameraTargetX.coerceIn(-targetLimit, targetLimit)
            cameraTargetZ = cameraTargetZ.coerceIn(-targetLimit, targetLimit)
        }
        updateCamera()
    }

    private fun updateCamera() {
        if (alignedTopDown) {
            val viewport = sceneViewport ?: return
            camera.lookAt(
                cameraTargetX.toDouble(),
                max(
                    sceneRadius * Scene3DCamera.TOP_DOWN_HEIGHT_MULTIPLIER,
                    Scene3DCamera.MIN_TOP_DOWN_HEIGHT_METERS
                ).toDouble(),
                cameraTargetZ.toDouble(),
                cameraTargetX.toDouble(),
                0.0,
                cameraTargetZ.toDouble(),
                -viewport.screenDownX.toDouble(),
                0.0,
                -viewport.screenDownZ.toDouble()
            )
            return
        }
        val yaw = Math.toRadians(cameraYaw.toDouble())
        val pitch = Math.toRadians(cameraPitch.toDouble())
        val horizontalDistance = cameraDistance * cos(pitch)
        val eyeX = cameraTargetX + horizontalDistance * sin(yaw)
        val eyeY = cameraDistance * sin(pitch)
        val eyeZ = cameraTargetZ + horizontalDistance * cos(yaw)
        camera.lookAt(
            eyeX,
            eyeY,
            eyeZ,
            cameraTargetX.toDouble(),
            0.0,
            cameraTargetZ.toDouble(),
            0.0,
            1.0,
            0.0
        )
    }

    private fun updateProjection() {
        val viewport = sceneViewport ?: return
        val halfWidth = viewport.widthMeters * orthographicZoom / 2.0
        val halfHeight = viewport.heightMeters * orthographicZoom / 2.0
        camera.setProjection(
            com.google.android.filament.Camera.Projection.ORTHO,
            -halfWidth,
            halfWidth,
            -halfHeight,
            halfHeight,
            Scene3DCamera.NEAR_CLIP_METERS,
            Scene3DCamera.FAR_CLIP_METERS
        )
    }

    /** Quaternion rotating Filament's canonical +Z normal onto the supplied surface normal. */
    private fun normalToQuaternion(nx: Float, ny: Float, nz: Float): FloatArray {
        if (nz < -0.9999f) return floatArrayOf(1f, 0f, 0f, 0f)
        val inverseLength = 1f / sqrt(nx * nx + ny * ny + (1f + nz) * (1f + nz))
        return floatArrayOf(-ny * inverseLength, nx * inverseLength, 0f, (1f + nz) * inverseLength)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (destroyed) return
        val chain = swapChain
        if (chain != null && uiHelper.isReadyToRender &&
            filamentRenderer.beginFrame(chain, frameTimeNanos)
        ) {
            filamentRenderer.render(view)
            filamentRenderer.endFrame()
        }
        choreographer.postFrameCallback(this)
    }

    private fun clearMesh() {
        clearSky()
        if (renderableEntity != 0) {
            scene.removeEntity(renderableEntity)
            engine.destroyEntity(renderableEntity)
            EntityManager.get().destroy(renderableEntity)
            renderableEntity = 0
        }
        vertexBuffer?.let(engine::destroyVertexBuffer)
        indexBuffer?.let(engine::destroyIndexBuffer)
        roofMaterial?.let(engine::destroyMaterial)
        wallMaterial?.let(engine::destroyMaterial)
        groundMaterial?.let(engine::destroyMaterial)
        vertexBuffer = null
        indexBuffer = null
        roofMaterial = null
        wallMaterial = null
        groundMaterial = null
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        choreographer.removeFrameCallback(this)
        uiHelper.detach()
        displayHelper?.detach()
        clearMesh()
        scene.removeEntity(sunEntity)
        engine.destroyEntity(sunEntity)
        EntityManager.get().destroy(sunEntity)
        scene.removeEntity(fillLightEntity)
        engine.destroyEntity(fillLightEntity)
        EntityManager.get().destroy(fillLightEntity)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(filamentRenderer)
        engine.destroy()
    }
}

private class GestureSurfaceView(context: Context) : SurfaceView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

private const val COMPASS_GROUND_OFFSET_METERS = 0.04f
