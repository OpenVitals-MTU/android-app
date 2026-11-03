package tech.mmarca.openvitals.wear

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import java.time.Instant

/*
 * The metric complications. Each gives what its tile gives: the heart rate
 * and the floors as a short text, the steps, distance and active calories as
 * a short text or as their share of the daily goal. The heart rate and the
 * distance say their unit in the value, and have a second complication with
 * the bare number, for a slot too narrow for both. A metric with
 * nothing to show, a permission off, a sensor missing, or no profile from the
 * phone yet, gives no data, and the watch face shows its own empty state.
 * A day's count is valid until midnight, so yesterday's never lingers.
 */

/**
 * The newest heart rate, no older than [FRESH_MINUTES]: unlike the tile, a
 * watch face shows the number without its age, so an old one would pass for
 * now. It is valid until it turns that old.
 */
open class HeartRateComplicationService : MetricComplicationService() {

    /** Whether the value says "bpm"; a screen reader always hears it. */
    internal open val withUnit: Boolean = true

    override val iconRes: Int = R.drawable.ic_metric_heart_rate

    override fun read(today: TodayReadings, type: ComplicationType): ComplicationData? {
        val sample = today.latestHeartRate() ?: return null
        val validUntil = Instant.ofEpochMilli(sample.epochMillis).plusSeconds(FRESH_MINUTES * SECONDS_PER_MINUTE)
        if (!validUntil.isAfter(Instant.ofEpochMilli(today.day.nowMillis))) return null
        return heartRate(sample.bpm.toString(), type, validUntil)
    }

    override fun preview(type: ComplicationType): ComplicationData? = heartRate(PREVIEW_BPM, type, validUntil = null)

    private fun heartRate(bpm: String, type: ComplicationType, validUntil: Instant?): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val value = getString(R.string.complication_value_bpm, bpm)
        return shortText(if (withUnit) value else bpm, unit = null, "${getString(R.string.tile_heart_rate_label)}: $value", validUntil)
    }

    private companion object {
        const val PREVIEW_BPM = "72"
        const val FRESH_MINUTES = 10L
        const val SECONDS_PER_MINUTE = 60L
    }
}

/** The heart rate as the bare number. */
class HeartRateNumberComplicationService : HeartRateComplicationService() {
    override val withUnit: Boolean = false
}

/**
 * Today's steps, and their share of the daily goal: the phone's once it sent
 * one, until then the phone app's default, 8,000 (`MetricDailyGoalKey.STEPS`),
 * so a ranged slot is not empty before the first sync.
 */
class StepsComplicationService : MetricComplicationService() {

    override val iconRes: Int = R.drawable.ic_metric_steps

    override fun read(today: TodayReadings, type: ComplicationType): ComplicationData? {
        if (today.stepsMissing() != null) return null
        val goal = today.profile?.goalSteps ?: DEFAULT_GOAL
        return steps(today.steps().shown, goal, type, today.day.endInstant)
    }

    override fun preview(type: ComplicationType): ComplicationData? = steps(PREVIEW_STEPS, DEFAULT_GOAL, type, validUntil = null)

    private fun steps(steps: Double, goal: Int, type: ComplicationType, validUntil: Instant?): ComplicationData? {
        val value = formatCount(steps.toLong())
        val unit = getString(R.string.complication_title_steps)
        val description = "$value ${getString(R.string.tile_steps_caption)}"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(value, unit, description, validUntil)
            ComplicationType.RANGED_VALUE -> goalRange(
                steps, goal.toDouble(), value, unit,
                goalDescription(description, steps, goal.toDouble(), formatCount(goal.toLong())), validUntil,
            )
            else -> null
        }
    }

    private companion object {
        const val PREVIEW_STEPS = 6_540.0
        const val DEFAULT_GOAL = 8_000
    }
}

/** Today's distance in the unit the phone shows distances in, and its share of the daily goal. */
open class DistanceComplicationService : MetricComplicationService() {

    /** Whether the value says its unit; a screen reader always hears it. */
    internal open val withUnit: Boolean = true

