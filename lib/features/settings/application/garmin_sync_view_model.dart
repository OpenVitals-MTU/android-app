import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../devices/core/sync/device_sync_port.dart';
import '../../../devices/garmin/garmin_ble_transport.dart';
import '../../../devices/garmin/garmin_session.dart';
import '../../../di/providers.dart';
import 'garmin_device_sync_port.dart';

part 'garmin_sync_view_model.freezed.dart';

/// Where a watch sync has got to, for the device row.
@freezed
abstract class GarminSyncState with _$GarminSyncState {
  const GarminSyncState._();

  const factory GarminSyncState({
    /// The device id being synced, or null when idle. Scoped rather than a bare
    /// bool because the screen can list several watches and only one row should
    /// show a spinner.
    String? syncingDeviceId,
    GarminSyncPhase? phase,
    @Default(0) int filesTotal,
    @Default(0) int filesDone,

    /// Files downloaded and handed to the importer by the last completed run.
    int? lastFileCount,
    String? errorMessage,

    /// The watch currently being made to ring, or null.
    String? findingDeviceId,

    /// The last find was refused by the watch. A flag, not a message: the
    /// wording is the screen's job, and this layer has no localizations.
    @Default(false) bool findFailed,
  }) = _GarminSyncState;

  bool get isSyncing => syncingDeviceId != null;

  bool isSyncingDevice(String deviceId) => syncingDeviceId == deviceId;

  bool isFindingDevice(String deviceId) => findingDeviceId == deviceId;
}

/// Runs a Garmin watch sync and feeds what it downloads into the existing FIT
/// import pipeline.
///
/// The division of labour: [GarminWatchSyncService] owns the radio and the
/// protocol, this owns the app-level sequence — sync, import, record what was
/// taken so the next run can skip it, stamp the device.
class GarminSyncViewModel extends Notifier<GarminSyncState> {
  @override
  GarminSyncState build() => const GarminSyncState();

