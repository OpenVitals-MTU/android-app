package tech.mmarca.openvitals

import android.app.Application
import tech.mmarca.openvitals.data.repository.ActivityRepository
import tech.mmarca.openvitals.data.repository.BodyRepository
import tech.mmarca.openvitals.data.repository.HeartRepository
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class OpenVitalsApp : Application() {

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(this)
    }

    val healthConnectManager: HealthConnectManager by lazy {
        HealthConnectManager(this)
    }

    val healthRepository: HealthRepository by lazy {
        HealthRepository(healthConnectManager)
    }

    val activityRepository: ActivityRepository by lazy {
        ActivityRepository(healthConnectManager)
    }

    val sleepRepository: SleepRepository by lazy {
        SleepRepository(healthConnectManager)
    }

    val heartRepository: HeartRepository by lazy {
        HeartRepository(healthConnectManager)
    }

    val bodyRepository: BodyRepository by lazy {
        BodyRepository(healthConnectManager)
    }
}
