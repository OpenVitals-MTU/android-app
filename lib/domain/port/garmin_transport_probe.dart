import '../model/garmin_transport.dart';

/// Asks a bonded Garmin device which GFDI transport it speaks.
///
/// A **port**, like [BleCapabilityProbe] and for the same reason: the domain
/// owns the question, the data layer owns the radio.
///
/// Must run AFTER bonding. Garmin's GFDI characteristics sit behind an encrypted
/// link, so enumerating an unbonded device either omits them or fails outright —
/// which would look identical to [GarminTransportVariant.unknown] and send the
/// reader hunting for a protocol bug that isn't there.
abstract interface class GarminTransportProbe {
  /// Connects to [address], enumerates its GATT services, classifies the
  /// transport and disconnects.
  ///
  /// Never throws: an unreachable device comes back as
  /// [GarminTransportVariant.unreachable] with no services.
  Future<GarminGattReport> probe(String address);
}
