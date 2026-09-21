package tech.mmarca.openvitals.data.repository

import io.mockk.unmockkStatic
import io.mockk.mockkStatic
import android.util.Log
import tech.mmarca.openvitals.domain.model.HydrationWriteRequest
import tech.mmarca.openvitals.domain.model.HydrationEntryChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HydrationRepositoryTest {

    private val hydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val hydrationWritePermission = HealthPermission.getWritePermission(HydrationRecord::class)
    private val nutritionWritePermission = HealthPermission.getWritePermission(NutritionRecord::class)

    @Test
    fun `DAY hydration uses raw full entries for selected day total`() = runTest {
        val date = LocalDate.of(2026, 6, 1)
        val entries = listOf(
            HydrationEntry(
                startTime = Instant.parse("2026-06-01T07:45:00Z"),
                endTime = Instant.parse("2026-06-01T07:46:00Z"),
                liters = 0.35,
                source = "test.source",
            ),
            HydrationEntry(
                startTime = Instant.parse("2026-06-01T13:10:00Z"),
                endTime = Instant.parse("2026-06-01T13:11:00Z"),
                liters = 0.50,
                source = "test.source",
            ),
        )
        val aggregate = listOf(DailyHydration(date = date, liters = 9.99))
        val hc = hc(entries = entries, dailyHydration = aggregate)

        val result = HydrationRepositoryImpl(hc).loadHydrationPeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = date)
        )

        assertEquals(entries, result.hydrationEntries)
        assertEquals(listOf(DailyHydration(date = date, liters = 0.85)), result.dailyHydration)
        coVerify(exactly = 0) { hc.readDailyHydration(date, date) }
    }

    @Test
    fun `deleteHydrationEntry deletes paired nutrition record when nutrition write permission exists`() = runTest {
        val hc = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission, nutritionWritePermission)
            coEvery { hc.deleteHydrationEntry("hydration-id") } returns "hydration-client-id"
            coEvery { hc.deleteHydrationNutritionEntry("hydration-client-id") } returns Unit
        }

        HydrationRepositoryImpl(hc).deleteHydrationEntry("hydration-id")

        coVerify { hc.deleteHydrationEntry("hydration-id") }
        coVerify { hc.deleteHydrationNutritionEntry("hydration-client-id") }
    }

    @Test
    fun `deleteHydrationEntry skips paired nutrition cleanup without nutrition write permission`() = runTest {
        val hc = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission)
            coEvery { hc.deleteHydrationEntry("hydration-id") } returns "hydration-client-id"
        }

        HydrationRepositoryImpl(hc).deleteHydrationEntry("hydration-id")

        coVerify { hc.deleteHydrationEntry("hydration-id") }
        coVerify(exactly = 0) { hc.deleteHydrationNutritionEntry(any()) }
    }

    private val noon: Instant = Instant.parse("2026-03-10T12:00:00Z")
    private val request = HydrationWriteRequest(time = noon.plusSeconds(3_600), volumeLiters = 0.5)

    private fun change(clientRecordId: String?) = HydrationEntryChange(
        clientRecordId = clientRecordId,
        oldTime = noon,
        newTime = noon.plusSeconds(3_600),
        oldVolumeLiters = 0.25,
        newVolumeLiters = 0.5,
    )

    @Test
    fun `updateHydrationEntry moves the drink's nutrition record with it`() = runTest {
        val hc = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission, nutritionWritePermission)
            coEvery { hc.updateHydrationEntry("hydration-id", request) } returns change("hydration-client-id")
            coEvery { hc.updateHydrationNutritionEntry(any()) } returns Unit
        }

        HydrationRepositoryImpl(hc).updateHydrationEntry("hydration-id", request)

        coVerify { hc.updateHydrationNutritionEntry(change("hydration-client-id")) }
    }

    @Test
    fun `updateHydrationEntry leaves nutrition alone for plain water or without the permission`() = runTest {
        val plainWater = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission, nutritionWritePermission)
            coEvery { hc.updateHydrationEntry("hydration-id", request) } returns change(clientRecordId = null)
        }
        val noPermission = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission)
            coEvery { hc.updateHydrationEntry("hydration-id", request) } returns change("hydration-client-id")
        }

        HydrationRepositoryImpl(plainWater).updateHydrationEntry("hydration-id", request)
        HydrationRepositoryImpl(noPermission).updateHydrationEntry("hydration-id", request)

        coVerify(exactly = 0) { plainWater.updateHydrationNutritionEntry(any()) }
        coVerify(exactly = 0) { noPermission.updateHydrationNutritionEntry(any()) }
    }

    @Test
    fun `a nutrition record that cannot follow does not fail the hydration edit`() = runTest {
        val hc = mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationWritePermission, nutritionWritePermission)
            coEvery { hc.updateHydrationEntry("hydration-id", request) } returns change("hydration-client-id")
            coEvery { hc.updateHydrationNutritionEntry(any()) } throws IllegalStateException("rate limited")
        }
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0

        try {
            HydrationRepositoryImpl(hc).updateHydrationEntry("hydration-id", request)
        } finally {
            unmockkStatic(Log::class)
        }

        coVerify { hc.updateHydrationEntry("hydration-id", request) }
    }

    private fun hc(
        entries: List<HydrationEntry>,
        dailyHydration: List<DailyHydration> = emptyList(),
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationPermission)
            coEvery { hc.readHydrationEntries(any(), any()) } returns entries
            coEvery { hc.readDailyHydration(any(), any()) } returns dailyHydration
        }
}
