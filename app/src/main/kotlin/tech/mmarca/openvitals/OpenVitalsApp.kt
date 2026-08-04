package tech.mmarca.openvitals

import android.app.Application
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
import javax.inject.Inject

@HiltAndroidApp
class OpenVitalsApp : Application() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var appForegroundGate: AppForegroundGate
    @Inject lateinit var reminderRestoreBootstrap: ReminderRestoreBootstrap

    override fun onCreate() {
        // The one-time Flutter->Kotlin data migration is deliberately split
        // around super.onCreate(). @HiltAndroidApp member-injects this class
        // DURING super.onCreate(), and PreferencesRepository (an injected
        // field here) eagerly snapshots its SharedPreferences into StateFlows
        // at construction — so every preference write must land before that,
        // which rules out Hilt-injecting the migrator itself. The beverage
        // database import instead needs the Hilt-provided Room singleton, so
        // it runs right after super.onCreate() via an EntryPoint (Room
        // singletons are created lazily on first request, and no Activity can
        // exist yet). See FlutterDataMigrator's KDoc.
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
    }
}
