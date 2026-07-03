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
}
