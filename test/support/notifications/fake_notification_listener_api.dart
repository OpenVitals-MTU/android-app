import 'package:flutter/services.dart';
import 'package:notification_listener_native/notification_listener_native.dart';

/// Every method on [NotificationListenerHostApi], answered in memory.
///
/// `implements`, not `extends`, for the same reason
/// `ExhaustiveFakeHostApi` does it: extending would leave un-overridden methods
/// falling through to the real Pigeon implementation, hitting a channel with no
/// handler. The compiler enforcing all of them is what stops a new host method
/// silently going untested.
// The two Pigeon-generated fields are part of the public interface, so
// implementing the class means implementing them. Their names are Pigeon's.
// ignore_for_file: non_constant_identifier_names
class FakeNotificationListenerApi implements NotificationListenerHostApi {
  FakeNotificationListenerApi({
    this.accessGranted = true,
    this.launchableApps = const [],
  });

  @override
  BinaryMessenger? get pigeonVar_binaryMessenger => null;

  @override
  String get pigeonVar_messageChannelSuffix => '';

  bool accessGranted;
  List<InstalledAppMsg> launchableApps;

  /// Notifications the native side is holding, for [takePendingNotifications].
  final List<NotificationMsg> pending = [];

  /// Calls recorded so a test can assert what the app told the native side.
  int? registeredCallbackHandle;
  bool openedAccessSettings = false;
  bool stoppedForwarder = false;
  ({
    bool enabled,
    List<String> blocked,
    String? watchAddress,
    bool diagnostics,
  })? lastConfig;

  /// Address → owner, mirroring [RadioLeases] closely enough to test priority.
  final Map<String, String> leases = {};

  @override
  Future<bool> isNotificationAccessGranted() async => accessGranted;

  @override
  Future<void> openNotificationAccessSettings() async {
    openedAccessSettings = true;
  }

  /// Last value pushed by [setDiagnosticsEnabled], or null if never called.
  bool? diagnosticsEnabled;

  @override
  Future<void> setDiagnosticsEnabled(bool enabled) async {
    diagnosticsEnabled = enabled;
  }

  @override
  Future<void> registerForwarderCallback(int callbackHandle) async {
    registeredCallbackHandle = callbackHandle;
  }

  @override
  Future<void> setForwardingConfig(
    bool enabled,
    List<String> blockedPackages,
    String? watchAddress,
    bool diagnostics,
  ) async {
    lastConfig = (
      enabled: enabled,
      blocked: blockedPackages,
      watchAddress: watchAddress,
      diagnostics: diagnostics,
    );
  }

  @override
  Future<List<NotificationMsg>> takePendingNotifications() async {
    final taken = List<NotificationMsg>.from(pending);
    pending.clear();
    return taken;
  }

  @override
  Future<void> stopForwarder() async {
    stoppedForwarder = true;
  }

  /// Actions performed from the wrist, in order.
  final List<({int id, int actionIndex, String? replyText})> performed = [];

  /// Ids dismissed from the wrist.
  final List<int> dismissed = [];

  /// Set false to simulate a notification the phone has already torn down.
  bool actionsSucceed = true;

  @override
  Future<bool> performNotificationAction(
    int id,
    int actionIndex,
    String? replyText,
  ) async {
    performed.add((id: id, actionIndex: actionIndex, replyText: replyText));
    return actionsSucceed;
  }

  @override
  Future<bool> dismissNotification(int id) async {
    dismissed.add(id);
    return actionsSucceed;
  }

  @override
  Future<List<InstalledAppMsg>> listLaunchableApps() async => launchableApps;

  @override
  Future<bool> acquireRadio(String address, String owner, int ttlMillis) async {
    final held = leases[address];
    if (held != null && held != owner) return false;
    if (radioWaiters[address] == owner) radioWaiters.remove(address);
    leases[address] = owner;
    return true;
  }

  /// Who has asked for a lease somebody else holds. A pending request makes the
  /// holder's renewal fail, which is how it is told to let go.
  final Map<String, String> radioWaiters = {};

  @override
  Future<void> requestRadio(String address, String owner) async {
    final held = leases[address];
    if (held == null || held == owner) return;
    radioWaiters[address] = owner;
  }

  @override
  Future<bool> renewRadio(String address, String owner) async {
    if (leases[address] != owner) return false;
    final waiter = radioWaiters[address];
    return waiter == null || waiter == owner;
  }

  @override
  Future<void> releaseRadio(String address, String owner) async {
    if (leases[address] == owner) leases.remove(address);
  }

  @override
  Future<String?> radioOwner(String address) async => leases[address];
}
