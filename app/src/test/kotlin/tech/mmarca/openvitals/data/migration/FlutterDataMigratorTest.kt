package tech.mmarca.openvitals.data.migration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.local.OpenVitalsDatabase
import tech.mmarca.openvitals.devices.FakeSharedPreferences

/**
 * Run conditions, offline maps and robustness of [FlutterDataMigrator] (Flutter to Kotlin).
 * The migrator reads real on-disk shapes, so the fixtures are a mocked [Context] over
 * [FakeSharedPreferences] files plus real temp directories.
 */
class FlutterDataMigratorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataDir: File
    private lateinit var filesDir: File
    private lateinit var flutterDocumentsDir: File
    private lateinit var prefsFiles: MutableMap<String, FakeSharedPreferences>

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        dataDir = temporaryFolder.newFolder("data")
        filesDir = temporaryFolder.newFolder("files")
        flutterDocumentsDir = temporaryFolder.newFolder("app_flutter")
        prefsFiles = mutableMapOf()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun prefs(name: String): FakeSharedPreferences =
        prefsFiles.getOrPut(name) { FakeSharedPreferences() }

    private val mainPrefs: FakeSharedPreferences
        get() = prefs(TargetPrefsFile.MAIN.fileName)

    /** Makes the single stat in `shouldMigrate` succeed. */
    private fun installFlutterPrefsFile() {
        val sharedPrefsDir = File(dataDir, "shared_prefs").apply { mkdirs() }
        File(sharedPrefsDir, "${FlutterPrefsReader.PREFS_FILE}.xml").writeText("<map/>")
    }

    /** Seeds one decoded Flutter preference (values ride under the plugin prefix). */
    private fun seedFlutterPreference(key: String, value: String) {
        prefs(FlutterPrefsReader.PREFS_FILE).edit().putString("flutter.$key", value).commit()
    }

    private fun context(): Context = mockk<Context>().also { context ->
        every { context.getSharedPreferences(any(), any()) } answers {
            prefs(firstArg()) as SharedPreferences
        }
        every { context.dataDir } returns dataDir
        every { context.filesDir } returns filesDir
        every { context.getDir("flutter", Context.MODE_PRIVATE) } returns flutterDocumentsDir
    }

    /** A database importer that does nothing. */
    private fun noOpImporter(): FlutterDatabaseImporter = mockk<FlutterDatabaseImporter>().also {
        every { it.importBeverages(any()) } just runs
        every { it.importGarminWellness(any()) } just runs
    }

    private fun database(): OpenVitalsDatabase = mockk<OpenVitalsDatabase>(relaxed = true)

    private fun migrator(
        context: Context,
        importer: FlutterDatabaseImporter = noOpImporter(),
    ) = FlutterDataMigrator(
        context = context,
        reader = FlutterPrefsReader(context),
        databaseImporter = importer,
    )

    /** Both phases, as `OpenVitalsApp.onCreate` runs them. */
    private fun runMigration(
        context: Context,
        importer: FlutterDatabaseImporter = noOpImporter(),
    ): Boolean {
        val migrator = migrator(context, importer)
        val started = migrator.migrateIfNeeded()
        if (started) migrator.importDatabaseAndFinish(database())
        return started
    }

    // region run conditions

    @Test
    fun `is a no-op when there is no legacy data`() {
        // No Flutter preferences file on disk: the fresh-install case.
        seedFlutterPreference("unit_system", "imperial")

        val started = runMigration(context())

        assertThat(started).isFalse()
        assertThat(mainPrefs.contains("unit_system")).isFalse()
        // Nothing ran, so nothing is flagged: a later restore must still migrate.
        assertThat(mainPrefs.contains(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY)).isFalse()
    }

    @Test
    fun `is idempotent - a second run changes nothing`() {
        installFlutterPrefsFile()
        seedFlutterPreference("unit_system", "imperial")
        val context = context()

        assertThat(runMigration(context)).isTrue()
        assertThat(mainPrefs.getBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, false)).isTrue()
        assertThat(mainPrefs.getString("unit_system", null)).isEqualTo("IMPERIAL")

        // The user then changes a setting on the Kotlin side.
        mainPrefs.edit().putString("unit_system", "METRIC").commit()

        assertThat(runMigration(context)).isFalse()

        // The second run must not resurrect the Flutter value.
        assertThat(mainPrefs.getString("unit_system", null)).isEqualTo("METRIC")
    }

    @Test
    fun `the one-shot flag is set even when every step fails`() {
        // A persistent failure must not retry on every launch forever.
        installFlutterPrefsFile()
        val context = mockk<Context>().also { context ->
            every { context.getSharedPreferences(any(), any()) } answers {
                val name = firstArg<String>()
                // Only the migration's own bookkeeping file is readable.
                if (name == TargetPrefsFile.MAIN.fileName) {
                    prefs(name) as SharedPreferences
                } else {
                    error("unreadable")
                }
            }
            every { context.dataDir } returns dataDir
            every { context.filesDir } throws IllegalStateException("unreadable")
            every { context.getDir(any(), any()) } throws IllegalStateException("unreadable")
        }
        val importer = mockk<FlutterDatabaseImporter>().also {
            every { it.importBeverages(any()) } throws IllegalStateException("unreadable")
            every { it.importGarminWellness(any()) } throws IllegalStateException("unreadable")
        }

        assertThat(runMigration(context, importer)).isTrue()

        assertThat(mainPrefs.getBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, false)).isTrue()
    }

    // endregion

    // region offline maps

    @Test
    fun `an existing destination is left alone`() {
        installFlutterPrefsFile()
        val source = File(flutterDocumentsDir, "offline_maps").apply { mkdirs() }
        File(filesDir, "offline_maps").mkdirs()

        val started = runMigration(context())

        // Untouched: the Kotlin app already has a maps directory of its own.
        assertThat(started).isTrue()
        assertThat(source.isDirectory).isTrue()
    }

    // endregion

    // region robustness

    @Test
    fun `a missing legacy database and files dir are simply skipped`() {
        installFlutterPrefsFile()
        seedFlutterPreference("unit_system", "imperial")
        // The Flutter documents directory is not there at all.
        flutterDocumentsDir = File(temporaryFolder.root, "nonexistent_app_flutter")

        val started = runMigration(
            context = context(),
            // The REAL importer, so the missing-database path is the one under test.
            importer = FlutterDatabaseImporter(context()),
        )

        assertThat(started).isTrue()
        assertThat(mainPrefs.getBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, false)).isTrue()
        assertThat(mainPrefs.getString("unit_system", null)).isEqualTo("IMPERIAL")
    }

    // endregion
}
