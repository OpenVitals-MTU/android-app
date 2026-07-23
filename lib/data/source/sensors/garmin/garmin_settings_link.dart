import 'dart:async';

import 'package:flutter/foundation.dart';

import 'garmin_ble_transport.dart';
import 'garmin_log.dart';
import 'garmin_session.dart';
import 'garmin_settings_model.dart';
import 'garmin_settings_service.dart';

/// An OPEN conversation with the watch's settings service.
///
/// Held open on purpose. The file sync connects, works and closes in about a
/// second; browsing settings is the opposite shape — a person reads a screen,
/// decides, taps, reads the result — and reconnecting per request would cost a
/// handshake every time and lose the watch's own idea of where it is in the
/// tree.
///
/// Everything here is correlated by CONTENT rather than request id: the watch
/// answers a settings request under an id of its own, so a reply is
/// indistinguishable from traffic it started, and matching on "is this settings
/// traffic" is not enough either — several arrive unprompted, and the first one
/// seen answered nothing that had been asked.
class GarminSettingsLink {
  GarminSettingsLink._(this._transport, this._session);

  final GarminBleTransport _transport;
  final GarminSession _session;

  final StreamController<Uint8List> _replies =
      StreamController<Uint8List>.broadcast();

  bool _closed = false;

  /// Fires the moment the link goes away, so a request in flight fails at once
  /// instead of waiting out a timeout for a reply that can never arrive. A
  /// dropped watch left the screen saying "Reading from the watch…" for thirty
  /// seconds with nothing on the other end.
  ///
  /// A broadcast stream rather than a completer so a request can stop listening
  /// when it finishes. A completer cannot: every request registered a callback
  /// that lived as long as the link, and a long browse accumulated one per
  /// screen read.
  final StreamController<void> _gone = StreamController<void>.broadcast();

  /// Whether the link is still usable. A watch that walks away closes it, and
  /// every later request would otherwise wait out its full timeout.
  bool get isOpen => !_closed;

  /// Connects, completes the handshake, and opens the settings service for a
  /// locale.
  ///
  /// The locale is not cosmetic: the watch translates every title it later
  /// sends using it, so this decides what language the whole tree comes back in.
  static Future<GarminSettingsLink> open({
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
      // Nothing to collect: this link exists to read settings, and a file sync
      // running underneath would fight it for the radio and die when it closes.
      syncFiles: false,
      onHandshakeReady: () {
        if (!ready.isCompleted) ready.complete();
      },
    );

    try {
      await transport.connect(onFrame: session.handleFrame);
      session.start();
      // Anything sent before the watch finishes introducing itself is dropped.
      await ready.future.timeout(const Duration(seconds: 15));
    } catch (error) {
      // Nothing is listening yet, so the transport is the only thing to undo.
      await transport.close();
      rethrow;
    }

    final link = GarminSettingsLink._(transport, session);
    session.protobuf.onUnsolicited = (payload) {
      if (GarminSettingsService.unwrap(payload) != null) {
        if (!link._replies.isClosed) link._replies.add(payload);
      }
    };
    link._dropListener = transport.onDisconnected.listen((reason) {
      garminLog('[GARMIN-SETTINGS] link dropped: $reason');
      session.abort(reason);
      link._closed = true;
      link._signalGone();
    });

    unawaited(link._ask(
      GarminSettingsService.init(language: language, region: region),
      'init',
      // The init reply lands on a field of its own that nothing downstream
      // needs; it is sent to open the service, not to be read.
      responseField: GarminSettingsService.definitionResponseField,
    ));
    return link;
  }

  StreamSubscription<String>? _dropListener;

  /// Fetches one screen: its layout AND the values currently behind it.
  ///
  /// Both, because they answer different questions — the definition says there
  /// is a "Repeat" row, the state says it is set to Weekday. A screen built from
  /// the definition alone cannot show what anything is set to, and a switch has
  /// no value at all outside the state.
  Future<GarminSettingsScreen?> screen(int screenId) async {
    final definition = await _ask(
      GarminSettingsService.screenDefinition(screenId),
      'screen $screenId definition',
      responseField: GarminSettingsService.definitionResponseField,
      expectScreen: screenId,
    );
    if (definition == null) return null;

    // Asked twice if need be. The state is what makes a switch a switch — a
    // screen without it renders every toggle inert — and a single dropped reply
    // was enough to leave an alarm looking uncontrollable. One retry costs a
    // round trip; getting it wrong costs the whole point of the screen.
    var state = await _ask(
      GarminSettingsService.screenState(screenId),
      'screen $screenId state',
      responseField: GarminSettingsService.stateResponseField,
      expectScreen: screenId,
    );
    if (state == null && isOpen) {
      garminLog('[GARMIN-SETTINGS] no state for $screenId, asking again');
      state = await _ask(
        GarminSettingsService.screenState(screenId),
        'screen $screenId state (retry)',
        responseField: GarminSettingsService.stateResponseField,
        expectScreen: screenId,
      );
    }
    if (state == null) {
      garminLog('[GARMIN-SETTINGS] screen $screenId has no state — every '
          'switch on it will render inert');
    }
    final screen = parseGarminSettingsScreen(definition, stateReply: state);
    if (screen != null) {
      // Every row as parsed, not as raw bytes: a long hex dump is truncated by
      // logcat, and what matters when a control is missing is which KIND each
      // row came out as.
      garminLog('[GARMIN-SETTINGS] screen $screenId "${screen.title}" '
          '${screen.entries.length} rows, state=${screen.hasState}');
      for (final entry in screen.entries) {
        if (entry.isBlank) continue;
        garminLog('[GARMIN-SETTINGS]   ${entry.id}: ${entry.kind.name} '
            '"${entry.title}" summary="${entry.summary}" '
            'sub=${entry.subscreenId} options=${entry.options.length} '
            'targetType=${entry.rawTargetType}');
      }
    }
    return screen;
  }

