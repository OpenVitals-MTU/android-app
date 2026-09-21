package tech.mmarca.openvitals.core.performance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a file read or write on the IO pool. A document picker answers on the
 * main thread, and a stream behind it can be a slow disk or a network share.
 */
suspend fun <T> offMainIo(block: () -> T): Result<T> =
    withContext(Dispatchers.IO) { runCatchingCancellable(block) }
