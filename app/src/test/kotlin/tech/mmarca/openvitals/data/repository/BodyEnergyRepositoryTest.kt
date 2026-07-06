package tech.mmarca.openvitals.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile

class BodyEnergyRepositoryTest {

    @Test
    fun `DAY body energy timeline uses raw full heart rate samples`() = runTest {
        val date = LocalDate.of(2026, 6, 1)
        val heartSamples = listOf(
            HeartRateSample(
                time = Instant.parse("2026-06-01T08:00:00Z"),
                beatsPerMinute = 68L,
                source = "test.source",
            )
        )
        val heartRepository = mockk<HeartRepository>()
        coEvery { heartRepository.loadRawHeartRateSamplesForDayGraph(date) } returns heartSamples
        coEvery { heartRepository.loadHeartRateSamples(any<Instant>(), any<Instant>()) } returns emptyList()
        coEvery { heartRepository.loadHrvSamples(any(), any()) } returns emptyList()
        coEvery { heartRepository.loadDailyRestingHR(any(), any()) } returns emptyList()
        coEvery { heartRepository.loadDailyHRV(any(), any()) } returns emptyList()
        coEvery { heartRepository.loadRestingHeartRate(date) } returns 60L

        val sleepRepository = mockk<SleepRepository>()
        coEvery { sleepRepository.loadSleepSessions(date.minusDays(1), date) } returns emptyList()

        val activityRepository = mockk<ActivityRepository>()
        coEvery { activityRepository.loadWorkouts(date, date) } returns emptyList()

        val vitalsRepository = mockk<VitalsRepository>()

        val healthRepository = mockk<HealthRepository>()
        every { healthRepository.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { healthRepository.grantedPermissions() } returns setOf("heart-rate")

        val preferencesRepository = mockk<PreferencesRepository>()
        every { preferencesRepository.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
        every { preferencesRepository.bodyProfile() } returns BodyProfile()

        val cacheStore = mockk<BodyEnergyTimelineCacheStore>()
        every { cacheStore.load(any(), any()) } returns null
        every { cacheStore.loadBaseline(any(), any()) } returns BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = 60L,
            observedMaxHeartRateBpm = 180L,
            hrvBaselineRmssdMs = null,
            respiratoryRateBaseline = null,
        )
        every { cacheStore.save(any()) } just runs
        every { cacheStore.saveBaseline(any(), any(), any()) } just runs

        BodyEnergyRepositoryImpl(
            heartRepository = heartRepository,
            sleepRepository = sleepRepository,
            activityRepository = activityRepository,
            vitalsRepository = vitalsRepository,
            healthRepository = healthRepository,
            preferencesRepository = preferencesRepository,
            cacheStore = cacheStore,
        ).loadTimeline(
            BodyEnergyTimelineQuery(
                period = DatePeriod(date, date),
                range = TimeRange.DAY,
            )
        )

        coVerify(exactly = 1) { heartRepository.loadRawHeartRateSamplesForDayGraph(date) }
        coVerify(exactly = 0) { heartRepository.loadHeartRateSamples(any<Instant>(), any<Instant>()) }
    }
}
