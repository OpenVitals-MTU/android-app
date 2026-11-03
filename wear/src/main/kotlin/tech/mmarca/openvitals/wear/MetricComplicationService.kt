package tech.mmarca.openvitals.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeRange
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One metric for watch faces: today's value as a short text, and for the
 * metrics with a daily goal its share as a ranged value. The values come from
 * [TodayReadings], so a watch face shows what the tile shows. Each request
 * opens its own [MetricStore] off the main thread and closes it; nothing here
 * starts a sensor. A tap opens [MainActivity].
 *
 * Wear OS asks again every five minutes (the manifest's
 * `UPDATE_PERIOD_SECONDS`, its shortest), and at once when the phone sends
 * its settings or today's totals ([requestUpdates]).
 */
abstract class MetricComplicationService : SuspendingComplicationDataSourceService() {

    /** The metric's icon, for a watch face that shows one with the value. */
    internal abstract val iconRes: Int

    /** Today's value as [type], or null when there is none to show. Valid until [TodayReadings.day] ends unless it says otherwise. */
    internal abstract fun read(today: TodayReadings, type: ComplicationType): ComplicationData?

    /** What the watch face editor shows before the complication is placed. */
    internal abstract fun preview(type: ComplicationType): ComplicationData?

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        withContext(Dispatchers.IO) {
            val store = MetricStore(this@MetricComplicationService)
            try {
                val day = TileDay.of(System.currentTimeMillis(), ZoneId.systemDefault())
                read(TodayReadings(this@MetricComplicationService, store, day), request.complicationType)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No data rather than the watch face's error state; the next request tries again.
                Log.e(TAG, "Complication not read", e)
                null
            } finally {
                store.close()
            }
        } ?: NoDataComplicationData()

    override fun getPreviewData(type: ComplicationType): ComplicationData? = preview(type)

    /**
     * [value] with [unit] as its title, shown until [validUntil] (null: always,
     * for a preview). A day's count ends at midnight, so a watch face never
     * carries yesterday's into today. A watch face can show the icon instead
     * of the title, so a value that needs its unit carries it and has no title.
     */
    internal fun shortText(value: String, unit: String?, description: String, validUntil: Instant?): ShortTextComplicationData =
        ShortTextComplicationData.Builder(text(value), text(description))
            .setTitle(unit?.let(::text))
            .setMonochromaticImage(icon())
            .setTapAction(openApp())
            .setValidTimeRange(validUntil?.let { TimeRange.before(it) })
            .build()

    /** [value] of [goal] as a ring or bar, with the value and [unit] as its text, shown until [validUntil]. */
    internal fun goalRange(
        value: Double,
        goal: Double,
        valueText: String,
        unit: String?,
        description: String,
        validUntil: Instant?,
    ): RangedValueComplicationData =
        RangedValueComplicationData.Builder(value.toFloat().coerceIn(0f, goal.toFloat()), 0f, goal.toFloat(), text(description))
            .setText(text(valueText))
            .setTitle(unit?.let(::text))
            .setMonochromaticImage(icon())
            .setTapAction(openApp())
            .setValidTimeRange(validUntil?.let { TimeRange.before(it) })
            .build()

    /** "6,540 steps today, 82% of 8,000": the value, and the goal's share, as a screen reader says it. */
    internal fun goalDescription(valueDescription: String, value: Double, goal: Double, goalText: String): String =
        "$valueDescription, ${getString(R.string.tile_goal_progress, (value / goal * PERCENT).toInt(), goalText)}"

    internal fun formatCount(count: Long): String = NumberFormat.getIntegerInstance().format(count)

    private fun text(value: String) = PlainComplicationText.Builder(value).build()

    private fun icon() = MonochromaticImage.Builder(Icon.createWithResource(this, iconRes)).build()

    private fun openApp(): PendingIntent =
        PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    internal companion object {
        private const val TAG = "MetricComplication"

        private const val PERCENT = 100

        /** Asks Wear OS to fetch the complications that show the phone's data again: steps, distance, active calories. */
        fun requestUpdates(context: Context) {
            for (service in PHONE_PROFILE_COMPLICATIONS) {
                ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, service)).requestUpdateAll()
            }
        }

        private val PHONE_PROFILE_COMPLICATIONS = listOf(
            StepsComplicationService::class.java,
            DistanceComplicationService::class.java,
            DistanceNumberComplicationService::class.java,
            ActiveCaloriesComplicationService::class.java,
        )
    }
}
