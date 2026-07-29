import 'dart:async';

import 'package:fake_async/fake_async.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_notification_forwarder.dart';
import 'package:openvitals/devices/garmin/garmin_notification_link.dart';
import 'package:openvitals/devices/garmin/garmin_notification_messages.dart';
import 'package:openvitals/devices/garmin/garmin_notifications_handler.dart';
import 'package:openvitals/devices/garmin/garmin_radio_lease.dart';

const _address = 'AA:BB:CC:DD:EE:FF';

/// A link that opens instantly and records what it was asked to send.
class _FakeLink implements GarminNotificationLink {
  _FakeLink();

  final List<GarminNotification> pushed = [];
  final List<int> withdrawn = [];
  bool closed = false;

  final StreamController<void> _gone = StreamController<void>.broadcast();
  final StreamController<void> _activity = StreamController<void>.broadcast();

  /// A real handler, because the forwarder reads `handler.held` on close to
  /// take back anything the watch never subscribed for. Its `send` goes
  /// nowhere: these tests assert on [pushed], not on wire bytes.
  @override
  late final GarminNotificationsHandler handler =
      GarminNotificationsHandler(send: (_) async {});

  @override
  bool get isOpen => !closed;

  @override
  bool get subscribed => true;

  @override
  Stream<void> get onGone => _gone.stream;

  @override
  Stream<void> get onActivity => _activity.stream;

  /// Simulates the watch saying something, which is what resets the idle hold.
  void watchSpoke() => _activity.add(null);

  /// Simulates the watch walking out of range.
  void drop() => _gone.add(null);

  @override
  Future<void> push(GarminNotification notification) async {
    pushed.add(notification);
    // Routed through the real handler, as the real link does, so `handler.held`
    // reflects what has actually been announced versus what is still waiting on
    // the watch to subscribe.
    await handler.post(notification);
  }

  @override
  Future<void> withdraw(int notificationId) async {
    withdrawn.add(notificationId);
  }

  @override
  Future<void> close() async {
    closed = true;
    // Not awaited: a broadcast controller's close future does not complete under
    // `fakeAsync`, and the real link does not make its caller wait for one
    // either.
    if (!_gone.isClosed) unawaited(_gone.close());
    if (!_activity.isClosed) unawaited(_activity.close());
  }
}

/// A lease that can be held by somebody else, to test priority.
class _FakeLease implements GarminRadioLease {
  String? holder;
  int acquireAttempts = 0;

  @override
  Future<bool> acquire(String address, String owner) async {
    acquireAttempts++;
    if (holder != null && holder != owner) return false;
    holder = owner;
    return true;
  }

  /// Set by a test to simulate a sync asking for the radio.
  String? requestedBy;

  @override
  Future<void> request(String address, String owner) async {
    requestedBy = owner;
  }

  @override
  Future<bool> renew(String address, String owner) async =>
      holder == owner && (requestedBy == null || requestedBy == owner);

  @override
  Future<void> release(String address, String owner) async {
    if (holder == owner) holder = null;
  }

  @override
  Future<String?> owner(String address) async => holder;
}

GarminNotification _notification(int id) => GarminNotification(
      id: id,
      packageName: 'com.example.chat',
      title: 'Ada',
      body: 'On my way',
      postedAt: DateTime(2026, 7, 28, 9, 5, 3),
    );

/// Builds a forwarder over [links], handing out one per open() call.
({GarminNotificationForwarder forwarder, List<_FakeLink> links, _FakeLease lease})
    _build({
  _FakeLease? lease,
  Future<GarminNotificationLink> Function()? openOverride,
  void Function()? onIdle,
}) {
  final links = <_FakeLink>[];
  final theLease = lease ?? _FakeLease();
  final forwarder = GarminNotificationForwarder(
    address: _address,
    phoneName: 'Pixel 6 Pro',
    manufacturer: 'Google',
    model: 'raven',
    lease: theLease,
    onIdle: onIdle,
    openLink: ({
      required String address,
      required String phoneName,
      required String manufacturer,
      required String model,
      Duration handshakeTimeout = const Duration(seconds: 15),
      Future<void> Function(GarminNotificationActionRequest request)? onAction,
    }) {
      if (openOverride != null) return openOverride();
      final link = _FakeLink();
      links.add(link);
      return Future.value(link);
    },
  );
  return (forwarder: forwarder, links: links, lease: theLease);
}

