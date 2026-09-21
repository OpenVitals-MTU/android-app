package tech.mmarca.openvitals

import android.app.Application
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import tech.mmarca.openvitals.core.diagnostics.CrashReportHandler
import tech.mmarca.openvitals.core.performance.AppForegroundGate
import tech.mmarca.openvitals.data.migration.FlutterDataMigrator
import tech.mmarca.openvitals.data.migration.FlutterMigrationEntryPoint
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltAndroidApp
class OpenVitalsApp : Application() {

    // Needed before the first screen: the language, and whether the app is in the foreground.
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var appForegroundGate: AppForegroundGate

    // Lazy, so Hilt does not build their graph inside super.onCreate(). See AppStartupActors.
    @Inject lateinit var startupActors: dagger.Lazy<AppStartupActors>

    override fun onCreate() {
        if (BuildConfig.DEBUG) logMainThreadStalls()
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
        } else if (flutterMigrator.garminWellnessImportMissed()) {
            // Months of samples: not on the main thread. Nothing at start-up waits for them.
            thread(name = "flutter-wellness-import") {
                flutterMigrator.importMissedGarminWellness(
                    EntryPointAccessors
                        .fromApplication(this, FlutterMigrationEntryPoint::class.java)
                        .openVitalsDatabase(),
                )
            }
        }
        CrashReportHandler.install(this)
        AppCompatDelegate.setApplicationLocales(preferencesRepository.appLanguage.toLocaleListCompat())
        appForegroundGate.registerProcessLifecycle(ProcessLifecycleOwner.get())
        thread(name = "app-startup") {
            startupActors.get().start { observer ->
                // A late observer is brought up to the current state, so no start is missed.
                mainHandler.post { ProcessLifecycleOwner.get().lifecycle.addObserver(observer) }
            }
        }
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Locale and regional-preference changes arrive here first.
        preferencesRepository.refreshSystemUnitSystem()
    }
}

// Debug builds only: log disk reads, disk writes and flagged slow calls on the main thread.
// Never crashes the app.
private fun logMainThreadStalls() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectCustomSlowCalls()
            .penaltyLog()
            .build(),
    )
}
