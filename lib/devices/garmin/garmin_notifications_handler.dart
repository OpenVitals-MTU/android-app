import 'dart:collection';
import 'dart:typed_data';

import 'garmin_crc.dart';
import 'garmin_log.dart';
import 'garmin_messages.dart';
import 'garmin_notification_messages.dart';

/// Holds the notifications the watch might ask about, announces new ones, and
/// serves the attribute blobs it requests.
///
/// **Transport-free by construction**, exactly like [GarminSession]: it is
/// handed a `send` callback that takes a built GFDI frame, so the whole
/// announce → request → chunked answer conversation is exercised over an
/// in-memory pipe with no Bluetooth.
///
/// The shape of this class is dictated by GNCS being a **pull** protocol.
/// [post] does not send a notification — it sends word that one exists. The text
/// leaves the phone only if and when the watch asks, which may be seconds later
/// (when the wearer raises their wrist), may happen twice (again with a larger
/// length limit when they scroll into the body), or may never happen at all.
/// That last case is not an error and nothing here treats it as one.
///
/// Ported from Gadgetbridge's `NotificationsHandler` (AGPLv3), with two
/// deliberate deviations noted at [handleDataStatus].
class GarminNotificationsHandler {
  GarminNotificationsHandler({
    required this.send,
    this.onAction,
    this.maxQueued = 10,
  });

  /// Hands one built GFDI frame to the transport below.
  final Future<void> Function(Uint8List frame) send;

  /// Invoked when the wearer acts on a notification.
  ///
  /// The handler deliberately does not perform anything itself: firing an
  /// action means talking to Android, and this class is transport-free and
  /// platform-free so the whole conversation stays testable over an in-memory
  /// pipe.
  final Future<void> Function(GarminNotificationActionRequest request)? onAction;

  /// How many notifications stay answerable.
  ///
  /// Gadgetbridge's number. The queue exists because the watch asks about a
  /// notification by id long after it was announced, so a notification that has
  /// fallen out simply cannot be answered — the watch renders it blank. Ten is
  /// what a wrist realistically has on screen; more would only hold text in
  /// memory that nobody will ask for.
  final int maxQueued;

  /// Whether the watch has subscribed. Until it has, everything here is a no-op:
  /// announcing to a watch that has not asked to be told is how a sync session
  /// ends up sending notification traffic it never meant to.
  bool get enabled => _enabled;
  bool _enabled = false;

  /// Records whether the watch is accepting notifications.
  ///
  /// Deliberately does NOT announce anything — see [flushHeld]. The caller has
  /// to answer the watch's subscription message before sending it anything else.
  void setEnabled({required bool enabled}) {
    if (_enabled == enabled) return;
    _enabled = enabled;
    garminLog('[GARMIN-NOTIFY] forwarding ${enabled ? "enabled" : "disabled"}');
    if (!enabled) _upload = null;
  }

  /// Announces everything that arrived before the watch subscribed.
  ///
  /// Load-bearing, not a nicety. This app OPENS the link in order to announce
  /// something, so the announcement is always ready before the watch has got
  /// round to subscribing — the subscription lands a couple of hundred
  /// milliseconds after the handshake. Dropping what arrived in that window
  /// meant the very notification the link was opened for was the one lost.
  ///
  /// Called AFTER the subscription status has gone out, never before. Garmin's
  /// own ordering is status-for-the-inbound-message first and follow-up second,
  /// and announcing ahead of that status means announcing to a watch that has
  /// not yet been told its subscription was accepted.
  Future<void> flushHeld() async {
    if (!_enabled) return;
    final waiting = List<GarminNotification>.of(_awaitingSubscription);
    _awaitingSubscription.clear();
    for (final notification in waiting) {
      // Skip anything evicted from the answerable queue while it waited: the
      // watch could ask about it and we would have nothing to answer with.
      if (_find(notification.id) == null) continue;
      await _announce(notification, isUpdate: false);
    }
  }

  /// Oldest first, so [_evict] drops the one least likely to be asked about.
  final Queue<GarminNotification> _queue = Queue<GarminNotification>();

