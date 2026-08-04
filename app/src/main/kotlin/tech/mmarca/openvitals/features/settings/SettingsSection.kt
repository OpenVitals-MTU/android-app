package tech.mmarca.openvitals.features.settings

import androidx.annotation.StringRes
import tech.mmarca.openvitals.R

enum class SettingsSection(
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
) {
    DISPLAY(
        titleRes = R.string.settings_display_group_title,
        summaryRes = R.string.settings_display_group_body,
    ),
    ACTIVITIES(
        titleRes = R.string.settings_activities_group_title,
        summaryRes = R.string.settings_activities_group_body,
    ),
    SENSORS(
        titleRes = R.string.settings_sensors_group_title,
        summaryRes = R.string.settings_sensors_group_body,
    ),

    // Its own section, not a group inside Sensors: a sensor streams while you
    // record and a watch hands over files afterwards. They shared a screen
    // only because they share a radio.
    WATCHES(
        titleRes = R.string.settings_watches_group_title,
        summaryRes = R.string.settings_watches_group_body,
    ),
    NUTRITION(
        titleRes = R.string.settings_nutrition_group_title,
        summaryRes = R.string.settings_nutrition_group_body,
    ),
    BODY_PROFILE(
        titleRes = R.string.settings_body_profile_group_title,
        summaryRes = R.string.settings_body_profile_group_body,
    ),
    RECOVERY(
        titleRes = R.string.settings_recovery_group_title,
        summaryRes = R.string.settings_recovery_group_body,
    ),
    DATA_IMPORT(
        titleRes = R.string.settings_data_import_group_title,
        summaryRes = R.string.settings_data_import_group_body,
    ),
    DEVICE_SYNC(
        titleRes = R.string.settings_device_sync_group_title,
        summaryRes = R.string.settings_device_sync_group_body,
    ),
    HEALTH_CONNECT(
        titleRes = R.string.settings_health_connect_group_title,
        summaryRes = R.string.settings_health_connect_group_body,
    ),
    DEBUG_DIAGNOSTICS(
        titleRes = R.string.settings_debug_diagnostics_group_title,
        summaryRes = R.string.settings_debug_diagnostics_group_body,
    ),
}
