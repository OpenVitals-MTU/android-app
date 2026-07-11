package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.HomeWidgetGlanceState
import es.antonborri.home_widget.HomeWidgetGlanceStateDefinition
import es.antonborri.home_widget.HomeWidgetGlanceWidgetReceiver
import es.antonborri.home_widget.HomeWidgetPlugin
import tech.mmarca.openvitals.R

/**
 * The two quick-beverage tiles, ported from the Kotlin `HomeQuickBeverageWidget.kt`:
 * a 2x1 with Add/Edit buttons and a 1x1 whose whole surface logs.
 *
 * Like every widget here they are RENDER-ONLY (Dart owns the drink catalog and
 * the formatting, and pushes the snapshot), with one exception that the Kotlin
 * app did not have to solve: **the tap has to log**, and the data to log with
 * lives in Dart.
 *
 * Kotlin logged in-process from the Glance `ActionCallback`, pulling the drink
 * out of Room through Hilt. Here the tap fires the `home_widget` plugin's
 * background broadcast (`HomeWidgetBackgroundIntent`), which runs a Dart callback
 * in a background isolate — `homeWidgetInteractivityCallback` in
 * `home_widget_beverage_log.dart`. That callback writes to Health Connect and
 * pushes the updated subtitle back. The only thing the URI carries is the
 * `appWidgetId`; the drink is looked up on the Dart side from the payload cached
 * when the widget was configured (drift is not reachable from that isolate).
 *
 * **Per-instance state**, as for the metric tile: `beverage.<appWidgetId>.`.
 * Both widgets share that namespace — exactly as Kotlin shares one state schema
 * between them and tells them apart by provider class.
 */
class HomeQuickBeverageWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()

    /** Kotlin `HomeQuickBeverageWidget.sizeMode`. */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 88.dp),
            DpSize(240.dp, 110.dp),
            DpSize(320.dp, 140.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The GlanceId is all we get; the key prefix is derived from the
        // appWidgetId behind it, which is what Dart pushed under.
        val prefix = homeQuickBeverageWidgetPrefix(
            GlanceAppWidgetManager(context).getAppWidgetId(id),
        )
        provideContent {
            val preferences = currentState<HomeWidgetGlanceState>().preferences
            val snapshot = preferences.readHomeWidgetSnapshot(prefix)
                ?: unconfiguredQuickBeverageSnapshot(LocalContext.current)
            val drinkId = preferences.getString("${prefix}selection_id", null).orEmpty()
            HomeQuickBeverageWidgetContent(snapshot = snapshot, drinkId = drinkId)
        }
    }
}

class HomeQuickBeverageOneTapWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()

    /** Kotlin `HomeQuickBeverageOneTapWidget.sizeMode`. */
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(88.dp, 88.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The GlanceId is all we get; the key prefix is derived from the
        // appWidgetId behind it, which is what Dart pushed under.
        val prefix = homeQuickBeverageWidgetPrefix(
            GlanceAppWidgetManager(context).getAppWidgetId(id),
        )
        provideContent {
            val preferences = currentState<HomeWidgetGlanceState>().preferences
            val snapshot = preferences.readHomeWidgetSnapshot(prefix)
                ?: unconfiguredQuickBeverageSnapshot(LocalContext.current)
            val drinkId = preferences.getString("${prefix}selection_id", null).orEmpty()
            HomeQuickBeverageOneTapWidgetContent(snapshot = snapshot, drinkId = drinkId)
        }
    }
}

class HomeQuickBeverageWidgetReceiver :
    HomeWidgetGlanceWidgetReceiver<HomeQuickBeverageWidget>() {
    override val glanceAppWidget = HomeQuickBeverageWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        clearQuickBeverageWidgetData(context, appWidgetIds)
    }
}

class HomeQuickBeverageOneTapWidgetReceiver :
    HomeWidgetGlanceWidgetReceiver<HomeQuickBeverageOneTapWidget>() {
    override val glanceAppWidget = HomeQuickBeverageOneTapWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        clearQuickBeverageWidgetData(context, appWidgetIds)
    }
}

