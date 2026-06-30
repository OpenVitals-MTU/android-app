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
    CALORIES(
        titleRes = R.string.settings_calories_group_title,
        summaryRes = R.string.settings_calories_group_body,
    ),
    SLEEP(
        titleRes = R.string.settings_sleep_group_title,
        summaryRes = R.string.settings_sleep_group_body,
    ),
    BODY_ENERGY(
        titleRes = R.string.settings_body_energy_group_title,
        summaryRes = R.string.settings_body_energy_group_body,
    ),
    CYCLE(
        titleRes = R.string.settings_cycle_group_title,
        summaryRes = R.string.settings_cycle_group_body,
    ),
    DATA_IMPORT(
        titleRes = R.string.settings_data_import_group_title,
        summaryRes = R.string.settings_data_import_group_body,
    ),
    HEALTH_CONNECT(
        titleRes = R.string.settings_health_connect_group_title,
        summaryRes = R.string.settings_health_connect_group_body,
    ),
    PERMISSIONS(
        titleRes = R.string.settings_permissions_group_title,
        summaryRes = R.string.settings_permissions_group_body,
    ),
    DEBUG_DIAGNOSTICS(
        titleRes = R.string.settings_debug_diagnostics_group_title,
        summaryRes = R.string.settings_debug_diagnostics_group_body,
    ),
}
