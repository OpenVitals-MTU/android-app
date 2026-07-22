import 'dart:async';

import 'package:flutter/foundation.dart';

import 'garmin_ble_transport.dart';
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
      return await session.done;
    } finally {
      await dropSub?.cancel();
      await transport.close();
      debugPrint('[GARMIN-SYNC] link closed');
    }
  }
}
