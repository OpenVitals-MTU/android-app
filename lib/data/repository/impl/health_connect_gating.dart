import '../../../domain/model/health_connect_availability.dart';
import '../../../health/health_data_source.dart';

/// The one rule every Health-Connect-backed repository has to apply before it
/// trusts a permission: **a grant only counts when Health Connect is actually
/// available.**
///
/// Without the availability check, an unsupported or not-installed provider
/// reports no granted permissions — which is indistinguishable from "the user
/// said no", and would send every screen down the "ask for permission" path on a
/// device that has nowhere to ask.
///
/// This lived as a byte-identical private copy in all nine repository impls plus
/// [dashboard_data_loader.dart]. It is an **extension**, not a mixin or a base
/// class, on purpose: a mixin's members are public, and `DashboardDataLoader` is
/// faked with `implements DashboardDataLoader` in the home-widget tests, so any
/// member added to its interface would force every one of those stubs to
/// implement it. An extension adds nothing to any class's interface.
extension HealthConnectGating on HealthDataSource {
  /// The granted permissions, or the empty set when Health Connect is
  /// unavailable — never the permissions of a provider that is not there.
  Future<Set<String>> grantedIfAvailable() async =>
      cachedAvailability == HealthConnectAvailability.available
          ? grantedPermissions()
          : <String>{};
}
