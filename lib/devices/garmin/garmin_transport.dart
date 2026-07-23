/// Which GFDI transport a Garmin device speaks, discovered by enumerating its
/// GATT services after bonding.
///
/// Gadgetbridge picks between these at RUNTIME, not from a model table:
/// `GarminSupport.initializeDevice` tries V2's characteristics first and falls
/// back to V1 if they are absent. So this cannot be inferred from the device
/// name — it has to be asked.
library;

/// The transport variant, plus enough of the GATT map to explain the verdict.
enum GarminTransportVariant {
  /// Single send/receive characteristic pair under service `6A4E2401-…`.
  v1,

  /// Multi-link service `6A4E2800-…` with `0x2810`/`0x2820`-style pairs. Newer
  /// watches; carries several logical channels over one connection.
  v2,

  /// Connected and enumerated, but neither variant's characteristics were
  /// found. Either not a Garmin device, or a transport this app has never seen.
  unknown,

  /// Could not connect or enumerate at all — says nothing about the device.
  unreachable,
}

/// One GATT service and the characteristics under it, as read off the device.
class GarminGattService {
  const GarminGattService({required this.uuid, required this.characteristics});

  final String uuid;

  /// Characteristic UUIDs, each with its property flags rendered for the log
  /// (`read`, `write`, `writeWithoutResponse`, `notify`, `indicate`).
  final Map<String, List<String>> characteristics;
}

/// What a probe found. Kept whole rather than reduced to the verdict: when the
/// verdict is [GarminTransportVariant.unknown] the raw map is the only thing
/// that explains why, and that is exactly the case worth reporting.
class GarminGattReport {
  const GarminGattReport({
    required this.address,
    required this.variant,
    required this.services,
  });

  final String address;
  final GarminTransportVariant variant;
  final List<GarminGattService> services;

  /// True when this app has a transport that can talk to the device.
  bool get isSupported =>
      variant == GarminTransportVariant.v1 ||
      variant == GarminTransportVariant.v2;

  /// A multi-line dump for the log — the whole point of the probe.
  String describe() {
    final buffer = StringBuffer()
      ..writeln('[GARMIN-GATT] $address variant=${variant.name} '
          'services=${services.length}');
    for (final service in services) {
      buffer.writeln('[GARMIN-GATT]   service ${service.uuid}');
      for (final entry in service.characteristics.entries) {
        buffer.writeln(
          '[GARMIN-GATT]     char ${entry.key} [${entry.value.join(",")}]',
        );
      }
    }
    return buffer.toString().trimRight();
  }
}
