package tech.mmarca.openvitals.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CancellationException

class ScreenErrorTest {

    @Test fun `toScreenError uses throwable message when present`() {
        val error = RuntimeException("timeout").toScreenError()
        assertEquals(ScreenError.Message("timeout"), error)
    }

    @Test fun `toScreenError uses fallback when message blank`() {
        val error = RuntimeException("").toScreenError("fallback")
        assertEquals(ScreenError.Message("fallback"), error)
    }

    @Test fun `toScreenError logs throwable conversion`() {
        val throwable = RuntimeException("timeout")
        var loggedTag: String? = null
        var loggedMessage: String? = null
        var loggedThrowable: Throwable? = null

        val previousSink = ScreenErrorHandler.sink
        ScreenErrorHandler.sink = { tag, message, error ->
            loggedTag = tag
            loggedMessage = message
            loggedThrowable = error
        }
        try {
            throwable.toScreenError(
                logTag = "ActivityEntryViewModel",
                logMessage = "Activity file import failed",
            )
        } finally {
            ScreenErrorHandler.sink = previousSink
        }

        assertEquals("ActivityEntryViewModel", loggedTag)
        assertEquals("Activity file import failed", loggedMessage)
        assertEquals(throwable, loggedThrowable)
    }

    @Test fun `toScreenError rethrows coroutine cancellation`() {
        val cancellation = CancellationException("cancelled")

        val failure = runCatching {
            cancellation.toScreenError()
        }

        assertTrue(failure.isFailure)
        assertEquals(cancellation, failure.exceptionOrNull())
    }

    @Test fun `onScreenError maps failed result through centralized handler`() {
        val throwable = RuntimeException("timeout")
        var handledError: ScreenError? = null

        val previousSink = ScreenErrorHandler.sink
        ScreenErrorHandler.sink = { _, _, _ -> }
        try {
            Result.failure<Unit>(throwable)
                .onScreenError { error ->
                    handledError = error
                }
        } finally {
            ScreenErrorHandler.sink = previousSink
        }

        assertEquals(ScreenError.Message("timeout"), handledError)
    }

    @Test fun `message error holds text`() {
        val error: ScreenError = ScreenError.Message("custom")
        assertTrue(error is ScreenError.Message)
        assertEquals("custom", (error as ScreenError.Message).text)
    }
}
