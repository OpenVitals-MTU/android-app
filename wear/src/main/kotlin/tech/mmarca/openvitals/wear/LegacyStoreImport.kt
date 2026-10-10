package tech.mmarca.openvitals.wear

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Moves what the two per-metric databases of earlier builds hold
 * (`heart_rate.db`, `sleep_minutes.db`) into [MetricStore], once, and
 * deletes them, so an update loses no unsynced night. A file that fails to
 * import is left in place and tried again at the next start.
 *
 * Delete this file once no watch can still carry those databases: a week
 * after every sideloaded watch has run a build with [MetricStore], their
 * rows would have aged out anyway.
 */
internal object LegacyStoreImport {

    private const val TAG = "LegacyStoreImport"
    const val HEART_RATE_DB = "heart_rate.db"
    const val SLEEP_MINUTES_DB = "sleep_minutes.db"

    private val SLEEP_COLUMNS = arrayOf(
        "time_ms", "kind", "offset_s", "flags", "n", "mv10", "bpm", "hsd10", "hn",
        "mx", "my", "mz", "sx", "sy", "sz", "zmin", "zmax", "zd10",
    )

    /** The file names are parameters only so a test on a real watch never touches the app's own files. */
    fun run(
        context: Context,
        store: MetricStore,
        heartRateDb: String = HEART_RATE_DB,
        sleepMinutesDb: String = SLEEP_MINUTES_DB,
    ) {
        importFile(context, heartRateDb) { db ->
            val samples = db.query("samples", arrayOf("time_ms", "bpm"), null, null, null, null, null).use { c ->
                buildList(c.count) { while (c.moveToNext()) add(WearLinkProtocol.HeartRateSample(c.getLong(0), c.getInt(1))) }
            }
            store.putAll(WearMetrics.HEART_RATE, samples)
            samples.size
        }
        importFile(context, sleepMinutesDb) { db ->
            val minutes = db.query("minutes", SLEEP_COLUMNS, null, null, null, null, null).use { c ->
                buildList(c.count) { while (c.moveToNext()) sleepMinuteOf(c)?.let(::add) }
            }
            store.putAll(WearMetrics.SLEEP_MINUTES, minutes)
            minutes.size
        }
    }

    private fun importFile(context: Context, name: String, copy: (SQLiteDatabase) -> Int) {
        val file = context.getDatabasePath(name)
        if (!file.exists()) return
        val copied = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use(copy)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot import $name; keeping it for the next start", e)
            return
        }
        context.deleteDatabase(name)
        Log.i(TAG, "Imported $copied row(s) from $name")
    }

    /** The column order of the sleep database of earlier builds, which is the `SM` line's. */
    private fun sleepMinuteOf(c: Cursor): WearLinkProtocol.SleepMinute? {
        val kind = WearLinkProtocol.MinuteKind.fromCode(c.getString(1)) ?: return null
        return WearLinkProtocol.SleepMinute(
            epochMillis = c.getLong(0),
            kind = kind,
            offsetSeconds = c.getInt(2),
            flags = c.getInt(3),
            sampleCount = c.getInt(4),
            movement10 = c.getInt(5),
            bpm = c.intOrNull(6),
            heartRateSd10 = c.intOrNull(7),
            heartRateSamples = c.getInt(8),
            meanMilliG = intArrayOf(c.getInt(9), c.getInt(10), c.getInt(11)),
            sdMilliG = intArrayOf(c.getInt(12), c.getInt(13), c.getInt(14)),
            zAngleMin = c.intOrNull(15),
            zAngleMax = c.intOrNull(16),
            zAngleDelta10 = c.intOrNull(17),
        )
    }

    private fun Cursor.intOrNull(index: Int): Int? = if (isNull(index)) null else getInt(index)
}
