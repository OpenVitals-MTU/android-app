import 'package:flutter/foundation.dart';
import 'package:health_connect_native/health_connect_native.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../core/result/result.dart';
import '../data/prefs/preferences_repository.dart';
import '../data/repository/impl/health_repository_impl.dart';
import '../data/source/health/health_data_source.dart';
import '../data/source/health/native/health_connect_native_data_source.dart';
import '../di/providers.dart';

/// Builds the Health Connect data source for a **background isolate** — and
/// resolves its availability before handing it back.
///
/// This is the single most expensive thing in this codebase to relearn, and it
/// has been relearned four times. `HealthDataSource.cachedAvailability` starts
/// at `notSupported`, and every repository gates its reads and writes on it. An
/// isolate that builds a data source and starts reading gets **empty results
/// and missing permissions, with no error** — the home widgets showed "grant
/// permission" to users who had granted everything; one-tap logging silently
/// wrote nothing; both reminder alarms read today's intake as zero and nagged
/// forever.
///
/// Screens never hit this, because `HealthConnectGate` mounts the refresh for
/// them. Isolates have no widget tree, so they have to do it themselves — and
/// the way to not forget is to have no other way to get a data source.
///
/// Use this from every isolate entrypoint. Do not construct
/// [HealthConnectNativeDataSource] directly.
/// [hostApi] exists for tests only, so the refresh-before-handoff contract is
/// provable against a fake host; production callers never pass it.
Future<Result<HealthDataSource>> openBackgroundHealthAccess({
  @visibleForTesting HealthConnectHostApi? hostApi,
}) async {
  final dataSource = await _buildBackgroundHealthDataSource(hostApi);
  final refreshed = await HealthRepositoryImpl(dataSource).refreshAvailability();
  return refreshed.map((_) => dataSource);
}

/// Builds the background Health Connect data source WITHOUT refreshing
/// availability. Private so no isolate can construct one that skips the refresh —
/// the whole reason this file exists.
///
/// The isolate must read the mindfulness opt-in the same way the app does. If it
/// did not, the mindfulness reminder would resolve the feature as unavailable,
/// read today's minutes as zero, decide the goal was never met, and nag forever —
/// the silent-empty failure this whole file exists to prevent (AGENTS.md §1).
Future<HealthDataSource> _buildBackgroundHealthDataSource(
  HealthConnectHostApi? hostApi,
) async {
  final prefs = await SharedPreferences.getInstance();
  final preferences = PreferencesRepository(prefs);
  return HealthConnectNativeDataSource(
    hostApi: hostApi,
    appPackageName: openVitalsPackageName,
    mindfulnessIntegrationEnabled: () =>
        preferences.healthConnectMindfulnessEnabled,
  );
}
