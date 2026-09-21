package tech.mmarca.openvitals.features.settings

internal data class SettingsScreenActions(
    val onOpenSection: (SettingsSection) -> Unit,
    val onOpenPrivacyPolicy: () -> Unit,
    val onOpenIssues: () -> Unit,
    val onOpenDiscussion: () -> Unit,
    val onOpenSupport: () -> Unit,
    val onSaveDebugLogs: () -> Unit,
    val onShareDebugLogs: () -> Unit,
    val onOpenManualPermissionSettings: () -> Unit,
    val onGrantPermissions: (Set<String>) -> Unit,
)
