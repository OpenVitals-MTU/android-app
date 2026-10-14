package tech.mmarca.openvitals.devices.wearos

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.wearlink.WearLinkProtocol
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingStatus
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.domain.model.WearSleepMinute
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import io.mockk.slot
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord

/**
 * What a Wear OS sync does around the link. The watch is a fake here: the
 * paging, the cursor and what each failure tells the user are the part that
 * matters.
 */
class WearOsWatchSyncServiceTest {

    private val t0 = Instant.parse("2026-10-07T10:00:00Z")

    private val deviceRepository = mockk<BleDeviceRepository>(relaxed = true)
    private val importRepository = mockk<AppleHealthImportRepository>(relaxed = true)
    private val cursors = WearOsSyncCursorStore(FakeSharedPreferences())
    private val healthConnect = mockk<HealthConnectManager>(relaxed = true)
    private val linkStore = WearOsLinkStore(FakeSharedPreferences())
    private val zone: ZoneOffset = ZoneOffset.ofHours(2)
    private val recording = MutableStateFlow(ActivityRecordingState())
    private val recordingController = mockk<ActivityRecordingController> {
        every { state } returns recording
    }

    /** Pages handed out in order; each pull consumes one. */
    private val pages = ArrayDeque<WearOsHeartRatePage?>()
    private val pullsSince = mutableListOf<Instant>()
    private var pullFailure: Throwable? = null

    /** Sleep pages the same way; with none queued the watch answers an empty page. */
    private val sleepPages = ArrayDeque<WearOsSleepMinutePage?>()
    private val sleepPullsSince = mutableListOf<Instant>()
    private var sleepPullFailure: Throwable? = null

    /** The minutes the watch holds; a pull answers those after `since`, [STEP_PAGE] at a time. */
    private val watchSteps = mutableListOf<WearOsStepMinute>()
    private val stepPullsSince = mutableListOf<Instant>()
    private var stepsNotPaired = false

    /** Floor pages the same way. */
    private val ascentPages = ArrayDeque<WearOsAscentPage>()
    private val ascentPullsSince = mutableListOf<Instant>()
    private val nodePort = object : WearOsNodePort {
        override suspend fun checkStatus(targetAddress: String?, targetName: String?) = WearOsCompanionStatus()
        override suspend fun pullHeartRate(targetAddress: String?, targetName: String?, since: Instant): WearOsHeartRatePage? {
            pullsSince += since
            // Pages first; once they run out, the configured failure.
            if (pages.isEmpty()) pullFailure?.let { throw it }
            return pages.removeFirst()
        }

        override suspend fun pullSleepMinutes(targetAddress: String?, targetName: String?, since: Instant): WearOsSleepMinutePage? {
            sleepPullsSince += since
            if (sleepPages.isEmpty()) {
                sleepPullFailure?.let { throw it }
                return WearOsSleepMinutePage(emptyList(), hasMore = false)
            }
            return sleepPages.removeFirst()
        }

        override suspend fun pullSteps(targetAddress: String?, targetName: String?, since: Instant): WearOsStepPage? {
            stepPullsSince += since
            if (stepsNotPaired) return null
            val after = watchSteps.filter { it.start.isAfter(since) }.sortedBy { it.start }
            return WearOsStepPage(after.take(STEP_PAGE), hasMore = after.size > STEP_PAGE)
        }

        override suspend fun pullAscent(targetAddress: String?, targetName: String?, since: Instant): WearOsAscentPage? {
            ascentPullsSince += since
            return ascentPages.removeFirstOrNull() ?: WearOsAscentPage(emptyList(), hasMore = false)
        }

        override suspend fun liveHeartRate(
            targetAddress: String?,
            targetName: String?,
            onSample: (WearOsHeartRateSample) -> Unit,
        ): Boolean = false
    }

    /**
     * Health Connect's rule for a `clientRecordId`: a batch is refused whole
     * when one record is already there at the same or a higher version.
     */
    private val healthConnectVersions = mutableMapOf<String, Long>()
    private val insertCalls = mutableListOf<List<androidx.health.connect.client.records.Record>>()

    /** A record type whose write permission the user withheld. */
    private var refused: (androidx.health.connect.client.records.Record) -> Boolean = { false }

    /** The body weight the walking energy reads; null as for a profile without one. */
    private var weight: Double? = 70.0

