package com.example.genpox.map.tile

import com.example.genpox.data.BuildingStructure
import com.example.genpox.data.RoadPoint
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

/**
 * High-performance, zero-dependency binary decoder for Mapbox Vector Tiles (MVT 2.1)
 * serialized via Google Protocol Buffers (protobuf).
 *
 * Employs zero-copy, in-place byte buffer navigation with direct index pointers,
 * eliminating stream wrappers, intermediate byte array slices, and primitive autoboxing.
 */
object MvtDecoder {

    private val ROAD_LAYER_NAMES = setOf("transportation", "road", "roads", "highway")
    private val BUILDING_LAYER_NAMES = setOf("building", "buildings", "building_part")

    /**
     * Decodes a raw MVT byte array for the specified tile coordinate.
     */
    fun decodeTile(coordinate: TileCoordinate, bytes: ByteArray): TileGeometry {
        val rawBytes = decompressIfNeeded(bytes)
        val pbf = ProtoBuffer(rawBytes)

        val roads = ArrayList<List<RoadPoint>>()
        val buildings = ArrayList<BuildingStructure>()

        // Tile message: repeated Layer layers = 3
        while (pbf.hasMore()) {
            val tag = pbf.readVarint32()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7

            if (fieldNumber == 3 && wireType == 2) {
                val layerLength = pbf.readVarint32()
                val layerEnd = minOf(pbf.pos + layerLength, pbf.limit)
                parseLayer(coordinate, pbf, layerEnd, roads, buildings)
                pbf.pos = layerEnd
            } else {
                pbf.skipField(wireType)
            }
        }

        return TileGeometry(
            coordinate = coordinate,
            roads = roads,
            buildings = buildings,
        )
    }

