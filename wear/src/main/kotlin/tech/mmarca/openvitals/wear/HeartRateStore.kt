package tech.mmarca.openvitals.wear

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The heart rate samples the watch has recorded and the phone has not
 * necessarily fetched yet. One row per sample, keyed by time, so a sample
 * written twice is one row. Plain SQLite: the watch has no Room and needs
 * no migrations for a two-column table.
 *
 * The phone keeps its own cursor and the watch keeps [RETENTION_MILLIS] of
 * history; nothing is deleted on the phone's behalf.
 */
class HeartRateStore(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "$COL_TIME INTEGER PRIMARY KEY, " +
                "$COL_BPM INTEGER NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Stores one sample; a sample at the same millisecond is kept as it was. */
    fun insert(epochMillis: Long, bpm: Int) {
        writableDatabase.insertWithOnConflict(
            TABLE,
            null,
            ContentValues(2).apply {
                put(COL_TIME, epochMillis)
                put(COL_BPM, bpm)
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
    }

    /** Up to [limit] samples newer than [epochMillis], oldest first. */
    fun since(epochMillis: Long, limit: Int): List<WearLinkProtocol.HeartRateSample> {
        val cursor = readableDatabase.query(
            TABLE,
            arrayOf(COL_TIME, COL_BPM),
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
                    add(WearLinkProtocol.HeartRateSample(it.getLong(0), it.getInt(1)))
                }
            }
        }
    }

    /** The newest sample, or null when nothing has been recorded. */
    fun latest(): WearLinkProtocol.HeartRateSample? =
        readableDatabase.query(
            TABLE,
            arrayOf(COL_TIME, COL_BPM),
            null,
            null,
            null,
            null,
            "$COL_TIME DESC",
            "1",
        ).use { if (it.moveToFirst()) WearLinkProtocol.HeartRateSample(it.getLong(0), it.getInt(1)) else null }

    fun count(): Long = android.database.DatabaseUtils.queryNumEntries(readableDatabase, TABLE)

    /** The mean rate of the samples in `[from, to)`, rounded, or null when there are none. */
    fun averageBetween(fromEpochMillis: Long, toEpochMillis: Long): Int? =
        readableDatabase.rawQuery(
            "SELECT AVG($COL_BPM) FROM $TABLE WHERE $COL_TIME >= ? AND $COL_TIME < ?",
            arrayOf(fromEpochMillis.toString(), toEpochMillis.toString()),
        ).use { if (it.moveToFirst() && !it.isNull(0)) Math.round(it.getDouble(0)).toInt() else null }

    /** Drops what is older than [RETENTION_MILLIS] as of [nowEpochMillis]. */
    fun prune(nowEpochMillis: Long) {
        writableDatabase.delete(TABLE, "$COL_TIME < ?", arrayOf((nowEpochMillis - RETENTION_MILLIS).toString()))
    }

    companion object {
        private const val DB_NAME = "heart_rate.db"
        private const val DB_VERSION = 1
        private const val TABLE = "samples"
        private const val COL_TIME = "time_ms"
        private const val COL_BPM = "bpm"

        /** A week: long enough to miss a few days of syncing, small enough to stay bounded. */
        const val RETENTION_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
