package tech.mmarca.openvitals.features.homewidgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string

/**
 * Ports the reachable half of Flutter's
 * `test/features/homewidgets/home_widget_configure_test.dart` and the
 * `widget-type resolution` / `backing out` cases of
 * `home_widget_beverage_configure_test.dart`.
 *
 * Flutter resolves the picker from a deep link (`/widget-configure/<type>`),
 * so its tests drive a route. This app has no such route: Android launches the
 * activity the widget's `appwidget-provider` names in `android:configure`, and
 * the picker is a plain `ListView` rather than Compose. The equivalent
 * assertions are therefore the provider metadata (which picker a widget type
 * opens) and the real activity under `ActivityScenario` (what it renders, and
 * what it returns when the user backs out).
 *
 * The launch id is a fixed number no placed widget can own; nothing here writes
 * a selection, so the device is left as it was found.
 */
@RunWith(AndroidJUnit4::class)
class HomeWidgetConfigurationActivityTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ── widget-type resolution: which picker each widget declares ────────────

    @Test
    fun aBeverageWidgetOpensTheBeveragePicker() {
        assertEquals(
            beveragePicker,
            configureActivityOf(HomeQuickBeverageWidgetReceiver::class.java),
        )
    }

    @Test
    fun theOneTapWidgetOpensTheSameBeveragePicker() {
        assertEquals(
            beveragePicker,
            configureActivityOf(HomeQuickBeverageOneTapWidgetReceiver::class.java),
        )
    }

    @Test
    fun aMetricWidgetNeverOpensTheBeveragePicker() {
        val metricConfigure = configureActivityOf(HomeMetricWidgetReceiver::class.java)
        assertEquals(metricPicker, metricConfigure)
        assertNotEquals(beveragePicker, metricConfigure)
    }

    @Test
    fun aBeverageWidgetNeverOpensTheMetricPicker() {
        assertNotEquals(
            metricPicker,
            configureActivityOf(HomeQuickBeverageWidgetReceiver::class.java),
        )
        assertNotEquals(
            metricPicker,
            configureActivityOf(HomeQuickBeverageOneTapWidgetReceiver::class.java),
        )
    }

    // ── the metric picker's own rendering ────────────────────────────────────

    @Test
    fun theMetricPickerRendersThePromptAndTheMetricCatalog() {
        launchMetricPicker().use { scenario ->
            onView(withText(string(R.string.home_metric_widget_config_prompt)))
                .check(matches(isDisplayed()))

            val catalog = homeMetricWidgetCatalog()
            val expectedLabels = catalog.map { metricId -> string(metricId.homeMetricTitleRes()) }
            var labels: List<String> = emptyList()
            scenario.onActivity { activity ->
                val listView = checkNotNull(activity.findFirstListView()) {
                    "the picker renders a list"
                }
                labels = (0 until listView.adapter.count)
                    .map { position -> listView.adapter.getItem(position) as String }
            }

            // The whole catalog, in catalog order — no more, no fewer.
            assertEquals(expectedLabels, labels)
            scenario.onActivity { activity ->
                assertEquals(string(R.string.home_metric_widget_config_title), activity.title)
            }
            // …and the top of it is actually on screen, not just in an adapter.
            onView(withText(string(R.string.metric_steps))).check(matches(isDisplayed()))
            onView(withText(string(R.string.metric_distance))).check(matches(isDisplayed()))
        }
    }

    /**
     * The picker the beverage widgets launch is a beverage picker whichever
     * branch it takes — the drink list or the "no drinks" message, which the
     * device's own catalog decides — and it is never the metric one. The title
     * is the assertion because the activity sets it before it reads anything.
     */
    @Test
    fun theBeveragePickerIsNeverTheMetricPicker() {
        launchBeveragePicker().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    string(R.string.home_quick_beverage_widget_config_title),
                    activity.title,
                )
            }
            onView(withText(string(R.string.home_metric_widget_config_prompt)))
                .check(doesNotExist())
        }
    }

    // ── backing out: RESULT_CANCELED stands, nothing is persisted ────────────

    @Test
    fun backingOutOfTheMetricPickerNeverFinishesTheConfiguration() {
        launchMetricPicker().use { scenario ->
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

            // Android drops a half-placed widget on RESULT_CANCELED.
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertFalse(
                "no metric may be recorded for a widget the user never configured",
                context.getSharedPreferences("home_metric_widgets", Context.MODE_PRIVATE)
                    .contains("metric_id_$LaunchAppWidgetId"),
            )
        }
    }

    @Test
    fun backingOutOfTheBeveragePickerNeverFinishesTheConfiguration() {
        launchBeveragePicker().use { scenario ->
            scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertFalse(
                "no drink may be recorded for a widget the user never configured",
                context.getSharedPreferences("home_quick_beverage_widgets", Context.MODE_PRIVATE)
                    .contains("drink_id_$LaunchAppWidgetId"),
            )
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private val beveragePicker: ComponentName
        get() = ComponentName(context, HomeQuickBeverageWidgetConfigurationActivity::class.java)

    private val metricPicker: ComponentName
        get() = ComponentName(context, HomeMetricWidgetConfigurationActivity::class.java)

    private fun configureActivityOf(receiver: Class<*>): ComponentName? =
        providerInfoOf(receiver).configure

    private fun providerInfoOf(receiver: Class<*>): AppWidgetProviderInfo {
        val provider = ComponentName(context, receiver)
        val info = AppWidgetManager.getInstance(context)
            .installedProviders
            .firstOrNull { candidate -> candidate.provider == provider }
        return checkNotNull(info) {
            "${receiver.simpleName} is not an installed widget provider"
        }
    }

    private fun launchMetricPicker(): ActivityScenario<HomeMetricWidgetConfigurationActivity> =
        ActivityScenario.launchActivityForResult(
            configureIntent(HomeMetricWidgetConfigurationActivity::class.java),
        )

    private fun launchBeveragePicker(): ActivityScenario<HomeQuickBeverageWidgetConfigurationActivity> =
        ActivityScenario.launchActivityForResult(
            configureIntent(HomeQuickBeverageWidgetConfigurationActivity::class.java),
        )

    private fun configureIntent(activity: Class<*>): Intent =
        Intent(context, activity)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, LaunchAppWidgetId)

    private fun Activity.findFirstListView(): ListView? =
        findViewById<View>(android.R.id.content).findFirstListView()

    private fun View.findFirstListView(): ListView? = when (this) {
        is ListView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { index ->
            getChildAt(index).findFirstListView()
        }
        else -> null
    }

    private companion object {
        /** The appWidgetId Android would hand the configuration launch. */
        const val LaunchAppWidgetId = 424_242
    }
}
