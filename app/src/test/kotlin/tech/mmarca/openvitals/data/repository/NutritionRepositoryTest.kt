package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class NutritionRepositoryTest {

    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val writeNutritionPermission = HealthPermission.getWritePermission(NutritionRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `DAY nutrition uses raw full entries for selected day metrics`() = runTest {
        val date = LocalDate.of(2026, 6, 1)
        val entries = listOf(
            NutritionEntry(
                time = Instant.parse("2026-06-01T08:15:00Z"),
                mealType = 0,
                name = "Breakfast",
                energyKcal = 320.0,
                proteinGrams = 20.0,
                carbsGrams = 38.0,
                fatGrams = 9.0,
                fiberGrams = null,
                sugarGrams = null,
                source = "test.source",
            ),
            NutritionEntry(
                time = Instant.parse("2026-06-01T12:30:00Z"),
                mealType = 0,
                name = "Lunch",
                energyKcal = 510.0,
                proteinGrams = 28.0,
                carbsGrams = 62.0,
                fatGrams = 18.0,
                fiberGrams = null,
                sugarGrams = null,
                source = "test.source",
            ),
        )
        val aggregate = listOf(
            DailyMacros(
                date = date,
                nutrientValues = mapOf(NutritionNutrient.ENERGY to 9_999.0),
            )
        )
        val hc = hc(entries = entries, dailyMacros = aggregate)

        val result = NutritionRepositoryImpl(hc).loadNutritionPeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = date)
        )

        assertEquals(entries, result.entries)
        assertEquals(1, result.dailyMacros.size)
        assertEquals(830.0, result.dailyMacros.single().energyKcal, 0.01)
        assertEquals(48.0, result.dailyMacros.single().proteinGrams, 0.01)
        assertEquals(100.0, result.dailyMacros.single().carbsGrams, 0.01)
        assertEquals(27.0, result.dailyMacros.single().fatGrams, 0.01)
        coVerify(exactly = 0) { hc.readDailyMacros(date, date) }
    }

    @Test
    fun `deleteNutritionEntry delegates when write permission exists`() = runTest {
        val hc = hc(
            entries = emptyList(),
            grantedPermissions = setOf(writeNutritionPermission),
        )
        coEvery { hc.deleteNutritionEntry("nutrition-id") } returns "nutrition-client-id"

        NutritionRepositoryImpl(hc).deleteNutritionEntry("nutrition-id")

        coVerify { hc.deleteNutritionEntry("nutrition-id") }
    }

    @Test
    fun `deleteNutritionEntry throws when write permission is missing`() = runTest {
        val hc = hc(
            entries = emptyList(),
            grantedPermissions = setOf(readNutritionPermission),
        )

        val error = runCatching {
            NutritionRepositoryImpl(hc).deleteNutritionEntry("nutrition-id")
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
        coVerify(exactly = 0) { hc.deleteNutritionEntry(any()) }
    }

    private fun hc(
        entries: List<NutritionEntry>,
        dailyMacros: List<DailyMacros> = emptyList(),
        grantedPermissions: Set<String> = setOf(readNutritionPermission),
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns grantedPermissions
            coEvery { hc.readNutritionEntries(any(), any()) } returns entries
            coEvery { hc.readDailyMacros(any(), any()) } returns dailyMacros
        }
}
