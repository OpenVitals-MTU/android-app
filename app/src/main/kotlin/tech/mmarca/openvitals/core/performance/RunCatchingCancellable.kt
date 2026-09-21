package tech.mmarca.openvitals.core.performance

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] for suspending work. A plain runCatching also catches the
 * CancellationException that stops a coroutine. The caller then treats "stop" as one more
 * failure, carries on, and the cancellation is lost.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
