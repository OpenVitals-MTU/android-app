import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/sync/body_energy_chain_sync_service.dart';
import 'package:openvitals/data/sync/calories_history_sync_service.dart';
import 'package:openvitals/data/sync/history_sync_scheduler.dart';
import 'package:openvitals/data/sync/vitals_history_sync_service.dart';

/// Records the order and overlap of the drains it stands in for.
class _Recorder {
  final List<String> started = [];
  final List<String> finished = [];
  final Set<String> inFlight = {};
  final Set<String> overlapped = {};
  final Set<String> throwing = {};

  Future<void> run(String name) async {
    started.add(name);
    if (inFlight.isNotEmpty) overlapped.addAll(inFlight);
    inFlight.add(name);
    await Future<void>.delayed(Duration.zero);
    inFlight.remove(name);
    finished.add(name);
    if (throwing.contains(name)) throw StateError('$name drain failed');
  }
}

class _FakeVitals implements VitalsHistorySyncService {
  _FakeVitals(this._recorder);
  final _Recorder _recorder;
  int fullSyncs = 0;

  @override
  Future<void> syncIncremental() => _recorder.run('vitals');

  @override
  Future<void> syncAll() async {
    fullSyncs++;
    await _recorder.run('vitals-full');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeCalories implements CaloriesHistorySyncService {
  _FakeCalories(this._recorder);
  final _Recorder _recorder;
  int fullSyncs = 0;

  @override
  Future<void> syncIncremental() => _recorder.run('calories');

  @override
  Future<void> syncAll() async {
    fullSyncs++;
    await _recorder.run('calories-full');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeBodyEnergy implements BodyEnergyChainSyncService {
  _FakeBodyEnergy(this._recorder);
  final _Recorder _recorder;

  @override
  Future<void> syncAll({bool force = false}) => _recorder.run('bodyEnergy');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late _Recorder recorder;
  late _FakeVitals vitals;
  late _FakeCalories calories;
  late _FakeBodyEnergy bodyEnergy;
  late HistorySyncScheduler scheduler;

  setUp(() {
    recorder = _Recorder();
    vitals = _FakeVitals(recorder);
    calories = _FakeCalories(recorder);
    bodyEnergy = _FakeBodyEnergy(recorder);
    scheduler = HistorySyncScheduler(vitals, calories, bodyEnergy);
  });

  test('the drains run one after another, never at the same time', () async {
    // Health Connect serializes concurrent reads, so overlapping drains are the
    // 30s→80s contention the per-screen sequencing exists to avoid.
    await scheduler.drainIncremental();

    expect(recorder.started, ['vitals', 'calories', 'bodyEnergy']);
    expect(recorder.overlapped, isEmpty);
  });

  test('no drain starts a first full sync', () async {
    // A first full sync is a multi-minute history read and stays owned by the
    // screen that needs the cache: a user who never opens the vitals overview
    // must not pay for its 730-day reads on every app open.
    await scheduler.drainIncremental();

    expect(vitals.fullSyncs, 0);
    expect(calories.fullSyncs, 0);
  });

  test('a failing drain does not starve the ones after it', () async {
    recorder.throwing.add('vitals');

    await scheduler.drainIncremental();

    expect(recorder.finished, containsAll(['calories', 'bodyEnergy']));
  });

  test('concurrent calls share one run', () async {
    await Future.wait([
      scheduler.drainIncremental(),
      scheduler.drainIncremental(),
      scheduler.drainIncremental(),
    ]);

    expect(recorder.started, ['vitals', 'calories', 'bodyEnergy']);
  });

  test('a later app open drains again', () async {
    await scheduler.drainIncremental();
    await scheduler.drainIncremental();

    expect(recorder.started, hasLength(6));
  });
}
