package tech.mmarca.openvitals.wear.tiles

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.EdgeContentLayout
import androidx.wear.protolayout.material.layouts.MultiButtonLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future
import tech.mmarca.openvitals.wear.MainActivity
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.WearPreferences
import tech.mmarca.openvitals.wear.WearRoutes
import tech.mmarca.openvitals.wear.features.measure.Measurement
import tech.mmarca.openvitals.wear.features.quicklog.EntryLog
import tech.mmarca.openvitals.wear.features.quicklog.EntryType
import tech.mmarca.openvitals.wear.features.quicklog.LoggedEntry
import tech.mmarca.openvitals.wear.features.quicklog.WaterServingMl
import tech.mmarca.openvitals.wear.health.CapabilityProbe
import tech.mmarca.openvitals.wear.health.WearCapabilities
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.HydrationColor

/**
 * A tile for logging without opening the app: a ring of today's water,
 * one button that adds a glass right on the tile, and shortcuts into the
 * app for a heart rate measurement (only on a watch that can take one) and
 * the full quick log.
 */
class QuickLogTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        scope.future {
            val entryLog = EntryLog(this@QuickLogTileService)
            val clicks = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            // The add-water id carries a counter, so a later request that repeats the
            // last clicked id (a refresh, not a tap) cannot log the same glass twice.
            val clickCount = clicks.getInt(KEY_CLICK_COUNT, 0)
            if (requestParams.currentState.lastClickableId == addWaterId(clickCount)) {
                entryLog.add(
                    LoggedEntry(EntryType.WATER, System.currentTimeMillis(), WaterServingMl),
                    refreshTile = false,
                )
                clicks.edit { putInt(KEY_CLICK_COUNT, clickCount + 1) }
            }
            val capabilities = cachedCapabilities ?: CapabilityProbe(this@QuickLogTileService).probe()
                .also { cachedCapabilities = it }
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val waterToday = entryLog.load()
                .filter { it.type == EntryType.WATER }
                .filter { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() == today }
                .sumOf { it.value }

            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTileTimeline(
                    Timeline.fromLayoutElement(
                        layout(
                            context = this@QuickLogTileService,
                            deviceParameters = requestParams.deviceConfiguration,
                            waterToday = waterToday,
                            capabilities = capabilities,
                            addWaterId = addWaterId(clicks.getInt(KEY_CLICK_COUNT, 0)),
                        ),
                    ),
                )
                .build()
        }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .addImage(ICON_WATER, R.drawable.ic_tile_water)
            .addImage(ICON_HEART, R.drawable.ic_tile_heart)
            .addImage(ICON_ADD, R.drawable.ic_tile_add)
            .build()
    }

    private fun layout(
        context: Context,
        deviceParameters: DeviceParameters,
        waterToday: Double,
        capabilities: WearCapabilities,
        addWaterId: String,
    ): LayoutElement {
        val preferences = WearPreferences()
        val unitSystem = UnitSystem.METRIC
        val goal = preferences.waterGoalMl.toDouble()
        val hydration = HydrationColor.toArgb()
        val unit = context.getString(R.string.unit_ml)

        val buttons = MultiButtonLayout.Builder()
            .addButtonContent(
                iconButton(
                    context,
                    Clickable.Builder().setId(addWaterId).setOnClick(ActionBuilders.LoadAction.Builder().build()).build(),
                    ICON_WATER,
                    hydration,
                    context.getString(
                        R.string.quicklog_add_water,
                        formatMetricValue(WearMetric.HYDRATION, WaterServingMl, unitSystem),
                        unit,
                    ),
                ),
            )
            .apply {
                if (Measurement.HEART_RATE in capabilities.measurements) {
                    addButtonContent(
                        iconButton(
                            context,
                            openApp(context, WearRoutes.measurement(Measurement.HEART_RATE)),
                            ICON_HEART,
                            HeartColor.toArgb(),
                            context.getString(R.string.measure_heart_rate),
                        ),
                    )
                }
            }
            .addButtonContent(
                iconButton(
                    context,
                    openApp(context, WearRoutes.QUICK_LOG),
                    ICON_ADD,
                    TonalButton,
                    context.getString(R.string.quicklog_title),
                ),
            )
            .build()

        return EdgeContentLayout.Builder(deviceParameters)
            .setResponsiveContentInsetEnabled(true)
            .setEdgeContent(
                CircularProgressIndicator.Builder()
                    .setProgress((waterToday / goal).toFloat().coerceIn(0f, 1f))
                    .setCircularProgressIndicatorColors(
                        ProgressIndicatorColors(hydration, ColorUtils.setAlphaComponent(hydration, TrackAlpha)),
                    )
                    .build(),
            )
            .setPrimaryLabelTextContent(
                Text.Builder(context, context.getString(R.string.metric_hydration))
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(hydration))
                    .build(),
            )
            .setContent(buttons)
            .setSecondaryLabelTextContent(
                Text.Builder(
                    context,
                    context.getString(
                        R.string.quicklog_water_today,
                        formatMetricValue(WearMetric.HYDRATION, waterToday, unitSystem),
                        formatMetricValue(WearMetric.HYDRATION, goal, unitSystem),
                        unit,
                    ),
                )
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .build(),
            )
            .build()
    }

    private fun iconButton(
        context: Context,
        clickable: Clickable,
        icon: String,
        background: Int,
        description: String,
    ): LayoutElement =
        Button.Builder(context, clickable)
            .setIconContent(icon)
            .setContentDescription(description)
            .setButtonColors(ButtonColors(background, OnButton))
            .build()

    private fun openApp(context: Context, route: String): Clickable =
        Clickable.Builder()
            .setId("open_$route")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(context.packageName)
                            .setClassName(MainActivity::class.java.name)
                            .addKeyToExtraMapping(
                                MainActivity.EXTRA_ROUTE,
                                ActionBuilders.AndroidStringExtra.Builder().setValue(route).build(),
                            )
                            .build(),
                    )
                    .build(),
            )
            .build()

    private fun ResourceBuilders.Resources.Builder.addImage(id: String, @DrawableRes resId: Int) =
        addIdToImageMapping(
            id,
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(resId).build(),
                )
                .build(),
        )

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val PREFS_NAME = "quick_log_tile"
        const val KEY_CLICK_COUNT = "add_water_clicks"
        const val ICON_WATER = "water"
        const val ICON_HEART = "heart"
        const val ICON_ADD = "add"
        const val TrackAlpha = 0x38
        const val TonalButton = 0xFF2B2D30.toInt()
        const val OnButton = 0xFFFFFFFF.toInt()

        /** Capabilities do not change while the watch runs; probing once per process is enough. */
        var cachedCapabilities: WearCapabilities? = null

        fun addWaterId(clickCount: Int) = "add_water_$clickCount"
    }
}