    private fun decompressIfNeeded(bytes: ByteArray): ByteArray {
        // Detect GZIP magic bytes: 0x1F 0x8B
        if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            val out = ByteArrayOutputStream(bytes.size * 4)
            val buffer = ByteArray(32768)
            val gz = GZIPInputStream(ByteArrayInputStream(bytes), 32768)
            var n: Int
            while (gz.read(buffer).also { n = it } > 0) {
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
        return bytes
    }

    private fun parseLayer(
        coordinate: TileCoordinate,
        pbf: ProtoBuffer,
        layerEnd: Int,
        outRoads: MutableList<List<RoadPoint>>,
        outBuildings: MutableList<BuildingStructure>,
    ) {
        var layerName = ""
        var extent = TileMath.DEFAULT_EXTENT
        val keys = ArrayList<String>()
        val values = ArrayList<Any?>()

        var featureCount = 0
        var featureOffsets = IntArray(128)
        var featureLengths = IntArray(128)

        while (pbf.pos < layerEnd) {
            val tag = pbf.readVarint32()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7

            when (fieldNumber) {
                1 -> { // required string name = 1
                    val len = pbf.readVarint32()
                    layerName = pbf.readString(len)
                    // If layer is neither roads nor buildings, immediately skip the rest of this layer!
                    if (!ROAD_LAYER_NAMES.contains(layerName) && !BUILDING_LAYER_NAMES.contains(layerName)) {
                        pbf.pos = layerEnd
                        return
                    }
                }
                2 -> { // repeated Feature features = 2
                    val len = pbf.readVarint32()
                    if (featureCount == featureOffsets.size) {
                        val newCap = featureOffsets.size * 2
                        featureOffsets = featureOffsets.copyOf(newCap)
                        featureLengths = featureLengths.copyOf(newCap)
                    }
                    featureOffsets[featureCount] = pbf.pos
                    featureLengths[featureCount] = len
                    pbf.pos = minOf(pbf.pos + len, layerEnd)
                    featureCount++
                }
                3 -> { // repeated string keys = 3
                    val len = pbf.readVarint32()
                    if (ROAD_LAYER_NAMES.contains(layerName)) {
                        // Road layers do not use feature tags/keys
                        pbf.pos = minOf(pbf.pos + len, layerEnd)
                    } else {
                        keys.add(pbf.readString(len))
                    }
                }
                4 -> { // repeated Value values = 4
                    val len = pbf.readVarint32()
                    if (ROAD_LAYER_NAMES.contains(layerName)) {
                        // Road layers do not use feature values
                        pbf.pos = minOf(pbf.pos + len, layerEnd)
                    } else {
                        val valEnd = minOf(pbf.pos + len, layerEnd)
                        values.add(parseValue(pbf, valEnd))
                        pbf.pos = valEnd
                    }
                }
                5 -> extent = pbf.readVarint32() // optional uint32 extent = 5
                else -> pbf.skipField(wireType)
            }
        }

        val isRoadLayer = ROAD_LAYER_NAMES.contains(layerName)
        val isBuildingLayer = BUILDING_LAYER_NAMES.contains(layerName)

        if ((!isRoadLayer && !isBuildingLayer) || featureCount == 0) return

        val projection = TileProjection(coordinate.x, coordinate.y, coordinate.z, extent)

        for (i in 0 until featureCount) {
            val featPos = featureOffsets[i]
            val featLen = featureLengths[i]
            pbf.pos = featPos
            parseFeature(
                coordinate = coordinate,
                pbf = pbf,
                featEnd = minOf(featPos + featLen, pbf.limit),
                extent = extent,
                projection = projection,
                keys = keys,
                values = values,
                isRoadLayer = isRoadLayer,
                isBuildingLayer = isBuildingLayer,
                outRoads = outRoads,
                outBuildings = outBuildings,
            )
        }
    }

    private fun parseValue(pbf: ProtoBuffer, valEnd: Int): Any? {
        while (pbf.pos < valEnd) {
            val tag = pbf.readVarint32()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7

            when (fieldNumber) {
                1 -> {
                    val len = pbf.readVarint32()
                    return pbf.readString(len)
                }
                2 -> return pbf.readFloat()
                3 -> return pbf.readDouble()
                4 -> return pbf.readVarint64()
                5 -> return pbf.readVarint64()
                6 -> return pbf.readSInt64()
                7 -> return pbf.readVarint32() != 0
                else -> pbf.skipField(wireType)
            }
        }
        return null
    }

    private fun parseFeature(
        coordinate: TileCoordinate,
        pbf: ProtoBuffer,
        featEnd: Int,
        extent: Int,
        projection: TileProjection,
        keys: List<String>,
        values: List<Any?>,
        isRoadLayer: Boolean,
        isBuildingLayer: Boolean,
        outRoads: MutableList<List<RoadPoint>>,
        outBuildings: MutableList<BuildingStructure>,
    ) {
        var tagsPos = -1
        var tagsLen = 0
        var geomPos = -1
        var geomLen = 0
        var geomType = 0

        while (pbf.pos < featEnd) {
            val tag = pbf.readVarint32()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x7

            when (fieldNumber) {
                2 -> { // repeated uint32 tags = 2 [packed = true]
                    val len = pbf.readVarint32()
                    if (isRoadLayer) {
                        pbf.pos = minOf(pbf.pos + len, featEnd)
                    } else {
                        tagsPos = pbf.pos
                        tagsLen = len
                        pbf.pos = minOf(pbf.pos + len, featEnd)
                    }
                }
                3 -> geomType = pbf.readVarint32() // optional GeomType type = 3
                4 -> { // repeated uint32 geometry = 4 [packed = true]
                    val len = pbf.readVarint32()
                    geomPos = pbf.pos
                    geomLen = len
                    pbf.pos = minOf(pbf.pos + len, featEnd)
                }
                else -> pbf.skipField(wireType)
            }
        }

        if (geomPos < 0 || geomLen == 0) return

        // GeomType: 1 = POINT, 2 = LINESTRING, 3 = POLYGON
        if (isRoadLayer && geomType == 2) {
            decodeLineStrings(pbf, geomPos, geomPos + geomLen, projection, outRoads)
        } else if (isBuildingLayer && geomType == 3) {
            val height = if (tagsPos >= 0 && tagsLen > 0) {
                extractBuildingHeightFromBuffer(pbf, tagsPos, tagsPos + tagsLen, keys, values)
            } else {
                6.0f
            }
            decodePolygons(pbf, geomPos, geomPos + geomLen, projection, height, outBuildings)
        }
    }

    private fun extractBuildingHeightFromBuffer(
        buf: ProtoBuffer,
        tagsPos: Int,
        tagsLimit: Int,
        keys: List<String>,
        values: List<Any?>,
    ): Float {
        buf.pos = tagsPos
        var height: Float? = null
        var levels: Float? = null

        while (buf.pos < tagsLimit) {
            val kIdx = buf.readVarint32()
            if (buf.pos >= tagsLimit) break
            val vIdx = buf.readVarint32()

            if (kIdx in keys.indices && vIdx in values.indices) {
                val key = keys[kIdx]
                when (key) {
                    "render_height", "height" -> {
                        val value = values[vIdx]
                        height = when (value) {
                            is Number -> value.toFloat()
                            is String -> value.replace("m", "").trim().toFloatOrNull()
                            else -> null
                        }
                    }
                    "building:levels", "levels" -> {
                        val value = values[vIdx]
                        levels = when (value) {
                            is Number -> value.toFloat()
                            is String -> value.trim().toFloatOrNull()
                            else -> null
                        }
                    }
                }
            }
        }

        return when {
            height != null && height > 0f -> height.coerceIn(2.5f, 400.0f)
            levels != null && levels > 0f -> (levels * 3.5f).coerceIn(3.0f, 400.0f)
            else -> 6.0f
        }
    }

    private fun decodeLineStrings(
        buf: ProtoBuffer,
        geomPos: Int,
        geomLimit: Int,
        projection: TileProjection,
        outRoads: MutableList<List<RoadPoint>>,
    ) {
        buf.pos = geomPos
        var cursorX = 0
        var cursorY = 0
        var currentLine = ArrayList<RoadPoint>()

        while (buf.pos < geomLimit) {
            val cmdHeader = buf.readVarint32()
            val cmdId = cmdHeader and 0x7
            val cmdCount = cmdHeader ushr 3

            when (cmdId) {
                1 -> { // MoveTo
                    if (currentLine.isNotEmpty()) {
                        outRoads.add(currentLine)
                        currentLine = ArrayList(cmdCount + 8)
                    }
                    for (c in 0 until cmdCount) {
                        cursorX += decodeZigZag(buf.readVarint32())
                        cursorY += decodeZigZag(buf.readVarint32())
                        currentLine.add(projection.toRoadPoint(cursorX, cursorY))
                    }
                }
                2 -> { // LineTo
                    currentLine.ensureCapacity(currentLine.size + cmdCount)
                    for (c in 0 until cmdCount) {
                        cursorX += decodeZigZag(buf.readVarint32())
                        cursorY += decodeZigZag(buf.readVarint32())
                        currentLine.add(projection.toRoadPoint(cursorX, cursorY))
                    }
                }
                7 -> { // ClosePath
                    if (currentLine.isNotEmpty()) {
                        outRoads.add(currentLine)
                        currentLine = ArrayList()
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            outRoads.add(currentLine)
        }
    }

    private fun decodePolygons(
        buf: ProtoBuffer,
        geomPos: Int,
        geomLimit: Int,
        projection: TileProjection,
        height: Float,
        outBuildings: MutableList<BuildingStructure>,
    ) {
        buf.pos = geomPos
        var cursorX = 0
        var cursorY = 0
        var currentRing = ArrayList<RoadPoint>()

        while (buf.pos < geomLimit) {
            val cmdHeader = buf.readVarint32()
            val cmdId = cmdHeader and 0x7
            val cmdCount = cmdHeader ushr 3

            when (cmdId) {
                1 -> { // MoveTo
                    if (currentRing.isNotEmpty()) {
                        if (currentRing.size >= 3) {
                            if (currentRing.first() != currentRing.last()) {
                                currentRing.add(currentRing.first())
                            }
                            outBuildings.add(BuildingStructure(points = currentRing, isFallback = false, heightMeters = height))
                        }
                        currentRing = ArrayList(cmdCount + 8)
                    }
                    for (c in 0 until cmdCount) {
                        cursorX += decodeZigZag(buf.readVarint32())
                        cursorY += decodeZigZag(buf.readVarint32())
                        currentRing.add(projection.toRoadPoint(cursorX, cursorY))
                    }
                }
                2 -> { // LineTo
                    currentRing.ensureCapacity(currentRing.size + cmdCount + 1)
                    for (c in 0 until cmdCount) {
                        cursorX += decodeZigZag(buf.readVarint32())
                        cursorY += decodeZigZag(buf.readVarint32())
                        currentRing.add(projection.toRoadPoint(cursorX, cursorY))
                    }
                }
                7 -> { // ClosePath
                    if (currentRing.isNotEmpty()) {
                        if (currentRing.first() != currentRing.last()) {
                            currentRing.add(currentRing.first())
                        }
                        if (currentRing.size >= 3) {
                            outBuildings.add(BuildingStructure(points = currentRing, isFallback = false, heightMeters = height))
                        }
                        currentRing = ArrayList()
                    }
                }
            }
        }
        if (currentRing.isNotEmpty() && currentRing.size >= 3) {
            if (currentRing.first() != currentRing.last()) {
                currentRing.add(currentRing.first())
            }
            outBuildings.add(BuildingStructure(points = currentRing, isFallback = false, heightMeters = height))
        }
    }

    private fun decodeZigZag(n: Int): Int = (n ushr 1) xor -(n and 1)

    /**
     * Minimal zero-allocation Protobuf binary reader wrapping a flat byte array.
     */
    private class ProtoBuffer(
        val buf: ByteArray,
        var pos: Int = 0,
        val limit: Int = buf.size,
    ) {
        fun hasMore(): Boolean = pos < limit

        fun readVarint32(): Int {
            if (pos >= limit) return 0
            val b0 = buf[pos++].toInt()
            if ((b0 and 0x80) == 0) return b0

            var result = b0 and 0x7F
            var shift = 7
            while (shift < 32) {
                if (pos >= limit) break
                val b = buf[pos++].toInt()
                result = result or ((b and 0x7F) shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
            }
            while (pos < limit && (buf[pos++].toInt() and 0x80) != 0) {}
            return result
        }

        fun readVarint64(): Long {
            if (pos >= limit) return 0L
            val b0 = buf[pos++].toLong()
            if ((b0 and 0x80L) == 0L) return b0

            var result = b0 and 0x7FL
            var shift = 7
            while (shift < 64) {
                if (pos >= limit) break
                val b = buf[pos++].toLong()
                result = result or ((b and 0x7FL) shl shift)
                if ((b and 0x80L) == 0L) return result
                shift += 7
            }
            return result
        }

        fun readSInt64(): Long {
            val n = readVarint64()
            return (n ushr 1) xor -(n and 1L)
        }

        fun readFloat(): Float {
            if (pos + 4 > limit) return 0f
            val b0 = buf[pos++].toInt() and 0xFF
            val b1 = buf[pos++].toInt() and 0xFF
            val b2 = buf[pos++].toInt() and 0xFF
            val b3 = buf[pos++].toInt() and 0xFF
            return Float.fromBits(b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))
        }

        fun readDouble(): Double {
            if (pos + 8 > limit) return 0.0
            var bits = 0L
            for (i in 0 until 8) {
                val b = buf[pos++].toLong() and 0xFF
                bits = bits or (b shl (i * 8))
            }
            return Double.fromBits(bits)
        }

        fun readString(length: Int): String {
            val clampedLen = minOf(length, limit - pos)
            val s = String(buf, pos, clampedLen, Charsets.UTF_8)
            pos += clampedLen
            return s
        }

        fun skipField(wireType: Int) {
            when (wireType) {
                0 -> readVarint64()
                1 -> pos = minOf(pos + 8, limit)
                2 -> {
                    val len = readVarint32()
                    pos = minOf(pos + len, limit)
                }
                5 -> pos = minOf(pos + 4, limit)
                else -> throw IllegalArgumentException("Unsupported wire type: $wireType")
            }
        }
    }
}
