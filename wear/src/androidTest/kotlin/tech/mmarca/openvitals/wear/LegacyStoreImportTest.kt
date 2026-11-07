package tech.mmarca.openvitals.wear

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two databases of earlier builds move into the store once and are
 * deleted. Under their own names: the test runs inside the installed app,
 * whose real files must stay untouched.
 */
@RunWith(AndroidJUnit4::class)
class LegacyStoreImportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: MetricStore

    @Before
    fun open() {
        store = MetricStore(context, name = null)
    }

    @After
    fun close() {
        store.close()
        context.deleteDatabase(HR_DB)
        context.deleteDatabase(SM_DB)
    }

    @Test
    fun bothOldDatabasesAreImportedAndDeleted() {
        legacy(HR_DB).use { db ->
            db.execSQL("CREATE TABLE samples (time_ms INTEGER PRIMARY KEY, bpm INTEGER NOT NULL)")
            db.execSQL("INSERT INTO samples VALUES (1000, 60), (2000, 62)")
        }
        legacy(SM_DB).use { db ->
            db.execSQL(
                "CREATE TABLE minutes (time_ms INTEGER PRIMARY KEY, kind TEXT NOT NULL, offset_s INTEGER NOT NULL, " +
                    "flags INTEGER NOT NULL, n INTEGER NOT NULL, mv10 INTEGER NOT NULL, bpm INTEGER, hsd10 INTEGER, " +
                    "hn INTEGER NOT NULL, mx INTEGER NOT NULL, my INTEGER NOT NULL, mz INTEGER NOT NULL, " +
                    "sx INTEGER NOT NULL, sy INTEGER NOT NULL, sz INTEGER NOT NULL, zmin INTEGER, zmax INTEGER, zd10 INTEGER)",
            )
            db.execSQL("INSERT INTO minutes VALUES (60000, 'R', 7200, 48, 374, 0, 64, 12, 6, -137, -754, 637, 3, 3, 3, 40, 41, NULL)")
        }

        LegacyStoreImport.run(context, store, HR_DB, SM_DB)

        assertEquals(listOf(1000L, 2000L), store.since(WearMetrics.HEART_RATE, 0, 10).map { it.epochMillis })
        val minute = store.since(WearMetrics.SLEEP_MINUTES, 0, 10).single()
        assertEquals(374, minute.sampleCount)
        assertEquals(null, minute.zAngleDelta10)
        assertFalse(context.getDatabasePath(HR_DB).exists())
        assertFalse(context.getDatabasePath(SM_DB).exists())
    }

    @Test
    fun nothingToImportIsANoOp() {
        LegacyStoreImport.run(context, store, HR_DB, SM_DB)
        assertEquals(0L, store.count(WearMetrics.HEART_RATE))
    }

    private fun legacy(name: String): SQLiteDatabase {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).also { it.parentFile?.mkdirs() }
        return SQLiteDatabase.openOrCreateDatabase(file, null)
    }

    private companion object {
        const val HR_DB = "test_legacy_heart_rate.db"
        const val SM_DB = "test_legacy_sleep_minutes.db"
    }
}
