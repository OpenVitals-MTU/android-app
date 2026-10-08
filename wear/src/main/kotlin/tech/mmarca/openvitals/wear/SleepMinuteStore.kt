package tech.mmarca.openvitals.wear

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The per-minute sleep input the watch has recorded and the phone may not
 * have fetched yet: one row per clock minute, keyed by its start, so a
 * minute written twice is one row. Plain SQLite, like [HeartRateStore].
 *
 * The phone keeps its own cursor and the watch keeps [RETENTION_MILLIS] of
 * history; nothing is deleted on the phone's behalf.
 */
class SleepMinuteStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "$COL_TIME INTEGER PRIMARY KEY, " +
                "$COL_KIND TEXT NOT NULL, " +
                "$COL_MOVEMENT REAL NOT NULL, " +
                "$COL_BPM INTEGER, " +
                "$COL_OFFSET INTEGER NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Stores one minute; the row for the same minute is replaced. */
    fun upsert(minute: WearLinkProtocol.SleepMinute) {
        writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            ContentValues(5).apply {
                put(COL_TIME, minute.epochMillis)
                put(COL_KIND, minute.kind.code)
                put(COL_MOVEMENT, minute.movement)
                if (minute.bpm == null) putNull(COL_BPM) else put(COL_BPM, minute.bpm)
                put(COL_OFFSET, minute.offsetSeconds)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** Up to [limit] minutes newer than [epochMillis], oldest first. */
    fun since(epochMillis: Long, limit: Int): List<WearLinkProtocol.SleepMinute> {
        val cursor = readableDatabase.query(
            TABLE,
            arrayOf(COL_TIME, COL_KIND, COL_MOVEMENT, COL_BPM, COL_OFFSET),
            "$COL_TIME > ?",
            arrayOf(epochMillis.toString()),
            null,
            null,
            "$COL_TIME ASC",
            limit.coerceAtLeast(1).toString(),
        )
        return cursor.use {
            buildList(it.count) {
                while (it.moveToNext()) {
                    val kind = WearLinkProtocol.MinuteKind.fromCode(it.getString(1)) ?: continue
                    add(
                        WearLinkProtocol.SleepMinute(
                            epochMillis = it.getLong(0),
                            kind = kind,
                            movement = it.getFloat(2),
                            bpm = if (it.isNull(3)) null else it.getInt(3),
                            offsetSeconds = it.getInt(4),
                        ),
                    )
                }
            }
        }
    }

    fun count(): Long = DatabaseUtils.queryNumEntries(readableDatabase, TABLE)

    /** Drops what is older than [RETENTION_MILLIS] as of [nowEpochMillis]. */
    fun prune(nowEpochMillis: Long) {
        writableDatabase.delete(TABLE, "$COL_TIME < ?", arrayOf((nowEpochMillis - RETENTION_MILLIS).toString()))
    }

    companion object {
        private const val DB_NAME = "sleep_minutes.db"
        private const val DB_VERSION = 1
        private const val TABLE = "minutes"
        private const val COL_TIME = "time_ms"
        private const val COL_KIND = "kind"
        private const val COL_MOVEMENT = "movement"
        private const val COL_BPM = "bpm"
        private const val COL_OFFSET = "offset_s"

        /** A week, like the heart rate store: a few missed syncs, bounded. */
        const val RETENTION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
