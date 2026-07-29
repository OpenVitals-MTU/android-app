import 'dart:async';

import 'garmin_ble_transport.dart';
import 'garmin_log.dart';
import 'garmin_notification_messages.dart';
import 'garmin_notifications_handler.dart';
import 'garmin_session.dart';

/// An OPEN conversation with the watch's notification service.
///
/// Held open on purpose, and for a reason peculiar to GNCS: it is a **pull**
/// protocol. Announcing a notification sends no text — the watch asks for the
/// words afterwards, and "afterwards" can be several seconds later, when the
/// wearer raises their wrist. Closing the link as soon as the announcement was
/// acknowledged would deliver a card the watch then renders empty.
///
/// A twin of [GarminSettingsLink] in shape: connect, finish the handshake, hold,
/// tear down once on every path out.
class GarminNotificationLink {
  GarminNotificationLink._(
    this._transport,
    this._session,
    this.handler,
    this._activity,
  );

  final GarminBleTransport _transport;
  final GarminSession _session;

  /// The queue and the chunked upload. Exposed because the forwarder announces
  /// through it.
  final GarminNotificationsHandler handler;

  bool _closed = false;

  /// Whether the link is still usable. A watch that walks away closes it.
  bool get isOpen => !_closed;

  /// Whether the watch has actually subscribed.
  ///
  /// False until it sends its subscription request, which it does about once a
  /// second — so this flips within a second of the handshake IF the wearer has
  /// notifications switched on. It staying false is the single most likely
  /// explanation for a silent watch, and it is not an error this end.
  bool get subscribed => handler.enabled;

  /// Fires when the link goes away, so a caller waiting on the watch stops
  /// waiting instead of timing out.
  Stream<void> get onGone => _gone.stream;
  final StreamController<void> _gone = StreamController<void>.broadcast();

  /// Fires on every frame in either direction, which is what the forwarder's
  /// idle timer is reset by. A watch mid-conversation must not have the link
  /// pulled out from under it.
  ///
  /// Created BEFORE the link and handed in, because the first frames arrive
  /// during the handshake — inside `transport.connect` — and therefore before
  /// there is a link to hang it off. A closure that reached for the link instead
  /// threw `LateInitializationError` on every inbound frame.
  Stream<void> get onActivity => _activity.stream;
  final StreamController<void> _activity;

  StreamSubscription<String>? _dropListener;

  /// Connects and completes the handshake.
  ///
  /// Throws [GarminBleTransportException] when the watch cannot be reached, and
  /// [TimeoutException] when it connects but never introduces itself.
  static Future<GarminNotificationLink> open({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Duration handshakeTimeout = const Duration(seconds: 15),
    Future<void> Function(GarminNotificationActionRequest request)? onAction,
  }) async {
    final transport = GarminBleTransport(address: address);
    final ready = Completer<void>();
    final activity = StreamController<void>.broadcast();

    final handler = GarminNotificationsHandler(
      send: (frame) => transport.mlOrThrow.sendFrame(frame),
      onAction: onAction,
    );
    final session = GarminSession(
      send: (frame) => transport.mlOrThrow.sendFrame(frame),
      bluetoothName: phoneName,
      manufacturer: manufacturer,
      model: model,
      // Mandatory, not incidental: this link is held for tens of seconds and
      // then closed, and a file transfer dragged along behind it would die
      // mid-flight — which can lose a file, since the watch is told to archive
      // only what was safely stored.
      syncFiles: false,
      notifications: handler,
      onHandshakeReady: () {
        if (!ready.isCompleted) ready.complete();
      },
    );

    try {
      await transport.connect(onFrame: (frame) {
        if (!activity.isClosed) activity.add(null);
        session.handleFrame(frame);
      });
      session.start();
      // Anything sent before the watch has finished introducing itself is
      // dropped on the floor.
      await ready.future.timeout(handshakeTimeout);
    } catch (error) {
      // Nothing is listening yet, so the transport is the only thing to undo.
      await activity.close();
      await transport.close();
      rethrow;
    }

    final link =
        GarminNotificationLink._(transport, session, handler, activity);
    link._dropListener = transport.onDisconnected.listen((reason) {
      garminLog('[GARMIN-NOTIFY] link dropped: $reason');
      session.abort(reason);
      link._closed = true;
      if (!link._gone.isClosed) link._gone.add(null);
    });
    return link;
  }

  void _touch() {
    if (!_activity.isClosed) _activity.add(null);
  }

  /// Announces [notification] to the watch, or withdraws it.
  ///
  /// Silently does nothing when the watch has not subscribed — see [subscribed].
  Future<void> push(GarminNotification notification) async {
    if (_closed) return;
    _touch();
    await handler.post(notification);
  }

  Future<void> withdraw(int notificationId) async {
    if (_closed) return;
    _touch();
    await handler.remove(notificationId);
  }

  /// Closes the link and releases everything. Idempotent.
  Future<void> close() async {
    if (_closed) {
      await _teardown();
      return;
    }
    _closed = true;
    await _teardown();
  }

  Future<void> _teardown() async {
    await _dropListener?.cancel();
    _dropListener = null;
    handler.reset();
    _session.abort('link closed');
    await _transport.close();
    if (!_gone.isClosed) await _gone.close();
    if (!_activity.isClosed) await _activity.close();
  }
}
