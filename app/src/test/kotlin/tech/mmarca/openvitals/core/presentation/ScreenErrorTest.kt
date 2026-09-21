package tech.mmarca.openvitals.core.presentation

import tech.mmarca.openvitals.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.CancellationException

class ScreenErrorTest {

    @Test fun `toScreenError uses throwable message when present`() {
        val error = RuntimeException("timeout").toScreenError()
        assertEquals(ScreenError.Message("timeout"), error)
    }

    @Test fun `a permission failure becomes ScreenError PermissionDenied`() {
        // Health Connect refuses an ungranted read with SecurityException; the screens need the type to offer a way out.
        val error = SecurityException("steps write").toScreenError()
        assertEquals(ScreenError.PermissionDenied, error)
    }

    @Test fun `a wrapped permission failure still becomes PermissionDenied`() {
        val error = RuntimeException("Load failed", SecurityException("steps write"))
            .toScreenError()
        assertEquals(ScreenError.PermissionDenied, error)
    }

    @Test fun `a non-permission failure keeps its message`() {
        val error = IllegalStateException("the provider hung up").toScreenError()
        assertEquals(ScreenError.Message("the provider hung up"), error)
    }

    @Test fun `a failure with no message shows our own sentence, which is translated`() {
        val error = RuntimeException("").toScreenError(R.string.screen_error_load_activity)
        assertEquals(ScreenError.Text(R.string.screen_error_load_activity), error)
    }

    @Test fun `with no sentence named, the generic one is used`() {
        assertEquals(ScreenError.Text(R.string.screen_error_generic), RuntimeException().toScreenError())
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
