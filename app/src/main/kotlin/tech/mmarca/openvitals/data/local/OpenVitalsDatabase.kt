package tech.mmarca.openvitals.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.mmarca.openvitals.data.local.beverage.BeverageDao
import tech.mmarca.openvitals.data.local.beverage.BeverageEntity
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyBucketEntity
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyDayEntity
import tech.mmarca.openvitals.data.local.bodyenergy.BodyEnergyTimelineDao
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessDao
import tech.mmarca.openvitals.data.local.garmin.GarminWellnessSampleEntity
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginDao
import tech.mmarca.openvitals.data.local.syncorigin.SyncedRecordOriginEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity

@Database(
    entities = [
        BeverageEntity::class,
        VitalsDailyAggregateEntity::class,
        VitalsSyncCursorEntity::class,
        BodyEnergyDayEntity::class,
        BodyEnergyBucketEntity::class,
        GarminWellnessSampleEntity::class,
        SyncedRecordOriginEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class OpenVitalsDatabase : RoomDatabase() {
    abstract fun beverageDao(): BeverageDao

    abstract fun vitalsDailyCacheDao(): VitalsDailyCacheDao

    abstract fun bodyEnergyTimelineDao(): BodyEnergyTimelineDao

    abstract fun garminWellnessDao(): GarminWellnessDao

    abstract fun syncedRecordOriginDao(): SyncedRecordOriginDao

    companion object {
        val MIGRATION_1_3 = beverageMigration(1)
        val MIGRATION_2_3 = beverageMigration(2)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createVitalsCacheTables(db)
            }
        }

        /**
         * The Body Energy chain moves off SharedPreferences into Room.
         *
         * Creation only, no data copy: the retired prefs store encoded a whole
         * day as one delimited string keyed by `date|signatureHash`, and every
         * such row is invalid under the v11 algorithm anyway. The chain rebuilds
         * itself from Health Connect on the next load, and the warm pass purges
         * the abandoned prefs keys.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createBodyEnergyChainTables(db)
            }
        }

        /**
         * Garmin watch-only wellness samples (stress, Body Battery, sleep
         * score, …) get their system of record.
         *
         * Creation only, no data copy: the table is column-identical to the
         * Flutter build's drift `garmin_wellness_samples`, and phase 5's
         * migrator imports the preserved drift rows 1:1 after this table
         * exists.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createGarminWellnessTable(db)
            }
        }

        /**
         * Original source apps of records received through phone-to-phone
         * sync. Creation only, no data copy: records synced before this
         * version landed without their origin on the wire, so there is
         * nothing to backfill — they keep displaying the receiver's own
         * attribution until a peer on a carrying version re-syncs them.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createSyncedRecordOriginsTable(db)
            }
        }

        private fun beverageMigration(startVersion: Int): Migration =
            object : Migration(startVersion, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createBeveragesTable(db)
                }
            }

        private fun createVitalsCacheTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vitals_daily_aggregates` (
                    `metric` TEXT NOT NULL,
                    `epoch_day` INTEGER NOT NULL,
                    `value_sum` REAL NOT NULL,
                    `secondary_sum` REAL,
                    `sample_count` INTEGER NOT NULL,
                    PRIMARY KEY(`metric`, `epoch_day`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vitals_sync_cursors` (
                    `metric` TEXT NOT NULL,
                    `changes_token` TEXT,
                    `last_full_sync_millis` INTEGER,
                    PRIMARY KEY(`metric`)
                )
                """.trimIndent()
            )
        }

        private fun createBodyEnergyChainTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `body_energy_days` (
                    `epoch_day` INTEGER NOT NULL,
                    `signature` TEXT NOT NULL,
                    `start_score` INTEGER NOT NULL,
                    `end_score` INTEGER NOT NULL,
                    `charged` INTEGER NOT NULL,
                    `drained` INTEGER NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `confidence_reason` TEXT NOT NULL,
                    `generated_at_millis` INTEGER NOT NULL,
                    `algorithm_version` INTEGER NOT NULL,
                    `bucket_minutes` INTEGER NOT NULL,
                    `heart_rate_sample_count` INTEGER NOT NULL,
                    `hrv_sample_count` INTEGER NOT NULL,
                    `sleep_session_count` INTEGER NOT NULL,
                    `workout_count` INTEGER NOT NULL,
                    `respiratory_sample_count` INTEGER NOT NULL,
                    `has_resting_heart_rate` INTEGER NOT NULL,
                    `has_baseline_resting_heart_rate` INTEGER NOT NULL,
                    `has_observed_max_heart_rate` INTEGER NOT NULL,
                    `has_hrv_baseline` INTEGER NOT NULL,
                    `has_respiratory_baseline` INTEGER NOT NULL,
                    `previous_end_score` INTEGER,
                    `carry_over_floor_applied` INTEGER NOT NULL,
                    `seed_source` TEXT NOT NULL,
                    `calibration_mode` TEXT NOT NULL,
                    PRIMARY KEY(`epoch_day`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `body_energy_buckets` (
                    `epoch_day` INTEGER NOT NULL,
                    `time_millis` INTEGER NOT NULL,
                    `score` INTEGER NOT NULL,
                    `delta` REAL NOT NULL,
                    `state` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `charge` REAL NOT NULL,
                    `intensity_drain` REAL NOT NULL,
                    `activity_energy_drain` REAL NOT NULL,
                    `basal_drain` REAL NOT NULL,
                    `stress_drain` REAL NOT NULL,
                    `recovery_debt_drain` REAL NOT NULL,
                    `primary_influence` TEXT NOT NULL,
                    PRIMARY KEY(`epoch_day`, `time_millis`)
                )
                """.trimIndent()
            )
        }

        private fun createSyncedRecordOriginsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `synced_record_origins` (
                    `client_record_id` TEXT NOT NULL,
                    `origin_package` TEXT NOT NULL,
                    PRIMARY KEY(`client_record_id`)
                )
                """.trimIndent()
            )
        }

        private fun createGarminWellnessTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `garmin_wellness_samples` (
                    `metric` TEXT NOT NULL,
                    `time_millis` INTEGER NOT NULL,
                    `value` INTEGER NOT NULL,
                    PRIMARY KEY(`metric`, `time_millis`)
                )
                """.trimIndent()
            )
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
