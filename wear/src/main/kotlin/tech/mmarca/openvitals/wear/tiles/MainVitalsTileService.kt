package tech.mmarca.openvitals.wear.tiles

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import tech.mmarca.openvitals.wear.MainActivity
import tech.mmarca.openvitals.wear.R
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class MainVitalsTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(buildTileLayout(this, requestParams.deviceConfiguration))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val future = CompletableFuture<TileBuilders.Tile>()
        future.complete(tile)
        return SettableListenableFuture(future)
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()

        val future = CompletableFuture<ResourceBuilders.Resources>()
        future.complete(resources)
        return SettableListenableFuture(future)
    }

    private fun buildTileLayout(
        context: Context,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
    ): LayoutElementBuilders.LayoutElement {
        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(launchAction)
            .setId("open_app")
            .build()

        val clickModifiers = ModifiersBuilders.Modifiers.Builder()
            .setClickable(clickable)
            .build()

        val primaryColor = 0xFF10B981.toInt()

        val headerText = Text.Builder(context, context.getString(R.string.app_name))
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(ColorBuilders.argb(primaryColor))
            .build()

        val stepsText = Text.Builder(context, context.getString(R.string.dashboard_today))
            .setTypography(Typography.TYPOGRAPHY_TITLE2)
            .build()

        val chip = CompactChip.Builder(
            context,
            context.getString(R.string.dashboard_start_activity),
            clickable,
            deviceParameters,
        )
            .setChipColors(
                ChipColors(
                    ColorBuilders.argb(primaryColor),
                    ColorBuilders.argb(0xFF000000.toInt()),
                )
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setModifiers(clickModifiers)
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(headerText)
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(dp(8f))
                            .build()
                    )
                    .addContent(stepsText)
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(dp(12f))
                            .build()
                    )
                    .addContent(chip)
                    .build()
            )
            .build()
    }

    private class SettableListenableFuture<V>(
        private val delegate: CompletableFuture<V>,
    ) : ListenableFuture<V> {
        override fun addListener(listener: Runnable, executor: Executor) {
            delegate.thenAcceptAsync({ listener.run() }, executor)
        }
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = delegate.cancel(mayInterruptIfRunning)
        override fun isCancelled(): Boolean = delegate.isCancelled
        override fun isDone(): Boolean = delegate.isDone
        override fun get(): V = delegate.get()
        override fun get(timeout: Long, unit: TimeUnit): V = delegate.get(timeout, unit)
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
