package tech.mmarca.openvitals.data.migration

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import tech.mmarca.openvitals.data.local.OpenVitalsDatabase

/**
 * One-time import of the Flutter era's app-local data into the Kotlin stores.
 *
 * The Kotlin build installs OVER the Flutter build (same `applicationId`,
 * same certificate), so `/data/data/tech.mmarca.openvitals/` survives — but
 * the Kotlin-era files it also still contains are ~1 month stale: the Flutter
 * app's own forward migration (`kotlin_data_migration.dart`) copied them and
 * never deleted them. Unlike that forward migration, this one therefore
 * OVERWRITES the Kotlin files with the Flutter values on its guarded first
 * run; without that, the app would boot on the stale Kotlin-era preferences.
 *
 * ## Guarantees (mirroring the forward migration)
 *
 * * Never throws: every step is guarded on its own, and the one-shot flag
 *   ([FlutterPrefsKeyTable.MIGRATED_FLAG_KEY]) is set at the end REGARDLESS of
 *   per-step failures — a migration that keeps failing must not retry on every
 *   launch.
 * * NEVER deletes a Flutter file. `app_flutter/openvitals.db` is preserved on
 *   disk (historically it was also the import source for the retired watch
 *   integration's `garmin_wellness_samples` table), and
 *   `FlutterSharedPreferences.xml` stays as the fallback source of truth for
 *   any copied-for-future key.
 * * Fresh installs no-op on a single file stat (no Flutter prefs file on disk).
 *
 * ## Why two phases (see `OpenVitalsApp.onCreate`)
 *
 * `@HiltAndroidApp` member-injects the Application during `super.onCreate()`,
 * and `PreferencesRepository` eagerly snapshots its preferences into
 * StateFlows at construction — so all preference writes must land BEFORE
 * `super.onCreate()`. That is [migrateIfNeeded]. The beverage import instead
 * needs the Hilt-provided Room database, which cannot exist before the Hilt
 * component does; Room singletons are created lazily on first request, so
 * resolving [OpenVitalsDatabase] through an [EntryPoint] right AFTER
 * `super.onCreate()` (and before any Activity) is both possible and safe.
 * That is [importDatabaseAndFinish].
 *
 * Writes go through raw [SharedPreferences] + `commit()` — never through the
 * repositories, which may not exist yet and whose StateFlows would go stale.
 */
