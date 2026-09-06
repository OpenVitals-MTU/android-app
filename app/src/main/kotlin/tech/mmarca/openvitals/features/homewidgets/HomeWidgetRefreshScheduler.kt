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

/**
 * Owns the home widgets' background refresh: one `WorkManager` periodic work
 * while any widget is placed, none otherwise. The widget's own
 * `updatePeriodMillis` tick is not honoured in Doze; this one is. Mirrors
 * [tech.mmarca.openvitals.features.watches.WatchAutoSyncScheduler].
 */
@Singleton
class HomeWidgetRefreshScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /** Schedules when a widget is placed, cancels when none is. Idempotent. */
    fun reconcile() {
        // Runs from Application.onCreate and widget receivers. Not worth failing either over.
        runCatching {
            if (anyHomeWidgetPlaced(context)) ensureScheduled() else cancel()
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

    private fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<HomeWidgetRefreshWorker>(REFRESH_MINUTES, TimeUnit.MINUTES)
            // Battery only: Health Connect is local, and the app has no internet permission.
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "home-widget-refresh"
        const val NOW_WORK_NAME = "home-widget-refresh-now"
        const val REFRESH_MINUTES = 30L
        private const val RETRY_BACKOFF_MINUTES = 10L
    }
}
