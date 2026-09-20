package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import tech.mmarca.openvitals.healthconnect.withStrictHealthConnectReads

/** Outlives a broadcast. For widget work that must not hold a receiver. */
internal val HomeWidgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Every receiver that draws a tile from stored data. Listed, so an omission is visible. */
internal val HomeWidgetReceivers: List<Class<*>> = listOf(
    HomeMetricWidgetReceiver::class.java,
    HomeDailyReadinessWidgetReceiver::class.java,
    HomeBodyEnergyWidgetReceiver::class.java,
    HomeTodayVitalsWidgetReceiver::class.java,
    HomeQuickBeverageWidgetReceiver::class.java,
    HomeQuickBeverageOneTapWidgetReceiver::class.java,
)

/** One tile's in-process refresh, per receiver. A test holds this to [HomeWidgetReceivers]. */
internal val HomeWidgetRefreshers: Map<Class<*>, suspend (Context, Int) -> Unit> = mapOf(
    HomeMetricWidgetReceiver::class.java to { context, id -> refreshHomeMetricWidget(context, id) },
    HomeDailyReadinessWidgetReceiver::class.java to { context, id -> refreshDailyReadinessWidget(context, id) },
    HomeBodyEnergyWidgetReceiver::class.java to { context, id -> refreshBodyEnergyWidget(context, id) },
    HomeTodayVitalsWidgetReceiver::class.java to { context, id -> refreshTodayVitalsWidget(context, id) },
    HomeQuickBeverageWidgetReceiver::class.java to { context, id -> refreshHomeQuickBeverageWidget(context, id) },
    HomeQuickBeverageOneTapWidgetReceiver::class.java to { context, id -> refreshHomeQuickBeverageWidget(context, id) },
)

/** Receivers whose tile comes from a Health Connect read. The beverage tiles come from preferences. */
internal val HealthConnectBackedWidgetReceivers: Set<Class<*>> = setOf(
    HomeMetricWidgetReceiver::class.java,
    HomeDailyReadinessWidgetReceiver::class.java,
    HomeBodyEnergyWidgetReceiver::class.java,
    HomeTodayVitalsWidgetReceiver::class.java,
)

/**
 * The tiles a background refresh may redraw. When Health Connect would only show this app's
 * own records, a Health Connect tile keeps what it shows: a fresh "0 steps" is worse than an
 * hour-old number.
 */
internal fun homeWidgetsToRefresh(
    placed: Map<Class<*>, IntArray>,
    readsOtherAppsData: Boolean,
): Map<Class<*>, IntArray> =
    if (readsOtherAppsData) placed else placed.filterKeys { it !in HealthConnectBackedWidgetReceivers }

/** The placed widget ids per receiver. Receivers with no placed widget are absent. */
internal fun placedHomeWidgetIds(context: Context): Map<Class<*>, IntArray> {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext) ?: return emptyMap()
    return buildMap {
        for (receiver in HomeWidgetReceivers) {
            val component = ComponentName(appContext, receiver)
            val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull()
            if (ids != null && ids.isNotEmpty()) put(receiver, ids)
        }
    }
}

fun anyHomeWidgetPlaced(context: Context): Boolean = placedHomeWidgetIds(context).isNotEmpty()

/**
 * Tells every placed widget to redraw after data landed from outside the
 * app; the periodic tick is not honoured in Doze. Returns at once when no
 * widget is placed. Fire-and-forget.
 */
fun refreshPlacedHomeWidgets(context: Context) {
    val appContext = context.applicationContext
    for ((receiver, ids) in placedHomeWidgetIds(appContext)) {
        val component = ComponentName(appContext, receiver)
        runCatching {
            appContext.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    this.component = component
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}

/**
 * Re-reads and redraws every placed widget on the caller's coroutine. For
 * WorkManager, which has minutes where a receiver has seconds. One failed
 * tile keeps its last snapshot and does not stop the rest.
 */
suspend fun refreshPlacedHomeWidgetsInProcess(context: Context) {
    val appContext = context.applicationContext
    val readsOtherAppsData = EntryPointAccessors
        .fromApplication(appContext, HomeWidgetRefreshWorkerEntryPoint::class.java)
        .healthConnectManager()
        .readsOtherAppsDataNow()
    if (!readsOtherAppsData) {
        Log.i(HomeWidgetLogTag, "Health Connect tiles kept: background read is not granted")
    }
    for ((receiver, ids) in homeWidgetsToRefresh(placedHomeWidgetIds(appContext), readsOtherAppsData)) {
        val refresh = HomeWidgetRefreshers[receiver] ?: continue
        for (id in ids) {
            try {
                // Strict: a read that fails must throw, so the tile keeps its last snapshot.
                // A swallowed failure used to redraw it as "0" and "No data".
                withStrictHealthConnectReads { refresh(appContext, id) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Log.e(HomeWidgetLogTag, "Home widget $id refresh failed", t)
            }
        }
    }
}
