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
import tech.mmarca.openvitals.devices.FakeSharedPreferences
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
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import io.mockk.slot

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
    }

    private fun service() = WearOsWatchSyncService(
        nodePort = nodePort,
        cursors = cursors,
        importRepository = importRepository,
        bleDeviceRepository = deviceRepository,
        recordingController = recordingController,
        healthConnect = healthConnect,
        clock = { t0 },
        zone = { ZoneId.of("+02:00") },
    )

    private fun raw(time: Instant, movement: Float, heartRate: Float) =
        WearOsSleepMinute(time, WearLinkProtocol.MinuteKind.RAW, movement, heartRate, zone)

    /** A night of 2026-10-08: still from 23:00 to 06:00 local, restless around it. */
    private fun nightMinutes(): List<WearOsSleepMinute> {
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
    fun `a night of minutes becomes one sleep session and moves the sleep cursor`() = runTest {
        pages += WearOsHeartRatePage(emptyList(), hasMore = false)
        val minutes = nightMinutes()
        sleepPages += WearOsSleepMinutePage(minutes, hasMore = false)
        val written = slot<List<androidx.health.connect.client.records.Record>>()
        coEvery { importRepository.insertImportedRecords(capture(written)) } returns Unit

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(0), result)
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
        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
        assertEquals(nightMinutes().last().time, cursors.sleepCursor(WATCH.id))
    }

    @Test
    fun `a watch that stops answering during the sleep pull keeps the heart rate and says so`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10), hasMore = false)
        sleepPullFailure = WearOsLinkException("The watch did not answer.")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertTrue((result as DeviceSyncResult.Failed).message.startsWith("Imported 2 sample(s)"))
        assertEquals(t0.plusSeconds(10), cursors.heartRateCursor(WATCH.id))
        assertEquals(Instant.EPOCH, cursors.sleepCursor(WATCH.id))
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
        assertTrue((result as DeviceSyncResult.Failed).message.contains("Pair it"))
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a silent watch fails the sync without moving the cursor`() = runTest {
        pullFailure = WearOsLinkException("The watch did not answer.")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertTrue((result as DeviceSyncResult.Failed).message.contains("did not answer"))
        assertEquals(Instant.EPOCH, cursors.heartRateCursor(WATCH.id))
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a link lost mid-way keeps what was written and says so`() = runTest {
        pages += WearOsHeartRatePage(samples(0, 10), hasMore = true)
        pullFailure = WearOsLinkException("The watch did not answer.")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertTrue((result as DeviceSyncResult.Failed).message.startsWith("Imported 2 sample(s)"))
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
        assertTrue((result as DeviceSyncResult.Failed).message.contains("Health Connect"))
        assertEquals(Instant.EPOCH, cursors.heartRateCursor(WATCH.id))
    }

    private companion object {
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
