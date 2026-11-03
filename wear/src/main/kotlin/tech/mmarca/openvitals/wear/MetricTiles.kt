package tech.mmarca.openvitals.wear

/*
 * The heart rate and floors tiles; the steps, distance and active calories
 * tiles are in `MovementTiles.kt`. Each reads today's values from
 * [TodayReadings] and says why when it has nothing to show: a permission
 * that is off, or a sensor the watch lacks. The accents copy the phone app's `ui/theme/Color.kt`;
 * a change to that palette belongs here too.
 */

/**
 * The newest heart rate, the day's quarter-hour means, and its range. The
 * newest sample can be from before midnight, as long as it is under an hour
 * old ([TileHeartRate]); the curve and the range are today's only.
 */
class HeartRateTileService : MetricTileService() {

    /** `HeartColor`. */
    override val accent: Int = 0xFFD2497B.toInt()

    override val titleRes: Int = R.string.tile_heart_rate_label

    override fun read(today: TodayReadings, chart: TileChart): MetricTile {
        val day = today.day
        val samples = today.heartRateToday()
        val now = today.heartRateNow()
        val (value, caption) = when (now) {
            TileHeartRate.NotGranted -> TILE_NO_VALUE to getString(R.string.tile_not_granted)
            TileHeartRate.None -> TILE_NO_VALUE to getString(R.string.tile_heart_rate_none)
            is TileHeartRate.Recent -> now.bpm.toString() to when (now.minutesAgo) {
                0L -> getString(R.string.tile_heart_rate_caption_now)
                else -> resources.getQuantityString(
                    R.plurals.tile_heart_rate_caption_minutes,
                    now.minutesAgo.toInt(),
                    now.minutesAgo.toInt(),
                )
            }
        }
        // Said as a sentence: the caption's unit belongs with the number, and its dot is not read out well.
        val spoken = when {
            now !is TileHeartRate.Recent -> caption
            now.minutesAgo == 0L -> getString(R.string.tile_heart_rate_spoken_now, now.bpm)
            else -> resources.getQuantityString(
                R.plurals.tile_heart_rate_spoken_minutes,
                now.minutesAgo.toInt(),
                now.bpm,
                now.minutesAgo.toInt(),
            )
        }
        val title = getString(titleRes)
        val detail = day.range(samples)?.let { getString(R.string.tile_heart_rate_range, it.first, it.last) }
        return MetricTile(
            title = title,
            value = value,
            caption = caption,
            accent = accent,
            description = listOfNotNull("$title: $spoken", detail).joinToString(", "),
            chart = chart.heartRate(day.meanPerBucket(samples), day),
            detail = detail,
        )
    }
}

/**
 * The floors climbed since midnight: the barometer's metres that enough
 * steps carry, over three metres a floor, as the phone counts them before it
 * writes them to Health Connect. Whole floors and whole metres, both rounded
 * down, as the phone's dashboard shows floors.
 */
class FloorsTileService : MetricTileService() {

    /** `FloorsColor`. */
    override val accent: Int = 0xFFA8881F.toInt()

    override val titleRes: Int = R.string.tile_floors_label

    override fun read(today: TodayReadings, chart: TileChart): MetricTile {
        today.floorsMissing()?.let { return unavailable(getString(it)) }
        val floors = today.floors()
        val title = getString(titleRes)
        val value = formatCount(floors.floors)
        val caption = getString(R.string.tile_floors_caption)
        val detail = getString(R.string.tile_floors_climbed, formatCount(TileDay.wholeDown(floors.metres)))
        return MetricTile(
            title = title,
            value = value,
            caption = caption,
            accent = accent,
            description = "$title: $value $caption, $detail",
            chart = chart.cumulative(floors.running, today.day),
            detail = detail,
        )
    }
}
