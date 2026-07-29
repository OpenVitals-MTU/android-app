import 'dart:async';

import 'garmin_log.dart';
import 'garmin_notification_link.dart';
import 'garmin_notification_messages.dart';
import 'garmin_radio_lease.dart';

/// Holds one link to the watch open for as long as the watch is in range, and
/// announces notifications over it.
///
/// **Why it is held rather than opened per notification.** The first version of
/// this closed the link after twenty idle seconds, and a real vívoactive 5 made
/// the cost obvious: the watch spent most of its life disconnected and said so
/// on the wrist ("reconnect to phone to refresh data"), and — worse — a link
/// re-opened a few seconds after the last one closed sometimes never
/// re-subscribed at all, silently losing the notification it had been opened
/// for. A Garmin watch is built to have a continuously connected phone; Garmin
/// Connect and Gadgetbridge both give it one.
///
/// So the only timing left is about recovery, not about closing:
///
/// * [coalesceWindow] — a burst arriving before the link is up shares one
///   connect. Bounded by [maxCoalesceWait] so a steady drip cannot postpone it
///   forever.
/// * [reconnectBackoff] — how long to wait after the watch walks out of range
///   before trying again, doubling up to [maxReconnectBackoff]. A watch left at
///   home must not be retried in a tight loop all day.
///
/// **Yielding.** Holding the radio indefinitely would block the sync, find and
/// settings paths, which are things the user actively asked for. The lease is
/// renewed on a timer, and a renewal that fails means somebody has asked for the
/// radio: the link is dropped at once and re-established after
/// [yieldRetryDelay].
///
/// Every timer is injectable so the whole thing is tested under `fakeAsync` with
/// no radio and no real time.
class GarminNotificationForwarder {
  GarminNotificationForwarder({
    required this.address,
    required this.phoneName,
    required this.manufacturer,
    required this.model,
    required this.lease,
    this.openLink = GarminNotificationLink.open,
    this.coalesceWindow = const Duration(milliseconds: 1500),
    this.maxCoalesceWait = const Duration(seconds: 4),
    this.reconnectBackoff = const Duration(seconds: 15),
    this.maxReconnectBackoff = const Duration(minutes: 5),
    this.yieldRetryDelay = const Duration(seconds: 20),
    this.busyRetry = const Duration(seconds: 10),
    this.onIdle,
    this.onAction,
  });

  final String address;
  final String phoneName;
  final String manufacturer;
  final String model;

  /// Guards the radio against the app's own isolate. See [GarminRadioLease].
  final GarminRadioLease lease;

  /// Invoked when the wearer acts on a notification from the wrist. Handed to
  /// every link this forwarder opens.
  final Future<void> Function(GarminNotificationActionRequest request)? onAction;

  /// Injected so tests drive the state machine with no Bluetooth.
  final Future<GarminNotificationLink> Function({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Duration handshakeTimeout,
    Future<void> Function(GarminNotificationActionRequest request)? onAction,
  }) openLink;

  final Duration coalesceWindow;
  final Duration maxCoalesceWait;

  /// First delay after the watch goes out of range. Doubles per failed attempt
  /// up to [maxReconnectBackoff].
  final Duration reconnectBackoff;
  final Duration maxReconnectBackoff;

  /// How long to stay off the radio after yielding it to a sync or a find,
  /// so the handover is not immediately fought over.
  final Duration yieldRetryDelay;

  /// How long to wait before retrying when the radio is held by something else.
  final Duration busyRetry;

  /// Called when the forwarder has given up entirely — no link, nothing queued,
  /// and no reconnect pending. With a held link this is reached only on dispose
  /// or when there is no watch to reach at all.
  final void Function()? onIdle;

  /// Announcements waiting for a link. Bounded by the native buffer upstream.
  final List<_Pending> _queue = [];

  GarminNotificationLink? _link;
  StreamSubscription<void>? _activitySub;
  StreamSubscription<void>? _goneSub;

  Timer? _coalesceTimer;
  Timer? _coalesceDeadline;
  Timer? _retryTimer;
  Timer? _renewTimer;

  bool _connecting = false;
  bool _disposed = false;

  /// Grows while the watch cannot be reached, so a watch left at home is not
  /// retried in a tight loop all day. Reset the moment a link opens.
  Duration _backoff = Duration.zero;

  /// Whether a link is currently open. Diagnostic and test-facing.
  bool get isLinkOpen => _link?.isOpen ?? false;

