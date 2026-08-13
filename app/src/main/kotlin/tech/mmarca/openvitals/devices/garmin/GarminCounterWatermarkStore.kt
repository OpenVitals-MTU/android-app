package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import org.json.JSONArray

/**
 * How far each day's Garmin monitoring counters have already been imported, as
 * of [time]. Consumed by the FIT wellness importer (sub-milestone 7c), which
 * differences each sync's cumulative counters against the reading before it.
 */
data class FitCounterWatermark(
    val time: Instant,
    val steps: Int = 0,
    val distance: Int = 0,
    val calories: Int = 0,
    /**
     * The per-activity-type readings behind the sums, as of [time].
     *
     * This is what keeps the walk continuous across syncs. The watch counts
     * each activity type separately and a sync's files restate only the types
     * recently active — so a sum rebuilt from one sync's points starts without
     * the others, dips below the watermark, reads as a counter rollover, and
     * when the missing type is restated the whole day re-enters as fresh
     * movement.
     *
     * Null on a watermark stored before these existed — the importer adopts
     * such types silently instead of re-counting them. An empty map means "no
     * types"; the two must never be flattened into each other.
     */
    val stepsByType: Map<Int, Int>? = null,
    val distanceByType: Map<Int, Int>? = null,
    val caloriesByType: Map<Int, Int>? = null,
    /**
     * What has already been written into the grid bucket containing [time].
     * The last bucket a sync touches is usually half-filled and still gets
     * written; the next sync recomputes it IN FULL — these values plus the new
     * deltas — and the upsert replaces the half with the whole. Zero on a
     * watermark stored before these existed, which is exactly right: those
     * syncs never wrote the open bucket.
     */
    val openBucketSteps: Int = 0,
    val openBucketDistance: Int = 0,
    val openBucketCalories: Int = 0,
    /**
     * Where the record for that open bucket actually STARTS, which is not
     * always its grid position: a bucket first entered part-way through — the
     * one holding the instant a sync resumed from — begins at the resume point,
     * so that it does not overlap the record the previous sync ended with.
     *
     * Persisted because the next sync re-writes that record in full under the
     * same id, and re-deriving the start from the grid would widen it back over
     * its predecessor — the overlap Health Connect discards when it aggregates.
     *
     * Null on a watermark stored before this existed, and on one whose open
     * bucket begins exactly on the grid; both mean "the grid position".
     */
    val openBucketStart: Instant? = null,
    /**
     * Whether this day's pre-intraday whole-day record has been superseded —
     * exactly one bucket per day is written under the legacy record id, which
     * overwrites it; this records that it has happened so the next sync does
     * not do it again to a different bucket.
     */
    val legacyRetired: Boolean = false,
)

/**
 * How far each day's Garmin monitoring counters have already been imported.
 *
 * The watch's step, distance and active-calorie counters run cumulatively from
 * local midnight, and every sync brings only the minutes since the last one.
 * To write those minutes as INTRADAY records — rather than one flat total per
 * day — each interval has to be differenced against the reading before it, and
 * for the first reading in a sync that predecessor is in a file this run does
 * not have. It was archived on the watch two syncs ago.
 *
 * So the last reading imported for a day is remembered here, and the next sync
 * differences from it. That is what keeps the day's total exact across any
 * number of syncs, and what makes re-importing a file already behind the
 * watermark write nothing instead of counting it twice.
 *
 * SharedPreferences-backed and fire-and-forget, like [GarminDeviceStateStore].
 * Keyed by local day (`yyyy-mm-dd`) and NOT by device: the counters belong to
 * the wearer's day, and a second watch reporting the same day's steps would be
 * describing the same walk.
 *
 * The KEY name and the pipe-delimited line format are exactly what the Flutter
 * build wrote — there the list lived in `FlutterSharedPreferences` under a
 * `flutter.` prefix; this store uses its OWN prefs file with the un-prefixed
 * key, and phase 5's migrator copies the Flutter lines across verbatim. Do NOT
 * read `FlutterSharedPreferences` here.
 */
class GarminCounterWatermarkStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    fun load(): Map<String, FitCounterWatermark> {
        val raw = readLines() ?: return emptyMap()
        val marks = mutableMapOf<String, FitCounterWatermark>()
        for (line in raw) {
            // Anything unreadable is DROPPED, not guessed at. A watermark is a
            // claim about what Health Connect already holds; half of one would
            // either lose a day's steps or write them twice, and re-importing
            // from the day's start is the safer of the two mistakes.
            //
            // Five/six/nine/twelve fields are the older forms, kept readable
            // rather than dropped. Five predates
            // [FitCounterWatermark.legacyRetired]; five and six predate the
            // per-type maps, which load as null so the importer adopts their
            // types silently instead of re-counting them. Nine predates the
            // open-bucket seed values, which load as zero — correct, because
            // those versions never wrote the open bucket. Twelve predates
            // [FitCounterWatermark.openBucketStart], which loads as null: those
            // versions started every bucket on the grid, which is what null
            // means.
            val parts = line.split('|')
            if (parts.size !in READABLE_FIELD_COUNTS) continue
            val timeMs = parts[1].toLongOrNull()
            val steps = parts[2].toIntOrNull()
            val distance = parts[3].toIntOrNull()
            val calories = parts[4].toIntOrNull()
            if (timeMs == null || steps == null || distance == null || calories == null) continue

            var readable = true
            fun decodeTypes(raw: String): Map<Int, Int>? {
                if (raw == "-") return null
                val types = mutableMapOf<Int, Int>()
                if (raw.isEmpty()) return types
                for (pair in raw.split(',')) {
                    val colon = pair.indexOf(':')
                    val type = if (colon < 0) null else pair.substring(0, colon).toIntOrNull()
                    val value = if (colon < 0) null else pair.substring(colon + 1).toIntOrNull()
                    if (type == null || value == null) {
                        readable = false
                        return null
                    }
                    types[type] = value
                }
                return types
            }

            val stepsByType = if (parts.size >= 9) decodeTypes(parts[6]) else null
            val distanceByType = if (parts.size >= 9) decodeTypes(parts[7]) else null
            val caloriesByType = if (parts.size >= 9) decodeTypes(parts[8]) else null
            val openSteps = if (parts.size >= 12) parts[9].toIntOrNull() else 0
            val openDistance = if (parts.size >= 12) parts[10].toIntOrNull() else 0
            val openCalories = if (parts.size >= 12) parts[11].toIntOrNull() else 0
            if (!readable || openSteps == null || openDistance == null || openCalories == null) continue
            // '-' is "no start of its own", which reads as the grid position.
            val openStartRaw = if (parts.size >= 13) parts[12] else "-"
            val openStart = if (openStartRaw == "-") {
                null
            } else {
                openStartRaw.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: continue
            }

            marks[parts[0]] = FitCounterWatermark(
                time = Instant.ofEpochMilli(timeMs),
                steps = steps,
                distance = distance,
                calories = calories,
                stepsByType = stepsByType,
                distanceByType = distanceByType,
                caloriesByType = caloriesByType,
                openBucketSteps = openSteps,
                openBucketDistance = openDistance,
                openBucketCalories = openCalories,
                openBucketStart = openStart,
                legacyRetired = parts.size >= 6 && parts[5] == "1",
            )
        }
        return marks
    }

    /**
     * Merges [marks] over what is stored and prunes to [RETAINED_DAYS].
     *
     * Merged rather than replaced: one sync touches the days its files
     * covered, and must not forget the others.
     */
    fun save(marks: Map<String, FitCounterWatermark>) {
        if (marks.isEmpty()) return
        val merged = load() + marks
        val days = merged.keys.sorted()
        val kept = if (days.size > RETAINED_DAYS) days.subList(days.size - RETAINED_DAYS, days.size) else days
        // '-' keeps a legacy mark's null maps null across a re-save: an empty
        // map means "no types", null means "types unknowable", and flattening
        // the two would turn silent adoption into a full re-count.
        fun encodeTypes(types: Map<Int, Int>?): String =
            types?.entries?.joinToString(",") { "${it.key}:${it.value}" } ?: "-"
        writeLines(
            kept.map { day ->
                val mark = merged.getValue(day)
                "$day|${mark.time.toEpochMilli()}" +
                    "|${mark.steps}|${mark.distance}" +
                    "|${mark.calories}" +
                    "|${if (mark.legacyRetired) 1 else 0}" +
                    "|${encodeTypes(mark.stepsByType)}" +
                    "|${encodeTypes(mark.distanceByType)}" +
                    "|${encodeTypes(mark.caloriesByType)}" +
                    "|${mark.openBucketSteps}" +
                    "|${mark.openBucketDistance}" +
                    "|${mark.openBucketCalories}" +
                    "|${mark.openBucketStart?.toEpochMilli() ?: "-"}"
            },
        )
    }

    /**
     * Forgets every watermark, so the next import writes each day from its
     * start.
     *
     * For a Health Connect wipe: the records the watermarks describe are gone,
     * so the watermarks are lies, and a sync that trusted them would write
     * only the minutes since — leaving the day short forever.
     */
    fun clear() {
        prefs.edit { remove(PREFS_KEY) }
    }

    private fun readLines(): List<String>? {
        val raw = prefs.getString(PREFS_KEY, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }
        }.getOrNull()
    }

    private fun writeLines(lines: List<String>) {
        prefs.edit { putString(PREFS_KEY, JSONArray(lines).toString()) }
    }

    companion object {
        const val PREFS_FILE = "garmin_counter_watermarks"
        private const val PREFS_KEY = "garmin_counter_watermarks"

        /**
         * Every line length this store has ever written, newest last. Older
         * forms stay readable rather than being dropped — a dropped watermark
         * re-imports its day from the start — and each missing field has a
         * defined meaning for the versions that lacked it, documented on
         * [FitCounterWatermark].
         */
        private val READABLE_FIELD_COUNTS = setOf(5, 6, 9, 12, 13)

        /**
         * Days kept. Long enough to cover a watch left in a drawer over a
         * holiday and synced on return; short enough that the list cannot grow
         * without bound.
         */
        private const val RETAINED_DAYS = 60
    }
}
