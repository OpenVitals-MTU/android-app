import '../../../domain/preferences/caffeine_preferences.dart';
import '../prefs_codec.dart';
import '../prefs_store.dart';

/// Storage for the caffeine profile: the 14 keys behind
/// `PreferencesRepository.caffeinePreferences()`.
///
/// Pure storage — the `ValueNotifier` that makes the profile reactive stays in
/// the repository, so [write] takes an already-[CaffeinePreferences.normalized]
/// value and persists exactly what it is given.
class CaffeineStore extends PrefsStore {
  const CaffeineStore(super.prefs);

  CaffeinePreferences read() => CaffeinePreferences(
        profileCompleted: prefs.getBool(_keyCaffeineProfileCompleted) ?? false,
        halfLifeMinutes: prefs.getInt(_keyCaffeineHalfLifeMinutes) ??
            CaffeinePreferences.defaultHalfLifeMinutes,
        absorptionMinutes: prefs.getInt(_keyCaffeineAbsorptionMinutes) ??
            CaffeinePreferences.defaultAbsorptionMinutes,
        sleepThresholdMg: prefs.getInt(_keyCaffeineSleepThresholdMg) ??
            CaffeinePreferences.defaultSleepThresholdMg,
        bedtime: toReminderTimeOrDefault(
          prefs.getString(_keyCaffeineBedtime),
          CaffeinePreferences.defaultBedtime,
        ),
        sleepSensitivity: enumByName(
              CaffeineSleepSensitivity.values,
              prefs.getString(_keyCaffeineSleepSensitivity),
            ) ??
            CaffeineSleepSensitivity.normal,
        smoker: prefs.getBool(_keyCaffeineSmoker) ?? false,
        alcoholUse: enumByName(
              CaffeineAlcoholUse.values,
              prefs.getString(_keyCaffeineAlcoholUse),
            ) ??
            CaffeineAlcoholUse.none,
        caffeineHabituation: enumByName(
              CaffeineHabituation.values,
              prefs.getString(_keyCaffeineHabituation),
            ) ??
            CaffeineHabituation.moderate,
        liverImpairment: prefs.getBool(_keyCaffeineLiverImpairment) ?? false,
        medicationInteraction:
            prefs.getBool(_keyCaffeineMedicationInteraction) ?? false,
        cyp1a2Genotype: enumByName(
              CaffeineGenotype.values,
              prefs.getString(_keyCaffeineCyp1a2Genotype),
            ) ??
            CaffeineGenotype.unknown,
        ahrGenotype: enumByName(
              CaffeineGenotype.values,
              prefs.getString(_keyCaffeineAhrGenotype),
            ) ??
            CaffeineGenotype.unknown,
        hormonalStatus: enumByName(
              CaffeineHormonalStatus.values,
              prefs.getString(_keyCaffeineHormonalStatus),
            ) ??
            CaffeineHormonalStatus.none,
      ).normalized();

  void write(CaffeinePreferences preferences) {
    putBool(_keyCaffeineProfileCompleted, preferences.profileCompleted);
    putInt(_keyCaffeineHalfLifeMinutes, preferences.halfLifeMinutes);
    putInt(_keyCaffeineAbsorptionMinutes, preferences.absorptionMinutes);
    putInt(_keyCaffeineSleepThresholdMg, preferences.sleepThresholdMg);
    putString(_keyCaffeineBedtime, preferences.bedtime.toString());
    putString(_keyCaffeineSleepSensitivity, preferences.sleepSensitivity.name);
    putBool(_keyCaffeineSmoker, preferences.smoker);
    putString(_keyCaffeineAlcoholUse, preferences.alcoholUse.name);
    putString(_keyCaffeineHabituation, preferences.caffeineHabituation.name);
    putBool(_keyCaffeineLiverImpairment, preferences.liverImpairment);
    putBool(_keyCaffeineMedicationInteraction, preferences.medicationInteraction);
    putString(_keyCaffeineCyp1a2Genotype, preferences.cyp1a2Genotype.name);
    putString(_keyCaffeineAhrGenotype, preferences.ahrGenotype.name);
    putString(_keyCaffeineHormonalStatus, preferences.hormonalStatus.name);
  }

  // region Keys (on-disk format — never rename one).
  static const String _keyCaffeineProfileCompleted =
      'caffeine_profile_completed';
  static const String _keyCaffeineHalfLifeMinutes = 'caffeine_half_life_minutes';
  static const String _keyCaffeineAbsorptionMinutes =
      'caffeine_absorption_minutes';
  static const String _keyCaffeineSleepThresholdMg =
      'caffeine_sleep_threshold_mg';
  static const String _keyCaffeineBedtime = 'caffeine_bedtime';
  static const String _keyCaffeineSleepSensitivity =
      'caffeine_sleep_sensitivity';
  static const String _keyCaffeineSmoker = 'caffeine_smoker';
  static const String _keyCaffeineAlcoholUse = 'caffeine_alcohol_use';
  static const String _keyCaffeineHabituation = 'caffeine_habituation';
  static const String _keyCaffeineLiverImpairment = 'caffeine_liver_impairment';
  static const String _keyCaffeineMedicationInteraction =
      'caffeine_medication_interaction';
  static const String _keyCaffeineCyp1a2Genotype = 'caffeine_cyp1a2_genotype';
  static const String _keyCaffeineAhrGenotype = 'caffeine_ahr_genotype';
  static const String _keyCaffeineHormonalStatus = 'caffeine_hormonal_status';
  // endregion
}
