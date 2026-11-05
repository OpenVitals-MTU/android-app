package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.OvulationTestRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleEstimateBasis
import tech.mmarca.openvitals.domain.cycle.OvulationMarker
import tech.mmarca.openvitals.domain.model.CycleEntry
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** The write guards and reconcile triggering. The permission failure must be a SecurityException: ScreenError maps only that type. */
class CycleRepositoryWriteTest {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val flowWrite = HealthPermission.getWritePermission(MenstruationFlowRecord::class)
    private val spottingWrite = HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class)

    private val flowRequest = CycleEntryWriteRequest(
        kind = CycleEntryKind.MENSTRUATION_FLOW,
        time = Instant.parse("2026-08-04T09:00:00Z"),
        flow = CycleRecordValues.FLOW_MEDIUM,
    )

    private val spottingRequest = CycleEntryWriteRequest(
        kind = CycleEntryKind.SPOTTING,
        time = Instant.parse("2026-08-04T09:00:00Z"),
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

    private fun hc(granted: Set<String>): HealthConnectManager = mockk(relaxed = true) {
        every { availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { grantedPermissions() } returns granted
    }

    @Test
    fun `a missing write permission throws SecurityException and never reaches Health Connect`() = runTest {
        val hc = hc(granted = emptySet())
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        assertThrows(SecurityException::class.java) {
            kotlinx.coroutines.runBlocking { repository.writeCycleEntry(flowRequest) }
        }
        coVerify(exactly = 0) { hc.writeCycleEntry(any()) }
    }

    @Test
    fun `a flow write reconciles the derived period over the written day`() = runTest {
        val hc = hc(granted = setOf(flowWrite))
        coEvery { hc.writeCycleEntry(flowRequest) } returns "client-id"
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        assertEquals("client-id", repository.writeCycleEntry(flowRequest))

        val day = flowRequest.time.atZone(zone).toLocalDate()
        coVerify(exactly = 1) { hc.reconcileMenstruationPeriods(setOf(day)) }
    }

    @Test
    fun `a non-flow write does not reconcile`() = runTest {
        val hc = hc(granted = setOf(spottingWrite))
        coEvery { hc.writeCycleEntry(spottingRequest) } returns "client-id"
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        repository.writeCycleEntry(spottingRequest)

        coVerify(exactly = 0) { hc.reconcileMenstruationPeriods(any()) }
    }

    @Test
    fun `a reconcile failure does not fail the write`() = runTest {
        val hc = hc(granted = setOf(flowWrite))
        coEvery { hc.writeCycleEntry(flowRequest) } returns "client-id"
        coEvery { hc.reconcileMenstruationPeriods(any()) } throws IllegalStateException("hc down")
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        assertEquals("client-id", repository.writeCycleEntry(flowRequest))
    }

    @Test
    fun `moving a flow entry reconciles both the old and the new day`() = runTest {
        val hc = hc(granted = setOf(flowWrite))
        val oldTime = Instant.parse("2026-08-01T10:00:00Z")
        coEvery { hc.readCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            time = oldTime,
            flow = CycleRecordValues.FLOW_LIGHT,
            isOpenVitalsEntry = true,
        )
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        repository.updateCycleEntry("uid", flowRequest)

        val expectedDays = setOf(
            oldTime.atZone(zone).toLocalDate(),
            flowRequest.time.atZone(zone).toLocalDate(),
        )
        coVerify(exactly = 1) { hc.updateCycleEntry("uid", flowRequest) }
        coVerify(exactly = 1) { hc.reconcileMenstruationPeriods(expectedDays) }
    }

    @Test
    fun `deleting a flow entry reconciles its former day`() = runTest {
        val hc = hc(granted = setOf(flowWrite))
        val oldTime = Instant.parse("2026-08-02T08:00:00Z")
        coEvery { hc.readCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") } returns CycleEntry(
            id = "uid",
            kind = CycleEntryKind.MENSTRUATION_FLOW,
            time = oldTime,
            flow = CycleRecordValues.FLOW_LIGHT,
            isOpenVitalsEntry = true,
        )
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        repository.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid")

        coVerify(exactly = 1) { hc.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid") }
        coVerify(exactly = 1) {
            hc.reconcileMenstruationPeriods(setOf(oldTime.atZone(zone).toLocalDate()))
        }
    }

    @Test
    fun `delete guards on the write permission too`() = runTest {
        val hc = hc(granted = emptySet())
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        assertThrows(SecurityException::class.java) {
            kotlinx.coroutines.runBlocking {
                repository.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "uid")
            }
        }
        coVerify(exactly = 0) { hc.deleteCycleEntry(any(), any()) }
    }

    @Test
    fun `statistics come from flow days united with period spans`() = runTest {
        val today = LocalDate.of(2026, 8, 5)
        val readMenstruation = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
        val hc = hc(granted = setOf(readMenstruation))
        coEvery { hc.readMenstruationFlowEntries(any(), any()) } returns emptyList()
        coEvery { hc.readMenstruationPeriods(any(), any()) } returns listOf(
            tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry(
                startTime = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant(),
                endTime = LocalDate.of(2026, 7, 5).atStartOfDay(zone).toInstant(),
                source = "other.app",
            ),
        )
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        val statistics = repository.loadCycleStatistics(today)

        assertEquals(listOf(LocalDate.of(2026, 7, 1)), statistics?.cycleStarts)
    }

    @Test
    fun `statistics read the whole history, not the last eighteen months`() = runTest {
        val today = LocalDate.of(2026, 8, 5)
        val readMenstruation = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
        val hc = hc(granted = setOf(readMenstruation))
        val starts = mutableListOf<java.time.Instant>()
        coEvery { hc.readMenstruationFlowEntries(any(), any()) } answers {
            starts += firstArg<java.time.Instant>()
            emptyList()
        }
        coEvery { hc.readMenstruationPeriods(any(), any()) } returns emptyList()
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        repository.loadCycleStatistics(today)

        // A drip import brings a decade of cycles; the history section must reach them.
        val tenYearsBack = today.minusYears(10).atStartOfDay(zone).toInstant()
        assertTrue("read from ${starts.single()}", !starts.single().isAfter(tenYearsBack))
    }

    @Test
    fun `statistics are null without the menstruation read permission`() = runTest {
        val repository = CycleRepositoryImpl(hc(granted = emptySet()), FakeCycleJournalRepository(), FakePreferences(), FakePreferences())
        assertEquals(null, repository.loadCycleStatistics(LocalDate.of(2026, 8, 5)))
    }

    @Test
    fun `a positive LH test read with its permission sharpens the estimate`() = runTest {
        val today = LocalDate.of(2026, 8, 12)
        val lhDay = LocalDate.of(2026, 8, 11)
        val readMenstruation = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
        val readOvulation = HealthPermission.getReadPermission(OvulationTestRecord::class)
        val hc = hc(granted = setOf(readMenstruation, readOvulation))
        coEvery { hc.readMenstruationFlowEntries(any(), any()) } returns emptyList()
        coEvery { hc.readMenstruationPeriods(any(), any()) } returns listOf(period(LocalDate.of(2026, 7, 1)), period(LocalDate.of(2026, 7, 29)))
        coEvery { hc.readOvulationTests(any(), any()) } returns listOf(
            OvulationTestEntry(lhDay.atTime(8, 0).atZone(zone).toInstant(), CycleRecordValues.OVULATION_POSITIVE, "other.app"),
        )
        val repository = CycleRepositoryImpl(hc, FakeCycleJournalRepository(), FakePreferences(), FakePreferences())

        val basis = repository.loadCycleStatistics(today)?.estimate?.estimateOrNull?.basis

        assertEquals(CycleEstimateBasis.Marker(OvulationMarker.LH_TEST, lhDay, emptySet(), 0), basis)
        coVerify(exactly = 0) { hc.readBasalBodyTemperatureEntries(any(), any()) }
        coVerify(exactly = 0) { hc.readCervicalMucusEntries(any(), any()) }
    }

    private fun period(start: LocalDate) = tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry(
        startTime = start.atStartOfDay(zone).toInstant(),
        endTime = start.plusDays(4).atStartOfDay(zone).toInstant(),
        source = "other.app",
    )
}
