package tech.mmarca.openvitals.data.migration

import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.CaffeineAlcoholUse
import tech.mmarca.openvitals.domain.preferences.CaffeineGenotype
import tech.mmarca.openvitals.domain.preferences.CaffeineHabituation
import tech.mmarca.openvitals.domain.preferences.CaffeineHormonalStatus
import tech.mmarca.openvitals.domain.preferences.CaffeineSleepSensitivity
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId

/** A SharedPreferences file the migration writes into. */
enum class TargetPrefsFile(val fileName: String) {
    /** `PreferencesRepository.PREFS_FILE` — the main preferences file. */
    MAIN("openvitals_prefs"),

    /** `BleDeviceRepository.PREFS_FILE` — the paired BLE sensor registry. */
    BLE_DEVICES("ble_sensor_devices"),

    /** `ActivityMarkerRepository.PreferencesName` — activity marker notes. */
    ACTIVITY_MARKERS("activity_marker_metadata"),
}

/** A value typed the way its Kotlin reader expects it on disk. */
sealed interface TargetValue {
    data class BooleanValue(val value: Boolean) : TargetValue
    data class IntValue(val value: Int) : TargetValue
    data class LongValue(val value: Long) : TargetValue
    data class FloatValue(val value: Float) : TargetValue
    data class StringValue(val value: String) : TargetValue
    data class StringSetValue(val value: Set<String>) : TargetValue
}

/** One write the migration should perform. */
data class TargetWrite(
    val file: TargetPrefsFile,
    val key: String,
    val value: TargetValue,
)

/** The outcome of mapping one Flutter preference entry. */
sealed interface KeyMapping {
    /** Write these entries (usually one; two for the mindfulness dual-write). */
    data class Write(val writes: List<TargetWrite>) : KeyMapping {
        constructor(vararg writes: TargetWrite) : this(writes.toList())
    }

    /** Deliberately not migrated. */
    data class Drop(val reason: String) : KeyMapping

    /** The key is wanted but this value cannot be translated — log loudly. */
    data class Skip(val reason: String) : KeyMapping
}

/**
 * Pure mapping table from a decoded Flutter preference entry (see
 * [FlutterPrefsReader]) to the Kotlin app's on-disk representation.
 *
 * This is the exact inverse of the Flutter app's forward migration
 * (`mobile-app/lib/data/migration/kotlin_data_migration.dart`): key names are
 * identical on both sides (the Dart repository was a 1:1 port that kept
 * Kotlin's storage keys), so most entries are a typed copy. The exceptions —
 * enums persisted as Dart lowerCamel `.name`s, the `app_language` storage
 * value, the renamed `detail_range_hrr` key, the flattened BLE registry key,
 * the list-vs-joined-string hydration history and the keys that are
 * meaningless on this side — are enumerated here.
 *
 * Types matter: SharedPreferences readers throw [ClassCastException] on a type
 * mismatch, so every value is emitted as the exact type the current Kotlin
 * reader uses (`getInt` vs `getLong` vs `getFloat` vs `getStringSet`).
 */
object FlutterPrefsKeyTable {

    /** Handled by the offline-maps step of the migrator, not as a pref write. */
    const val OFFLINE_MAPS_METADATA_KEY = "offline_maps_metadata"

    /** The key the completed migration is flagged under (in [TargetPrefsFile.MAIN]). */
    const val MIGRATED_FLAG_KEY = "flutter_data_migrated"

