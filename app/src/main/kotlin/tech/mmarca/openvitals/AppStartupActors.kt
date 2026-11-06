package tech.mmarca.openvitals

import androidx.lifecycle.LifecycleObserver
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.core.performance.ReminderRestoreBootstrap
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController

/**
 * What the app starts at every process start that the first screen does not wait for.
 *
 * The Application asks for this through `dagger.Lazy` and builds it on a background
 * thread. As plain injected fields, they once pulled about 70 graph types into
 * `super.onCreate()` on the main thread, with a handler thread, ten preference
 * stores and a file read among them.
 */
@Singleton
class AppStartupActors @Inject constructor(
    private val reminderRestoreBootstrap: ReminderRestoreBootstrap,
    private val syncedRecordOriginRepository: SyncedRecordOriginRepository,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler,
    private val appleHealthImportWorkController: AppleHealthImportWorkController,
) {
    /** [observeProcessLifecycle] must add its observer on the main thread. This runs off it. */
    fun start(observeProcessLifecycle: (LifecycleObserver) -> Unit) {
        observeProcessLifecycle(reminderRestoreBootstrap)
        // Synced records show their original source app.
        syncedRecordOriginRepository.warmOverlay()
        // Widgets placed before this schedule existed, or a missed onEnabled.
        homeWidgetRefreshScheduler.reconcile()
        // A health export copied for an analysis the user never finished.
        appleHealthImportWorkController.clearAbandonedStagedExport()
    }
}