  /// Queues [notification] for the watch.
  void post(GarminNotification notification) =>
      _enqueue(_Pending.post(notification));

  /// Queues a withdrawal for a notification the phone has dismissed.
  void withdraw(int notificationId) =>
      _enqueue(_Pending.withdraw(notificationId));

  void _enqueue(_Pending item) {
    if (_disposed) return;
    _queue.add(item);

    final link = _link;
    if (link != null && link.isOpen) {
      // The link is already up — the normal case now that it is held — so there
      // is nothing to coalesce.
      unawaited(_drain());
      return;
    }
    if (_connecting || _retryTimer != null) return;

    _coalesceTimer?.cancel();
    _coalesceTimer = Timer(coalesceWindow, _openAndDrain);
    // First arrival starts the ceiling; later ones must not push it out.
    _coalesceDeadline ??= Timer(maxCoalesceWait, () {
      _coalesceTimer?.cancel();
      _openAndDrain();
    });
  }

  Future<void> _openAndDrain() async {
    _coalesceTimer?.cancel();
    _coalesceTimer = null;
    _coalesceDeadline?.cancel();
    _coalesceDeadline = null;
    if (_disposed || _connecting || _queue.isEmpty) return;
    if (_link?.isOpen ?? false) {
      await _drain();
      return;
    }

    _connecting = true;
    try {
      if (!await lease.acquire(address, GarminRadioOwner.notifications)) {
        // A user-initiated sync or find owns the radio. Notifications wait —
        // this is the right priority, and it is safe precisely because GNCS is
        // pull-based: the watch asks again once we can answer.
        final holder = await lease.owner(address);
        garminLog('[GARMIN-NOTIFY] radio held by ${holder ?? "another task"}; '
            'retrying in ${busyRetry.inSeconds}s');
        _scheduleRetry();
        return;
      }

      garminLog('[GARMIN-NOTIFY] opening a link for ${_queue.length} '
          'notification(s)');
      final link = await openLink(
        address: address,
        phoneName: phoneName,
        manufacturer: manufacturer,
        model: model,
        onAction: onAction,
      );
      if (_disposed) {
        await link.close();
        await lease.release(address, GarminRadioOwner.notifications);
        return;
      }
      _link = link;
      _backoff = Duration.zero;
      _goneSub = link.onGone.listen((_) {
        // Says what was OBSERVED, not what it was assumed to mean. The transport
        // reports a disconnect; whether the watch walked away, the radio was
        // reset, or power management dropped a background link is not knowable
        // from here, and guessing in the log sent one investigation down the
        // wrong path.
        garminLog('[GARMIN-NOTIFY] the transport reported a disconnect; '
            'will reconnect');
        unawaited(_closeLink(reconnect: true));
      });
      _startRenewals();
      garminLog('[GARMIN-NOTIFY] link held open');
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not open a link: $error');
      await lease.release(address, GarminRadioOwner.notifications);
      // The queue is KEPT. With a held link the watch is normally reachable, so
      // a failure here means it is momentarily away — and the notification the
      // link was opened for should go out when it comes back, not be discarded.
      _connecting = false;
      _scheduleReconnect();
      return;
    } finally {
      _connecting = false;
    }

    await _drain();
  }

  /// Renews the lease, and gives the radio up the moment somebody else asks.
  ///
  /// This is what stops a permanently held link from blocking the things the
  /// user actively initiates. A failed renewal means either the lease expired
  /// (it should not have) or a sync/find/settings action has requested it; both
  /// mean stop.
  void _startRenewals() {
    _renewTimer?.cancel();
    _renewTimer = Timer.periodic(GarminRadioLease.renewInterval, (_) async {
      if (_disposed || _link == null) return;
      if (await lease.renew(address, GarminRadioOwner.notifications)) return;
      garminLog('[GARMIN-NOTIFY] the radio was requested by something the user '
          'started; yielding');
      await _closeLink(reconnect: true, after: yieldRetryDelay);
    });
  }

  void _scheduleRetry() {
    _retryTimer?.cancel();
    _retryTimer = Timer(busyRetry, () {
      _retryTimer = null;
      unawaited(_openAndDrain());
    });
  }

  /// Re-establishes the link after [after], or after a growing backoff.
  void _scheduleReconnect({Duration? after}) {
    if (_disposed) return;
    _retryTimer?.cancel();
    final delay = after ?? _nextBackoff();
    garminLog('[GARMIN-NOTIFY] reconnecting in ${delay.inSeconds}s');
    _retryTimer = Timer(delay, () {
      _retryTimer = null;
      unawaited(_reopen());
    });
  }

