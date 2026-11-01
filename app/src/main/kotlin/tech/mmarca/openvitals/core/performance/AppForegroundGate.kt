package tech.mmarca.openvitals.core.performance

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundGate @Inject constructor() {

    private val foregroundState = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Observable [isForeground]. Garmin watches defer online errands until the companion is active. */
    val foregroundFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = foregroundState

    private var foreground: Boolean
        get() = foregroundState.value
        set(value) { foregroundState.value = value }

    val isForeground: Boolean
        get() = foregroundState.value

    /**
     * How many times the app has come to the foreground. A cache that must not
     * outlive a trip to another app (the Health Connect permission screen, say)
     * remembers the epoch it was filled in and is stale once this moves.
     */
    @Volatile
    var foregroundEpoch: Int = 0
        private set

    fun registerProcessLifecycle(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    foregroundEpoch += 1
                    foreground = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    foreground = false
                }
            },
        )
    }
}