    init {
        coEvery { healthConnect.grantedPermissions() } returns setOf(HealthPermission.getWritePermission(StepsRecord::class))
        coEvery { importRepository.insertImportedRecords(any()) } answers {
            val records = firstArg<List<androidx.health.connect.client.records.Record>>()
            if (records.any(refused)) throw SecurityException("Caller doesn't have the write permission")
            insertCalls += records
            val present = records.any { record ->
                val id = record.metadata.clientRecordId ?: return@any false
                (healthConnectVersions[id] ?: Long.MIN_VALUE) >= record.metadata.clientRecordVersion
            }
            if (present) throw IllegalStateException("clientRecordId already exists")
            records.forEach { record -> record.metadata.clientRecordId?.let { healthConnectVersions[it] = record.metadata.clientRecordVersion } }
        }
    }

    private fun stepCounts(): Map<Instant, Long> =
        insertCalls.flatten().filterIsInstance<StepsRecord>().associate { it.startTime to it.count }

    private inline fun <reified T> insertedOf(): List<T> = insertCalls.flatten().filterIsInstance<T>()

    private fun service() = WearOsWatchSyncService(
        nodePort = nodePort,
        cursors = cursors,
        importRepository = importRepository,
        bleDeviceRepository = deviceRepository,
        recordingController = recordingController,
        healthConnect = healthConnect,
        linkStore = linkStore,
        weightKg = { weight },
        clock = { t0 },
        zone = { ZoneId.of("+02:00") },
    )

    private fun raw(time: Instant, movement: Float, heartRate: Float) =
        WearSleepMinutes.minute(time, movement, heartRate, zone)

    /** A night of 2026-10-08: still from 23:00 to 06:00 local, restless around it. */
    private fun nightMinutes(): List<WearSleepMinute> {
        val start = Instant.parse("2026-10-07T19:00:00Z")
        val onset = Instant.parse("2026-10-07T21:00:00Z")
        val wake = Instant.parse("2026-10-08T04:00:00Z")
        val end = Instant.parse("2026-10-08T06:00:00Z")
        return generateSequence(start) { it.plusSeconds(60) }.takeWhile { it.isBefore(end) }.mapIndexed { index, time ->
            if (time.isBefore(onset) || !time.isBefore(wake)) raw(time, 8f + (index * 5) % 8, 70f) else raw(time, 0f, 50f + (index % 40) / 10f)
        }.toList()
    }

    private fun samples(vararg offsetsSeconds: Long) =
        offsetsSeconds.map { WearOsHeartRateSample(t0.plusSeconds(it), 70) }