/**
 * Fires the Dart background log for the tapped instance (Kotlin
 * `HomeQuickBeverageLogAction`, which logged directly).
 *
 * Glance's `clickable` wants an `Action`, and the plugin hands out a
 * `PendingIntent`, so the broadcast is sent from an `ActionCallback` — the shape
 * the plugin's own Glance example uses. The `appWidgetId` is recovered from the
 * `GlanceId`, exactly as `provideGlance` recovers the key prefix.
 */
class HomeQuickBeverageLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        HomeWidgetBackgroundIntent
            .getBroadcast(context, quickBeverageLogUri(appWidgetId))
            .send()
    }
}

/**
 * `openvitals://beverage_log?appWidgetId=<id>` — the URI Dart's interactivity
 * callback parses (`quickBeverageLogAppWidgetId`).
 *
 * The id is all it carries: which widget owns it, and which drink it holds, are
 * both resolved from widget storage on the Dart side. That is also what makes a
 * spoofed broadcast inert — an unknown id has no configured drink.
 */
internal fun quickBeverageLogUri(appWidgetId: Int): Uri = Uri.Builder()
    .scheme(HomeWidgetRouteScheme)
    .authority(QuickBeverageLogHost)
    .appendQueryParameter(QuickBeverageLogAppWidgetIdParam, appWidgetId.toString())
    .build()

private const val QuickBeverageLogHost = "beverage_log"
private const val QuickBeverageLogAppWidgetIdParam = "appWidgetId"

/** `beverage.<appWidgetId>.` — Dart's `homeWidgetKeyPrefix(HomeWidgetId.quickBeverage, …)`. */
internal fun homeQuickBeverageWidgetPrefix(appWidgetId: Int): String = "beverage.$appWidgetId."

/**
 * Drops a removed instance's keys, the cached drink payload included (Kotlin
 * `HomeQuickBeverageWidget.onDelete` → `clearDrink`).
 *
 * Android recycles appWidgetIds, so without this a freshly placed tile could
 * inherit a deleted one's drink — and, worse than for the metric tile, a tap on
 * it would *log* that drink. Best effort: the broadcast is the system telling us
 * the widget is already gone.
 */
private fun clearQuickBeverageWidgetData(context: Context, appWidgetIds: IntArray) {
    runCatching {
        val preferences = HomeWidgetPlugin.getData(context)
        val prefixes = appWidgetIds.map(::homeQuickBeverageWidgetPrefix)
        val stale = preferences.all.keys.filter { key ->
            prefixes.any(key::startsWith)
        }
        preferences.edit {
            stale.forEach(::remove)
        }
    }
}

/**
 * What an instance shows before it has been configured — i.e. before the picker's
 * first push lands (Kotlin's `provideGlance` fallback). Tapping it opens the
 * hydration entry screen rather than dead-ending.
 */
private fun unconfiguredQuickBeverageSnapshot(context: Context): HomeWidgetSnapshot =
    HomeWidgetSnapshot(
        title = context.getString(R.string.home_quick_beverage_widget_config_title),
        value = "--",
        unit = "",
        subtitle = context.getString(R.string.home_quick_beverage_widget_not_configured),
        route = QuickBeverageEntryRoute,
    )

/** Dart's `quickBeverageEntryRoute` / Kotlin's `Screen.HydrationEntry.route`. */
private const val QuickBeverageEntryRoute = "manual_entry/hydration"

/**
 * The tap action: log the configured drink, or — while unconfigured — open the
 * hydration entry screen (Kotlin `quickBeverageWidgetLogAction`).
 */
@Composable
private fun quickBeverageLogAction(
    context: Context,
    snapshot: HomeWidgetSnapshot,
    drinkId: String,
): Action =
    if (drinkId.isBlank()) {
        openRouteAction(context, snapshot.route)
    } else {
        actionRunCallback<HomeQuickBeverageLogAction>()
    }

