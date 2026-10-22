package com.example.genpox.map.tile

import com.example.genpox.util.TestDispatchersProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TileStreamManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var testDispatchers: TestDispatchersProvider
    private lateinit var fakeDataProvider: FakeTileDataProvider
    private lateinit var streamManager: TileStreamManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testDispatchers = TestDispatchersProvider(testDispatcher)
        fakeDataProvider = FakeTileDataProvider()
        streamManager = TileStreamManager(
            tileDataProvider = fakeDataProvider,
            dispatchers = testDispatchers,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateCenter_streamsSurrounding3x3Tiles() = runTest {
        // Kansas City Downtown: ~39.0997, -94.5786 (Tile 14/3887/6255)
        streamManager.updateCenter(39.0997, -94.5786, radius = 1)
        advanceUntilIdle()

        val active = streamManager.activeTiles.value
        assertEquals(9, active.size)
        assertTrue(active.containsKey(TileCoordinate(14, 3887, 6255)))

        // Verify all 8 neighbors are present
        for (dx in -1..1) {
            for (dy in -1..1) {
                val expected = TileCoordinate(14, 3887 + dx, 6255 + dy)
                assertTrue("Missing neighbor tile $expected", active.containsKey(expected))
            }
        }
    }

    @Test
    fun testUpdateCenter_deduplicatesIdenticalLocation() = runTest {
        streamManager.updateCenter(39.0997, -94.5786, radius = 1)
        advanceUntilIdle()

        val initialCallCount = fakeDataProvider.requestedCoordinates.size
        assertEquals(9, initialCallCount)

        // Updating center with exact same coordinate (same tile) should be a NO-OP
        streamManager.updateCenter(39.0997, -94.5786, radius = 1)
        advanceUntilIdle()

        assertEquals(initialCallCount, fakeDataProvider.requestedCoordinates.size)
    }

    @Test
    fun testEvictionPolicy_capsAtMaxTiles() = runTest {
        // Stream multiple distant locations to exceed MAX_ACTIVE_TILES (25)
        for (i in 0..5) {
            // Space out points so they map to completely disjoint tiles
            val lat = 30.0 + i * 2.0
            val lng = -90.0 + i * 2.0
            streamManager.updateCenter(lat, lng, radius = 1)
            advanceUntilIdle()
        }

        val active = streamManager.activeTiles.value
        assertTrue("Active tiles (${active.size}) must not exceed 25", active.size <= 25)
    }
}
