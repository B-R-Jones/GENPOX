package com.example.genpox.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestDispatchersProvider(
    private val testDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : DispatchersProvider {
    override val main: CoroutineDispatcher get() = testDispatcher
    override val io: CoroutineDispatcher get() = testDispatcher
    override val default: CoroutineDispatcher get() = testDispatcher
    override val unconfined: CoroutineDispatcher get() = testDispatcher
}
