import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../data/local/open_vitals_database.dart';
import '../../../data/source/sensors/garmin/garmin_ble_transport.dart';
import '../../../data/source/sensors/garmin/garmin_session.dart';
import '../../../di/providers.dart';
import '../../../state/app_providers.dart';
import '../../imports/application/route_bulk_import_view_model.dart';
import '../../manualentry/activity/routeimport/fit_route_parser.dart';
import 'watch_metrics_view_model.dart';
import 'watch_settings_view_model.dart';

part 'garmin_sync_view_model.freezed.dart';

/// The importer instance the watch sync writes through.
///
/// Its own provider, following [fitBulkImportProvider]'s reasoning: the same
/// engine as the folder importers, with independent state, so a "file 3/12"
/// progress line appears under the watch that started it and nowhere else.
final garminBulkImportProvider =
    NotifierProvider<RouteBulkImportViewModel, RouteBulkImportState>(
  RouteBulkImportViewModel.new,
);

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

    final repository = ref.read(bleDeviceRepositoryProvider);
    final service = ref.read(garminWatchSyncServiceProvider);
    final phone = ref.read(phoneIdentityProvider);

    state = GarminSyncState(
      syncingDeviceId: deviceId,
      phase: GarminSyncPhase.handshake,
    );

    final List<GarminDownloadedFile> downloaded;
    try {
      // A watch has ONE link. Browsing its settings holds it open for a while
      // after the screen closes, and a sync starting inside that window used to
      // connect against a radio already in use and never come back — no error,
      // no log, just a spinner. Take the link back first.
      //
      // Inside the try, because `state` already says "syncing": a throw out
      // here would leave that stuck forever, and the guard at the top of this
      // method would then refuse every future sync until the app restarted.
      await releaseWatchSettingsLink(deviceId);
      // The provider still holds the link that was just closed. Without this an
      // open settings screen keeps a dead one and quietly reports that the
      // watch sent nothing.
      ref.invalidate(watchSettingsLinkProvider(deviceId));
      downloaded = await service.sync(
        address: device.address,
        phoneName: phone.bluetoothName,
        manufacturer: phone.manufacturer,
        model: phone.model,
        alreadySynced: repository.syncedFileKeys(deviceId),
        listenAfter: listenAfter,
        onCapabilities: (capabilities) =>
            repository.recordCapabilities(deviceId, capabilities),
        onProgress: (progress) {
          if (!ref.mounted || state.syncingDeviceId != deviceId) return;
          state = state.copyWith(
            phase: progress.phase,
            filesTotal: progress.filesTotal,
            filesDone: progress.filesDone,
          );
        },
      );
    } on GarminBleTransportException catch (error) {
      if (!ref.mounted) return 0;
      state = GarminSyncState(errorMessage: _describe(error));
      return 0;
    } catch (error) {
      if (!ref.mounted) return 0;
      debugPrint('[GARMIN-SYNC] failed: $error');
      state = GarminSyncState(errorMessage: _describe(error));
      return 0;
    }
    if (!ref.mounted) return 0;

    if (downloaded.isNotEmpty) {
      try {
        // Straight into the folder importer's path: batching, Health Connect
        // quota handling, the wellness/activity split and per-file error
        // tolerance all come for free, and stay in step with it forever.
        await ref.read(garminBulkImportProvider.notifier).importRouteFiles(
              [
                for (final file in downloaded)
                  ActivityRouteFileSource.ofBytes(
                    bytes: file.bytes,
                    // Indexed, not numbered: several files share the 65535
                    // "unset" file number, and identically-named entries in the
                    // import log cannot be told apart when one of them fails.
                    fileName: '${file.entry.type.name}_'
                        '${file.entry.fileIndex}.fit',
                  ),
              ],
              ref.read(unitSystemProvider),
            );
        // Stress and Body Battery have no Health Connect type, so they go to the
        // app's own table instead — separately from the import above, which only
        // handles what Health Connect can hold.
        await _storeWatchOnlyMetrics(downloaded);
      } catch (error) {
        // The importer tolerates a bad FILE internally, so reaching here means
        // the write path itself is unavailable (no Health Connect, permissions
        // revoked mid-run). Without this catch the throw escaped syncDevice and
        // left the row spinning forever with no way back.
        if (!ref.mounted) return 0;
        debugPrint('[GARMIN-SYNC] import failed: $error');
        state = GarminSyncState(errorMessage: _describe(error));
        // Neither the keys nor the sync stamp are written: nothing reached
        // Health Connect, so the next run must fetch these files again.
        return 0;
      }
      if (!ref.mounted) return downloaded.length;

      // Recorded AFTER the import, so a run that died mid-import re-downloads
      // rather than skipping files that never reached Health Connect.
      repository.recordSyncedFileKeys(
        deviceId,
        // Files with no stable key are not recorded — they are re-fetched every
        // sync by design rather than skipped on a key that identifies nothing.
        downloaded.map((f) => f.entry.dedupKey).whereType<String>(),
      );
    }

    repository.markSynced(deviceId, DateTime.now().toUtc());
    // The device view, the watch-data screen and the summary tile all read the
    // stored metrics; without this they keep showing what was there before the
    // sync that just ran.
    ref.invalidate(watchMetricsProvider);
    state = GarminSyncState(
      phase: GarminSyncPhase.complete,
      lastFileCount: downloaded.length,
    );
    return downloaded.length;
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

  /// Extracts the watch-only metrics from the downloaded files and upserts them.
  ///
  /// Deliberately re-decodes each file rather than threading these through the
  /// Health Connect importer: that pipeline is about records Health Connect can
  /// hold, and widening it to carry passengers it cannot store would blur what
  /// it is for. Decoding a kilobyte twice costs nothing.
  Future<void> _storeWatchOnlyMetrics(
    List<GarminDownloadedFile> downloaded,
  ) async {
    final rows = <GarminWellnessSamplesCompanion>[];
    for (final file in downloaded) {
      final FitWellness wellness;
      try {
        wellness = FitRouteParser.parseWellness(
          file.bytes,
          fileName: file.entry.type.name,
        );
      } catch (_) {
        continue; // A file the decoder rejects is the importer's problem to log.
      }
      void add(GarminWellnessMetric metric, DateTime at, int value) {
        rows.add(GarminWellnessSamplesCompanion.insert(
          metric: metric.storageName,
          timeMillis: at.toUtc().millisecondsSinceEpoch,
          value: value,
        ));
      }

      // The metrics file: one snapshot, several unrelated numbers. VO2 max is
      // absent on purpose — Health Connect has a type for it, so it goes down
      // the import path with everything else it can hold.
      final metrics = wellness.metrics;
      final metricsAt = metrics?.time;
      if (metrics != null && metricsAt != null) {
        final recovery = metrics.recoveryTimeMinutes;
        if (recovery != null) {
          add(GarminWellnessMetric.recoveryTime, metricsAt, recovery);
        }
        final readiness = metrics.trainingReadiness;
        if (readiness != null) {
          add(GarminWellnessMetric.trainingReadiness, metricsAt, readiness);
        }
        final acute = metrics.trainingLoadAcute;
        if (acute != null) {
          add(GarminWellnessMetric.trainingLoadAcute, metricsAt, acute);
        }
        final chronic = metrics.trainingLoadChronic;
        if (chronic != null) {
          add(GarminWellnessMetric.trainingLoadChronic, metricsAt, chronic);
        }
      }

      // daily_sleep: the watch's own nightly summary, which arrives in the
      // METRICS file rather than the sleep file. Keyed to the night's end,
      // which is the only instant the message carries.
      final daily = wellness.dailySleep;
      final dailyAt = daily?.endTime;
      if (daily != null && dailyAt != null) {
        final score = daily.score;
        if (score != null) {
          add(GarminWellnessMetric.sleepScore, dailyAt, score);
        }
        final awake = daily.awakeDuration;
        if (awake != null) {
          add(GarminWellnessMetric.sleepAwakeSeconds, dailyAt, awake.inSeconds);
          debugPrint('[FIT-SLEEP] watch awake_duration=${awake.inMinutes}m '
              'for the night ending ${dailyAt.toIso8601String()}');
        }
        final pressure = daily.pressure;
        if (pressure != null) {
          add(GarminWellnessMetric.sleepPressure, dailyAt, pressure);
        }
      }

      // Sleep Coach.
      final demand = wellness.sleepDemand;
      final demandAt = demand?.time;
      if (demand != null && demandAt != null) {
        final normal = demand.normal;
        if (normal != null) {
          add(GarminWellnessMetric.sleepNeedNormalMinutes, demandAt,
              normal.inMinutes);
        }
        final needed = demand.demand;
        if (needed != null) {
          add(GarminWellnessMetric.sleepNeedMinutes, demandAt,
              needed.inMinutes);
        }
      }

      // The watch's own verdict on a night, keyed to when the night began.
      final sleep = wellness.sleep;
      if (sleep != null) {
        final score = sleep.overallScore;
        if (score != null) {
          add(GarminWellnessMetric.sleepScore, sleep.start, score);
        }
        final awakenings = sleep.awakeningsCount;
        if (awakenings != null) {
          add(GarminWellnessMetric.sleepAwakenings, sleep.start, awakenings);
        }
      }

      // Health Snapshot stress / Body Battery. Stored under the same metrics as
      // the all-day series: they are the same quantity on the same scale, just
      // measured deliberately rather than passively, and the (metric, time) key
      // keeps them from colliding.
      final snapshot = wellness.healthSnapshot;
      if (snapshot != null) {
        for (final (at, value) in snapshot.stress) {
          add(GarminWellnessMetric.stress, at, value);
        }
        for (final (at, value) in snapshot.bodyEnergy) {
          add(GarminWellnessMetric.bodyEnergy, at, value);
        }
      }

      final monitoring = wellness.monitoring;
      if (monitoring == null) continue;
      for (final (at, value) in monitoring.stress) {
        add(GarminWellnessMetric.stress, at, value);
      }
      for (final (at, value) in monitoring.bodyEnergy) {
        add(GarminWellnessMetric.bodyEnergy, at, value);
      }
      for (final (at, value) in monitoring.moderateMinutes) {
        add(GarminWellnessMetric.moderateMinutes, at, value);
      }
      for (final (at, value) in monitoring.vigorousMinutes) {
        add(GarminWellnessMetric.vigorousMinutes, at, value);
      }
    }
    if (rows.isEmpty) return;
    await ref.read(garminWellnessDaoProvider).upsertSamples(rows);
    debugPrint('[GARMIN-SYNC] stored ${rows.length} watch-only samples');

    // Let the new Body Battery teach the Body Energy gains. Best-effort by
    // design: calibration is an enhancement, so a failure here must not fail a
    // sync whose data has already landed.
    try {
      await ref.read(fitBodyEnergyFromWatchUseCaseProvider)();
    } catch (error) {
      debugPrint('[GARMIN-SYNC] body-energy calibration skipped: $error');
    }
  }

  /// Clears the finished/failed banner so the row goes back to normal.
  void clear() => state = const GarminSyncState();

  String _describe(Object error) {
    if (error is GarminBleTransportException) return error.message;
    final text = error.toString();
    return text.isEmpty ? 'The watch could not be synced.' : text;
  }
}

final garminSyncViewModelProvider =
    NotifierProvider<GarminSyncViewModel, GarminSyncState>(
  GarminSyncViewModel.new,
);