  Duration _nextBackoff() {
    if (_backoff == Duration.zero) {
      _backoff = reconnectBackoff;
    } else {
      final doubled = _backoff * 2;
      _backoff = doubled > maxReconnectBackoff ? maxReconnectBackoff : doubled;
    }
    return _backoff;
  }

  /// Opens the link again whether or not anything is queued — the point of
  /// holding it is that the watch stays connected between notifications.
  Future<void> _reopen() async {
    if (_disposed || _connecting) return;
    if (_link?.isOpen ?? false) return;
    _queue.isEmpty ? await _openHeld() : await _openAndDrain();
  }

  /// The no-traffic path into [_openAndDrain], which otherwise returns early on
  /// an empty queue.
  Future<void> _openHeld() async {
    _queue.add(_Pending.keepAlive());
    await _openAndDrain();
  }

  Future<void> _drain() async {
    final link = _link;
    if (link == null || !link.isOpen) return;
    while (_queue.isNotEmpty) {
      final item = _queue.removeAt(0);
      if (item.isKeepAlive) continue;
      try {
        if (item.notification != null) {
          await link.push(item.notification!);
        } else {
          await link.withdraw(item.withdrawId!);
        }
      } catch (error) {
        garminLog('[GARMIN-NOTIFY] failed to announce: $error');
      }
    }
  }

  /// Drops the link, optionally re-establishing it.
  ///
  /// [reconnect] false is only for [dispose]: while the feature is on, a link
  /// that ends for any other reason is meant to come back.
  Future<void> _closeLink({bool reconnect = false, Duration? after}) async {
    final link = _link;
    _link = null;

    // Take back anything the link accepted but never announced, so it survives
    // into the next one. A handler lives and dies with its link, and the
    // forwarder has already dropped these from its own queue — so without this
    // a watch that walks away between "queued" and "subscribed" loses exactly
    // the notification the link was opened for, which is the failure this whole
    // held-link design exists to remove.
    if (link != null) {
      final unsent = link.handler.held.toList();
      if (unsent.isNotEmpty) {
        garminLog('[GARMIN-NOTIFY] re-queueing ${unsent.length} notification(s) '
            'the watch never subscribed for');
        _queue.insertAll(0, unsent.map(_Pending.post));
      }
    }
    _renewTimer?.cancel();
    _renewTimer = null;
    // NOT awaited. Cancelling a subscription to a controller that is about to be
    // closed is housekeeping, and blocking the teardown on it means the link and
    // the lease are still held while it settles — which under `fakeAsync` never
    // happens at all, because a broadcast subscription's cancel future does not
    // complete there.
    final activitySub = _activitySub;
    final goneSub = _goneSub;
    _activitySub = null;
    _goneSub = null;
    if (activitySub != null) unawaited(activitySub.cancel());
    if (goneSub != null) unawaited(goneSub.cancel());
    if (link != null) await link.close();
    await lease.release(address, GarminRadioOwner.notifications);

    if (!reconnect || _disposed) {
      _finishIfIdle();
      return;
    }
    _scheduleReconnect(after: after);
  }

  void _finishIfIdle() {
    if (_disposed) return;
    if (_link != null || _connecting) return;
    if (_retryTimer != null) return;
    onIdle?.call();
  }

  /// Closes everything. Idempotent; the forwarder is unusable afterwards.
  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;
    _coalesceTimer?.cancel();
    _coalesceDeadline?.cancel();
    _retryTimer?.cancel();
    _retryTimer = null;
    _renewTimer?.cancel();
    _queue.clear();
    await _closeLink();
  }
}

/// One queued instruction: announce a notification, or withdraw one.
class _Pending {
  _Pending.post(this.notification)
      : withdrawId = null,
        isKeepAlive = false;

  _Pending.withdraw(int id)
      : notification = null,
        withdrawId = id,
        isKeepAlive = false;

  /// Carries nothing. It exists so the connect path, which returns early on an
  /// empty queue, can be used to re-establish a link the user wants held even
  /// when there is no notification waiting.
  _Pending.keepAlive()
      : notification = null,
        withdrawId = null,
        isKeepAlive = true;

  final GarminNotification? notification;
  final int? withdrawId;
  final bool isKeepAlive;
}
