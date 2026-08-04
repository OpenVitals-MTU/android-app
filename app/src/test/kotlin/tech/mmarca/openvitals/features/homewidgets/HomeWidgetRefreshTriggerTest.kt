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
 * Redrawing the home screen after data lands from outside the app.
 *
 * A widget's own tick is `updatePeriodMillis`, which the system honours at its
 * convenience and not at all in Doze — so a Garmin sync at 08:05 left the
 * morning's tiles on their pre-sync numbers for half an hour or more, while the
 * user was looking at the screen having just watched the sync succeed.
 *
 * Only the two JVM-reachable halves are here. `ComponentName` and `Intent` are
 * throwing stubs in the unit-test android.jar, so the shape of the broadcast
 * itself — action, component, id array — cannot be asserted without
 * instrumentation, and a test that mocked its way around them would be
 * asserting the mocks. What IS checked is the guard that decides whether any
 * work happens at all, and that no widget is left out of the list.
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
        // This runs after every watch sync, phone sync and import. A refresh
        // costs several forced dashboard loads and a Body Energy timeline per
        // placed tile, so the common case — no widget on the home screen — has
        // to be an id enumeration and nothing more.
        refreshPlacedHomeWidgets(context)

        verify(exactly = 0) { context.sendBroadcast(any()) }
    }

    @Test
    fun `every widget the manifest declares is one this refreshes`() {
        // The list is written out by hand so that adding a widget without
        // adding it here is visible. This is what makes that true: a new
        // receiver in the manifest and not in the list fails here rather than
        // shipping as a tile that silently stops updating after a sync.
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
