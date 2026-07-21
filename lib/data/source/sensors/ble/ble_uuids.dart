import '../../../../domain/model/ble_sensor_models.dart';

/// A GATT service UUID paired with the measurement (notify) characteristic UUID
/// carried within it. Port of the Kotlin `BleServiceMeasurementUuid`.
///
/// UUIDs are held as lowercase, canonical 128-bit strings (e.g.
/// `00002a37-0000-1000-8000-00805f9b34fb`) so this stays pure Dart; the
/// coordinator matches them against `flutter_blue_plus` `Guid.str128`, which is
/// also lowercase canonical.
class BleServiceMeasurementUuid {
  const BleServiceMeasurementUuid({
    required this.serviceUuid,
    required this.measurementUuid,
  });

  final String serviceUuid;
  final String measurementUuid;
}

/// Pure port of the Kotlin `BleUuids` — standard GATT service/characteristic
/// UUIDs plus the capability ↔ UUID mappings. No `flutter_blue_plus` import.
class BleUuids {
  const BleUuids._();

  static const String clientCharacteristicConfig =
      '00002902-0000-1000-8000-00805f9b34fb';

  static const String batteryService = '0000180f-0000-1000-8000-00805f9b34fb';

  static const String batteryLevel = '00002a19-0000-1000-8000-00805f9b34fb';

  static const BleServiceMeasurementUuid heartRate = BleServiceMeasurementUuid(
    serviceUuid: '0000180d-0000-1000-8000-00805f9b34fb',
    measurementUuid: '00002a37-0000-1000-8000-00805f9b34fb',
  );

  static const BleServiceMeasurementUuid heartRateMiband =
      BleServiceMeasurementUuid(
    serviceUuid: '0000fee0-0000-1000-8000-00805f9b34fb',
    measurementUuid: '00002a37-0000-1000-8000-00805f9b34fb',
  );

  static const BleServiceMeasurementUuid cyclingSpeedCadence =
      BleServiceMeasurementUuid(
    serviceUuid: '00001816-0000-1000-8000-00805f9b34fb',
    measurementUuid: '00002a5b-0000-1000-8000-00805f9b34fb',
  );

  static const BleServiceMeasurementUuid cyclingPower =
      BleServiceMeasurementUuid(
    serviceUuid: '00001818-0000-1000-8000-00805f9b34fb',
    measurementUuid: '00002a63-0000-1000-8000-00805f9b34fb',
  );

  static const BleServiceMeasurementUuid runningSpeedCadence =
      BleServiceMeasurementUuid(
    serviceUuid: '00001814-0000-1000-8000-00805f9b34fb',
    measurementUuid: '00002a53-0000-1000-8000-00805f9b34fb',
  );

  /// Garmin's Bluetooth SIG member service (16-bit `0xFE1F`) — what a Garmin
  /// watch actually puts in its ADVERTISEMENT, and therefore the only Garmin
  /// UUID a scan filter can match on.
  ///
  /// Confirmed against a vívoactive 5, whose advertisement carries exactly
  /// `mServiceUuids=[0000fe1f-…]`, service data under the same UUID, and
  /// manufacturer ID 135 (0x0087, Garmin International) — and carries no trace
  /// of [garminGfdiServiceV1].
  ///
  /// A device advertising this is a watch/bike computer to onboard as
  /// [BleDeviceKind.watch], never a source of live capabilities — which is why
  /// [capabilitiesForService] returns empty for it.
  static const String garminMemberService =
      '0000fe1f-0000-1000-8000-00805f9b34fb';

  /// Garmin's GFDI service — the transport this app pulls FIT files over.
  ///
  /// **Not advertised.** This is a GATT service, discoverable only AFTER
  /// connecting, so it must never go in [scanServiceUuids]: a filter built on
  /// it matches nothing and hides every Garmin watch from the scan. Kept for the
  /// connect path. From Gadgetbridge's
  /// `CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V1`.
  static const String garminGfdiServiceV1 =
      '6a4e2401-667b-11e3-949a-0800200c9a66';

  static const String garminGfdiSendV1 =
      '6a4e4c80-667b-11e3-949a-0800200c9a66';

  static const String garminGfdiReceiveV1 =
      '6a4ecd28-667b-11e3-949a-0800200c9a66';

  static const List<String> scanServiceUuids = [
    '0000180d-0000-1000-8000-00805f9b34fb', // heartRate
    '0000fee0-0000-1000-8000-00805f9b34fb', // heartRateMiband
    '00001816-0000-1000-8000-00805f9b34fb', // cyclingSpeedCadence
    '00001818-0000-1000-8000-00805f9b34fb', // cyclingPower
    '00001814-0000-1000-8000-00805f9b34fb', // runningSpeedCadence
    garminMemberService,
  ];

  /// Capabilities advertised by a given GATT [serviceUuid] (lowercase 128-bit).
  static Set<BleSensorCapability> capabilitiesForService(String serviceUuid) {
    if (serviceUuid == heartRate.serviceUuid ||
        serviceUuid == heartRateMiband.serviceUuid) {
      return const {BleSensorCapability.heartRate};
    }
    if (serviceUuid == cyclingSpeedCadence.serviceUuid) {
      return const {
        BleSensorCapability.cyclingCadence,
        BleSensorCapability.cyclingSpeedDistance,
      };
    }
    if (serviceUuid == cyclingPower.serviceUuid) {
      return const {
        BleSensorCapability.cyclingPower,
        BleSensorCapability.cyclingCadence,
      };
    }
    if (serviceUuid == runningSpeedCadence.serviceUuid) {
      return const {BleSensorCapability.runningSpeedCadence};
    }
    return const {};
  }

  /// Capabilities carried by a measurement [characteristicUuid] (lowercase
  /// 128-bit).
  static Set<BleSensorCapability> capabilitiesForCharacteristic(
    String characteristicUuid,
  ) {
    if (characteristicUuid == heartRate.measurementUuid) {
      return const {BleSensorCapability.heartRate};
    }
    if (characteristicUuid == cyclingSpeedCadence.measurementUuid) {
      return const {
        BleSensorCapability.cyclingCadence,
        BleSensorCapability.cyclingSpeedDistance,
      };
    }
    if (characteristicUuid == cyclingPower.measurementUuid) {
      return const {
        BleSensorCapability.cyclingPower,
        BleSensorCapability.cyclingCadence,
      };
    }
    if (characteristicUuid == runningSpeedCadence.measurementUuid) {
      return const {BleSensorCapability.runningSpeedCadence};
    }
    return const {};
  }

  /// Which service/measurement pairs can supply a given [capability].
  static List<BleServiceMeasurementUuid> measurementUuidsForCapability(
    BleSensorCapability capability,
  ) {
    switch (capability) {
      case BleSensorCapability.heartRate:
        return const [heartRate, heartRateMiband];
      case BleSensorCapability.cyclingCadence:
        return const [cyclingSpeedCadence, cyclingPower];
      case BleSensorCapability.cyclingPower:
        return const [cyclingPower];
      case BleSensorCapability.cyclingSpeedDistance:
        return const [cyclingSpeedCadence];
      case BleSensorCapability.runningSpeedCadence:
        return const [runningSpeedCadence];
    }
  }
}
