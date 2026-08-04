package tech.mmarca.openvitals.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import tech.mmarca.openvitals.core.performance.DispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val testDispatcher = UnconfinedTestDispatcher()
    val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
