package tech.mmarca.openvitals.data.migration

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import kotlinx.coroutines.runBlocking
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.beverage.BeverageEntity
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessDao
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessSampleEntity

/**
 * Imports the beverage catalog from the Flutter drift database at
 * `app_flutter/openvitals.db`. The tables are column-identical and the
 * drift catalog is newer, so the Room table is replaced. Ids stay stable.
 *
 * `garmin_wellness_samples` is imported by [importGarminWellness]. The
 * derived caches and the body-energy tables are left alone. The source is
 * opened read-only; a live WAL sidecar is handled via a cache copy.
 */
class FlutterDatabaseImporter(private val context: Context) {

    /** Replaces the Room `beverages` table with the drift catalog, if present. */
    fun importBeverages(beverageDao: BeverageDao) {
        val beverages = readTable("beverages", ::queryBeverages) ?: return
        if (beverages.isEmpty()) {
            // Drift always seeds defaults, so an empty read means something is off.
            Log.w(TAG, "Flutter beverages table is empty; keeping the Kotlin-era catalog.")
            return
        }
        runBlocking { beverageDao.replaceAll(beverages) }
        Log.i(TAG, "Imported ${beverages.size} beverages from the Flutter database.")
    }

    /**
     * Copies the watch-only wellness samples into Room. Both tables are keyed
     * on `(metric, time_millis)`, so the upsert converges and deletes nothing.
     */
    fun importGarminWellness(dao: GarminWellnessDao) {
        val samples = readTable("garmin_wellness_samples", ::queryGarminWellness) ?: return
        if (samples.isEmpty()) {
            Log.i(TAG, "Flutter wellness table is empty; nothing to import.")
            return
        }
        runBlocking {
            samples.chunked(WELLNESS_UPSERT_CHUNK).forEach { chunk ->
                dao.upsertSamples(chunk)
            }
        }
        Log.i(TAG, "Imported ${samples.size} Garmin wellness samples from the Flutter database.")
    }

    private fun <T> readTable(tableName: String, query: (SQLiteDatabase) -> T): T? {
        val source = File(flutterDocumentsDir(context), DATABASE_NAME)
        if (!source.exists()) {
            Log.i(TAG, "No Flutter database at ${source.path}; skipping $tableName import.")
            return null
        }
        val result = readReadOnly(source, query) ?: readViaCacheCopy(source, query)
        if (result == null) {
            Log.w(TAG, "Could not read the Flutter $tableName table; skipping.")
        }
        return result
    }

    private fun <T> readReadOnly(source: File, query: (SQLiteDatabase) -> T): T? =
        try {
            SQLiteDatabase.openDatabase(source.path, null, SQLiteDatabase.OPEN_READONLY)
                .use(query)
        } catch (error: Exception) {
            Log.w(TAG, "Read-only open of ${source.path} failed; retrying via cache copy.", error)
            null
        }

    /**
     * WAL fallback: copy db and sidecars into cache and open the copy
     * read-write, so SQLite recovers the WAL into the copy.
     */
    private fun <T> readViaCacheCopy(source: File, query: (SQLiteDatabase) -> T): T? {
        val scratchDir = File(context.cacheDir, CACHE_COPY_DIR)
        return try {
            scratchDir.mkdirs()
            val copy = File(scratchDir, source.name)
            for (suffix in listOf("", "-wal", "-shm")) {
                val sidecar = File(source.path + suffix)
                if (sidecar.exists()) sidecar.copyTo(File(copy.path + suffix), overwrite = true)
            }
            SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE)
                .use(query)
        } catch (error: Exception) {
            Log.w(TAG, "Cache-copy read of ${source.path} failed.", error)
            null
        } finally {
            scratchDir.deleteRecursively()
        }
    }

    private fun queryGarminWellness(database: SQLiteDatabase): List<GarminWellnessSampleEntity> =
        database.rawQuery(
            "SELECT metric, time_millis, value FROM garmin_wellness_samples",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        GarminWellnessSampleEntity(
                            metric = cursor.getString(0),
                            timeMillis = cursor.getLong(1),
                            value = cursor.getLong(2),
                        ),
                    )
                }
            }
        }

    private fun queryBeverages(database: SQLiteDatabase): List<BeverageEntity> =
        database.rawQuery("SELECT * FROM beverages", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toBeverageEntity())
                }
            }
        }

    private fun Cursor.toBeverageEntity(): BeverageEntity =
        BeverageEntity(
            id = getString(getColumnIndexOrThrow("id")),
            name = getString(getColumnIndexOrThrow("name")),
            category = stringOrNull("category"),
            volumeMilliliters = getDouble(getColumnIndexOrThrow("volume_milliliters")),
            hydrationMultiplier = getDouble(getColumnIndexOrThrow("hydration_multiplier")),
            isPreloaded = getInt(getColumnIndexOrThrow("is_preloaded")) != 0,
            isDeleted = getInt(getColumnIndexOrThrow("is_deleted")) != 0,
            sortOrder = getInt(getColumnIndexOrThrow("sort_order")),
            energyKcal = doubleOrNull("energy_kcal"),
            proteinGrams = doubleOrNull("protein_grams"),
            totalCarbohydrateGrams = doubleOrNull("total_carbohydrate_grams"),
            totalFatGrams = doubleOrNull("total_fat_grams"),
            dietaryFiberGrams = doubleOrNull("dietary_fiber_grams"),
            sugarGrams = doubleOrNull("sugar_grams"),
            saturatedFatGrams = doubleOrNull("saturated_fat_grams"),
            sodiumGrams = doubleOrNull("sodium_grams"),
            potassiumGrams = doubleOrNull("potassium_grams"),
            calciumGrams = doubleOrNull("calcium_grams"),
            caffeineGrams = doubleOrNull("caffeine_grams"),
        )

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.doubleOrNull(column: String): Double? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getDouble(index) else null
    }

    companion object {
        /** Flutter's documents directory on Android: `Context.getDir("flutter")`. */
        fun flutterDocumentsDir(context: Context): File =
            context.getDir("flutter", Context.MODE_PRIVATE)

        const val DATABASE_NAME = "openvitals.db"
        private const val CACHE_COPY_DIR = "flutter_migration_db"
        private const val WELLNESS_UPSERT_CHUNK = 500
        private const val TAG = "FlutterDbImporter"
    }
}
