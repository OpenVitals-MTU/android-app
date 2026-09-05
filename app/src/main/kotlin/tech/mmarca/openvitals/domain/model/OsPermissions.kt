package tech.mmarca.openvitals.domain.model

/**
 * The Android-level permissions the app asks for, as opposed to Health
 * Connect's. Grouped by what the user recognises.
 */
enum class OsPermissionId {
    BLUETOOTH,
    NOTIFICATIONS,
    LOCATION,
    BATTERY_OPTIMIZATION,
    NOTIFICATION_FORWARDING,
}

/**
 * One row of the OS-permission step. Empty [permissions] marks a special
 * row: only a settings screen can grant it, one at a time.
 */
data class OsPermissionRow(
    val id: OsPermissionId,
    val permissions: List<String>,
    val granted: Boolean,
) {
    val isSpecial: Boolean get() = permissions.isEmpty()
}

/** What this device can be asked for, already filtered by OS version and by other grants. */
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