    @Test
    fun `a sync while a recording runs is refused before the link is touched`() = runTest {
        recording.value = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING)

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(0, pullsSince.size)
    }

    @Test
    fun `pages are written one by one and the cursor follows the newest sample`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10, 20), hasMore = true)
        pages += WearOsHeartRatePage(samples(30, 40), hasMore = false)
        val progress = mutableListOf<DeviceSyncProgress>()

        val result = service().sync(WATCH, Duration.ZERO) { progress += it }

        assertEquals(DeviceSyncResult.Succeeded(5), result)
        // The first pull starts at the epoch; the second at the first page's newest sample.
        assertEquals(listOf(Instant.EPOCH, t0.plusSeconds(20)), pullsSince)
        assertEquals(t0.plusSeconds(40), cursors.heartRateCursor(WATCH.id))
        coVerify(exactly = 2) { importRepository.insertImportedRecords(any()) }
        coVerify(exactly = 1) { deviceRepository.markSynced(WATCH.id, any()) }
        assertEquals(DeviceSyncPhase.HANDSHAKE, progress.first().phase)
        assertEquals(DeviceSyncPhase.COMPLETE, progress.last().phase)
    }

    @Test
    fun `step minutes are pulled page by page, each from the newest minute written`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += List(5) { WearOsStepMinute(t0.plusSeconds(60L * it), 10 + it) }

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0, stepMinuteCount = 5), result)
        assertEquals(listOf(Instant.EPOCH, t0.plusSeconds(60), t0.plusSeconds(180)), stepPullsSince)
        assertEquals(t0.plusSeconds(240), cursors.stepsCursor(WATCH.id))
        assertEquals((0..4).associate { t0.plusSeconds(60L * it) to 10L + it }, stepCounts())
        assertEquals((0..4).map { 10.0 + it }, insertedOf<androidx.health.connect.client.records.StepsCadenceRecord>().map { it.samples.single().rate })
    }

    @Test
    fun `the next sync takes the cursor minute again, and a minute that grew replaces its record`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += listOf(WearOsStepMinute(t0, 40), WearOsStepMinute(t0.plusSeconds(60), 12))
        service().sync(WATCH, Duration.ZERO, null)

        // Late steps for the cursor minute, and a new minute.
        watchSteps[1] = WearOsStepMinute(t0.plusSeconds(60), 30)
        watchSteps += WearOsStepMinute(t0.plusSeconds(120), 7)
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        insertCalls.clear()
        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(t0.plusSeconds(60).minusMillis(1), stepPullsSince.last())
        assertEquals(DeviceSyncResult.Succeeded(0, stepMinuteCount = 2), result)
        assertEquals(mapOf(t0.plusSeconds(60) to 30L, t0.plusSeconds(120) to 7L), stepCounts())
    }

    @Test
    fun `an unchanged cursor minute does not make the new minutes fall back to one by one`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += WearOsStepMinute(t0, 40)
        service().sync(WATCH, Duration.ZERO, null)

        watchSteps += listOf(WearOsStepMinute(t0.plusSeconds(60), 5), WearOsStepMinute(t0.plusSeconds(120), 6))
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        insertCalls.clear()
        val result = service().sync(WATCH, Duration.ZERO, null)

        // The cursor minute is written alone and refused as present; no record is tried twice.
        assertEquals(listOf(t0), insertCalls.first().map { (it as StepsRecord).startTime })
        assertEquals(3, insertedOf<StepsRecord>().size)
        assertEquals(DeviceSyncResult.Succeeded(0, stepMinuteCount = 2), result)
    }

    @Test
    fun `walking energy comes with the steps, from a cadence above rest`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += listOf(WearOsStepMinute(t0, 40), WearOsStepMinute(t0.plusSeconds(60), 12))

        service().sync(WATCH, Duration.ZERO, null)

        // 40 steps a minute costs a little more than rest; 12 does not.
        assertEquals(1, insertedOf<androidx.health.connect.client.records.ActiveCaloriesBurnedRecord>().size)
    }

    @Test
    fun `no body weight writes no walking energy, and a withheld permission for an estimate does not fail the sync`() = runTest {
        weight = null
        refused = { it is androidx.health.connect.client.records.StepsCadenceRecord }
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += WearOsStepMinute(t0, 110)

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0, stepMinuteCount = 1, missingBodyWeight = true), result)
        assertEquals(1, insertedOf<StepsRecord>().size)
        assertTrue(insertedOf<androidx.health.connect.client.records.ActiveCaloriesBurnedRecord>().isEmpty())
    }

    @Test
    fun `without the grant to write steps, the steps stay on the watch and sleep still syncs`() = runTest {
        coEvery { healthConnect.grantedPermissions() } returns emptySet()
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += WearOsStepMinute(t0, 40)
        sleepPages += WearOsSleepMinutePage(nightMinutes(), hasMore = false)

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0, nightCount = 1), result)
        assertEquals(emptyList<Instant>(), stepPullsSince)
        assertEquals(Instant.EPOCH, cursors.stepsCursor(WATCH.id))
    }

    @Test
    fun `a watch that is no longer paired during the step pull fails the sync`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        stepsNotPaired = true

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_bt_not_paired), (result as DeviceSyncResult.Failed).error)
        assertEquals(Instant.EPOCH, cursors.stepsCursor(WATCH.id))
    }

    @Test
    fun `a watch that stops answering after steps were written says so`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += WearOsStepMinute(t0, 40)
        sleepPullFailure = WearOsLinkFailure.NoAnswer()

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_fail_partial), (result as DeviceSyncResult.Failed).error)
    }

    @Test
    fun `floors climbed with steps are written, an elevator ride is not, and the next pull takes the cursor minute again`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += WearOsStepMinute(t0, 40)
        ascentPages += WearOsAscentPage(
            listOf(WearOsAscentMinute(t0, 6.0), WearOsAscentMinute(t0.plusSeconds(600), 15.0)),
            hasMore = false,
        )

        service().sync(WATCH, Duration.ZERO, null)

        assertEquals(listOf(2.0), insertedOf<androidx.health.connect.client.records.FloorsClimbedRecord>().map { it.floors })
        assertEquals(
            listOf(6.0),
            insertedOf<androidx.health.connect.client.records.ElevationGainedRecord>().map { it.elevation.inMeters },
        )
        assertEquals(t0.plusSeconds(600), cursors.ascentCursor(WATCH.id))

        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        service().sync(WATCH, Duration.ZERO, null)

        assertEquals(t0.plusSeconds(600).minusMillis(1), ascentPullsSince.last())
    }

    @Test
    fun `the step pull reaches back to the minute before the floors cursor, without writing it again`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        watchSteps += List(3) { WearOsStepMinute(t0.plusSeconds(60L * it), 40) }
        ascentPages += WearOsAscentPage(listOf(WearOsAscentMinute(t0, 3.0)), hasMore = false)
        service().sync(WATCH, Duration.ZERO, null)
        // Steps reached t0 + 2 min, floors only t0.

        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        ascentPages += WearOsAscentPage(listOf(WearOsAscentMinute(t0.plusSeconds(60), 6.0)), hasMore = false)
        insertCalls.clear()
        stepPullsSince.clear()
        service().sync(WATCH, Duration.ZERO, null)

        assertEquals(t0.minusSeconds(60).minusMillis(1), stepPullsSince.first())
        // Only the cursor minute is tried again; t0 and t0 + 1 min are not rewritten.
        assertEquals(listOf(t0.plusSeconds(120)), insertedOf<StepsRecord>().map { it.startTime })
        assertEquals(listOf(2.0), insertedOf<androidx.health.connect.client.records.FloorsClimbedRecord>().map { it.floors })
    }

    @Test
    fun `without the grant to write steps, floors stay on the watch too`() = runTest {
        coEvery { healthConnect.grantedPermissions() } returns emptySet()
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        ascentPages += WearOsAscentPage(listOf(WearOsAscentMinute(t0, 3.0)), hasMore = false)

        service().sync(WATCH, Duration.ZERO, null)

        assertEquals(emptyList<Instant>(), ascentPullsSince)
        assertEquals(Instant.EPOCH, cursors.ascentCursor(WATCH.id))
    }

    @Test
    fun `a night of minutes becomes one sleep session and moves the sleep cursor`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        val minutes = nightMinutes()
        sleepPages += WearOsSleepMinutePage(minutes, hasMore = false)
        val written = slot<List<androidx.health.connect.client.records.Record>>()
        coEvery { importRepository.insertImportedRecords(capture(written)) } returns Unit

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0, nightCount = 1), result)
        assertEquals(listOf(Instant.EPOCH), sleepPullsSince)
        val session = written.captured.single() as SleepSessionRecord
        assertEquals("wearos_sleep_est_2026-10-08", session.metadata.clientRecordId)
        assertEquals(minutes.last().time, cursors.sleepCursor(WATCH.id))
    }

    @Test
    fun `the next sleep pull restarts at the night's window so the night is re-estimated whole`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        cursors.setSleepCursor(WATCH.id, Instant.parse("2026-10-08T01:00:00Z"))

        service().sync(WATCH, Duration.ZERO, null)

        // 18:00 local the evening before, in +02:00, one millisecond earlier for the exclusive "since".
        assertEquals(listOf(Instant.parse("2026-10-07T16:00:00Z").minusMillis(1)), sleepPullsSince)
    }

    @Test
    fun `a night that already holds a session from elsewhere is left alone`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        sleepPages += WearOsSleepMinutePage(nightMinutes(), hasMore = false)
        val foreign = mockk<SleepSessionRecord> {
            every { metadata } returns Metadata.manualEntry(clientRecordId = "samsung_sleep_1")
        }
        coEvery { healthConnect.forEachSyncRecordPage(SleepSessionRecord::class, any(), any(), any()) } coAnswers {
            arg<suspend (List<androidx.health.connect.client.records.Record>) -> Unit>(3).invoke(listOf(foreign))
        }

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0), result)
        assertTrue(insertedOf<SleepSessionRecord>().isEmpty())
        assertEquals(nightMinutes().last().time, cursors.sleepCursor(WATCH.id))
    }

    @Test
    fun `an estimated night also gives its nocturnal resting heart rate`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        sleepPages += WearOsSleepMinutePage(nightMinutes(), hasMore = false)
        service().sync(WATCH, Duration.ZERO, null)

        val resting = insertedOf<RestingHeartRateRecord>().single()
        assertEquals("wearos_rhr_est_2026-10-08", resting.metadata.clientRecordId)
        // The asleep minutes run 50.0 to 53.9 bpm in a 40-minute cycle; the lowest half hour averages about 51.
        assertTrue(resting.beatsPerMinute in 50L..52L)
    }

    @Test
    fun `a resting heart rate from elsewhere wins over the estimate`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        sleepPages += WearOsSleepMinutePage(nightMinutes(), hasMore = false)
        val foreign = mockk<RestingHeartRateRecord> {
            every { metadata } returns Metadata.manualEntry(clientRecordId = "samsung_rhr_1")
        }
        coEvery { healthConnect.forEachSyncRecordPage(RestingHeartRateRecord::class, any(), any(), any()) } coAnswers {
            arg<suspend (List<androidx.health.connect.client.records.Record>) -> Unit>(3).invoke(listOf(foreign))
        }
        service().sync(WATCH, Duration.ZERO, null)

        assertTrue(insertedOf<RestingHeartRateRecord>().isEmpty())
        assertEquals(1, insertedOf<SleepSessionRecord>().size)
    }

    @Test
    fun `a watch that stops answering during the sleep pull keeps the heart rate and says so`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10), hasMore = false)
        sleepPullFailure = WearOsLinkFailure.NoAnswer()

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_fail_partial), (result as DeviceSyncResult.Failed).error)
        assertEquals(t0.plusSeconds(10), cursors.heartRateCursor(WATCH.id))
        assertEquals(Instant.EPOCH, cursors.sleepCursor(WATCH.id))
    }

    @Test
    fun `a pending confirmation or a refusal is a typed failure that the status row shows`() = runTest {
        pullFailure = WearOsLinkFailure.PendingConfirmation()

        val pending = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_status_pending), (pending as DeviceSyncResult.Failed).error)
        assertEquals(WearOsAppStatus.PENDING_CONFIRMATION, linkStore.snapshots.value.getValue(WATCH.id).status)
        // A refusal is not retried: one pull, one failure.
        assertEquals(1, pullsSince.size)

        pullFailure = WearOsLinkFailure.Unauthorized(WearLinkProtocol.UnauthorizedReason.MISMATCH)
        val refused = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_status_unauthorized_mismatch), (refused as DeviceSyncResult.Failed).error)
    }

    @Test
    fun `a success records the running status`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)

        service().sync(WATCH, Duration.ZERO, null)

        assertEquals(WearOsAppStatus.APP_RUNNING, linkStore.snapshots.value.getValue(WATCH.id).status)
    }

    @Test
    fun `nothing new is still a success, and the watch is stamped`() = runTest {
        cursors.setHeartRateCursor(WATCH.id, t0)
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0), result)
        assertEquals(listOf(t0), pullsSince)
        assertEquals(t0, cursors.heartRateCursor(WATCH.id))
        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
        coVerify(exactly = 1) { deviceRepository.markSynced(WATCH.id, any()) }
    }

    @Test
    fun `no bonded watch fails the sync and stamps nothing`() = runTest {
        pages += null

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_bt_not_paired), (result as DeviceSyncResult.Failed).error)
        assertEquals(WearOsAppStatus.NOT_PAIRED, linkStore.snapshots.value.getValue(WATCH.id).status)
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a silent watch fails the sync without moving the cursor`() = runTest {
        pullFailure = WearOsLinkFailure.NoAnswer()

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_app_no_answer), (result as DeviceSyncResult.Failed).error)
        assertEquals(WearOsAppStatus.NO_ANSWER, linkStore.snapshots.value.getValue(WATCH.id).status)
        assertEquals(Instant.EPOCH, cursors.heartRateCursor(WATCH.id))
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a link lost mid-way keeps what was written and says so`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10), hasMore = true)
        pullFailure = WearOsLinkFailure.NoAnswer()

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_fail_partial), (result as DeviceSyncResult.Failed).error)
        assertEquals(t0.plusSeconds(10), cursors.heartRateCursor(WATCH.id))
        coVerify(exactly = 1) { importRepository.insertImportedRecords(any()) }
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a refused Health Connect write names the permission and leaves the cursor`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10), hasMore = false)
        coEvery { importRepository.insertImportedRecords(any()) } throws SecurityException("WRITE_HEART_RATE")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(ScreenError.Text(R.string.settings_watch_wearos_fail_health_connect), (result as DeviceSyncResult.Failed).error)
        assertEquals(Instant.EPOCH, cursors.heartRateCursor(WATCH.id))
    }

    private companion object {
        const val STEP_PAGE = 2

        val WATCH = BleSensorDevice(
            id = "watch-1",
            displayName = "Galaxy Watch8",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Galaxy Watch8 (89FZ)",
            capabilities = emptySet(),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-01-01T00:00:00Z"),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )
    }
}
