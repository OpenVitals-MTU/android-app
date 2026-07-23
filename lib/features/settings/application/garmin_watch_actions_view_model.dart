import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../devices/garmin/garmin_ble_transport.dart';
import '../../../di/providers.dart';
import 'device_sync_view_model.dart';

part 'garmin_watch_actions_view_model.freezed.dart';

/// The Garmin-only watch actions that deliberately do NOT go through the sync
/// seam: making the watch ring (find) and the debug settings-tree probe. Both
/// are gated on GarminCapability and speak GFDI directly, so they stay
/// Garmin-typed rather than pretending to be device-agnostic.
@freezed
abstract class GarminWatchActionsState with _$GarminWatchActionsState {
  const GarminWatchActionsState._();

  const factory GarminWatchActionsState({
    /// The watch currently being made to ring, or null.
    String? findingDeviceId,

    /// The last find was refused by the watch. A flag, not a message: the
    /// wording is the screen's job, and this layer has no localizations.
    @Default(false) bool findFailed,
    String? errorMessage,
  }) = _GarminWatchActionsState;

  bool isFindingDevice(String deviceId) => findingDeviceId == deviceId;
}

class GarminWatchActionsViewModel extends Notifier<GarminWatchActionsState> {
  @override
  GarminWatchActionsState build() => const GarminWatchActionsState();

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
    // One radio: a find cannot start while a sync is running (the sync state
    // lives in the generic view-model now), nor while another find is in flight.
    if (ref.read(deviceSyncViewModelProvider).isSyncing ||
        state.findingDeviceId != null) {
      return;
    }

    final devices = ref.read(readPairedBleDevicesUseCaseProvider)();
    final device = devices.where((d) => d.id == deviceId).firstOrNull;
    if (device == null || !device.isGarminGfdi) return;

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
    if (ref.read(deviceSyncViewModelProvider).isSyncing ||
        state.findingDeviceId != null) {
      return 0;
    }
    final device = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((d) => d.id == deviceId)
        .firstOrNull;
    if (device == null || !device.isGarminGfdi) return 0;

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

  String _describe(Object error) {
    if (error is GarminBleTransportException) return error.message;
    final text = error.toString();
    return text.isEmpty ? 'The watch could not be synced.' : text;
  }
}

final garminWatchActionsViewModelProvider =
    NotifierProvider<GarminWatchActionsViewModel, GarminWatchActionsState>(
  GarminWatchActionsViewModel.new,
);
