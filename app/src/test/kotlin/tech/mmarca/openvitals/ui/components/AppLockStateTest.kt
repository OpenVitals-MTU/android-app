package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The lock used to open once per task and stay open, even across a process death. */
class AppLockStateTest {

    private val state = AppLockState(relockAfterMillis = 60_000)

    @Test
    fun `a new process starts locked and asks at once`() {
        assertTrue(state.locked)
        assertTrue(state.promptPending)
        assertFalse(state.everUnlocked)
    }

    @Test
    fun `a confirmed prompt unlocks`() {
        state.onPromptLaunched()
        state.onPromptResult(confirmed = true)

        assertFalse(state.locked)
        assertTrue(state.everUnlocked)
    }

    @Test
    fun `a cancelled prompt stays locked and does not ask again by itself`() {
        state.onPromptLaunched()
        state.onPromptResult(confirmed = false)

        assertTrue(state.locked)
        // Asking again at once would trap the user in a loop of prompts.
        assertFalse(state.promptPending)

        state.requestPrompt()

        assertTrue(state.promptPending)
    }

    @Test
    fun `a short trip to another app does not lock`() {
        unlock()

        state.onStop(nowMillis = 1_000)
        state.onStart(nowMillis = 1_000 + 59_999)

        assertFalse(state.locked)
    }

    @Test
    fun `a minute away locks again and asks again`() {
        unlock()

        state.onStop(nowMillis = 1_000)
        state.onStart(nowMillis = 1_000 + 60_000)

        assertTrue(state.locked)
        assertTrue(state.promptPending)
        // The screens stay composed under the lock screen.
        assertTrue(state.everUnlocked)
    }

    @Test
    fun `a rotation does not lock`() {
        unlock()

        state.onStop(nowMillis = 5_000)
        state.onStart(nowMillis = 5_040)

        assertFalse(state.locked)
    }

    @Test
    fun `a long prompt does not lock the user out after they confirm`() {
        // The prompt is another activity, so ours stops while it shows.
        state.onPromptLaunched()
        state.onStop(nowMillis = 0)
        state.onStart(nowMillis = 300_000)
        state.onPromptResult(confirmed = true)

        assertFalse(state.locked)
    }

    @Test
    fun `a start with no stop before it changes nothing`() {
        unlock()

        state.onStart(nowMillis = 999_999)

        assertFalse(state.locked)
    }

    private fun unlock() {
        state.onPromptLaunched()
        state.onPromptResult(confirmed = true)
    }
}
