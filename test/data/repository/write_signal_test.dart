import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/refresh/data_change_sink.dart';
import 'package:openvitals/domain/refresh/data_domain.dart';
import 'package:openvitals/state/refresh_coordinator.dart';

import '../../support/boot_container.dart';

/// The third refresh trigger, proved where it is emitted: at the repository
/// boundary, over the real graph down to the fake host API.
///
/// Signalling here rather than at the ~20 view-model call sites is the whole
/// design — a view-model does not know that writing a drink also stores a
/// nutrition record, and every historical attempt to remember at the call site
/// missed at least one path (the Apple Health import invalidated nothing at all).
void main() {
  late List<RefreshSignal> signals;

  Future<HealthHarness> boot({Set<String>? granted}) async {
    final h = await bootContainer(granted: granted);
    signals = <RefreshSignal>[];
    h.container.listen<RefreshSignal>(
      refreshCoordinatorProvider,
      (previous, next) => signals.add(next),
    );
    return h;
  }

  /// Lets the coordinator's trailing debounce fire.
  Future<void> settle() async {
    await Future<void>.delayed(kDataChangeDebounce * 2);
    await pumpEventQueue();
  }

  test('writing a body measurement announces the body domain', () async {
    final h = await boot();

    final result = await h.container
        .read(bodyRepositoryProvider)
        .writeBodyMeasurementEntry(BodyMeasurementWriteRequest(
          type: BodyMeasurementType.weight,
          time: DateTime.utc(2025, 6, 25, 8),
          value: 72.5,
        ));
    expect(result, isA<Ok<String>>());
    await settle();

    expect(signals, hasLength(1));
    expect(signals.single.reason, RefreshReason.dataChanged);
    expect(signals.single.domains, contains(DataDomain.body));
  });

  test('logging a drink announces nutrition and caffeine as well as hydration',
      () async {
    // The hydration repository stores a paired nutrition record and caffeine
    // rides on it. Only the boundary knows that; the entry form does not.
    final h = await boot();

    final result = await h.container
        .read(hydrationRepositoryProvider)
        .writeHydrationEntry(HydrationWriteRequest(
          time: DateTime.utc(2025, 6, 25, 10),
          volumeLiters: 0.25,
        ));
    expect(result, isA<Ok<String>>());
    await settle();

    expect(signals, hasLength(1));
    expect(
      signals.single.domains,
      containsAll(<DataDomain>{
        DataDomain.hydration,
        DataDomain.nutrition,
        DataDomain.caffeine,
      }),
    );
  });

  test('a vitals write announces the vitals domain', () async {
    final h = await boot();

    final result = await h.container
        .read(vitalsRepositoryProvider)
        .writeVitalsMeasurementEntry(VitalsMeasurementWriteRequest(
          type: VitalsMeasurementType.spo2,
          time: DateTime.utc(2025, 6, 25, 9),
          value: 97,
        ));
    expect(result, isA<Ok<String>>());
    await settle();

    expect(signals.single.domains, contains(DataDomain.vitals));
  });

  test('a write refused for a missing permission announces nothing', () async {
    // Nothing was stored, so nothing became stale. A signal here would send
    // every visible screen back to Health Connect for no reason.
    final h = await boot(granted: const <String>{});

    final result = await h.container
        .read(bodyRepositoryProvider)
        .writeBodyMeasurementEntry(BodyMeasurementWriteRequest(
          type: BodyMeasurementType.weight,
          time: DateTime.utc(2025, 6, 25, 8),
          value: 72.5,
        ));
    expect(result, isA<Err<String>>());
    await settle();

    expect(signals, isEmpty);
  });

  test('several writes in a row collapse into one signal', () async {
    final h = await boot();
    final repository = h.container.read(hydrationRepositoryProvider);

    for (var hour = 8; hour < 14; hour++) {
      await repository.writeHydrationEntry(HydrationWriteRequest(
        time: DateTime.utc(2025, 6, 25, hour),
        volumeLiters: 0.25,
      ));
    }
    await settle();

    expect(signals, hasLength(1));
  });

  test('the default sink lets a repository write with no container at all', () {
    // The background-isolate shape: the home-screen widget's one-tap log and the
    // reminder alarms build their own repositories and have nowhere to broadcast
    // to. The app-open refresh is what covers them.
    const sink = NoopDataChangeSink();
    expect(() => sink.changed({DataDomain.hydration}), returnsNormally);
  });
}
