package tech.mmarca.openvitals.features.settings

import androidx.compose.runtime.Composable

/** Sensors: the paired BLE devices. The section owns its ViewModel, so the route comes straight here. */
@Composable
fun SensorsSettingsScreen() {
    SettingsSectionList {
        item { BleDevicesSettingsSection() }
    }
}
