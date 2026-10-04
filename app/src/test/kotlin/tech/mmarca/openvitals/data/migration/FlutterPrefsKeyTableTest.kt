package tech.mmarca.openvitals.data.migration

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FlutterPrefsKeyTableTest {

    // region Enum transcoding (Dart lowerCamel to Kotlin SCREAMING)

    @Test
    fun `dart enum names transcode to kotlin constants, multi word names included`() {
        val expected = mapOf(
            ("unit_system" to "metric") to "METRIC",
            ("unit_system" to "imperial") to "IMPERIAL",
            ("app_theme_mode" to "system") to "SYSTEM",
            ("app_theme_mode" to "amoled") to "AMOLED",
            ("activity_week_mode" to "mondayToSunday") to "MONDAY_TO_SUNDAY",
            ("activity_week_mode" to "last7Days") to "LAST_7_DAYS",
            ("chart_aggregation_mode" to "off") to "OFF",
            ("chart_aggregation_mode" to "min5") to "MIN5",
            ("chart_aggregation_mode" to "min30") to "MIN30",
            ("caffeine_sleep_sensitivity" to "insomnia") to "INSOMNIA",
            ("caffeine_alcohol_use" to "occasional") to "OCCASIONAL",
            ("caffeine_habituation" to "moderate") to "MODERATE",
            ("caffeine_cyp1a2_genotype" to "slow") to "SLOW",
            ("caffeine_ahr_genotype" to "fast") to "FAST",
            ("caffeine_hormonal_status" to "oralContraceptive") to "ORAL_CONTRACEPTIVE",
        )

        assertThat(expected.keys.associateWith { (key, value) -> mappedMainValue(key, value) })
            .isEqualTo(expected.mapValues { TargetValue.StringValue(it.value) })
    }

    @Test
    fun `stale kotlin era enum values still resolve`() {
        // The forward migration copied Kotlin values verbatim; SCREAMING names must fold-match themselves.
        assertThat(mappedMainValue("unit_system", "METRIC"))
            .isEqualTo(TargetValue.StringValue("METRIC"))
        assertThat(mappedMainValue("caffeine_hormonal_status", "ORAL_CONTRACEPTIVE"))
            .isEqualTo(TargetValue.StringValue("ORAL_CONTRACEPTIVE"))
    }

    @Test
    fun `unknown enum values are skipped not written`() {
        assertThat(FlutterPrefsKeyTable.map("app_theme_mode", "purple"))
            .isInstanceOf(KeyMapping.Skip::class.java)
        assertThat(FlutterPrefsKeyTable.map("unit_system", 3L))
            .isInstanceOf(KeyMapping.Skip::class.java)
    }

    // endregion

    // region Detail ranges

    @Test
    fun `detail ranges transcode time range names`() {
        val keys = listOf(
            "detail_range_steps", "detail_range_calories", "detail_range_activities",
            "detail_range_sleep", "detail_range_heart", "detail_range_body",
            "detail_range_hydration", "detail_range_nutrition", "detail_range_mindfulness",
        )
        val dartNames = listOf("day", "week", "month", "year")
        val kotlinNames = listOf("DAY", "WEEK", "MONTH", "YEAR").map { TargetValue.StringValue(it) }

        assertThat(keys.associateWith { key -> dartNames.map { mappedMainValue(key, it) } })
            .isEqualTo(keys.associateWith { kotlinNames })
    }

    @Test
    fun `dart hrr range key is renamed to the kotlin key`() {
        assertThat(FlutterPrefsKeyTable.map("detail_range_hrr", "month")).isEqualTo(
            KeyMapping.Write(
                TargetWrite(TargetPrefsFile.MAIN, "detail_range_heart_rate_recovery", TargetValue.StringValue("MONTH")),
            ),
        )
    }

    @Test
    fun `stale kotlin hrr range key is dropped`() {
        assertThat(FlutterPrefsKeyTable.map("detail_range_heart_rate_recovery", "WEEK"))
            .isInstanceOf(KeyMapping.Drop::class.java)
    }

    // endregion

    // region app_language

    @Test
    fun `app language maps dart names to kotlin storage values`() {
        assertThat(mappedMainValue("app_language", "english"))
            .isEqualTo(TargetValue.StringValue("en"))
        assertThat(mappedMainValue("app_language", "spanish"))
            .isEqualTo(TargetValue.StringValue("es"))
        assertThat(mappedMainValue("app_language", "german"))
            .isEqualTo(TargetValue.StringValue("de"))
        assertThat(mappedMainValue("app_language", "italian"))
            .isEqualTo(TargetValue.StringValue("it"))
        assertThat(mappedMainValue("app_language", "estonian"))
            .isEqualTo(TargetValue.StringValue("et"))
        assertThat(mappedMainValue("app_language", "system"))
            .isEqualTo(TargetValue.StringValue("SYSTEM"))
    }

    @Test
    fun `app language tolerates stale kotlin storage values`() {
        assertThat(mappedMainValue("app_language", "en"))
            .isEqualTo(TargetValue.StringValue("en"))
        assertThat(mappedMainValue("app_language", "SYSTEM"))
            .isEqualTo(TargetValue.StringValue("SYSTEM"))
    }

    @Test
    fun `unknown app language is skipped`() {
        assertThat(FlutterPrefsKeyTable.map("app_language", "klingon"))
            .isInstanceOf(KeyMapping.Skip::class.java)
    }

    // endregion

    // region Special cases

    @Test
    fun `mindfulness flag writes both the new and the legacy key`() {
        val mapping = FlutterPrefsKeyTable.map("health_connect_mindfulness_enabled", true)
        val writes = (mapping as KeyMapping.Write).writes
        assertThat(writes).containsExactly(
            TargetWrite(
                TargetPrefsFile.MAIN,
                "health_connect_mindfulness_enabled",
                TargetValue.BooleanValue(true),
            ),
            TargetWrite(TargetPrefsFile.MAIN, "mindfulness_opt_in", TargetValue.BooleanValue(true)),
        )
    }

    @Test
    fun `recent hydration amounts list becomes a comma joined string`() {
        assertThat(mappedMainValue("recent_hydration_amounts_milliliters", listOf("250.0", "500.0")))
            .isEqualTo(TargetValue.StringValue("250.0,500.0"))
        assertThat(mappedMainValue("recent_hydration_amounts_milliliters", emptyList<String>()))
            .isEqualTo(TargetValue.StringValue(""))
    }

    @Test
    fun `ble registry routes to its own file under the devices key verbatim`() {
        val payload = """[{"id":"x","kind":"watch","integration":"garmin"}]"""
        assertThat(FlutterPrefsKeyTable.map("ble_sensor_devices", payload)).isEqualTo(
            KeyMapping.Write(TargetWrite(TargetPrefsFile.BLE_DEVICES, "devices", TargetValue.StringValue(payload))),
        )
    }

    @Test
    fun `activity markers route to the marker file keeping their key`() {
        assertThat(FlutterPrefsKeyTable.map("activity_markers_abc123", "{}")).isEqualTo(
            KeyMapping.Write(
                TargetWrite(TargetPrefsFile.ACTIVITY_MARKERS, "activity_markers_abc123", TargetValue.StringValue("{}")),
            ),
        )
    }

    @Test
    fun `offline maps metadata is not written as a preference`() {
        assertThat(FlutterPrefsKeyTable.map(FlutterPrefsKeyTable.OFFLINE_MAPS_METADATA_KEY, "{}"))
            .isInstanceOf(KeyMapping.Drop::class.java)
    }

    // endregion

    // region Drop list

    @Test
    fun `unportable and bookkeeping keys are dropped`() {
        val keys = listOf(
            "dashboard_widget_order",
            "dashboard_ring_order",
            "dashboard_hidden_widgets",
            "kotlin_data_migrated",
            "flutter_data_migrated",
            "bodyEnergyPrefsTimelinePurged.v1",
        )

        assertThat(keys.associateWith { FlutterPrefsKeyTable.map(it, "anything") is KeyMapping.Drop })
            .isEqualTo(keys.associateWith { true })
    }

    @Test
    fun `body energy setup epoch is honored as a typed copy`() {
        // The Flutter-era body-energy preferences are honored wholesale.
        assertThat(mappedMainValue("body_energy_setup_epoch", 1721000000L))
            .isEqualTo(TargetValue.IntValue(1721000000))
    }

    @Test
    fun `transient recording state is dropped`() {
        assertThat(FlutterPrefsKeyTable.map("status", "recording"))
            .isInstanceOf(KeyMapping.Drop::class.java)
        assertThat(FlutterPrefsKeyTable.map("points", "1;2;3"))
            .isInstanceOf(KeyMapping.Drop::class.java)
        assertThat(FlutterPrefsKeyTable.map("start_time", 123L))
            .isInstanceOf(KeyMapping.Drop::class.java)
        assertThat(FlutterPrefsKeyTable.map("dashboard_fields", "x"))
            .isInstanceOf(KeyMapping.Drop::class.java)
    }

    @Test
    fun `flutter side body energy caches are dropped`() {
        assertThat(FlutterPrefsKeyTable.map("baseline|2026-07-10|12345", "payload"))
            .isInstanceOf(KeyMapping.Drop::class.java)
    }

    // endregion

    // region Typed copies

    @Test
    fun `booleans copy under the same key`() {
        assertThat(mappedMainValue("onboarding_done", true))
            .isEqualTo(TargetValue.BooleanValue(true))
        assertThat(mappedMainValue("dynamic_color", false))
            .isEqualTo(TargetValue.BooleanValue(false))
    }

    @Test
    fun `dart ints become kotlin ints`() {
        assertThat(mappedMainValue("sleep_night_start_hour", 22L))
            .isEqualTo(TargetValue.IntValue(22))
        assertThat(mappedMainValue("high_heart_rate_threshold_bpm", 150L))
            .isEqualTo(TargetValue.IntValue(150))
        assertThat(mappedMainValue("last_activity_exercise_type", Int.MIN_VALUE.toLong()))
            .isEqualTo(TargetValue.IntValue(Int.MIN_VALUE))
    }

    @Test
    fun `keys kotlin reads with getLong stay long`() {
        assertThat(mappedMainValue("privacy_policy_accepted_at", 1_752_000_000_000L))
            .isEqualTo(TargetValue.LongValue(1_752_000_000_000L))
        assertThat(mappedMainValue("body_energy_watch_fit_watermark_millis", 5L))
            .isEqualTo(TargetValue.LongValue(5L))
    }

    @Test
    fun `unknown out of int range values stay long instead of truncating`() {
        assertThat(mappedMainValue("some_future_millis_key", 9_000_000_000L))
            .isEqualTo(TargetValue.LongValue(9_000_000_000L))
    }

    @Test
    fun `doubles become kotlin floats`() {
        assertThat(mappedMainValue("goal_steps", 12000.0))
            .isEqualTo(TargetValue.FloatValue(12000.0f))
        assertThat(mappedMainValue("hydration_daily_goal_liters", 2.5))
            .isEqualTo(TargetValue.FloatValue(2.5f))
        assertThat(mappedMainValue("body_profile_weight_kg", 71.3))
            .isEqualTo(TargetValue.FloatValue(71.3f))
    }

    @Test
    fun `string lists become string sets`() {
        assertThat(mappedMainValue("custom_hydration_drinks", listOf("a", "b")))
            .isEqualTo(TargetValue.StringSetValue(setOf("a", "b")))
        assertThat(mappedMainValue("hydration_container_volume_milliliters", listOf("Cup=250.0")))
            .isEqualTo(TargetValue.StringSetValue(setOf("Cup=250.0")))
        assertThat(mappedMainValue("acknowledged_permissions", listOf("p1", "p2")))
            .isEqualTo(TargetValue.StringSetValue(setOf("p1", "p2")))
        assertThat(
            mappedMainValue("acknowledged_feature_permissions_hydration", listOf("p1")),
        ).isEqualTo(TargetValue.StringSetValue(setOf("p1")))
    }

    @Test
    fun `strings with kotlin compatible wire formats copy verbatim`() {
        assertThat(mappedMainValue("manual_entry_widget_order", "WEIGHT,STEPS"))
            .isEqualTo(TargetValue.StringValue("WEIGHT,STEPS"))
        assertThat(mappedMainValue("metric_detail_section_order", "CHART|STATS"))
            .isEqualTo(TargetValue.StringValue("CHART|STATS"))
        assertThat(mappedMainValue("activity_recording_dashboard_layout_running", "a|b"))
            .isEqualTo(TargetValue.StringValue("a|b"))
        assertThat(mappedMainValue("caffeine_bedtime", "23:30"))
            .isEqualTo(TargetValue.StringValue("23:30"))
    }

    @Test
    fun `copy for future garmin keys keep their typed values`() {
        assertThat(mappedMainValue("garmin_notifications_enabled", true))
            .isEqualTo(TargetValue.BooleanValue(true))
        assertThat(mappedMainValue("garmin_notifications_disclosure_accepted", true))
            .isEqualTo(TargetValue.BooleanValue(true))
        assertThat(
            mappedMainValue("garmin_notifications_blocked_packages", listOf("com.spam.app")),
        ).isEqualTo(TargetValue.StringSetValue(setOf("com.spam.app")))
    }

    // endregion

    // region Home widget metric ids

    @Test
    fun `known metric widget ids pass through`() {
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("STEPS")).isEqualTo("STEPS")
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("BODY_ENERGY")).isEqualTo("BODY_ENERGY")
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("WEEKLY_CARDIO_LOAD"))
            .isEqualTo("WEEKLY_CARDIO_LOAD")
    }

    @Test
    fun `dart only intensity minutes maps back to cardio load`() {
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("INTENSITY_MINUTES"))
            .isEqualTo("CARDIO_LOAD")
    }

    @Test
    fun `unknown metric widget ids are rejected`() {
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("NOT_A_METRIC")).isNull()
        assertThat(FlutterPrefsKeyTable.kotlinMetricWidgetId("steps")).isNull()
    }

    // endregion

    private fun mappedMainValue(key: String, value: Any): TargetValue {
        val mapping = FlutterPrefsKeyTable.map(key, value)
        assertThat(mapping).isInstanceOf(KeyMapping.Write::class.java)
        val write = (mapping as KeyMapping.Write).writes.single()
        assertThat(write.file).isEqualTo(TargetPrefsFile.MAIN)
        assertThat(write.key).isEqualTo(key)
        return write.value
    }
}
