package tech.mmarca.openvitals

import androidx.lifecycle.LifecycleObserver
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.performance.ReminderRestoreBootstrap
import tech.mmarca.openvitals.data.repository.SyncedRecordOriginRepository
import tech.mmarca.openvitals.devices.garmin.GarminLocalData
import tech.mmarca.openvitals.devices.garmin.GarminMusicRelay
import tech.mmarca.openvitals.devices.garmin.GarminNavigationRelay
import tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.features.imports.applehealth.AppleHealthImportWorkController
import tech.mmarca.openvitals.features.watches.WatchAutoSyncScheduler

class AppStartupActorsTest {

    private val reminders = mockk<ReminderRestoreBootstrap>(relaxed = true)
    private val origins = mockk<SyncedRecordOriginRepository>(relaxed = true)
    private val bridge = mockk<GarminNotificationBridge>(relaxed = true)
    private val navigation = mockk<GarminNavigationRelay>(relaxed = true)
    private val music = mockk<GarminMusicRelay>(relaxed = true)
    private val autoSync = mockk<WatchAutoSyncScheduler>(relaxed = true)
    private val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
    private val appleHealth = mockk<AppleHealthImportWorkController>(relaxed = true)
    private val garminData = mockk<GarminLocalData>(relaxed = true)

    private fun subject() = AppStartupActors(
        reminders, origins, bridge, navigation, music, autoSync, widgets, appleHealth, garminData,
    )

    @Test
    fun `every actor is started once`() {
        subject().start {}

        verify(exactly = 1) { origins.warmOverlay() }
        verify(exactly = 1) { bridge.onAppStart() }
        verify(exactly = 1) { navigation.start() }
        verify(exactly = 1) { music.start() }
        verify(exactly = 1) { autoSync.restoreAll() }
        verify(exactly = 1) { widgets.reconcile() }
        verify(exactly = 1) { appleHealth.clearAbandonedStagedExport() }
        verify(exactly = 1) { garminData.pruneOnAppStart() }
    }

    @Test
    fun `the reminder restore is handed over, not added here`() {
        // This runs off the main thread, where a lifecycle refuses a new observer.
        val handedOver = mutableListOf<LifecycleObserver>()

        subject().start { handedOver += it }

        assertEquals(listOf<LifecycleObserver>(reminders), handedOver)
    }

    @Test
    fun `the Application injects no more than it needs before the first screen`() {
        // Each plain injected field is built inside super.onCreate(), on the main thread.
        val source = File("src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt").readText()
        val eagerFields = Regex("""@Inject lateinit var \w+: (?!dagger\.Lazy<)""").findAll(source).count()

        assertWithMessage("eager @Inject fields in OpenVitalsApp. Put a new start-up actor in AppStartupActors.")
            .that(eagerFields)
            .isAtMost(2)
    }
}
