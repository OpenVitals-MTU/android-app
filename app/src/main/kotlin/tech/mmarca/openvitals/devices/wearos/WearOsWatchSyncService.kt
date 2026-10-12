package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.contract.BodyProfilePreferences
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.core.sync.insertSkippingPresent
import tech.mmarca.openvitals.devices.core.sync.sleepNightWindow
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.WearSleepMinute
import tech.mmarca.openvitals.features.imports.applehealth.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Pulls what the OpenVitals Wear OS app recorded and writes it to Health
 * Connect. The [DeviceSyncPort] for `(WATCH, WEAROS)` devices.
 *
 * Heart rate first: one page at a time from the watch's cursor, each page
 * written before the next is asked for, and the cursor advanced after each
 * write. A sync that dies mid-way therefore loses nothing: the next one
 * resumes at the last page written, and a page written twice upserts.
 *
 * Then steps, the same way, but each pull starts a few minutes before the
 * cursor: the watch rewrites a minute while late steps arrive, and a minute
 * that grew replaces its record. Two estimates come with the steps: cadence,
 * and walking energy when the body weight is known ([WearOsStepsImport]).
 * If the user withheld the write permission for one, it is skipped and the
 * sync goes on.
 *
 * Then sleep: the minutes from the start of the night the sleep cursor sits
 * in, every night those minutes touch estimated whole and written as one
 * session per night, replacing the earlier estimate. A night that already
 * holds a session from anywhere else, the vendor's app or the user's own
 * entry, is left alone. The sleep cursor moves once the nights are written.
 *
 * [DeviceSyncResult.Succeeded.fileCount] carries the number of heart rate
 * samples and [DeviceSyncResult.Succeeded.nightCount] the nights written;
 * the Wear OS card words them so.
 */
