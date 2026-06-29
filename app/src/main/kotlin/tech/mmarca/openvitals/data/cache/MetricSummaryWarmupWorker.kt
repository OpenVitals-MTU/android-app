package tech.mmarca.openvitals.data.cache

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.AppForegroundGate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.HeartPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.VitalsPeriodMetric
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode

class MetricSummaryWarmupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                MetricSummaryWarmupEntryPoint::class.java,
            )
            val dashboardDataLoader = entryPoint.dashboardDataLoader()
            val prefs = entryPoint.preferencesRepository()
            val foregroundGate = entryPoint.appForegroundGate()
            val today = LocalDate.now()
            val baseQuery = DashboardQuery(
                date = today,
                sleepRangeMode = prefs.sleepRangeMode,
                activityWeekMode = prefs.activityWeekMode,
                refreshMode = RefreshMode.NORMAL,
            )

            if (!foregroundGate.isForeground) {
                dashboardDataLoader.loadDashboard(baseQuery.copy(visibleMetrics = WarmupReadinessMetrics))
                warmPeriodSummaries(entryPoint, prefs, today)
            }

            val ninetyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
            entryPoint.metricSummaryCacheStore().prune(ninetyDaysAgo)
            entryPoint.derivedMetricStore().prune(ninetyDaysAgo)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        private const val WorkName = "metric-summary-warmup"
        private const val PeriodicWorkName = "metric-summary-warmup-periodic"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MetricSummaryWarmupWorker>()
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                WorkName,
                ExistingWorkPolicy.KEEP,
                request,
            )
            val periodicRequest = PeriodicWorkRequestBuilder<MetricSummaryWarmupWorker>(
                1,
                TimeUnit.HOURS,
            ).build()
            workManager.enqueueUniquePeriodicWork(
                PeriodicWorkName,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest,
            )
        }
    }
}

private suspend fun warmPeriodSummaries(
    entryPoint: MetricSummaryWarmupEntryPoint,
    prefs: PreferencesRepository,
    today: LocalDate,
) {
    val dayQuery = PeriodLoadQuery(
        range = TimeRange.DAY,
        anchorDate = today,
        weekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode(),
    )
    val weekQuery = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = today,
        weekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode(),
    )
    val monthQuery = PeriodLoadQuery(
        range = TimeRange.MONTH,
        anchorDate = today,
        weekPeriodMode = prefs.activityWeekMode.toWeekPeriodMode(),
    )

    entryPoint.activityRepository().loadActivityPeriod(weekQuery, includeSteps = true, includeNutrition = false)
    entryPoint.sleepRepository().loadSleepPeriod(weekQuery, prefs.sleepRangeMode)
    entryPoint.heartRepository().loadHeartPeriod(dayQuery, HeartPeriodMetric.ALL)
    entryPoint.hydrationRepository().loadHydrationPeriod(weekQuery)
    entryPoint.nutritionRepository().loadNutritionPeriod(weekQuery)
    entryPoint.bodyRepository().loadBodyPeriod(monthQuery, BodyPeriodMetric.ALL)
    entryPoint.vitalsRepository().loadVitalsPeriod(weekQuery, VitalsPeriodMetric.ALL)
    entryPoint.mindfulnessRepository().loadMindfulnessPeriod(weekQuery)
    entryPoint.cycleRepository().loadCyclePeriod(monthQuery)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MetricSummaryWarmupEntryPoint {
    fun dashboardDataLoader(): DashboardDataLoader
    fun preferencesRepository(): PreferencesRepository
    fun metricSummaryCacheStore(): MetricSummaryCacheStore
    fun derivedMetricStore(): DerivedMetricStore
    fun appForegroundGate(): AppForegroundGate
    fun activityRepository(): ActivityRepository
    fun sleepRepository(): SleepRepository
    fun heartRepository(): HeartRepository
    fun hydrationRepository(): HydrationRepository
    fun nutritionRepository(): NutritionRepository
    fun bodyRepository(): BodyRepository
    fun vitalsRepository(): VitalsRepository
    fun mindfulnessRepository(): MindfulnessRepository
    fun cycleRepository(): CycleRepository
}

private val WarmupReadinessMetrics = setOf(
    DashboardMetric.SLEEP,
    DashboardMetric.WORKOUT,
    DashboardMetric.AVG_HEART_RATE,
    DashboardMetric.RESTING_HEART_RATE,
    DashboardMetric.HRV,
    DashboardMetric.BODY_TEMPERATURE,
    DashboardMetric.SKIN_TEMPERATURE,
    DashboardMetric.WEEKLY_CARDIO_LOAD,
    DashboardMetric.INTENSITY_MINUTES,
    DashboardMetric.HYDRATION,
    DashboardMetric.CALORIES_IN,
    DashboardMetric.PROTEIN,
    DashboardMetric.CARBS,
    DashboardMetric.FAT,
    DashboardMetric.MINDFULNESS,
)