    override val iconRes: Int = R.drawable.ic_metric_distance

    override fun read(today: TodayReadings, type: ComplicationType): ComplicationData? {
        if (today.stepsMissing() != null) return null
        val profile = today.profile ?: return null
        val units = DistanceUnits.of(profile.units)
        return distance(today.distance(profile).shown, profile.goalDistanceMetres, units, type, today.day.endInstant)
    }

    override fun preview(type: ComplicationType): ComplicationData? =
        distance(PREVIEW_METRES, PREVIEW_GOAL_METRES, DistanceUnits.KILOMETRES, type, validUntil = null)

    private fun distance(metres: Double, goalMetres: Int, units: DistanceUnits, type: ComplicationType, validUntil: Instant?): ComplicationData? {
        val value = units.format(metres)
        val text = if (withUnit) getString(units.amount, value) else value
        val description = "$value ${getString(units.caption)}"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(text, unit = null, description, validUntil)
            ComplicationType.RANGED_VALUE -> goalRange(
                metres, goalMetres.toDouble(), text, unit = null,
                goalDescription(description, metres, goalMetres.toDouble(), getString(units.amount, units.formatGoal(goalMetres))),
                validUntil,
            )
            else -> null
        }
    }

    private companion object {
        const val PREVIEW_METRES = 4_890.0
        const val PREVIEW_GOAL_METRES = 5_000
    }
}

/** Today's distance as the bare number, and its share of the daily goal. */
class DistanceNumberComplicationService : DistanceComplicationService() {
    override val withUnit: Boolean = false
}

/** Today's walking energy, and its share of the daily goal. */
class ActiveCaloriesComplicationService : MetricComplicationService() {

    override val iconRes: Int = R.drawable.ic_metric_active_calories

    override fun read(today: TodayReadings, type: ComplicationType): ComplicationData? {
        if (today.stepsMissing() != null) return null
        val profile = today.profile ?: return null
        val weightGrams = profile.weightGrams ?: return null
        return calories(today.activeKcal(weightGrams).shown, profile.goalActiveKcal, type, today.day.endInstant)
    }

    override fun preview(type: ComplicationType): ComplicationData? = calories(PREVIEW_KCAL, PREVIEW_GOAL_KCAL, type, validUntil = null)

    private fun calories(kcal: Double, goal: Int, type: ComplicationType, validUntil: Instant?): ComplicationData? {
        val value = formatCount(TileDay.wholeDown(kcal))
        val unit = getString(R.string.complication_title_kcal)
        val description = "$value ${getString(R.string.tile_active_calories_caption)}"
        return when (type) {
            ComplicationType.SHORT_TEXT -> shortText(value, unit, description, validUntil)
            ComplicationType.RANGED_VALUE -> goalRange(
                kcal, goal.toDouble(), value, unit,
                goalDescription(description, kcal, goal.toDouble(), getString(R.string.tile_amount_kcal, formatCount(goal.toLong()))),
                validUntil,
            )
            else -> null
        }
    }

    private companion object {
        const val PREVIEW_KCAL = 312.0
        const val PREVIEW_GOAL_KCAL = 400
    }
}

/** Today's floors, counted as the phone counts them. */
class FloorsComplicationService : MetricComplicationService() {

    override val iconRes: Int = R.drawable.ic_metric_floors

    override fun read(today: TodayReadings, type: ComplicationType): ComplicationData? {
        if (today.floorsMissing() != null) return null
        return floors(formatCount(today.floors().floors), type, today.day.endInstant)
    }

    override fun preview(type: ComplicationType): ComplicationData? = floors(PREVIEW_FLOORS, type, validUntil = null)

    private fun floors(value: String, type: ComplicationType, validUntil: Instant?): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return shortText(value, getString(R.string.complication_title_floors), "$value ${getString(R.string.tile_floors_caption)}", validUntil)
    }

    private companion object {
        const val PREVIEW_FLOORS = "8"
    }
}

/** The end of the day, local midnight, as an instant: when a day's count stops being valid. */
private val TileDay.endInstant: Instant get() = Instant.ofEpochMilli(endMillis)