  /// Flips a switch on the watch, and reports whether the watch agreed.
  ///
  /// The FIRST write in this stack — everything else reads. Returns null when
  /// the watch did not answer at all, which is deliberately distinct from
  /// false: "it refused" and "it never heard" call for different words on
  /// screen, and collapsing them would report a lost message as a rejection.
  Future<bool?> setSwitch({
    required int screenId,
    required int entryId,
    required bool value,
  }) async {
    garminLog('[GARMIN-SETTINGS] → switch $screenId/$entryId = $value');
    final reply = await _ask(
      GarminSettingsService.changeSwitch(
        screenId: screenId,
        entryId: entryId,
        value: value,
      ),
      'change switch',
      responseField: GarminSettingsService.changeResponseField,
      expectScreen: screenId,
    );
    final ok = GarminSettingsService.changeSucceeded(reply);
    garminLog('[GARMIN-SETTINGS] ← switch ${ok ?? "no answer"}');
    return ok;
  }

  /// Chooses one of the options the watch supplied for an entry.
  Future<bool?> setOption({
    required int screenId,
    required int entryId,
    required int index,
  }) async {
    final reply = await _ask(
      GarminSettingsService.changeOption(
        screenId: screenId,
        entryId: entryId,
        index: index,
      ),
      'change option',
      responseField: GarminSettingsService.changeResponseField,
      expectScreen: screenId,
    );
    return GarminSettingsService.changeSucceeded(reply);
  }

  /// Activates a delete row. The answer matters more here than anywhere else:
  /// this is the one operation that cannot be undone.
  Future<bool?> delete({required int screenId, required int entryId}) async {
    garminLog('[GARMIN-SETTINGS] → delete $screenId/$entryId');
    final reply = await _ask(
      GarminSettingsService.changeDelete(screenId: screenId, entryId: entryId),
      'delete',
      responseField: GarminSettingsService.changeResponseField,
      expectScreen: screenId,
    );
    final ok = GarminSettingsService.changeSucceeded(reply);
    garminLog('[GARMIN-SETTINGS] ← delete ${ok ?? "no answer"}');
    return ok;
  }

  /// Sets a time of day.
  Future<bool?> setTime({
    required int screenId,
    required int entryId,
    required Duration sinceMidnight,
  }) async {
    final reply = await _ask(
      GarminSettingsService.changeTime(
        screenId: screenId,
        entryId: entryId,
        sinceMidnight: sinceMidnight,
      ),
      'change time',
      responseField: GarminSettingsService.changeResponseField,
      expectScreen: screenId,
    );
    return GarminSettingsService.changeSucceeded(reply);
  }

  Future<void> close() async {
    if (_closed) return;
    _closed = true;
    _signalGone();
    await _dropListener?.cancel();
    await _replies.close();
    _session.protobuf.abort();
    await _transport.close();
    garminLog('[GARMIN-SETTINGS] link closed');
  }

  /// Sends a request and waits for the reply that ANSWERS it.
  ///
  /// Both sources are RACED rather than awaited together: some replies echo the
  /// request id and some arrive as the watch's own traffic, and waiting for both
  /// made every screen cost the full timeout, because the id-based one never
  /// completes on this watch.
  /// [expectScreen] is the screen the reply must be ABOUT. Without it, a
  /// retransmitted definition for some other screen satisfies the wait, and the
  /// caller pairs one screen's layout with another's values — an alarm list
  /// whose titles came from the old read and whose times came from the alarm
  /// underneath it.
  Future<Uint8List?> _ask(
    Uint8List request,
    String label, {
    required int responseField,
    int? expectScreen,
  }) async {
    if (_closed) return null;
    final answer = Completer<Uint8List?>();
    void offer(Uint8List? reply) {
      if (answer.isCompleted) return;
      if (reply == null) return;
      if (!GarminSettingsService.carries(reply, responseField)) return;
      if (expectScreen != null) {
        final about = GarminSettingsService.screenIdOf(reply, responseField);
        if (about != null && about != expectScreen) {
          garminLog('[GARMIN-SETTINGS] ignoring a reply about screen $about '
              'while waiting for $expectScreen');
          return;
        }
      }
      answer.complete(reply);
    }

    final subscription = _replies.stream.listen(offer);
    // A dropped link resolves the wait immediately — see [_gone]. Cancelled
    // alongside the reply subscription below, so a long browse does not leave a
    // listener behind per screen read.
    final goneSubscription = _gone.stream.listen((_) {
      if (!answer.isCompleted) answer.complete(null);
    });
    unawaited(_session.protobuf
        .request(request,
            label: label, timeout: GarminSettingsService.replyTimeout)
        .then(offer)
        .catchError((_) {}));
    try {
      return await answer.future.timeout(
        GarminSettingsService.replyTimeout,
        onTimeout: () => null,
      );
    } finally {
      await subscription.cancel();
      await goneSubscription.cancel();
    }
  }

  /// Tells every waiting request that nothing more is coming.
  void _signalGone() {
    if (_gone.isClosed) return;
    _gone.add(null);
    unawaited(_gone.close());
  }
}
