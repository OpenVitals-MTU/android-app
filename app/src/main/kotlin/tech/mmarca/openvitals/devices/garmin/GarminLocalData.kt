package tech.mmarca.openvitals.devices.garmin

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.contract.GarminSleepMinuteRepository
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.devices.garmin.wellness.SleepMinuteRetention

/**
 * Owns what a Garmin watch leaves on the phone outside Health Connect:
 *
 * - copies of downloaded files, kept 30 days to replay an import;
 * - per-minute sleep data, kept 45 days to estimate sleep stages;
 * - wellness series Health Connect has no type for. This table is their only copy.
 *
 * The first two used to age out only inside a sync. A watch that was removed, lost or
 * broken never syncs again, so they stayed until the app was uninstalled.
 */
@Singleton
class GarminLocalData @Inject constructor(
    private val fileStore: GarminFileStore,
    private val sleepMinutes: GarminSleepMinuteRepository,
    private val wellness: GarminWellnessRepository,
) {
    private val housekeepingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** For app start, fire-and-forget. */
    fun pruneOnAppStart() {
        housekeepingScope.launch { runCatching { prune(Instant.now()) } }
    }

    /** Applies both retention rules. A sync does the same. */
    suspend fun prune(now: Instant) {
        fileStore.prune(now)
        sleepMinutes.pruneBefore(now.minusMillis(SleepMinuteRetention.inWholeMilliseconds))
    }

    /**
     * For when no Garmin watch is left. The file copies and the sleep minutes only serve a
     * paired watch, so they go. The wellness history is the user's record and has no other
     * copy, so it goes only when the user asked: [deleteWellnessHistory].
     */
    suspend fun clearAfterLastWatchRemoved(deleteWellnessHistory: Boolean) {
        fileStore.clearAll()
        sleepMinutes.deleteAll()
        if (deleteWellnessHistory) wellness.deleteAll()
    }
}
