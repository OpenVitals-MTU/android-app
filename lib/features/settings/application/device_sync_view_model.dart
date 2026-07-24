import 'dart:async';

import 'package:collection/collection.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../devices/core/sync/device_sync_port.dart';
import '../../../di/providers.dart';
import '../../manualentry/activity/activity_entry_providers.dart';
import 'garmin_device_sync_port.dart';

part 'device_sync_view_model.freezed.dart';

/// Where a device sync has got to, for the row that started it.
///
/// Device-agnostic: any integration's sync drives this same state through the
/// [DeviceSyncPort] seam, so a second integration needs no new view-model.
@freezed
abstract class DeviceSyncState with _$DeviceSyncState {
  const DeviceSyncState._();

  const factory DeviceSyncState({
    /// The device id being synced, or null when idle. Scoped rather than a bare
    /// bool because the screen can list several watches and only one row should
    /// show a spinner.
    String? syncingDeviceId,
    DeviceSyncPhase? phase,
    @Default(0) int filesTotal,
    @Default(0) int filesDone,

    /// Files downloaded and handed to the importer by the last completed run.
    int? lastFileCount,
    String? errorMessage,
  }) = _DeviceSyncState;

  bool get isSyncing => syncingDeviceId != null;

  bool isSyncingDevice(String deviceId) => syncingDeviceId == deviceId;
}

/// Runs a device sync through whichever integration owns the device, and feeds
/// the row's state off the port's progress and outcome.
class DeviceSyncViewModel extends Notifier<DeviceSyncState> {
  @override
  DeviceSyncState build() => const DeviceSyncState();

  /// Syncs [deviceId]. Returns the number of files handed to the importer.
  ///
  /// One sync at a time: the radio is a single resource, and two sessions
  /// against one watch would fight over its handles.
  ///
  /// [listenAfter] is a diagnostic window held open after the sync — see the
  /// long-press on "Sync now". A device with no integration that [DeviceSyncPort.canSync]
  /// claims (a live sensor, an unknown id) is a no-op returning 0.
  Future<int> syncDevice(
    String deviceId, {
    Duration listenAfter = Duration.zero,
  }) async {
    if (state.isSyncing) return 0;

    // One radio: a bike computer can be a live BLE sensor during a recording,
    // and a GFDI file-sync over the same link would fight it for the device's
    // handles. So a sync waits until the recording ends. A watch never streams
    // live, but blocking it too keeps the rule simple and costs a user nothing
    // they would sensibly want mid-recording.
    if (ref.read(isRecordingActiveProvider)()) return 0;

    final device = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((d) => d.id == deviceId)
        .firstOrNull;
    if (device == null) return 0;
    final port = ref
        .read(deviceSyncPortsProvider)
        .firstWhereOrNull((p) => p.canSync(device));
    if (port == null) return 0;

    state = DeviceSyncState(
      syncingDeviceId: deviceId,
      phase: DeviceSyncPhase.handshake,
    );

    // The port owns the pull → import → store → stamp sequence; this view-model
    // only drives the row's state off its progress and outcome.
    final result = await port.sync(
      device,
      listenAfter: listenAfter,
      onProgress: (progress) {
        if (!ref.mounted || state.syncingDeviceId != deviceId) return;
        state = state.copyWith(
          phase: progress.phase,
          filesTotal: progress.filesTotal,
          filesDone: progress.filesDone,
        );
      },
    );
    if (!ref.mounted) {
      return switch (result) {
        DeviceSyncSucceeded(:final fileCount) => fileCount,
        DeviceSyncFailed() => 0,
      };
    }
    switch (result) {
      case DeviceSyncSucceeded(:final fileCount):
        state = DeviceSyncState(
          phase: DeviceSyncPhase.complete,
          lastFileCount: fileCount,
        );
        // The freshly-imported files are exactly the data the home-screen
        // widgets sit waiting on all morning: refresh them now rather than
        // leaving them to the 30-minute alarm. Fire-and-forget — the sync
        // outcome must not ride on a widget push.
        if (fileCount > 0) {
          unawaited(ref.read(homeWidgetRefresherProvider).refreshIfPlaced());
        }
        return fileCount;
      case DeviceSyncFailed(:final message):
        state = DeviceSyncState(errorMessage: message);
        return 0;
    }
  }

  /// Clears the finished/failed banner so the row goes back to normal.
  void clear() => state = const DeviceSyncState();
}

/// The sync ports, one per file-sync integration. The first whose
/// [DeviceSyncPort.canSync] matches a device owns its sync.
final deviceSyncPortsProvider = Provider<List<DeviceSyncPort>>(
  (ref) => [ref.watch(garminDeviceSyncPortProvider)],
);

final deviceSyncViewModelProvider =
    NotifierProvider<DeviceSyncViewModel, DeviceSyncState>(
  DeviceSyncViewModel.new,
);
