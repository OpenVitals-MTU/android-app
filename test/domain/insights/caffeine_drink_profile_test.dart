import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/insights/caffeine_drink_profile.dart';
import 'package:openvitals/domain/insights/caffeine_insight_calculator.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';

/// One drink, worked out on its own.
///
/// The arithmetic is the calculator's, not this file's — the whole-day curve is the SUM of
/// exactly these per-drink contributions, so a drink's bump in the day's line and its own
/// chart are the same number by construction. What is tested here is the reading of that
/// curve: where the peak is, and when the drink is half gone and gone.

final _drunkAt = DateTime(2026, 7, 14, 8, 0);

CaffeineEntry _coffee({double mg = 95, Duration over = Duration.zero}) =>
    CaffeineEntry(
      id: 'c1',
      startTime: _drunkAt,
      endTime: _drunkAt.add(over),
      caffeineMg: mg,
      name: 'Coffee',
      source: 'test',
      mealType: 0,
    );

CaffeineDrinkProfile _profile(
  CaffeineEntry entry, {
  DateTime? now,
}) =>
    caffeineDrinkProfile(
      entry: entry,
      now: now ?? _drunkAt,
      preferences: const CaffeinePreferences(),
    );

void main() {
  test('the peak is LESS than the dose, and comes after the drink', () {
    final profile = _profile(_coffee(mg: 95));

    // A 95mg coffee never puts 95mg in you at once: absorption takes time, and
    // elimination has already started before absorption has finished. A screen that
    // showed the dose as the peak would be overstating every drink in the app.
    expect(profile.peakMg, lessThan(95));
    expect(profile.peakMg, greaterThan(0));
    expect(profile.peakTime.isBefore(_drunkAt), isFalse);
  });

  test('it fades: half gone, then gone', () {
    final profile = _profile(_coffee());

    expect(profile.halfGoneTime, isNotNull);
    expect(profile.goneTime, isNotNull);
    // Half of it goes before all of it does. Obvious, and worth pinning: both are read
    // off the same curve and a sign error would swap them.
    expect(profile.halfGoneTime!.isBefore(profile.goneTime!), isTrue);
    expect(profile.halfGoneTime!.isAfter(profile.peakTime), isTrue);
  });

  test('the thresholds are read AFTER the peak, not on the way up', () {
    // On the way up the drink passes through every value below its peak. If the crossing
    // were searched for from the start of the curve, "half gone" would be found during
    // ABSORPTION — minutes after the first sip, while the coffee is still arriving. It
    // would report a drink as half finished before it had finished being drunk.
    final profile = _profile(_coffee());

    expect(profile.halfGoneTime!.isAfter(profile.peakTime), isTrue,
        reason: 'a threshold crossed while rising is the drink ARRIVING, not fading');
  });

  test('right now is zero before the drink was drunk', () {
    final profile = _profile(
      _coffee(),
      now: _drunkAt.subtract(const Duration(hours: 1)),
    );

    expect(profile.currentMg, 0);
    expect(profile.isActive, isFalse);
  });

  test('a drink still in you is active; a day-old one is not', () {
    final soonAfter = _profile(
      _coffee(),
      now: _drunkAt.add(const Duration(hours: 1)),
    );
    expect(soonAfter.currentMg, greaterThan(caffeineNegligibleMg));
    expect(soonAfter.isActive, isTrue);

    // Not 20 hours: a 95mg coffee still has about 6mg in you then, which is the point of
    // the whole feature. It takes the best part of a day and a half to truly clear.
    final muchLater = _profile(
      _coffee(),
      now: _drunkAt.add(const Duration(hours: 30)),
    );
    expect(muchLater.isActive, isFalse);
  });

  test('a bigger drink peaks higher and lasts longer', () {
    final small = _profile(_coffee(mg: 40));
    final large = _profile(_coffee(mg: 200));

    expect(large.peakMg, greaterThan(small.peakMg));
    expect(large.goneTime!.isAfter(small.goneTime!), isTrue);
  });

  test('a drink of nothing does nothing', () {
    final profile = _profile(_coffee(mg: 0));

    expect(profile.peakMg, 0);
    expect(profile.currentMg, 0);
    // Nothing to fade FROM, so nothing to report.
    expect(profile.halfGoneTime, isNull);
  });

  test('the drink profile agrees with the calculator it is built on', () {
    final entry = _coffee();
    final at = _drunkAt.add(const Duration(hours: 3));
    final profile = _profile(entry, now: at);

    // The day's curve is the SUM of these. If the detail screen and the day chart ever
    // disagreed about the same coffee, one of them would be lying.
    expect(
      profile.currentMg,
      CaffeineInsightCalculator.contributionMg(
        entry: entry,
        at: at,
        preferences: const CaffeinePreferences(),
      ),
    );
  });
}