    /** Maps one decoded Flutter entry. [value] must be a [FlutterPrefsReader] value type. */
    fun map(key: String, value: Any): KeyMapping {
        if (key == MIGRATED_FLAG_KEY || key == "kotlin_data_migrated") {
            // Never let legacy data forge (or resurrect) a migration flag.
            return KeyMapping.Drop("migration bookkeeping flag")
        }
        if (key in droppedKeys) return KeyMapping.Drop(droppedKeys.getValue(key))
        if (key.startsWith(BODY_ENERGY_BASELINE_CACHE_PREFIX)) {
            return KeyMapping.Drop("derived Flutter-side body-energy cache")
        }
        if (key.startsWith(GARMIN_KEY_PREFIX) || key.startsWith(BODY_ENERGY_WATCH_KEY_PREFIX)) {
            // The watch integration is retired; watch data arrives through
            // Health Connect (e.g. via Gadgetbridge). No Kotlin reader exists
            // for any of these, so copying them would only strand orphans.
            return KeyMapping.Drop("retired watch integration state with no reader")
        }

        if (key == BLE_DEVICES_KEY) {
            // Kotlin keeps the same JSON under file "ble_sensor_devices", key
            // "devices" — the one key in the whole migration whose name differs
            // from its file. Copied VERBATIM, watch entries included: the
            // Kotlin decoder tolerates the extra `kind`/`integration` fields
            // and simply ignores watch-era entries (the app no longer links to
            // watches), so nothing is lost or corrupted in the copy.
            if (value !is String || value.isEmpty()) return KeyMapping.Skip("BLE registry is not a string")
            return KeyMapping.Write(TargetWrite(TargetPrefsFile.BLE_DEVICES, "devices", TargetValue.StringValue(value)))
        }
        if (key.startsWith(ACTIVITY_MARKERS_PREFIX)) {
            if (value !is String || value.isEmpty()) return KeyMapping.Skip("activity marker is not a string")
            return KeyMapping.Write(TargetWrite(TargetPrefsFile.ACTIVITY_MARKERS, key, TargetValue.StringValue(value)))
        }
        if (key == OFFLINE_MAPS_METADATA_KEY) {
            return KeyMapping.Drop("re-homed to offline_maps/metadata.json by the offline-maps step")
        }

        if (key == "app_language") return mapAppLanguage(value)
        if (key == "health_connect_mindfulness_enabled") {
            if (value !is Boolean) return KeyMapping.Skip("expected a boolean")
            // The Kotlin repository reads health_connect_mindfulness_enabled
            // with the legacy mindfulness_opt_in as fallback default; writing
            // both keeps every read path consistent.
            return KeyMapping.Write(
                mainWrite(key, TargetValue.BooleanValue(value)),
                mainWrite("mindfulness_opt_in", TargetValue.BooleanValue(value)),
            )
        }
        if (key == "recent_hydration_amounts_milliliters") {
            // Flutter: plugin-encoded string LIST of doubles. Kotlin: one
            // comma-joined string (see PreferencesRepository.recentHydrationAmountsMilliliters).
            if (value !is List<*>) return KeyMapping.Skip("expected a string list")
            val joined = value.filterIsInstance<String>().joinToString(",") { it.trim() }
            return KeyMapping.Write(mainWrite(key, TargetValue.StringValue(joined)))
        }
        if (key == DART_HRR_RANGE_KEY) {
            // Dart renamed Kotlin's detail_range_heart_rate_recovery to
            // detail_range_hrr; translate the value AND the key back.
            return mapEnum(key, value, timeRangeNames, targetKey = KOTLIN_HRR_RANGE_KEY)
        }
        if (key == KOTLIN_HRR_RANGE_KEY) {
            // Dart never writes this key: it can only be the stale Kotlin-era
            // value the forward migration copied into the Flutter file. The
            // fresh Flutter value lives under detail_range_hrr and wins.
            return KeyMapping.Drop("superseded by $DART_HRR_RANGE_KEY")
        }

        val enumNames = enumValuedKeys[key]
        if (enumNames != null) return mapEnum(key, value, enumNames)

        return mapTyped(key, value)
    }

    /**
     * The Kotlin `DashboardWidgetId` name for a home-metric-widget selection
     * stored by the Flutter build (`DashboardMetric.storageName`), or null when
     * it cannot be represented (skip + log; the user re-picks the metric).
     *
     * Dart split Kotlin's CARDIO_LOAD into WEEKLY_CARDIO_LOAD and
     * INTENSITY_MINUTES. Today's Kotlin enum has WEEKLY_CARDIO_LOAD (kept
     * as-is) but no INTENSITY_MINUTES, which maps back to CARDIO_LOAD.
     */
    fun kotlinMetricWidgetId(storedId: String): String? {
        val mapped = if (storedId == "INTENSITY_MINUTES") "CARDIO_LOAD" else storedId
        return DashboardWidgetId.entries.firstOrNull { it.name == mapped }?.name
    }

    // region Enum transcoding

