package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Every receiver that draws a tile from stored data. Listed, so an omission is visible. */
internal val HomeWidgetReceivers: List<Class<*>> = listOf(
    HomeMetricWidgetReceiver::class.java,
    HomeDailyReadinessWidgetReceiver::class.java,
    HomeBodyEnergyWidgetReceiver::class.java,
    HomeTodayVitalsWidgetReceiver::class.java,
    HomeQuickBeverageWidgetReceiver::class.java,
    HomeQuickBeverageOneTapWidgetReceiver::class.java,
)

/**
 * Tells every placed widget to redraw after data landed from outside the
 * app; the periodic tick is not honoured in Doze. Returns at once when no
 * widget is placed. Fire-and-forget.
 */
fun refreshPlacedHomeWidgets(context: Context) {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext) ?: return
    for (receiver in HomeWidgetReceivers) {
        val component = ComponentName(appContext, receiver)
        val ids = runCatching { manager.getAppWidgetIds(component) }.getOrNull()
        if (ids == null || ids.isEmpty()) continue
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
