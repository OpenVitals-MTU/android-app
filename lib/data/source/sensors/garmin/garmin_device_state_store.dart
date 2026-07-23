import 'package:shared_preferences/shared_preferences.dart';

import 'garmin_capabilities.dart';

/// Garmin's own per-device state, kept out of the generic [BleDeviceRepository]
/// so that registry carries no Garmin knowledge: the GFDI capability bitmap a
/// watch declared in its last handshake, and which of its files a previous sync
/// already pulled.
///
/// SharedPreferences-backed and keyed by the registry's `deviceId`. The key
/// strings are the ones the registry used before this state was split out, so
/// existing users' watch state survives the move untouched. Fire-and-forget
/// like the registry (the persist behind each mutator is not awaited), so there
/// is nothing to `Result`-type.
class GarminDeviceStateStore {
  GarminDeviceStateStore(this._prefs);

  final SharedPreferences _prefs;

  /// Cap on remembered file keys per watch. A few years of daily monitor, sleep
  /// and HRV files plus activities lands well inside this; the cap only exists
  /// so the list cannot grow without bound in SharedPreferences.
  static const int _maxSyncedFileKeys = 4000;

  String _syncedKeysPrefsKey(String deviceId) => 'ble_synced_files_$deviceId';

  String _capabilitiesPrefsKey(String deviceId) =>
      'garmin_capabilities_$deviceId';

  /// What the watch declared it can do, from the last handshake. Empty when a
  /// watch has never synced.
  Set<GarminCapability> capabilities(String deviceId) {
    final raw = _prefs.getStringList(_capabilitiesPrefsKey(deviceId));
    if (raw == null) return const {};
    // Matched by WIRE NAME, not index: the enum's order is the bitmap's order,
    // so storing indexes would rot the moment a flag is named.
    final byName = {for (final c in GarminCapability.values) c.wireName: c};
    return {for (final name in raw) if (byName[name] != null) byName[name]!};
  }

  void recordCapabilities(String deviceId, Set<GarminCapability> capabilities) {
    if (capabilities.isEmpty) return;
    _prefs.setStringList(
      _capabilitiesPrefsKey(deviceId),
      [for (final c in capabilities) c.wireName],
    );
  }

  /// Which of a watch's files a previous sync already pulled, keyed by
  /// `GarminDirectoryEntry.dedupKey`.
  Set<String> syncedFileKeys(String deviceId) {
    final raw = _prefs.getStringList(_syncedKeysPrefsKey(deviceId));
    return raw == null ? <String>{} : raw.toSet();
  }

  void recordSyncedFileKeys(String deviceId, Iterable<String> keys) {
    if (keys.isEmpty) return;
    final key = _syncedKeysPrefsKey(deviceId);
    // Order matters for the cap: a List keeps insertion order, so trimming from
    // the front drops the OLDEST keys, which are the least likely to be
    // re-offered by the watch.
    final existing = _prefs.getStringList(key) ?? const <String>[];
    final merged = <String>[
      ...existing,
      ...keys.where((k) => !existing.contains(k)),
    ];
    final trimmed = merged.length > _maxSyncedFileKeys
        ? merged.sublist(merged.length - _maxSyncedFileKeys)
        : merged;
    _prefs.setStringList(key, trimmed);
  }

  void clearSyncedFileKeys(String deviceId) =>
      _prefs.remove(_syncedKeysPrefsKey(deviceId));

  /// Drops everything this store holds for [deviceId] — capabilities and synced
  /// file keys. Called when a watch is forgotten, so a re-pairing starts clean
  /// (re-learning capabilities from a fresh handshake and re-fetching files
  /// rather than trusting a record of a device that is no longer here).
  void clear(String deviceId) {
    clearSyncedFileKeys(deviceId);
    _prefs.remove(_capabilitiesPrefsKey(deviceId));
  }
}
