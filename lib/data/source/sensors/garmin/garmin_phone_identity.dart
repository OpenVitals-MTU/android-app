/// How this phone introduces itself to a watch during the GFDI handshake.
///
/// Gadgetbridge sends the real `BluetoothAdapter.getName()`, `Build.MANUFACTURER`
/// and `Build.DEVICE`. These are COSMETIC — the watch stores them to show which
/// phone it is paired with, and nothing in the sync branches on them — so this
/// app sends fixed strings rather than adding a device-info plugin (and, with
/// it, another Kotlin-Gradle-plugin dependency to keep clear of the AGP 9 build).
///
/// A provider wraps this so the real values can be plumbed in later — the
/// `bluetooth_sync_native` plugin already holds a `BluetoothAdapter` and could
/// expose the local name in one host method — without touching any call site.
class GarminPhoneIdentity {
  const GarminPhoneIdentity({
    this.bluetoothName = 'OpenVitals',
    this.manufacturer = 'OpenVitals',
    this.model = 'Android',
  });

  final String bluetoothName;
  final String manufacturer;
  final String model;
}
