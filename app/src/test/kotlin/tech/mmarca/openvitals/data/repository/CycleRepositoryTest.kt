package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class CycleRepositoryTest {

    private val startDate = LocalDate.of(2026, 4, 1)
    private val endDate = LocalDate.of(2026, 4, 30)
    private val zone = ZoneId.systemDefault()
    private val startInstant = startDate.atStartOfDay(zone).toInstant()
    private val endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant()

    private val menstruationPermission = HealthPermission.getReadPermission(MenstruationFlowRecord::class)
    private val ovulationPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val mucusPermission = HealthPermission.getReadPermission(CervicalMucusRecord::class)
    private val bbtPermission = HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)
    private val allCyclePermissions = setOf(
        menstruationPermission,
        ovulationPermission,
        mucusPermission,
        bbtPermission,
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `missingPermissions returns phase 4 permissions not granted`() = runTest {
        val hc = hc(grantedPermissions = setOf(menstruationPermission, bbtPermission))
        every { hc.phase4Permissions } returns allCyclePermissions
        val repository = CycleRepositoryImpl(hc)

        assertEquals(
            setOf(ovulationPermission, mucusPermission),
            repository.missingPermissions(),
        )
    }

    @Test fun `missingPermissions returns all cycle permissions when Health Connect is unavailable`() = runTest {
        val hc = hc(
            availability = HealthConnectAvailability.NOT_SUPPORTED,
            grantedPermissions = allCyclePermissions,
        )
        every { hc.phase4Permissions } returns allCyclePermissions
        val repository = CycleRepositoryImpl(hc)

        assertEquals(allCyclePermissions, repository.missingPermissions())
        coVerify(exactly = 0) { hc.grantedPermissions() }
    }

    @Test fun `loadCycleData skips reads when no cycle permissions are granted`() = runTest {
        val hc = hc(grantedPermissions = emptySet())
        val repository = CycleRepositoryImpl(hc)

        val data = repository.loadCycleData(startDate, endDate)

        assertFalse(data.hasData)
        coVerify(exactly = 0) { hc.readMenstruationFlowEntries(any(), any()) }
        coVerify(exactly = 0) { hc.readMenstruationPeriods(any(), any()) }
        coVerify(exactly = 0) { hc.readOvulationTests(any(), any()) }
        coVerify(exactly = 0) { hc.readCervicalMucusEntries(any(), any()) }
        coVerify(exactly = 0) { hc.readBasalBodyTemperatureEntries(any(), any()) }
    }

    @Test fun `loadCycleData reads only granted cycle groups`() = runTest {
        val hc = hc(grantedPermissions = setOf(menstruationPermission, bbtPermission))
        val flow = MenstruationFlowEntry(Instant.parse("2026-04-05T08:00:00Z"), flow = 2, source = "test")
        val period = MenstruationPeriodEntry(
            startTime = Instant.parse("2026-04-04T00:00:00Z"),
            endTime = Instant.parse("2026-04-08T00:00:00Z"),
            source = "test",
        )
        val bbt = BasalBodyTemperatureEntry(
            time = Instant.parse("2026-04-06T06:00:00Z"),
            temperatureCelsius = 36.7,
            measurementLocation = 4,
            source = "test",
        )
        coEvery { hc.readMenstruationFlowEntries(startInstant, endInstant) } returns listOf(flow)
        coEvery { hc.readMenstruationPeriods(startInstant, endInstant) } returns listOf(period)
        coEvery { hc.readBasalBodyTemperatureEntries(startInstant, endInstant) } returns listOf(bbt)
        val repository = CycleRepositoryImpl(hc)

        val data = repository.loadCycleData(startDate, endDate)

        assertEquals(listOf(flow), data.menstruationFlows)
        assertEquals(listOf(period), data.menstruationPeriods)
        assertEquals(listOf(bbt), data.basalBodyTemperature)
        assertTrue(data.ovulationTests.isEmpty())
        assertTrue(data.cervicalMucus.isEmpty())
        coVerify(exactly = 0) { hc.readOvulationTests(any(), any()) }
        coVerify(exactly = 0) { hc.readCervicalMucusEntries(any(), any()) }
    }

    @Test fun `loadCycleData combines all granted cycle data`() = runTest {
        val hc = hc(grantedPermissions = allCyclePermissions)
        val flow = MenstruationFlowEntry(Instant.parse("2026-04-05T08:00:00Z"), flow = 3, source = "test")
        val period = MenstruationPeriodEntry(
            startTime = Instant.parse("2026-04-04T00:00:00Z"),
            endTime = Instant.parse("2026-04-08T00:00:00Z"),
            source = "test",
        )
        val ovulation = OvulationTestEntry(
            time = Instant.parse("2026-04-16T08:00:00Z"),
            result = 1,
            source = "test",
        )
        val mucus = CervicalMucusEntry(
            time = Instant.parse("2026-04-15T08:00:00Z"),
            appearance = 5,
            sensation = 2,
            source = "test",
        )
        val bbt = BasalBodyTemperatureEntry(
            time = Instant.parse("2026-04-06T06:00:00Z"),
            temperatureCelsius = 36.7,
            measurementLocation = 4,
            source = "test",
        )
        coEvery { hc.readMenstruationFlowEntries(startInstant, endInstant) } returns listOf(flow)
        coEvery { hc.readMenstruationPeriods(startInstant, endInstant) } returns listOf(period)
        coEvery { hc.readOvulationTests(startInstant, endInstant) } returns listOf(ovulation)
        coEvery { hc.readCervicalMucusEntries(startInstant, endInstant) } returns listOf(mucus)
        coEvery { hc.readBasalBodyTemperatureEntries(startInstant, endInstant) } returns listOf(bbt)
        val repository = CycleRepositoryImpl(hc)

        val data = repository.loadCycleData(startDate, endDate)

        assertTrue(data.hasData)
        assertEquals(listOf(flow), data.menstruationFlows)
        assertEquals(listOf(period), data.menstruationPeriods)
        assertEquals(listOf(ovulation), data.ovulationTests)
        assertEquals(listOf(mucus), data.cervicalMucus)
        assertEquals(listOf(bbt), data.basalBodyTemperature)
    }

    private fun hc(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String>,
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns availability
            coEvery { hc.grantedPermissions() } returns grantedPermissions
        }
}
