package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Spaces Health Connect calls out. A quota counts calls in a window, and a
 * burst of a hundred in three seconds is what spends it; the same hundred
 * over twenty seconds is not. A dashboard that fills tile by tile does not
 * notice the difference, the standalone Health Connect app does.
 *
 * [baseGapMillis] is the gap between call starts from the first call on:
 * zero on the platform provider (Android 14+), a breath on the standalone
 * one. After the first refusal anywhere in the process, every call keeps
 * [afterRefusalGapMillis] whatever the provider.
 */
internal class HealthConnectCallPacer(
    private val nowMillis: () -> Long,
    private val baseGapMillis: Long,
    private val afterRefusalGapMillis: Long = AfterRefusalGapMillis,
) {
    private val lock = Mutex()
    private var nextStartAtMillis = 0L

    /** Takes the next start slot. Returns how long the caller waits before it, 0 when none. */
    suspend fun reserve(afterRefusal: Boolean): Long {
        val gap = if (afterRefusal) maxOf(baseGapMillis, afterRefusalGapMillis) else baseGapMillis
        if (gap <= 0L) return 0L
        return lock.withLock {
            val now = nowMillis()
            val startAt = maxOf(now, nextStartAtMillis)
            nextStartAtMillis = startAt + gap
            startAt - now
        }
    }

    companion object {
        /** Between call starts on the standalone provider. */
        const val StandaloneProviderGapMillis = 150L

        /** Between call starts once a refusal has been seen, on any provider. */
        const val AfterRefusalGapMillis = 250L
    }
}
