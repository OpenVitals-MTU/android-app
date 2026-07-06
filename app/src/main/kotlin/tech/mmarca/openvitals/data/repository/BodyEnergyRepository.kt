package tech.mmarca.openvitals.data.repository

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineInputs
import tech.mmarca.openvitals.domain.insights.calculateBodyEnergyTimeline
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode

@Singleton
class BodyEnergyRepositoryImpl @Inject constructor(
    private val heartRepository: HeartRepository,
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
    private val vitalsRepository: VitalsRepository,
    private val healthRepository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val cacheStore: BodyEnergyTimelineCacheStore,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : BodyEnergyRepository {

    override suspend fun loadTimeline(query: BodyEnergyTimelineQuery): BodyEnergyTimelineResult {
        val dates = query.period.dates()
        val days = dates.map { date ->
            loadDay(date, query.refreshMode)
        }
        return BodyEnergyTimelineResult(query = query, days = days)
    }

    private suspend fun loadDay(
        date: LocalDate,
        refreshMode: RefreshMode,
    ): BodyEnergyTimeline = coroutineScope {
        val calibration = preferencesRepository.bodyEnergyCalibration()
        val bodyProfile = preferencesRepository.bodyProfile()
        val permissionSignature = permissionSignature()
        val combinedSignature = "${calibration.signature()}|${bodyProfile.signature(date)}"
        val signature = timelineSignature(combinedSignature, permissionSignature)
        val cached = cacheStore.load(date, signature)
        if (cached != null && refreshMode == RefreshMode.NORMAL && !cached.isStale(date)) {
            return@coroutineScope cached
        }

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        val baselineStart = date.minusDays(BaselineDays)
        val baselineEnd = date.minusDays(1)
        val baselines = async {
            loadBaselines(
                date = date,
                baselineStart = baselineStart,
                baselineEnd = baselineEnd,
                dayStart = dayStart,
                signature = baselineSignature(permissionSignature),
            )
        }
        val heartRate = async { heartRepository.loadRawHeartRateSamplesForDayGraph(date) }
        val hrvSamples = async { heartRepository.loadHrvSamples(dayStart, dayEnd) }
        val sleep = async { sleepRepository.loadSleepSessions(date.minusDays(1), date) }
        val workouts = async { activityRepository.loadWorkouts(date, date) }
        val baselineValues = baselines.await()
        val respiratory = if (baselineValues.respiratoryRateBaseline != null) {
            async { vitalsRepository.loadRespiratoryRate(date, date) }
        } else {
            null
        }
        val resting = async { heartRepository.loadRestingHeartRate(date) }
        val previousEnd = cacheStore.load(date.minusDays(1), signature)?.currentScore
        val timeline = withContext(dispatchers.default) {
            calculateBodyEnergyTimeline(
                BodyEnergyTimelineInputs(
                    date = date,
                    heartRateSamples = heartRate.await(),
                    hrvSamples = hrvSamples.await(),
                    sleepSessions = sleep.await(),
                    workouts = workouts.await(),
                    respiratoryRateSamples = respiratory?.await().orEmpty(),
                    restingHeartRateBpm = resting.await(),
                    baselineRestingHeartRateBpm = baselineValues.baselineRestingHeartRateBpm,
                    observedMaxHeartRateBpm = baselineValues.observedMaxHeartRateBpm,
                    hrvBaselineRmssdMs = baselineValues.hrvBaselineRmssdMs,
                    respiratoryRateBaseline = baselineValues.respiratoryRateBaseline,
                    previousEndScore = previousEnd,
                    calibration = calibration,
                    bodyProfile = bodyProfile,
                    now = Instant.now(),
                    zone = zone,
                )
            ).copy(signature = signature)
        }
        cacheStore.save(timeline)
        timeline
    }

    private suspend fun loadBaselines(
        date: LocalDate,
        baselineStart: LocalDate,
        baselineEnd: LocalDate,
        dayStart: Instant,
        signature: String,
    ): BodyEnergyBaselineCacheEntry = coroutineScope {
        val cached = loadReusableBaseline(date, signature)
        if (cached != null && !cached.isStale()) {
            return@coroutineScope cached
        }

        val zone = ZoneId.systemDefault()
        val baselineStartInstant = baselineStart.atStartOfDay(zone).toInstant()
        val baselineResting = async {
            heartRepository.loadDailyRestingHR(baselineStart, baselineEnd)
                .map { it.bpm }
                .medianLongOrNull()
        }
        val observedMax = async {
            heartRepository.loadHeartRateSamples(baselineStartInstant, dayStart)
                .maxOfOrNull { it.beatsPerMinute }
        }
        val hrvBaseline = async {
            heartRepository.loadDailyHRV(baselineStart, baselineEnd)
                .map { it.rmssdMs }
                .medianDoubleOrNull()
        }
        val baseline = BodyEnergyBaselineCacheEntry(
            baselineRestingHeartRateBpm = baselineResting.await(),
            observedMaxHeartRateBpm = observedMax.await(),
            hrvBaselineRmssdMs = hrvBaseline.await(),
            respiratoryRateBaseline = cached?.respiratoryRateBaseline,
        )
        cacheStore.saveBaseline(date, signature, baseline)
        baseline
    }

    private fun loadReusableBaseline(
        date: LocalDate,
        signature: String,
    ): BodyEnergyBaselineCacheEntry? {
        val exact = cacheStore.loadBaseline(date, signature)
        if (exact != null && !exact.isStale()) return exact

        val adjacent = listOf(date.minusDays(1), date.plusDays(1))
            .firstNotNullOfOrNull { adjacentDate ->
                cacheStore.loadBaseline(adjacentDate, signature)
                    ?.takeUnless { it.isStale() }
            }
        if (adjacent != null) {
            cacheStore.saveBaseline(date, signature, adjacent)
        }
        return adjacent
    }

    private suspend fun permissionSignature(): Int =
        runCatching {
            if (healthRepository.availability() == HealthConnectAvailability.AVAILABLE) {
                healthRepository.grantedPermissions()
                    .sorted()
                    .joinToString(",")
                    .hashCode()
            } else {
                0
            }
        }.getOrDefault(0)

    private fun timelineSignature(calibrationSignature: String, permissionSignature: Int): String =
        listOf(
            "v$BodyEnergyTimelineAlgorithmVersion",
            calibrationSignature.hashCode(),
            permissionSignature,
        ).joinToString("|")

    private fun baselineSignature(permissionSignature: Int): String =
        listOf(
            "v$BodyEnergyTimelineAlgorithmVersion",
            "baseline",
            permissionSignature,
        ).joinToString("|")

    private fun BodyEnergyTimeline.isStale(date: LocalDate): Boolean {
        val age = Duration.between(generatedAt, Instant.now())
        return if (date == LocalDate.now()) {
            age.toMinutes() >= CurrentDayCacheMinutes
        } else {
            age.toHours() >= PastDayCacheHours
        }
    }

    private fun BodyEnergyBaselineCacheEntry.isStale(): Boolean {
        val age = Duration.between(generatedAt, Instant.now())
        return age.toHours() >= BaselineCacheHours
    }

    private companion object {
        const val BaselineDays = 28L
        const val CurrentDayCacheMinutes = 15L
        const val PastDayCacheHours = 24L
        const val BaselineCacheHours = 24L
    }
}

private fun tech.mmarca.openvitals.core.period.DatePeriod.dates(): List<LocalDate> {
    val days = ChronoUnit.DAYS.between(start, end).coerceAtLeast(0L).toInt() + 1
    return List(days) { start.plusDays(it.toLong()) }
}

private fun List<Long>.medianLongOrNull(): Long? {
    if (isEmpty()) return null
    val sorted = sorted()
    return sorted[sorted.lastIndex / 2]
}

private fun List<Double>.medianDoubleOrNull(): Double? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = sorted.lastIndex / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middle] + sorted[middle + 1]) / 2.0
    } else {
        sorted[middle]
    }
}
