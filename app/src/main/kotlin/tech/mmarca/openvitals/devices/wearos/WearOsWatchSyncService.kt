package tech.mmarca.openvitals.devices.wearos

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
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
import tech.mmarca.openvitals.devices.core.sync.UnwrittenSeries
import tech.mmarca.openvitals.data.repository.RecordWrite
import tech.mmarca.openvitals.data.repository.insertWithOutcomes
import tech.mmarca.openvitals.devices.core.sync.sleepNightWindow
import tech.mmarca.openvitals.domain.insights.WearSleepEstimator
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.WearSleepMinute
import tech.mmarca.openvitals.data.repository.isDuplicateClientRecordFailure
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.wearlink.AscentCredit
import tech.mmarca.openvitals.healthconnect.withStrictHealthConnectReads

/**
 * Pulls what the OpenVitals Wear OS app recorded and writes it to Health
 * Connect. The [DeviceSyncPort] for `(WATCH, WEAROS)` devices.
 *
 * Heart rate first: one page at a time from the watch's cursor, each page
 * written before the next is asked for, and the cursor advanced after each
 * write. A sync that dies mid-way therefore loses nothing: the next one
 * resumes at the last page written, and a page written twice upserts.
 *
 * Then steps, the same way, but each pull also takes the cursor's minute
 * again: the watch adds late steps only to its newest minute, and a minute
 * that grew replaces its record. Without the Health Connect grant to write
 * steps, the steps stay on the watch and the sync goes on. Two estimates
 * come with the steps: cadence, and walking energy when the body weight is
 * known ([WearOsStepsImport]). If the user withheld the write permission for
 * one, it is skipped and the sync goes on. Then floors, the same way; only
 * minutes with steps are written ([WearOsFloorsImport]), so they wait for
 * the steps grant too.
 *
 * Then sleep: the minutes from the start of the night the sleep cursor sits
 * in, every night those minutes touch estimated whole and written as one
 * session per night, replacing the earlier estimate. A night that already
 * holds a session from anywhere else, the vendor's app or the user's own
 * entry, is left alone. Each finished night with sleep also gives a nocturnal
 * resting heart rate ([WearOsRestingHeartRateImport]). The app averages the
 * values of a calendar day, so a day that holds one from elsewhere gets none
 * from here, and one that turns up later removes ours. Without the grants to
 * read and write it, or the right to read other apps' data, none is written.
 * The sleep cursor moves once the nights are written.
 *
 * If the user withheld the write permission for an estimate, it is skipped
 * and the sync goes on.
 *
 * [DeviceSyncResult.Succeeded.fileCount] carries the number of heart rate
 * samples, [DeviceSyncResult.Succeeded.stepMinuteCount] the minutes of
 * steps and [DeviceSyncResult.Succeeded.nightCount] the nights written; the
 * Wear OS card words them so.
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
        // Strict: the readers turn a failed read into null, which would pass for "no weight".
        { withStrictHealthConnectReads { bodyRepository.resolveBodyProfile(bodyProfilePreferences.bodyProfile()).weightKg } },
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
            val steps = pullSteps(device, progress) ?: return notPaired(device)
            // The floors filter needs the steps, so floors wait when the steps were not pulled.
            if (STEPS_WRITE_PERMISSION !in progress.withheldPermissions && !pullAscent(device, progress, steps)) {
                return notPaired(device)
            }
            if (!pullSleep(device, progress)) return notPaired(device)
        } catch (error: CancellationException) {
            throw error
        } catch (failure: WearOsLinkFailure) {
            linkStore.record(device.id, WearOsLinkSnapshot(failure.toAppStatus(), checkedAt = clock()))
            WearOsLog.log("sync failed: ${failure.message}")
            return DeviceSyncResult.Failed(
                if (progress.hasWritten && failure is WearOsLinkFailure.NoAnswer) {
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
        return DeviceSyncResult.Succeeded(
            progress.written, progress.nights, progress.stepMinutes, progress.missingBodyWeight,
            progress.weightReadWithheld, progress.unwritten.toSet(), progress.withheldPermissions.toSet(),
        )
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

    /**
     * The step minutes pulled, for the floors. Null when no bonded watch
     * matches. The minutes written are counted on [progress]. The pull also
     * reaches back to the minute before the floors cursor, because the floors
     * filter needs it; those older minutes are not written again.
     */
    private suspend fun pullSteps(device: BleSensorDevice, progress: Progress): List<WearOsStepMinute>? {
        val granted = healthConnect.grantedPermissions()
        val withheld = STEPS_SYNC_PERMISSIONS - granted
        if (STEPS_WRITE_PERMISSION in withheld) {
            // Nothing pulled, so whether a minute would need the energy grants is unknown; the steps come first.
            progress.withheldPermissions += STEPS_WRITE_PERMISSION
            progress.unwritten += UnwrittenSeries.STEPS
            WearOsLog.log("steps and floors: not pulled, Health Connect permission missing")
            return emptyList()
        }
        // In the background without that grant Health Connect shows this app its own records only: another
        // app's weight and exercise sessions are invisible, so the energy would be filtered against nothing.
        val seesOtherApps = healthConnect.readsOtherAppsDataNow()
        // Without the exercise read grant every session read fails, so it gates the energy like the write grant.
        val writeEnergy = ACTIVE_CALORIES_WRITE_PERMISSION !in withheld && EXERCISE_READ_PERMISSION !in withheld && seesOtherApps
        if (ACTIVE_CALORIES_WRITE_PERMISSION in withheld || EXERCISE_READ_PERMISSION in withheld) {
            WearOsLog.log("walking energy: not written, Health Connect permission missing")
        } else if (!seesOtherApps) {
            WearOsLog.log("walking energy: not written, other apps' sessions are not readable in the background")
        }
        val cursor = cursors.stepsCursor(device.id)
        // The watch sends what is after `since`, so this takes the cursor's minute again.
        var since = if (cursor == Instant.EPOCH) cursor else cursor.minusMillis(1)
        val ascentCursor = cursors.ascentCursor(device.id)
        if (ascentCursor != Instant.EPOCH) since = minOf(since, ascentCursor.minusSeconds(60).minusMillis(1))
        val allMinutes = ArrayList<WearOsStepMinute>()
        var pages = 0
        var pulledSteps = 0
        var energyMinutes = 0
        var energyKcal = 0.0
        // The energy grants are reported only when a pulled minute would have used them.
        var energyNeeded = false
        // Read once, and only when a minute would need it. A failed read is not "no weight": the card would ask
        // for one the user has.
        var weightLookup: Result<Double?>? = null
        suspend fun knownWeight(): Result<Double?> = weightLookup ?: bodyWeightKg().also { weightLookup = it }

        // A page's batches share one read of the exercise sessions over the whole page, made when a batch first needs it.
        var page: List<WearOsStepMinute> = emptyList()
        var pageSessionsRead = false
        var pageSessions: List<ClosedRange<Instant>>? = null
        suspend fun sessionsOfPage(): List<ClosedRange<Instant>>? {
            if (!pageSessionsRead) {
                // Asked again per page: a sync begun in the foreground goes on after the user leaves the app.
                pageSessions = if (healthConnect.readsOtherAppsDataNow()) exerciseSessions(page) else null
                pageSessionsRead = true
            }
            return pageSessions
        }

        suspend fun writeBatch(batch: List<WearOsStepMinute>) {
            if (batch.isEmpty()) return
            // A minute is estimated when its steps were written; see writeSteps for the minutes past the cursor.
            val due = writeSteps(batch, cursor, progress)
            if (!WearOsStepsImport.hasWalkingEnergy(due)) return
            if (!writeEnergy) {
                // The sessions cannot be read here, so whether a recorded workout covers these minutes is unknown.
                energyNeeded = true
                return
            }
            // Sessions before the weight: minutes inside a recorded workout get no estimate, so they need no weight.
            val sessions = sessionsOfPage() ?: return
            val minutes = WearOsStepsImport.outsideExerciseSessions(due, sessions)
            if (!WearOsStepsImport.hasWalkingEnergy(minutes)) return
            energyNeeded = true
            val weightKg = knownWeight().getOrElse { return }
            if (weightKg == null) {
                if (!progress.missingBodyWeight) WearOsLog.log("steps: no body weight, so no walking energy")
                progress.missingBodyWeight = true
                // A weight in Health Connect is out of reach without this grant, so the card asks for it.
                if (WEIGHT_READ_PERMISSION !in granted) {
                    progress.weightReadWithheld = true
                    progress.withheldPermissions += WEIGHT_READ_PERMISSION
                }
                return
            }
            val energy = WearOsStepsImport.activeCaloriesRecords(minutes, weightKg, zone())
            // Counted as written, not as tried: a minute refused as present adds nothing.
            val written = energy.zip(insertEstimates(energy, "walking energy")).filter { (_, outcome) -> outcome == RecordWrite.WRITTEN }
            energyMinutes += written.size
            energyKcal += written.sumOf { (record, _) -> record.energy.inKilocalories }
        }

        while (true) {
            val pulled = retryOnce { nodePort.pullSteps(device.address, device.bluetoothName, since) } ?: return null
            progress.page()
            // Each minute once, so every builder below works on the same minutes.
            page = WearOsStepsImport.latestPerMinute(pulled.minutes)
            allMinutes += page
            pageSessionsRead = false
            val newest = page.maxOfOrNull { it.start }
            if (newest != null) {
                // Minutes before the cursor are unchanged. The cursor minute is written apart, so when it is
                // unchanged too the batch does not fall back to one by one.
                val unwritten = page.filter { !it.start.isBefore(cursor) }
                val (again, fresh) = unwritten.partition { !it.start.isAfter(cursor) }
                writeBatch(again)
                writeBatch(fresh)
                pulledSteps += unwritten.sumOf { it.steps }
                if (newest.isAfter(cursors.stepsCursor(device.id))) cursors.setStepsCursor(device.id, newest)
                since = newest
            }
            if (!pulled.hasMore || newest == null) break
            if (++pages >= MAX_PAGES) break
        }
        if (energyNeeded && withheld.isNotEmpty()) {
            progress.withheldPermissions += withheld
            progress.unwritten += UnwrittenSeries.WALKING_ENERGY
        }
        val weight = weightLookup?.getOrNull()
        // The watch has no screen for these yet; testers read this line.
        WearOsLog.log(
            "steps: ${progress.stepMinutes} minute(s) written, $pulledSteps steps pulled (the re-pulled minute counts again); " +
                if (energyMinutes == 0) {
                    "no walking energy written"
                } else {
                    "walking energy ${"%.1f".format(Locale.ROOT, energyKcal)} kcal written over $energyMinutes minute(s) at $weight kg"
                },
        )
        return allMinutes
    }

    /**
     * False when no bonded watch matches. Ascent is pulled the way steps are,
     * taking the cursor's minute again, and written as floors only for
     * minutes with [steps] around them; see [WearOsFloorsImport].
     */
    private suspend fun pullAscent(device: BleSensorDevice, progress: Progress, steps: List<WearOsStepMinute>): Boolean {
        val cursor = cursors.ascentCursor(device.id)
        var since = if (cursor == Instant.EPOCH) cursor else cursor.minusMillis(1)
        var pages = 0
        var climbed = 0.0
        var credited = 0.0
        while (true) {
            val page = retryOnce { nodePort.pullAscent(device.address, device.bluetoothName, since) } ?: return false
            progress.page()
            val newest = page.minutes.maxOfOrNull { it.start }
            if (newest != null) {
                val records = WearOsFloorsImport.records(page.minutes, steps)
                // The cursor minute apart, as for steps.
                for (part in records.partition { (it as FloorsClimbedRecord).startTime.isAfter(cursor) }.toList()) {
                    insertEstimates(part, "floors")
                }
                climbed += page.minutes.sumOf { it.metres }
                credited += records.sumOf { (it as FloorsClimbedRecord).floors } * AscentCredit.METRES_PER_FLOOR
                if (newest.isAfter(cursors.ascentCursor(device.id))) cursors.setAscentCursor(device.id, newest)
                since = newest
            }
            if (!page.hasMore || newest == null) break
            if (++pages >= MAX_PAGES) break
        }
        // The watch has no screen for these yet; testers read this line.
        WearOsLog.log(
            "ascent: ${"%.1f".format(Locale.ROOT, credited)} of ${"%.1f".format(Locale.ROOT, climbed)} m climbed written as floors; " +
                "the rest had too few steps (elevators, weather)",
        )
        return true
    }

    /**
     * The steps of [minutes], then the cadence of the minutes due an estimate:
     * those Health Connect did not already hold at that count, and every
     * minute past [stepsCursor], which a sync that failed after its steps may
     * have left without one. Returns the minutes due, for the walking energy.
     */
    private suspend fun writeSteps(minutes: List<WearOsStepMinute>, stepsCursor: Instant, progress: Progress): List<WearOsStepMinute> {
        val records = WearOsStepsImport.records(minutes, zone())
        val changed = records.zip(importRepository.insertWithOutcomes(records))
            .filter { (_, outcome) -> outcome == RecordWrite.WRITTEN }
            .mapTo(HashSet()) { (record, _) -> record.startTime }
        progress.stepMinutes += changed.size
        val due = minutes.filter { it.start in changed || it.start.isAfter(stepsCursor) }
        // Health Connect files cadence under the steps grant.
        insertEstimates(WearOsStepsImport.cadenceRecords(due, zone()), "cadence")
        return due
    }

    /**
     * Writes records derived from the steps. They are optional: a failure is
     * logged and skipped, so it never fails the sync and keeps sleep from
     * being pulled. Returns what became of each record, or nothing when the
     * write failed.
     */
    private suspend fun insertEstimates(records: List<Record>, what: String): List<RecordWrite> = try {
        importRepository.insertWithOutcomes(records)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        WearOsLog.log("$what: not written: $error")
        emptyList()
    }

    /** The weight for the walking energy; a failure when it could not be read. It is optional, so that never fails the sync. */
    private suspend fun bodyWeightKg(): Result<Double?> = try {
        Result.success(weightKg())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        WearOsLog.log("steps: could not read body weight for walking energy: $error")
        Result.failure(error)
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
        val restingHeartRate = RestingHeartRateAccess(
            withheld = RESTING_HEART_RATE_PERMISSIONS - healthConnect.grantedPermissions(),
            seesOtherApps = healthConnect.readsOtherAppsDataNow(),
        )
        for ((night, offset) in WearOsSleepImport.touchedNights(minutes, cursor)) {
            val (from, to) = sleepNightWindow(night, offset)
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
            // Before the session check: a vendor that owns the session may not own the resting heart rate.
            writeRestingHeartRate(minutes, estimated, night, offset, restingHeartRate, progress)
            if (hasForeignRecord(SleepSessionRecord::class, from, to, WearOsSleepImport::isOwnRecordId)) {
                WearOsLog.log("$night: Health Connect already holds a session from elsewhere, skipping")
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
     * Writes the night's resting heart rate unless its calendar day already holds one from
     * elsewhere; then it removes ours. A read that fails counts as "held": a second value next to
     * the vendor's would be averaged with it. Optional, so no failure here fails the sync.
     */
    private suspend fun writeRestingHeartRate(
        minutes: List<WearSleepMinute>,
        estimated: WearSleepEstimator.Night,
        night: LocalDate,
        offset: ZoneOffset,
        access: RestingHeartRateAccess,
        progress: Progress,
    ) {
        val record = WearOsRestingHeartRateImport.record(minutes, estimated, night, offset, version = clock().toEpochMilli())
        if (record == null) {
            WearOsLog.log("$night: no resting heart rate, the night runs on or has too few readings")
            return
        }
        if (access.withheld.isNotEmpty()) {
            progress.withheldPermissions += access.withheld
            progress.unwritten += UnwrittenSeries.RESTING_HEART_RATE
            WearOsLog.log("$night: resting heart rate not written, Health Connect permission missing")
            return
        }
        if (!access.seesOtherApps) {
            WearOsLog.log("$night: resting heart rate not written, other apps' values are not readable in the background")
            return
        }
        try {
            val (from, to) = WearOsRestingHeartRateImport.dayWindow(estimated.session.end, zone())
            val held = holdings(RestingHeartRateRecord::class, from, to, WearOsRestingHeartRateImport::isOwnRecordId)
            if (held.foreign) {
                WearOsLog.log("$night: Health Connect already holds a resting heart rate from elsewhere, skipping")
                if (held.own) {
                    healthConnect.deleteImportedRecordsByClientIds(RestingHeartRateRecord::class, listOf(record.metadata.clientRecordId!!))
                }
                return
            }
            WearOsLog.log("$night: resting heart rate ${record.beatsPerMinute} bpm")
            writeReplacing(record)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WearOsLog.log("$night: resting heart rate not written: $error")
        }
    }

    /** Whether the window has a record of [type] from another app. A read failure counts as no. */
    private suspend fun hasForeignRecord(
        type: KClass<out Record>,
        from: Instant,
        to: Instant,
        isOwn: (String?) -> Boolean,
    ): Boolean = try {
        holdings(type, from, to, isOwn).foreign
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        WearOsLog.log("Could not read existing ${type.simpleName}: $error")
        false
    }

    /** Who wrote the records of [type] in the window. A failed read throws. */
    private suspend fun holdings(type: KClass<out Record>, from: Instant, to: Instant, isOwn: (String?) -> Boolean): Holdings {
        var foreign = false
        var own = false
        healthConnect.forEachSyncRecordPage(type, from, to) { page ->
            for (record in page) {
                if (isOwn(record.metadata.clientRecordId)) own = true else foreign = true
            }
        }
        return Holdings(foreign, own)
    }

    /** Time ranges of exercise sessions overlapping the step minutes range; null when they could not be read. */
    private suspend fun exerciseSessions(minutes: List<WearOsStepMinute>): List<ClosedRange<Instant>>? {
        if (minutes.isEmpty()) return emptyList()
        // Health Connect filters by a record's bounds: a workout begun before the first minute (or ended after the
        // last) is found only through a window widened both ways. The overlap test in WearOsStepsImport keeps
        // what actually covers a minute.
        val readStart = minutes.minOf { it.start }.minus(SESSION_LOOKAROUND)
        val readEnd = minutes.maxOf { it.start }.plusSeconds(60).plus(SESSION_LOOKAROUND)
        // Strict: the reader turns a failed read into an empty list, which would pass for "no workout".
        return try {
            withStrictHealthConnectReads { healthConnect.readExerciseSessions(readStart, readEnd) }
                .map { it.startTime..it.endTime }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WearOsLog.log("Could not read exercise sessions for walking energy filtering: $error")
            null
        }
    }

    /** A higher version replaces the earlier estimate. If Health Connect still objects, delete and retry once. */
    private suspend fun writeReplacing(record: Record) {
        try {
            importRepository.insertImportedRecords(listOf(record))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (!error.isDuplicateClientRecordFailure()) throw error
            val id = requireNotNull(record.metadata.clientRecordId)
            healthConnect.deleteImportedRecordsByClientIds(record::class, listOf(id))
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

    private class Holdings(val foreign: Boolean, val own: Boolean)

    /** What the sync may do with resting heart rates: [withheld] are the grants missing. */
    private class RestingHeartRateAccess(val withheld: Set<String>, val seesOtherApps: Boolean)

    /** Counts the pages pulled, for the progress ticks, and what was written. */
    private class Progress(private val onProgress: ((DeviceSyncProgress) -> Unit)?) {
        var pages = 0
            private set
        /** Heart rate samples. */
        var written = 0
        var stepMinutes = 0
        var nights = 0

        val hasWritten: Boolean
            get() = written > 0 || stepMinutes > 0

        /** Steps came in but no body weight was known, so the walking energy was skipped. */
        var missingBodyWeight = false

        /** With [missingBodyWeight]: Health Connect's weight was not readable for want of the grant. */
        var weightReadWithheld = false

        /** Health Connect grants for the steps and their estimates the user has not given. */
        val withheldPermissions = mutableSetOf<String>()

        /** The series those grants kept out of Health Connect. */
        val unwritten = mutableSetOf<UnwrittenSeries>()

        fun page() {
            pages++
            onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.DOWNLOADING, filesTotal = pages, filesDone = pages - 1))
        }
    }

    private companion object {
        /** A safety net against a watch that always says "more": two million samples. */
        private const val MAX_PAGES = 1000

        private const val RETRY_DELAY_MILLIS = 1_500L

        /** Longer than any workout, as `HealthConnectSeries`' lookaround is longer than any series record. */
        private val SESSION_LOOKAROUND: java.time.Duration = java.time.Duration.ofHours(30)

        private val STEPS_WRITE_PERMISSION = HealthPermission.getWritePermission(StepsRecord::class)
        private val ACTIVE_CALORIES_WRITE_PERMISSION = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
        private val EXERCISE_READ_PERMISSION = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val WEIGHT_READ_PERMISSION = HealthPermission.getReadPermission(WeightRecord::class)

        /** Reading tells whether the vendor holds a value for the day; writing is the estimate itself. */
        private val RESTING_HEART_RATE_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getWritePermission(RestingHeartRateRecord::class),
        )

        private val WALKING_ENERGY_PERMISSIONS = setOf(ACTIVE_CALORIES_WRITE_PERMISSION, EXERCISE_READ_PERMISSION)

        /** The grants the steps and their estimates need: the walking energy reads the sessions it leaves out. */
        private val STEPS_SYNC_PERMISSIONS = WALKING_ENERGY_PERMISSIONS + STEPS_WRITE_PERMISSION
    }
}
