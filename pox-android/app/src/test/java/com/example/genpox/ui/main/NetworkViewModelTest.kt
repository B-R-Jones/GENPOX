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
class NetworkViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testNetworkSimulationActiveWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = NetworkViewModel(
            creatureRepository = repository,
            geneStockRepository = repository,
            preferencesRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel.isNetworkSimulationActive.test {
            assertEquals(false, awaitItem())

            viewModel.toggleNetworkSimulation(true)
            assertEquals(true, awaitItem())
        }
    }
}
