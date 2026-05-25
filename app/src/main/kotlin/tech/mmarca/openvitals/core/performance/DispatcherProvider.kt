package tech.mmarca.openvitals.core.performance

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val main: CoroutineContext
    val io: CoroutineContext
}

object DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineContext = Dispatchers.Main
    override val io: CoroutineContext = Dispatchers.IO
}

enum class RefreshMode {
    NORMAL,
    FORCE,
}
