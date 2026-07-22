import 'dart:async';

import 'package:flutter/foundation.dart';

import 'garmin_ble_transport.dart';
import 'garmin_capabilities.dart';
import 'garmin_protobuf_transport.dart';
import 'garmin_file_store.dart';
import 'garmin_session.dart';

/// Drives one end-to-end sync with a watch: open the link, run the GFDI
/// session, hand back whatever it downloaded.
///
/// The seam between the protocol stack and the app. Everything below it is
/// radio-free and unit-tested; everything above it deals in FIT files and
/// Health Connect and knows nothing about handles or COBS.
///
/// Deliberately does NOT import: that is the caller's job, so the downloaded
/// bytes flow into the SAME `RouteBulkImportViewModel.importRouteFiles` path a
/// hand-picked folder uses — batching, per-call quota handling and per-file
/// error tolerance included — rather than growing a second write path that
/// would drift from it.
class GarminWatchSyncService {
  const GarminWatchSyncService({this.fileStore});

  /// Keeps a copy of every download before the watch is told to archive it.
  ///
  /// Null disables that safety net, which only makes sense in tests: without it,
  /// an importer bug means the data is unrecoverable, since archiving stops the
  /// watch ever offering the file again.
  final GarminFileStore? fileStore;

  /// Makes the watch at [address] alert, and keeps the link open while it does.
  ///
  /// Find is a TOGGLE with a timeout, not a one-shot: the watch alerts for
  /// [timeout] unless cancelled, so the link has to stay open for the duration —
  /// a sync closes it in about a second, which would end the alert with it.
  /// Completing [cancelled] stops the alert early.
  ///
  /// Returns whether the watch ACCEPTED the request. That is not the same as
  /// "the watch is ringing": the protocol answers OK (100) or ERROR (200), and
  /// only the first means anything happened.
  Future<bool> findWatch({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Duration timeout = GarminFindMyWatch.defaultTimeout,
    Future<void>? cancelled,
  }) async {
    final transport = GarminBleTransport(address: address);
    final ready = Completer<void>();
    // The watch narrates a find it has ended itself — dismissed on the wrist,
    // or run out. Without listening for that, the phone holds "Stop" for the
    // full minute after the alert has already stopped, which is what a real
    // wearer hit first.
    final endedOnWatch = Completer<void>();
    final session = GarminSession(
      send: (frame) => transport.mlOrThrow.sendFrame(frame),
      bluetoothName: phoneName,
      manufacturer: manufacturer,
      model: model,
      syncFiles: false,
      onHandshakeReady: () {
        if (!ready.isCompleted) ready.complete();
      },
    );
    session.protobuf.onUnsolicited = (payload) {
      if (!GarminFindMyWatch.isFindMessage(payload)) return;
      debugPrint('[GARMIN-FIND] the watch says the alert ended');
      if (!endedOnWatch.isCompleted) endedOnWatch.complete();
    };

    StreamSubscription<String>? dropSub;
    var ringing = false;
    try {
      await transport.connect(onFrame: session.handleFrame);
      dropSub = transport.onDisconnected.listen(session.abort);
      session.start();
      // The watch ignores anything sent before it has finished introducing
      // itself, so wait for the handshake rather than racing it.
      await ready.future.timeout(const Duration(seconds: 15));

      final reply = await session.protobuf.request(
        GarminFindMyWatch.start(timeout: timeout),
        label: 'find start',
      );
      final outcome = GarminFindMyWatch.outcome(reply);
      debugPrint('[GARMIN-FIND] ${outcome.name}');
      // Only an explicit ERROR is a refusal. A reply this app cannot read is
      // NOT: the watch was seen ringing while an unparsed reply was being
      // treated as failure, and bailing here is what left it ringing with no
      // way to stop it.
      if (outcome.declined) return false;

      ringing = true;
      // Hold the link for the alert, or until the user stops it.
      await Future.any([
        Future<void>.delayed(timeout),
        endedOnWatch.future,
        ?cancelled,
      ]);
      return true;
    } on TimeoutException {
      debugPrint('[GARMIN-FIND] the watch never finished its handshake');
      return false;
    } finally {
      // ALWAYS cancel a started alert, on every path out — including a thrown
      // error and a user who backed out. A buzzing watch that the phone has
      // forgotten about is the one outcome worth writing code to prevent.
      if (ringing) {
        try {
          await session.protobuf
              .request(GarminFindMyWatch.cancel(), label: 'find cancel')
              .timeout(const Duration(seconds: 3));
        } catch (error) {
          debugPrint('[GARMIN-FIND] could not cancel: $error');
        }
      }
      await dropSub?.cancel();
      session.protobuf.abort();
      await transport.close();
      debugPrint('[GARMIN-FIND] link closed');
    }
  }

  /// Syncs the watch at [address].
  ///
  /// [alreadySynced] are dedup keys a previous run pulled; [phoneName],
  /// [manufacturer] and [model] identify this phone in the GFDI handshake.
  ///
  /// Throws [GarminBleTransportException] when the watch cannot be reached or
  /// speaks a transport this app does not implement. A link that drops
  /// mid-sync is NOT an error: the session aborts and returns what it already
  /// has, because a night of sleep already on the phone should not be thrown
  /// away because the user walked out of range.
  Future<List<GarminDownloadedFile>> sync({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Set<String> alreadySynced = const {},
    void Function(GarminSyncProgress)? onProgress,
    Duration listenAfter = Duration.zero,
    void Function(Set<GarminCapability>)? onCapabilities,
  }) async {
    final transport = GarminBleTransport(address: address);
    late final GarminSession session;

    session = GarminSession(
      // `send` is bound after the transport opens; the closure defers the
      // lookup so the session can be constructed first and wired in one place.
      send: (frame) => transport.mlOrThrow.sendFrame(frame),
      bluetoothName: phoneName,
      manufacturer: manufacturer,
      model: model,
      alreadySynced: alreadySynced,
      onProgress: onProgress,
      onFileDownloaded: fileStore == null
          ? null
          : (file) => fileStore!.save(file, now: DateTime.now()),
      keepAnsweringAfterSync: listenAfter > Duration.zero,
    );

    // Housekeeping before the link opens, so it cannot delay the sync itself.
    await fileStore?.prune(now: DateTime.now());

    StreamSubscription<String>? dropSub;
    try {
      await transport.connect(onFrame: session.handleFrame);
      // A dropped link ends the sync with what it has rather than hanging on
      // `done` forever waiting for frames that will never arrive.
      dropSub = transport.onDisconnected.listen(session.abort);
      session.start();
      final files = await session.done;
      onCapabilities?.call(session.capabilities);
      if (listenAfter > Duration.zero) {
        // Diagnostic pass: the sync itself takes about a second, so holding the
        // link open is the only way to see what the watch sends unprompted.
        // Whatever arrives is logged by the session; the files are returned and
        // imported as usual once the window closes.
        debugPrint('[GARMIN-LISTEN] holding the link open for '
            '${listenAfter.inMinutes}m — touch the watch now');
        await Future<void>.delayed(listenAfter);
        debugPrint('[GARMIN-LISTEN] window closed');
      }
      return files;
    } finally {
      await dropSub?.cancel();
      await transport.close();
      debugPrint('[GARMIN-SYNC] link closed');
    }
  }
}
