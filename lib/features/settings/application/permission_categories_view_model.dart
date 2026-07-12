import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../data/repository/contract/health_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../ui/components/health_connect_gate.dart';

part 'permission_categories_view_model.freezed.dart';

/// A single Health Connect permission category, mirroring the Kotlin
/// `SettingsPermissionCategory` data class. Carries the permission strings that
/// make up the category plus the subset that can only be granted manually
/// (via the Health Connect settings screen rather than the runtime dialog).
class PermissionCategory {
  const PermissionCategory({
    required this.id,
    required this.permissions,
    this.manualPermissions = const <String>{},
    this.available = true,
  });

  final String id;
  final Set<String> permissions;
  final Set<String> manualPermissions;

  /// False when the installed Health Connect version cannot support the
  /// category (e.g. mindfulness on an older provider).
  final bool available;
}

/// The permission-categories card state: the taxonomy, the Health Connect
/// availability and granted set it is rendered against, and the lifecycle of the
/// grant/open action.
///
/// [availability] or [granted] still being null means "checking" — the card
/// renders its placeholder rather than a wrong status.
@freezed
abstract class PermissionCategoriesState with _$PermissionCategoriesState {
  const factory PermissionCategoriesState({
    @Default(<PermissionCategory>[]) List<PermissionCategory> categories,
    HealthConnectAvailability? availability,
    Set<String>? granted,
    @Default(CommandState<void>.idle()) CommandState<void> request,
  }) = _PermissionCategoriesState;
}

/// Owns the settings permission taxonomy and the grant action behind it.
///
/// The category list is a faithful port of `SettingsViewModel.permissionCategories`
/// — same ids, same membership, same `isNotEmpty` filter and mindfulness gate.
/// The request is a [CommandState]: the card renders `running` / `failure` and
/// never touches [HealthRepository] itself.
class PermissionCategoriesViewModel
    extends Notifier<PermissionCategoriesState> {
  /// Survives the rebuilds that the availability / granted providers trigger —
  /// a permission request that just failed must not be erased by the granted-set
  /// refresh it itself asked for.
  CommandState<void> _request = const CommandState.idle();

  @override
  PermissionCategoriesState build() {
    final repo = ref.watch(healthRepositoryProvider);
    final availability = ref.watch(healthConnectAvailabilityProvider).value;
    final granted = ref.watch(grantedHealthPermissionsProvider).value;

    return PermissionCategoriesState(
      categories: _categories(repo, availability),
      availability: availability,
      granted: granted,
      request: _request,
    );
  }

  List<PermissionCategory> _categories(
    HealthRepository repo,
    HealthConnectAvailability? availability,
  ) {
    final mindfulnessAvailable =
        availability == HealthConnectAvailability.available &&
            repo.isMindfulnessAvailable();

    return <PermissionCategory>[
      PermissionCategory(id: 'activity_sleep', permissions: repo.corePermissions),
      PermissionCategory(id: 'heart_recovery', permissions: repo.heartPermissions),
      PermissionCategory(id: 'body', permissions: repo.bodyPermissions),
      PermissionCategory(
        id: 'activity_extras',
        permissions: repo.activityExtrasPermissions,
      ),
      PermissionCategory(
        id: 'nutrition_hydration',
        permissions: repo.nutritionHydrationPermissions,
      ),
      PermissionCategory(
        id: 'manual_entry_write',
        permissions: repo.requestableWritePermissions,
      ),
      PermissionCategory(
        id: 'mindfulness',
        permissions: repo.mindfulnessPermissions,
        available: mindfulnessAvailable,
      ),
      PermissionCategory(
        id: 'additional_data_access',
        permissions: {
          ...repo.additionalDataAccessPermissions,
          ...repo.routePermissions,
        },
        manualPermissions: repo.routePermissions,
      ),
      PermissionCategory(id: 'vitals', permissions: repo.vitalsPermissions),
      PermissionCategory(
        id: 'cycle_tracking',
        permissions: repo.cyclePermissions,
      ),
      // Drop a category with nothing to show — but KEEP one that is explicitly
      // unavailable, so Settings can still say "Not supported" with a reason
      // (Kotlin's SettingsViewModel does not filter these out; only *onboarding*
      // does). Since Kotlin 1.9.0 (1f2b435) `mindfulnessPermissions` is correctly
      // empty on a provider that lacks mindfulness, so filtering on emptiness
      // alone would silently delete that row.
    ].where((c) => c.permissions.isNotEmpty || !c.available).toList();
  }

  /// Grants a category: the runtime dialog for the [requestable] permissions,
  /// or — when only manually-grantable ones are left — the Health Connect
  /// settings screen. Refreshes the granted set on success.
  Future<void> requestCategory({
    required Set<String> requestable,
    required Set<String> manual,
  }) async {
    if (requestable.isEmpty && manual.isEmpty) return;
    _setRequest(const CommandState.running());

    final repo = ref.read(healthRepositoryProvider);
    final result = requestable.isNotEmpty
        ? await repo.requestPermissions(requestable)
        : await repo.openHealthConnectSettings();
    if (!ref.mounted) return;

    switch (result) {
      case Ok():
        _setRequest(const CommandState.success(null));
        ref.invalidate(grantedHealthPermissionsProvider);
      case Err(:final failure):
        _setRequest(
          CommandState.failure(
            failure.toScreenError(fallback: 'Unable to request permissions.'),
          ),
        );
    }
  }

  /// Returns the command to rest — the card calls this once it has consumed a
  /// success, or when the user dismisses a failure.
  void clearRequest() => _setRequest(const CommandState.idle());

  void _setRequest(CommandState<void> next) {
    _request = next;
    state = state.copyWith(request: next);
  }
}

/// The state provider for the Health Connect permission-categories card.
final permissionCategoriesProvider = NotifierProvider<
    PermissionCategoriesViewModel, PermissionCategoriesState>(
  PermissionCategoriesViewModel.new,
);
