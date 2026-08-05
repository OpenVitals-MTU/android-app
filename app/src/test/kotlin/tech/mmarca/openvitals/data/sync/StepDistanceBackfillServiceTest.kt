package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class StepDistanceBackfillServiceTest {

    private val stepsRead = readPermission(StepsRecord::class)
    private val distanceRead = readPermission(DistanceRecord::class)
    private val distanceWrite = HealthPermission.getWritePermission(DistanceRecord::class)
    private val allRequired = setOf(stepsRead, distanceRead, distanceWrite)

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
        granted: Set<String> = allRequired,
        historyPermissionDefined: Boolean = false,
    ): HealthConnectManager = mockk(relaxed = true) {
        every { availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { grantedPermissions() } returns granted
        every { additionalDataAccessPermissions } returns if (historyPermissionDefined) {
            setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY)
        } else {
            emptySet()
        }
        coEvery { readDailySteps(any(), any(), any(), any(), any(), any(), any(), any()) } returns emptyList()
    }

    private fun prefs(enabled: Boolean = true, strideMeters: Double = 0.7): PreferencesRepository =
        mockk {
            every { stepDistanceBackfillEnabled } returns enabled
            every { strideLengthMeters } returns strideMeters
        }

    @Test
    fun `disabled feature never touches Health Connect`() = runTest {
        val hc = hc()
        StepDistanceBackfillService(hc, prefs(enabled = false)).syncNow()

        coVerify(exactly = 0) { hc.readDailySteps(any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { hc.reconcileStepDerivedDistance(any(), any(), any()) }
    }

    @Test
    fun `a missing write permission is a no-op`() = runTest {
        val hc = hc(granted = setOf(stepsRead, distanceRead))
        StepDistanceBackfillService(hc, prefs()).syncNow()

        coVerify(exactly = 0) { hc.reconcileStepDerivedDistance(any(), any(), any()) }
    }

    @Test
    fun `the window spans ninety days and feeds the reconcile`() = runTest {
        val hc = hc()
        val today = LocalDate.now()
        coEvery {
            hc.readDailySteps(any(), any(), any(), any(), any(), any(), any(), any())
        } returns listOf(DailySteps(date = today, steps = 1200L, distanceMeters = 0.0))

        StepDistanceBackfillService(hc, prefs(strideMeters = 0.7)).syncNow()

        val window = slot<ClosedRange<LocalDate>>()
        val steps = slot<Map<LocalDate, Long>>()
        coVerify(exactly = 1) { hc.reconcileStepDerivedDistance(capture(window), capture(steps), 0.7) }
        assertEquals(today.minusDays(89), window.captured.start)
        assertEquals(today, window.captured.endInclusive)
        assertEquals(mapOf(today to 1200L), steps.captured)
    }

    @Test
    fun `the window clamps to thirty days without the history permission`() = runTest {
        val hc = hc(historyPermissionDefined = true)

        StepDistanceBackfillService(hc, prefs()).syncNow()

        val window = slot<ClosedRange<LocalDate>>()
        coVerify(exactly = 1) { hc.reconcileStepDerivedDistance(capture(window), any(), any()) }
        assertEquals(LocalDate.now().minusDays(29), window.captured.start)
    }

    @Test
    fun `the incremental pass is throttled but syncNow is not`() = runTest {
        val hc = hc()
        val service = StepDistanceBackfillService(hc, prefs())

        service.syncIncremental()
        service.syncIncremental()
        coVerify(exactly = 1) { hc.reconcileStepDerivedDistance(any(), any(), any()) }

        service.syncNow()
        coVerify(exactly = 2) { hc.reconcileStepDerivedDistance(any(), any(), any()) }
    }

    @Test
    fun `purge covers the full history window even when the feature is off`() = runTest {
        val hc = hc()
        StepDistanceBackfillService(hc, prefs(enabled = false)).purgeDerivedRecords()

        val window = slot<ClosedRange<LocalDate>>()
        coVerify(exactly = 1) { hc.purgeStepDerivedDistance(capture(window)) }
        assertEquals(LocalDate.now().minusDays(HistoryLookbackDays), window.captured.start)
    }

    @Test
    fun `a reconcile failure is swallowed`() = runTest {
        val hc = hc()
        coEvery { hc.reconcileStepDerivedDistance(any(), any(), any()) } throws IllegalStateException("hc down")

        StepDistanceBackfillService(hc, prefs()).syncNow()
    }
}
