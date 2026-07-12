import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

import 'prefs_codec.dart';

/// The primitives every preference store is built from.
///
/// Each sub-store extends this; `PreferencesRepository` **composes** one for
/// the keys it still owns itself, deliberately rather than extending it — a
/// repository that inherited these would re-expose `putString`, `remove` and
/// the raw [SharedPreferences] on its own public API, letting any caller write
/// an arbitrary key straight past the facade. Encapsulating storage is the
/// point of the split, so the primitives stay behind a private field.
///
/// **The writes are deliberately fire-and-forget.** SharedPreferences updates
/// its in-memory cache synchronously and only the platform write is async, so a
/// read immediately after a write already sees the new value (this is the
/// Kotlin `apply()` semantics the app was ported from, and the whole class
/// depends on it). Do not turn any of these into `await`s.
class PrefsStore {
  const PrefsStore(this.prefs);

  final SharedPreferences prefs;

  /// Reads an int that is absent rather than defaulted — [SharedPreferences]
  /// cannot tell "no value" from "0" without the [containsKey] probe.
  int? intOrNull(String key) =>
      prefs.containsKey(key) ? prefs.getInt(key) : null;

  double? doubleOrNull(String key) =>
      prefs.containsKey(key) ? prefs.getDouble(key) : null;

  /// Reads one of the separator-joined id lists (widget/ring/section orders),
  /// returning null when the key was never written so a caller can tell "never
  /// customized" from "customized to nothing".
  List<String>? readOrderList(String key) => prefs
      .getString(key)
      ?.split(valueSeparator)
      .where((it) => it.isNotEmpty)
      .toList();

  void putOrRemoveInt(String key, int? value) {
    if (value != null) {
      putInt(key, value);
    } else {
      remove(key);
    }
  }

  void putOrRemoveDouble(String key, double? value) {
    if (value != null) {
      putDouble(key, value);
    } else {
      remove(key);
    }
  }

  void putString(String key, String value) =>
      unawaited(prefs.setString(key, value));

  void putBool(String key, bool value) => unawaited(prefs.setBool(key, value));

  void putInt(String key, int value) => unawaited(prefs.setInt(key, value));

  void putDouble(String key, double value) =>
      unawaited(prefs.setDouble(key, value));

  void putStringList(String key, List<String> value) =>
      unawaited(prefs.setStringList(key, value));

  void remove(String key) => unawaited(prefs.remove(key));
}
