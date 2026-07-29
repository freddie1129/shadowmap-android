package com.gooludou.shadowplanner.scene

import com.gooludou.shadowplanner.core.solar.SolarPosition
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ScenePoint3(val x: Float, val y: Float, val z: Float)

data class SceneLineMesh(val vertices: List<ScenePoint3>, val indices: List<Int>)

data class SceneTriangleMesh(val vertices: List<ScenePoint3>, val indices: List<Int>)

operator fun SceneLineMesh.plus(other: SceneLineMesh): SceneLineMesh {
    val offset = vertices.size
    return SceneLineMesh(
        vertices = vertices + other.vertices,
        indices = indices + other.indices.map { it + offset }
    )
}

data class SceneSkyPadding(
    val leftPx: Int = 24,
    val topPx: Int = 24,
    val rightPx: Int = 24,
    val bottomPx: Int = 176
)

data class SceneSkyFrame(
    val center: ScenePoint3,
    val radiusMeters: Float,
    val outerRadiusMeters: Float,
    val horizontalMetersPerPixel: Float,
    val verticalMetersPerPixel: Float
)

object SceneSkyGeometry {
    const val DEFAULT_MERIDIAN_COUNT = 24
    const val DEFAULT_RING_SEGMENTS = 180
    const val DEFAULT_ALTITUDE_RINGS = 5
    private const val MIN_DOME_TO_AVAILABLE_RADIUS_RATIO = 0.55f

    fun calculateFrame(
        viewport: SceneViewport,
        surfaceWidthPx: Int,
        surfaceHeightPx: Int,
        orthographicZoom: Float,
        centerX: Float,
        centerZ: Float,
        padding: SceneSkyPadding = SceneSkyPadding(),
        compassBandPx: Float = 0f
    ): SceneSkyFrame {
        val horizontalInsetPx = maxOf(padding.leftPx, padding.rightPx)
        val verticalInsetPx = maxOf(padding.topPx, padding.bottomPx)
        val widthPx = (surfaceWidthPx - 2 * horizontalInsetPx).coerceAtLeast(1)
        val heightPx = (surfaceHeightPx - 2 * verticalInsetPx).coerceAtLeast(1)
        val horizontalMetersPerPixel = Scene3DCamera.orthographicMetersPerPixel(
            viewport.widthMeters,
            orthographicZoom,
            surfaceWidthPx
        )
        val verticalMetersPerPixel = Scene3DCamera.orthographicMetersPerPixel(
            viewport.heightMeters,
            orthographicZoom,
            surfaceHeightPx
        )
        val halfWidthPx = widthPx / 2f
        val halfHeightPx = heightPx / 2f
        val outerRadius = minOf(
            halfWidthPx * horizontalMetersPerPixel,
            halfHeightPx * verticalMetersPerPixel
        ).coerceAtLeast(0.001f)
        val reservedBandPx = compassBandPx.coerceAtLeast(0f)
        val domeHalfWidthPx = (halfWidthPx - reservedBandPx)
            .coerceAtLeast(halfWidthPx * MIN_DOME_TO_AVAILABLE_RADIUS_RATIO)
        val domeHalfHeightPx = (halfHeightPx - reservedBandPx)
            .coerceAtLeast(halfHeightPx * MIN_DOME_TO_AVAILABLE_RADIUS_RATIO)
        val radius = minOf(
            domeHalfWidthPx * horizontalMetersPerPixel,
            domeHalfHeightPx * verticalMetersPerPixel
        ).coerceIn(0.001f, outerRadius)
        return SceneSkyFrame(
            center = ScenePoint3(centerX, 0f, centerZ),
            radiusMeters = radius,
            outerRadiusMeters = outerRadius,
            horizontalMetersPerPixel = horizontalMetersPerPixel,
            verticalMetersPerPixel = verticalMetersPerPixel
        )
    }

