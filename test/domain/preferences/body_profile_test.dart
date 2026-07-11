import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';

void main() {
  test('normalization keeps optional values in safe ranges', () {
    final normalized = const BodyProfile(
      birthYear: 2030,
      weightKg: 5.0,
      maxHeartRateBpm: 260,
      restingHeartRateBpm: 20,
    ).normalized(today: LocalDate(2026, 6, 30));

    expect(normalized.birthYear, isNull);
    expect(normalized.weightKg, BodyProfile.minWeightKg);
    expect(normalized.maxHeartRateBpm, BodyProfile.maxMaxHeartRateBpm);
    expect(normalized.restingHeartRateBpm, BodyProfile.minRestingHeartRateBpm);
  });

  test('age is derived from birth year', () {
    const profile = BodyProfile(birthYear: 1990);

    expect(profile.ageYears(today: LocalDate(2026, 6, 30)), 36);
  });

  test('empty profile has no age and automatic signature', () {
    const profile = BodyProfile();

    expect(profile.ageYears(today: LocalDate(2026, 6, 30)), isNull);
    expect(profile.signature(today: LocalDate(2026, 6, 30)).contains('auto'),
        isTrue);
  });
}
