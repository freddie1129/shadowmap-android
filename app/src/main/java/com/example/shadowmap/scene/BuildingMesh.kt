package com.example.shadowmap.scene

import com.example.shadowmap.domain.BuildingFootprint
import kotlin.math.PI
import kotlin.math.cos

data class MeshVertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val normalX: Float,
    val normalY: Float,
    val normalZ: Float
)

data class BuildingMesh(
    val vertices: List<MeshVertex>,
    val indices: List<Int>,
    val radiusMeters: Float,
    val wallIndexOffset: Int = 0,
    val groundIndexOffset: Int = 0
)

object BuildingMeshGenerator {
    @Suppress("CyclomaticComplexMethod")
    fun generate(
        buildings: List<BuildingFootprint>,
        viewport: SceneViewport? = null
    ): BuildingMesh {
        val points = buildings.flatMap { it.polygon.rings.firstOrNull().orEmpty() }
        if (points.isEmpty()) return BuildingMesh(emptyList(), emptyList(), 1f)
        val originLongitude = viewport?.centerLongitude ?: points.map { it.longitude }.average()
        val originLatitude = viewport?.centerLatitude ?: points.map { it.latitude }.average()
        val latitudeScale = Scene3DGeometry.WGS84_EARTH_RADIUS_METERS * PI / 180.0
        val longitudeScale = latitudeScale * cos(originLatitude * PI / 180.0)
        val vertices = mutableListOf<MeshVertex>()
        val roofIndices = mutableListOf<Int>()
        val wallIndices = mutableListOf<Int>()

        buildings.forEach { building ->
            val ring = building.polygon.rings.firstOrNull().orEmpty()
                .dropClosingPoint()
                .distinctBy { it.longitude to it.latitude }
            if (ring.size < 3) return@forEach
            val local = ring.map {
                Pair(
                    ((it.longitude - originLongitude) * longitudeScale).toFloat(),
                    (-(it.latitude - originLatitude) * latitudeScale).toFloat()
                )
            }
            val bottom = building.minHeightMeters.coerceAtLeast(0.0).toFloat()
            val top = building.heightMeters.coerceAtLeast(building.minHeightMeters).toFloat()
            val roofStart = vertices.size
            local.forEach { (x, z) -> vertices += MeshVertex(x, top, z, 0f, 1f, 0f) }
            triangulate(local).chunked(3).forEach { triangle ->
                roofIndices += listOf(
                    roofStart + triangle[0],
                    roofStart + triangle[2],
                    roofStart + triangle[1]
                )
            }

            local.indices.forEach { index ->
                val next = (index + 1) % local.size
                val (x0, z0) = local[index]
                val (x1, z1) = local[next]
                val dx = x1 - x0
                val dz = z1 - z0
                val length = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.0001f)
                val nx = dz / length
                val nz = -dx / length
                val start = vertices.size
                vertices += MeshVertex(x0, bottom, z0, nx, 0f, nz)
                vertices += MeshVertex(x1, bottom, z1, nx, 0f, nz)
                vertices += MeshVertex(x1, top, z1, nx, 0f, nz)
                vertices += MeshVertex(x0, top, z0, nx, 0f, nz)
                wallIndices += listOf(start, start + 2, start + 1, start, start + 3, start + 2)
            }
        }
        val radius = (vertices.maxOfOrNull { kotlin.math.sqrt(it.x * it.x + it.z * it.z) } ?: 1f)
            .coerceAtLeast(1f)
        val groundHalfWidth = viewport?.widthMeters?.div(2f)
            ?: radius * Scene3DGeometry.FALLBACK_GROUND_EXTENT_MULTIPLIER
        val groundHalfHeight = viewport?.heightMeters?.div(2f)
            ?: radius * Scene3DGeometry.FALLBACK_GROUND_EXTENT_MULTIPLIER
        val rightX = viewport?.screenRightX ?: 1f
        val rightZ = viewport?.screenRightZ ?: 0f
        val downX = viewport?.screenDownX ?: 0f
        val downZ = viewport?.screenDownZ ?: 1f
        val indices = mutableListOf<Int>().apply {
            addAll(roofIndices)
            addAll(wallIndices)
        }
        val wallIndexOffset = roofIndices.size
        val groundStart = vertices.size
        val groundIndexOffset = indices.size
        fun groundVertex(right: Float, down: Float) = MeshVertex(
            rightX * right + downX * down,
            0f,
            rightZ * right + downZ * down,
            0f,
            1f,
            0f
        )
        vertices += groundVertex(-groundHalfWidth, -groundHalfHeight)
        vertices += groundVertex(groundHalfWidth, -groundHalfHeight)
        vertices += groundVertex(groundHalfWidth, groundHalfHeight)
        vertices += groundVertex(-groundHalfWidth, groundHalfHeight)
        indices += listOf(
            groundStart,
            groundStart + 2,
            groundStart + 1,
            groundStart,
            groundStart + 3,
            groundStart + 2
        )
        return BuildingMesh(vertices, indices, radius, wallIndexOffset, groundIndexOffset)
    }

    // Ear clipping supports the concave outer rings returned by Mapbox.
    private fun triangulate(points: List<Pair<Float, Float>>): List<Int> {
        val remaining = points.indices.toMutableList()
        val result = mutableListOf<Int>()
        val area = points.indices.sumOf { i ->
            val a = points[i]
            val b = points[(i + 1) % points.size]
            (a.first * b.second - b.first * a.second).toDouble()
        }
        if (area < 0.0) remaining.reverse()
        var guard = points.size * points.size
        while (remaining.size > 3 && guard-- > 0) {
            val ear = remaining.indices.firstOrNull { position ->
                val previous = remaining[(position - 1 + remaining.size) % remaining.size]
                val current = remaining[position]
                val next = remaining[(position + 1) % remaining.size]
                isConvex(points[previous], points[current], points[next]) &&
                    remaining.none { candidate ->
                        candidate != previous && candidate != current && candidate != next &&
                            insideTriangle(
                                points[candidate],
                                points[previous],
                                points[current],
                                points[next]
                            )
                    }
            } ?: break
            val previous = remaining[(ear - 1 + remaining.size) % remaining.size]
            val current = remaining[ear]
            val next = remaining[(ear + 1) % remaining.size]
            result += listOf(previous, current, next)
            remaining.removeAt(ear)
        }
        if (remaining.size == 3) result += remaining
        return result
    }

    private fun isConvex(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>) =
        (b.first - a.first) * (c.second - a.second) - (b.second - a.second) * (c.first - a.first) >
            0f

    private fun insideTriangle(
        p: Pair<Float, Float>,
        a: Pair<Float, Float>,
        b: Pair<Float, Float>,
        c: Pair<Float, Float>
    ): Boolean {
        fun sign(p1: Pair<Float, Float>, p2: Pair<Float, Float>, p3: Pair<Float, Float>) =
            (p1.first - p3.first) * (p2.second - p3.second) -
                (p2.first - p3.first) * (p1.second - p3.second)
        val d1 = sign(p, a, b)
        val d2 = sign(p, b, c)
        val d3 = sign(p, c, a)
        return !(d1 < 0f || d2 < 0f || d3 < 0f) || !(d1 > 0f || d2 > 0f || d3 > 0f)
    }

    private fun <T> List<T>.dropClosingPoint(): List<T> =
        if (size > 1 && first() == last()) dropLast(1) else this
}
