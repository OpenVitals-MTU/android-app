package tech.mmarca.openvitals.core.performance

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundGate @Inject constructor() {

    @Volatile
    private var foreground: Boolean = false

    val isForeground: Boolean
        get() = foreground

    fun registerProcessLifecycle(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    foreground = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    foreground = false
                }
            },
        )
    }
}
