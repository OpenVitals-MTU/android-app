package tech.mmarca.openvitals

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import tech.mmarca.openvitals.core.diagnostics.CrashReportHandler
import tech.mmarca.openvitals.core.performance.AppForegroundGate
import tech.mmarca.openvitals.core.performance.ReminderRestoreBootstrap
import tech.mmarca.openvitals.data.migration.FlutterDataMigrator
import tech.mmarca.openvitals.data.migration.FlutterMigrationEntryPoint
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.features.watches.WatchAutoSyncScheduler
import javax.inject.Inject

@HiltAndroidApp
class OpenVitalsApp : Application() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var appForegroundGate: AppForegroundGate
    @Inject lateinit var reminderRestoreBootstrap: ReminderRestoreBootstrap
    @Inject lateinit var syncedRecordOriginRepository: SyncedRecordOriginRepository
    @Inject lateinit var garminNotificationBridge: tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge
    @Inject lateinit var garminNavigationRelay: tech.mmarca.openvitals.devices.garmin.GarminNavigationRelay
    @Inject lateinit var watchAutoSyncScheduler: WatchAutoSyncScheduler

    override fun onCreate() {
        // The Flutter migration splits around super.onCreate(): preference writes
        // must land before Hilt constructs PreferencesRepository, and the database
        // import needs the Hilt-provided Room. See FlutterDataMigrator.
        val flutterMigrator = FlutterDataMigrator(this)
        val flutterMigrationPending = flutterMigrator.migrateIfNeeded()
        super.onCreate()
        if (flutterMigrationPending) {
            flutterMigrator.importDatabaseAndFinish(
                EntryPointAccessors
                    .fromApplication(this, FlutterMigrationEntryPoint::class.java)
                    .openVitalsDatabase(),
            )
        }
        CrashReportHandler.install(this)
        AppCompatDelegate.setApplicationLocales(preferencesRepository.appLanguage.toLocaleListCompat())
        appForegroundGate.registerProcessLifecycle(ProcessLifecycleOwner.get())
        ProcessLifecycleOwner.get().lifecycle.addObserver(reminderRestoreBootstrap)
        // Synced records show their original source app.
        syncedRecordOriginRepository.warmOverlay()
        // Companion mode must be re-armed on every start.
        garminNotificationBridge.onAppStart()
        garminNavigationRelay.start()
        // Re-plans the sync schedules after what WorkManager does not cover.
        watchAutoSyncScheduler.restoreAll()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Locale and regional-preference changes arrive here first.
        preferencesRepository.refreshSystemUnitSystem()
    }
}
