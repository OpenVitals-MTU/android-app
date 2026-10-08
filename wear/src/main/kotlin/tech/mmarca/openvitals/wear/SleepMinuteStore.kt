package tech.mmarca.openvitals.wear

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The per-minute sleep input the watch has recorded and the phone may not
 * have fetched yet: one row per clock minute, keyed by its start, so a
 * minute written twice is one row. Plain SQLite, like [HeartRateStore]. The
 * columns are the `SM` line's fields; see
 * `docs/engineering/sleep-minute-features.md`.
 *
 * The phone keeps its own cursor and the watch keeps [RETENTION_MILLIS] of
 * history; nothing is deleted on the phone's behalf.
 */
class SleepMinuteStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "time_ms INTEGER PRIMARY KEY, kind TEXT NOT NULL, offset_s INTEGER NOT NULL, " +
                "flags INTEGER NOT NULL, n INTEGER NOT NULL, mv10 INTEGER NOT NULL, " +
                "bpm INTEGER, hsd10 INTEGER, hn INTEGER NOT NULL, " +
                "mx INTEGER NOT NULL, my INTEGER NOT NULL, mz INTEGER NOT NULL, " +
                "sx INTEGER NOT NULL, sy INTEGER NOT NULL, sz INTEGER NOT NULL, " +
                "zmin INTEGER, zmax INTEGER, zd10 INTEGER)",
        )
    }

    /** Nothing shipped with the earlier shape: start over. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /** Stores one minute; the row for the same minute is replaced. */
    fun upsert(m: WearLinkProtocol.SleepMinute) {
        val values = ContentValues(18).apply {
            put("time_ms", m.epochMillis)
            put("kind", m.kind.code)
            put("offset_s", m.offsetSeconds)
            put("flags", m.flags)
            put("n", m.sampleCount)
            put("mv10", m.movement10)
            putNullable("bpm", m.bpm)
            putNullable("hsd10", m.heartRateSd10)
            put("hn", m.heartRateSamples)
            put("mx", m.meanMilliG[0])
            put("my", m.meanMilliG[1])
            put("mz", m.meanMilliG[2])
            put("sx", m.sdMilliG[0])
            put("sy", m.sdMilliG[1])
            put("sz", m.sdMilliG[2])
            putNullable("zmin", m.zAngleMin)
            putNullable("zmax", m.zAngleMax)
            putNullable("zd10", m.zAngleDelta10)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Up to [limit] minutes newer than [epochMillis], oldest first. */
    fun since(epochMillis: Long, limit: Int): List<WearLinkProtocol.SleepMinute> {
        val cursor = readableDatabase.query(
            TABLE,
            COLUMNS,
            "time_ms > ?",
            arrayOf(epochMillis.toString()),
            null,
            null,
            "time_ms ASC",
            limit.coerceAtLeast(1).toString(),
        )
        return cursor.use {
            buildList(it.count) {
                while (it.moveToNext()) rowOf(it)?.let(::add)
            }
        }
    }

    private fun rowOf(c: Cursor): WearLinkProtocol.SleepMinute? {
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

    fun count(): Long = DatabaseUtils.queryNumEntries(readableDatabase, TABLE)

    /** Drops what is older than [RETENTION_MILLIS] as of [nowEpochMillis]. */
    fun prune(nowEpochMillis: Long) {
        writableDatabase.delete(TABLE, "time_ms < ?", arrayOf((nowEpochMillis - RETENTION_MILLIS).toString()))
    }

    private fun ContentValues.putNullable(column: String, value: Int?) {
        if (value == null) putNull(column) else put(column, value)
    }

    private fun Cursor.intOrNull(index: Int): Int? = if (isNull(index)) null else getInt(index)

    companion object {
        private const val DB_NAME = "sleep_minutes.db"
        private const val DB_VERSION = 2
        private const val TABLE = "minutes"
        private val COLUMNS = arrayOf(
            "time_ms", "kind", "offset_s", "flags", "n", "mv10", "bpm", "hsd10", "hn",
            "mx", "my", "mz", "sx", "sy", "sz", "zmin", "zmax", "zd10",
        )

        /** A week, like the heart rate store: a few missed syncs, bounded. */
        const val RETENTION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
