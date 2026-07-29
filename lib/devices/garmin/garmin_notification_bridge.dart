/// The seam between Android's notification listener and the Garmin stack.
///
/// Android binds a `NotificationListenerService` whenever this process is alive
/// — usually with no Flutter engine anywhere — so the native side buffers what
/// survives its filter and starts a HEADLESS engine at
/// [garminNotificationForwarderCallback]. This file is that entry point and the
/// registration that makes it findable.
///
/// **Nothing here may throw.** An exception escaping the entry point takes the
/// engine down mid-notification with no user-visible trace, exactly as the
/// hydration quick-add's header warns. Everything is wrapped.
///
/// **No Health Connect, no drift.** This isolate reads two things —
/// SharedPreferences for the paired watch, and the native buffer — and writes
/// nothing. `AGENTS.md` invariant #1 (a background isolate gets its
/// `HealthDataSource` from `openBackgroundHealthAccess()`) therefore does not
/// apply, and #6 (a background isolate must never open drift) is satisfied by
/// there being no database call at all. If either ever becomes necessary, read
/// those invariants first.
library;

import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:notification_listener_native/notification_listener_native.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../core/diagnostics/diagnostics_build_config.dart';
import '../core/registry/ble_device_repository_impl.dart';
import 'garmin_log.dart';
import 'garmin_notification_actions.dart';
import 'garmin_notification_forwarder.dart';
import 'garmin_notification_messages.dart';
import 'garmin_phone_identity.dart';
import 'garmin_radio_lease.dart';

/// Registers the entry point so the native side can find it later.
///
/// Must run on **every** app start, not once. The plugin persists a raw AOT
/// callback handle, and an app update invalidates it — a stale handle silently
/// drops every notification until the app is next opened, which is the same trap
/// `registerHomeWidgetInteractivity` documents.
Future<void> registerGarminNotificationForwarder() async {
  try {
    final handle =
        PluginUtilities.getCallbackHandle(garminNotificationForwarderCallback);
    if (handle == null) {
      // Only possible if tree-shaking dropped the entry point, which the
      // pragma below exists to prevent.
      garminLog('[GARMIN-NOTIFY] forwarder entry point did not resolve');
      return;
    }
    final api = NotificationListenerHostApi();
    // Every app start, alongside the handle, and deliberately not only from the
    // watch settings screen: the listener service logs from a process with no
    // Flutter engine, and everything it logs is notification-derived. A store
    // release that never opened that screen must still be silent, and a nightly
    // must still be useful.
    await api.setDiagnosticsEnabled(kDiagnosticsEnabled);
    await api.registerForwarderCallback(handle.toRawHandle());
  } on PlatformException catch (error) {
    garminLog('[GARMIN-NOTIFY] could not register the forwarder: $error');
  } on MissingPluginException {
    // No host to register against (tests, desktop).
  } catch (error) {
    garminLog('[GARMIN-NOTIFY] could not register the forwarder: $error');
  }
}

/// Runs in a **headless isolate** when a notification survives the native
/// filter.
///
/// Must be top-level and `@pragma('vm:entry-point')`, or tree-shaking drops it
/// and the stored handle will not resolve.
@pragma('vm:entry-point')
Future<void> garminNotificationForwarderCallback() async {
  // The headless engine has no `runApp`, so the bindings the platform channels
  // need are not up yet.
  WidgetsFlutterBinding.ensureInitialized();
  try {
    DartPluginRegistrant.ensureInitialized();
    await GarminNotificationBridge().run();
  } catch (error, stack) {
    garminLog('[GARMIN-NOTIFY] forwarder failed: $error\n$stack');
  }
}

/// Drains the native buffer into a [GarminNotificationForwarder], and stops the
/// engine once there is nothing left to do.
///
/// Constructed by hand with no Riverpod container: the provider graph belongs to
/// the app's isolate and does not exist here — the same shape as
/// `buildBackgroundHydrationQuickAddLogger`.
class GarminNotificationBridge {
  GarminNotificationBridge({
    NotificationListenerHostApi? api,
    GarminRadioLease? lease,
    this.phoneIdentity = const GarminPhoneIdentity(),
  })  : _api = api ?? NotificationListenerHostApi(),
        _lease = lease ?? NativeGarminRadioLease();

  final NotificationListenerHostApi _api;
  final GarminRadioLease _lease;
  final GarminPhoneIdentity phoneIdentity;

  GarminNotificationForwarder? _forwarder;