void main() {
  group('coalescing', () {
    test('three notifications 200ms apart open ONE link, not three', () {
      fakeAsync((async) {
        final f = _build();
        for (var i = 0; i < 3; i++) {
          f.forwarder.post(_notification(i));
          async.elapse(const Duration(milliseconds: 200));
        }
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();

        expect(f.links, hasLength(1));
        expect(f.links.single.pushed.map((n) => n.id), [0, 1, 2]);
      });
    });

    test('a steady drip cannot postpone the connect past the ceiling', () {
      fakeAsync((async) {
        final f = _build();
        // One arrival per second forever would reset a pure debounce every time.
        for (var i = 0; i < 10; i++) {
          f.forwarder.post(_notification(i));
          async.elapse(const Duration(seconds: 1));
        }

        expect(f.links, isNotEmpty,
            reason: 'the max coalesce wait must force the connect');
      });
    });

    test('a notification arriving while the link is open is sent immediately',
        () {
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();
        expect(f.links.single.pushed, hasLength(1));

        f.forwarder.post(_notification(2));
        async.elapse(const Duration(milliseconds: 10));
        async.flushMicrotasks();

        expect(f.links.single.pushed, hasLength(2));
        expect(f.links, hasLength(1));
      });
    });

    test('a dismissal is forwarded as a withdrawal', () {
      fakeAsync((async) {
        final f = _build();
        f.forwarder.withdraw(42);
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();

        expect(f.links.single.withdrawn, [42]);
      });
    });
  });

  group('the link is held', () {
    test('the link is still open minutes after the last notification', () {
      // The whole point of the change: a Garmin watch expects a continuously
      // connected phone, and a link that closes leaves it saying "reconnect to
      // phone to refresh data" and sometimes failing to re-subscribe.
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(minutes: 10));
        async.flushMicrotasks();

        expect(f.forwarder.isLinkOpen, isTrue);
        expect(f.links, hasLength(1), reason: 'and it was never re-opened');
      });
    });

    test('a watch that walks out of range is reconnected to', () {
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();
        f.links.single.drop();
        async.elapse(const Duration(seconds: 20));
        async.flushMicrotasks();

        expect(f.links, hasLength(2));
        expect(f.forwarder.isLinkOpen, isTrue);
      });
    });

    test('a notification the watch never subscribed for survives the link '
        'dropping', () {
      // The handler lives and dies with its link, and the forwarder has already
      // dropped the item from its own queue by then — so without taking the
      // unannounced ones back, a watch that walks away in the second between
      // "queued" and "subscribed" loses exactly the notification the link was
      // opened for.
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();

        // The link is up but the watch has not subscribed, so the handler is
        // holding it rather than having announced it.
        final first = f.links.single;
        expect(first.pushed.single.id, 1);
        expect(first.handler.held.map((n) => n.id), [1],
            reason: 'held, because the watch has not subscribed');

        first.drop();
        async.elapse(const Duration(seconds: 20));
        async.flushMicrotasks();

        expect(f.links, hasLength(2));
        expect(f.links.last.pushed.map((n) => n.id), contains(1));
      });
    });

    test('a watch that stays away is retried on a growing backoff, not in a '
        'tight loop', () {
      fakeAsync((async) {
        var attempts = 0;
        final f = _build(openOverride: () {
          attempts++;
          return Future.error(StateError('out of range'));
        });
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(minutes: 30));
        async.flushMicrotasks();

        // Unbounded retries at the first backoff would be ~120 in half an hour.
        expect(attempts, lessThan(15));
        expect(attempts, greaterThan(3), reason: 'but it must keep trying');
      });
    });

    test('a notification that arrives while the watch is away is kept for '
        'when it returns', () {
      fakeAsync((async) {
        var fail = true;
        final links = <_FakeLink>[];
        final lease = _FakeLease();
        final forwarder = GarminNotificationForwarder(
          address: _address,
          phoneName: 'Pixel 6 Pro',
          manufacturer: 'Google',
          model: 'raven',
          lease: lease,
          openLink: ({
            required String address,
            required String phoneName,
            required String manufacturer,
            required String model,
            Duration handshakeTimeout = const Duration(seconds: 15),
            Future<void> Function(GarminNotificationActionRequest request)?
                onAction,
          }) {
            if (fail) return Future.error(StateError('out of range'));
            final link = _FakeLink();
            links.add(link);
            return Future.value(link);
          },
        );

        forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 5));
        async.flushMicrotasks();
        expect(links, isEmpty);

        fail = false; // the watch comes back
        async.elapse(const Duration(seconds: 30));
        async.flushMicrotasks();

        expect(links.single.pushed.single.id, 1,
            reason: 'the notification the link was opened for must survive');
      });
    });
  });

  group('the radio lease', () {
    test('the lease is taken before connecting and held with the link', () {
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();
        expect(f.lease.holder, GarminRadioOwner.notifications);

        async.elapse(const Duration(minutes: 5));
        async.flushMicrotasks();
        expect(f.lease.holder, GarminRadioOwner.notifications);
      });
    });

    test('the radio is given up when a sync asks for it, and taken back after',
        () {
      // Without this a permanently held link would block every sync, find and
      // settings browse — things the user actively initiated.
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();
        expect(f.forwarder.isLinkOpen, isTrue);

        f.lease.requestedBy = GarminRadioOwner.sync;
        async.elapse(const Duration(seconds: 10));
        async.flushMicrotasks();

        expect(f.forwarder.isLinkOpen, isFalse);
        expect(f.lease.holder, isNull, reason: 'so the sync can take it');

        // The sync finishes and stops asking.
        f.lease.requestedBy = null;
        async.elapse(const Duration(seconds: 30));
        async.flushMicrotasks();

        expect(f.forwarder.isLinkOpen, isTrue);
      });
    });

    test('a sync holding the radio defers the notification instead of '
        'interrupting it', () {
      fakeAsync((async) {
        final lease = _FakeLease()..holder = GarminRadioOwner.sync;
        final f = _build(lease: lease);
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));
        async.flushMicrotasks();

        expect(f.links, isEmpty, reason: 'the sync must not be interrupted');
      });
    });

    test('the deferred notification is sent once the sync releases the radio',
        () {
      fakeAsync((async) {
        final lease = _FakeLease()..holder = GarminRadioOwner.sync;
        final f = _build(lease: lease);
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));

        lease.holder = null; // the sync finished
        async.elapse(const Duration(seconds: 15));
        async.flushMicrotasks();

        expect(f.links, hasLength(1));
        expect(f.links.single.pushed.single.id, 1);
      });
    });
  });

  group('failure', () {
    test('a failed connect does not leave the lease held', () {
      fakeAsync((async) {
        final f = _build(
          openOverride: () => Future.error(StateError('out of range')),
        );
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 5));
        async.flushMicrotasks();

        expect(f.lease.holder, isNull);
      });
    });

    test('a held link never reports idle, so the isolate is not torn down', () {
      // The bridge waits on onIdle to stop the engine. Firing it while a link
      // is held would kill the forwarder underneath a connected watch.
      fakeAsync((async) {
        var idleCount = 0;
        final f = _build(onIdle: () => idleCount++);
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(minutes: 5));
        async.flushMicrotasks();

        expect(idleCount, 0);
        expect(f.forwarder.isLinkOpen, isTrue);
      });
    });

    test('disposing closes an open link and releases the radio', () {
      fakeAsync((async) {
        final f = _build();
        f.forwarder.post(_notification(1));
        async.elapse(const Duration(seconds: 2));

        f.forwarder.dispose();
        async.elapse(const Duration(milliseconds: 10));
        async.flushMicrotasks();
        async.flushMicrotasks();

        expect(f.links.single.closed, isTrue);
        expect(f.lease.holder, isNull);
      });
    });
  });
}