    /**
     * Resolves a Dart enum `.name` (lowerCamelCase) against the REAL Kotlin
     * enum constants by fold-matching — strip `_`, lowercase — so
     * `oralContraceptive` matches `ORAL_CONTRACEPTIVE`, `last7Days` matches
     * `LAST_7_DAYS`, and (stale, Kotlin-era) `MONDAY_TO_SUNDAY` still matches
     * itself. A value with no Kotlin counterpart is skipped, never written as
     * garbage.
     */
    private fun mapEnum(
        key: String,
        value: Any,
        kotlinNames: List<String>,
        targetKey: String = key,
    ): KeyMapping {
        if (value !is String || value.isEmpty()) return KeyMapping.Skip("expected an enum name string")
        val wanted = fold(value)
        val match = kotlinNames.firstOrNull { fold(it) == wanted }
            ?: return KeyMapping.Skip("no Kotlin enum constant matches \"$value\"")
        return KeyMapping.Write(mainWrite(targetKey, TargetValue.StringValue(match)))
    }

    private fun fold(name: String): String = name.replace("_", "").lowercase()

    /**
     * `app_language` differs in WHAT is stored, not just its casing: Dart
     * persists the enum `.name` ("english", "system"), Kotlin persists
     * `AppLanguage.storageValue` — the BCP-47 tag ("en") or the literal
     * "SYSTEM". Raw tags are also accepted in case a stale Kotlin-era value
     * ("en"/"SYSTEM") survived in the Flutter file.
     */
    private fun mapAppLanguage(value: Any): KeyMapping {
        if (value !is String || value.isEmpty()) return KeyMapping.Skip("expected a language name string")
        val wanted = fold(value)
        val match = knownLanguages.firstOrNull { language ->
            fold(language.name) == wanted || language.languageTag == value
        } ?: return KeyMapping.Skip("no Kotlin language matches \"$value\"")
        return KeyMapping.Write(mainWrite("app_language", TargetValue.StringValue(match.storageValue)))
    }

    private val knownLanguages = listOf(
        AppLanguage.SYSTEM,
        AppLanguage.ENGLISH,
        AppLanguage.SPANISH,
        AppLanguage.GERMAN,
        AppLanguage.ITALIAN,
        AppLanguage.ESTONIAN,
    )

    // endregion

    // region Typed copy

    /**
     * The default: same key, value re-typed for its Kotlin reader.
     *
     * * `Boolean` -> Boolean.
     * * `Long` (every Dart int) -> Kotlin `Int`, except the two keys Kotlin
     *   reads with `getLong` ([longKeys]); unknown keys whose value exceeds Int
     *   range also stay Long rather than truncate.
     * * `Double` -> Kotlin `Float` (every fractional preference is `getFloat`).
     * * `List<String>` -> Kotlin `StringSet` (`acknowledged_permissions`,
     *   `acknowledged_feature_permissions_*`, `custom_hydration_drinks`,
     *   `hydration_container_volume_milliliters` are all `getStringSet`).
     *
     * Keys with no current Kotlin reader (e.g. `apple_health_import_*`) are
     * still copied typed under the same name — harmless, and future readers
     * keep Flutter's key names. Watch-only families are dropped earlier in
     * [map] instead.
     */
    private fun mapTyped(key: String, value: Any): KeyMapping =
        when (value) {
            is Boolean -> KeyMapping.Write(mainWrite(key, TargetValue.BooleanValue(value)))
            is Long ->
                if (key in longKeys || value !in Int.MIN_VALUE..Int.MAX_VALUE) {
                    KeyMapping.Write(mainWrite(key, TargetValue.LongValue(value)))
                } else {
                    KeyMapping.Write(mainWrite(key, TargetValue.IntValue(value.toInt())))
                }
            is Double -> KeyMapping.Write(mainWrite(key, TargetValue.FloatValue(value.toFloat())))
            is String -> KeyMapping.Write(mainWrite(key, TargetValue.StringValue(value)))
            is List<*> -> KeyMapping.Write(
                mainWrite(key, TargetValue.StringSetValue(value.filterIsInstance<String>().toSet())),
            )
            else -> KeyMapping.Skip("unsupported value type ${value::class.java.simpleName}")
        }

    private fun mainWrite(key: String, value: TargetValue): TargetWrite =
        TargetWrite(TargetPrefsFile.MAIN, key, value)

    /** The keys the Kotlin repository reads with `getLong`. */
    private val longKeys = setOf(
        "privacy_policy_accepted_at",
    )

    // endregion

    // region Key tables

    private val timeRangeNames = TimeRange.entries.map { it.name }

