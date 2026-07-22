import 'package:flutter_blue_plus/flutter_blue_plus.dart';

import '../../../../domain/model/garmin_transport.dart';
import '../../../../domain/port/garmin_transport_probe.dart';
import '../ble/ble_uuids.dart';
import 'garmin_log.dart';

/// [GarminTransportProbe] over `flutter_blue_plus`.
///
/// The first piece of the GFDI transport: before anything can be sent to a
/// watch, this establishes which characteristics to send it on. It connects,
/// enumerates, classifies and hangs up — no GFDI traffic, no writes.
///
/// The full service map is logged, not just the verdict. A watch that comes back
/// [GarminTransportVariant.unknown] is the case that needs diagnosing, and the
/// map is the only evidence that explains it.
class GarminGattProbe implements GarminTransportProbe {
  const GarminGattProbe();

  /// Long, because the connect happens right after bonding, when the watch may
  /// still be settling its encrypted link.
  static const Duration _connectTimeout = Duration(seconds: 20);

  /// Gadgetbridge's V2 multi-link characteristic window: receive handles run
  /// `0x2810`..`0x2814`, each paired with a send handle `+0x10`
  /// (`CommunicatorV2.initializeDevice`).
  static const int _v2FirstReceiveHandle = 0x2810;
  static const int _v2LastReceiveHandle = 0x2814;
  static const int _v2SendHandleOffset = 0x10;

  /// `6A4E%04X-667B-11E3-949A-0800200C9A66` — Gadgetbridge's `BASE_UUID`, with
  /// the 16-bit handle spliced into the first group.
  static String _garminUuid(int handle) =>
      '6a4e${handle.toRadixString(16).padLeft(4, '0')}'
      '-667b-11e3-949a-0800200c9a66';

  @override
  Future<GarminGattReport> probe(String address) async {
    if (!await FlutterBluePlus.isSupported) {
      return _unreachable(address);
    }
    final device = BluetoothDevice.fromId(address);
    try {
      await device.connect(
        license: License.nonprofit,
        timeout: _connectTimeout,
      );
    } catch (error) {
      garminLog('[GARMIN-GATT] $address connect failed: $error');
      return _unreachable(address);
    }

    try {
      final discovered = await device.discoverServices();
      final services = [
        for (final service in discovered)
          GarminGattService(
            uuid: service.uuid.str128,
            characteristics: {
              for (final characteristic in service.characteristics)
                characteristic.uuid.str128:
                    _properties(characteristic.properties),
            },
          ),
      ];
      final report = GarminGattReport(
        address: address,
        variant: _classify(services),
        services: services,
      );
      // debugPrint chunks long strings; emit line by line so nothing is dropped
      // or reordered in logcat.
      for (final line in report.describe().split('\n')) {
        garminLog(line);
      }
      return report;
    } catch (error) {
      garminLog('[GARMIN-GATT] $address discoverServices failed: $error');
      return _unreachable(address);
    } finally {
      try {
        await device.disconnect();
      } catch (_) {
        // Already gone.
      }
    }
  }

  /// V2 is checked FIRST, matching `GarminSupport.initializeDevice`: a watch
  /// that offers both must be driven over the multi-link transport, because that
  /// is what its firmware expects to carry the sync.
  GarminTransportVariant _classify(List<GarminGattService> services) {
    final characteristics = <String>{
      for (final service in services) ...service.characteristics.keys,
    };

    for (var handle = _v2FirstReceiveHandle;
        handle <= _v2LastReceiveHandle;
        handle++) {
      final receive = _garminUuid(handle);
      final send = _garminUuid(handle + _v2SendHandleOffset);
      if (characteristics.contains(receive) && characteristics.contains(send)) {
        return GarminTransportVariant.v2;
      }
    }

    if (characteristics.contains(BleUuids.garminGfdiSendV1) &&
        characteristics.contains(BleUuids.garminGfdiReceiveV1)) {
      return GarminTransportVariant.v1;
    }

    return GarminTransportVariant.unknown;
  }

  List<String> _properties(CharacteristicProperties p) => [
        if (p.read) 'read',
        if (p.write) 'write',
        if (p.writeWithoutResponse) 'writeNoRsp',
        if (p.notify) 'notify',
        if (p.indicate) 'indicate',
      ];

  GarminGattReport _unreachable(String address) => GarminGattReport(
        address: address,
        variant: GarminTransportVariant.unreachable,
        services: const [],
      );
}
