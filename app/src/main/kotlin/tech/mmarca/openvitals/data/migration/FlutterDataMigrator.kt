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
 * One-time import of the Flutter era's app-local data into the Kotlin stores, on a
 * guarded first run. Never throws, sets the one-shot flag regardless of per-step
 * failures, and never deletes a Flutter file.
 *
 * Two phases: preference writes land before `super.onCreate()` ([migrateIfNeeded]);
 * the beverage import needs the Hilt Room database ([importDatabaseAndFinish]).
 */
class FlutterDataMigrator(
    private val context: Context,
    private val reader: FlutterPrefsReader = FlutterPrefsReader(context),
    private val databaseImporter: FlutterDatabaseImporter = FlutterDatabaseImporter(context),
) {

    /**
     * Phase 1: preference and file migration. Must run before
     * `super.onCreate()`. True means call [importDatabaseAndFinish] after it.
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

    /** Phase 2: beverage catalog import plus the completion flag, written even on failure. */
    fun importDatabaseAndFinish(database: OpenVitalsDatabase) {
        step("beverage database") { databaseImporter.importBeverages(database.beverageDao()) }
        step("garmin wellness samples") {
            databaseImporter.importGarminWellness(database.garminWellnessDao())
        }
        step("completion flag") {
            targetPrefs(TargetPrefsFile.MAIN).edit()
                .putBoolean(FlutterPrefsKeyTable.MIGRATED_FLAG_KEY, true)
                .commit()
        }
        Log.i(TAG, "Flutter data migration finished.")
    }

    /** The flag is absent and the Flutter preferences file exists. A fresh install fails the stat. */
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

    /** Maps every Flutter entry through [FlutterPrefsKeyTable] and commits per target file. */
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
     * The widget ids survive the reinstall; only each selection moves from
     * the Flutter `HomeWidgetPreferences` file to the per-type Kotlin files.
     * Metric ids are validated against [tech.mmarca.openvitals.features.dashboard.DashboardWidgetId].
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
     * Moves the map packs back by rename and writes their metadata JSON,
     * which has the same shape on both sides, to `offline_maps/metadata.json`.
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

/** Resolves the Room database after `super.onCreate()`; the migrator predates the Hilt component. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FlutterMigrationEntryPoint {
    fun openVitalsDatabase(): OpenVitalsDatabase
}
