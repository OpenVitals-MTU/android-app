package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.insights.BasalMetabolicRate
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.BiologicalSex
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class BmrEstimateServiceTest {

    private val today: LocalDate = LocalDate.now()
    private val weightRead = readPermission(WeightRecord::class)
    private val heightRead = readPermission(HeightRecord::class)
    private val allGranted = BmrEstimateService.RequiredPermissions + weightRead + heightRead

    private val profile = BodyProfile(
        birthYear = today.year - 30,
        weightKg = 70.0,
        heightCm = 175.0,
        sex = BiologicalSex.MALE,
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun hc(
        granted: Set<String> = allGranted,
        weights: List<WeightEntry> = emptyList(),
        heightCm: Double? = null,
    ): HealthConnectManager = mockk(relaxed = true) {
        every { availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { grantedPermissions() } returns granted
        coEvery { readsOtherAppsDataNow() } returns true
        every { additionalDataAccessPermissions } returns emptySet()
        coEvery { readWeightEntries(any(), any()) } returns weights
        coEvery { readLatestHeight() } returns heightCm
    }

    private fun prefs(
        enabled: Boolean = true,
        profile: BodyProfile = this.profile,
        purgePending: Boolean = false,
    ): PreferencesRepository {
        var pending = purgePending
        return mockk {
            every { bmrEstimateEnabled } returns enabled
            every { bodyProfile() } returns profile
            every { bmrEstimatePurgePending } answers { pending }
            every { bmrEstimatePurgePending = any() } answers { pending = firstArg() }
        }
    }

    private fun expected(day: LocalDate, weightKg: Double): Double =
        BasalMetabolicRate.mifflinStJeor(BiologicalSex.MALE, weightKg, 175.0, day.year - profile.birthYear!!)

    @Test
    fun `disabled feature never touches Health Connect`() = runTest {
        val hc = hc()
        BmrEstimateService(hc, prefs(enabled = false)).syncNow()

        coVerify(exactly = 0) { hc.readWeightEntries(any(), any()) }
        coVerify(exactly = 0) { hc.reconcileEstimatedBmr(any(), any()) }
    }

    @Test
    fun `no write grant, no pass`() = runTest {
        val hc = hc(granted = setOf(HealthPermission.getReadPermission(BasalMetabolicRateRecord::class), weightRead))
        BmrEstimateService(hc, prefs()).syncNow()

        coVerify(exactly = 0) { hc.reconcileEstimatedBmr(any(), any()) }
        assertFalse(BmrEstimateService(hc, prefs()).canWrite())
    }

    @Test
    fun `a complete profile estimates every day of the window from the declared weight`() = runTest {
        val hc = hc()
        val estimates = slot<Map<LocalDate, Double>>()
        coEvery { hc.reconcileEstimatedBmr(any(), capture(estimates)) } returns Unit

        BmrEstimateService(hc, prefs()).syncNow()

        assertEquals(90, estimates.captured.size)
        assertEquals(expected(today, 70.0), estimates.captured.getValue(today), 1e-9)
        val first = today.minusDays(89)
        assertEquals(expected(first, 70.0), estimates.captured.getValue(first), 1e-9)
    }

    @Test
    fun `a measured weight applies from its day on, the declared one before it`() = runTest {
        val zone = ZoneId.systemDefault()
        val measuredDay = today.minusDays(10)
        val weights = listOf(
            WeightEntry(time = measuredDay.atStartOfDay(zone).toInstant().plusSeconds(43_200), weightKg = 80.0, source = "scale"),
        )
        val hc = hc(weights = weights, heightCm = 175.0)
        val estimates = slot<Map<LocalDate, Double>>()
        coEvery { hc.reconcileEstimatedBmr(any(), capture(estimates)) } returns Unit

        BmrEstimateService(hc, prefs()).syncNow()

        assertEquals(expected(measuredDay.minusDays(1), 70.0), estimates.captured.getValue(measuredDay.minusDays(1)), 1e-9)
        assertEquals(expected(measuredDay, 80.0), estimates.captured.getValue(measuredDay), 1e-9)
        assertEquals(expected(today, 80.0), estimates.captured.getValue(today), 1e-9)
    }

    @Test
    fun `an incomplete profile reconciles with no estimates, so stale records go`() = runTest {
        val hc = hc()
        val estimates = slot<Map<LocalDate, Double>>()
        coEvery { hc.reconcileEstimatedBmr(any(), capture(estimates)) } returns Unit

        BmrEstimateService(hc, prefs(profile = profile.copy(sex = null))).syncNow()

        assertEquals(emptyMap<LocalDate, Double>(), estimates.captured)
    }

    @Test
    fun `switched off with a purge still pending, the next pass finishes the purge`() = runTest {
        val hc = hc()
        val prefs = prefs(enabled = false, purgePending = true)

        BmrEstimateService(hc, prefs).syncIncremental()

        coVerify(exactly = 1) { hc.purgeEstimatedBmr(any()) }
        assertFalse(prefs.bmrEstimatePurgePending)
    }
}
