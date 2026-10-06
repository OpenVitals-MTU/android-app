package tech.mmarca.openvitals.devices.xiaomi

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.FakeScaleWeighInRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.pairing.CompanionDevicePairing
import tech.mmarca.openvitals.domain.model.ScaleReading

/** When the phone listens for the scale, and what keeps it listening. */
@OptIn(ExperimentalCoroutinesApi::class)
class XiaomiScaleListenerTest {

    private class FakeRadio : ScaleScanRadio {
        var answer = ScaleListenerStatus.LISTENING

        /** The address and restart flag of every arm, in order. */
        val arms = mutableListOf<Pair<String?, Boolean>>()
        var disarms = 0

        override fun arm(address: String?, restart: Boolean): ScaleListenerStatus {
            arms += address to restart
            return answer
        }

        override fun disarm() {
            disarms += 1
        }
    }

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val radio = FakeRadio()
    private val store = XiaomiScaleStore(FakeSharedPreferences())
    private val writer = mockk<ScaleWeighInWriter>(relaxed = true)
    private val companion = mockk<CompanionDevicePairing>(relaxed = true)
    private val weighIns = FakeScaleWeighInRepository()

    /** What the foreground port was asked to do, in order. */
    private val foregroundCalls = mutableListOf<String>()
    private val foreground = object : ScaleForeground {
        override fun raise(): Boolean = true.also { foregroundCalls += "raise" }
        override fun lower() {
            foregroundCalls += "lower"
        }
    }

    private fun listener(scope: TestScope = TestScope(UnconfinedTestDispatcher())) = XiaomiScaleListener(
        context = mockk<Context>(relaxed = true),
        store = store,
        radio = radio,
        writer = writer,
        companion = companion,
        weighIns = weighIns,
        foreground = foreground,
        scope = scope,
    )

    private val listener = listener()
    private val key = ByteArray(16) { it.toByte() }

