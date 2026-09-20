package tech.mmarca.openvitals.features.devicesync

import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.features.devicesync.bluetooth.BluetoothSyncManager
import tech.mmarca.openvitals.features.devicesync.bluetooth.DiscoveredSyncDevice
import tech.mmarca.openvitals.features.devicesync.bluetooth.SyncConnectionState
import tech.mmarca.openvitals.features.devicesync.protocol.CODES_DIFFER_REASON
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncPipe
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRole
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSession
import tech.mmarca.openvitals.features.devicesync.protocol.SyncSessionConfig
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingState
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The wizard around the session: the guest's dial, and the step where the user compares the codes. */
class DeviceSyncViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bluetooth = mockk<BluetoothSyncManager>(relaxed = true)
    private val recordingController = mockk<ActivityRecordingController>(relaxed = true)
    private val pixel = DiscoveredSyncDevice(address = "AA:BB:CC:DD:EE:FF", name = "Pixel", bonded = false)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { bluetooth.isBluetoothEnabled() } returns true
        every { bluetooth.bondedCandidates() } returns emptyList()
        every { bluetooth.startDiscovery() } returns emptyFlow()
        every { bluetooth.connectionState } returns MutableStateFlow(SyncConnectionState.CONNECTED)
        every { recordingController.state } returns MutableStateFlow(ActivityRecordingState())
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun viewModel() = DeviceSyncViewModel(
        context = mockk<Context>(relaxed = true),
        bluetooth = bluetooth,
        healthConnectManager = mockk(relaxed = true),
        importRepository = mockk(relaxed = true),
        originRepository = mockk(relaxed = true),
        reportStore = mockk(relaxed = true),
        recordingController = recordingController,
    )

    // The guest's dial.

    @Test
    fun `picking a phone connects and moves on without asking for a code`() = runTest {
        val viewModel = viewModel().apply { startScanning() }

        viewModel.selectDevice(pixel)

        assertEquals(DeviceSyncStep.RANGE, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.connecting)
    }

    @Test
    fun `a failed connect stays on the list and says so`() = runTest {
        coEvery { bluetooth.connect(any()) } throws IOException("not listening")
        val viewModel = viewModel().apply { startScanning() }

        viewModel.selectDevice(pixel)

        val state = viewModel.uiState.value
        assertEquals(DeviceSyncStep.GUEST_SCANNING, state.step)
        assertEquals(DeviceSyncError.CONNECT_FAILED, state.error)
        assertFalse(state.connecting)
    }

    @Test
    fun `cancel while connecting stops the dial`() = runTest {
        var dialCancelled = false
        coEvery { bluetooth.connect(any()) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                dialCancelled = true
            }
        }
        val viewModel = viewModel().apply { startScanning() }
        viewModel.selectDevice(pixel)
        assertTrue(viewModel.uiState.value.connecting)

        viewModel.cancel()

        assertTrue(dialCancelled)
        assertEquals(DeviceSyncStep.ROLE, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.connecting)
    }

    @Test
    fun `a second tap while connecting does not dial twice`() = runTest {
        var dials = 0
        coEvery { bluetooth.connect(any()) } coAnswers {
            dials += 1
            awaitCancellation()
        }
        val viewModel = viewModel().apply { startScanning() }

        viewModel.selectDevice(pixel)
        viewModel.selectDevice(pixel)

        assertEquals(1, dials)
        viewModel.cancel()
    }

    // The compare step.

    @Test
    fun `the compare step shows the session's code, and They match lets the sync finish`() = runTest {
        var peerCode = ""
        val (viewModel, peer) = startSyncAgainstPeer { peerCode = it; true }

        val state = viewModel.uiState.value
        assertEquals(DeviceSyncStep.COMPARE_CODE, state.step)
        assertEquals(peerCode, state.code)

        viewModel.answerCodeComparison(matches = true)

        assertTrue(peer.await().completed)
        assertEquals(DeviceSyncStep.REPORT, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `They do not match ends the sync and says why`() = runTest {
        val (viewModel, peer) = startSyncAgainstPeer { true }

        viewModel.answerCodeComparison(matches = false)

        assertFalse(peer.await().completed)
        val state = viewModel.uiState.value
        assertEquals(DeviceSyncStep.REPORT, state.step)
        assertEquals(DeviceSyncError.CODES_DIFFER, state.error)
        assertEquals(CODES_DIFFER_REASON, state.report?.abortReason)
    }

    @Test
    fun `the other phone's words cannot claim that this user said no`() = runTest {
        // The peer's user says the codes differ. Its abort text is the same constant.
        val (viewModel, peer) = startSyncAgainstPeer { false }

        peer.await()

        val state = viewModel.uiState.value
        assertEquals(DeviceSyncStep.REPORT, state.step)
        assertNull(state.error)
        assertTrue(state.report?.abortReason.orEmpty().startsWith("peer aborted"))
    }

    @Test
    fun `leaving the wizard on the compare step drops the code`() = runTest {
        val (viewModel, peer) = startSyncAgainstPeer { true }
        assertEquals(DeviceSyncStep.COMPARE_CODE, viewModel.uiState.value.step)

        viewModel.cancel()
        // A late tap from the old screen must not reach a later session.
        viewModel.answerCodeComparison(matches = true)

        assertEquals(DeviceSyncStep.ROLE, viewModel.uiState.value.step)
        assertEquals("", viewModel.uiState.value.code)
        peer.cancel()
    }

    /**
     * Puts the view model, as host, on one end of an in-memory link with a real guest
     * session on the other. Returns once both sides wait for their user.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.startSyncAgainstPeer(
        peerConfirms: suspend (String) -> Boolean,
    ): Pair<DeviceSyncViewModel, Deferred<SyncReport>> {
        val (hostPipe, guestPipe) = SyncPipe.create()
        every { bluetooth.transport() } returns hostPipe
        val peer = async {
            SyncSession(
                transport = guestPipe,
                store = EmptyStore,
                config = SyncSessionConfig(
                    role = SyncRole.GUEST,
                    confirmCode = peerConfirms,
                    deviceName = "Peer",
                    supportedTypes = listOf("StepsRecord"),
                ),
            ).run()
        }
        val viewModel = viewModel()
        viewModel.startHosting(grantedSeconds = 120)
        viewModel.startSync()
        // Runs the handshake. Does not move the clock, so no timeout fires.
        runCurrent()
        return viewModel to peer
    }

    private object EmptyStore : SyncRecordStore {
        override fun readKeys(types: Set<String>): Flow<String> = emptyFlow()
        override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = emptyFlow()
        override suspend fun writeItems(items: List<SyncItem>): Set<String> = emptySet()
    }
}