@Singleton
class WearOsWatchSyncService(
    private val nodePort: WearOsNodePort,
    private val cursors: WearOsSyncCursorStore,
    private val importRepository: AppleHealthImportRepository,
    private val bleDeviceRepository: BleDeviceRepository,
    private val recordingController: ActivityRecordingController,
    private val healthConnect: HealthConnectManager,
    private val linkStore: WearOsLinkStore,
    /** Body weight for the walking energy: Health Connect's latest, else the profile's. Null when neither has one. */
    private val weightKg: suspend () -> Double?,
    private val clock: () -> Instant,
    private val zone: () -> ZoneId,
) : DeviceSyncPort {

    @Inject
    constructor(
        nodePort: WearOsNodePort,
        cursors: WearOsSyncCursorStore,
        importRepository: AppleHealthImportRepository,
        bleDeviceRepository: BleDeviceRepository,
        recordingController: ActivityRecordingController,
        healthConnect: HealthConnectManager,
        linkStore: WearOsLinkStore,
        bodyRepository: BodyRepository,
        bodyProfilePreferences: BodyProfilePreferences,
    ) : this(
        nodePort, cursors, importRepository, bleDeviceRepository, recordingController, healthConnect, linkStore,
        { bodyRepository.resolveBodyProfile(bodyProfilePreferences.bodyProfile()).weightKg },
        Instant::now, ZoneId::systemDefault,
    )

    private val syncMutex = Mutex()

    init {
        WearOsLog.installLogcatSink()
    }

    override fun canSync(device: BleSensorDevice): Boolean = device.isWearosWatch

    override suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult = syncMutex.withLock {
        syncSerially(device, onProgress)
    }

    private suspend fun syncSerially(
        device: BleSensorDevice,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult {
        // A live recording holds the foreground slot. Refuse, as the Garmin sync does.
        if (recordingController.state.value.isActive) {
            return DeviceSyncResult.Failed(ScreenError.Text(R.string.settings_watch_wearos_fail_recording))
        }

        onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.HANDSHAKE))
        val progress = Progress(onProgress)
        try {
            if (!pullHeartRate(device, progress)) return notPaired(device)
            if (!pullSteps(device, progress)) return notPaired(device)
            if (!pullSleep(device, progress)) return notPaired(device)
        } catch (error: CancellationException) {
            throw error
        } catch (failure: WearOsLinkFailure) {
            linkStore.record(device.id, WearOsLinkSnapshot(failure.toAppStatus(), checkedAt = clock()))
            WearOsLog.log("sync failed: ${failure.message}")
            return DeviceSyncResult.Failed(
                if (progress.written > 0 && failure is WearOsLinkFailure.NoAnswer) {
                    ScreenError.Text(R.string.settings_watch_wearos_fail_partial)
                } else {
                    failure.toScreenError()
                },
            )
        } catch (error: SecurityException) {
            return DeviceSyncResult.Failed(permissionError(error))
        } catch (error: Exception) {
            if (error.isPermissionFailure()) return DeviceSyncResult.Failed(permissionError(error))
            WearOsLog.log("sync failed: $error")
            return DeviceSyncResult.Failed(ScreenError.Text(R.string.settings_watch_wearos_fail_generic))
        }
        linkStore.record(device.id, WearOsLinkSnapshot(WearOsAppStatus.APP_RUNNING, checkedAt = clock()))

        onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.COMPLETE, filesTotal = progress.pages, filesDone = progress.pages))
        bleDeviceRepository.markSynced(device.id, clock())
        return DeviceSyncResult.Succeeded(progress.written, progress.nights, progress.missingBodyWeight)
    }

    /** False when no bonded watch matches. The samples written are counted on [progress]. */
    private suspend fun pullHeartRate(device: BleSensorDevice, progress: Progress): Boolean {
        var cursor = cursors.heartRateCursor(device.id)
        var pages = 0
        while (true) {
            val page = retryOnce { nodePort.pullHeartRate(device.address, device.bluetoothName, cursor) } ?: return false
            progress.page()
            val newest = page.samples.maxOfOrNull { it.time }
            if (newest != null) {
                importRepository.insertImportedRecords(WearOsHeartRateImport.records(page.samples))
                progress.written += page.samples.size
                cursor = newest
                cursors.setHeartRateCursor(device.id, cursor)
            }
            if (!page.hasMore || newest == null) break
            if (++pages >= MAX_PAGES) break
        }
        return true
    }

    /** False when no bonded watch matches. */
    private suspend fun pullSteps(device: BleSensorDevice, progress: Progress): Boolean {
        val cursor = cursors.stepsCursor(device.id)
        var since = if (cursor == Instant.EPOCH) cursor else cursor.minusMillis(STEPS_RESTATED_MILLIS)
        var pages = 0
        var written = 0
        var pulledSteps = 0
        var energyMinutes = 0
        var energyKcal = 0.0
        val weight = weightKg()
        while (true) {
            val page = retryOnce { nodePort.pullSteps(device.address, device.bluetoothName, since) } ?: return false
            progress.page()
            val newest = page.minutes.maxOfOrNull { it.start }
            if (newest != null) {
                written += importRepository.insertSkippingPresent(WearOsStepsImport.records(page.minutes))
                writeEstimates("cadence", WearOsStepsImport.cadenceRecords(page.minutes))
                val energy = WearOsStepsImport.activeCaloriesRecords(page.minutes, weight)
                writeEstimates("walking energy", energy)
                pulledSteps += page.minutes.sumOf { it.steps }
                energyMinutes += energy.size
                energyKcal += energy.sumOf { (it as ActiveCaloriesBurnedRecord).energy.inKilocalories }
                if (weight == null && !progress.missingBodyWeight) {
                    WearOsLog.log("steps: no body weight, so no walking energy")
                    progress.missingBodyWeight = true
                }
                if (newest.isAfter(cursors.stepsCursor(device.id))) cursors.setStepsCursor(device.id, newest)
                since = newest
            }
            if (!page.hasMore || newest == null) break
            if (++pages >= MAX_PAGES) break
        }
        // The watch has no screen for these yet; testers read this line.
        WearOsLog.log(
            "steps: $written minute(s) written, $pulledSteps steps pulled (re-pulled minutes count again); " +
                if (weight == null) "no walking energy" else "walking energy ${"%.1f".format(energyKcal)} kcal over $energyMinutes minute(s) at $weight kg",
        )
        return true
    }

    /** False when no bonded watch matches. */
    private suspend fun pullSleep(device: BleSensorDevice, progress: Progress): Boolean {
        val cursor = cursors.sleepCursor(device.id)
        val minutes = ArrayList<WearSleepMinute>()
        var since = WearOsSleepImport.pullStart(cursor, zone().rules.getOffset(cursor))
        var pages = 0
        while (true) {
            val page = retryOnce { nodePort.pullSleepMinutes(device.address, device.bluetoothName, since) } ?: return false
            progress.page()
            minutes += page.minutes
            val newest = page.minutes.maxOfOrNull { it.time }
            if (!page.hasMore || newest == null) break
            since = newest
            if (++pages >= MAX_PAGES) break
        }
        val newest = minutes.maxOfOrNull { it.time } ?: return true

        var nights = 0
        for ((night, offset) in WearOsSleepImport.touchedNights(minutes, cursor)) {
            val (from, to) = sleepNightWindow(night, offset)
            if (hasForeignSession(from, to)) {
                WearOsLog.log("$night: Health Connect already holds a session from elsewhere, skipping")
                continue
            }
            val estimated = WearOsSleepImport.night(minutes, night, offset)
            if (estimated == null) {
                WearOsLog.log("$night: no night in the minutes")
                continue
            }
            WearOsLog.log("$night: ${estimated.summary()}")
            val record = WearOsSleepImport.record(estimated, night, offset, version = clock().toEpochMilli())
            if (record == null) {
                WearOsLog.log("$night: nothing counted as sleep")
                continue
            }
            writeReplacing(record)
            nights++
        }
        cursors.setSleepCursor(device.id, newest)
        progress.nights = nights
        WearOsLog.log("sleep: $nights night(s) written from ${minutes.size} minutes")
        return true
    }

    /**
     * A page pull again after one link failure. The first connection to a
     * watch that has been dozing in bedtime mode is refused now and then and
     * the next one goes through; a second failure is reported as before.
     */
    private suspend fun <T> retryOnce(pull: suspend () -> T): T = try {
        pull()
    } catch (failure: WearOsLinkFailure.NoAnswer) {
        WearOsLog.log("link failed once (${failure.message}); trying again")
        kotlinx.coroutines.delay(RETRY_DELAY_MILLIS)
        pull()
    }

    /**
     * Writes derived records. If the user withheld the permission for their
     * type, they are skipped and logged, and the sync goes on.
     */
    private suspend fun writeEstimates(what: String, records: List<Record>) {
        if (records.isEmpty()) return
        try {
            importRepository.insertSkippingPresent(records)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isPermissionFailure()) throw error
            WearOsLog.log("$what: not written, Health Connect permission missing")
        }
    }

    /** Whether the window holds a sleep session that is not this import's. A read failure counts as no. */
    private suspend fun hasForeignSession(from: Instant, to: Instant): Boolean {
        var found = false
        try {
            healthConnect.forEachSyncRecordPage(SleepSessionRecord::class, from, to) { page ->
                if (page.any { !WearOsSleepImport.isOwnRecordId(it.metadata.clientRecordId) }) found = true
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WearOsLog.log("Could not read existing sleep sessions: $error")
        }
        return found
    }

    /** A higher version replaces the earlier estimate. If Health Connect still objects, delete and retry once. */
    private suspend fun writeReplacing(record: SleepSessionRecord) {
        try {
            importRepository.insertImportedRecords(listOf(record))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isDuplicateClientRecordFailure()) throw error
            val id = requireNotNull(record.metadata.clientRecordId)
            healthConnect.deleteImportedRecordsByClientIds(SleepSessionRecord::class, listOf(id))
            importRepository.insertImportedRecords(listOf(record))
        }
    }

    private fun notPaired(device: BleSensorDevice): DeviceSyncResult.Failed {
        linkStore.record(device.id, WearOsLinkSnapshot(WearOsAppStatus.NOT_PAIRED, checkedAt = clock()))
        return DeviceSyncResult.Failed(ScreenError.Text(R.string.settings_watch_wearos_bt_not_paired))
    }

    /** The Bluetooth grant shows as the grant affordance; a Health Connect refusal names the write. */
    private fun permissionError(error: Throwable): ScreenError =
        if (error is SecurityException && error.message?.contains("BLUETOOTH", ignoreCase = true) == true) {
            ScreenError.PermissionDenied
        } else {
            ScreenError.Text(R.string.settings_watch_wearos_fail_health_connect)
        }

    /** Counts the pages pulled, for the progress ticks, and the heart rate samples written. */
    private class Progress(private val onProgress: ((DeviceSyncProgress) -> Unit)?) {
        var pages = 0
            private set
        var written = 0
        var nights = 0

        /** Steps came in but no body weight was known, so the walking energy was skipped. */
        var missingBodyWeight = false

        fun page() {
            pages++
            onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.DOWNLOADING, filesTotal = pages, filesDone = pages - 1))
        }
    }

    private companion object {
        /** A safety net against a watch that always says "more": two million samples. */
        const val MAX_PAGES = 1000

        const val RETRY_DELAY_MILLIS = 1_500L

        /** How far a step pull reaches back past its cursor: the watch rewrites a minute for up to its batch latency. */
        const val STEPS_RESTATED_MILLIS = 5 * 60_000L
    }
}
