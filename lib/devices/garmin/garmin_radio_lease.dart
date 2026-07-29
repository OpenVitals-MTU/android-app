import 'dart:async';

import 'package:notification_listener_native/notification_listener_native.dart';

import 'garmin_log.dart';

/// Exclusive access to one watch's radio, across isolates.
///
/// **Why this exists.** Until notification forwarding, everything that opened a
/// Garmin link ran in the app's own isolate, where a Riverpod-scoped `isSyncing`
/// check was enough to stop a sync and a find colliding. The forwarder runs in a
/// HEADLESS Flutter engine, and each engine gets its own `FlutterBluePlusPlugin`
/// with its own `BluetoothGatt` map. Neither knows the other exists, and no Dart
/// mutex can span them — so the lock has to be process-wide, which in a Flutter
/// app means Kotlin.
///
/// The existing Dart-side guards stay. They are the fast in-isolate check and
/// the thing that greys out a button; this is the correctness boundary beneath
/// them, and the only one the forwarder can see.
abstract class GarminRadioLease {
  /// Takes the lease on [address] for [owner], or returns false when something
  /// else holds it.
  Future<bool> acquire(String address, String owner);

  /// Announces that [owner] wants a lease somebody else holds.
  ///
  /// Grants nothing — it makes the holder's next [renew] fail. Notification
  /// forwarding holds its lease for as long as the watch is in range, so without
  /// this a tap on Sync would wait until the user walked away from their watch.
  Future<void> request(String address, String owner);

  /// Extends a held lease. False once it has expired, been taken, or somebody
  /// else has asked for it — the last of which is the holder's cue to stop.
  Future<bool> renew(String address, String owner);

  /// Releases a lease. A no-op when [owner] is not the holder, so a late release
  /// cannot cancel somebody else's work.
  Future<void> release(String address, String owner);

  /// Who holds [address], or null when it is free. Diagnostic.
  Future<String?> owner(String address);

  /// How long a lease survives without a renewal.
  ///
  /// Short on purpose: the forwarder isolate can be killed at any moment — that
  /// is the whole premise of a headless engine — and a lease that outlived its
  /// holder would wedge the radio until the process died. The holder renews at
  /// [renewInterval] while it is working.
  static const Duration ttl = Duration(seconds: 15);
  static const Duration renewInterval = Duration(seconds: 5);

  /// How long a user-initiated action waits for the holder to let go.
  ///
  /// Comfortably longer than [renewInterval], which bounds how quickly
  /// notification forwarding notices it has been asked to stop.
  static const Duration handoverWait = Duration(seconds: 8);
}

/// The real lease, backed by the native plugin.
class NativeGarminRadioLease implements GarminRadioLease {
  NativeGarminRadioLease({NotificationListenerHostApi? api})
      : _api = api ?? NotificationListenerHostApi();

  final NotificationListenerHostApi _api;

  @override
  Future<bool> acquire(String address, String owner) async {
    try {
      return await _api.acquireRadio(
        address,
        owner,
        GarminRadioLease.ttl.inMilliseconds,
      );
    } catch (error) {
      // A platform that has no host implementation (a test host, iOS) must not
      // make the Garmin stack unusable — it just has no second isolate to guard
      // against either.
      garminLog('[GARMIN-RADIO] lease unavailable, proceeding: $error');
      return true;
    }
  }

  @override
  Future<void> request(String address, String owner) async {
    try {
      await _api.requestRadio(address, owner);
    } catch (_) {
      // No host: nothing is holding anything either.
    }
  }

  @override
  Future<bool> renew(String address, String owner) async {
    try {
      return await _api.renewRadio(address, owner);
    } catch (_) {
      return true;
    }
  }

  @override
  Future<void> release(String address, String owner) async {
    try {
      await _api.releaseRadio(address, owner);
    } catch (_) {
      // Nothing to do: the lease expires on its own.
    }
  }

  @override
  Future<String?> owner(String address) async {
    try {
      return await _api.radioOwner(address);
    } catch (_) {
      return null;
    }
  }
}

/// A lease that grants everything, for hosts with no plugin (unit tests).
class PermissiveGarminRadioLease implements GarminRadioLease {
  const PermissiveGarminRadioLease();

  @override
  Future<bool> acquire(String address, String owner) async => true;

  @override
  Future<void> request(String address, String owner) async {}

  @override
  Future<bool> renew(String address, String owner) async => true;

  @override
  Future<void> release(String address, String owner) async {}

  @override
  Future<String?> owner(String address) async => null;
}

/// Runs [body] holding the lease on [address], renewing it throughout, and
/// releases it however [body] ends.
///
/// Throws [GarminRadioBusyException] when the lease cannot be taken. Callers
/// decide what that means: a user-initiated action reports it, the forwarder
/// backs off and tries again.
Future<T> withGarminRadio<T>(
  GarminRadioLease lease,
  String address,
  String owner,
  Future<T> Function() body, {
  Duration waitFor = GarminRadioLease.handoverWait,
}) async {
  if (!await lease.acquire(address, owner)) {
    // Ask, then wait, rather than failing outright. The likely holder is
    // notification forwarding, which gives the radio up on its next renew tick —
    // a second or two — and a user who tapped Sync should not be told the watch
    // is busy with something they never asked for.
    await lease.request(address, owner);
    var waited = Duration.zero;
    const step = Duration(milliseconds: 250);
    while (waited < waitFor) {
      await Future<void>.delayed(step);
      waited += step;
      if (await lease.acquire(address, owner)) {
        garminLog('[GARMIN-RADIO] $owner took the radio after '
            '${waited.inMilliseconds}ms');
        return _runHolding(lease, address, owner, body);
      }
    }
    final holder = await lease.owner(address);
    throw GarminRadioBusyException(holder ?? 'another task');
  }
  return _runHolding(lease, address, owner, body);
}

Future<T> _runHolding<T>(
  GarminRadioLease lease,
  String address,
  String owner,
  Future<T> Function() body,
) async {
  // Renewed on a timer rather than at each protocol step: a file download can
  // sit inside one await for several seconds, and a lease that expired mid-sync
  // would let the forwarder open a second link to the same watch.
  final renewals = Timer.periodic(
    GarminRadioLease.renewInterval,
    (_) => lease.renew(address, owner),
  );
  try {
    return await body();
  } finally {
    renewals.cancel();
    await lease.release(address, owner);
  }
}

/// The radio is held by something else.
class GarminRadioBusyException implements Exception {
  const GarminRadioBusyException(this.holder);

  /// The [withGarminRadio] owner tag of whatever holds it.
  final String holder;

  @override
  String toString() => 'GarminRadioBusyException: the watch is busy ($holder)';
}

/// Owner tags. Stable strings rather than an enum because they cross the Pigeon
/// boundary and appear in logcat, where a readable name is the whole point.
class GarminRadioOwner {
  const GarminRadioOwner._();

  static const String sync = 'sync';
  static const String find = 'find';
  static const String settings = 'settings';
  static const String notifications = 'notifications';
}
