/// Recognises which Garmin device a discovered advertisement is, from its
/// advertised Bluetooth NAME, so the classifier can decide: a watch family
/// (vívoactive, fēnix, …) → a GFDI file-sync watch; an Edge → a bike computer;
/// anything else → a plain live BLE sensor.
///
/// Ported in spirit — not line for line — from Gadgetbridge's per-model
/// coordinators under `devices/garmin/watches/`, `.../bike/` (AGPLv3, same
/// licence as this app). Gadgetbridge needs ~100 EXACT-match patterns because
/// each one selects a coordinator class carrying that model's quirks. This app
/// needs only the product FAMILY — durable against a `vívoactive 7` that does
/// not exist yet, which is recognised here and would need a new Gadgetbridge
/// class.
///
/// The NAME is authoritative for the kind. Garmin's member service UUID `0xFE1F`
/// (`BleUuids.garminMemberService`) is used only to SURFACE a device in the scan
/// (`GarminScanClassifier` / `BleDiscoveredDevice.advertisesSyncService`), never
/// to decide watch-vs-sensor: a device advertising `0xFE1F` but not matching a
/// known Garmin family is treated as a plain sensor, not swept up as a watch.
/// Deliberately absent from every family list: `HRM*` chest straps — they expose
/// the standard Heart Rate GATT service and belong to the live-sensor path.
library;

/// Watch product families. The accented forms are what the devices actually
/// advertise ("vívoactive", "fēnix"); the unaccented spellings appear on some
/// firmware and in some locales, so both are matched.
final List<RegExp> _garminWatchFamilies = [
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
];

/// Bike-computer product families. Not watches: they speak the same GFDI
/// protocol and carry the same activity FIT files (so they onboard and sync
/// identically), but they classify as [BleDeviceKind.bikeComputer] so the UI can
/// present them as cycling devices and offer them the live-sensor role. The
/// single `^edge\b` pattern already covers "Edge Explore", "Edge MTB",
/// "Edge 1040" — the `\b` sits right after "edge".
final List<RegExp> _garminBikeComputerFamilies = [
  RegExp(r'^edge\b', caseSensitive: false),
];

/// Some models advertise with a `Garmin ` prefix (Gadgetbridge carries
/// `^(Garmin )?Forerunner 265[sS]$` for exactly this), so it is stripped before
/// matching rather than doubling every pattern above.
final RegExp _garminPrefix = RegExp(r'^garmin\s+', caseSensitive: false);

/// True when [name] is a Garmin smartwatch — onboard as a [BleDeviceKind.watch].
/// Disjoint from the bike-computer families, so a device is never both.
///
/// A null or blank name is not a match: an unnamed advertisement carries no
/// evidence, so it is left to fall through to a plain sensor.
bool isGarminWatchName(String? name) {
  final trimmed = _strippedGarminName(name);
  if (trimmed == null) return false;
  return _garminWatchFamilies.any((pattern) => pattern.hasMatch(trimmed));
}

/// True when [name] is a Garmin Edge bike computer — onboard as a
/// [BleDeviceKind.bikeComputer]. Disjoint from the watch families.
bool isGarminBikeComputerName(String? name) {
  final trimmed = _strippedGarminName(name);
  if (trimmed == null) return false;
  return _garminBikeComputerFamilies.any((pattern) => pattern.hasMatch(trimmed));
}

/// True when [name] is any Garmin GFDI file-sync device — a watch OR a bike
/// computer. The union of the two family checks.
bool isGarminSyncDeviceName(String? name) =>
    isGarminWatchName(name) || isGarminBikeComputerName(name);

/// The device name with a leading `Garmin ` prefix stripped, or null when it is
/// null/blank (an unnamed advertisement carries no evidence either way).
String? _strippedGarminName(String? name) {
  if (name == null) return null;
  final trimmed = name.trim().replaceFirst(_garminPrefix, '');
  return trimmed.isEmpty ? null : trimmed;
}
