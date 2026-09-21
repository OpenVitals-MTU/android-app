package tech.mmarca.openvitals.architecture

import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test
import tech.mmarca.openvitals.data.local.OpenVitalsDatabase

/**
 * AGENTS.md tells new work to follow the docs when code and docs disagree. These are the facts
 * in them that a schema change makes wrong without anyone noticing.
 */
class ArchitectureDocTest {

    private val architecture = File("../docs/engineering/architecture.md").readText()
    private val agents = File("../AGENTS.md").readText()

    @Test
    fun `the architecture document lists every Room table`() {
        val tables = File("src/main/kotlin/tech/mmarca/openvitals/data/local").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> TableName.findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()

        assertWithMessage("no entity found: the pattern is out of date").that(tables).isNotEmpty()
        assertWithMessage("Room tables missing from the table list in docs/engineering/architecture.md")
            .that(tables.filterNot { "`$it`" in architecture })
            .isEmpty()
    }

    @Test
    fun `both documents name the current Room version`() {
        val version = OpenVitalsDatabase.VERSION

        assertWithMessage("docs/engineering/architecture.md does not say VERSION = $version")
            .that(architecture)
            .contains("`VERSION = $version`")
        assertWithMessage("AGENTS.md does not say Room is at version $version")
            .that(agents)
            .contains("Room is at version $version.")
    }

    @Test
    fun `the package map names every top-level package and its parts`() {
        val root = File("src/main/kotlin/tech/mmarca/openvitals")
        val rows = PackageRow.findAll(architecture).associate { it.groupValues[1] to it.groupValues[2] }
        val topLevel = root.listFiles { file -> file.isDirectory }.orEmpty().map { it.name }

        assertWithMessage("top-level packages missing from the package map")
            .that(topLevel.filterNot { name -> rows.keys.any { it == name || it.startsWith("$name/") } })
            .isEmpty()

        // These three are described part by part, so a new part must be named too.
        val unnamedParts = listOf("core", "data", "devices").flatMap { name ->
            File(root, name).listFiles { file -> file.isDirectory }.orEmpty()
                .map { it.name }
                .filterNot { part -> "`$part`" in rows.getValue(name) }
                .map { part -> "$name/$part" }
        }
        assertWithMessage("sub-packages missing from their row in the package map")
            .that(unnamedParts)
            .isEmpty()
    }

    private companion object {
        /** A row of the package map: the package in backticks, then what it owns. */
        val PackageRow = Regex("""(?m)^\| `([a-z/]+)/` \| (.+) \|$""")
        val TableName = Regex("""tableName\s*=\s*"([a-z_]+)"""")
    }
}
