package tech.mmarca.openvitals.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenVitalsDatabaseMigrationTest {
    @Test
    fun `legacy version one migrates to beverage schema version three`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_1_3.migrate(db)

        assertEquals(1, OpenVitalsDatabase.MIGRATION_1_3.startVersion)
        assertEquals(3, OpenVitalsDatabase.MIGRATION_1_3.endVersion)
        verify { db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `beverages`") }) }
    }

    @Test
    fun `legacy version two migrates to beverage schema version three`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_2_3.migrate(db)

        assertEquals(2, OpenVitalsDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, OpenVitalsDatabase.MIGRATION_2_3.endVersion)
        verify { db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `beverages`") }) }
    }

    @Test
    fun `version four migrates to the body energy chain tables`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_4_5.migrate(db)

        assertEquals(4, OpenVitalsDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, OpenVitalsDatabase.MIGRATION_4_5.endVersion)
        verify { db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `body_energy_days`") }) }
        verify { db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `body_energy_buckets`") }) }
        // The chain reuses `vitals_sync_cursors`, so the migration must not create a table that exists at v4.
        verify(exactly = 0) {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `vitals_sync_cursors`") })
        }
    }

    @Test
    fun `version five migrates to the garmin wellness table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_5_6.migrate(db)

        assertEquals(5, OpenVitalsDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, OpenVitalsDatabase.MIGRATION_5_6.endVersion)
        // Column-identical to the Flutter drift table so preserved rows import 1:1.
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `garmin_wellness_samples`") &&
                        it.contains("`metric` TEXT NOT NULL") &&
                        it.contains("`time_millis` INTEGER NOT NULL") &&
                        it.contains("`value` INTEGER NOT NULL") &&
                        it.contains("PRIMARY KEY(`metric`, `time_millis`)")
                },
            )
        }
    }

    @Test
    fun `version thirteen adds the pill intakes table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_13_14.migrate(db)

        assertEquals(13, OpenVitalsDatabase.MIGRATION_13_14.startVersion)
        assertEquals(14, OpenVitalsDatabase.MIGRATION_13_14.endVersion)
        verify {
            db.execSQL(
                match { it.contains("CREATE TABLE IF NOT EXISTS `pill_intakes`") && it.contains("PRIMARY KEY(`date`)") },
            )
        }
    }

    @Test
    fun `version fourteen adds the medical document tables and their indices`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_14_15.migrate(db)

        assertEquals(14, OpenVitalsDatabase.MIGRATION_14_15.startVersion)
        assertEquals(15, OpenVitalsDatabase.MIGRATION_14_15.endVersion)
        verify {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `medical_documents`") && it.contains("PRIMARY KEY(`id`)") })
            db.execSQL(match { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_medical_documents_sha256`") })
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `medical_document_records`") })
            // Room checks indices when it opens the database, so the migration must make them too.
            db.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_medical_document_records_data_source_id_resource_type_resource_id`") })
        }
    }

    @Test
    fun `version nine adds the garmin sleep minutes table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_9_10.migrate(db)

        assertEquals(9, OpenVitalsDatabase.MIGRATION_9_10.startVersion)
        assertEquals(10, OpenVitalsDatabase.MIGRATION_9_10.endVersion)
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `garmin_sleep_minutes`") &&
                        it.contains("`time_millis` INTEGER NOT NULL") &&
                        it.contains("`kind` TEXT NOT NULL") &&
                        it.contains("`features` BLOB") &&
                        it.contains("PRIMARY KEY(`time_millis`)")
                },
            )
        }
    }

    @Test
    fun `version ten adds the heart rate days table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_10_11.migrate(db)

        assertEquals(10, OpenVitalsDatabase.MIGRATION_10_11.startVersion)
        assertEquals(11, OpenVitalsDatabase.MIGRATION_10_11.endVersion)
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `heart_rate_days`") &&
                        it.contains("`signature` TEXT NOT NULL") &&
                        it.contains("`average_bpm` REAL NOT NULL") &&
                        it.contains("PRIMARY KEY(`epoch_day`)")
                },
            )
        }
    }

    @Test
    fun `version eleven adds the food tables`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_11_12.migrate(db)

        assertEquals(11, OpenVitalsDatabase.MIGRATION_11_12.startVersion)
        assertEquals(12, OpenVitalsDatabase.MIGRATION_11_12.endVersion)
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `foods`") &&
                        it.contains("`amount_grams` REAL NOT NULL") &&
                        it.contains("`is_deleted` INTEGER NOT NULL") &&
                        it.contains("PRIMARY KEY(`id`)")
                },
            )
        }
        // One row per nutrient, so a food can carry any of them without a column each.
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `food_nutrients`") &&
                        it.contains("`value` REAL NOT NULL") &&
                        it.contains("PRIMARY KEY(`food_id`, `nutrient`)")
                },
            )
        }
    }

    @Test
    fun `version twelve adds the cycle journal tables`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_12_13.migrate(db)

        assertEquals(12, OpenVitalsDatabase.MIGRATION_12_13.startVersion)
        assertEquals(13, OpenVitalsDatabase.MIGRATION_12_13.endVersion)
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `cycle_journal_entries`") &&
                        it.contains("`symptoms` TEXT NOT NULL") &&
                        it.contains("`updated_at_millis` INTEGER NOT NULL") &&
                        it.contains("PRIMARY KEY(`date`)")
                },
            )
        }
        // One row per excluded cycle, keyed by the start the app resolved from the date the user picked.
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `cycle_exclusions`") &&
                        it.contains("PRIMARY KEY(`start_date`)")
                },
            )
        }
    }

    @Test
    fun `version eight restores the garmin wellness table`() {
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)

        OpenVitalsDatabase.MIGRATION_8_9.migrate(db)

        assertEquals(8, OpenVitalsDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, OpenVitalsDatabase.MIGRATION_8_9.endVersion)
        verify {
            db.execSQL(
                match {
                    it.contains("CREATE TABLE IF NOT EXISTS `garmin_wellness_samples`") &&
                        it.contains("PRIMARY KEY(`metric`, `time_millis`)")
                },
            )
        }
    }
}
