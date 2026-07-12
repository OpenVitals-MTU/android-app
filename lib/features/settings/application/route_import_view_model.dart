import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../imports/application/pending_route_import.dart';
import '../../imports/application/route_bulk_import_view_model.dart';
import '../../manualentry/activity/activity_entry_view_model.dart';

part 'route_import_view_model.freezed.dart';

/// The route-import card's own state: the write permissions the import needs,
/// how many of them are granted, whether Health Connect can be asked at all, and
/// the lifecycle of the grant request.
///
/// The bulk import itself keeps its existing home in
/// [RouteBulkImportViewModel] — this state does not duplicate it.
@freezed
abstract class RouteImportState with _$RouteImportState {
  const RouteImportState._();

  const factory RouteImportState({
    @Default(<String>{}) Set<String> importPermissions,
    @Default(<String>{}) Set<String> granted,
    HealthConnectAvailability? availability,
    @Default(CommandState<void>.idle()) CommandState<void> grant,
  }) = _RouteImportState;

  int get grantedCount => importPermissions.where(granted.contains).length;

  Set<String> get missingPermissions => importPermissions.difference(granted);

  bool get healthConnectAvailable =>
      availability == HealthConnectAvailability.available;

  bool get isGranting => grant is CommandRunning<void>;
}

/// Owns the route-import card's repository access: the activity write-permission
/// taxonomy it displays and the Health Connect grant it can fire. The picker and
/// the navigation stay in the card — they are platform/UI concerns — but nothing
/// there touches a repository any more.
class RouteImportViewModel extends Notifier<RouteImportState> {
  /// Survives the rebuilds the granted-set refresh triggers, so a failed grant
  /// is not erased by the invalidation it asked for.
  CommandState<void> _grant = const CommandState.idle();

  @override
  RouteImportState build() {
    final activityRepo = ref.watch(activityRepositoryProvider);
    return RouteImportState(
      importPermissions: activityRepo.activityWritePermissions(),
      granted: ref.watch(grantedHealthPermissionsProvider).value ??
          const <String>{},
      availability: ref.watch(healthConnectAvailabilityProvider).value,
      grant: _grant,
    );
  }

  /// Asks Health Connect for the permissions the import is missing, then
  /// refreshes both gates so the card re-renders against the new grant.
  Future<void> grantPermissions() async {
    final missing = state.missingPermissions;
    if (missing.isEmpty) return;
    _setGrant(const CommandState.running());

    final result =
        await ref.read(healthRepositoryProvider).requestPermissions(missing);
    if (!ref.mounted) return;

    switch (result) {
      case Ok():
        _setGrant(const CommandState.success(null));
        ref.invalidate(grantedHealthPermissionsProvider);
        ref.invalidate(healthConnectAvailabilityProvider);
      case Err(:final failure):
        _setGrant(
          CommandState.failure(
            failure.toScreenError(fallback: 'Unable to request permissions.'),
          ),
        );
    }
  }

  /// Hands one picked file to the activity-entry form for review — the Kotlin
  /// `ExternalRouteImportRequest`. The card navigates once this returns.
  void stageSingleImport(ActivityRouteFileHandle handle) =>
      ref.read(pendingRouteImportProvider.notifier).set(handle);

  /// Writes every picked file straight through, no review step. The files are
  /// opened one at a time as the importer reaches them — see
  /// [ActivityRouteFileSource].
  Future<void> importRouteFiles(List<ActivityRouteFileSource> files) =>
      ref.read(routeBulkImportProvider.notifier).importRouteFiles(
            files,
            ref.read(unitSystemProvider),
          );

  void _setGrant(CommandState<void> next) {
    _grant = next;
    state = state.copyWith(grant: next);
  }
}

/// The state provider for the Settings route-import card.
final routeImportCardProvider =
    NotifierProvider<RouteImportViewModel, RouteImportState>(
  RouteImportViewModel.new,
);
