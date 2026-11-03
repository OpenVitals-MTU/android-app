package tech.mmarca.openvitals.wear

/*
 * The steps, distance and active calories tiles: today's count as
 * [TodayReadings] gives it, the larger of the phone's total and the watch's
 * own, with the watch's own curve and the share of the daily goal. Distance
 * and walking energy need the phone's step length and weight, so they wait
 * for the first sync with a phone that sends them. The accents copy the
 * phone app's `ui/theme/Color.kt`.
 */

/** The steps since midnight, their running total, and the share of the daily goal. */
class StepsTileService : MetricTileService() {

    /** `StepsColor`. */
    override val accent: Int = 0xFF3F9A63.toInt()

    override val titleRes: Int = R.string.tile_steps_label

    override fun read(today: TodayReadings, chart: TileChart): MetricTile {
        today.stepsMissing()?.let { return unavailable(getString(it)) }
        val steps = today.steps()
        val title = getString(titleRes)
        val value = formatCount(steps.shown.toLong())
        val caption = getString(R.string.tile_steps_caption)
        val detail = today.profile?.let { profile ->
            goalProgress(steps.shown, profile.goalSteps.toDouble(), formatCount(profile.goalSteps.toLong()))
        }
        return MetricTile(
            title = title,
            value = value,
            caption = caption,
            accent = accent,
            description = listOfNotNull("$title: $value $caption", detail).joinToString(", "),
            chart = chart.cumulative(steps.running, today.day),
            detail = detail,
        )
    }
}

/** The distance walked since midnight: the watch's steps times the phone's step length, in the phone's units. */
class DistanceTileService : MetricTileService() {

    /** `DistanceColor`. */
    override val accent: Int = 0xFF3B7DD8.toInt()

    override val titleRes: Int = R.string.tile_distance_label

    override fun read(today: TodayReadings, chart: TileChart): MetricTile {
        today.stepsMissing()?.let { return unavailable(getString(it)) }
        val profile = today.profile ?: return unavailable(getString(R.string.tile_needs_phone))
        val distance = today.distance(profile)
        val units = DistanceUnits.of(profile.units)
        val title = getString(titleRes)
        val value = units.format(distance.shown)
        val caption = getString(units.caption)
        val goal = getString(units.amount, units.formatGoal(profile.goalDistanceMetres))
        val detail = goalProgress(distance.shown, profile.goalDistanceMetres.toDouble(), goal)
        return MetricTile(
            title = title,
            value = value,
            caption = caption,
            accent = accent,
            description = "$title: $value $caption, $detail",
            chart = chart.cumulative(distance.running, today.day),
            detail = detail,
        )
    }
}

/** The walking energy since midnight ([TodayReadings.activeKcal]) and the share of the daily goal. */
class ActiveCaloriesTileService : MetricTileService() {

    /** `ActiveCaloriesColor`. */
    override val accent: Int = 0xFFDE6C39.toInt()

    override val titleRes: Int = R.string.tile_active_calories_label

    override fun read(today: TodayReadings, chart: TileChart): MetricTile {
        today.stepsMissing()?.let { return unavailable(getString(it)) }
        val profile = today.profile ?: return unavailable(getString(R.string.tile_needs_phone))
        val weightGrams = profile.weightGrams ?: return unavailable(getString(R.string.tile_needs_weight))
        val kcal = today.activeKcal(weightGrams)
        val title = getString(titleRes)
        val value = formatCount(TileDay.wholeDown(kcal.shown))
        val caption = getString(R.string.tile_active_calories_caption)
        val goal = getString(R.string.tile_amount_kcal, formatCount(profile.goalActiveKcal.toLong()))
        val detail = goalProgress(kcal.shown, profile.goalActiveKcal.toDouble(), goal)
        return MetricTile(
            title = title,
            value = value,
            caption = caption,
            accent = accent,
            description = "$title: $value $caption, $detail",
            chart = chart.cumulative(kcal.running, today.day),
            detail = detail,
        )
    }
}
