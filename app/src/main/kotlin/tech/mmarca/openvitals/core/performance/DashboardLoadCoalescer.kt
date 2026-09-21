package tech.mmarca.openvitals.core.performance

import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow

data class DashboardLoadCoalesceKey(
    val date: LocalDate,
    val sleepWindow: SleepWindow,
    val activityWeekMode: ActivityWeekMode,
    val visibleMetrics: String,
    val includeHistoricalBaselines: Boolean,
    val includeWeeklyTrainingSignals: Boolean,
    val permissionFingerprint: String,
    val showOpenVitalsCalculatedCalories: Boolean,
) {
    companion object {
        fun from(
            query: DashboardQuery,
            granted: Set<String>,
            showOpenVitalsCalculatedCalories: Boolean,
        ): DashboardLoadCoalesceKey =
            DashboardLoadCoalesceKey(
                date = query.date,
                sleepWindow = query.sleepWindow,
                activityWeekMode = query.activityWeekMode,
                visibleMetrics = query.visibleMetrics.sortedBy { it.name }.joinToString(",") { it.name },
                includeHistoricalBaselines = query.includeHistoricalBaselines,
                includeWeeklyTrainingSignals = query.includeWeeklyTrainingSignals,
                permissionFingerprint = granted.sorted().joinToString(separator = ","),
                showOpenVitalsCalculatedCalories = showOpenVitalsCalculatedCalories,
            )
    }
}

class DashboardLoadCoalescer {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<DashboardLoadCoalesceKey, CompletableDeferred<DashboardData>>()

    suspend fun getOrPut(
        key: DashboardLoadCoalesceKey,
        loader: suspend () -> DashboardData,
    ): DashboardData {
        val lookup = mutex.withLock {
            inFlight[key]?.let { return@withLock CoalesceLookup.Pending(it) }
            CompletableDeferred<DashboardData>().also { deferred ->
                inFlight[key] = deferred
            }.let(CoalesceLookup::Owner)
        }

        if (lookup is CoalesceLookup.Pending) {
            return lookup.deferred.await()
        }

        val pending = (lookup as CoalesceLookup.Owner).deferred
        return try {
            val value = loader()
            pending.complete(value)
            value
        } catch (t: Throwable) {
            pending.completeExceptionally(t)
            throw t
        } finally {
            // The owner can be cancelled mid-load, and releasing the slot needs the
            // mutex. Taking a contended lock on a cancelled coroutine throws, which
            // used to leave the key in flight with a result nobody would ever
            // complete: every later load of that key waited for good, and its tile
            // sat on "Loading..." until the app was restarted.
            withContext(NonCancellable) {
                mutex.withLock {
                    if (inFlight[key] === pending) {
                        inFlight.remove(key)
                    }
                }
                // A cancelled owner completes nothing above, so say so here. The
                // callers retry, and the key is free for them.
                pending.completeExceptionally(
                    CancellationException("dashboard load was cancelled before it finished"),
                )
            }
        }
    }

    private sealed interface CoalesceLookup {
        data class Pending(val deferred: CompletableDeferred<DashboardData>) : CoalesceLookup
        data class Owner(val deferred: CompletableDeferred<DashboardData>) : CoalesceLookup
    }
}
