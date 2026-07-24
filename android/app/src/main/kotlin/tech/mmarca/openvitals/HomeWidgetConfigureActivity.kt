package tech.mmarca.openvitals

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Base of the two home-widget configuration activities — the Flutter ports of the
 * Kotlin `HomeMetricWidgetConfigurationActivity` and
 * `HomeQuickBeverageWidgetConfigurationActivity`.
 *
 * **Why these exist at all.** Every configurable widget used to point
 * `android:configure` at [MainActivity], which is `singleTop`. When the app was
 * already running, Android delivered `ACTION_APPWIDGET_CONFIGURE` to the *existing*
 * MainActivity through `onNewIntent` instead of starting a new one, so:
 *
 * * `main()` never re-ran, and the configure intent was read off the activity's
 *   *initial* intent — absent, or stale from a previous configure launch (a stale
 *   id resolves to the wrong widget type, which is how a metric tile ended up
 *   showing the beverage picker); and
 * * the activity had never been `startActivityForResult`-ed, so the `RESULT_OK`
 *   the picker set reached nobody and the launcher silently discarded the widget.
 *
 * A dedicated activity per widget fixes both at the root: it is genuinely started
 * for result, it *knows* what it is configuring, and (with the default launch
 * mode) every configure launch is a fresh activity with its own Flutter engine.
 *
 * **How Dart learns what to show.** The widget type and the `appWidgetId` are
 * handed over as the engine's **initial route**
 * (`/widget-configure/<type>?appWidgetId=<id>`), which `main()` reads off
 * `PlatformDispatcher.defaultRouteName` and turns into the right picker. No
 * `installedWidgets()` scan, no `home_widget` configure API — that API only exists
 * to paper over the single-activity setup this replaces.
 *
 * **The result contract.** `RESULT_CANCELED` is set in [onCreate], so backing out
 * of the picker leaves the launcher with a cancelled configure and no half-placed
 * tile. Only when the user actually picks does Dart call [CONFIGURE_CHANNEL]'s
 * `finishConfigure`, which sets `RESULT_OK` with `EXTRA_APPWIDGET_ID` — the one
 * thing that makes the launcher keep the widget (Kotlin's `configure()` did
 * exactly this).
 */
abstract class HomeWidgetConfigureActivity : FlutterFragmentActivity() {

    /** The instance being configured, off the launching intent. */
    private val appWidgetId: Int
        get() = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    /**
     * The Dart `HomeWidgetId.name` this activity is configuring. Passed the id so
     * the shared beverage activity can tell its two providers apart.
     */
    protected abstract fun widgetType(appWidgetId: Int): String