    fun groundDiskMesh(
        radiusMeters: Float,
        viewport: SceneViewport,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f),
        segments: Int = DEFAULT_RING_SEGMENTS,
        yMeters: Float = 0f
    ): SceneTriangleMesh {
        val vertices = mutableListOf(center.copy(y = yMeters))
        repeat(segments + 1) { index ->
            val point = pointOnDome(
                index.toFloat() / segments * 360f,
                0f,
                radiusMeters,
                viewport,
                center
            )
            vertices += point.copy(y = yMeters)
        }
        val indices = mutableListOf<Int>()
        repeat(segments) { index ->
            indices += listOf(0, index + 2, index + 1)
        }
        return SceneTriangleMesh(vertices, indices)
    }

    fun domeMesh(
        radiusMeters: Float,
        viewport: SceneViewport,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f),
        meridianCount: Int = DEFAULT_MERIDIAN_COUNT,
        ringSegments: Int = DEFAULT_RING_SEGMENTS,
        altitudeRings: Int = DEFAULT_ALTITUDE_RINGS
    ): SceneLineMesh {
        val vertices = mutableListOf<ScenePoint3>()
        val indices = mutableListOf<Int>()
        fun addLoop(altitudeDegrees: Float) {
            val start = vertices.size
            repeat(ringSegments + 1) { index ->
                val azimuth = index.toFloat() / ringSegments * 360f
                vertices += pointOnDome(azimuth, altitudeDegrees, radiusMeters, viewport, center)
                if (index > 0) indices += listOf(start + index - 1, start + index)
            }
        }
        addLoop(0f)
        for (ring in 1..altitudeRings) {
            addLoop(ring.toFloat() / (altitudeRings + 1) * 90f)
        }
        repeat(meridianCount) { index ->
            val start = vertices.size
            val azimuth = index.toFloat() / meridianCount * 360f
            val meridianSteps = (altitudeRings + 1) * 3
            repeat(meridianSteps + 1) { step ->
                vertices += pointOnDome(
                    azimuth,
                    step.toFloat() / meridianSteps * 90f,
                    radiusMeters,
                    viewport,
                    center
                )
                if (step > 0) {
                    indices += listOf(start + step - 1, start + step)
                }
            }
        }
        return SceneLineMesh(vertices, indices)
    }

    fun compassMesh(
        radiusMeters: Float,
        viewport: SceneViewport,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f),
        segments: Int = DEFAULT_RING_SEGMENTS
    ): SceneLineMesh = circleMesh(radiusMeters, 0f, viewport, center, segments)

    fun sunPathMesh(
        positions: List<SolarPosition>,
        radiusMeters: Float,
        viewport: SceneViewport,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f)
    ): SceneLineMesh {
        val visible = positions.filter { it.isAboveHorizon }
        val vertices = visible.map {
            pointOnDome(
                it.azimuthDegrees.toFloat(),
                (90.0 - it.zenithDegrees).toFloat(),
                radiusMeters,
                viewport,
                center
            )
        }
        return SceneLineMesh(vertices, vertices.indices.drop(1).flatMap { listOf(it - 1, it) })
    }

    fun sunPathRibbonMesh(
        positions: List<SolarPosition>,
        radiusMeters: Float,
        viewport: SceneViewport,
        widthMeters: Float,
        surfaceOffsetMeters: Float,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f)
    ): SceneTriangleMesh {
        val path = positions.filter { it.isAboveHorizon }.map {
            pointOnDome(
                it.azimuthDegrees.toFloat(),
                (90.0 - it.zenithDegrees).toFloat(),
                radiusMeters,
                viewport,
                center
            )
        }
        if (path.size < 2) return SceneTriangleMesh(emptyList(), emptyList())
        val halfWidth = widthMeters.coerceAtLeast(0.001f) / 2f
        val vertices = mutableListOf<ScenePoint3>()
        path.indices.forEach { index ->
            val previous = path[(index - 1).coerceAtLeast(0)]
            val next = path[(index + 1).coerceAtMost(path.lastIndex)]
            val tangent = (next - previous).normalized()
            val normal = (path[index] - center).normalized()
            val side = normal.cross(tangent).normalized()
            val lifted = path[index] + normal * surfaceOffsetMeters
            vertices += lifted - side * halfWidth
            vertices += lifted + side * halfWidth
        }
        val indices = mutableListOf<Int>()
        repeat(path.lastIndex) { index ->
            val first = index * 2
            val next = first + 2
            indices += listOf(first, next, first + 1, first + 1, next, next + 1)
        }
        return SceneTriangleMesh(vertices, indices)
    }

    fun sunSphereMesh(
        center: ScenePoint3,
        radiusMeters: Float,
        latitudeSegments: Int = 12,
        longitudeSegments: Int = 24
    ): SceneTriangleMesh {
        val vertices = mutableListOf<ScenePoint3>()
        val indices = mutableListOf<Int>()
        repeat(latitudeSegments + 1) { latitudeIndex ->
            val latitude = Math.PI * latitudeIndex / latitudeSegments
            val y = cos(latitude).toFloat() * radiusMeters
            val horizontal = sin(latitude).toFloat() * radiusMeters
            repeat(longitudeSegments + 1) { longitudeIndex ->
                val longitude = 2.0 * Math.PI * longitudeIndex / longitudeSegments
                vertices += ScenePoint3(
                    center.x + cos(longitude).toFloat() * horizontal,
                    center.y + y,
                    center.z + sin(longitude).toFloat() * horizontal
                )
            }
        }
        repeat(latitudeSegments) { latitudeIndex ->
            repeat(longitudeSegments) { longitudeIndex ->
                val first = latitudeIndex * (longitudeSegments + 1) + longitudeIndex
                val second = first + longitudeSegments + 1
                indices += listOf(first, second, first + 1, first + 1, second, second + 1)
            }
        }
        return SceneTriangleMesh(vertices, indices)
    }

    fun connectorTubeMesh(
        start: ScenePoint3,
        end: ScenePoint3,
        diameterMeters: Float,
        radialSegments: Int = 8
    ): SceneTriangleMesh {
        val direction = (end - start).normalized()
        val reference = if (kotlin.math.abs(direction.y) < 0.9f) {
            ScenePoint3(0f, 1f, 0f)
        } else {
            ScenePoint3(1f, 0f, 0f)
        }
        val firstAxis = direction.cross(reference).normalized()
        val secondAxis = direction.cross(firstAxis).normalized()
        val radius = diameterMeters.coerceAtLeast(0.001f) / 2f
        val vertices = mutableListOf<ScenePoint3>()
        repeat(radialSegments) { index ->
            val angle = 2.0 * Math.PI * index / radialSegments
            val offset = firstAxis * (cos(angle).toFloat() * radius) +
                secondAxis * (sin(angle).toFloat() * radius)
            vertices += start + offset
            vertices += end + offset
        }
        val indices = mutableListOf<Int>()
        repeat(radialSegments) { index ->
            val next = (index + 1) % radialSegments
            val startCurrent = index * 2
            val endCurrent = startCurrent + 1
            val startNext = next * 2
            val endNext = startNext + 1
            indices += listOf(startCurrent, startNext, endCurrent, endCurrent, startNext, endNext)
        }
        return SceneTriangleMesh(vertices, indices)
    }

    fun pointOnDome(
        azimuthDegrees: Float,
        altitudeDegrees: Float,
        radiusMeters: Float,
        viewport: SceneViewport,
        center: ScenePoint3 = ScenePoint3(0f, 0f, 0f)
    ): ScenePoint3 {
        val azimuth = Math.toRadians(azimuthDegrees.toDouble())
        val altitude = Math.toRadians(altitudeDegrees.coerceIn(0f, 90f).toDouble())
        val horizontal = (cos(altitude) * radiusMeters).toFloat()
        val east = (sin(azimuth) * horizontal).toFloat()
        val north = (cos(azimuth) * horizontal).toFloat()
        return ScenePoint3(
            x = center.x + viewport.screenRightX * east - viewport.screenDownX * north,
            y = center.y + (sin(altitude) * radiusMeters).toFloat(),
            z = center.z + viewport.screenRightZ * east - viewport.screenDownZ * north
        )
    }

    private fun circleMesh(
        radiusMeters: Float,
        altitudeDegrees: Float,
        viewport: SceneViewport,
        center: ScenePoint3,
        segments: Int
    ): SceneLineMesh {
        val vertices = (0..segments).map { index ->
            pointOnDome(
                index.toFloat() / segments * 360f,
                altitudeDegrees,
                radiusMeters,
                viewport,
                center
            )
        }
        return SceneLineMesh(vertices, vertices.indices.drop(1).flatMap { listOf(it - 1, it) })
    }
}

private operator fun ScenePoint3.plus(other: ScenePoint3): ScenePoint3 =
    ScenePoint3(x + other.x, y + other.y, z + other.z)

private operator fun ScenePoint3.minus(other: ScenePoint3): ScenePoint3 =
    ScenePoint3(x - other.x, y - other.y, z - other.z)

private operator fun ScenePoint3.times(value: Float): ScenePoint3 =
    ScenePoint3(x * value, y * value, z * value)

private fun ScenePoint3.cross(other: ScenePoint3): ScenePoint3 = ScenePoint3(
    y * other.z - z * other.y,
    z * other.x - x * other.z,
    x * other.y - y * other.x
)

private fun ScenePoint3.normalized(): ScenePoint3 {
    val length = sqrt(x * x + y * y + z * z).coerceAtLeast(0.0001f)
    return ScenePoint3(x / length, y / length, z / length)
}
