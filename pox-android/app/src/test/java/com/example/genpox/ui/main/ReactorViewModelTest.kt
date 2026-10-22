package com.example.genpox.ui.main

import app.cash.turbine.test
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.util.FakeDataRepository
import com.example.genpox.util.TestDispatchersProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReactorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        com.example.genpox.data.WaveMath.isTestMode = true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        com.example.genpox.data.WaveMath.isTestMode = false
    }

    @Test
    fun testReactorTemperatureEmissionsWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = ReactorViewModel(
            geneStockRepository = repository,
            preferencesRepository = repository,
            creatureRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel.reactorTemperature.test {
            assertEquals(37.0f, awaitItem())

            viewModel.setReactorTemperature(45.0f)
            assertEquals(45.0f, awaitItem())
        }
    }

    @Test
    fun testActivePolymeraseEmissionsWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = ReactorViewModel(
            geneStockRepository = repository,
            preferencesRepository = repository,
            creatureRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel._bioWaste.value = 5000L
        viewModel.activePolymerase.test {
            assertEquals("Taq", awaitItem())

            viewModel.setActivePolymerase("Pfu")
            assertEquals("Pfu", awaitItem())
        }
    }

    @Test
    fun testInletRatiosEmissionsWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = ReactorViewModel(
            geneStockRepository = repository,
            preferencesRepository = repository,
            creatureRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel.inletRatioA.test {
            assertEquals(0.25f, awaitItem())

            viewModel.setInletRatioA(0.40f)
            assertEquals(0.40f, awaitItem())
        }
    }
}
