import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import '../../../state/app_providers.dart';
import 'activity_entry_clock.dart';
import 'activity_entry_view_model.dart';
import 'recording/activity_recording.dart';
import 'recording/activity_recording_device_support.dart';
import 'recording/activity_recording_draft_store.dart';
import 'recording/activity_recording_serialization.dart';
import 'recording/activity_recording_service.dart';

/// DI graph for the activity manual-entry / recording feature (Phase 6d),
/// replacing the Hilt bindings for `ActivityRecordingStore`,
/// `ActivityRecordingController`, `ActivityRecordingDraftStore` and
/// `RouteFileImporter`.

final activityRecordingStoreProvider = Provider<ActivityRecordingStore>(
  (ref) => ActivityRecordingStore(ref.watch(sharedPreferencesProvider)),
);

/// App-lifetime in-memory hand-off of a finished-but-unsaved recording draft.
final activityRecordingDraftStoreProvider =
    Provider<ActivityRecordingDraftStore>((ref) => ActivityRecordingDraftStore());

/// The device-bound recording service (GPS / sensors / foreground service).
/// The PLATFORM side: `ActivityRecordingViewModel` is the screen's.
final activityRecordingServiceProvider =
    Provider<ActivityRecordingService>((ref) {
  final service = ActivityRecordingService(
    preferencesRepository: ref.watch(preferencesRepositoryProvider),
    bleSensorCoordinator: ref.watch(bleSensorRepositoryProvider),
    recordingStore: ref.watch(activityRecordingStoreProvider),
    unitFormatter: ref.watch(unitFormatterProvider),
    deviceSupport: ref.watch(activityRecordingDeviceSupportProvider),
  );
  ref.onDispose(service.dispose);
  return service;
});

/// The recorder as its consumers see it. Overriding THIS is how a test (and,
/// once it moves over, the view-model) swaps the device out.
final activityRecordingControllerProvider =
    Provider<ActivityRecordingController>(
  (ref) => ref.watch(activityRecordingServiceProvider),
);

final routeFileImporterProvider = Provider<RouteFileImporter>(
  (ref) => const DefaultRouteFileImporter(),
);

/// The clock the entry form dates its records with. Overridable, so a test can
/// pin "now" without pinning the device clock.
final activityEntryClockProvider = Provider<ActivityEntryClock>(
  (ref) => ActivityEntryClock.system(),
);
