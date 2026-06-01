package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class ActivityRepositoryTest {

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val distancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val historyPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    @Test
    fun `loadDailySteps reads steps when distance permission is missing`() = runTest {
        val start = LocalDate.of(2026, 5, 1)
        val end = LocalDate.of(2026, 5, 7)
        val dailySteps = listOf(DailySteps(date = end, steps = 4_000, distanceMeters = 0.0))
        val hc = hc(granted = setOf(stepsPermission), dailySteps = dailySteps)

        val result = ActivityRepository(hc).loadDailySteps(start, end)

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

        ActivityRepository(hc).loadDailySteps(start, end)

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

        ActivityRepository(hc).loadDailySteps(start, end)

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
