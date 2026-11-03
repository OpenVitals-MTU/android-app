package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.wearlink.WearLinkProfile

class WearOsTileProfilePushTest {

    private val zone = ZoneId.of("Europe/Amsterdam")
    private val now = Instant.parse("2026-11-03T17:30:00Z")
    private val midnight = Instant.parse("2026-11-02T23:00:00Z")
    private val settings = WearOsTileProfilePush.Settings(0.72, UnitSystem.METRIC, 8_000.0, 5_000.0, 400.0)
    private val dayTotals = WearOsTileProfilePush.DayTotals(steps = 6_797, distanceMeters = 4_890.4, activeKcal = 212.6)

    @Test
    fun `the profile goes in whole millimetres, grams and goals`() {
        assertEquals(
            WearLinkProfile.Profile(720, 81_500, WearLinkProfile.Units.METRIC, 8_000, 5_000, 400),
            WearOsTileProfilePush.profile(settings, weightKg = 81.5),
        )
    }

    @Test
    fun `a weight the watch would refuse goes as none, and the stride is bounded as the app bounds it`() {
        val profile = WearOsTileProfilePush.profile(settings.copy(strideMeters = 9.0, distanceUnits = UnitSystem.IMPERIAL), weightKg = 4.0)

        assertNull(profile.weightGrams)
        assertNull(WearOsTileProfilePush.profile(settings, weightKg = Double.NaN).weightGrams)
        assertEquals(1_500, profile.strideMillimetres)
        assertEquals(WearLinkProfile.Units.IMPERIAL, profile.units)
    }

    @Test
    fun `today's totals start at local midnight, say when they were read, and leave out what may not be read`() {
        assertEquals(
            WearLinkProfile.Totals(midnight.toEpochMilli(), now.toEpochMilli(), 6_797, 4_890, 213),
            WearOsTileProfilePush.totals(dayTotals, midnight, now),
        )
        assertEquals(
            WearLinkProfile.Totals(midnight.toEpochMilli(), now.toEpochMilli(), 6_797, null, null),
            WearOsTileProfilePush.totals(WearOsTileProfilePush.DayTotals(6_797, null, null), midnight, now),
        )
    }

    @Test
    fun `in the foreground the watch gets the profile and the totals, which hold its steps through the newest written minute`() = runTest {
        val port = RecordingPort()
        val written = Instant.parse("2026-11-03T17:12:00Z")

        push(port).push(WATCH, watchStepsThrough = written)

        assertEquals(81_500, port.pushes.single().first?.weightGrams)
        assertEquals(6_797L, port.pushes.single().second?.steps)
        // The watch adds its own minutes from the next one on.
        assertEquals(written.plusSeconds(60).toEpochMilli(), port.pushes.single().second?.asOfEpochMillis)
    }

    @Test
    fun `steps the watch wrote before today, or never, leave all of today's own minutes on top, from midnight`() = runTest {
        val port = RecordingPort()

        push(port).push(WATCH, watchStepsThrough = Instant.parse("2026-11-01T09:00:00Z"))
        push(port).push(WATCH, watchStepsThrough = null)

        assertEquals(listOf(midnight.toEpochMilli(), midnight.toEpochMilli()), port.pushes.map { it.second?.asOfEpochMillis })
    }

    @Test
    fun `nothing goes when other apps' data is not visible, before or after the reads`() = runTest {
        val port = RecordingPort()
        val visibility = ArrayDeque(listOf(true, false))

        push(port, othersVisible = { false }).push(WATCH, null)
        push(port, othersVisible = { visibility.removeFirst() }).push(WATCH, null)

        assertEquals(0, port.pushes.size)
    }

    @Test
    fun `a failed weight read keeps the profile back, a failed totals read the totals`() = runTest {
        val port = RecordingPort()

        push(port, weight = { throw IllegalStateException("rate limited") }).push(WATCH, null)
        push(port, totals = { throw IllegalStateException("rate limited") }).push(WATCH, null)

        assertNull(port.pushes[0].first)
        assertEquals(6_797L, port.pushes[0].second?.steps)
        assertNotNull(port.pushes[1].first)
        assertNull(port.pushes[1].second)
    }

    @Test
    fun `a failure to reach the watch never escapes`() = runTest {
        val port = RecordingPort(failure = WearOsLinkFailure.NoAnswer())

        push(port).push(WATCH, null)

        assertEquals(0, port.pushes.size)
    }

    private fun push(
        port: WearOsNodePort,
        othersVisible: suspend () -> Boolean = { true },
        weight: suspend () -> Double? = { 81.5 },
        totals: suspend (LocalDate) -> WearOsTileProfilePush.DayTotals? = { dayTotals },
    ) = WearOsTileProfilePush(port, { settings }, weight, othersVisible, totals, { now }, { zone })

    private class RecordingPort(private val failure: Exception? = null) : WearOsNodePort {
        val pushes = ArrayList<Pair<WearLinkProfile.Profile?, WearLinkProfile.Totals?>>()

        override suspend fun pushProfile(
            targetAddress: String?,
            targetName: String?,
            profile: WearLinkProfile.Profile?,
            totals: WearLinkProfile.Totals?,
        ): Boolean {
            failure?.let { throw it }
            pushes += profile to totals
            return true
        }

        override suspend fun checkStatus(targetAddress: String?, targetName: String?) = WearOsCompanionStatus()
        override suspend fun pullHeartRate(targetAddress: String?, targetName: String?, since: Instant): WearOsHeartRatePage? = null
        override suspend fun pullSleepMinutes(targetAddress: String?, targetName: String?, since: Instant): WearOsSleepMinutePage? = null
        override suspend fun pullSteps(targetAddress: String?, targetName: String?, since: Instant): WearOsStepPage? = null
        override suspend fun pullAscent(targetAddress: String?, targetName: String?, since: Instant): WearOsAscentPage? = null
        override suspend fun liveHeartRate(
            targetAddress: String?,
            targetName: String?,
            onSample: (WearOsHeartRateSample) -> Unit,
        ): Boolean = false
    }

    private companion object {
        val WATCH = BleSensorDevice(
            id = "watch-1",
            displayName = "TicWatch Pro 5",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "TicWatch Pro 5",
            capabilities = emptySet(),
            enabled = true,
            wheelCircumferenceMm = null,
            addedAt = Instant.parse("2026-01-01T00:00:00Z"),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.WEAROS,
        )
    }
}
