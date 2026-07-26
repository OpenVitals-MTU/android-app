import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/impl/body_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';

/// A source with an optional measured weight and height, and a controllable
/// permission set — the two axes the resolution has to get right.
class _FakeSource extends HealthDataSource {
  _FakeSource({this.weightKg, this.heightCm, Set<String>? granted})
      : granted = granted ??
            {HcPermissions.readWeight, HcPermissions.readHeight};

  final double? weightKg;
  final double? heightCm;
  final Set<String> granted;

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => granted;

  @override
  Future<WeightEntry?> readLatestWeight() async => weightKg == null
      ? null
      : WeightEntry(
          time: DateTime(2026, 7, 24),
          weightKg: weightKg!,
          source: 'scale',
        );

  @override
  Future<double?> readLatestHeight() async => heightCm;

  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

void main() {
  const declared = BodyProfile(
    birthYear: 1993,
    weightKg: 76.0,
    heightCm: 178.0,
    restingHeartRateBpm: 70,
    maxHeartRateBpm: 160,
  );

  Future<BodyProfile> resolve(_FakeSource source) async =>
      (await BodyRepositoryImpl(source).resolveBodyProfile(declared)).orThrow();

  test('a measured weight beats the declared one', () async {
    // The whole point: the app used to be 76 kg on the caffeine screen and
    // 81 kg on the body screen, with nothing reconciling the two.
    final resolved = await resolve(_FakeSource(weightKg: 81.2, heightCm: 181.0));

    expect(resolved.weightKg, 81.2);
    expect(resolved.heightCm, 181.0);
  });

  test('the declared value survives when nothing is recorded', () async {
    final resolved = await resolve(_FakeSource());

    expect(resolved.weightKg, 76.0);
    expect(resolved.heightCm, 178.0);
  });

  test('a missing permission falls back rather than blanking the value',
      () async {
    // A filter that returns nothing must be indistinguishable from "no
    // preference", not from "you weigh nothing".
    final resolved = await resolve(
      _FakeSource(weightKg: 81.2, heightCm: 181.0, granted: const {}),
    );

    expect(resolved.weightKg, 76.0);
    expect(resolved.heightCm, 178.0);
  });

  test('the rest of the profile is untouched', () async {
    // Resolution is about body size only. Age and the heart rates are declared
    // facts with no measured counterpart.
    final resolved = await resolve(_FakeSource(weightKg: 81.2));

    expect(resolved.birthYear, 1993);
    expect(resolved.restingHeartRateBpm, 70);
    expect(resolved.maxHeartRateBpm, 160);
  });

  test('a measured value out of range is normalised, not trusted blindly',
      () async {
    final resolved = await resolve(_FakeSource(weightKg: 900.0));

    expect(resolved.weightKg, BodyProfile.maxWeightKg);
  });
}
