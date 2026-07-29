// Pigeon contract for the `notification_listener_native` plugin.
//
// This file defines the Android NotificationListener <-> Flutter bridge used to
// mirror phone notifications to a paired Garmin watch. It is the SINGLE SOURCE
// OF TRUTH for the generated message classes:
//
//   * Dart   -> lib/src/messages.g.dart
//   * Kotlin -> android/src/main/kotlin/tech/mmarca/openvitals/notification_listener_native/Messages.g.kt
//
// Regenerate both after editing this file (run from the plugin directory):
//
//   dart run pigeon --input pigeons/messages.dart
//
// DESIGN NOTE
// -----------
// This plugin CAPTURES and FILTERS. It never touches Bluetooth. The GFDI/GNCS
// protocol and every byte that reaches the watch live in pure Dart under
// `lib/devices/garmin/`, so the whole notification conversation is unit-tested
// with no radio and no Android at all.
//
// The awkward shape of this bridge follows from where the two halves run.
// Android binds a NotificationListenerService whenever the process is alive —
// typically with no Flutter engine anywhere — while the Garmin stack is Dart on
// top of `flutter_blue_plus`. So the native side buffers what survives its
// filter and spins a HEADLESS Flutter engine from a stored callback handle
// (the same mechanism `android_alarm_manager_plus` and
// `flutter_local_notifications` use for their background callbacks), and the
// Dart side drains the buffer when it wakes.
//
// Filtering happens NATIVELY, before any engine is spun. That is the single
// biggest battery decision in the feature: a music player posting an ongoing
// notification every second must cost a boolean, not a Dart isolate.
//
// No notification content is persisted anywhere. It lives in a bounded native
// ring buffer until Dart drains it, and in a bounded Dart queue until the watch
// stops asking about it.
import 'package:pigeon/pigeon.dart';

/// One thing the user can do to a notification from the wrist.
class NotificationActionMsg {
  NotificationActionMsg(
    this.index,
    this.title,
    this.isReply,
    this.fireableFromBackground,
  );

  /// Position in the Android notification's own action list. Sent back verbatim
  /// to [NotificationListenerHostApi.performNotificationAction], so the phone
  /// never has to re-derive which action was meant — Gadgetbridge re-walks the
  /// list counting action types to map one back, and its own comment calls that
  /// fragile.
  final int index;

  /// What the posting app called it: "Reply", "Mark as read", "Snooze".
  final String title;

  /// Whether it expects text (a `RemoteInput`), in which case the watch offers
  /// its keyboard or canned replies and sends the result back.
  final bool isReply;

  /// Whether invoking it from a background service actually does anything.
  ///
  /// False for an ACTIVITY `PendingIntent`. Some apps — a stock SMS app among
  /// them — publish a "Reply" that opens their compose screen with the text
  /// prefilled rather than sending it, and Android blocks background activity
  /// launches outright, so firing one throws nothing and does nothing. Offering
  /// it on the wrist would be exactly the dead button this feature exists to
  /// remove.
  final bool fireableFromBackground;
}

/// One notification that survived the native filter, already reduced to what
/// GNCS can carry.
class NotificationMsg {
  NotificationMsg(
    this.id,
    this.packageName,
    this.appLabel,
    this.title,
    this.subtitle,
    this.body,
    this.whenEpochMillis,
    this.categoryOrdinal,
    this.removed,
    this.actions,
    this.dismissable,
  );

  /// Stable within a boot, derived from the notification's own key so an update
  /// to the same notification carries the same id — which is what makes the
  /// watch redraw one card instead of buzzing twice.
  final int id;

  /// The posting app's package name. Sent to the watch as APP_IDENTIFIER.
  final String packageName;

  /// The posting app's human-readable label, or null when the launcher query
  /// cannot resolve it. Never used as APP_IDENTIFIER — some watch faces resolve
  /// an icon from the package name and would fail on a label.
  final String? appLabel;

  final String? title;
  final String? subtitle;
  final String? body;

  final int whenEpochMillis;

  /// Pre-mapped natively to `GarminNotificationCategory`'s ordinal. Mapped on
  /// this side because it is derived from Android's own category constants and
  /// the posting package, both of which only exist here.
  final int categoryOrdinal;

  /// True when this is a dismissal rather than a post — the watch is told to
  /// withdraw the card.
  final bool removed;

  /// What the posting app offers, in its own order. Capped natively at what
  /// GNCS can carry (five custom actions plus a reply).
  final List<NotificationActionMsg> actions;

  /// Whether the notification can be cleared. An ongoing one cannot, and
  /// offering a dismiss button that silently fails is worse than not offering
  /// one.
  final bool dismissable;
}

/// An installed app the user can choose to block, for the settings picker.
class InstalledAppMsg {
  InstalledAppMsg(this.packageName, this.label);

  final String packageName;
  final String label;
}

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    kotlinOut:
        'android/src/main/kotlin/tech/mmarca/openvitals/notification_listener_native/Messages.g.kt',
    kotlinOptions: KotlinOptions(
      package: 'tech.mmarca.openvitals.notification_listener_native',
    ),
    dartPackageName: 'notification_listener_native',
  ),
)