  /// Syncs [deviceId]. Returns the number of files handed to the importer.
  ///
  /// One sync at a time: the radio is a single resource, and two sessions
  /// against one watch would fight over its ML handles.
  ///
  /// [listenAfter] is a diagnostic: the link normally closes the moment the sync
  /// finishes, about a second in, so anything the watch volunteers on its own
  /// schedule is never seen. A non-zero window holds it open and logs what
  /// arrives. Debug builds only — see the long-press on "Sync now".
  Future<int> syncDevice(
    String deviceId, {
    Duration listenAfter = Duration.zero,
  }) async {
    if (state.isSyncing) return 0;

    final devices = ref.read(readPairedBleDevicesUseCaseProvider)();
    final device = devices.where((d) => d.id == deviceId).firstOrNull;
    if (device == null || !device.isWatch) return 0;

    state = GarminSyncState(
      syncingDeviceId: deviceId,
      phase: GarminSyncPhase.handshake,
    );

    // The port owns the pull → import → store → stamp sequence (incl. the
    // load-bearing BLE-link release); this view-model only drives the row's
    // state off its progress and outcome.
    final result = await ref.read(garminDeviceSyncPortProvider).sync(
      device,
      listenAfter: listenAfter,
      onProgress: (progress) {
        if (!ref.mounted || state.syncingDeviceId != deviceId) return;
        state = state.copyWith(
          phase: _garminPhase(progress.phase),
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
        state = GarminSyncState(
          phase: GarminSyncPhase.complete,
          lastFileCount: fileCount,
        );
        return fileCount;
      case DeviceSyncFailed(:final message):
        state = GarminSyncState(errorMessage: message);
        return 0;
    }
  }

  /// Makes the watch ring, and stops it.
  ///
  /// A toggle rather than a fire-and-forget: the protocol alerts for a minute
  /// unless cancelled, so the same control has to be able to stop it — and the
  /// link stays open for the duration, which is why this cannot share the sync
  /// path that closes it a second in.
  Future<void> toggleFind(String deviceId) async {
    if (state.isFindingDevice(deviceId)) {
      // Stop stays enabled until the watch answers the cancel — a full round
      // trip — so this branch is reachable twice, and completing a completed
      // completer throws.
      final cancel = _findCancel;
      if (cancel != null && !cancel.isCompleted) cancel.complete();
      return;
    }
    if (state.isSyncing || state.findingDeviceId != null) return;

    final devices = ref.read(readPairedBleDevicesUseCaseProvider)();
    final device = devices.where((d) => d.id == deviceId).firstOrNull;
    if (device == null || !device.isWatch) return;

    final cancel = Completer<void>();
    _findCancel = cancel;
    state = state.copyWith(
      findingDeviceId: deviceId,
      findFailed: false,
      errorMessage: null,
    );
    try {
      final accepted =
          await ref.read(garminWatchSyncServiceProvider).findWatch(
                address: device.address,
                phoneName: ref.read(phoneIdentityProvider).bluetoothName,
                manufacturer: ref.read(phoneIdentityProvider).manufacturer,
                model: ref.read(phoneIdentityProvider).model,
                cancelled: cancel.future,
              );
      if (!ref.mounted) return;
      state = state.copyWith(findingDeviceId: null, findFailed: !accepted);
    } catch (error) {
      if (!ref.mounted) return;
      debugPrint('[GARMIN-FIND] failed: $error');
      state = state.copyWith(
        findingDeviceId: null,
        errorMessage: _describe(error),
      );
    } finally {
      _findCancel = null;
    }
  }

  Completer<void>? _findCancel;

  /// Debug-only: opens the watch's settings service and dumps its root screen.
  ///
  /// A diagnostic rather than a feature — the tree is defined by the watch and
  /// read with a schema older than its firmware, so the first step is to look at
  /// what actually comes back before drawing anything from it.
  Future<int> probeSettings(String deviceId) async {
    if (state.isSyncing || state.findingDeviceId != null) return 0;
    final device = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((d) => d.id == deviceId)
        .firstOrNull;
    if (device == null || !device.isWatch) return 0;

    final phone = ref.read(phoneIdentityProvider);
    final locale = PlatformDispatcher.instance.locale;
    return ref.read(garminWatchSyncServiceProvider).probeSettings(
          address: device.address,
          phoneName: phone.bluetoothName,
          manufacturer: phone.manufacturer,
          model: phone.model,
          // The watch translates the whole tree with this, so it must be the
          // phone's locale rather than a hard-coded en_US.
          language: '${locale.languageCode}_'
              '${(locale.countryCode ?? 'US').toUpperCase()}',
          region: (locale.countryCode ?? 'US').toLowerCase(),
        );
  }

  /// Clears the finished/failed banner so the row goes back to normal.
  void clear() => state = const GarminSyncState();

  String _describe(Object error) {
    if (error is GarminBleTransportException) return error.message;
    final text = error.toString();
    return text.isEmpty ? 'The watch could not be synced.' : text;
  }
}

/// Maps the generic sync phase the port reports back onto [GarminSyncPhase]
/// (1:1 today), so the row's state stays Garmin-typed until Phase 3 generalises
/// this view-model.
GarminSyncPhase _garminPhase(DeviceSyncPhase phase) => switch (phase) {
      DeviceSyncPhase.handshake => GarminSyncPhase.handshake,
      DeviceSyncPhase.listing => GarminSyncPhase.listing,
      DeviceSyncPhase.downloading => GarminSyncPhase.downloading,
      DeviceSyncPhase.complete => GarminSyncPhase.complete,
      DeviceSyncPhase.failed => GarminSyncPhase.failed,
    };

final garminSyncViewModelProvider =
    NotifierProvider<GarminSyncViewModel, GarminSyncState>(
  GarminSyncViewModel.new,
);