/** Ported verbatim from Kotlin `HomeQuickBeverageWidgetContent` (HomeQuickBeverageWidget.kt:380-441). */
@Composable
private fun HomeQuickBeverageWidgetContent(
    snapshot: HomeWidgetSnapshot,
    drinkId: String,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isCompact = size.width <= 220.dp || size.height <= 110.dp
    val horizontalPadding = if (isCompact) 8.dp else 16.dp
    val verticalPadding = if (isCompact) 4.dp else 12.dp
    val titleFontSize = if (isCompact) 14.sp else 18.sp
    val amountFontSize = if (isCompact) 22.sp else 28.sp
    val actionFontSize = if (isCompact) 14.sp else 16.sp
    val amountBottomSpacing = if (isCompact) 3.dp else 10.dp
    val actionWidth = if (isCompact) 52.dp else 64.dp
    val actionHeight = if (isCompact) 28.dp else 36.dp
    val actionGap = if (isCompact) 12.dp else 14.dp
    val logAction = quickBeverageLogAction(context, snapshot, drinkId)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HomeWidgetTokens.BackgroundProvider)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.PrimaryTextProvider,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = snapshot.value,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.PrimaryTextProvider,
                fontSize = amountFontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(amountBottomSpacing))
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            HomeQuickBeverageWidgetButton(
                text = context.getString(R.string.action_add),
                action = logAction,
                width = actionWidth,
                height = actionHeight,
                fontSize = actionFontSize,
            )
            Spacer(modifier = GlanceModifier.width(actionGap))
            HomeQuickBeverageWidgetButton(
                // Opens the drink's entry screen (`manual_entry/hydration/log/<id>`),
                // or the plain entry screen while unconfigured — the route Dart pushed.
                text = context.getString(R.string.action_edit),
                action = openRouteAction(context, snapshot.route),
                width = actionWidth,
                height = actionHeight,
                fontSize = actionFontSize,
            )
        }
    }
}

/** Ported verbatim from Kotlin `HomeQuickBeverageOneTapWidgetContent` (HomeQuickBeverageWidget.kt:444-489). */
@Composable
private fun HomeQuickBeverageOneTapWidgetContent(
    snapshot: HomeWidgetSnapshot,
    drinkId: String,
) {
    val context = LocalContext.current
    val action = quickBeverageLogAction(context, snapshot, drinkId)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HomeWidgetTokens.BackgroundProvider)
            .clickable(action)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 2,
            style = TextStyle(
                color = HomeWidgetTokens.PrimaryTextProvider,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = snapshot.value,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.PrimaryTextProvider,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        // The third line exists only for what the user has to notice: the
        // transient "Saved now" confirmation, or an error. At rest the subtitle
        // is "Tap to log", which a 1x1 has no room to spend a line on.
        //
        // Kotlin compared against its own R.string here. We cannot: the subtitle
        // is localized in Dart, and these native strings are English-only — so a
        // French tile would never match and would show its resting subtitle
        // forever. Dart therefore pushes the resting text alongside (in the
        // otherwise-unused `unit` slot) and we compare against that, falling back
        // to the English string for the pre-push fallback snapshot.
        val restingSubtitle = snapshot.unit.ifBlank {
            context.getString(R.string.home_quick_beverage_widget_tap_to_log)
        }
        if (snapshot.subtitle.isNotBlank() && snapshot.subtitle != restingSubtitle) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = snapshot.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.PrimaryTextProvider,
                    fontSize = 9.sp,
                ),
            )
        }
    }
}

/** Kotlin `HomeQuickBeverageWidgetButton`. */
@Composable
private fun HomeQuickBeverageWidgetButton(
    text: String,
    action: Action,
    width: Dp,
    height: Dp,
    fontSize: TextUnit,
) {
    Column(
        modifier = GlanceModifier
            .width(width)
            .height(height)
            .background(HomeWidgetTokens.ActionBackgroundProvider)
            .clickable(action),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.PrimaryTextProvider,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
