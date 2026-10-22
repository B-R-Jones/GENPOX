package com.example.genpox.map.tile

/**
 * Pure computational geometry utility for 2D polygon triangulation using ear clipping.
 * Used for triangulating building roof caps in vector tiles for 3D GPU mesh compilation.
 */
object PolygonTriangulator {

    /**
     * High-performance zero-allocation ear-clipping polygon triangulation for arbitrary simple 2D polygons.
     * Takes flat [coords] array where vertex i is stored at coords[i * 2], coords[i * 2 + 1].
     * Dispatches indices (i1, i2, i3) of triangles formed by the input point vertices.
     */
    fun triangulatePolygonEarClipping(
        coords: FloatArray,
        count: Int,
        onTriangle: (i1: Int, i2: Int, i3: Int) -> Unit,
    ) {
        if (count < 3) return
        if (count == 3) {
            onTriangle(0, 1, 2)
            return
        }
        if (count == 4) {
            onTriangle(0, 1, 2)
            onTriangle(0, 2, 3)
            return
        }

        val indices = IntArray(count) { it }
        var remaining = count
        var iterationLimit = 3 * count

        var curr = 0
        while (remaining > 2 && iterationLimit-- > 0) {
            val prev = (curr + remaining - 1) % remaining
            val next = (curr + 1) % remaining

            val iPrev = indices[prev]
            val iCurr = indices[curr]
            val iNext = indices[next]

            val ax = coords[iPrev * 2]
            val ay = coords[iPrev * 2 + 1]
            val bx = coords[iCurr * 2]
            val by = coords[iCurr * 2 + 1]
            val cx = coords[iNext * 2]
            val cy = coords[iNext * 2 + 1]

            val cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)

            if (cross >= -0.0001f) {
                var isEar = true
                for (i in 0 until remaining) {
                    if (i == prev || i == curr || i == next) continue
                    val idx = indices[i]
                    val px = coords[idx * 2]
                    val py = coords[idx * 2 + 1]
                    if (isPointInTriangle2D(px, py, ax, ay, bx, by, cx, cy)) {
                        isEar = false
                        break
                    }
                }

                if (isEar) {
                    onTriangle(iPrev, iCurr, iNext)
                    for (i in curr until remaining - 1) {
                        indices[i] = indices[i + 1]
                    }
                    remaining--
                    if (curr >= remaining) curr = 0
                    continue
                }
            }

            curr = (curr + 1) % remaining
        }

        if (remaining > 2) {
            for (i in 1 until remaining - 1) {
                onTriangle(indices[0], indices[i], indices[i + 1])
            }
        }
    }

    /**
     * Ear-clipping polygon triangulation for arbitrary simple 2D polygons (convex and concave).
     * Dispatches indices (i1, i2, i3) of triangles formed by the input point vertices.
     */
    fun triangulatePolygonEarClipping(
        points: List<Pair<Float, Float>>,
        onTriangle: (i1: Int, i2: Int, i3: Int) -> Unit,
    ) {
        val n = points.size
        if (n < 3) return
        val coords = FloatArray(n * 2)
        for (i in 0 until n) {
            coords[i * 2] = points[i].first
            coords[i * 2 + 1] = points[i].second
        }
        triangulatePolygonEarClipping(coords, n, onTriangle)
    }

    fun isPointInTriangle2D(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
    ): Boolean {
        val v0x = cx - ax
        val v0y = cy - ay
        val v1x = bx - ax
        val v1y = by - ay
        val v2x = px - ax
        val v2y = py - ay

        val dot00 = v0x * v0x + v0y * v0y
        val dot01 = v0x * v1x + v0y * v1y
        val dot02 = v0x * v2x + v0y * v2y
        val dot11 = v1x * v1x + v1y * v1y
        val dot12 = v1x * v2x + v1y * v2y

        val invDenom = 1.0f / (dot00 * dot11 - dot01 * dot01).coerceAtLeast(1e-9f)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom

        return (u >= 0f) && (v >= 0f) && (u + v <= 1f)
    }
}