  /// Boots the forwarder and keeps this isolate alive until it goes idle.
  Future<void> run() async {
    final address = await _pairedWatchAddress();
    if (address == null) {
      garminLog('[GARMIN-NOTIFY] no Garmin watch paired; nothing to forward to');
      await _stopEngine();
      return;
    }

    final done = Completer<void>();
    final forwarder = GarminNotificationForwarder(
      address: address,
      phoneName: phoneIdentity.bluetoothName,
      manufacturer: phoneIdentity.manufacturer,
      model: phoneIdentity.model,
      lease: _lease,
      onAction: _performAction,
      onIdle: () {
        if (!done.isCompleted) done.complete();
      },
    );
    _forwarder = forwarder;

    // Later notifications arrive while this isolate is alive, so the engine is
    // reused rather than restarted — which is what lets a burst share one link.
    NotificationListenerFlutterApi.setUp(_PendingReceiver(_drain));

    await _drain();
    // Waits indefinitely by design. The forwarder holds the watch link open for
    // as long as the watch is in range, so `onIdle` fires only when there is
    // genuinely nothing left to do — it could not connect at all, or the feature
    // was switched off. Returning earlier would tear down the engine underneath
    // a link the user is relying on.
    await done.future;
    await forwarder.dispose();
    await _stopEngine();
  }

  /// Moves everything the native side is holding into the forwarder.
  Future<void> _drain() async {
    final forwarder = _forwarder;
    if (forwarder == null) return;
    final List<NotificationMsg> pending;
    try {
      pending = await _api.takePendingNotifications();
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not read the pending buffer: $error');
      return;
    }
    if (pending.isEmpty) return;
    garminLog('[GARMIN-NOTIFY] draining ${pending.length} notification(s)');
    for (final message in pending) {
      if (message.removed) {
        forwarder.withdraw(message.id);
        continue;
      }
      forwarder.post(_toNotification(message));
    }
  }

  /// The address of the paired Garmin watch, or null.
  ///
  /// Read straight out of SharedPreferences through the same registry the app
  /// uses. No drift, per invariant #6, and none is needed: the registry has been
  /// prefs-backed since it was written.
  Future<String?> _pairedWatchAddress() async {
    try {
      final preferences = await SharedPreferences.getInstance();
      // A pairing made by the app's isolate since this engine started must be
      // visible, and a prefs cache filled at engine start would not show it.
      await preferences.reload();
      final registry =
          BleDeviceRepositoryImpl(preferences);
      final watch = registry.devices.where((d) => d.isGarminGfdi).firstOrNull;
      return watch?.address;
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] could not read the paired watch: $error');
      return null;
    }
  }

  Future<void> _stopEngine() async {
    try {
      await _api.stopForwarder();
    } catch (_) {
      // Nothing to do — the engine outliving its work costs memory, not
      // correctness, and the process will reclaim it.
    }
  }

  /// Performs what the wearer chose on the wrist.
  ///
  /// Dismiss is OURS, not the posting app's — there is no notification action
  /// for "clear this", so it maps onto the listener's own cancel rather than a
  /// PendingIntent. Everything else is the app's own button, fired by index.
  Future<void> _performAction(GarminNotificationActionRequest request) async {
    try {
      final performed = request.action.isSynthetic
          ? await _api.dismissNotification(request.notificationId)
          : await _api.performNotificationAction(
              request.notificationId,
              request.action.androidIndex,
              request.replyText,
            );
      if (!performed) {
        // The notification is gone from the phone, or its intent was cancelled.
        // There is no way to tell the watch, so this is only worth logging.
        garminLog('[GARMIN-NOTIFY] "${request.action.label}" could not be '
            'performed on ${request.notificationId}');
      }
    } catch (error) {
      garminLog('[GARMIN-NOTIFY] performing "${request.action.label}" '
          'failed: $error');
    }
  }

  GarminNotification _toNotification(NotificationMsg message) =>
      GarminNotification(
        id: message.id,
        packageName: message.packageName,
        title: message.title ?? message.appLabel ?? message.packageName,
        subtitle: message.subtitle ?? '',
        body: message.body ?? '',
        category: _categoryOf(message.categoryOrdinal),
        postedAt:
            DateTime.fromMillisecondsSinceEpoch(message.whenEpochMillis),
        actions: garminActionsFor(message),
      );

  /// The native side pre-maps Android's category constants onto this enum's
  /// ordinals. An ordinal outside the enum means the two have drifted apart, and
  /// [GarminNotificationCategory.other] is the safe reading — a notification
  /// mislabelled as an incoming call would be worse than one left unlabelled.
  GarminNotificationCategory _categoryOf(int ordinal) =>
      ordinal >= 0 && ordinal < GarminNotificationCategory.values.length
          ? GarminNotificationCategory.values[ordinal]
          : GarminNotificationCategory.other;
}

/// Wakes the drain when the native side buffers something new while this isolate
/// is already running.
class _PendingReceiver implements NotificationListenerFlutterApi {
  _PendingReceiver(this._onPending);

  final Future<void> Function() _onPending;

  @override
  void onNotificationsPending() {
    unawaited(_onPending());
  }
}
