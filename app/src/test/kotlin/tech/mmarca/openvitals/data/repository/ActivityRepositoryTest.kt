package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class ActivityRepositoryTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val totalCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val activeCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val bmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val historyPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    @Test
    fun `loadDailySteps reads steps when distance permission is missing`() = runTest {
        val start = LocalDate.of(2026, 5, 1)
        val end = LocalDate.of(2026, 5, 7)
        val dailySteps = listOf(DailySteps(date = end, steps = 4_000, distanceMeters = 0.0))
        val hc = hc(granted = setOf(stepsPermission), dailySteps = dailySteps)

        val result = ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        assertEquals(dailySteps, result)
        coVerify {
            hc.readDailySteps(
                startDate = start,
                endDate = end,
                includeDistance = false,
                includeFloors = false,
                includeActiveCalories = false,
                includeElevation = false,
            )
        }
    }

    @Test
    fun `loadDailySteps clamps to recent days when history permission is required but missing`() = runTest {
        val start = LocalDate.of(2009, 1, 1)
        val end = LocalDate.of(2026, 6, 1)
        val hc = hc(
            granted = setOf(stepsPermission, distancePermission),
            additionalDataAccessPermissions = setOf(historyPermission),
        )

        ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        coVerify {
            hc.readDailySteps(
                startDate = end.minusDays(29),
                endDate = end,
                includeDistance = true,
                includeFloors = false,
                includeActiveCalories = false,
                includeElevation = false,
            )
        }
    }

    @Test
    fun `loadDailySteps keeps full range when history permission is granted`() = runTest {
        val start = LocalDate.of(2009, 1, 1)
        val end = LocalDate.of(2026, 6, 1)
        val hc = hc(
            granted = setOf(stepsPermission, distancePermission, historyPermission),
            additionalDataAccessPermissions = setOf(historyPermission),
        )

        ActivityRepositoryImpl(hc).loadDailySteps(start, end)

        coVerify {
            hc.readDailySteps(
                startDate = start,
                endDate = end,
                includeDistance = true,
                includeFloors = false,
                includeActiveCalories = false,
                includeElevation = false,
            )
        }
    }

    @Test
    fun `loadDailyNutrition reads plain Health Connect total calories by default`() = runTest {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 7)
        val nutrition = listOf(DailyNutrition(date = start, hydrationLiters = 0.0, caloriesBurnedKcal = 123.0))
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery {
            hc.readDailyNutrition(
                startDate = start,
                endDate = end,
                includeHydration = false,
                includeEstimatedCalories = false,
            )
        } returns nutrition

        val result = ActivityRepositoryImpl(hc).loadDailyNutrition(start, end)

        assertEquals(nutrition, result)
        coVerify(exactly = 0) {
            hc.readDailyNutrition(
                startDate = start,
                endDate = end,
                includeHydration = false,
                includeEstimatedCalories = true,
            )
        }
    }

    @Test
    fun `loadDailyNutrition enables OpenVitals calorie calculations when preference is on`() = runTest {
        val start = LocalDate.of(2026, 6, 1)
        val end = LocalDate.of(2026, 6, 7)
        val nutrition = listOf(DailyNutrition(date = start, hydrationLiters = 0.0, caloriesBurnedKcal = 456.0))
        val hc = mockk<HealthConnectManager>()
        val prefs = mockk<PreferencesRepository>()
        every { prefs.showOpenVitalsCalculatedCalories } returns true
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(
            totalCaloriesPermission,
            activeCaloriesPermission,
            bmrPermission,
        )
        coEvery {
            hc.readDailyNutrition(
                startDate = start,
                endDate = end,
                includeHydration = false,
                includeEstimatedCalories = true,
            )
        } returns nutrition

        val result = ActivityRepositoryImpl(
            hc = hc,
            preferencesRepository = prefs,
        ).loadDailyNutrition(start, end)

        assertEquals(nutrition, result)
    }

    private fun hc(
        granted: Set<String>,
        dailySteps: List<DailySteps> = emptyList(),
        additionalDataAccessPermissions: Set<String> = emptySet(),
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            every { hc.additionalDataAccessPermissions } returns additionalDataAccessPermissions
            coEvery { hc.grantedPermissions() } returns granted
            coEvery {
                hc.readDailySteps(
                    startDate = any(),
                    endDate = any(),
                    includeDistance = any(),
                    includeFloors = any(),
                    includeActiveCalories = any(),
                    includeElevation = any(),
                )
            } returns dailySteps
        }
}
