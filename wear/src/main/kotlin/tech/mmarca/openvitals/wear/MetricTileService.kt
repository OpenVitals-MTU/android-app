package tech.mmarca.openvitals.wear

import android.content.ComponentName
import android.content.Context
import android.database.sqlite.SQLiteException
import android.graphics.BitmapFactory
import android.util.Log
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.ResourceBuilders.InlineImageResource
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

/**
 * One metric's tile: today's value and its curve, read from the
 * [MetricStore] off the main thread. A tile only reads: the recorders in
 * [WearAppService] write whether a tile is shown or not, and nothing here
 * starts a sensor. A tap opens [MainActivity].
 *
 * The curve is a PNG served by [onTileResourcesRequest] under the resources
 * version the tile names, the hash and size of its bytes. The last few PNGs
 * are kept in the cache directory under their versions, so a resources
 * request gets exactly the image its tile was built with, even after a newer
 * tile or a restart of the service. Each read opens its own [MetricStore]
 * and closes it, so nothing closes a store under a read that is still
 * running.
 */
abstract class MetricTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The metric's accent, the colour the phone app draws it in (`ui/theme/Color.kt`). */
    internal abstract val accent: Int

    /** The tile's title, also its title when the store cannot be read. */
    internal abstract val titleRes: Int

    /** Today's tile, read on the IO dispatcher. */
    internal abstract fun read(today: TodayReadings, chart: TileChart): MetricTile

    override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> =
        SuspendToFutureAdapter.launchFuture(scope.coroutineContext) {
            val device = requestParams.deviceConfiguration
            val tile = withContext(Dispatchers.IO) {
                val today = readToday(device)
                if (today.chart == null || keep(today.chart)) today else today.withoutChart()
            }
            val version = tile.chart?.let(::versionOf) ?: NO_CHART_VERSION
            val open = clickable(launchAction(ComponentName(this@MetricTileService, MainActivity::class.java)))
            val layout = materialScope(this@MetricTileService, device) {
                metricTileLayout(tile, open, device.screenDensity)
            }
            Tile.Builder()
                .setResourcesVersion(version)
                .setFreshnessIntervalMillis(FRESHNESS_MILLIS)
                .setTileTimeline(Timeline.fromLayoutElement(layout))
                .build()
        }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        SuspendToFutureAdapter.launchFuture(scope.coroutineContext) {
            val version = requestParams.version
            val png = if (version == NO_CHART_VERSION) null else withContext(Dispatchers.IO) { pngFor(requestParams) }
            Resources.Builder()
                .setVersion(version)
                .apply { png?.let { addIdToImageMapping(CHART_RESOURCE_ID, imageOf(it)) } }
                .build()
        }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    internal fun formatCount(count: Long): String = NumberFormat.getIntegerInstance().format(count)

    /** [value] as a share of [goal], in whole percent, with the goal as [goalText]: "68% of 8,000". */
    internal fun goalProgress(value: Double, goal: Double, goalText: String): String =
        getString(R.string.tile_goal_progress, (value / goal * PERCENT).toInt(), goalText)

    /** A tile without a value: a dash, [reason] as the caption, no curve. */
    internal fun unavailable(reason: String): MetricTile {
        val title = getString(titleRes)
        return MetricTile(title, TILE_NO_VALUE, reason, accent, "$title: $reason", chart = null)
    }

    /** Today's tile, or a dash when the store cannot be read, rather than the system's error tile. */
    private fun readToday(device: DeviceParameters): MetricTile {
        val density = device.screenDensity
        val chartHeightDp = (device.screenHeightDp * CHART_HEIGHT_SHARE).coerceIn(MIN_CHART_HEIGHT_DP, MAX_CHART_HEIGHT_DP)
        val chart = TileChart(
            widthPx = (device.screenWidthDp * CHART_WIDTH_SHARE * density).roundToInt(),
            heightPx = (chartHeightDp * density).roundToInt(),
            density = density,
            accent = accent,
        )
        val store = MetricStore(this)
        return try {
            read(TodayReadings(this, store, TileDay.of(System.currentTimeMillis(), ZoneId.systemDefault())), chart)
        } catch (e: SQLiteException) {
            Log.e(TAG, "Metric store unreadable", e)
            unavailable(getString(R.string.tile_read_failed))
        } finally {
            store.close()
        }
    }

    /**
     * The PNG kept under the requested version. If it is gone (the cache was
     * cleared), the tile is asked for again rather than drawn anew: a new
     * drawing ends at a later now, so it would not be that version's image.
     */
    private fun pngFor(requestParams: ResourcesRequest): ByteArray? =
        kept(requestParams.version) ?: run {
            getUpdater(this).requestUpdate(javaClass)
            null
        }

    /**
     * Keeps [chart] under its version for the resources request, written whole
     * before it gets its name, and drops all but this tile's [KEPT_VERSIONS]
     * newest, and temporary files a crash left behind. A failure only costs
     * the chart: false, and the tile goes without its curve, since a tile
     * naming a version with no file would only be asked for again.
     */
    private fun keep(chart: TileChart.Png): Boolean =
        try {
            val dir = File(cacheDir, CACHE_DIR).apply { mkdirs() }
            val file = File(dir, fileName(versionOf(chart)))
            if (!file.isFile) {
                // A temporary file of its own, so two requests drawing the same version never share one.
                val partial = File.createTempFile(file.name, PART_SUFFIX, dir)
                try {
                    partial.writeBytes(chart.bytes)
                    partial.renameTo(file)
                } finally {
                    partial.delete()
                }
            }
            file.setLastModified(System.currentTimeMillis())
            dir.listFiles { kept -> kept.name.startsWith(filePrefix) && kept.name.endsWith(PNG_SUFFIX) }
                ?.sortedByDescending(File::lastModified)
                ?.drop(KEPT_VERSIONS)
                ?.forEach(File::delete)
            val abandoned = System.currentTimeMillis() - ABANDONED_PART_MILLIS
            dir.listFiles { part -> part.name.startsWith(filePrefix) && part.name.endsWith(PART_SUFFIX) && part.lastModified() < abandoned }
                ?.forEach(File::delete)
            file.isFile
        } catch (e: IOException) {
            Log.w(TAG, "Chart not kept", e)
            false
        }

    /** The PNG kept under [version], or null when it is gone or cannot be read. */
    private fun kept(version: String): ByteArray? =
        try {
            File(File(cacheDir, CACHE_DIR), fileName(version)).takeIf(File::isFile)?.readBytes()
        } catch (e: IOException) {
            Log.w(TAG, "Kept chart unreadable", e)
            null
        }

    private val filePrefix: String get() = "${javaClass.simpleName}-"

    private fun fileName(version: String): String = "$filePrefix$version$PNG_SUFFIX"

    private fun versionOf(chart: TileChart.Png): String =
        "${chart.bytes.contentHashCode().toUInt().toString(RADIX)}-${chart.bytes.size.toString(RADIX)}"

    private fun imageOf(png: ByteArray): ImageResource {
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(png, 0, png.size, size)
        // No format set: a PNG of unspecified format is what every tile renderer decodes.
        return ImageResource.Builder()
            .setInlineResource(
                InlineImageResource.Builder()
                    .setData(png)
                    .setWidthPx(size.outWidth)
                    .setHeightPx(size.outHeight)
                    .build(),
            )
            .build()
    }

    internal companion object {
        /**
         * Asks Wear OS to draw the tiles that show what the phone sends
         * ([PhoneProfile]) again: the recorders' rows reach the tiles on
         * their own schedule, the phone's only when it syncs.
         */
        fun requestUpdates(context: Context) {
            val updater = getUpdater(context)
            for (tile in PHONE_PROFILE_TILES) updater.requestUpdate(tile)
        }

        private val PHONE_PROFILE_TILES = listOf(
            StepsTileService::class.java,
            DistanceTileService::class.java,
            ActiveCaloriesTileService::class.java,
        )

        /** The one image a metric tile has. */
        const val CHART_RESOURCE_ID = "chart"

        private const val TAG = "MetricTileService"
        private const val PERCENT = 100
        private const val NO_CHART_VERSION = "none"
        private const val CACHE_DIR = "tile-charts"
        private const val PNG_SUFFIX = ".png"
        private const val PART_SUFFIX = ".part"

        /** A temporary file this old was left by a write that never finished. */
        private const val ABANDONED_PART_MILLIS: Long = 10L * 60 * 1000

        /** Enough for a tile request that is overtaken by the next before its resources are asked for. */
        private const val KEPT_VERSIONS = 4
        private const val RADIX = 36

        /** How long a shown tile stays current; Wear OS asks again once it has passed. */
        private const val FRESHNESS_MILLIS: Long = 5L * 60 * 1000

        /** About the width the main slot leaves on a round screen. */
        private const val CHART_WIDTH_SHARE = 0.72f

        /** The curve's share of the screen height, so a small screen keeps room for the text around it. */
        private const val CHART_HEIGHT_SHARE = 0.24f
        private const val MIN_CHART_HEIGHT_DP = 40f
        private const val MAX_CHART_HEIGHT_DP = 56f
    }
}

/** A dash where a tile has no value; the caption says why. */
internal const val TILE_NO_VALUE = "–"
