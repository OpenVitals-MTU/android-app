package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException

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
    for ((receiver, ids) in placedHomeWidgetIds(appContext)) {
        val refresh = HomeWidgetRefreshers[receiver] ?: continue
        for (id in ids) {
            try {
                refresh(appContext, id)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Log.e(HomeWidgetLogTag, "Home widget $id refresh failed", t)
            }
        }
    }
}