    /**
     * The route the Dart entrypoint boots on. Consulted by
     * `FlutterFragmentActivity` while the fragment is created, i.e. before the
     * Dart isolate runs, so `main()` sees it as `defaultRouteName`.
     */
    override fun getInitialRoute(): String {
        val id = appWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return "/"
        return "$CONFIGURE_ROUTE_PREFIX${widgetType(id)}?$APP_WIDGET_ID_PARAM=$id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = appWidgetId
        // Kotlin's `setResult(RESULT_CANCELED)` in onCreate: whatever happens from
        // here — a back press, a crash, a swipe away — the launcher must not keep a
        // tile the user never configured.
        setResult(RESULT_CANCELED, resultIntent(id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) finish()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CONFIGURE_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "finishConfigure" -> {
                    finishConfigure(call.argument<Int>(APP_WIDGET_ID_PARAM) ?: appWidgetId)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    /** The user picked: keep the widget, and hand the launcher back its id. */
    private fun finishConfigure(id: Int) {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        setResult(RESULT_OK, resultIntent(id))
        finish()
    }

    private fun resultIntent(id: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)

    companion object {
        /** Mirrored by `homeWidgetConfigureChannelName` in Dart. */
        const val CONFIGURE_CHANNEL = "tech.mmarca.openvitals/home_widget_configure"

        /** Mirrored by `homeWidgetConfigureRoutePrefix` in Dart. */
        const val CONFIGURE_ROUTE_PREFIX = "/widget-configure/"
        const val APP_WIDGET_ID_PARAM = "appWidgetId"

        /** The three Dart `HomeWidgetId` enum names that are configurable. */
        const val TYPE_METRIC = "metric"
        const val TYPE_QUICK_BEVERAGE = "quickBeverage"
        const val TYPE_QUICK_BEVERAGE_ONE_TAP = "quickBeverageOneTap"

        /** The three zero-config status widgets, gated at add time (see
         *  [HomeStatusWidgetConfigureActivity]). */
        const val TYPE_DAILY_READINESS = "dailyReadiness"
        const val TYPE_BODY_ENERGY = "bodyEnergy"
        const val TYPE_TODAY_VITALS = "todayVitals"
    }
}

/** `android:configure` of `@xml/home_metric_widget_info`. */
class HomeMetricWidgetConfigureActivity : HomeWidgetConfigureActivity() {
    override fun widgetType(appWidgetId: Int): String = TYPE_METRIC
}

/**
 * `android:configure` of **both** quick-beverage widgets — the 2x1 and the 1x1
 * one-tap — exactly as the Kotlin app shared one configuration activity between
 * them.
 *
 * They share a picker but not a provider, so the pick has to be pushed to the
 * right receiver. The bound provider is what tells them apart (Kotlin's
 * `quickBeverageWidgetReceiverClassForAppWidgetId` reads the same thing): by the
 * time a configuration activity runs, the host has already bound the id, so
 * `getAppWidgetInfo` resolves.
 */
/**
 * `android:configure` of the three status widgets (Body Energy, Daily
 * Readiness, Today Vitals), which need no picker at all.
 *
 * It exists so Dart can ask for `SCHEDULE_EXACT_ALARM` at ADD time: the
 * widgets' 30-minute refresh chain is deferred to Doze maintenance windows —
 * hours apart overnight — unless it may arm exact, so a widget placed without
 * the permission froze on its pre-dawn snapshot every morning. When the
 * permission is already granted the Dart side calls `finishConfigure`
 * immediately and the user never sees a screen; declining keeps the widget
 * (the gate finishes RESULT_OK either way — only backing out cancels).
 *
 * Shared by the three providers exactly as the beverage activity is shared by
 * its two: the bound provider tells them apart.
 */
class HomeStatusWidgetConfigureActivity : HomeWidgetConfigureActivity() {
    override fun widgetType(appWidgetId: Int): String {
        val provider = AppWidgetManager.getInstance(this)
            ?.getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
        return when {
            provider != null && provider.endsWith(BODY_ENERGY_RECEIVER) ->
                TYPE_BODY_ENERGY
            provider != null && provider.endsWith(TODAY_VITALS_RECEIVER) ->
                TYPE_TODAY_VITALS
            else -> TYPE_DAILY_READINESS
        }
    }

    private companion object {
        const val BODY_ENERGY_RECEIVER = "HomeBodyEnergyWidgetReceiver"
        const val TODAY_VITALS_RECEIVER = "HomeTodayVitalsWidgetReceiver"
    }
}

class HomeQuickBeverageWidgetConfigureActivity : HomeWidgetConfigureActivity() {
    override fun widgetType(appWidgetId: Int): String {
        val provider = AppWidgetManager.getInstance(this)
            ?.getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
        // The 2x1 is the safe default: it renders an Add button rather than
        // logging on any tap, so a misresolved id can never log a drink by itself.
        return if (provider != null && provider.endsWith(ONE_TAP_RECEIVER)) {
            TYPE_QUICK_BEVERAGE_ONE_TAP
        } else {
            TYPE_QUICK_BEVERAGE
        }
    }

    private companion object {
        const val ONE_TAP_RECEIVER = "HomeQuickBeverageOneTapWidgetReceiver"
    }
}
