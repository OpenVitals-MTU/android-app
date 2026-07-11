package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import es.antonborri.home_widget.HomeWidgetGlanceState
import es.antonborri.home_widget.HomeWidgetGlanceStateDefinition
import es.antonborri.home_widget.HomeWidgetGlanceWidgetReceiver
import es.antonborri.home_widget.HomeWidgetPlugin
import tech.mmarca.openvitals.R

/**
 * The configurable single-metric tile, ported from the Kotlin `HomeMetricWidget.kt`.
 *
 * Like the other widgets here it is RENDER-ONLY: Dart owns the metric catalog and
 * all of the formatting, so it computes the snapshot and pushes it (see
 * `HomeWidgetService.pushSnapshot`), and this composable just draws what it finds.
 * None of the Kotlin loader / Hilt entry point / `refreshHomeMetricWidget` machinery
 * is ported.
 *
 * **Per-instance state.** Every placed tile shows its own metric, so — unlike the
 * read-only widgets, which have one key namespace each — this one namespaces by
 * `appWidgetId` too: `metric.<appWidgetId>.` (Dart's
 * `homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId:)`). Kotlin needed no such
 * thing: Glance gives each instance its own datastore, whereas the `home_widget`
 * plugin puts every widget's data in one shared SharedPreferences file.
 *
 * **Configuration** is the Flutter side too: `android:configure` on the provider
 * points at `MainActivity`, which the plugin turns into a configure launch (see
 * `home_widget_configure.dart`). The picker persists the chosen metric under
 * `metric.<appWidgetId>.selection_id` — the handshake `HomeWidgetRefresher` reads
 * back to know which metric each placed tile is showing — and pushes the first
 * snapshot before finishing.
 */
class HomeMetricWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()

    /** Kotlin `HomeMetricWidget.sizeMode`. */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 88.dp),
            DpSize(220.dp, 110.dp),
            DpSize(320.dp, 140.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The GlanceId is all we get; the key prefix is derived from the
        // appWidgetId behind it, which is what Dart pushed under.
        val prefix = homeMetricWidgetPrefix(GlanceAppWidgetManager(context).getAppWidgetId(id))
        provideContent {
            val preferences = currentState<HomeWidgetGlanceState>().preferences
            val snapshot = preferences.readHomeWidgetSnapshot(prefix)
                ?: unconfiguredSnapshot(LocalContext.current)
            HomeMetricWidgetContent(snapshot = snapshot)
        }
    }
}

class HomeMetricWidgetReceiver : HomeWidgetGlanceWidgetReceiver<HomeMetricWidget>() {
    override val glanceAppWidget = HomeMetricWidget()

    /**
     * Drops a removed instance's keys (Kotlin `HomeMetricWidget.onDelete` →
     * `HomeMetricWidgetSelection.clearMetric`).
     *
     * Without this the shared preferences file keeps `metric.<id>.*` forever, and
     * Android recycles appWidgetIds — so a freshly added tile could inherit the
     * metric, value and `selection_id` of a deleted one and look configured before
     * the user ever picked anything. Best effort: nothing here may throw, the
     * broadcast is the system telling us the widget is already gone.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        runCatching {
            val preferences = HomeWidgetPlugin.getData(context)
            val prefixes = appWidgetIds.map(::homeMetricWidgetPrefix)
            val stale = preferences.all.keys.filter { key ->
                prefixes.any(key::startsWith)
            }
            preferences.edit {
                stale.forEach(::remove)
            }
        }
    }
}

/** `metric.<appWidgetId>.` — Dart's `homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId:)`. */
internal fun homeMetricWidgetPrefix(appWidgetId: Int): String = "metric.$appWidgetId."

/**
 * What an instance shows before it has been configured — i.e. before the picker's
 * first push lands (Kotlin's `provideGlance` fallback). Tapping it opens the
 * dashboard rather than dead-ending.
 */
private fun unconfiguredSnapshot(context: Context): HomeWidgetSnapshot = HomeWidgetSnapshot(
    title = context.getString(R.string.home_metric_widget_config_title),
    value = "--",
    unit = "",
    subtitle = context.getString(R.string.home_metric_widget_not_configured),
    route = HomeWidgetSnapshot.DefaultRoute,
)
