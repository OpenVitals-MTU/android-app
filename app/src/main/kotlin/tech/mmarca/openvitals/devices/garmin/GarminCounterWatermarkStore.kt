package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import org.json.JSONArray

/** How far each day's Garmin monitoring counters have been imported, as of [time]. */
data class FitCounterWatermark(
    val time: Instant,
    val steps: Int = 0,
    val distance: Int = 0,
    val calories: Int = 0,
    /**
     * The per-type readings behind the sums. Keeps the walk continuous: a
     * sync restates only the recently active types. Null on a watermark from
     * before these existed; the importer then adopts types silently. An empty
     * map means "no types"; never flatten the two.
     */
    val stepsByType: Map<Int, Int>? = null,
    val distanceByType: Map<Int, Int>? = null,
    val caloriesByType: Map<Int, Int>? = null,
    /**
     * What is already written into the bucket containing [time]. The next
     * sync recomputes that bucket in full. Zero on older watermarks.
     */
    val openBucketSteps: Int = 0,
    val openBucketDistance: Int = 0,
    val openBucketCalories: Int = 0,
    /**
     * Where the open bucket's record starts, when not its grid position: a
     * bucket entered mid-way begins at the resume point so it does not
     * overlap the previous record. Null means the grid position.
     */
    val openBucketStart: Instant? = null,
    /** Whether the legacy whole-day record has been overwritten by one bucket. */
    val legacyRetired: Boolean = false,
)

/**
 * How far each day's counters have been imported: the last reading, keyed by local day,
 * which the next sync differences from.
 *
 * The key and the pipe-delimited line format are the Flutter build's; the migrator copies
 * the lines across. Do not read `FlutterSharedPreferences` here.
 */
class GarminCounterWatermarkStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    fun load(): Map<String, FitCounterWatermark> {
        val raw = readLines() ?: return emptyMap()
        val marks = mutableMapOf<String, FitCounterWatermark>()
        for (line in raw) {
            // Unreadable lines are dropped, not guessed at. Older field counts
            // stay readable: missing fields load as null or zero, each with a
            // defined meaning on [FitCounterWatermark].
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
            // '-' means no start of its own: the grid position.
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

    /** Merges [marks] over what is stored and prunes to [RETAINED_DAYS]. */
    fun save(marks: Map<String, FitCounterWatermark>) {
        if (marks.isEmpty()) return
        val merged = load() + marks
        val days = merged.keys.sorted()
        val kept = if (days.size > RETAINED_DAYS) days.subList(days.size - RETAINED_DAYS, days.size) else days
        // '-' keeps a null map null: null means unknowable, empty means no types.
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

    /** Forgets every watermark, for a Health Connect wipe. */
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

        /** Every line length ever written, newest last. Older forms stay readable. */
        private val READABLE_FIELD_COUNTS = setOf(5, 6, 9, 12, 13)

        /** Days kept: covers a watch left in a drawer over a holiday. */
        private const val RETAINED_DAYS = 60
    }
}
