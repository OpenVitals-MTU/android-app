import 'health_data_source.dart';

/// [HealthDataSource] for platforms with no native health bridge (currently
/// everything except Android).
///
/// It adds no overrides: the [HealthDataSource] base already returns
/// `notSupported` availability and empty/`null`/`0` results for every read, and
/// throws / no-ops for writes, which is exactly the desired behaviour when no
/// health provider is wired up.
///
// TODO(healthkit): native HealthKit bridge — iOS support is a planned later
//   native bridge; until then iOS resolves to this not-supported source.
class UnsupportedHealthDataSource extends HealthDataSource {
  UnsupportedHealthDataSource({super.appPackageName});
}