  /// Announced the moment the watch subscribes. See [setEnabled].
  final List<GarminNotification> _awaitingSubscription = [];

  _NotificationUpload? _upload;

  /// The notifications still answerable, oldest first. Diagnostic only.
  Iterable<GarminNotification> get queued => List.unmodifiable(_queue);

  /// Announcements this handler accepted but never got to send, because the
  /// watch had not subscribed.
  ///
  /// A handler lives and dies with one link, so when the watch walks out of
  /// range these would vanish with it — losing exactly the notification the link
  /// was opened for. The forwarder takes them back and re-queues them for the
  /// next link.
  Iterable<GarminNotification> get held =>
      List.unmodifiable(_awaitingSubscription);

  /// Announces [notification] to the watch.
  ///
  /// An id already in the queue is announced as MODIFY rather than ADD — the
  /// watch updates the one it is showing instead of buzzing a second time,
  /// which is what a progress notification or an edited message needs.
  Future<void> post(GarminNotification notification) async {
    final isUpdate = _remove(notification.id);
    _queue.addLast(notification);
    _evict();

    if (!_enabled) {
      // Held, not dropped — see [setEnabled]. Logged because "the watch has not
      // subscribed" is the most likely reason nothing reaches the wrist, and it
      // is not a fault at this end: notifications are switched off ON THE WATCH.
      _awaitingSubscription
        ..removeWhere((queued) => queued.id == notification.id)
        ..add(notification);
      garminLog('[GARMIN-NOTIFY] the watch has not subscribed yet; holding '
          'notification ${notification.id}');
      return;
    }
    await _announce(notification, isUpdate: isUpdate);
  }

  Future<void> _announce(
    GarminNotification notification, {
    required bool isUpdate,
  }) async {
    // Logged because an announcement carries no text and produces no visible
    // effect on its own: whether one went out, and whether the watch then asked
    // about it, is the only way to tell "the watch never heard" from "the watch
    // heard and did not care".
    garminLog('[GARMIN-NOTIFY] announcing ${notification.id} '
        '(${isUpdate ? "modify" : "add"}, ${notification.category.name})');
    await send(buildNotificationUpdate(
      updateType: isUpdate
          ? GarminNotificationUpdateType.modify
          : GarminNotificationUpdateType.add,
      category: notification.category,
      count: _countOf(notification.category),
      notificationId: notification.id,
      // Without this the watch draws no action controls at all, however many
      // the ACTIONS attribute later offers — the announcement is where it
      // decides whether to ask.
      hasActions: notification.hasActions,
    ));
  }

  /// Withdraws a notification the phone has dismissed.
  ///
  /// Silent for an id the queue no longer holds: that is the normal outcome for
  /// anything older than [maxQueued], and telling the watch to remove something
  /// it was never told about is noise.
  Future<void> remove(int id) async {
    if (!_enabled) {
      // Withdraw it from what is waiting to be announced, or the watch would be
      // told about a notification the phone has already dismissed. Logged
      // because a run of silent dismissals otherwise reads as a link that
      // opened, did nothing and closed — which is what made one session here
      // impossible to interpret.
      final held = _awaitingSubscription.length;
      _awaitingSubscription.removeWhere((queued) => queued.id == id);
      _remove(id);
      if (_awaitingSubscription.length != held) {
        garminLog('[GARMIN-NOTIFY] $id was dismissed before it was announced');
      }
      return;
    }
    final notification = _find(id);
    if (notification == null) return;
    _remove(id);
    await send(buildNotificationUpdate(
      updateType: GarminNotificationUpdateType.remove,
      category: notification.category,
      count: _countOf(notification.category),
      notificationId: id,
    ));
  }

