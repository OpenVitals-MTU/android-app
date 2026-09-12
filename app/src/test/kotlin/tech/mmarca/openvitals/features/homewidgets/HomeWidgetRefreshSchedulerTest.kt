package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.HomeWidgetRefreshInterval

/** The schedule follows the widgets and the chosen interval: on while one is placed, gone when none is. */
class HomeWidgetRefreshSchedulerTest {

    private val appWidgetManager = mockk<AppWidgetManager>()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true).also { context ->
        every { context.applicationContext } returns context
        every { context.packageName } returns "tech.mmarca.openvitals"
    }
    private var storedInterval = HomeWidgetRefreshInterval.DEFAULT
    private val preferences = mockk<PreferencesRepository> {
        every { homeWidgetRefreshInterval } answers { storedInterval }
        every { homeWidgetRefreshInterval = any() } answers { storedInterval = firstArg() }
    }
    private val scheduler = HomeWidgetRefreshScheduler(context, preferences)

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        // getInstance lives on the companion, so mockkStatic does not reach it.
        mockkObject(WorkManager)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        every { WorkManager.getInstance(any()) } returns workManager
        every { appWidgetManager.getAppWidgetIds(any()) } returns IntArray(0)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppWidgetManager::class)
        unmockkObject(WorkManager)
    }

    @Test
    fun `no widget placed means no schedule`() {
        scheduler.reconcile()

        verify(exactly = 1) { workManager.cancelUniqueWork(HomeWidgetRefreshScheduler.PERIODIC_WORK_NAME) }
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `a placed widget schedules a battery-aware refresh at the default 30 minutes, keeping a live one`() {
        every { appWidgetManager.getAppWidgetIds(any()) } returns intArrayOf(7)
        val request = slot<PeriodicWorkRequest>()

        scheduler.reconcile()

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HomeWidgetRefreshScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(request),
            )
        }
        val spec = request.captured.workSpec
        assertThat(spec.workerClassName).isEqualTo(HomeWidgetRefreshWorker::class.java.name)
        assertThat(spec.intervalDuration).isEqualTo(TimeUnit.MINUTES.toMillis(30))
        assertThat(spec.constraints.requiresBatteryNotLow()).isTrue()
        verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun `reconcile plans at the stored interval`() {
        storedInterval = HomeWidgetRefreshInterval.EVERY_2_HOURS
        every { appWidgetManager.getAppWidgetIds(any()) } returns intArrayOf(7)
        val request = slot<PeriodicWorkRequest>()

        scheduler.reconcile()

        verify { workManager.enqueueUniquePeriodicWork(any(), ExistingPeriodicWorkPolicy.KEEP, capture(request)) }
        assertThat(request.captured.workSpec.intervalDuration).isEqualTo(TimeUnit.MINUTES.toMillis(120))
    }

    @Test
    fun `setInterval stores the choice and re-plans a placed widget from now`() {
        every { appWidgetManager.getAppWidgetIds(any()) } returns intArrayOf(7)
        val request = slot<PeriodicWorkRequest>()

        scheduler.setInterval(HomeWidgetRefreshInterval.EVERY_15_MINUTES)

        assertThat(storedInterval).isEqualTo(HomeWidgetRefreshInterval.EVERY_15_MINUTES)
        assertThat(scheduler.interval).isEqualTo(HomeWidgetRefreshInterval.EVERY_15_MINUTES)
        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                HomeWidgetRefreshScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                capture(request),
            )
        }
        assertThat(request.captured.workSpec.intervalDuration).isEqualTo(TimeUnit.MINUTES.toMillis(15))
    }

    @Test
    fun `setInterval with no widget placed only stores the choice`() {
        scheduler.setInterval(HomeWidgetRefreshInterval.HOURLY)

        assertThat(storedInterval).isEqualTo(HomeWidgetRefreshInterval.HOURLY)
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any()) }
    }

    @Test
    fun `refreshNow is one run, and a burst is still one run`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.refreshNow()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                HomeWidgetRefreshScheduler.NOW_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertThat(request.captured.workSpec.workerClassName).isEqualTo(HomeWidgetRefreshWorker::class.java.name)
    }
}
