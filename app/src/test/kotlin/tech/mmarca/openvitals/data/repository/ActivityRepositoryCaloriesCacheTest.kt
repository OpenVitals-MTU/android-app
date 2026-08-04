package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.data.sync.VitalsCacheKeys
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class ActivityRepositoryCaloriesCacheTest {

    private val today = LocalDate.now()
    private val start = today.minusDays(2)
    private val caloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val activeCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val bmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun hc(granted: Set<String> = setOf(caloriesPermission)): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        coEvery { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns granted
        coEvery { hc.readDailyNutrition(any(), any(), any(), any(), any()) } returns emptyList()
        return hc
    }

    private fun dao(cursorToken: String?, rows: List<VitalsDailyAggregateEntity> = emptyList()): VitalsDailyCacheDao {
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(VitalsCacheKeys.CALORIES_BURNED) } answers {
            cursorToken?.let { VitalsSyncCursorEntity(VitalsCacheKeys.CALORIES_BURNED, it, null) }
        }
        coEvery { dao.aggregatesBetween(VitalsCacheKeys.CALORIES_BURNED, any(), any()) } returns rows
        return dao
    }

    @Test fun `cache hit serves the range zero-filled without touching Health Connect`() = runTest {
        val hc = hc()
        val dao = dao(
            cursorToken = "token",
            rows = listOf(
                VitalsDailyAggregateEntity(
                    metric = VitalsCacheKeys.CALORIES_BURNED,
                    epochDay = today.minusDays(1).toEpochDay(),
                    valueSum = 2050.0,
                    secondarySum = null,
                    sampleCount = 1,
                ),
            ),
        )
        val repository = ActivityRepositoryImpl(hc, cacheDao = dao)

        val result = repository.loadDailyNutrition(start, today)

        assertEquals(3, result.size)
        assertEquals(0.0, result[0].caloriesBurnedKcal, 0.0001)
        assertEquals(2050.0, result[1].caloriesBurnedKcal, 0.0001)
        assertEquals(0.0, result[2].caloriesBurnedKcal, 0.0001)
        coVerify(exactly = 0) { hc.readDailyNutrition(any(), any(), any(), any(), any()) }
    }

    @Test fun `no cursor means the cache is not trusted and the live read runs`() = runTest {
        val hc = hc()
        val repository = ActivityRepositoryImpl(hc, cacheDao = dao(cursorToken = null))

        repository.loadDailyNutrition(start, today)

        coVerify { hc.readDailyNutrition(start, today, false, any(), false) }
    }

    @Test fun `calculated-calories mode bypasses the cache it cannot be served from`() = runTest {
        val hc = hc(granted = setOf(caloriesPermission, activeCaloriesPermission, bmrPermission))
        val preferences = mockk<PreferencesRepository>()
        every { preferences.showOpenVitalsCalculatedCalories } returns true
        val dao = dao(cursorToken = "token")
        val repository = ActivityRepositoryImpl(hc, preferencesRepository = preferences, cacheDao = dao)

        repository.loadDailyNutrition(start, today)

        coVerify { hc.readDailyNutrition(start, today, false, any(), true) }
        coVerify(exactly = 0) { dao.aggregatesBetween(any(), any(), any()) }
    }

    @Test fun `ranges predating the lookback window fall through to the live read`() = runTest {
        val hc = hc()
        val repository = ActivityRepositoryImpl(hc, cacheDao = dao(cursorToken = "token"))
        val ancient = today.minusDays(1000)

        repository.loadDailyNutrition(ancient, ancient.plusDays(10))

        coVerify { hc.readDailyNutrition(ancient, ancient.plusDays(10), false, any(), false) }
    }
}
