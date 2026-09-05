package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Redrawing the home screen after data lands from outside the app. `updatePeriodMillis`
 * is honoured at the system's convenience, so a sync left the tiles stale for half an hour.
 * `ComponentName` and `Intent` are stubs in the unit-test android.jar, so only the guard
 * and the widget list are checked here.
 */
class HomeWidgetRefreshTriggerTest {

    private val appWidgetManager = mockk<AppWidgetManager>()
    private val context = mockk<Context>(relaxed = true).also { context ->
        every { context.applicationContext } returns context
        every { context.packageName } returns "tech.mmarca.openvitals"
    }

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        every { appWidgetManager.getAppWidgetIds(any()) } returns IntArray(0)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppWidgetManager::class)
    }

    @Test
    fun `a launcher with no OpenVitals widget costs nothing`() {
        // This runs after every sync and import, so the no-widget case must be an id enumeration and nothing more.
        refreshPlacedHomeWidgets(context)

        verify(exactly = 0) { context.sendBroadcast(any()) }
    }

    @Test
    fun `every widget the manifest declares is one this refreshes`() {
        // Written out by hand, so a new receiver in the manifest and not in the list fails here.
        val declared = MANIFEST_WIDGET_RECEIVER
            .findAll(File("src/main/AndroidManifest.xml").readText())
            .filter { "APPWIDGET_UPDATE" in it.groupValues[2] }
            .map { it.groupValues[1].removePrefix(".") }
            .toSet()

        assertThat(declared).isNotEmpty()
        assertThat(HomeWidgetReceivers.map { it.name.removePrefix("tech.mmarca.openvitals.") })
            .containsExactlyElementsIn(declared)
    }

    private companion object {
        val MANIFEST_WIDGET_RECEIVER =
            Regex("""<receiver[^>]*android:name="([^"]+)"[^>]*>(.*?)</receiver>""", RegexOption.DOT_MATCHES_ALL)
    }
}
