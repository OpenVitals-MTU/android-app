import 'dart:async';

import 'package:flutter/foundation.dart';

import 'garmin_ble_transport.dart';
import 'garmin_capabilities.dart';
import 'garmin_protobuf_transport.dart';
import 'garmin_settings_service.dart';
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

  /// How deep to follow the settings tree, and how many screens to fetch at
  /// most. Alarms sits two levels down (Settings → Clocks → Alarms), so three
  /// reaches it with room to spare without walking the whole watch.
  static const int maxSettingsDepth = 3;
  static const int maxSettingsScreens = 24;

  /// Opens the watch's settings service and fetches its root screen, printing
  /// what came back.
  ///
  /// A DIAGNOSTIC, not a feature: the settings tree is defined entirely by the
  /// watch, and the schema this app reads it with is older than the firmware
  /// sending it. Building UI on an assumed shape would produce a screen of
  /// plausible but wrong controls, so the first step is to look.
  Future<int> probeSettings({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    String language = 'en_US',
    String region = 'us',
  }) async {
    final transport = GarminBleTransport(address: address);
    final ready = Completer<void>();
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

    StreamSubscription<String>? dropSub;
    try {
      await transport.connect(onFrame: session.handleFrame);
      dropSub = transport.onDisconnected.listen(session.abort);
      session.start();
      await ready.future.timeout(const Duration(seconds: 15));

      // The watch answers a settings request under an id OF ITS OWN rather
      // than echoing ours, so the reply arrives as unsolicited traffic and has
      // to be correlated by CONTENT. Collect every settings payload and match
      // each request against the next one that fits.
      final settingsReplies = StreamController<Uint8List>.broadcast();
      session.protobuf.onUnsolicited = (payload) {
        if (GarminSettingsService.unwrap(payload) != null) {
          settingsReplies.add(payload);
        }
      };

      /// Sends a settings request and waits for the reply that ANSWERS it.
      ///
      /// Matched on the response field, not merely on "is this settings
      /// traffic": the watch emits several settings messages unprompted, and
      /// the first to arrive was a five-byte one answering nothing we had
      /// asked. Matched on content rather than id because the watch replies
      /// under an id of its own.
      Future<Uint8List?> ask(
        Uint8List request,
        String label, {
        required int responseField,
      }) async {
        // RACED, not awaited together. Both sources are tried because some
        // replies echo our request id and some arrive as the watch's own
        // traffic — but waiting for both meant every screen cost the full
        // timeout, since the id-based one never completes on this watch. Six
        // screens took three minutes when they should have taken six seconds.
        final answer = Completer<Uint8List?>();
        void offer(Uint8List? reply) {
          if (answer.isCompleted) return;
          if (reply != null &&
              GarminSettingsService.carries(reply, responseField)) {
            answer.complete(reply);
          }
        }

        final subscription = settingsReplies.stream.listen(offer);
        unawaited(session.protobuf
            .request(request,
                label: label, timeout: GarminSettingsService.replyTimeout)
            .then(offer)
            .catchError((_) {}));
        try {
          return await answer.future
              .timeout(GarminSettingsService.replyTimeout, onTimeout: () => null);
        } finally {
          await subscription.cancel();
        }
      }

      /// Fetches one screen — its layout AND the values currently behind it.
      ///
      /// Both, because they answer different questions: the definition says
      /// there is a "Repeat" row, the state says it is set to Mon–Fri. A control
      /// rendered from the definition alone cannot show what it is set to.
      Future<Uint8List?> fetchScreen(int screenId, String label) async {
        final definition = await ask(
          GarminSettingsService.screenDefinition(screenId, language: language),
          '$label definition',
          responseField: GarminSettingsService.definitionResponseField,
        );
        debugPrint('[GARMIN-SETTINGS] $label definition: '
            '${definition == null ? "none" : "${definition.length}B"}');
        if (definition != null) GarminSettingsService.describe(definition);

        final state = await ask(
          GarminSettingsService.screenState(screenId),
          '$label state',
          responseField: GarminSettingsService.stateResponseField,
        );
        debugPrint('[GARMIN-SETTINGS] $label state: '
            '${state == null ? "none" : "${state.length}B"}');
        if (state != null) GarminSettingsService.describe(state);

        return definition;
      }

      // Init is fire-and-forget: it opens the service for a locale and the
      // watch answers on a field of its own, which nothing downstream needs.
      unawaited(session.protobuf.request(
        GarminSettingsService.init(language: language, region: region),
        label: 'settings init',
        timeout: GarminSettingsService.replyTimeout,
      ));

      final root = await fetchScreen(GarminSettingsService.rootScreenId, 'root');
      if (root == null) {
        await settingsReplies.close();
        return 0;
      }

      // Walk into every subscreen the root offers, which is where alarms live —
      // the root itself carries only categories.
      // DEPTH-first from the root. Breadth-first spent the entire screen
      // budget on the top level and stopped before reaching anything at depth
      // three — an alarm's own screen is exactly there (Settings → Clocks →
      // Alarms → the alarm), so the interesting leaves were always the ones cut
      // off. Going deep first reaches one in three hops.
      //
      // Bounded three ways, because the tree is the WATCH's and nothing here
      // knows how deep or wide it goes: a visited set (screens are reachable
      // from more than one parent, and a cycle would loop forever), a depth
      // limit, and a total cap. Each screen costs a round trip.
      final visited = <int>{GarminSettingsService.rootScreenId};

      Future<void> walk(GarminSettingsSubscreen entry, int depth) async {
        if (depth > maxSettingsDepth) return;
        if (visited.length >= maxSettingsScreens) return;
        if (!visited.add(entry.screenId)) return;
        final label = '[$depth] ${entry.screenId} '
            '(${entry.title ?? "untitled"})';
        final screen = await fetchScreen(entry.screenId, label);
        if (screen == null) return;
        for (final child in GarminSettingsService.subscreens(screen)) {
          await walk(child, depth + 1);
        }
      }

      for (final entry in GarminSettingsService.subscreens(root)) {
        await walk(entry, 1);
        if (visited.length >= maxSettingsScreens) {
          debugPrint('[GARMIN-SETTINGS] stopping at $maxSettingsScreens '
              'screens — the rest of the tree is not walked');
          break;
        }
      }

      await settingsReplies.close();
      debugPrint('[GARMIN-SETTINGS] walked ${visited.length} screens');
      return visited.length;
    } on TimeoutException {
      debugPrint('[GARMIN-SETTINGS] the watch never finished its handshake');
      return 0;
    } catch (error) {
      debugPrint('[GARMIN-SETTINGS] failed: $error');
      return 0;
    } finally {
      await dropSub?.cancel();
      session.protobuf.abort();
      await transport.close();
      debugPrint('[GARMIN-SETTINGS] link closed');
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