  /// Answers a control request from the watch.
  ///
  /// The caller is expected to have sent the control status already — see the
  /// `GarminNotificationControl` arm of [GarminSession].
  Future<void> handleControl(GarminNotificationControl message) async {
    if (!_enabled) return;

    switch (message.command) {
      case GarminNotificationCommand.getNotificationAttributes:
        break; // handled below
      case GarminNotificationCommand.performNotificationAction:
      case GarminNotificationCommand.performLegacyNotificationAction:
        await _performAction(message);
        return;
      case GarminNotificationCommand.getAppAttributes:
        // Gadgetbridge marks this "unknown/untested" and no watch here has sent
        // one. Logged so that stops being true silently.
        garminLog('[GARMIN-NOTIFY] app attributes requested for '
            '${message.appIdentifier}; not implemented');
        return;
    }

    final notification = _find(message.notificationId);
    if (notification == null) {
      // Nothing to send and nothing to report. The watch asked about something
      // that has aged out of the queue; there is no protocol way to say so, and
      // an error status would abort a transfer that never started.
      garminLog('[GARMIN-NOTIFY] no notification ${message.notificationId} '
          'left to answer with');
      return;
    }

    final blob = encodeGarminNotificationAttributes(
      notification: notification,
      requested: message.attributes,
    );
    garminLog('[GARMIN-NOTIFY] answering ${message.notificationId} with '
        '${message.attributes.length} attributes (${blob.length}B)');
    final upload = _NotificationUpload(blob);
    _upload = upload;
    await _sendNext(upload);
  }

  /// Resolves an action the wearer invoked and hands it to [onAction].
  Future<void> _performAction(GarminNotificationControl message) async {
    final notification = _find(message.notificationId);
    if (notification == null) {
      garminLog('[GARMIN-NOTIFY] action for unknown notification '
          '${message.notificationId}');
      return;
    }
    final action = _resolveAction(notification, message);
    if (action == null) {
      garminLog('[GARMIN-NOTIFY] no action matching code '
          '${message.actionCode} on ${message.notificationId}');
      return;
    }
    garminLog('[GARMIN-NOTIFY] the wearer chose "${action.label}" '
        '(${action.kind.name}) on ${message.notificationId}'
        '${message.actionText == null ? "" : " with a reply"}');
    await onAction?.call(GarminNotificationActionRequest(
      notificationId: notification.id,
      action: action,
      replyText: message.actionText,
    ));
  }

  /// Matches what the watch invoked to what was offered.
  ///
  /// A legacy action carries no code this app chose — it is the accept/refuse
  /// pair the watch draws from the ACTION_DECLINE category flag, whose ordinals
  /// are 0 and 1. Refuse means dismiss, which is what a wearer swiping a card
  /// away expects, so it maps onto whatever dismiss action was offered.
  GarminNotificationAction? _resolveAction(
    GarminNotification notification,
    GarminNotificationControl message,
  ) {
    if (message.command ==
        GarminNotificationCommand.performLegacyNotificationAction) {
      const legacyRefuse = 1;
      if (message.actionCode != legacyRefuse) return null;
      return notification.actions
          .where((a) => a.kind == GarminNotificationActionKind.dismiss)
          .firstOrNull;
    }
    final code = message.actionCode;
    if (code == null) return null;
    return notification.actions
        .where((a) => a.kind.code == code)
        .firstOrNull;
  }

  /// Drives the chunked upload from the watch's per-chunk verdict.
  ///
  /// Two deliberate deviations from Gadgetbridge:
  ///
  /// * **RESEND is honoured, once.** Gadgetbridge abandons the upload with a
  ///   `TODO`. Repeating the last chunk costs nothing — its offset and CRC are
  ///   already held — and the alternative is a notification that arrives on the
  ///   wrist with an empty body and no way to tell why.
  /// * **OFFSET_MISMATCH abandons rather than seeks.** The status carries no
  ///   offset, so there is nothing to recover to; guessing would corrupt the
  ///   blob more quietly than failing does.
  Future<void> handleDataStatus(GarminNotificationDataStatus status) async {
    if (!_enabled) return;
    final upload = _upload;
    if (upload == null) {
      garminLog('[GARMIN-NOTIFY] transfer status with nothing in flight');
      return;
    }

    if (status.canProceed) {
      if (upload.isComplete) {
        _upload = null;
        garminLog('[GARMIN-NOTIFY] sent ${upload.totalSize}B');
        await send(buildNotificationDataFinalAck());
        return;
      }
      await _sendNext(upload);
      return;
    }

    if (status.transferStatus == GarminNotificationTransferStatus.resend &&
        upload.canResend) {
      garminLog('[GARMIN-NOTIFY] resending chunk at ${upload.lastOffset}');
      await _resend(upload);
      return;
    }

    garminLog('[GARMIN-NOTIFY] abandoning transfer: '
        '${status.transferStatus.name}');
    _upload = null;
  }

