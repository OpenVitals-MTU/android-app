package tech.mmarca.openvitals.features.settings

internal data class SettingsScreenActions(
    val onOpenSection: (SettingsSection) -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onGrantCyclePermissions: () -> Unit,
    val onGrantDataImportPermissions: () -> Unit,
    val onImportAppleHealth: () -> Unit,
    val onImportOfflineMap: () -> Unit,
    val onCopyAppleHealthReport: (String) -> Unit,
    val onCopyAppleHealthError: (String) -> Unit,
    val onSaveAppleHealthReport: () -> Unit,
    val onSaveDebugLogs: () -> Unit,
    val onOpenManualPermissionSettings: () -> Unit,
    val onGrantPermissions: (Set<String>) -> Unit,
    val onClearCache: () -> Unit,
)
