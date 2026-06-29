package tech.mmarca.openvitals.core.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenErrorTest {

    @Test fun `toScreenError uses throwable message when present`() {
        val error = RuntimeException("timeout").toScreenError()
        assertEquals(ScreenError.Message("timeout"), error)
    }

    @Test fun `toScreenError uses fallback when message blank`() {
        val error = RuntimeException("").toScreenError("fallback")
        assertEquals(ScreenError.Message("fallback"), error)
    }

    @Test fun `message error holds text`() {
        val error = ScreenError.Message("custom")
        assertTrue(error is ScreenError.Message)
        assertEquals("custom", (error as ScreenError.Message).text)
    }
}
