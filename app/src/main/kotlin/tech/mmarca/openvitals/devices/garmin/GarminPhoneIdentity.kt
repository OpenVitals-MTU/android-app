package tech.mmarca.openvitals.devices.garmin

/**
 * How this phone introduces itself to a watch during the GFDI handshake.
 *
 * Gadgetbridge sends the real `BluetoothAdapter.getName()`,
 * `Build.MANUFACTURER` and `Build.DEVICE`. These are COSMETIC — the watch
 * stores them to show which phone it is paired with, and nothing in the sync
 * branches on them — so this app sends fixed strings, matching the Flutter
 * build byte for byte.
 *
 * A value class wraps this so the real values can be plumbed in later —
 * `BluetoothAdapter.name` is one call away on this side — without touching
 * any call site.
 */
data class GarminPhoneIdentity(
    val bluetoothName: String = "OpenVitals",
    val manufacturer: String = "OpenVitals",
    val model: String = "Android",
)
