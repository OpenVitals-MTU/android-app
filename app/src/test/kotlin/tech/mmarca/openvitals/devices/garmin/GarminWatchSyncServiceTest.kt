package tech.mmarca.openvitals.devices.garmin

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.SystemClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.usecase.FitBodyEnergyFromWatchUseCase
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingStatus
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore

/**
 * What a sync does around the radio. The watch is a fake here: the order, the refusals and
 * what each failure tells the user are the part that has gone wrong before.
 */
class GarminWatchSyncServiceTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val deviceRepository = mockk<BleDeviceRepository>(relaxed = true)
    private val stateStore = GarminDeviceStateStore(FakeSharedPreferences())
    private val downloadImport = mockk<GarminDownloadImport>()
    private val recording = MutableStateFlow(ActivityRecordingState())
    private val recordingController = mockk<ActivityRecordingController> {
        every { state } returns recording
    }
    private val fileStore by lazy { GarminFileStore(resolveDirectory = { temp.root }) }

    private var pullResult: GarminPullResult = GarminPullResult(files = emptyList())
    private var pullFailure: Throwable? = null
    private var pulls = 0

    private val radio = object : GarminRadio {
        override suspend fun pull(
            device: BleSensorDevice,
            listenAfter: Duration,
            onProgress: ((DeviceSyncProgress) -> Unit)?,
        ): GarminPullResult {
            pulls++
            pullFailure?.let { throw it }
            return pullResult
        }

        override suspend fun find(address: String, timeout: Duration, cancelled: CompletableDeferred<Unit>?) = true

        override suspend fun upload(
            device: BleSensorDevice,
            type: GarminUploadFileType,
            bytes: ByteArray,
            onStage: (GarminSendStage) -> Unit,
        ): GarminSendFileResult = GarminSendFileResult.Sent
    }

    @Before
    fun setUp() {
        // The widget refresh after a sync asks the platform what is placed.
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns null
        // The radio lease times its holder.
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        coEvery { downloadImport.import(any(), any()) } returns GarminActivityImportResult(written = 1)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppWidgetManager::class)
        unmockkStatic(SystemClock::class)
    }

    private fun service() = GarminWatchSyncService(
        context = context,
        bleDeviceRepository = deviceRepository,
        stateStore = stateStore,
        downloadImport = downloadImport,
        fileStore = fileStore,
        recordingController = recordingController,
        bodyEnergyTimelineStore = mockk<BodyEnergyTimelineStore>(relaxed = true),
        bodyEnergyChainSync = mockk<BodyEnergyChainSyncService>(relaxed = true),
        fitBodyEnergyFromWatch = mockk<FitBodyEnergyFromWatchUseCase>(relaxed = true),
        radio = radio,
    )

    @Test
    fun `a sync while a recording runs is refused before the radio is touched`() = runTest {
        recording.value = ActivityRecordingState(status = ActivityRecordingStatus.RECORDING)

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertTrue(result is DeviceSyncResult.Failed)
        assertEquals(0, pulls)
        coVerify(exactly = 0) { downloadImport.import(any(), any()) }
    }

    @Test
    fun `files that landed are imported, then the watch is stamped`() = runTest {
        pullResult = GarminPullResult(files = listOf(file(1), file(2)))

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Succeeded(2), result)
        coVerify(exactly = 1) { downloadImport.import(WATCH.id, pullResult.files) }
        // Stamped last: a sync that failed to import must come back for the same files.
        coVerify(exactly = 1) { deviceRepository.markSynced(WATCH.id, any()) }
    }

    @Test
    fun `a watch out of range fails the sync and stamps nothing`() = runTest {
        pullFailure = GarminGattClientException("out of range")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Failed("out of range"), result)
        coVerify(exactly = 0) { downloadImport.import(any(), any()) }
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `a failed import leaves the watch unstamped, so the next sync fetches again`() = runTest {
        pullResult = GarminPullResult(files = listOf(file(1)))
        coEvery { downloadImport.import(any(), any()) } throws IllegalStateException("Health Connect is down")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Failed("Health Connect is down"), result)
        coVerify(exactly = 0) { deviceRepository.markSynced(any(), any()) }
    }

    @Test
    fun `an interrupted sync that landed files says both things`() = runTest {
        pullResult = GarminPullResult(files = listOf(file(1)), incompleteReason = "the link dropped")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Failed("Imported 1 file(s), but sync was interrupted: the link dropped"), result)
        // The files did land, so they are imported before the failure is reported.
        coVerify(exactly = 1) { downloadImport.import(WATCH.id, pullResult.files) }
    }

    @Test
    fun `an interrupted sync that landed nothing reports only the interruption`() = runTest {
        pullResult = GarminPullResult(files = emptyList(), incompleteReason = "the watch went away")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Failed("the watch went away"), result)
    }

    @Test
    fun `workouts Health Connect refused are named, not hidden behind Synced`() = runTest {
        pullResult = GarminPullResult(files = listOf(file(1)))
        coEvery { downloadImport.import(any(), any()) } returns
            GarminActivityImportResult(written = 0, missingPermission = listOf(file(1)))

        val result = service().sync(WATCH, Duration.ZERO, null)

        val message = (result as DeviceSyncResult.Failed).message
        assertTrue(message, message.startsWith("1 workout(s) were not imported."))
    }

    @Test
    fun `files an earlier run saved but never imported go in before the pull`() = runTest {
        val pending = file(9)
        fileStore.save(pending, now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH.id)

        service().sync(WATCH, Duration.ZERO, null)

        // Their keys must be recorded before the listing is filtered, or they are fetched twice.
        coVerify(exactly = 1) { downloadImport.import(WATCH.id, match { it.size == 1 }) }
        assertEquals(1, pulls)
    }

    @Test
    fun `a pending import that fails stops the sync before the radio opens`() = runTest {
        fileStore.save(file(9), now = Instant.parse("2026-07-22T10:00:00Z"), deviceId = WATCH.id)
        coEvery { downloadImport.import(any(), any()) } throws IllegalStateException("Health Connect is down")

        val result = service().sync(WATCH, Duration.ZERO, null)

        assertEquals(DeviceSyncResult.Failed("Health Connect is down"), result)
        assertEquals("pulling more would only queue more behind a write path that is down", 0, pulls)
    }

    private fun file(index: Int) = GarminDownloadedFile(
        entry = GarminDirectoryEntry(
            fileIndex = index,
            type = GarminFileType.ACTIVITY,
            fileNumber = index,
            specificFlags = 0,
            fileFlags = 0,
            fileSize = 3,
            fileDate = Instant.parse("2026-07-22T10:00:00Z"),
        ),
        bytes = byteArrayOf(1, 2, 3),
    )

    private companion object {
        val WATCH = BleSensorDevice(
            id = "watch-1",
            displayName = "Forerunner",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Forerunner 255",
            capabilities = emptySet(),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-07-01T10:00:00Z"),
            kind = BleDeviceKind.WATCH,
        )
    }
}
