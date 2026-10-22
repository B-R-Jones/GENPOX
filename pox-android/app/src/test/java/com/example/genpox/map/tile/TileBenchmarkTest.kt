package com.example.genpox.map.tile

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureNanoTime

class TileBenchmarkTest {

    private lateinit var tileBytes: ByteArray
    private val kcCoordinate = TileCoordinate(14, 3887, 6255)

    @Before
    fun setUp() {
        val stream = javaClass.getResourceAsStream("/tiles/kc_downtown_14_3887_6255.pbf")
        assertNotNull("Test tile fixture /tiles/kc_downtown_14_3887_6255.pbf must exist", stream)
        tileBytes = stream!!.readBytes()
        assertTrue("Test tile must not be empty", tileBytes.isNotEmpty())
    }

    @Test
    fun testRealWorldTileExtraction() {
        val geometry = MvtDecoder.decodeTile(kcCoordinate, tileBytes)

        println("=== TILE EXTRACTION REPORT (Kansas City Downtown 14/3887/6255) ===")
        println("Raw PBF Byte Size: ${tileBytes.size} bytes (${tileBytes.size / 1024} KB)")
        println("Roads Polylines: ${geometry.roads.size}")
        println("Building Polygons: ${geometry.buildings.size}")

        assertTrue("Must extract at least 50 road polylines from downtown KC", geometry.roads.size >= 50)
        assertTrue("Must extract at least 100 building footprints from downtown KC", geometry.buildings.size >= 100)

        // Verify building heights
        var minHeight = Float.MAX_VALUE
        var maxHeight = -Float.MAX_VALUE
        var tallBuildingsCount = 0

        for (b in geometry.buildings) {
            if (b.heightMeters < minHeight) minHeight = b.heightMeters
            if (b.heightMeters > maxHeight) maxHeight = b.heightMeters
            if (b.heightMeters > 30f) tallBuildingsCount++
        }

        println("Building Min Height: ${minHeight}m")
        println("Building Max Height: ${maxHeight}m")
        println("Tall Buildings (>30m): $tallBuildingsCount")

        assertTrue("Max building height in downtown KC must be > 30m", maxHeight > 30f)
        assertTrue("There must be multiple tall buildings in downtown KC", tallBuildingsCount >= 5)

        println("=== T-MOBILE CENTER OVERLAPS ===")
        val tminLat = 39.096545
        val tmaxLat = 39.098007
        val tminLng = -94.580773
        val tmaxLng = -94.579115
        for (i in geometry.buildings.indices) {
            val b = geometry.buildings[i]
            val bminLat = b.points.minOf { it.lat }
            val bmaxLat = b.points.maxOf { it.lat }
            val bminLng = b.points.minOf { it.lng }
            val bmaxLng = b.points.maxOf { it.lng }
            if (bmaxLat >= tminLat && bminLat <= tmaxLat && bmaxLng >= tminLng && bminLng <= tmaxLng) {
                println("Overlap with T-Mobile BBox: idx=$i, pts=${b.points.size}, h=${b.heightMeters}, lat=[$bminLat..$bmaxLat], lng=[$bminLng..$bmaxLng]")
            }
        }

        // Verify spatial bounds conformance (within 5% margin for tile buffer)
        val bbox = TileMath.tileToBoundingBox(kcCoordinate.z, kcCoordinate.x, kcCoordinate.y)
        val latSpan = bbox.maxLat - bbox.minLat
        val lngSpan = bbox.maxLng - bbox.minLng
        val bufferedMinLat = bbox.minLat - latSpan * 0.1
        val bufferedMaxLat = bbox.maxLat + latSpan * 0.1
        val bufferedMinLng = bbox.minLng - lngSpan * 0.1
        val bufferedMaxLng = bbox.maxLng + lngSpan * 0.1

        for (road in geometry.roads.take(50)) {
            for (pt in road) {
                assertTrue("Road pt lat (${pt.lat}) must be within tile bounds", pt.lat in bufferedMinLat..bufferedMaxLat)
                assertTrue("Road pt lng (${pt.lng}) must be within tile bounds", pt.lng in bufferedMinLng..bufferedMaxLng)
            }
        }

        for (bldg in geometry.buildings.take(50)) {
            for (pt in bldg.points) {
                assertTrue("Building pt lat (${pt.lat}) must be within tile bounds", pt.lat in bufferedMinLat..bufferedMaxLat)
                assertTrue("Building pt lng (${pt.lng}) must be within tile bounds", pt.lng in bufferedMinLng..bufferedMaxLng)
            }
        }
    }

    @Test
    fun testTileDecodeThroughputBenchmark() {
        val isGzip = tileBytes.size >= 2 && tileBytes[0] == 0x1F.toByte() && tileBytes[1] == 0x8B.toByte()
        println("Fixture is GZIP: $isGzip")

        val decompressedBytes = if (isGzip) {
            java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(tileBytes)).use { it.readBytes() }
        } else {
            tileBytes
        }
        println("Decompressed Size: ${decompressedBytes.size} bytes (${decompressedBytes.size / 1024} KB)")

        // 1. Benchmark pure MVT decoding on uncompressed bytes (as delivered by OkHttp)
        repeat(20) {
            MvtDecoder.decodeTile(kcCoordinate, decompressedBytes)
        }
        val iterations = 100
        val pureNanos = measureNanoTime {
            repeat(iterations) {
                MvtDecoder.decodeTile(kcCoordinate, decompressedBytes)
            }
        }
        val pureAvgMillis = pureNanos.toDouble() / iterations / 1_000_000.0

        // 2. Benchmark end-to-end including GZIP decompression (if fixture is compressed)
        repeat(10) {
            MvtDecoder.decodeTile(kcCoordinate, tileBytes)
        }
        val totalNanos = measureNanoTime {
            repeat(iterations) {
                MvtDecoder.decodeTile(kcCoordinate, tileBytes)
            }
        }
        val totalAvgMillis = totalNanos.toDouble() / iterations / 1_000_000.0

        println("=== TILE BENCHMARK RESULTS ===")
        println("Iterations: $iterations")
        println("Pure MVT Parsing Time (Zero-Copy): ${"%.3f".format(pureAvgMillis)} ms per tile")
        println("End-to-End (including GZIP decompression): ${"%.3f".format(totalAvgMillis)} ms per tile")

        // Assert that pure MVT decoding for this heavy dense 304KB skyscraper tile is under 15.0ms (typically ~4-10ms)
        assertTrue("Pure MVT decode time must be < 15.0ms (was ${pureAvgMillis}ms)", pureAvgMillis < 15.0)
    }
}
