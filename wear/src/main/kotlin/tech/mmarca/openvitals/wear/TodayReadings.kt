package tech.mmarca.openvitals.wear

import android.content.Context
import java.text.NumberFormat
import tech.mmarca.openvitals.wearlink.AscentCredit
import tech.mmarca.openvitals.wearlink.WearLinkProfile
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.HeartRateSample

/**
 * Today's values as the tiles and the watch-face complications show them,
 * from the [MetricStore] the recorders fill and the [PhoneProfile] the phone
 * sends: one place for the rules, so a watch face shows what a tile shows.
 * Reads on the caller's thread, which must not be the main one.
 *
 * Steps, distance and active calories are Health Connect's total as the
 * phone last sent it, every source together, plus what the watch recorded
 * after that sync, or the watch's own count for the day when that is larger
 * ([TileDay.shownTotal]). The running total is the watch's own, the only one
 * it has by the minute.
 */
internal class TodayReadings(private val context: Context, private val store: MetricStore, val day: TileDay) {

    /** A running total from midnight, one point per bucket so far, and the count to show. */
    class Total(val running: List<Double>, val shown: Double)

    /** The floors as a running total, and the metres that counted. */
    class Floors(val running: List<Double>, val metres: Double) {
        val floors: Long get() = TileDay.wholeDown(running.last())
    }

    /** The phone's settings, or null before the first sync with a phone that sends them. */
    val profile: WearLinkProfile.Profile? by lazy { PhoneProfile.profile(store) }

    private val phoneTotals: WearLinkProfile.Totals? by lazy { PhoneProfile.totals(store, day) }

    private val stepMinutes by lazy { store.since(WearMetrics.STEPS, day.startMillis - 1, TileDay.MAX_MINUTES_PER_DAY) }

    /** Why the watch has no steps to count, as a string resource, or null when it has. Steps, distance and calories all need them. */
    fun stepsMissing(): Int? = when {
        !StepRecorder.hasStepCounter(context) -> R.string.tile_steps_no_sensor
        !WearPermissions.hasActivityRecognition(context) -> R.string.tile_not_granted
        else -> null
    }

    fun steps(): Total = total(TileDay.stepAmounts(stepMinutes), phoneTotals?.steps)

    /** In metres: the watch's steps times the phone's step length. */
    fun distance(profile: WearLinkProfile.Profile): Total =
        total(TileDay.distanceAmounts(stepMinutes, profile.strideMillimetres), phoneTotals?.distanceMetres)

    /**
     * In active kilocalories: the walking energy of the watch's step minutes
     * for the phone's body weight, the estimate the phone writes. The phone
     * leaves out the minutes of a recorded workout and the watch does not
     * know them, so during a workout the watch's count runs ahead until the
     * next sync.
     */
    fun activeKcal(weightGrams: Int): Total =
        total(TileDay.walkingKcalAmounts(stepMinutes, weightGrams), phoneTotals?.activeKcal)

    /** Why the watch has no floors to count, as a string resource, or null when it has. */
    fun floorsMissing(): Int? = when {
        !AscentRecorder.hasBarometer(context) -> R.string.tile_floors_no_sensor
        // Height counts only with steps around it, so floors need the step counter too.
        else -> stepsMissing()
    }

    /**
     * The barometer's metres that enough steps carry, over three metres a
     * floor, as the phone counts them before it writes them to Health Connect.
     */
    fun floors(): Floors {
        val ascent = store.since(WearMetrics.ASCENT, day.startMillis - 1, TileDay.MAX_MINUTES_PER_DAY)
        // The minute before midnight carries the first minute's credit.
        val steps = store.since(WearMetrics.STEPS, day.startMillis - TileDay.MINUTE_MILLIS - 1, TileDay.MAX_MINUTES_PER_DAY + 1)
        val metres = day.cumulative(TileDay.creditedAscent(ascent, steps))
        return Floors(metres.map { it / AscentCredit.METRES_PER_FLOOR }, metres.last())
    }

    val heartRateGranted: Boolean by lazy { WearPermissions.hasHeartRate(context) }

    /** The newest heart rate and its age; it can be from before midnight, as long as it is under an hour old. */
    fun heartRateNow(): TileHeartRate =
        TileHeartRate.of(heartRateGranted, store.latest(WearMetrics.HEART_RATE), day.nowMillis)

    /** The newest sample, whenever it was taken; null without the grant or before the first. */
    fun latestHeartRate(): HeartRateSample? = if (heartRateGranted) store.latest(WearMetrics.HEART_RATE) else null

    /** Today's samples, empty without the grant. */
    fun heartRateToday(): List<HeartRateSample> =
        if (heartRateGranted) {
            store.since(WearMetrics.HEART_RATE, day.startMillis - 1, TileDay.MAX_HEART_RATE_SAMPLES_PER_DAY)
        } else {
            emptyList()
        }

    private fun total(amounts: List<Pair<Long, Double>>, phone: Long?): Total {
        val running = day.cumulative(amounts)
        return Total(running, day.shownTotal(running.last(), amounts, phone, phoneTotals?.asOfEpochMillis))
    }
}

/** Kilometres or miles, as the phone app shows distance. */
internal enum class DistanceUnits(private val metresPerUnit: Double, val caption: Int, val amount: Int) {
    KILOMETRES(1_000.0, R.string.tile_distance_caption_km, R.string.tile_amount_km),
    MILES(1_609.344, R.string.tile_distance_caption_mi, R.string.tile_amount_mi),
    ;

    /** Two decimals under ten, one above: 4.89, 12.4; a value that rounds up to ten gets one: 10.0, not 10.00. */
    fun format(metres: Double): String {
        val value = metres / metresPerUnit
        val digits = if (Math.round(value * HUNDREDTHS) / HUNDREDTHS < TWO_DECIMALS_BELOW) 2 else 1
        return NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }.format(value)
    }

    /** A goal as the user set it: 5, 3.1. */
    fun formatGoal(metres: Int): String =
        NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(metres / metresPerUnit)

    companion object {
        private const val TWO_DECIMALS_BELOW = 10.0
        private const val HUNDREDTHS = 100.0

        fun of(units: WearLinkProfile.Units): DistanceUnits = when (units) {
            WearLinkProfile.Units.METRIC -> KILOMETRES
            WearLinkProfile.Units.IMPERIAL -> MILES
        }
    }
}
