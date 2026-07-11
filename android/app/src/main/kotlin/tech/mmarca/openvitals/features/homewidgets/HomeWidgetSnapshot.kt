package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import es.antonborri.home_widget.actionStartActivity
import tech.mmarca.openvitals.MainActivity

/**
 * Shared model + renderer for the read-only home-screen widgets.
 *
 * These widgets are RENDER-ONLY. Unlike the Kotlin source app — where each
 * widget loads from repositories via Hilt inside `onUpdate` — the data lives in
 * Dart: `HomeWidgetService.pushSnapshot` flattens a snapshot into the shared
 * `HomeWidgetPreferences` file and broadcasts an update. The composables here
 * only read strings back out and draw them, so none of the Kotlin loaders,
 * entry points or `refresh*Widget` helpers are ported.
 *
 * Ported from `HomeMetricWidget.kt` (snapshot model + `HomeMetricWidgetContent`).
 */

/** One label/value(/subtitle) line. Kotlin `HomeMetricWidgetRow`. */
internal data class HomeWidgetRow(
    val label: String,
    val value: String,
    val subtitle: String = "",
)

/** The flat data a widget renders. Kotlin `HomeMetricWidgetSnapshot`. */
internal data class HomeWidgetSnapshot(
    val title: String,
    val value: String = "",
    val unit: String = "",
    val subtitle: String = "",
    val route: String = DefaultRoute,
    val rows: List<HomeWidgetRow> = emptyList(),
) {
    companion object {
        /** Dart's `HomeWidgetSnapshot.defaultRoute` / Kotlin's `Screen.Dashboard.route`. */
        const val DefaultRoute = "dashboard"
    }
}

/**
 * Reads the snapshot Dart pushed under [prefix] (e.g. `daily_readiness.`).
 *
 * Every widget shares one SharedPreferences file, hence the namespace. Returns
 * `null` when `title` is absent — i.e. Dart has not pushed yet — which gates the
 * fallback UI exactly as the Kotlin `Preferences.toWidgetSnapshot` does.
 */
internal fun SharedPreferences.readHomeWidgetSnapshot(prefix: String): HomeWidgetSnapshot? {
    val title = getString("${prefix}title", null) ?: return null
    val rowCount = getInt("${prefix}row_count", 0).coerceIn(0, HomeWidgetTokens.MaxRows)
    val rows = (0 until rowCount).mapNotNull { index ->
        val label = getString("${prefix}row_${index}_label", null) ?: return@mapNotNull null
        val value = getString("${prefix}row_${index}_value", null) ?: return@mapNotNull null
        HomeWidgetRow(
            label = label,
            value = value,
            subtitle = getString("${prefix}row_${index}_subtitle", null).orEmpty(),
        )
    }
    return HomeWidgetSnapshot(
        title = title,
        value = getString("${prefix}value", null) ?: "--",
        unit = getString("${prefix}unit", null).orEmpty(),
        subtitle = getString("${prefix}subtitle", null).orEmpty(),
        route = getString("${prefix}route", null) ?: HomeWidgetSnapshot.DefaultRoute,
        rows = rows,
    )
}

/** `"<label>: <value>[ - <subtitle>]"`. Kotlin `HomeMetricWidgetRow.displayText()`. */
internal fun HomeWidgetRow.displayText(): String =
    buildString {
        append(label)
        append(": ")
        append(value)
        if (subtitle.isNotBlank()) {
            append(" - ")
            append(subtitle)
        }
    }

/**
 * Deep link a widget tap into the app at [route].
 *
 * Kotlin passed the route as an intent extra; here the `home_widget` plugin owns
 * the launch intent (MainActivity already declares its
 * `es.antonborri.home_widget.action.LAUNCH` filter), so the route rides on the
 * intent's data URI instead: `openvitals://widget?route=<route>`. Dart reads it
 * back with `Uri.queryParameters['route']` — `appendQueryParameter` percent-encodes,
 * `queryParameters` decodes, so routes containing `/` survive the round trip.
 *
 * The plugin's own `actionStartActivity` builds the `LAUNCH`-action intent that
 * `MainActivity`'s intent-filter (and the plugin's Dart-side `initiallyLaunchedFromHomeWidget`
 * / `widgetClicked`) expects. Glance 1.1.1 has no `PendingIntent` overload of
 * `actionStartActivity`, so this — rather than `HomeWidgetLaunchIntent.getActivity` —
 * is the supported path.
 */
internal fun openRouteAction(context: Context, route: String): Action {
    val uri = Uri.Builder()
        .scheme(HomeWidgetRouteScheme)
        .authority(HomeWidgetRouteHost)
        .appendQueryParameter(HomeWidgetRouteQueryParam, route)
        .build()
    return actionStartActivity<MainActivity>(context, uri)
}

internal const val HomeWidgetRouteScheme = "openvitals"
internal const val HomeWidgetRouteHost = "widget"
internal const val HomeWidgetRouteQueryParam = "route"

/**
 * The shared status-widget renderer (daily readiness + body energy).
 *
 * Ported verbatim from Kotlin `HomeMetricWidgetContent` (HomeMetricWidget.kt:334-407).
 */
@Composable
internal fun HomeMetricWidgetContent(snapshot: HomeWidgetSnapshot) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HomeWidgetTokens.BackgroundProvider)
            .clickable(openRouteAction(context, snapshot.route))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.MutedTextProvider,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (snapshot.value.isNotBlank()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.Bottom,
            ) {
                Text(
                    text = snapshot.value,
                    maxLines = 1,
                    style = TextStyle(
                        color = HomeWidgetTokens.PrimaryTextProvider,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (snapshot.unit.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = snapshot.unit,
                        maxLines = 1,
                        style = TextStyle(
                            color = HomeWidgetTokens.MutedTextProvider,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
        if (snapshot.subtitle.isNotBlank()) {
            Text(
                text = snapshot.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.MutedTextProvider,
                    fontSize = 12.sp,
                ),
            )
        }
        if (snapshot.rows.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            snapshot.rows.forEach { row ->
                Text(
                    text = row.displayText(),
                    maxLines = 1,
                    style = TextStyle(
                        color = HomeWidgetTokens.PrimaryTextProvider,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
        }
    }
}
