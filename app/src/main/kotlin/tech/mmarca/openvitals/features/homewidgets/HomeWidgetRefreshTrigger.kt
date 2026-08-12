package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Every receiver that draws a home-screen tile from stored health data.
 *
 * Listed rather than discovered so that adding a widget without adding it here
 * is a visible omission rather than a tile that silently stops updating.
 */
internal val HomeWidgetReceivers: List<Class<*>> = listOf(
    HomeMetricWidgetReceiver::class.java,
    HomeDailyReadinessWidgetReceiver::class.java,
    HomeBodyEnergyWidgetReceiver::class.java,
    HomeTodayVitalsWidgetReceiver::class.java,
    HomeQuickBeverageWidgetReceiver::class.java,
    HomeQuickBeverageOneTapWidgetReceiver::class.java,
)

/**
 * Tells every placed widget to redraw, after data landed from outside the app.
 *
 * A widget's own periodic tick is `updatePeriodMillis`, which the system honours
 * at its convenience and not at all in Doze. So a Garmin sync at 08:05 or a
 * finished Apple Health import leaves the morning's tiles showing pre-sync
 * numbers for up to half an hour, and in practice often longer — the user is
 * looking at their home screen having just watched the sync succeed.
 *
 * Returns without touching anything when no OpenVitals widget is placed, which
 * is the common case: this fires after every sync, and enumerating ids is
 * cheap where the six dashboard loads a refresh costs are not.
 *
 * Fire-and-forget by design. The data has already reached Health Connect, and a
 * failure to repaint a tile must not be reported as a failed sync.
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
