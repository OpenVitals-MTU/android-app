package tech.mmarca.openvitals.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.beverage.BeverageEntity

@Database(
    entities = [BeverageEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class OpenVitalsDatabase : RoomDatabase() {
    abstract fun beverageDao(): BeverageDao

    companion object {
        val MIGRATION_1_3 = beverageMigration(1)
        val MIGRATION_2_3 = beverageMigration(2)

        private fun beverageMigration(startVersion: Int): Migration =
            object : Migration(startVersion, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createBeveragesTable(db)
                }
            }

        private fun createBeveragesTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `beverages` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT,
                    `volume_milliliters` REAL NOT NULL,
                    `hydration_multiplier` REAL NOT NULL,
                    `is_preloaded` INTEGER NOT NULL,
                    `is_deleted` INTEGER NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `energy_kcal` REAL,
                    `protein_grams` REAL,
                    `total_carbohydrate_grams` REAL,
                    `total_fat_grams` REAL,
                    `dietary_fiber_grams` REAL,
                    `sugar_grams` REAL,
                    `saturated_fat_grams` REAL,
                    `sodium_grams` REAL,
                    `potassium_grams` REAL,
                    `calcium_grams` REAL,
                    `caffeine_grams` REAL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }
    }
}