    @Before
    fun setUp() {
        // getInstance lives on the companion, so mockkStatic does not reach it.
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any()) } returns workManager
    }

    @After
    fun tearDown() {
        unmockkObject(WorkManager)
    }

    @Test
    fun `without a scale an app start touches neither the radio nor the schedule`() {
        listener.onAppStart()

        assertEquals(emptyList<Any>(), radio.arms)
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
        assertEquals(ScaleListenerStatus.OFF, listener.status.value)
    }

    @Test
    fun `an app start listens again, keeps the half-hourly re-arm, and retries what is waiting`() {
        store.setBindKey(key)
        store.bind("8C:D0:B2:F6:BE:EF", profile = 1)
        val request = slot<PeriodicWorkRequest>()

        listener.onAppStart()

        // Not a restart: the process a broadcast started must not cut the scan mid weigh-in.
        assertEquals(listOf("8C:D0:B2:F6:BE:EF" to false), radio.arms)
        assertEquals(ScaleListenerStatus.LISTENING, listener.status.value)
        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                XiaomiScaleListener.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertEquals(TimeUnit.MINUTES.toMillis(30), request.captured.workSpec.intervalDuration)
        coVerify(exactly = 1) { writer.writePending() }
    }

    @Test
    fun `Bluetooth being off is a status, and the re-arm stays scheduled to catch it coming back`() {
        store.setBindKey(key)
        radio.answer = ScaleListenerStatus.BLUETOOTH_OFF

        assertEquals(ScaleListenerStatus.BLUETOOTH_OFF, listener.arm())

        assertEquals(ScaleListenerStatus.BLUETOOTH_OFF, listener.status.value)
        verify(exactly = 1) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `Android can be asked to watch for the scale once the scale is known`() = runTest {
        store.setBindKey(key)
        coEvery { companion.associate(any(), any(), any()) } returns true
        every { companion.isAssociated("8C:D0:B2:F6:BE:EF") } returns true

        assertFalse("no scale to name yet", listener.allowSystemWake())
        assertFalse(listener.isWokenBySystem())

        store.bind("8C:D0:B2:F6:BE:EF", profile = 1)

        assertTrue(listener.allowSystemWake())
        assertTrue(listener.isWokenBySystem())
    }

    private val scale = "8C:D0:B2:F6:BE:EF"
    private val heardAt = Instant.ofEpochSecond(1744250625)

    private suspend fun hear(reading: ScaleReading, scaleTimestamp: Long = 1744250605) {
        weighIns.merge(scaleTimestamp, profile = 1, reading = reading, time = heardAt, now = heardAt)
    }

    @Test
    fun `the scale waking holds the foreground and a fresh scan until both frames are in`() = runTest {
        store.setBindKey(key)
        store.bind(scale, profile = 1)
        val listener = listener(TestScope(testScheduler))

        // Android hands the address over in lower case.
        listener.onCompanionDeviceAppeared(scale.lowercase())
        runCurrent()
        assertEquals(listOf("raise"), foregroundCalls)
        assertEquals(listOf(scale to true), radio.arms)

        hear(ScaleReading(weightKg = 69.9, heartRateBpm = 92, impedanceLowOhm = 543.2))
        runCurrent()
        assertEquals("the second frame is still to come", listOf("raise"), foregroundCalls)

        hear(ScaleReading(impedanceHighOhm = 497.6))
        runCurrent()
        assertEquals(listOf("raise", "lower"), foregroundCalls)
    }

    @Test
    fun `a weigh-in that never completes gives the foreground back when its window runs out`() = runTest {
        store.setBindKey(key)
        store.bind(scale, profile = 1)
        // Yesterday's weigh-in is complete, and is not what this window waits for.
        hear(ScaleReading(70.1, 80, 540.0, 495.0), scaleTimestamp = 1744164205)
        val listener = listener(TestScope(testScheduler))

        listener.onCompanionDeviceAppeared(scale)
        // Socks on: the weight arrives and the second frame never does.
        hear(ScaleReading(weightKg = 69.9))
        advanceTimeBy(XiaomiScaleListener.WEIGH_IN_WINDOW_MILLIS - 1)
        assertEquals(listOf("raise"), foregroundCalls)

        advanceTimeBy(2)
        assertEquals(listOf("raise", "lower"), foregroundCalls)
    }

    @Test
    fun `one window per weigh-in, and none for a device that is not the scale`() = runTest {
        store.setBindKey(key)
        store.bind(scale, profile = 1)
        val listener = listener(TestScope(testScheduler))

        // A watch is a companion device too, and Android reports every one to every service.
        listener.onCompanionDeviceAppeared("F8:83:06:00:00:01")
        runCurrent()
        assertEquals(emptyList<Any>(), foregroundCalls)

        // Android can report a scale that is still awake a second time.
        listener.onCompanionDeviceAppeared(scale)
        listener.onCompanionDeviceAppeared(scale)
        runCurrent()
        assertEquals(listOf("raise"), foregroundCalls)
        assertEquals("a second restart would cut the scan mid weigh-in", 1, radio.arms.size)

        // Once the window has closed, the next weigh-in gets its own.
        advanceTimeBy(XiaomiScaleListener.WEIGH_IN_WINDOW_MILLIS + 1)
        listener.onCompanionDeviceAppeared(scale)
        runCurrent()
        assertEquals(listOf("raise", "lower", "raise"), foregroundCalls)
    }

    @Test
    fun `a new key listens for any scale afresh, and removing the scale stops everything`() {
        store.setBindKey(key)
        store.bind("8C:D0:B2:F6:BE:EF", profile = 1)

        listener.useKey(ByteArray(16) { 9 })

        // The old scale's address went with the old key.
        assertEquals(listOf<Pair<String?, Boolean>>(null to true), radio.arms)

        store.bind("8C:D0:B2:F6:BE:EF", profile = 1)
        listener.forget()

        // Android must stop watching for a scale the app no longer knows.
        verify(exactly = 1) { companion.disassociate("8C:D0:B2:F6:BE:EF") }
        assertEquals(1, radio.disarms)
        verify(exactly = 1) { workManager.cancelUniqueWork(XiaomiScaleListener.WORK_NAME) }
        assertNull(store.bindKey())
        assertEquals(ScaleListenerStatus.OFF, listener.status.value)
    }
}
