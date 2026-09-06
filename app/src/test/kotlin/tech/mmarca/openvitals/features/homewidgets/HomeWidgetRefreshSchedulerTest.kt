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

/** The schedule follows the widgets: on while one is placed, gone when none is. */
class HomeWidgetRefreshSchedulerTest {

    private val appWidgetManager = mockk<AppWidgetManager>()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true).also { context ->
        every { context.applicationContext } returns context
        every { context.packageName } returns "tech.mmarca.openvitals"
    }
    private val scheduler = HomeWidgetRefreshScheduler(context)

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
    fun `a placed widget schedules a 30-minute battery-aware refresh, keeping a live one`() {
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
