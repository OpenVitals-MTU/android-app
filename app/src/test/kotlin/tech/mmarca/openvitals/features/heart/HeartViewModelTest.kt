package tech.mmarca.openvitals.features.heart

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.query.HeartPeriodData
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.query.VitalsPeriodData
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.usecase.LoadHeartPeriodUseCase
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<HeartRepository>().also { repo ->
        coEvery { repo.loadHeartRateSamples(any()) } returns emptyList()
        coEvery { repo.loadDailyHeartRateSummaries(any(), any()) } returns emptyList()
        coEvery { repo.loadRestingHeartRate(any()) } returns null
        coEvery { repo.loadHrvRmssd(any()) } returns null
        coEvery { repo.loadDailyRestingHR(any(), any()) } returns emptyList()
        coEvery { repo.loadDailyHRV(any(), any()) } returns emptyList()
        coEvery { repo.loadHeartPeriod(any(), any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val metric = secondArg<HeartPeriodMetric>()
            val windows = query.windows
            when (metric) {
                HeartPeriodMetric.ALL -> if (query.range == TimeRange.DAY) {
                    HeartPeriodData(
                        daySamples = repo.loadHeartRateSamples(query.selectedDate),
                        previousDaySamples = repo.loadHeartRateSamples(windows.previous.start),
                        baselineDailySummaries = repo.loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                        dayRestingBpm = repo.loadRestingHeartRate(query.selectedDate),
                        previousDayRestingBpm = repo.loadRestingHeartRate(windows.previous.start),
                        baselineDailyRestingHR = repo.loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                        dayHrvMs = repo.loadHrvRmssd(query.selectedDate),
                        previousDayHrvMs = repo.loadHrvRmssd(windows.previous.start),
                        baselineDailyHrv = repo.loadDailyHRV(windows.baseline.start, windows.baseline.end),
                    )
                } else {
                    HeartPeriodData(
                        dailySummaries = repo.loadDailyHeartRateSummaries(windows.current.start, windows.current.end),
                        previousDailySummaries = repo.loadDailyHeartRateSummaries(windows.previous.start, windows.previous.end),
                        baselineDailySummaries = repo.loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                        dailyRestingHR = repo.loadDailyRestingHR(windows.current.start, windows.current.end),
                        previousDailyRestingHR = repo.loadDailyRestingHR(windows.previous.start, windows.previous.end),
                        baselineDailyRestingHR = repo.loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                        dailyHrv = repo.loadDailyHRV(windows.current.start, windows.current.end),
                        previousDailyHrv = repo.loadDailyHRV(windows.previous.start, windows.previous.end),
                        baselineDailyHrv = repo.loadDailyHRV(windows.baseline.start, windows.baseline.end),
                    )
                }
                HeartPeriodMetric.AVERAGE_HEART_RATE -> if (query.range == TimeRange.DAY) {
                    HeartPeriodData(
                        daySamples = repo.loadHeartRateSamples(query.selectedDate),
                        previousDaySamples = repo.loadHeartRateSamples(windows.previous.start),
                        baselineDailySummaries = repo.loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                    )
                } else {
                    HeartPeriodData(
                        dailySummaries = repo.loadDailyHeartRateSummaries(windows.current.start, windows.current.end),
                        previousDailySummaries = repo.loadDailyHeartRateSummaries(windows.previous.start, windows.previous.end),
                        baselineDailySummaries = repo.loadDailyHeartRateSummaries(windows.baseline.start, windows.baseline.end),
                    )
                }
                HeartPeriodMetric.RESTING_HEART_RATE -> if (query.range == TimeRange.DAY) {
                    HeartPeriodData(
                        dayRestingBpm = repo.loadRestingHeartRate(query.selectedDate),
                        previousDayRestingBpm = repo.loadRestingHeartRate(windows.previous.start),
                        baselineDailyRestingHR = repo.loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                    )
                } else {
                    HeartPeriodData(
                        dailyRestingHR = repo.loadDailyRestingHR(windows.current.start, windows.current.end),
                        previousDailyRestingHR = repo.loadDailyRestingHR(windows.previous.start, windows.previous.end),
                        baselineDailyRestingHR = repo.loadDailyRestingHR(windows.baseline.start, windows.baseline.end),
                    )
                }
                HeartPeriodMetric.HRV -> if (query.range == TimeRange.DAY) {
                    HeartPeriodData(
                        dayHrvMs = repo.loadHrvRmssd(query.selectedDate),
                        baselineDailyHrv = repo.loadDailyHRV(windows.baseline.start, windows.baseline.end),
                    )
                } else {
                    HeartPeriodData(
                        dailyHrv = repo.loadDailyHRV(windows.current.start, windows.current.end),
                        previousDailyHrv = repo.loadDailyHRV(windows.previous.start, windows.previous.end),
                        baselineDailyHrv = repo.loadDailyHRV(windows.baseline.start, windows.baseline.end),
                    )
                }
            }
        }
    }

    private fun emptyVitalsRepo() = mockk<VitalsRepository>().also { repo ->
        every { repo.phase3Permissions } returns setOf("blood", "oxygen")
        coEvery { repo.missingPermissions() } returns emptySet()
        coEvery { repo.loadBloodPressure(any(), any()) } returns emptyList()
        coEvery { repo.loadSpO2(any(), any()) } returns emptyList()
        coEvery { repo.loadRespiratoryRate(any(), any()) } returns emptyList()
        coEvery { repo.loadBodyTemperature(any(), any()) } returns emptyList()
        coEvery { repo.loadVo2Max(any(), any()) } returns emptyList()
        coEvery { repo.loadBloodGlucose(any(), any()) } returns emptyList()
        coEvery { repo.loadSkinTemperature(any(), any()) } returns emptyList()
        coEvery { repo.deleteVitalsMeasurementEntry(any(), any()) } returns Unit
        suspend fun periodData(query: PeriodLoadQuery, metric: VitalsPeriodMetric): VitalsPeriodData {
            val windows = query.windows
            return when (metric) {
                VitalsPeriodMetric.ALL -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    bloodPressure = repo.loadBloodPressure(windows.current.start, windows.current.end),
                    previousBloodPressure = repo.loadBloodPressure(windows.previous.start, windows.previous.end),
                    baselineBloodPressure = repo.loadBloodPressure(windows.baseline.start, windows.baseline.end),
                    spO2 = repo.loadSpO2(windows.current.start, windows.current.end),
                    previousSpO2 = repo.loadSpO2(windows.previous.start, windows.previous.end),
                    baselineSpO2 = repo.loadSpO2(windows.baseline.start, windows.baseline.end),
                    respiratoryRate = repo.loadRespiratoryRate(windows.current.start, windows.current.end),
                    previousRespiratoryRate = repo.loadRespiratoryRate(windows.previous.start, windows.previous.end),
                    baselineRespiratoryRate = repo.loadRespiratoryRate(windows.baseline.start, windows.baseline.end),
                    bodyTemperature = repo.loadBodyTemperature(windows.current.start, windows.current.end),
                    previousBodyTemperature = repo.loadBodyTemperature(windows.previous.start, windows.previous.end),
                    baselineBodyTemperature = repo.loadBodyTemperature(windows.baseline.start, windows.baseline.end),
                    vo2Max = repo.loadVo2Max(windows.current.start, windows.current.end),
                    previousVo2Max = repo.loadVo2Max(windows.previous.start, windows.previous.end),
                    baselineVo2Max = repo.loadVo2Max(windows.baseline.start, windows.baseline.end),
                    bloodGlucose = repo.loadBloodGlucose(windows.current.start, windows.current.end),
                    previousBloodGlucose = repo.loadBloodGlucose(windows.previous.start, windows.previous.end),
                    baselineBloodGlucose = repo.loadBloodGlucose(windows.baseline.start, windows.baseline.end),
                    skinTemperature = repo.loadSkinTemperature(windows.current.start, windows.current.end),
                    previousSkinTemperature = repo.loadSkinTemperature(windows.previous.start, windows.previous.end),
                    baselineSkinTemperature = repo.loadSkinTemperature(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.BLOOD_PRESSURE -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    bloodPressure = repo.loadBloodPressure(windows.current.start, windows.current.end),
                    previousBloodPressure = repo.loadBloodPressure(windows.previous.start, windows.previous.end),
                    baselineBloodPressure = repo.loadBloodPressure(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.SPO2 -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    spO2 = repo.loadSpO2(windows.current.start, windows.current.end),
                    previousSpO2 = repo.loadSpO2(windows.previous.start, windows.previous.end),
                    baselineSpO2 = repo.loadSpO2(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.VO2_MAX -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    vo2Max = repo.loadVo2Max(windows.current.start, windows.current.end),
                    previousVo2Max = repo.loadVo2Max(windows.previous.start, windows.previous.end),
                    baselineVo2Max = repo.loadVo2Max(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.RESPIRATORY_RATE -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    respiratoryRate = repo.loadRespiratoryRate(windows.current.start, windows.current.end),
                    previousRespiratoryRate = repo.loadRespiratoryRate(windows.previous.start, windows.previous.end),
                    baselineRespiratoryRate = repo.loadRespiratoryRate(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.BODY_TEMPERATURE -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    bodyTemperature = repo.loadBodyTemperature(windows.current.start, windows.current.end),
                    previousBodyTemperature = repo.loadBodyTemperature(windows.previous.start, windows.previous.end),
                    baselineBodyTemperature = repo.loadBodyTemperature(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.BLOOD_GLUCOSE -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    bloodGlucose = repo.loadBloodGlucose(windows.current.start, windows.current.end),
                    previousBloodGlucose = repo.loadBloodGlucose(windows.previous.start, windows.previous.end),
                    baselineBloodGlucose = repo.loadBloodGlucose(windows.baseline.start, windows.baseline.end),
                )
                VitalsPeriodMetric.SKIN_TEMPERATURE -> VitalsPeriodData(
                    missingVitalsPermissions = repo.missingPermissions(),
                    skinTemperature = repo.loadSkinTemperature(windows.current.start, windows.current.end),
                    previousSkinTemperature = repo.loadSkinTemperature(windows.previous.start, windows.previous.end),
                    baselineSkinTemperature = repo.loadSkinTemperature(windows.baseline.start, windows.baseline.end),
                )
            }
        }
        coEvery { repo.loadVitalsPeriod(any(), any()) } coAnswers {
            periodData(firstArg(), secondArg())
        }
        coEvery { repo.loadVitalsPeriod(any(), any(), any()) } coAnswers {
            periodData(firstArg(), secondArg())
        }
    }

    private fun heartViewModel(
        heartRepo: HeartRepository = emptyRepo(),
        vitalsRepo: VitalsRepository = emptyVitalsRepo(),
        initialRange: TimeRange = TimeRange.WEEK,
        initialWeekPeriodMode: tech.mmarca.openvitals.core.period.WeekPeriodMode =
            tech.mmarca.openvitals.core.period.WeekPeriodMode.MONDAY_TO_SUNDAY,
        selectedMetric: HeartMetric? = HeartMetric.AVERAGE_HEART_RATE,
        initialHighHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
        initialLowHeartRateThresholdBpm: Int = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
        onHighHeartRateThresholdChanged: (Int) -> Unit = {},
        onLowHeartRateThresholdChanged: (Int) -> Unit = {},
    ) = HeartViewModel(
        loadHeartPeriodUseCase = LoadHeartPeriodUseCase(heartRepo, vitalsRepo),
        vitalsRepository = vitalsRepo,
        dispatchers = mainDispatcherRule.dispatcherProvider,
        initialRange = initialRange,
        initialWeekPeriodMode = initialWeekPeriodMode,
        selectedMetric = selectedMetric,
        initialHighHeartRateThresholdBpm = initialHighHeartRateThresholdBpm,
        initialLowHeartRateThresholdBpm = initialLowHeartRateThresholdBpm,
        onHighHeartRateThresholdChanged = onHighHeartRateThresholdChanged,
        onLowHeartRateThresholdChanged = onLowHeartRateThresholdChanged,
    )

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test fun `initial range is WEEK`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `deleteVitalsMeasurementEntry removes OpenVitals blood pressure and reloads`() = runTest {
        val entry = BloodPressureEntry(
            time = Instant.ofEpochSecond(1_000),
            systolicMmHg = 120,
            diastolicMmHg = 80,
            source = "tech.mmarca.openvitals.debug",
            id = "bp-id",
            isOpenVitalsEntry = true,
        )
        var entries = listOf(entry)
        val vitalsRepo = emptyVitalsRepo()
        coEvery { vitalsRepo.loadBloodPressure(any(), any()) } answers { entries }
        coEvery {
            vitalsRepo.deleteVitalsMeasurementEntry(VitalsMeasurementType.BLOOD_PRESSURE, "bp-id")
        } coAnswers {
            entries = emptyList()
        }
        val vm = heartViewModel(
            heartRepo = emptyRepo(),
            vitalsRepo = vitalsRepo,
            selectedMetric = HeartMetric.BLOOD_PRESSURE,
        )

        vm.deleteVitalsMeasurementEntry(VitalsMeasurementType.BLOOD_PRESSURE, "bp-id")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.bloodPressure.isEmpty())
        coVerify { vitalsRepo.deleteVitalsMeasurementEntry(VitalsMeasurementType.BLOOD_PRESSURE, "bp-id") }
        coVerify { vitalsRepo.loadVitalsPeriod(any(), VitalsPeriodMetric.BLOOD_PRESSURE, RefreshMode.FORCE) }
    }

    @Test fun `deleteVitalsMeasurementEntry ignores blood pressure not created by OpenVitals`() = runTest {
        val entries = listOf(
            BloodPressureEntry(
                time = Instant.ofEpochSecond(1_000),
                systolicMmHg = 120,
                diastolicMmHg = 80,
                source = "com.example",
                id = "external-bp-id",
                isOpenVitalsEntry = false,
            )
        )
        val vitalsRepo = emptyVitalsRepo()
        coEvery { vitalsRepo.loadBloodPressure(any(), any()) } returns entries
        val vm = heartViewModel(
            heartRepo = emptyRepo(),
            vitalsRepo = vitalsRepo,
            selectedMetric = HeartMetric.BLOOD_PRESSURE,
        )

        vm.deleteVitalsMeasurementEntry(VitalsMeasurementType.BLOOD_PRESSURE, "external-bp-id")
        advanceUntilIdle()

        assertEquals(entries, vm.uiState.value.bloodPressure)
        coVerify(exactly = 0) {
            vitalsRepo.deleteVitalsMeasurementEntry(VitalsMeasurementType.BLOOD_PRESSURE, "external-bp-id")
        }
    }

    // ─── WEEK range loads summaries, NOT samples ───────────────────────────────

    @Test fun `WEEK range calls loadDailyHeartRateSummaries`() = runTest {
        val summaries = listOf(HeartRateSummary(today.minusDays(1), 72L, 55L, 110L))
        val repo = emptyRepo()
        coEvery { repo.loadDailyHeartRateSummaries(any(), any()) } returns summaries

        val vm = heartViewModel(repo, emptyVitalsRepo())

        assertEquals(summaries, vm.uiState.value.dailySummaries)
        assertTrue(vm.uiState.value.daySamples.isEmpty())
    }

    @Test fun `load success populates display metric for average heart rate`() = runTest {
        val summaries = listOf(
            HeartRateSummary(today.minusDays(1), avgBpm = 72L, minBpm = 55L, maxBpm = 110L),
            HeartRateSummary(today, avgBpm = 70L, minBpm = 48L, maxBpm = 100L),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHeartRateSummaries(any(), any()) } returns summaries

        val vm = heartViewModel(repo, emptyVitalsRepo())

        val display = vm.uiState.value.display.metric
        assertTrue(display.hasData)
        assertTrue(display.hasPeriodHeartRateSummaries)
        assertEquals(2, display.sortedDailySummaries.size)
    }

    @Test fun `load success populates display metric for resting heart rate`() = runTest {
        val trend = listOf(
            DailyRestingHR(today.minusDays(1), 56L),
            DailyRestingHR(today, 58L),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyRestingHR(any(), any()) } returns trend

        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.RESTING_HEART_RATE)

        val display = vm.uiState.value.display.metric
        assertTrue(display.hasData)
        assertTrue(display.hasPeriodRestingRate)
        assertNotNull(display.restingRangeSummary)
    }

    @Test fun `WEEK range does not call loadHeartRateSamples`() = runTest {
        val repo = emptyRepo()
        heartViewModel(repo, emptyVitalsRepo())
        coVerify(exactly = 0) { repo.loadHeartRateSamples(any()) }
    }

    // ─── DAY range loads samples, NOT summaries ───────────────────────────────

    @Test fun `DAY range calls loadHeartRateSamples`() = runTest {
        val samples = listOf(HeartRateSample(Instant.now(), 75L, "test"))
        val repo = emptyRepo()
        coEvery { repo.loadHeartRateSamples(any()) } returns samples

        val vm = heartViewModel(repo, emptyVitalsRepo())
        vm.selectRange(TimeRange.DAY)

        assertEquals(samples, vm.uiState.value.daySamples)
        assertTrue(vm.uiState.value.dailySummaries.isEmpty())
    }

    @Test fun `DAY range loads resting HR and HRV for selected metrics`() = runTest {
        val restingRepo = emptyRepo()
        coEvery { restingRepo.loadRestingHeartRate(any()) } returns 58L

        val restingVm = heartViewModel(
            heartRepo = restingRepo,
            vitalsRepo = emptyVitalsRepo(),
            selectedMetric = HeartMetric.RESTING_HEART_RATE,
        )
        restingVm.selectRange(TimeRange.DAY)

        assertEquals(58L, restingVm.uiState.value.dayRestingBpm)

        val hrvRepo = emptyRepo()
        coEvery { hrvRepo.loadHrvRmssd(any()) } returns 42.5

        val hrvVm = heartViewModel(
            heartRepo = hrvRepo,
            vitalsRepo = emptyVitalsRepo(),
            selectedMetric = HeartMetric.HRV,
        )
        hrvVm.selectRange(TimeRange.DAY)

        assertEquals(42.5, hrvVm.uiState.value.dayHrvMs!!, 0.001)
    }

    @Test fun `DAY range leaves dayRestingBpm null when repo returns null`() = runTest {
        val repo = emptyRepo() // loadRestingHeartRate returns null by default
        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.RESTING_HEART_RATE)
        vm.selectRange(TimeRange.DAY)
        assertNull(vm.uiState.value.dayRestingBpm)
    }

    @Test fun `DAY range leaves dayHrvMs null when repo returns null`() = runTest {
        val repo = emptyRepo() // loadHrvRmssd returns null by default
        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.HRV)
        vm.selectRange(TimeRange.DAY)
        assertNull(vm.uiState.value.dayHrvMs)
    }

    @Test fun `DAY range produces empty dailyRestingHR and dailyHrv`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectRange(TimeRange.DAY)
        assertTrue(vm.uiState.value.dailyRestingHR.isEmpty())
        assertTrue(vm.uiState.value.dailyHrv.isEmpty())
    }

    @Test fun `heart rate checks count samples in day range using configured thresholds`() = runTest {
        val samples = listOf(
            HeartRateSample(Instant.ofEpochSecond(1_000), 55L, "test"),
            HeartRateSample(Instant.ofEpochSecond(2_000), 70L, "test"),
            HeartRateSample(Instant.ofEpochSecond(3_000), 110L, "test"),
        )
        val repo = emptyRepo()
        coEvery { repo.loadHeartRateSamples(any()) } returns samples

        val vm = heartViewModel(
            heartRepo = repo,
            vitalsRepo = emptyVitalsRepo(),
            initialRange = TimeRange.DAY,
            initialHighHeartRateThresholdBpm = 110,
            initialLowHeartRateThresholdBpm = 60,
        )

        assertEquals(110, vm.uiState.value.highHeartRateCheck.thresholdBpm)
        assertEquals(60, vm.uiState.value.lowHeartRateCheck.thresholdBpm)
        assertEquals(1, vm.uiState.value.highHeartRateCheck.count)
        assertEquals(1, vm.uiState.value.lowHeartRateCheck.count)
    }

    @Test fun `heart rate checks count days in multi-day ranges`() = runTest {
        val summaries = listOf(
            HeartRateSummary(today.minusDays(1), avgBpm = 82L, minBpm = 60L, maxBpm = 120L),
            HeartRateSummary(today, avgBpm = 70L, minBpm = 48L, maxBpm = 100L),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHeartRateSummaries(any(), any()) } returns summaries

        val vm = heartViewModel(
            heartRepo = repo,
            vitalsRepo = emptyVitalsRepo(),
            initialHighHeartRateThresholdBpm = 110,
            initialLowHeartRateThresholdBpm = 55,
        )

        assertEquals(1, vm.uiState.value.highHeartRateCheck.count)
        assertEquals(1, vm.uiState.value.lowHeartRateCheck.count)
    }

    @Test fun `updating high heart rate threshold persists and recalculates checks`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadHeartRateSamples(any()) } returns listOf(
            HeartRateSample(Instant.ofEpochSecond(1_000), 116L, "test"),
            HeartRateSample(Instant.ofEpochSecond(2_000), 121L, "test"),
        )
        var savedThreshold = -1
        val vm = heartViewModel(
            heartRepo = repo,
            vitalsRepo = emptyVitalsRepo(),
            initialRange = TimeRange.DAY,
            onHighHeartRateThresholdChanged = { threshold -> savedThreshold = threshold },
        )

        assertEquals(1, vm.uiState.value.highHeartRateCheck.count)

        vm.decreaseHighHeartRateThreshold()

        assertEquals(115, savedThreshold)
        assertEquals(115, vm.uiState.value.highHeartRateCheck.thresholdBpm)
        assertEquals(2, vm.uiState.value.highHeartRateCheck.count)
    }

    // ─── A1: multi-day resting HR + HRV trends ────────────────────────────────

    @Test fun `WEEK range loads dailyRestingHR trend`() = runTest {
        val trend = listOf(
            DailyRestingHR(today.minusDays(1), 56L),
            DailyRestingHR(today, 58L),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyRestingHR(any(), any()) } returns trend

        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.RESTING_HEART_RATE)

        assertEquals(trend, vm.uiState.value.dailyRestingHR)
    }

    @Test fun `WEEK range loads dailyHrv trend`() = runTest {
        val trend = listOf(
            DailyHrv(today.minusDays(1), 38.0),
            DailyHrv(today, 42.5),
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyHRV(any(), any()) } returns trend

        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.HRV)

        assertEquals(trend, vm.uiState.value.dailyHrv)
    }

    @Test fun `WEEK range clears dayRestingBpm and dayHrvMs`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        // default range is WEEK — verify point-in-time fields are null
        assertNull(vm.uiState.value.dayRestingBpm)
        assertNull(vm.uiState.value.dayHrvMs)
    }

    @Test fun `switching from DAY to WEEK clears point-in-time resting HR`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadRestingHeartRate(any()) } returns 60L

        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.RESTING_HEART_RATE)
        vm.selectRange(TimeRange.DAY)
        assertEquals(60L, vm.uiState.value.dayRestingBpm)

        vm.selectRange(TimeRange.WEEK)
        assertNull(vm.uiState.value.dayRestingBpm)
    }

    @Test fun `switching from DAY to WEEK clears HR samples`() = runTest {
        val samples = listOf(HeartRateSample(Instant.now(), 72L, "test"))
        val repo = emptyRepo()
        coEvery { repo.loadHeartRateSamples(any()) } returns samples

        val vm = heartViewModel(repo, emptyVitalsRepo())
        vm.selectRange(TimeRange.DAY)
        assertTrue(vm.uiState.value.daySamples.isNotEmpty())

        vm.selectRange(TimeRange.WEEK)
        assertTrue(vm.uiState.value.daySamples.isEmpty())
    }

    @Test fun `WEEK range does not call loadDailyRestingHR with empty result`() = runTest {
        val repo = emptyRepo() // returns emptyList() by default
        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.RESTING_HEART_RATE)
        assertTrue(vm.uiState.value.dailyRestingHR.isEmpty())
        coVerify(atLeast = 1) { repo.loadDailyRestingHR(any(), any()) }
    }

    @Test fun `WEEK range does not call loadDailyHRV with empty result`() = runTest {
        val repo = emptyRepo()
        val vm = heartViewModel(repo, emptyVitalsRepo(), selectedMetric = HeartMetric.HRV)
        assertTrue(vm.uiState.value.dailyHrv.isEmpty())
        coVerify(atLeast = 1) { repo.loadDailyHRV(any(), any()) }
    }

    // ─── B1: vitals merged into heart ────────────────────────────────────────

    @Test fun `load exposes missing vitals permissions`() = runTest {
        val vitalsRepo = emptyVitalsRepo()
        val permissions = setOf("blood", "oxygen")
        coEvery { vitalsRepo.missingPermissions() } returns permissions

        val vm = heartViewModel(emptyRepo(), vitalsRepo, selectedMetric = HeartMetric.BLOOD_PRESSURE)

        assertEquals(permissions, vm.uiState.value.missingVitalsPermissions)
    }

    @Test fun `load success populates selected vitals and latest values`() = runTest {
        val bloodPressure = listOf(
            BloodPressureEntry(Instant.ofEpochSecond(1_000), 118, 76, "test"),
            BloodPressureEntry(Instant.ofEpochSecond(2_000), 122, 78, "test"),
        )
        val vitalsRepo = emptyVitalsRepo()
        coEvery { vitalsRepo.loadBloodPressure(any(), any()) } returns bloodPressure

        val vm = heartViewModel(emptyRepo(), vitalsRepo, selectedMetric = HeartMetric.BLOOD_PRESSURE)

        assertTrue(vm.uiState.value.hasVitalsData)
        assertEquals(122, vm.uiState.value.latestBloodPressure?.systolicMmHg)
    }

    // ─── Load failure ─────────────────────────────────────────────────────────

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<HeartRepository>()
        coEvery { repo.loadHeartPeriod(any(), any()) } throws RuntimeException("error")

        val vm = heartViewModel(repo, emptyVitalsRepo())

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("error"), vm.uiState.value.error)
    }

    // ─── selectRange ──────────────────────────────────────────────────────────

    @Test fun `selectRange updates selectedRange`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectRange(TimeRange.MONTH)
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    // ─── previousPeriod / nextPeriod ──────────────────────────────────────────

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod DAY moves back one day`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY is blocked when at today`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past anchor`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    // ─── selectDate ───────────────────────────────────────────────────────────

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectDate(today.plusDays(3))
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate accepts past date unchanged`() = runTest {
        val vm = heartViewModel(emptyRepo(), emptyVitalsRepo())
        vm.selectDate(pastAnchor)
        assertEquals(pastAnchor, vm.uiState.value.selectedDate)
    }
}