/// Host (Android/Kotlin) API. Android-only: on other platforms the channel has
/// no host implementation and calls throw.
@HostApi()
abstract class NotificationListenerHostApi {
  /// Whether the user has granted notification access in system settings.
  ///
  /// There is no runtime prompt for this permission — the only way to grant it
  /// is [openNotificationAccessSettings], so the UI has to poll rather than
  /// await a result.
  bool isNotificationAccessGranted();

  /// Opens `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
  void openNotificationAccessSettings();

  /// Stores the raw callback handle of the Dart entry point to wake.
  ///
  /// Persisted, not held in memory: the listener service is bound long before
  /// the app's own engine has ever run — after a reboot, or after the process
  /// was killed — so the handle has to survive without Dart. Re-register on
  /// every app start; an app update invalidates a stored handle.
  void registerForwarderCallback(int callbackHandle);

  /// Tells the native side whether this build may log what it is doing.
  ///
  /// Mirrors Dart's `kDiagnosticsEnabled`: true in a debug build and in a
  /// nightly, false in a store release. Called on every app start rather than
  /// only when the feature's settings are touched, because the listener service
  /// runs with no Flutter engine and everything it logs is notification-derived
  /// — a build that has never opened the watch screen must still be silent, and
  /// a nightly must still be useful.
  void setDiagnosticsEnabled(bool enabled);

  /// Mirrors the user's configuration natively so the filter can run before an
  /// engine is spun.
  ///
  /// [watchAddress] null means no watch is paired, which disables capture
  /// outright — there is nowhere to send anything.
  ///
  /// [diagnostics] mirrors Dart's `kDiagnosticsEnabled` — true in a debug build
  /// and in a nightly, false in a store release. Everything this plugin logs is
  /// derived from notifications, so it says nothing at all without it.
  void setForwardingConfig(
    bool enabled,
    List<String> blockedPackages,
    String? watchAddress,
    bool diagnostics,
  );

  /// Drains and clears the pending buffer.
  List<NotificationMsg> takePendingNotifications();

  /// Tears down the headless forwarder engine.
  ///
  /// Called by the forwarder once it has closed the watch link and has nothing
  /// left to do. Without it the isolate would live for the rest of the process
  /// to no purpose. A no-op when called from the app's own engine.
  void stopForwarder();

  /// Performs one of a notification's own actions, as if the user had tapped it
  /// on the phone.
  ///
  /// [actionIndex] is [NotificationActionMsg.index]. [replyText] is the text the
  /// wearer dictated or picked, for a reply action, and null otherwise.
  ///
  /// Returns whether it was performed. False means the notification is no longer
  /// held — it was dismissed or aged out — which the watch cannot be told about,
  /// so it is only worth logging.
  bool performNotificationAction(int id, int actionIndex, String? replyText);

  /// Clears a notification from the phone's shade, as swiping it away would.
  bool dismissNotification(int id);

  /// Apps with a launcher entry, for the blocklist picker.
  ///
  /// Backed by a `<queries>` MAIN/LAUNCHER intent filter rather than
  /// QUERY_ALL_PACKAGES, which is a Play-restricted permission whose mere
  /// presence blocks upload.
  List<InstalledAppMsg> listLaunchableApps();

  /// Takes the process-wide lease on the watch's radio, or returns false when
  /// something else holds it.
  ///
  /// Native because the contenders live in DIFFERENT Flutter engines — the UI
  /// isolate and the headless forwarder each get their own
  /// `FlutterBluePlusPlugin` instance with its own GATT map — and no Dart mutex
  /// can span them. [ttlMillis] bounds the damage from an isolate that dies
  /// holding it.
  bool acquireRadio(String address, String owner, int ttlMillis);

  /// Announces that [owner] wants a lease somebody else holds.
  ///
  /// Grants nothing. It makes the holder's next [renewRadio] fail, which is how
  /// an indefinitely-held lease — notification forwarding holds one for as long
  /// as the watch is in range — is given up. The caller then retries
  /// [acquireRadio].
  void requestRadio(String address, String owner);

  /// Extends a held lease. False when it has expired, been taken, or somebody
  /// else has asked for it.
  bool renewRadio(String address, String owner);

  /// Releases a lease. A no-op when [owner] is not the holder, so a late
  /// release cannot cancel somebody else's work.
  void releaseRadio(String address, String owner);

  /// Who holds the lease on [address], for diagnostics. Null when it is free.
  String? radioOwner(String address);
}

/// Flutter API, called on whichever engine registered it.
@FlutterApi()
abstract class NotificationListenerFlutterApi {
  /// At least one notification is waiting in the native buffer.
  ///
  /// Carries no payload: the buffer is drained with
  /// [NotificationListenerHostApi.takePendingNotifications] so a burst
  /// collapses into one wake-up and one drain.
  void onNotificationsPending();
}
