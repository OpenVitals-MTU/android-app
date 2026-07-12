import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../manualentry/activity/activity_entry_view_model.dart';

/// App-lifetime hand-off of a route file picked outside the activity-entry form
/// (the Settings "single route import" and "FIT import" cards) into the form for
/// review. Dart analogue of the Kotlin `ExternalRouteImportRequest`: the settings
/// card sets the pending handle then navigates to the activity-entry route, and
/// [ActivityEntryScreen] consumes it once on open (calling
/// `ActivityEntryViewModel.importRouteFile`) and clears it.
class PendingRouteImportViewModel extends Notifier<ActivityRouteFileHandle?> {
  @override
  ActivityRouteFileHandle? build() => null;

  /// Stores the picked file, replacing any earlier un-consumed handle.
  void set(ActivityRouteFileHandle handle) => state = handle;

  /// Reads and clears the pending handle in one shot, so it is consumed exactly
  /// once. Returns null when nothing is pending.
  ActivityRouteFileHandle? take() {
    final handle = state;
    state = null;
    return handle;
  }

  void clear() => state = null;
}

final pendingRouteImportProvider =
    NotifierProvider<PendingRouteImportViewModel, ActivityRouteFileHandle?>(
  PendingRouteImportViewModel.new,
);
