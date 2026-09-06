package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * One refresh of every placed home widget, run by `WorkManager`. Reads
 * Health Connect in the background, so the tiles move without the app
 * being opened. Always succeeds: a failed read keeps the last tile, and
 * retrying a rate-limited Health Connect would make the next read worse.
 */
class HomeWidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!anyHomeWidgetPlaced(applicationContext)) {
            // The schedule outlived the widgets.
            EntryPointAccessors.fromApplication(applicationContext, HomeWidgetRefreshWorkerEntryPoint::class.java)
                .homeWidgetRefreshScheduler()
                .cancel()
            return Result.success()
        }
        refreshPlacedHomeWidgetsInProcess(applicationContext)
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeWidgetRefreshWorkerEntryPoint {
    fun homeWidgetRefreshScheduler(): HomeWidgetRefreshScheduler
}
