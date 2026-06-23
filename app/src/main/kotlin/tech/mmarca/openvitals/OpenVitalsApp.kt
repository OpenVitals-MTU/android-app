package tech.mmarca.openvitals

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import tech.mmarca.openvitals.data.cache.MetricSummaryWarmupWorker

@HiltAndroidApp
class OpenVitalsApp : Application() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(preferencesRepository.appLanguage.toLocaleListCompat())
        MetricSummaryWarmupWorker.enqueue(this)
    }
}
