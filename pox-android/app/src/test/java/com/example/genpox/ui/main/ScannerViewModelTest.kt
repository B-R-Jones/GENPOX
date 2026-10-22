package com.example.genpox.ui.main

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.AnomalySpectralClass
import com.example.genpox.map.tile.FakeTileDataProvider
import com.example.genpox.map.tile.TileStreamManager
import com.example.genpox.util.FakeDataRepository
import com.example.genpox.util.FakeLocationRepository
import com.example.genpox.util.TestDispatchersProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val activeViewModels = mutableListOf<ScannerViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        com.example.genpox.data.WaveMath.isTestMode = true
    }

    @After
    fun tearDown() {
        activeViewModels.forEach { vm ->
            vm.stopAllTickers()
            vm.viewModelScope.coroutineContext.cancelChildren()
        }
        activeViewModels.clear()
        Dispatchers.resetMain()
        com.example.genpox.data.WaveMath.isTestMode = false
    }

    private fun createViewModel(
        repository: FakeDataRepository = FakeDataRepository(),
        locationRepository: FakeLocationRepository = FakeLocationRepository(),
        fakeDataProvider: FakeTileDataProvider = FakeTileDataProvider(),
    ): Pair<ScannerViewModel, TileStreamManager> {
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val tileStreamManager = TileStreamManager(
            tileDataProvider = fakeDataProvider,
            dispatchers = dispatchers,
        )
        val viewModel = ScannerViewModel(
            missionRepository = repository,
            creatureRepository = repository,
            preferencesRepository = repository,
            geneStockRepository = repository,
            synthManager = synthManager,
            locationRepository = locationRepository,
            tileStreamManager = tileStreamManager,
            dispatchers = dispatchers,
        )
        activeViewModels.add(viewModel)
        return Pair(viewModel, tileStreamManager)
    }

    @Test
    fun testTrackedMissionIdWithTurbine() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.trackedMissionId.test {
            assertEquals(null, awaitItem())

            viewModel.setTrackedMissionId("MSN-123")
            assertEquals("MSN-123", awaitItem())
        }
    }

    @Test
    fun testSelectedAnomalyIdWithTurbine() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.selectedAnomalyId.test {
            assertEquals(null, awaitItem())

            viewModel.setSelectedAnomalyId("ANOM-456")
            assertEquals("ANOM-456", awaitItem())
        }
    }

    @Test
    fun testSingleTestAnomalySphereGenerated() = runTest {
        val (viewModel, _) = createViewModel()

        val anomalies = viewModel.anomalies.value
        assertEquals(1, anomalies.size)
        val testSphere = anomalies[0]
        assertEquals("ANM-TEST-CLASS-M", testSphere.id)
        assertEquals("Class M Conduit-Creeper", testSphere.name)
        assertEquals(viewModel.latitude.value, testSphere.lat, 0.0001)
        assertEquals(viewModel.longitude.value, testSphere.lng, 0.0001)
        assertEquals(100.0 * 3.28084, testSphere.heatZoneDiameter, 0.01)
        assertEquals(0.0, testSphere.distance, 0.01)
        assertEquals(AnomalySpectralClass.CLASS_M, testSphere.spectralClass)
    }

    @Test
    fun testCameraPanUpdatesTileStreamManager() = runTest {
        val fakeDataProvider = FakeTileDataProvider()
        val (viewModel, streamManager) = createViewModel(fakeDataProvider = fakeDataProvider)

        viewModel.updateCameraPan(500f, -500f)

        // After camera pan, active tiles flow is updated with tiles around the panned center
        val active = streamManager.activeTiles.value
        assertTrue("Active tiles should be streamed around camera target", active.isNotEmpty())
    }

    @Test
    fun testLocationUpdateTriggersTileStream() = runTest {
        val fakeDataProvider = FakeTileDataProvider()
        val (viewModel, streamManager) = createViewModel(fakeDataProvider = fakeDataProvider)

        // KC Downtown coordinates
        viewModel.updateLocation(39.0997, -94.5786)

        assertEquals(39.0997, viewModel.latitude.value, 0.0001)
        assertEquals(-94.5786, viewModel.longitude.value, 0.0001)

        val active = streamManager.activeTiles.value
        assertTrue("Active tiles should stream around updated user location", active.isNotEmpty())
    }
}
