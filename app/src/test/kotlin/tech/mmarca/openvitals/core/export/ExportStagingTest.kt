package tech.mmarca.openvitals.core.export

import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.diagnostics.DiagnosticsExportCacheDirectory
import tech.mmarca.openvitals.features.activity.RouteExportCacheDirectory

/** The staging area every export passes through: the feature directory, the 24 h sweep and the overwrite rule. */
class ExportStagingTest {

    private lateinit var cacheRoot: File

    @Before
    fun setUp() {
        cacheRoot = File.createTempFile("export_staging_test", "").let { file ->
            file.delete()
            file.also { it.mkdirs() }
        }
    }

    @After
    fun tearDown() {
        cacheRoot.deleteRecursively()
    }

    @Test
    fun `a staged file lands under the feature directory, named as asked`() {
        val file = featureDirectory(RouteExportCacheDirectory)
            .stageExport("morning-run.gpx") { output -> output.write("x".toByteArray()) }

        assertTrue(file.path, file.path.endsWith("/$RouteExportCacheDirectory/morning-run.gpx"))
        // The receiving app is handed a path, so it must exist.
        assertTrue(file.exists())
    }

    @Test
    fun `two features staging the same name do not collide`() {
        // The directory name is what keeps a diagnostics log from overwriting a route export.
        assertNotEquals(DiagnosticsExportCacheDirectory, RouteExportCacheDirectory)

        val route = featureDirectory(RouteExportCacheDirectory)
            .stageExport("export.txt") { output -> output.write("route".toByteArray()) }
        val diagnostics = featureDirectory(DiagnosticsExportCacheDirectory)
            .stageExport("export.txt") { output -> output.write("log".toByteArray()) }

        assertNotEquals(route.path, diagnostics.path)
        assertEquals("route", route.readText())
        assertEquals("log", diagnostics.readText())
    }

    @Test
    fun `staging prunes copies older than a day and keeps fresh ones`() {
        val directory = featureDirectory(RouteExportCacheDirectory).apply { mkdirs() }
        val stale = File(directory, "stale.gpx").apply { writeText("x") }
        val fresh = File(directory, "fresh.gpx").apply { writeText("x") }
        stale.setLastModified(System.currentTimeMillis() - 25 * 60 * 60 * 1000L)

        directory.stageExport("new.gpx") { output -> output.write("x".toByteArray()) }

        assertFalse(stale.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun `re-staging the same name overwrites rather than stacking up`() {
        val directory = featureDirectory(RouteExportCacheDirectory)
        directory.stageExport("report.txt") { output -> output.write("first".toByteArray()) }
        directory.stageExport("report.txt") { output -> output.write("second".toByteArray()) }

        assertEquals(1, directory.listFiles().orEmpty().count { it.isFile })
        assertEquals("second", File(directory, "report.txt").readText())
    }

    @Test
    fun `a locked or vanished file does not abort the export`() {
        // Best-effort pruning: the user asked for an export, not for cache hygiene.
        val directory = featureDirectory(RouteExportCacheDirectory).apply { mkdirs() }
        File(directory, "a-subdirectory").apply {
            mkdirs()
            setLastModified(System.currentTimeMillis() - 25 * 60 * 60 * 1000L)
        }

        val file = directory.stageExport("report.txt") { output ->
            output.write("x".toByteArray())
        }

        assertTrue(file.exists())
    }

    private fun featureDirectory(name: String): File = File(cacheRoot, name)
}
