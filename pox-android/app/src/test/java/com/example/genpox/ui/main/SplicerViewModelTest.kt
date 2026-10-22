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
class SplicerViewModelTest {

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
    fun testActiveSlotSelectionWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = SplicerViewModel(
            geneStockRepository = repository,
            preferencesRepository = repository,
            creatureRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel.activeSlotSelection.test {
            assertEquals(null, awaitItem())

            viewModel.selectSplicerSlot(2)
            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun testCustomScaffoldModeWithTurbine() = runTest {
        val repository = FakeDataRepository()
        val dispatchers = TestDispatchersProvider(testDispatcher)
        val synthManager = PoxSynthManager().apply { isMuted = true }
        val viewModel = SplicerViewModel(
            geneStockRepository = repository,
            preferencesRepository = repository,
            creatureRepository = repository,
            synthManager = synthManager,
            dispatchers = dispatchers,
        )

        viewModel.isCustomScaffoldMode.test {
            assertEquals(false, awaitItem())

            viewModel.setCustomScaffoldMode(true)
            assertEquals(true, awaitItem())
        }
    }
}
