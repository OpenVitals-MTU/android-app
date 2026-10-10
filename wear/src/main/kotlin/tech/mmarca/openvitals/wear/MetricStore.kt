package tech.mmarca.openvitals.wear

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Everything the watch has recorded and the phone may not have fetched yet,
 * for every [WearMetric], in one table: one row per metric and time, the
 * value kept as its protocol line. Plain SQLite: the watch has no Room, and
 * a three-column table needs no migrations; a new metric is a new key, not
 * a new table.
 *
 * The phone keeps its own cursors and the watch keeps each metric's
 * retention of history; nothing is deleted on the phone's behalf.
 */
class MetricStore(context: Context, name: String? = DB_NAME) : SQLiteOpenHelper(context, name, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE (" +
                "$COL_METRIC TEXT NOT NULL, " +
                "$COL_TIME INTEGER NOT NULL, " +
                "$COL_LINE TEXT NOT NULL, " +
                "PRIMARY KEY ($COL_METRIC, $COL_TIME)) WITHOUT ROWID",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    /** Stores one value; one already at its time stays or is replaced as [WearMetric.onSameTime] says. */
    fun <T> put(metric: WearMetric<T>, value: T) {
        put(writableDatabase, metric, value)
    }

    /** Stores several values in one transaction. */
    fun <T> putAll(metric: WearMetric<T>, values: Iterable<T>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (value in values) put(db, metric, value)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Up to [limit] values newer than [epochMillis], oldest first. Rows this build cannot read are skipped. */
    fun <T> since(metric: WearMetric<T>, epochMillis: Long, limit: Int): List<T> =
        readableDatabase.query(
            TABLE,
            arrayOf(COL_LINE),
            "$COL_METRIC = ? AND $COL_TIME > ?",
            arrayOf(metric.key, epochMillis.toString()),
            null,
            null,
            "$COL_TIME ASC",
            limit.coerceAtLeast(1).toString(),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) metric.decode(cursor.getString(0))?.let(::add)
            }
        }

    /** The newest value, or null when there is none or this build cannot read it. */
    fun <T> latest(metric: WearMetric<T>): T? =
        readableDatabase.query(
            TABLE,
            arrayOf(COL_LINE),
            "$COL_METRIC = ?",
            arrayOf(metric.key),
            null,
            null,
            "$COL_TIME DESC",
            "1",
        ).use { cursor -> if (cursor.moveToFirst()) metric.decode(cursor.getString(0)) else null }

    fun count(metric: WearMetric<*>): Long =
        DatabaseUtils.queryNumEntries(readableDatabase, TABLE, "$COL_METRIC = ?", arrayOf(metric.key))

    /** Drops what is older than [metric]'s retention as of [nowEpochMillis]. */
    fun prune(metric: WearMetric<*>, nowEpochMillis: Long) {
        writableDatabase.delete(
            TABLE,
            "$COL_METRIC = ? AND $COL_TIME < ?",
            arrayOf(metric.key, (nowEpochMillis - metric.retentionMillis).toString()),
        )
    }

    private fun <T> put(db: SQLiteDatabase, metric: WearMetric<T>, value: T) {
        val values = ContentValues(3).apply {
            put(COL_METRIC, metric.key)
            put(COL_TIME, metric.timeOf(value))
            put(COL_LINE, metric.encode(value))
        }
        val conflict = when (metric.onSameTime) {
            WearMetric.OnSameTime.KEEP -> SQLiteDatabase.CONFLICT_IGNORE
            WearMetric.OnSameTime.REPLACE -> SQLiteDatabase.CONFLICT_REPLACE
        }
        db.insertWithOnConflict(TABLE, null, values, conflict)
    }

    companion object {
        const val DB_NAME = "metrics.db"
        private const val DB_VERSION = 1
        const val TABLE = "rows"
        private const val COL_METRIC = "metric"
        private const val COL_TIME = "time_ms"
        private const val COL_LINE = "line"
    }
}
