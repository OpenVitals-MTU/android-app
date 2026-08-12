package tech.mmarca.openvitals.domain.model

/**
 * The Android-level permissions the app asks for, as opposed to the Health
 * Connect ones in [OnboardingPermissionCatalog]. Two different systems: these
 * go through the OS, grant nothing in Health Connect, and gate the device half
 * of the app — scanning, syncing, and telling the user when either finishes.
 *
 * Grouped by what the user recognises rather than by permission string, so a
 * row named "Bluetooth" asks for scan and connect together.
 */
enum class OsPermissionId {
    BLUETOOTH,
    NOTIFICATIONS,
    LOCATION,
    BATTERY_OPTIMIZATION,
    NOTIFICATION_FORWARDING,
}

/**
 * One row of the OS-permission step.
 *
 * [permissions] empty marks a SPECIAL row: one the runtime dialog cannot ask
 * for, only a settings screen the user has to walk through. That split drives
 * the whole request flow — the plain rows batch into a single dialog, the
 * special ones open one at a time and are re-checked when the app resumes.
 */
data class OsPermissionRow(
    val id: OsPermissionId,
    val permissions: List<String>,
    val granted: Boolean,
) {
    val isSpecial: Boolean get() = permissions.isEmpty()
}

/**
 * What this device can be asked for, already filtered: a row absent here is
 * one this OS version does not have, or one another grant has made moot (a
 * companion association covers background running, so the battery row goes).
 */
data class OsPermissionCatalog(
    val rows: List<OsPermissionRow> = emptyList(),
) {
    val allGranted: Boolean get() = rows.all { it.granted }

    /** The one batched dialog: every outstanding non-special permission. */
    val requestablePermissions: List<String>
        get() = rows.filterNot { it.granted || it.isSpecial }.flatMap { it.permissions }

    /** Outstanding settings-screen walks, in row order — the queue. */
    val outstandingSpecials: List<OsPermissionId>
        get() = rows.filter { !it.granted && it.isSpecial }.map { it.id }
}
