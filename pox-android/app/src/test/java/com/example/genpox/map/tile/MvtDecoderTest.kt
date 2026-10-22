package com.example.genpox.map.tile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class MvtDecoderTest {

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    private fun writeField(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int, data: ByteArray) {
        writeVarint(out, ((fieldNumber.toLong() shl 3) or wireType.toLong()))
        writeVarint(out, data.size.toLong())
        out.write(data)
    }

    private fun writeStringField(out: ByteArrayOutputStream, fieldNumber: Int, str: String) {
        writeField(out, fieldNumber, 2, str.toByteArray(Charsets.UTF_8))
    }

    private fun writeVarintField(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        writeVarint(out, ((fieldNumber.toLong() shl 3) or 0L))
        writeVarint(out, value)
    }

    private fun encodeZigZag(n: Int): Int = (n shl 1) xor (n shr 31)

    private fun buildSyntheticMvtPayload(): ByteArray {
        val tileStream = ByteArrayOutputStream()

        // 1. Build "transportation" layer (field 3 in Tile)
        val roadLayerStream = ByteArrayOutputStream()
        writeStringField(roadLayerStream, 1, "transportation") // name

        // Road Feature (field 2 in Layer)
        val roadFeatureStream = ByteArrayOutputStream()
        writeVarintField(roadFeatureStream, 3, 2) // GeomType = LINESTRING (2)

        // Geometry: MoveTo(1) (100, 100), LineTo(1) (200, 200)
        val geomStream = ByteArrayOutputStream()
        writeVarint(geomStream, (1 or (1 shl 3)).toLong()) // MoveTo count 1 = 9
        writeVarint(geomStream, encodeZigZag(100).toLong())
        writeVarint(geomStream, encodeZigZag(100).toLong())
        writeVarint(geomStream, (2 or (1 shl 3)).toLong()) // LineTo count 1 = 10
        writeVarint(geomStream, encodeZigZag(100).toLong())
        writeVarint(geomStream, encodeZigZag(100).toLong())

        writeField(roadFeatureStream, 4, 2, geomStream.toByteArray())
        writeField(roadLayerStream, 2, 2, roadFeatureStream.toByteArray())

        // 2. Build "building" layer (field 3 in Tile)
        val bldgLayerStream = ByteArrayOutputStream()
        writeStringField(bldgLayerStream, 1, "building") // name
        writeStringField(bldgLayerStream, 3, "render_height") // key[0] = "render_height"

        // Value[0] = float 25.5f (field 4 in Layer)
        val valueStream = ByteArrayOutputStream()
        val floatBits = java.lang.Float.floatToIntBits(25.5f)
        val floatBytes = byteArrayOf(
            (floatBits and 0xFF).toByte(),
            ((floatBits ushr 8) and 0xFF).toByte(),
            ((floatBits ushr 16) and 0xFF).toByte(),
            ((floatBits ushr 24) and 0xFF).toByte(),
        )
        writeVarint(valueStream, ((2.toLong() shl 3) or 5L)) // field 2 (float_value), wireType 5
        valueStream.write(floatBytes)
        writeField(bldgLayerStream, 4, 2, valueStream.toByteArray())

        // Building Feature (field 2 in Layer)
        val bldgFeatureStream = ByteArrayOutputStream()
        writeVarintField(bldgFeatureStream, 3, 3) // GeomType = POLYGON (3)

        // Tags: key 0 -> val 0
        val tagsStream = ByteArrayOutputStream()
        writeVarint(tagsStream, 0)
        writeVarint(tagsStream, 0)
        writeField(bldgFeatureStream, 2, 2, tagsStream.toByteArray())

        // Geometry: MoveTo(1) (500, 500), LineTo(3) dx,dy, ClosePath(7)
        val bldgGeomStream = ByteArrayOutputStream()
        writeVarint(bldgGeomStream, (1 or (1 shl 3)).toLong()) // MoveTo 1
        writeVarint(bldgGeomStream, encodeZigZag(500).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(500).toLong())

        writeVarint(bldgGeomStream, (2 or (3 shl 3)).toLong()) // LineTo 3
        writeVarint(bldgGeomStream, encodeZigZag(100).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(0).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(0).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(100).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(-100).toLong())
        writeVarint(bldgGeomStream, encodeZigZag(0).toLong())

        writeVarint(bldgGeomStream, (7 or (1 shl 3)).toLong()) // ClosePath 1 = 15

        writeField(bldgFeatureStream, 4, 2, bldgGeomStream.toByteArray())
        writeField(bldgLayerStream, 2, 2, bldgFeatureStream.toByteArray())

        // Add layers to tile message (field 3, wireType 2)
        writeField(tileStream, 3, 2, roadLayerStream.toByteArray())
        writeField(tileStream, 3, 2, bldgLayerStream.toByteArray())

        return tileStream.toByteArray()
    }

    @Test
    fun testDecodeSyntheticMvtUncompressed() {
        val payload = buildSyntheticMvtPayload()
        val coord = TileCoordinate(15, 7774, 12790)

        val geometry = MvtDecoder.decodeTile(coord, payload)
        assertNotNull(geometry)
        assertEquals(coord, geometry.coordinate)

        // 1 road polyline with 2 points
        assertEquals(1, geometry.roads.size)
        assertEquals(2, geometry.roads[0].size)

        // 1 building polygon with closed ring
        assertEquals(1, geometry.buildings.size)
        val bldg = geometry.buildings[0]
        assertEquals(25.5f, bldg.heightMeters, 0.01f)
        assertTrue("Building ring must have at least 4 points", bldg.points.size >= 4)
        assertEquals("Building ring must close on itself", bldg.points.first(), bldg.points.last())
    }

    @Test
    fun testDecodeSyntheticMvtGzipCompressed() {
        val raw = buildSyntheticMvtPayload()
        val gzipOut = ByteArrayOutputStream()
        GZIPOutputStream(gzipOut).use { it.write(raw) }
        val compressed = gzipOut.toByteArray()

        val coord = TileCoordinate(15, 7774, 12790)
        val geometry = MvtDecoder.decodeTile(coord, compressed)

        assertEquals(1, geometry.roads.size)
        assertEquals(1, geometry.buildings.size)
        assertEquals(25.5f, geometry.buildings[0].heightMeters, 0.01f)
    }
}
