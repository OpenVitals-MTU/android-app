package tech.mmarca.openvitals

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import tech.mmarca.openvitals.core.diagnostics.CrashReportHandler
import tech.mmarca.openvitals.core.performance.AppForegroundGate
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import javax.inject.Inject

@HiltAndroidApp
class OpenVitalsApp : Application() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var appForegroundGate: AppForegroundGate

    override fun onCreate() {
        super.onCreate()
        CrashReportHandler.install(this)
        AppCompatDelegate.setApplicationLocales(preferencesRepository.appLanguage.toLocaleListCompat())
        appForegroundGate.registerProcessLifecycle(ProcessLifecycleOwner.get())
    }
}
