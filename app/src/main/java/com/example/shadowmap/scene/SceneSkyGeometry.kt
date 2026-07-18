package com.example.shadowmap.scene

import com.example.shadowmap.domain.SolarPosition
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin

data class ScenePoint3(val x: Float, val y: Float, val z: Float)

data class SceneLineMesh(
    val vertices: List<ScenePoint3>,
    val indices: List<Int>
)

data class SceneTriangleMesh(
    val vertices: List<ScenePoint3>,
    val indices: List<Int>
)

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
    val radiusMeters: Float,
    val usableWidthMeters: Float,
    val usableHeightMeters: Float
)

object SceneSkyGeometry {
    const val DEFAULT_SEGMENTS = 24
    const val DEFAULT_ALTITUDE_RINGS = 5
    const val VISUAL_MARGIN_METERS = 1f

    fun calculateFrame(
        viewport: SceneViewport,
        surfaceWidthPx: Int,
        surfaceHeightPx: Int,
        padding: SceneSkyPadding = SceneSkyPadding(),
        visualMarginMeters: Float = VISUAL_MARGIN_METERS
    ): SceneSkyFrame {
        val widthPx = (surfaceWidthPx - padding.leftPx - padding.rightPx).coerceAtLeast(1)
        val heightPx = (surfaceHeightPx - padding.topPx - padding.bottomPx).coerceAtLeast(1)
        val usableWidth = viewport.widthMeters * widthPx.toFloat() / surfaceWidthPx.coerceAtLeast(1)
        val usableHeight = viewport.heightMeters * heightPx.toFloat() / surfaceHeightPx.coerceAtLeast(1)
        val radius = (minOf(usableWidth, usableHeight) / 2f - visualMarginMeters)
            .coerceAtLeast(1f)
        return SceneSkyFrame(radius, usableWidth, usableHeight)
    }

    fun domeMesh(
        radiusMeters: Float,
        viewport: SceneViewport,
        segments: Int = DEFAULT_SEGMENTS,
        altitudeRings: Int = DEFAULT_ALTITUDE_RINGS
    ): SceneLineMesh {
        val vertices = mutableListOf<ScenePoint3>()
        val indices = mutableListOf<Int>()
        fun addLoop(altitudeDegrees: Float) {
            val start = vertices.size
            repeat(segments + 1) { index ->
                val azimuth = index.toFloat() / segments * 360f
                vertices += pointOnDome(azimuth, altitudeDegrees, radiusMeters, viewport)
                if (index > 0) indices += listOf(start + index - 1, start + index)
            }
        }
        addLoop(0f)
        for (ring in 1..altitudeRings) {
            addLoop(ring.toFloat() / (altitudeRings + 1) * 90f)
        }
        repeat(segments) { index ->
            val start = vertices.size
            val azimuth = index.toFloat() / segments * 360f
            val meridianSteps = (altitudeRings + 1) * 3
            repeat(meridianSteps + 1) { step ->
                vertices += pointOnDome(
                    azimuth,
                    step.toFloat() / meridianSteps * 90f,
                    radiusMeters,
                    viewport
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
        segments: Int = DEFAULT_SEGMENTS
    ): SceneLineMesh = circleMesh(radiusMeters, 0f, viewport, segments)

    fun sunPathMesh(
        positions: List<SolarPosition>,
        radiusMeters: Float,
        viewport: SceneViewport
    ): SceneLineMesh {
        val visible = positions.filter { it.isAboveHorizon }
        val vertices = visible.map {
            pointOnDome(
                it.azimuthDegrees.toFloat(),
                (90.0 - it.zenithDegrees).toFloat(),
                radiusMeters,
                viewport
            )
        }
        return SceneLineMesh(vertices, vertices.indices.drop(1).flatMap { listOf(it - 1, it) })
    }

    fun sunPathRibbonMesh(
        positions: List<SolarPosition>,
        radiusMeters: Float,
        viewport: SceneViewport,
        widthMeters: Float,
        surfaceOffsetMeters: Float
    ): SceneTriangleMesh {
        val path = positions.filter { it.isAboveHorizon }.map {
            pointOnDome(
                it.azimuthDegrees.toFloat(),
                (90.0 - it.zenithDegrees).toFloat(),
                radiusMeters,
                viewport
            )
        }
        if (path.size < 2) return SceneTriangleMesh(emptyList(), emptyList())
        val halfWidth = widthMeters.coerceAtLeast(0.001f) / 2f
        val vertices = mutableListOf<ScenePoint3>()
        path.indices.forEach { index ->
            val previous = path[(index - 1).coerceAtLeast(0)]
            val next = path[(index + 1).coerceAtMost(path.lastIndex)]
            val tangent = (next - previous).normalized()
            val normal = path[index].normalized()
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

    fun pointOnDome(
        azimuthDegrees: Float,
        altitudeDegrees: Float,
        radiusMeters: Float,
        viewport: SceneViewport
    ): ScenePoint3 {
        val azimuth = Math.toRadians(azimuthDegrees.toDouble())
        val altitude = Math.toRadians(altitudeDegrees.coerceIn(0f, 90f).toDouble())
        val horizontal = (cos(altitude) * radiusMeters).toFloat()
        val east = (sin(azimuth) * horizontal).toFloat()
        val north = (cos(azimuth) * horizontal).toFloat()
        return ScenePoint3(
            x = viewport.screenRightX * east - viewport.screenDownX * north,
            y = (sin(altitude) * radiusMeters).toFloat(),
            z = viewport.screenRightZ * east - viewport.screenDownZ * north
        )
    }

    private fun circleMesh(
        radiusMeters: Float,
        altitudeDegrees: Float,
        viewport: SceneViewport,
        segments: Int
    ): SceneLineMesh {
        val vertices = (0..segments).map { index ->
            pointOnDome(index.toFloat() / segments * 360f, altitudeDegrees, radiusMeters, viewport)
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
