package tech.mmarca.openvitals.devices.garmin

/**
 * How this phone introduces itself in the handshake. Cosmetic: the watch
 * only shows it. Fixed strings, matching the Flutter build.
 */
data class GarminPhoneIdentity(
    val bluetoothName: String = "OpenVitals",
    val manufacturer: String = "OpenVitals",
    val model: String = "Android",
)