    /**
     * Enum-valued keys, resolved against the real Kotlin enum constants. The
     * detail-range family comes from [PeriodRangePreferenceKey] itself so a new
     * screen cannot fall out of sync; `detail_range_heart_rate_recovery` is
     * excluded because it is dropped/renamed above.
     */
    private val enumValuedKeys: Map<String, List<String>> = buildMap {
        put("unit_system", UnitSystem.entries.map { it.name })
        put("app_theme_mode", AppThemeMode.entries.map { it.name })
        put("activity_week_mode", ActivityWeekMode.entries.map { it.name })
        put("chart_aggregation_mode", ChartAggregationMode.entries.map { it.name })
        put("caffeine_sleep_sensitivity", CaffeineSleepSensitivity.entries.map { it.name })
        put("caffeine_alcohol_use", CaffeineAlcoholUse.entries.map { it.name })
        put("caffeine_habituation", CaffeineHabituation.entries.map { it.name })
        put("caffeine_cyp1a2_genotype", CaffeineGenotype.entries.map { it.name })
        put("caffeine_ahr_genotype", CaffeineGenotype.entries.map { it.name })
        put("caffeine_hormonal_status", CaffeineHormonalStatus.entries.map { it.name })
        for (rangeKey in PeriodRangePreferenceKey.entries) {
            if (rangeKey.storageKey == KOTLIN_HRR_RANGE_KEY) continue
            put(rangeKey.storageKey, timeRangeNames)
        }
    }

    /** Verbatim from `activity_recording_serialization.dart`'s `_recordingKeys`. */
    private val flutterTransientRecordingKeys = listOf(
        "status", "recording_kind", "activity_type_id", "exercise_type",
        "start_time", "end_time", "paused_started_at", "total_paused_millis",
        "pause_intervals", "points", "route_break_indexes", "manual_laps",
        "markers", "distance_meters", "elevation_meters", "elevation_lost_meters",
        "barometer_elevation_gained_meters", "barometer_elevation_lost_meters",
        "has_barometer_elevation", "last_barometer_altitude_meters",
        "current_speed_meters_per_second", "max_speed_meters_per_second",
        "gps_status", "keep_screen_on_during_recording", "auto_idle_enabled",
        "auto_idle_timeout_millis", "last_movement_at", "total_idle_millis",
        "repetition_count", "current_set_repetition_count", "repetition_sets",
        "repetition_rest_seconds", "current_set_started_at", "rest_started_at",
        "accumulated_rest_millis", "last_accuracy_meters", "last_location_time",
        "dropped_point_count", "error_message", "dashboard_template",
        "dashboard_fields",
    )

    /**
     * Keys deliberately not migrated, with the reason logged for each.
     *
     * The dashboard trio is unportable by vocabulary: Kotlin persists
     * `DashboardWidgetId` enum names while the Flutter dashboard persisted tile
     * TITLES (plus a separate ring order) — copying them would inject ids the
     * Kotlin grid can never match. One manual re-order is the whole cost.
     *
     * The `_recordingKeys` block mirrors Flutter's
     * `activity_recording_serialization.dart`: a transient in-flight recording
     * snapshot, stored under generic key names (`status`, `points`, …) that
     * would land as junk in `openvitals_prefs`. The forward migration skipped
     * in-flight recordings for the same reason.
     */
    private val droppedKeys: Map<String, String> = buildMap {
        val dashboard = "Flutter dashboard layout uses tile titles, not DashboardWidgetId names"
        put("dashboard_widget_order", dashboard)
        put("dashboard_ring_order", dashboard)
        put("dashboard_hidden_widgets", dashboard)
        // body_energy_setup_epoch is deliberately NOT dropped: the Flutter-era
        // body-energy preferences are honored wholesale (user decision), so it
        // rides the generic typed copy for the body-energy chain to consume.
        put("bodyEnergyPrefsTimelinePurged.v1", "Flutter-side cache-purge bookkeeping")

        val transient = "transient in-flight activity recording state"
        for (recordingKey in flutterTransientRecordingKeys) put(recordingKey, transient)
    }

    private const val BLE_DEVICES_KEY = "ble_sensor_devices"
    private const val ACTIVITY_MARKERS_PREFIX = "activity_markers_"
    private const val BODY_ENERGY_BASELINE_CACHE_PREFIX = "baseline|"
    private const val GARMIN_KEY_PREFIX = "garmin_"
    private const val BODY_ENERGY_WATCH_KEY_PREFIX = "body_energy_watch_"
    private const val DART_HRR_RANGE_KEY = "detail_range_hrr"
    private const val KOTLIN_HRR_RANGE_KEY = "detail_range_heart_rate_recovery"

    // endregion
}