  /// Forgets everything. Called when a link ends, so a new one does not answer
  /// with a transfer the previous watch conversation left half-sent.
  void reset() {
    _upload = null;
    _queue.clear();
    _awaitingSubscription.clear();
  }

  Future<void> _sendNext(_NotificationUpload upload) async {
    final chunk = upload.take();
    await send(buildNotificationData(
      chunk: chunk,
      totalSize: upload.totalSize,
      dataOffset: upload.lastOffset,
      runningCrc: upload.lastCrc,
    ));
  }

  Future<void> _resend(_NotificationUpload upload) async {
    await send(buildNotificationData(
      chunk: upload.lastChunk,
      totalSize: upload.totalSize,
      dataOffset: upload.lastOffset,
      runningCrc: upload.lastCrc,
    ));
    upload.markResent();
  }

  GarminNotification? _find(int id) {
    for (final notification in _queue) {
      if (notification.id == id) return notification;
    }
    return null;
  }

  /// Removes [id] if present. Returns whether it was.
  bool _remove(int id) {
    final before = _queue.length;
    _queue.removeWhere((notification) => notification.id == id);
    return _queue.length != before;
  }

  /// Drops the oldest until the queue fits.
  ///
  /// No REMOVE is sent for what falls out — Gadgetbridge notes the same gap. The
  /// watch keeps showing it and simply gets nothing back if it ever asks, which
  /// is the same outcome as a notification the phone dismissed while the link
  /// was down.
  void _evict() {
    while (_queue.length > maxQueued) {
      _queue.removeFirst();
    }
  }

  int _countOf(GarminNotificationCategory category) =>
      _queue.where((n) => n.category == category).length;
}

/// One attribute blob being streamed out: the bytes, how far they have got, and
/// the running CRC the watch checks each chunk against.
///
/// The mirror image of `_ActiveDownload` in `garmin_session.dart`, which does
/// the same bookkeeping for a file arriving.
class _NotificationUpload {
  _NotificationUpload(this._blob);

  /// The protocol's own ceiling, not the MTU's: the ML layer fragments a frame
  /// to fit whatever MTU was negotiated, so this stays 300 regardless.
  static const int maxChunkSize = 300;

  /// How many times one chunk may be repeated before the upload is abandoned.
  /// One retry covers a dropped write; a watch asking twice is telling us
  /// something the retry will not fix.
  static const int maxResends = 1;

  final Uint8List _blob;

  int _offset = 0;
  int _runningCrc = 0;
  int _resends = 0;

  /// The chunk last handed to the transport, kept so a RESEND can repeat it
  /// byte-for-byte with the CRC the watch was already told to expect.
  Uint8List lastChunk = Uint8List(0);
  int lastOffset = 0;
  int lastCrc = 0;

  int get totalSize => _blob.length;
  bool get isComplete => _offset >= _blob.length;
  bool get canResend => _resends < maxResends;

  /// Takes the next chunk, advancing the offset and the running CRC.
  Uint8List take() {
    final end = _offset + maxChunkSize > _blob.length
        ? _blob.length
        : _offset + maxChunkSize;
    final chunk = Uint8List.sublistView(_blob, _offset, end);
    _runningCrc = GarminCrc.compute(chunk, initialCrc: _runningCrc);
    lastChunk = chunk;
    lastOffset = _offset;
    lastCrc = _runningCrc;
    _offset = end;
    _resends = 0;
    return chunk;
  }

  void markResent() => _resends++;
}
