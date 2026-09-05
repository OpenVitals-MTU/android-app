package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * The notification buttons land on [ActivityRecordingController.pauseRecording],
 * `resumeRecording` and `discardRecording`, and the restore-from-store path runs at construction.
 * Timed recordings throughout, so no satellite is needed.
 */
class ActivityRecordingControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** A type with no GPS route and no repetition unit -> a timed recording. */
    private val stationaryBike = DefaultActivityEntryTypes.single { it.id == "stationary_bike" }

    private lateinit var filesDir: File
    private lateinit var preferences: FakeSharedPreferences
    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var ble: BleSensorCoordinator

    @Before fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } returns Unit
        mockkObject(ActivityRecordingService.Companion)
        every { ActivityRecordingService.intent(any(), any()) } returns mockk(relaxed = true)
        mockkConstructor(Intent::class)

        filesDir = Files.createTempDirectory("activity-recording-controller").toFile()
        preferences = FakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { context.filesDir } returns filesDir
        every { context.getString(any()) } returns "recording error"
        preferencesRepository = mockk()
        every { preferencesRepository.activityRecordingPreferences() } returns ActivityRecordingPreferences()
        every { preferencesRepository.activityRecordingDashboardLayout(any()) } returns
            ActivityRecordingDashboardLayout()
        ble = bleCoordinator()
    }

    @After fun tearDown() {
        filesDir.deleteRecursively()
        unmockkAll()
    }

    private fun bleCoordinator() = mockk<BleSensorCoordinator>(relaxed = true).also { coordinator ->
        every { coordinator.metrics } returns MutableStateFlow(BleRecordingMetrics())
        every { coordinator.stopRecording() } returns BleRecordingSampleBuffer()
    }

    private fun controller(
        bleSensorCoordinator: BleSensorCoordinator = ble,
    ) = ActivityRecordingController(
        context = context,
        preferencesRepository = preferencesRepository,
        bleSensorCoordinator = bleSensorCoordinator,
        coMapsNavigationRepository = mockk(relaxed = true),
        coMapsGuidanceFeed = tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed(mockk(relaxed = true)),
        recordingStore = ActivityRecordingStore(context),
    )

    @Test fun `notification buttons drive the recording - pause, resume, discard`() {
        val recorder = controller()
        assertTrue(recorder.startRecording(stationaryBike, null))
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)

        recorder.pauseRecording()
        assertEquals(ActivityRecordingStatus.PAUSED, recorder.state.value.status)
        assertNotNull(recorder.state.value.pausedStartedAt)

        // The pause has to be measurably long, or the closed interval is empty and dropped.
        Thread.sleep(5)
        recorder.resumeRecording()
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)
        // Resume must close the open pause interval.
        assertNull(recorder.state.value.pausedStartedAt)
        assertEquals(1, recorder.state.value.pauseIntervals.size)

        recorder.discardRecording()
        assertFalse(recorder.state.value.isActive)
    }

    @Test fun `discard clears the persisted draft, so a restart stays idle`() {
        val recorder = controller()
        recorder.startRecording(stationaryBike, null)

        recorder.discardRecording()

        assertFalse(ActivityRecordingStore(context).restore().isActive)
        verify { ble.stopRecording() }
    }

    @Test fun `a denied notification permission refuses to start and says why`() {
        // A foreground service cannot post its notification without the permission; starting anyway would die with the activity.
        mockkObject(ActivityRecordingController.Companion)
        every { ActivityRecordingController.hasNotificationPermission(any()) } returns false
        val recorder = controller()

        assertFalse(recorder.startRecording(stationaryBike, null))
        assertFalse(recorder.state.value.isActive)
        assertNotNull(recorder.state.value.errorMessage)
    }

    @Test fun `finishRecording snapshots the session and hands back the BLE buffer`() {
        val recorder = controller()
        recorder.startRecording(stationaryBike, null)

        val snapshot = recorder.finishRecording()

        assertNotNull(snapshot)
        assertEquals(ActivityRecordingKind.TIMED, snapshot!!.recordingKind)
        // A zero-length session is clamped to at least one second.
        assertTrue(snapshot.endTime.isAfter(snapshot.startTime))
        verify { ble.stopRecording() }
    }

    /** The restore half. The controller re-subscribes to BLE metrics on construction but never calls `startRecording()` again. */
    @Test fun `a recording restored after process death comes up already recording`() {
        val first = controller()
        first.startRecording(stationaryBike, null)

        // A second controller booting on the survived preferences.
        val second = controller(bleSensorCoordinator = bleCoordinator())

        assertEquals(ActivityRecordingStatus.RECORDING, second.state.value.status)
        assertEquals(stationaryBike.id, second.state.value.activityTypeId)
    }
}
