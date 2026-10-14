package tech.mmarca.openvitals.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Four of these tables hold the only copy of user data, and no test ran the migrations
 * against the schema Room checks at start-up. There is no SQLite on the JVM here, so this
 * replays every migration's statements and compares the tables they leave with the schema
 * Room exported. That works because the migrations only create and drop tables.
 */
class OpenVitalsDatabaseSchemaTest {

    private val schemaDir = File("schemas/${OpenVitalsDatabase::class.java.name}")

    @Test
    fun `the schema of the current version is committed`() {
        val file = File(schemaDir, "${OpenVitalsDatabase.VERSION}.json")

        assertTrue("Build once and commit ${file.path}", file.isFile)
        assertEquals(OpenVitalsDatabase.VERSION, exportedDatabase(file).getValue("version").jsonPrimitive.int)
    }

    @Test
    fun `every shipped version can reach the current one, one way only`() {
        val migrations = OpenVitalsDatabase.ALL_MIGRATIONS
        val starts = migrations.map { it.startVersion }

        assertEquals("two migrations start at the same version", starts.distinct(), starts)
        for (start in starts) {
            var version = start
            while (version != OpenVitalsDatabase.VERSION) {
                val next = migrations.firstOrNull { it.startVersion == version }
                assertTrue("no migration leaves version $version", next != null)
                assertTrue("migration $version goes backwards", next!!.endVersion > version)
                version = next.endVersion
            }
        }
    }

    @Test
    fun `migrating from the first version leaves the tables Room expects`() {
        for (start in listOf(1, 2)) {
            val tables = replayMigrationsFrom(start)
            val expected = exportedDatabase(File(schemaDir, "${OpenVitalsDatabase.VERSION}.json"))
                .getValue("entities").jsonArray
                .associate { entity ->
                    val name = entity.jsonObject.getValue("tableName").jsonPrimitive.content
                    val sql = entity.jsonObject.getValue("createSql").jsonPrimitive.content
                    name to sql.replace("\${TABLE_NAME}", name).normalized()
                }

            assertEquals("tables after migrating from version $start", expected.keys, tables.keys)
            for ((name, sql) in expected) {
                assertEquals("table $name after migrating from version $start", sql, tables.getValue(name))
            }
        }
    }

    @Test
    fun `migration 16 to 17 adds the journal columns a device lacks and leaves present ones alone`() {
        fun columnsAddedWhenTableHas(vararg present: String): List<String> {
            val statements = mutableListOf<String>()
            val db = mockk<SupportSQLiteDatabase>(relaxed = true)
            every { db.execSQL(any()) } answers { statements += firstArg<String>() }
            every { db.query(any<String>()) } answers {
                val cursor = mockk<android.database.Cursor>(relaxed = true)
                var row = -1
                every { cursor.getColumnIndex("name") } returns 0
                every { cursor.moveToNext() } answers { ++row < present.size }
                every { cursor.getString(0) } answers { present[row] }
                cursor
            }
            OpenVitalsDatabase.MIGRATION_16_17.migrate(db)
            return statements.filter { it.startsWith("ALTER TABLE") }
        }

        assertEquals(4, columnsAddedWhenTableHas("date", "cervical_sensation").size)
        assertEquals(
            listOf("ALTER TABLE `cycle_journal_entries` ADD COLUMN `desire` INTEGER"),
            columnsAddedWhenTableHas("cervix_opening", "cervix_firmness", "cervix_position"),
        )
        assertTrue(columnsAddedWhenTableHas("cervix_opening", "cervix_firmness", "cervix_position", "desire").isEmpty())
    }

    /** Runs the chain from [start] on a database that only records. Returns each table's CREATE statement. */
    private fun replayMigrationsFrom(start: Int): Map<String, String> {
        val tables = linkedMapOf<String, String>()
        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        every { db.execSQL(any()) } answers {
            val sql = firstArg<String>().normalized()
            CreateTable.find(sql)?.let { tables[it.groupValues[1]] = sql }
            DropTable.find(sql)?.let { tables.remove(it.groupValues[1]) }
            Unit
        }
        var version = start
        while (version != OpenVitalsDatabase.VERSION) {
            val migration = OpenVitalsDatabase.ALL_MIGRATIONS.first { it.startVersion == version }
            migration.migrate(db)
            version = migration.endVersion
        }
        return tables
    }

    private fun exportedDatabase(file: File) =
        Json.parseToJsonElement(file.readText()).jsonObject.getValue("database").jsonObject

    /** One spacing for hand-written and generated SQL. */
    private fun String.normalized(): String =
        trim().replace(Regex("\\s+"), " ").replace("( ", "(").replace(" )", ")").replace(" ,", ",")

    private companion object {
        val CreateTable = Regex("^CREATE TABLE IF NOT EXISTS `([^`]+)`")
        val DropTable = Regex("^DROP TABLE IF EXISTS `([^`]+)`")
    }
}
