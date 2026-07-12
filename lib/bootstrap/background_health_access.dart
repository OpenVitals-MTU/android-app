import '../core/result/result.dart';
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
Future<Result<HealthDataSource>> openBackgroundHealthAccess() async {
  final dataSource =
      HealthConnectNativeDataSource(appPackageName: openVitalsPackageName);
  final refreshed = await HealthRepositoryImpl(dataSource).refreshAvailability();
  return refreshed.map((_) => dataSource);
}
