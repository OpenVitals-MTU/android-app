package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * When the app lock asks for the device credential. Times are from a clock that
 * keeps running while the phone sleeps.
 *
 * It lives in a ViewModel: a rotation keeps it, and a new process starts locked.
 * Saved instance state would bring "unlocked" back after the process died.
 */
class AppLockState(private val relockAfterMillis: Long = RELOCK_AFTER_MILLIS) {

    var locked by mutableStateOf(true)
        private set

    /** True once per lock. The prompt opens by itself; after a cancel only the button asks again. */
    var promptPending by mutableStateOf(true)
        private set

    /** The app's screens are first composed after the first unlock, then kept under the lock screen. */
    var everUnlocked by mutableStateOf(false)
        private set

    private var stoppedAtMillis: Long? = null

    fun onStop(nowMillis: Long) {
        stoppedAtMillis = nowMillis
    }

    fun onStart(nowMillis: Long) {
        val awayMillis = stoppedAtMillis?.let { nowMillis - it }
        stoppedAtMillis = null
        if (!locked && awayMillis != null && awayMillis >= relockAfterMillis) {
            locked = true
            promptPending = true
        }
    }

    fun onPromptLaunched() {
        promptPending = false
    }

    fun onPromptResult(confirmed: Boolean) {
        if (confirmed) unlock()
    }

    /** A phone with no screen lock has nothing to ask for. */
    fun unlock() {
        locked = false
        everUnlocked = true
    }

    /** The Unlock button. */
    fun requestPrompt() {
        if (locked) promptPending = true
    }

    companion object {
        /** Long enough to answer a message, short enough for a phone left on a table. */
        const val RELOCK_AFTER_MILLIS: Long = 60_000
    }
}

class AppLockViewModel : ViewModel() {
    val state = AppLockState()
}
