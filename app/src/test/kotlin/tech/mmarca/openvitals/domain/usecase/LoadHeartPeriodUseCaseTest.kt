package tech.mmarca.openvitals.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.HeartPeriodData
import tech.mmarca.openvitals.domain.query.VitalsPeriodData
import java.time.LocalDate

class LoadHeartPeriodUseCaseTest {

    private val heartRepository: HeartRepository = mockk()
    private val vitalsRepository: VitalsRepository = mockk()
    private val useCase = LoadHeartPeriodUseCase(heartRepository, vitalsRepository)

    private val query = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = LocalDate.of(2026, 6, 1),
        weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
    )

    @Test fun `heart only request loads heart repository`() = runTest {
        coEvery { heartRepository.loadHeartPeriod(query, HeartPeriodMetric.AVERAGE_HEART_RATE) }
            .returns(HeartPeriodData(dailySummaries = emptyList()))

        val result = useCase(
            query = query,
            request = HeartPeriodLoadRequest.HeartOnly(HeartPeriodMetric.AVERAGE_HEART_RATE),
        )

        coVerify { heartRepository.loadHeartPeriod(query, HeartPeriodMetric.AVERAGE_HEART_RATE) }
        assertTrue(result.dailySummaries.isEmpty())
    }

    @Test fun `vitals only request loads vitals repository`() = runTest {
        coEvery { vitalsRepository.loadVitalsPeriod(query, VitalsPeriodMetric.BLOOD_PRESSURE) }
            .returns(VitalsPeriodData(bloodPressure = emptyList()))

        val result = useCase(
            query = query,
            request = HeartPeriodLoadRequest.VitalsOnly(VitalsPeriodMetric.BLOOD_PRESSURE),
        )

        coVerify { vitalsRepository.loadVitalsPeriod(query, VitalsPeriodMetric.BLOOD_PRESSURE) }
        assertTrue(result.bloodPressure.isEmpty())
    }

    @Test fun `combined request merges heart and vitals`() = runTest {
        coEvery { heartRepository.loadHeartPeriod(query, HeartPeriodMetric.ALL) }
            .returns(HeartPeriodData(dayRestingBpm = 60L))
        coEvery { vitalsRepository.loadVitalsPeriod(query, VitalsPeriodMetric.ALL) }
            .returns(VitalsPeriodData(missingVitalsPermissions = setOf("perm")))

        val result = useCase(query = query, request = HeartPeriodLoadRequest.Combined)

        assertEquals(60L, result.dayRestingBpm)
        assertEquals(setOf("perm"), result.missingVitalsPermissions)
    }

    @Test fun `force refresh passes refresh mode to repositories`() = runTest {
        coEvery {
            heartRepository.loadHeartPeriod(query, HeartPeriodMetric.HRV, RefreshMode.FORCE)
        }.returns(HeartPeriodData(dayHrvMs = 42.0))

        val result = useCase(
            query = query,
            request = HeartPeriodLoadRequest.HeartOnly(HeartPeriodMetric.HRV),
            refreshMode = RefreshMode.FORCE,
        )

        coVerify { heartRepository.loadHeartPeriod(query, HeartPeriodMetric.HRV, RefreshMode.FORCE) }
        assertEquals(42.0, result.dayHrvMs)
    }
}
