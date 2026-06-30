package tech.mmarca.openvitals.core.performance

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

data class DashboardLoadCoalesceKey(
    val date: LocalDate,
    val sleepRangeMode: SleepRangeMode,
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
                sleepRangeMode = query.sleepRangeMode,
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
            mutex.withLock {
                if (inFlight[key] === pending) {
                    inFlight.remove(key)
                }
            }
            pending.complete(value)
            value
        } catch (t: Throwable) {
            mutex.withLock {
                if (inFlight[key] === pending) {
                    inFlight.remove(key)
                }
            }
            pending.completeExceptionally(t)
            throw t
        }
    }

    private sealed interface CoalesceLookup {
        data class Pending(val deferred: CompletableDeferred<DashboardData>) : CoalesceLookup
        data class Owner(val deferred: CompletableDeferred<DashboardData>) : CoalesceLookup
    }
}
