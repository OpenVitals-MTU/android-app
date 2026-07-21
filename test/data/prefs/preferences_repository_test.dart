import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/domain/insights/daily_goals.dart';
import 'package:openvitals/domain/model/hydration_reminder_config.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/model/mindfulness_reminder_config.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/preferences/app_theme_mode.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<PreferencesRepository> newRepo([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  final prefs = await SharedPreferences.getInstance();
  return PreferencesRepository(prefs);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('scalar keys', () {
    test('onboardingDone defaults false and round-trips', () async {
      final repo = await newRepo();
      expect(repo.onboardingDone, isFalse);
      repo.onboardingDone = true;
      expect(repo.onboardingDone, isTrue);
    });

    test('healthConnectSyncEnabled defaults true', () async {
      final repo = await newRepo();
      expect(repo.healthConnectSyncEnabled, isTrue);
      repo.healthConnectSyncEnabled = false;
      expect(repo.healthConnectSyncEnabled, isFalse);
    });

    test('permission cancel count coerces to at least zero', () async {
      final repo = await newRepo();
      expect(repo.healthConnectPermissionCancelCount, 0);
      repo.healthConnectPermissionCancelCount = -5;
      expect(repo.healthConnectPermissionCancelCount, 0);
      repo.healthConnectPermissionCancelCount = 3;
      expect(repo.healthConnectPermissionCancelCount, 3);
    });

    test('accepted privacy version can be cleared', () async {
      final repo = await newRepo();
      expect(repo.acceptedPrivacyPolicyVersion, isNull);
      repo.acceptedPrivacyPolicyVersion = '1.0';
      expect(repo.acceptedPrivacyPolicyVersion, '1.0');
      repo.acceptedPrivacyPolicyVersion = null;
      expect(repo.acceptedPrivacyPolicyVersion, isNull);
    });

    test('nullable exercise types round-trip and clear', () async {
      final repo = await newRepo();
      expect(repo.lastActivityExerciseType, isNull);
      repo.lastActivityExerciseType = 79;
      expect(repo.lastActivityExerciseType, 79);
      repo.lastActivityExerciseType = null;
      expect(repo.lastActivityExerciseType, isNull);
      repo.favoriteActivityExerciseType = 12;
      expect(repo.favoriteActivityExerciseType, 12);
    });

    test('hydration daily goal defaults 2.0 and clamps', () async {
      final repo = await newRepo();
      expect(repo.hydrationDailyGoalLiters, closeTo(2.0, 1e-9));
      repo.hydrationDailyGoalLiters = 99.0;
      expect(repo.hydrationDailyGoalLiters, closeTo(10.0, 1e-9));
      repo.hydrationDailyGoalLiters = 0.0;
      expect(repo.hydrationDailyGoalLiters, closeTo(0.25, 1e-9));
    });

    test('heart-rate thresholds default and clamp', () async {
      final repo = await newRepo();
      expect(repo.highHeartRateThresholdBpm,
          PreferencesRepository.defaultHighHeartRateThresholdBpm);
      expect(repo.lowHeartRateThresholdBpm,
          PreferencesRepository.defaultLowHeartRateThresholdBpm);
      repo.highHeartRateThresholdBpm = 500;
      expect(repo.highHeartRateThresholdBpm,
          PreferencesRepository.maxHighHeartRateThresholdBpm);
      repo.lowHeartRateThresholdBpm = 1;
      expect(repo.lowHeartRateThresholdBpm,
          PreferencesRepository.minLowHeartRateThresholdBpm);
    });
  });

  group('the unit-system default is a function of the locale, not the host', () {
    // Before the locale seam was injected, this default came straight off
    // `Platform.localeName`, so it was whatever the machine running the test
    // happened to be set to -- which is why the listenable test below still has
    // to toggle "to whichever value differs from the default" rather than just
    // naming one. These pin it.
    Future<PreferencesRepository> repoIn(
      String localeName, [
      Map<String, Object> initial = const {},
    ]) async {
      SharedPreferences.setMockInitialValues(initial);
      final prefs = await SharedPreferences.getInstance();
      return PreferencesRepository(prefs, localeName: localeName);
    }

    test('a US device starts out imperial', () async {
      expect((await repoIn('en_US')).unitSystem, UnitSystem.imperial);
      // The country may arrive with a charset/modifier suffix attached.
      expect((await repoIn('en_US.UTF-8')).unitSystem, UnitSystem.imperial);
    });

    test('the rest of the world starts out metric', () async {
      for (final locale in ['en_GB', 'de_DE', 'fr_FR', 'ja_JP']) {
        expect(
          (await repoIn(locale)).unitSystem,
          UnitSystem.metric,
          reason: locale,
        );
      }
    });

    test('a locale with no country is metric, not a crash', () async {
      expect((await repoIn('en')).unitSystem, UnitSystem.metric);
      expect((await repoIn('')).unitSystem, UnitSystem.metric);
    });

    test('a stored choice wins over the locale', () async {
      // The locale only ever seeds a user who has never picked. Someone in the
      // US who chose metric must stay metric.
      final repo = await repoIn('en_US', {'unit_system': 'metric'});
      expect(repo.unitSystem, UnitSystem.metric);
    });
  });

  group('enum-backed reactive values', () {
    test('unitSystem set/read and notifies the listenable', () async {
      final repo = await newRepo();
      // Toggle to whichever value differs from the host-derived default so the
      // notifier is guaranteed to fire.
      final target = repo.unitSystem == UnitSystem.metric
          ? UnitSystem.imperial
          : UnitSystem.metric;
      final observed = <UnitSystem>[];
      repo.unitSystemListenable.addListener(
        () => observed.add(repo.unitSystem),
      );
      repo.unitSystem = target;
      expect(repo.unitSystem, target);
      expect(observed, [target]);
    });

    test('appThemeMode / sleep window round-trip via a fresh instance',
        () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final repo = PreferencesRepository(prefs);
      repo.appThemeMode = AppThemeMode.amoled;
      repo.nightStartHour = 20;
      repo.nightEndHour = 9;

      final reloaded = PreferencesRepository(prefs);
      expect(reloaded.appThemeMode, AppThemeMode.amoled);
      expect(reloaded.sleepWindow.startHour, 20);
      expect(reloaded.sleepWindow.endHour, 9);
    });

    test('sleep window defaults to 18:00-10:00 and clamps out-of-range hours',
        () async {
      SharedPreferences.setMockInitialValues({});
      final repo = PreferencesRepository(await SharedPreferences.getInstance());
      expect(repo.sleepWindow.startHour, 18);
      expect(repo.sleepWindow.endHour, 10);
      repo.nightStartHour = 30;
      expect(repo.nightStartHour, 23);
    });
  });

  group('time ranges and daily goals', () {
    test('timeRangeFor default then override', () async {
      final repo = await newRepo();
      expect(repo.timeRangeFor(PeriodRangePreferenceKey.body), TimeRange.month);
      repo.setTimeRangeFor(PeriodRangePreferenceKey.body, TimeRange.year);
      expect(repo.timeRangeFor(PeriodRangePreferenceKey.body), TimeRange.year);
    });

    test('dailyGoalFor default then normalized override', () async {
      final repo = await newRepo();
      expect(repo.dailyGoalFor(MetricDailyGoalKey.steps),
          closeTo(MetricDailyGoalKey.steps.defaultValue, 1e-6));
      repo.setDailyGoalFor(MetricDailyGoalKey.steps, 10000000.0);
      expect(repo.dailyGoalFor(MetricDailyGoalKey.steps),
          closeTo(MetricDailyGoalKey.steps.maxValue, 1e-6));
    });
  });

  group('structured configs', () {
    test('bodyProfile round-trips and normalizes', () async {
      final repo = await newRepo();
      repo.setBodyProfile(
        const BodyProfile(
          birthYear: 1990,
          weightKg: 72.5,
          restingHeartRateBpm: 55,
          maxHeartRateBpm: 190,
        ),
      );
      final profile = repo.bodyProfile();
      expect(profile.birthYear, 1990);
      expect(profile.weightKg, closeTo(72.5, 1e-6));
      expect(profile.restingHeartRateBpm, 55);
      expect(profile.maxHeartRateBpm, 190);

      final reloaded = PreferencesRepository(
        await SharedPreferences.getInstance(),
      );
      expect(reloaded.bodyProfile().signature(), profile.signature());
    });

    test('bodyEnergyCalibration round-trips manual zones', () async {
      final repo = await newRepo();
      const zones = HeartZoneThresholds(
        zone1LowerBpm: 90,
        zone2LowerBpm: 110,
        zone3LowerBpm: 130,
        zone4LowerBpm: 150,
        zone5LowerBpm: 170,
      );
      repo.setBodyEnergyCalibration(
        const BodyEnergyCalibration(
          manualZoneThresholdsBpm: zones,
          useManualZones: true,
          setupCompleted: true,
        ),
      );
      final reloaded = PreferencesRepository(
        await SharedPreferences.getInstance(),
      );
      final calibration = reloaded.bodyEnergyCalibration();
      expect(calibration.useManualZones, isTrue);
      expect(calibration.setupCompleted, isTrue);
      expect(
        calibration.manualZoneThresholdsBpm?.toPreferenceString(),
        zones.toPreferenceString(),
      );
    });

    test('caffeinePreferences round-trips every field', () async {
      final repo = await newRepo();
      const prefs = CaffeinePreferences(
        profileCompleted: true,
        halfLifeMinutes: 400,
        absorptionMinutes: 50,
        sleepThresholdMg: 40,
        bedtime: LocalTime(23, 15),
        sleepSensitivity: CaffeineSleepSensitivity.high,
        smoker: true,
        alcoholUse: CaffeineAlcoholUse.regular,
        caffeineHabituation: CaffeineHabituation.high,
        liverImpairment: true,
        medicationInteraction: true,
        cyp1a2Genotype: CaffeineGenotype.slow,
        ahrGenotype: CaffeineGenotype.fast,
        hormonalStatus: CaffeineHormonalStatus.pregnant,
      );
      repo.setCaffeinePreferences(prefs);

      final reloaded = PreferencesRepository(
        await SharedPreferences.getInstance(),
      ).caffeinePreferences();
      expect(reloaded.profileCompleted, isTrue);
      expect(reloaded.halfLifeMinutes, 400);
      expect(reloaded.absorptionMinutes, 50);
      expect(reloaded.sleepThresholdMg, 40);
      expect(reloaded.bedtime, const LocalTime(23, 15));
      expect(reloaded.sleepSensitivity, CaffeineSleepSensitivity.high);
      expect(reloaded.smoker, isTrue);
      expect(reloaded.alcoholUse, CaffeineAlcoholUse.regular);
      expect(reloaded.caffeineHabituation, CaffeineHabituation.high);
      expect(reloaded.liverImpairment, isTrue);
      expect(reloaded.medicationInteraction, isTrue);
      expect(reloaded.cyp1a2Genotype, CaffeineGenotype.slow);
      expect(reloaded.ahrGenotype, CaffeineGenotype.fast);
      expect(reloaded.hormonalStatus, CaffeineHormonalStatus.pregnant);
    });

    test('hydration reminder config round-trips and normalizes interval',
        () async {
      final repo = await newRepo();
      repo.setHydrationReminderConfig(
        const HydrationReminderConfig(
          enabled: true,
          intervalMinutes: 95, // normalized to a 30-minute step
          activeStartTime: LocalTime(8, 0),
          activeEndTime: LocalTime(22, 0),
        ),
      );
      final config = repo.hydrationReminderConfig();
      expect(config.enabled, isTrue);
      expect(config.intervalMinutes, 90);
      expect(config.activeStartTime, const LocalTime(8, 0));
      expect(config.activeEndTime, const LocalTime(22, 0));
    });

    test('mindfulness reminder + timer config round-trip', () async {
      final repo = await newRepo();
      repo.setMindfulnessReminderConfig(
        const MindfulnessReminderConfig(
          enabled: true,
          reminderTime: LocalTime(7, 30),
        ),
      );
      expect(repo.mindfulnessReminderConfig().enabled, isTrue);
      expect(repo.mindfulnessReminderConfig().reminderTime,
          const LocalTime(7, 30));

      repo.setMindfulnessTimerConfig(
        const MindfulnessTimerConfig(
          durationMinutes: 20,
          intervalMinutes: 5,
          bellSound: MindfulnessBellSound.temple,
          backgroundSound: MindfulnessBackgroundSound.chimes,
        ),
      );
      final timer = repo.mindfulnessTimerConfig();
      expect(timer.durationMinutes, 20);
      expect(timer.intervalMinutes, 5);
      expect(timer.bellSound, MindfulnessBellSound.temple);
      expect(timer.backgroundSound, MindfulnessBackgroundSound.chimes);
    });

    test('legacy mindfulness bell sound values map forward', () async {
      final repo = await newRepo({
        'mindfulness_timer_bell_sound': 'SOFT',
      });
      expect(repo.mindfulnessTimerConfig().bellSound, MindfulnessBellSound.struck);
    });
  });

  group('hydration containers and custom drinks', () {
    test('container volumes accumulate and reject invalid input', () async {
      final repo = await newRepo();
      repo.setHydrationContainerVolumeMilliliters('mug', 250.0);
      repo.setHydrationContainerVolumeMilliliters('bottle', 750.0);
      repo.setHydrationContainerVolumeMilliliters('bad', -1.0);
      final volumes = repo.hydrationContainerVolumeMilliliters();
      expect(volumes['mug'], closeTo(250.0, 1e-6));
      expect(volumes['bottle'], closeTo(750.0, 1e-6));
      expect(volumes.containsKey('bad'), isFalse);
    });

    test('last custom hydration amount round-trips', () async {
      final repo = await newRepo();
      expect(repo.lastCustomHydrationAmountMilliliters(), isNull);
      repo.setLastCustomHydrationAmountMilliliters(333.0);
      expect(repo.lastCustomHydrationAmountMilliliters(), closeTo(333.0, 1e-6));
    });

    test('custom drinks save, reorder, and delete preserving order', () async {
      final repo = await newRepo();
      const a = CustomHydrationDrink(
        id: 'a',
        name: 'Alpha',
        volumeMilliliters: 200,
        nutrientValues: {NutritionNutrient.caffeine: 80.0},
      );
      const b = CustomHydrationDrink(
        id: 'b',
        name: 'Beta',
        volumeMilliliters: 300,
      );
      repo.saveCustomHydrationDrink(a);
      repo.saveCustomHydrationDrink(b);
      expect(repo.customHydrationDrinks().map((d) => d.id), ['a', 'b']);

      repo.reorderCustomHydrationDrinks(['b', 'a']);
      expect(repo.customHydrationDrinks().map((d) => d.id), ['b', 'a']);

      // Nutrient values survive the string round-trip.
      final reloadedA =
          repo.customHydrationDrinks().firstWhere((d) => d.id == 'a');
      expect(reloadedA.nutrientValues[NutritionNutrient.caffeine],
          closeTo(80.0, 1e-6));

      repo.deleteCustomHydrationDrink('b');
      expect(repo.customHydrationDrinks().map((d) => d.id), ['a']);
    });

    test('custom drinks with special characters survive encoding', () async {
      final repo = await newRepo();
      const drink = CustomHydrationDrink(
        id: 'weird|=;,id',
        name: 'Name = with; separators, and | pipes',
        volumeMilliliters: 250,
      );
      repo.saveCustomHydrationDrink(drink);
      final reloaded = repo.customHydrationDrinks().single;
      expect(reloaded.id, 'weird|=;,id');
      expect(reloaded.name, 'Name = with; separators, and | pipes');
    });
  });

  group('ordered widget lists and acknowledged permissions', () {
    test('dashboard/manual/section order round-trip', () async {
      final repo = await newRepo();
      expect(repo.dashboardWidgetOrder(), isNull);
      repo.setDashboardWidgetOrder(['steps', 'sleep']);
      expect(repo.dashboardWidgetOrder(), ['steps', 'sleep']);
      repo.setManualEntryWidgetOrder(['weight']);
      expect(repo.manualEntryWidgetOrder(), ['weight']);
      repo.setMetricDetailSectionOrder(['PERIOD_CHART', 'STATISTICS']);
      expect(repo.metricDetailSectionOrder(), ['PERIOD_CHART', 'STATISTICS']);
    });

    test('acknowledged permissions union', () async {
      final repo = await newRepo();
      expect(repo.acknowledgedPermissions(), isEmpty);
      repo.acknowledgePermissions({'READ_STEPS'});
      repo.acknowledgePermissions({'READ_STEPS', 'READ_SLEEP'});
      expect(repo.acknowledgedPermissions(),
          {'READ_STEPS', 'READ_SLEEP'});

      repo.acknowledgePermissionsForFeature('STEPS', {'READ_STEPS'});
      expect(repo.acknowledgedPermissionsFor('STEPS'), {'READ_STEPS'});
    });
  });

  test('legacy body-profile values migrate on first read', () async {
    final repo = await newRepo({
      'caffeine_age_years': 30,
      'caffeine_weight_kg': 68.0,
      'body_energy_resting_hr_bpm': 52,
    });
    final profile = repo.bodyProfile();
    expect(profile.birthYear, LocalDate.now().year - 30);
    expect(profile.weightKg, closeTo(68.0, 1e-6));
    expect(profile.restingHeartRateBpm, 52);
  });
}
