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
        // The chain's own bookkeeping reuses `vitals_sync_cursors`, so the
        // migration must not try to create a table that already exists at v4.
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
        // Column-identical to the Flutter drift table so phase 5's preserved
        // drift rows can be imported 1:1.
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
