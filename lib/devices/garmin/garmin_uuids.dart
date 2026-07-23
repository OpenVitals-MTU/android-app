/// Garmin GFDI transport UUIDs — the GATT service/characteristic identifiers the
/// FIT-file sync connects over. Split out of the shared `BleUuids` so the generic
/// BLE stack carries no Garmin protocol knowledge: these are discoverable only
/// AFTER connecting (never advertised), so nothing outside the Garmin transport
/// has any use for them.
///
/// The one Garmin UUID that stays in `BleUuids` is `garminMemberService`
/// (`0xFE1F`): it is what a watch puts in its ADVERTISEMENT, so the shared
/// scanner needs it to spot a watch in the first place.
class GarminUuids {
  const GarminUuids._();

  /// Garmin's GFDI V1 service — the transport older watches pull FIT files over.
  ///
  /// **Not advertised.** A GATT service discoverable only after connecting, so it
  /// must never go in a scan filter. From Gadgetbridge's
  /// `CommunicatorV1.UUID_SERVICE_GARMIN_GFDI_V1`.
  static const String gfdiServiceV1 = '6a4e2401-667b-11e3-949a-0800200c9a66';

  static const String gfdiSendV1 = '6a4e4c80-667b-11e3-949a-0800200c9a66';

  static const String gfdiReceiveV1 = '6a4ecd28-667b-11e3-949a-0800200c9a66';

  /// Garmin's V2 multi-link service — what a vívoactive 5 exposes (confirmed by
  /// the on-device GATT probe). Also GATT-only, never advertised.
  static const String mlServiceV2 = '6a4e2800-667b-11e3-949a-0800200c9a66';

  /// V2 receive (notify) characteristic handles. Each is paired with a send
  /// characteristic at `handle + [mlSendHandleOffset]`, and the first pair that
  /// exists on the device is the one to use (`CommunicatorV2.initializeDevice`).
  static const int mlFirstReceiveHandle = 0x2810;
  static const int mlLastReceiveHandle = 0x2814;
  static const int mlSendHandleOffset = 0x10;

  /// Builds a Garmin 128-bit UUID from its 16-bit handle, splicing it into
  /// Gadgetbridge's `BASE_UUID` (`6A4E%04X-667B-11E3-949A-0800200C9A66`).
  /// Lowercase, to match `flutter_blue_plus` `Guid.str128`.
  static String uuidForHandle(int handle) =>
      '6a4e${handle.toRadixString(16).padLeft(4, '0')}'
      '-667b-11e3-949a-0800200c9a66';
}
