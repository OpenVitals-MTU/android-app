package tech.mmarca.openvitals.core.performance

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val main: CoroutineContext
    val io: CoroutineContext
    val default: CoroutineContext
}

object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineContext = Dispatchers.Main
    override val io: CoroutineContext = Dispatchers.IO
    override val default: CoroutineContext = Dispatchers.Default
}

enum class RefreshMode {
    NORMAL,
    FORCE,
}
