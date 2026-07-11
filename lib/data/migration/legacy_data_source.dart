import 'package:flutter/services.dart';

/// Read-only access to the *Kotlin* app's on-device data.
///
/// The Flutter app replaces the Kotlin one in place (same `applicationId`, same
/// signing certificate), so a Play update keeps
/// `/data/data/tech.mmarca.openvitals/` intact — but Flutter reads different
/// files, so without a migration the user's goals, drinks, sensors, reminders
/// and offline maps all silently reset.
///
/// Everything here is a *read*. Nothing in this app writes the legacy store, and
/// nothing native writes the Flutter one: `shared_preferences_android` encodes a
/// Dart `double` and a `List<String>` as prefixed strings — a plugin-internal
/// encoding that has already changed between plugin versions — so the native
/// side hands over plain typed values and Dart writes them back through the
/// [SharedPreferences] API, which owns its own encoding.
///
/// Implemented natively only on Android. On iOS (and in tests) the channel is
/// absent; [MethodChannelLegacyDataSource] then reports "no legacy data" rather
/// than throwing, because there is no Kotlin app to migrate from.
abstract interface class LegacyDataSource {
  /// Whether the Kotlin app ever ran on this device (its main preferences file
  /// exists). The migration's trigger.
  Future<bool> hasLegacyData();

  /// Every entry of the legacy shared-preferences file [name], typed as Dart
  /// values: Kotlin `Float` arrives as `double`, `Int`/`Long` as `int`, and
  /// `Set<String>` as `List<String>`.
  ///
  /// An absent file yields an empty map.
  Future<Map<String, Object?>> readLegacyPrefs(String name);

  /// Absolute path of the Kotlin app's Room database (`databases/openvitals.db`).
  Future<String?> legacyDatabasePath();

  /// Absolute path of the Kotlin app's `files/` directory.
  Future<String?> legacyFilesDir();
}

/// [LegacyDataSource] backed by the `MainActivity` method channel.
///
/// Every call is guarded: a [MissingPluginException] (iOS, unit tests, or a
/// background isolate that never registered the channel) and any
/// [PlatformException] degrade to "nothing to migrate" instead of propagating.
/// The migration must never be able to brick app startup.
class MethodChannelLegacyDataSource implements LegacyDataSource {
  const MethodChannelLegacyDataSource();

  static const MethodChannel _channel =
      MethodChannel('tech.mmarca.openvitals/legacy_migration');

  @override
  Future<bool> hasLegacyData() async =>
      await _invoke<bool>('hasLegacyData') ?? false;

  @override
  Future<Map<String, Object?>> readLegacyPrefs(String name) async {
    final result = await _invoke<Map<Object?, Object?>>(
      'readLegacyPrefs',
      <String, Object?>{'name': name},
    );
    if (result == null) return const <String, Object?>{};
    return result.map(
      (key, value) => MapEntry('$key', _normalize(value)),
    );
  }

  @override
  Future<String?> legacyDatabasePath() => _invoke<String>('legacyDatabasePath');

  @override
  Future<String?> legacyFilesDir() => _invoke<String>('legacyFilesDir');

  /// The `StandardMessageCodec` decodes a list as `List<Object?>`; the string
  /// lists this carries (Kotlin `Set<String>` values) are re-typed here so the
  /// migration can hand them straight to `setStringList`.
  static Object? _normalize(Object? value) {
    if (value is List) return value.whereType<String>().toList(growable: false);
    return value;
  }

  static Future<T?> _invoke<T>(String method, [Object? arguments]) async {
    try {
      return await _channel.invokeMethod<T>(method, arguments);
    } on MissingPluginException {
      // No native side: iOS, tests, or an isolate without the channel.
      return null;
    } on PlatformException {
      return null;
    }
  }
}
