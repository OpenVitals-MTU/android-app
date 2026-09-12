package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.HomeWidgetRefreshInterval

/**
 * Owns the home widgets' background refresh: one `WorkManager` periodic work
 * at the interval the user chose, while any widget is placed; none
 * otherwise. The widget's own `updatePeriodMillis` tick is not honoured in
 * Doze; this one is. Mirrors
 * [tech.mmarca.openvitals.features.watches.WatchAutoSyncScheduler].
 */
@Singleton
class HomeWidgetRefreshScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    val interval: HomeWidgetRefreshInterval get() = preferencesRepository.homeWidgetRefreshInterval

    /** Stores the choice and re-plans from now, so a shorter interval runs sooner. */
    fun setInterval(interval: HomeWidgetRefreshInterval) {
        preferencesRepository.homeWidgetRefreshInterval = interval
        runCatching {
            if (anyHomeWidgetPlaced(context)) enqueue(interval, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
        }.onFailure { Log.w(HomeWidgetLogTag, "Could not re-plan the widget refresh schedule", it) }
    }

    /** Schedules when a widget is placed, cancels when none is. Idempotent; KEEP leaves a live schedule alone. */
    fun reconcile() {
        // Runs from Application.onCreate and widget receivers. Not worth failing either over.
        runCatching {
            if (anyHomeWidgetPlaced(context)) enqueue(interval, ExistingPeriodicWorkPolicy.KEEP) else cancel()
        }.onFailure { Log.w(HomeWidgetLogTag, "Could not reconcile the widget refresh schedule", it) }
    }

    fun cancel() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** One run now. KEEP: a burst of app refreshes is one widget redraw. */
    fun refreshNow() {
        runCatching {
            workManager.enqueueUniqueWork(
                NOW_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<HomeWidgetRefreshWorker>().build(),
            )
        }.onFailure { Log.w(HomeWidgetLogTag, "Could not enqueue the widget refresh", it) }
    }

    private fun enqueue(interval: HomeWidgetRefreshInterval, policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<HomeWidgetRefreshWorker>(interval.minutes.toLong(), TimeUnit.MINUTES)
            // Battery only: Health Connect is local, and the app has no internet permission.
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, policy, request)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "home-widget-refresh"
        const val NOW_WORK_NAME = "home-widget-refresh-now"
        private const val RETRY_BACKOFF_MINUTES = 10L
    }
}
