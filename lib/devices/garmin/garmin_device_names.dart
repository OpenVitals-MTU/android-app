/// Recognises a Garmin device that speaks GFDI (the protocol OpenVitals uses to
/// pull FIT files off a watch) from its advertised Bluetooth name.
///
/// Ported in spirit — not line for line — from Gadgetbridge's per-model
/// coordinators under `devices/garmin/watches/`, `.../bike/` (AGPLv3, same
/// licence as this app). Gadgetbridge needs ~100 EXACT-match patterns because
/// each one selects a coordinator class carrying that model's quirks. This app
/// needs one bit of information — "does this thing speak GFDI" — so matching by
/// product FAMILY is both sufficient and more durable: a `vívoactive 7` that
/// does not exist yet is recognised here and would need a new Gadgetbridge
/// class.
///
/// Name matching is the FALLBACK. The authoritative signal is Garmin's member
/// service UUID `0xFE1F` in the advertisement (`BleUuids.garminMemberService`) —
/// some watches advertise a shortened name, or none at all. The name check
/// still earns its place: it is what classifies a watch found through "Show all
/// devices", whose advertisement the scan filter never had to match.
library;

import '../../domain/model/ble_sensor_models.dart';

/// Product families whose devices speak GFDI and hold FIT files.
///
/// Deliberately absent: `HRM*` (HRM 200, HRMPro+, HRM600). Those are chest
/// straps that expose the standard Heart Rate GATT service, so they belong to
/// the live-recording sensor path — classifying one as a file-sync device would
/// take it out of [BleSensorCapability] assignment and break heart-rate
/// recording for anyone using one.
final List<RegExp> _garminFamilies = [
  // Watches. The accented forms are what the devices actually advertise
  // ("vívoactive", "fēnix"); the unaccented spellings appear on some firmware
  // and in some locales, so both are matched.
  RegExp(r'^v[íi]voactive\b', caseSensitive: false),
  RegExp(r'^v[íi]vomove\b', caseSensitive: false),
  RegExp(r'^v[íi]vosmart\b', caseSensitive: false),
  RegExp(r'^v[íi]vosport\b', caseSensitive: false),
  RegExp(r'^f[ēe]nix\b', caseSensitive: false),
  RegExp(r'^forerunner\b', caseSensitive: false),
  RegExp(r'^instinct\b', caseSensitive: false),
  RegExp(r'^venu\b', caseSensitive: false),
  RegExp(r'^epix\b', caseSensitive: false),
  RegExp(r'^enduro\b', caseSensitive: false),
  RegExp(r'^descent\b', caseSensitive: false),
  RegExp(r'^tactix\b', caseSensitive: false),
  RegExp(r'^quatix\b', caseSensitive: false),
  RegExp(r'^lily\b', caseSensitive: false),
  RegExp(r'^swim \d', caseSensitive: false),
  // Bike computers. Not watches, but they speak the same protocol and carry the
  // same activity FIT files, so they onboard and sync identically.
  RegExp(r'^edge\b', caseSensitive: false),
];

/// Some models advertise with a `Garmin ` prefix (Gadgetbridge carries
/// `^(Garmin )?Forerunner 265[sS]$` for exactly this), so it is stripped before
/// matching rather than doubling every pattern above.
final RegExp _garminPrefix = RegExp(r'^garmin\s+', caseSensitive: false);

/// True when [name] is a Garmin device this app can sync FIT files from.
///
/// A null or blank name is not a match: an unnamed advertisement carries no
/// evidence either way, and the caller falls back to the GFDI service UUID.
bool isGarminSyncDeviceName(String? name) {
  if (name == null) return false;
  final trimmed = name.trim().replaceFirst(_garminPrefix, '');
  if (trimmed.isEmpty) return false;
  return _garminFamilies.any((pattern) => pattern.hasMatch(trimmed));
}

/// True when [device] is a Garmin device to onboard as a [BleDeviceKind.watch]
/// (pull FIT files) rather than as a live-streaming sensor.
///
/// The advertised member service ([BleDiscoveredDevice.advertisesGarminService])
/// is the authoritative signal; [isGarminSyncDeviceName] is the fallback for a
/// watch found via "Show all devices", whose advertisement the scan filter never
/// had to match. Lives here, not on the shared [BleDiscoveredDevice], so the
/// generic discovery model carries no Garmin classification knowledge.
bool isGarminSyncDevice(BleDiscoveredDevice device) =>
    device.advertisesGarminService || isGarminSyncDeviceName(device.name);
