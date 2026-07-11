import 'dart:async';

import 'package:shared_preferences/shared_preferences.dart';

import '../../core/time/local_date.dart';

/// One-shot read migrations over the raw preference store.
///
/// These run once, eagerly, from the `PreferencesRepository` constructor.

/// The body profile keys the migration below writes *into* and that the
/// repository then reads and writes as normal preferences. They live here so
/// the migration and the repository share one definition of each on-disk key
/// rather than two copies that can drift apart.
const String keyBodyProfileBirthYear = 'body_profile_birth_year';
const String keyBodyProfileWeightKg = 'body_profile_weight_kg';
const String keyBodyProfileRestingHrBpm = 'body_profile_resting_hr_bpm';
const String keyBodyProfileMaxHrBpm = 'body_profile_max_hr_bpm';

// The keys an older build wrote the same facts under. Read-only, and read only
// from here.
const String _keyBodyEnergyBirthYear = 'body_energy_birth_year';
const String _keyBodyEnergyMaxHrBpm = 'body_energy_max_hr_bpm';
const String _keyBodyEnergyRestingHrBpm = 'body_energy_resting_hr_bpm';
const String _keyCaffeineAgeYears = 'caffeine_age_years';
const String _keyCaffeineWeightKg = 'caffeine_weight_kg';

/// Folds the age/weight/heart-rate values that used to live under the body
/// energy and caffeine feature keys into the single body profile.
///
/// A no-op once any body profile key exists — that is what marks the migration
/// as already done; there is no separate flag.
///
/// The stored legacy value was an *age*, so it is turned into a birth year
/// against the current year. That makes this clock-dependent, deliberately:
/// migrating in 2026 and in 2027 produce different birth years for the same
/// stored age, and there is nothing better to do with an age.
///
/// The writes are fire-and-forget (`unawaited`) and the caller reads the keys
/// back **synchronously** straight afterwards. That is safe only because
/// SharedPreferences updates its in-memory cache before the platform write
/// completes. Do not turn these into `await`s, and do not reorder them.
void migrateLegacyBodyProfileValues(SharedPreferences prefs) {
  final hasNewProfileData = prefs.containsKey(keyBodyProfileBirthYear) ||
      prefs.containsKey(keyBodyProfileWeightKg) ||
      prefs.containsKey(keyBodyProfileRestingHrBpm) ||
      prefs.containsKey(keyBodyProfileMaxHrBpm);
  if (hasNewProfileData) return;

  final legacyBirthYear = _intOrNull(prefs, _keyBodyEnergyBirthYear);
  final legacyAgeYears = _intOrNull(prefs, _keyCaffeineAgeYears);
  final legacyWeightKg = _doubleOrNull(prefs, _keyCaffeineWeightKg);
  final legacyRestingHr = _intOrNull(prefs, _keyBodyEnergyRestingHrBpm);
  final legacyMaxHr = _intOrNull(prefs, _keyBodyEnergyMaxHrBpm);
  final migratedBirthYear = legacyBirthYear ??
      (legacyAgeYears != null ? LocalDate.now().year - legacyAgeYears : null);
  if (migratedBirthYear == null &&
      legacyWeightKg == null &&
      legacyRestingHr == null &&
      legacyMaxHr == null) {
    return;
  }
  if (migratedBirthYear != null) {
    unawaited(prefs.setInt(keyBodyProfileBirthYear, migratedBirthYear));
  }
  if (legacyWeightKg != null) {
    unawaited(prefs.setDouble(keyBodyProfileWeightKg, legacyWeightKg));
  }
  if (legacyRestingHr != null) {
    unawaited(prefs.setInt(keyBodyProfileRestingHrBpm, legacyRestingHr));
  }
  if (legacyMaxHr != null) {
    unawaited(prefs.setInt(keyBodyProfileMaxHrBpm, legacyMaxHr));
  }
}

int? _intOrNull(SharedPreferences prefs, String key) =>
    prefs.containsKey(key) ? prefs.getInt(key) : null;

double? _doubleOrNull(SharedPreferences prefs, String key) =>
    prefs.containsKey(key) ? prefs.getDouble(key) : null;