class FlutterDataMigrator(
    private val context: Context,
    private val reader: FlutterPrefsReader = FlutterPrefsReader(context),
    private val databaseImporter: FlutterDatabaseImporter = FlutterDatabaseImporter(context),
) {

    /**
     * Phase 1 — all preference/file migration. MUST run before
     * `super.onCreate()`. Returns whether a migration is in progress, in which
     * case the caller must invoke [importDatabaseAndFinish] after
     * `super.onCreate()`.
     */
    fun migrateIfNeeded(): Boolean {
        val shouldMigrate = try {
            shouldMigrate()
        } catch (error: Exception) {
            Log.e(TAG, "Precondition check failed; not migrating.", error)
            false
        }
        if (!shouldMigrate) return false
        Log.i(TAG, "Importing Flutter-era user data.")

        val decodedPrefs = step("read Flutter preferences") { reader.readAll() } ?: emptyMap()
        step("preferences") { migratePreferences(decodedPrefs) }
        step("home widgets") { migrateHomeWidgets() }
        step("offline maps") { migrateOfflineMaps(decodedPrefs[FlutterPrefsKeyTable.OFFLINE_MAPS_METADATA_KEY] as? String) }
        return true
    }

    /**
     * Phase 2 — beverage catalog import plus the completion flag. Call after
     * `super.onCreate()` whenever [migrateIfNeeded] returned true. The flag is
     * written even when the import fails (never-retry).
     */
    fun importDatabaseAndFinish(database: OpenVitalsDatabase) {
        step("beverage database") { databaseImporter.importBeverages(database.beverageDao()) }
        step("completion flag") {
            targetPrefs(TargetPrefsFile.MAIN).edit()
                .putBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, true)
                .commit()
        }
        Log.i(TAG, "Flutter data migration finished.")
    }

    /**
     * Both conditions must hold: the one-shot flag is absent, and the Flutter
     * preferences file exists on disk (a fresh install fails this single stat
     * and never pays more than it).
     */
    private fun shouldMigrate(): Boolean {
        val main = targetPrefs(TargetPrefsFile.MAIN)
        if (main.getBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, false)) return false
        return File(context.dataDir, "shared_prefs/${FlutterPrefsReader.PREFS_FILE}.xml").exists()
    }

    private fun <T> step(name: String, body: () -> T): T? =
        try {
            body()
        } catch (error: Exception) {
            Log.e(TAG, "Step \"$name\" failed; continuing.", error)
            null
        }

    // region Preferences

    /**
     * Maps every decoded Flutter entry through [FlutterPrefsKeyTable] and
     * commits the writes grouped per target file. Overwrites stale Kotlin-era
     * values by design (see the class KDoc).
     */
    @SuppressLint("ApplySharedPref")
    private fun migratePreferences(decodedPrefs: Map<String, Any>) {
        val editors = mutableMapOf<TargetPrefsFile, SharedPreferences.Editor>()
        for ((key, value) in decodedPrefs) {
            when (val mapping = FlutterPrefsKeyTable.map(key, value)) {
                is KeyMapping.Write -> mapping.writes.forEach { write ->
                    val editor = editors.getOrPut(write.file) { targetPrefs(write.file).edit() }
                    editor.put(write.key, write.value)
                }
                is KeyMapping.Drop -> Log.i(TAG, "Dropping \"$key\": ${mapping.reason}.")
                is KeyMapping.Skip -> Log.w(TAG, "Skipping \"$key\": ${mapping.reason}.")
            }
        }
        editors.forEach { (file, editor) ->
            if (!editor.commit()) Log.e(TAG, "Commit to ${file.fileName} failed.")
        }
    }

    private fun SharedPreferences.Editor.put(key: String, value: TargetValue) {
        when (value) {
            is TargetValue.BooleanValue -> putBoolean(key, value.value)
            is TargetValue.IntValue -> putInt(key, value.value)
            is TargetValue.LongValue -> putLong(key, value.value)
            is TargetValue.FloatValue -> putFloat(key, value.value)
            is TargetValue.StringValue -> putString(key, value.value)
            is TargetValue.StringSetValue -> putStringSet(key, value.value)
        }
    }

    private fun targetPrefs(file: TargetPrefsFile): SharedPreferences =
        context.getSharedPreferences(file.fileName, Context.MODE_PRIVATE)

    // endregion

    // region Home widgets

    /**
     * Re-points the placed home-screen widgets at what they were showing.
     *
     * The widget instances survive the reinstall (the Kotlin receivers carry
     * the same class names, so the launcher keeps the `appWidgetId`s); only
     * each instance's selection moves. The Flutter `home_widget` plugin kept
     * every selection in one `HomeWidgetPreferences` file (plain keys, no
     * `flutter.` prefix) under `metric.<appWidgetId>.selection_id` /
     * `beverage.<appWidgetId>.selection_id`; Kotlin keeps them in one file per
     * widget type (`home_metric_widgets` / `home_quick_beverage_widgets`,
     * matching `HomeMetricWidgetSelection` / `HomeQuickBeverageWidgetSelection`).
     *
     * Metric ids are validated against the real [tech.mmarca.openvitals.features.dashboard.DashboardWidgetId]
     * (with Dart-only INTENSITY_MINUTES mapped back to CARDIO_LOAD); drink ids
     * are opaque `beverages.id` strings that stay resolvable because the drift
     * catalog is imported verbatim.
     */
    @SuppressLint("ApplySharedPref")
    private fun migrateHomeWidgets() {
        val flutterWidgetPrefs =
            context.getSharedPreferences(FLUTTER_HOME_WIDGET_PREFS, Context.MODE_PRIVATE)
        val metricEditor = context
            .getSharedPreferences(KOTLIN_METRIC_WIDGET_PREFS, Context.MODE_PRIVATE)
            .edit()
        val beverageEditor = context
            .getSharedPreferences(KOTLIN_BEVERAGE_WIDGET_PREFS, Context.MODE_PRIVATE)
            .edit()
        var wroteMetric = false
        var wroteBeverage = false

        for ((key, value) in flutterWidgetPrefs.all) {
            if (key == null || value !is String || value.isEmpty()) continue
            metricSelectionRegex.matchEntire(key)?.let { match ->
                val appWidgetId = match.groupValues[1]
                val kotlinId = FlutterPrefsKeyTable.kotlinMetricWidgetId(value)
                if (kotlinId == null) {
                    Log.w(TAG, "Metric widget $appWidgetId has no Kotlin metric for \"$value\"; skipping.")
                } else {
                    metricEditor.putString("metric_id_$appWidgetId", kotlinId)
                    wroteMetric = true
                }
            }
            beverageSelectionRegex.matchEntire(key)?.let { match ->
                beverageEditor.putString("drink_id_${match.groupValues[1]}", value)
                wroteBeverage = true
            }
        }
        if (wroteMetric) metricEditor.commit()
        if (wroteBeverage) beverageEditor.commit()
    }

    // endregion

    // region Offline maps

    /**
     * Moves the map packs back (`app_flutter/offline_maps` ->
     * `files/offline_maps`, a same-filesystem rename — packs run to hundreds
     * of megabytes) and re-homes their metadata: the Flutter build kept the
     * library JSON in the `offline_maps_metadata` preference, while Kotlin's
     * `OfflineMapMetadataStore` reads `offline_maps/metadata.json`. The JSON
     * shape is identical on both sides (the Dart store was written to
     * round-trip Kotlin payloads), so the string is written verbatim —
     * overwriting the stale Kotlin-era `metadata.json` that travelled along
     * inside the moved directory. Pack paths survive because both sides
     * reconstruct them as `<mapsDir>/<id><extension>`.
     */
    private fun migrateOfflineMaps(metadataJson: String?) {
        val source = File(FlutterDatabaseImporter.flutterDocumentsDir(context), OFFLINE_MAPS_DIR)
        val destination = File(context.filesDir, OFFLINE_MAPS_DIR)

        if (source.isDirectory && !destination.exists()) {
            if (!source.renameTo(destination)) {
                Log.e(TAG, "Could not move ${source.path} to ${destination.path}.")
            }
        }

        if (metadataJson.isNullOrEmpty()) return
        destination.mkdirs()
        val metadataFile = File(destination, OFFLINE_MAPS_METADATA_FILE)
        val temp = File(destination, "$OFFLINE_MAPS_METADATA_FILE.tmp")
        temp.writeText(metadataJson)
        if (!temp.renameTo(metadataFile)) {
            temp.delete()
            Log.e(TAG, "Could not write ${metadataFile.path}.")
        }
    }

    // endregion

    private companion object {
        const val TAG = "FlutterDataMigrator"

        /** `home_widget` plugin storage (`HomeWidgetPlugin.PREFERENCES`). */
        const val FLUTTER_HOME_WIDGET_PREFS = "HomeWidgetPreferences"

        /** `HomeMetricWidgetSelection`'s file. */
        const val KOTLIN_METRIC_WIDGET_PREFS = "home_metric_widgets"

        /** `HomeQuickBeverageWidgetSelection`'s file. */
        const val KOTLIN_BEVERAGE_WIDGET_PREFS = "home_quick_beverage_widgets"

        /** `OfflineMapRepository.MapsDirectoryName` / `MetadataFileName`. */
        const val OFFLINE_MAPS_DIR = "offline_maps"
        const val OFFLINE_MAPS_METADATA_FILE = "metadata.json"

        val metricSelectionRegex = Regex("""metric\.(\d+)\.selection_id""")
        val beverageSelectionRegex = Regex("""beverage\.(\d+)\.selection_id""")
    }
}

/**
 * Resolves the Room database for [FlutterDataMigrator.importDatabaseAndFinish]
 * after `super.onCreate()` — the migrator itself cannot be Hilt-injected
 * because it must run before the Hilt component exists.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FlutterMigrationEntryPoint {
    fun openVitalsDatabase(): OpenVitalsDatabase
}
