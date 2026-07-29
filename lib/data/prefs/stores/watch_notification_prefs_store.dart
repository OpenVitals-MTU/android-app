import '../prefs_store.dart';

/// Which phone notifications are mirrored to a paired Garmin watch.
///
/// A **blocklist**, not an allow-list: the feature forwards everything the
/// structural filter lets through, and the user silences the apps they do not
/// want. An allow-list would quietly contradict what the switch says it does,
/// and would leave a newly-installed messaging app silent for no visible
/// reason.
///
/// The disclosure flag is stored here rather than derived from [enabled]
/// because the two are not the same: consent is given once and remembered, and
/// switching the feature off and on again must not re-prompt.
///
/// Everything here is mirrored to the native side by the view-model that writes
/// it — the filter runs before any Flutter engine exists, so it cannot read
/// these keys itself.
class WatchNotificationPrefsStore extends PrefsStore {
  const WatchNotificationPrefsStore(super.prefs);

  static const String _keyEnabled = 'garmin_notifications_enabled';
  static const String _keyBlocked = 'garmin_notifications_blocked_packages';
  static const String _keyDisclosureAccepted =
      'garmin_notifications_disclosure_accepted';

  bool get enabled => prefs.getBool(_keyEnabled) ?? false;

  set enabled(bool value) => putBool(_keyEnabled, value);

  /// Packages the user has silenced, as a set for the membership test the
  /// filter and the picker both do.
  Set<String> get blockedPackages =>
      (prefs.getStringList(_keyBlocked) ?? const <String>[]).toSet();

  set blockedPackages(Set<String> value) =>
      putStringList(_keyBlocked, value.toList()..sort());

  void setBlocked(String packageName, {required bool blocked}) {
    final next = blockedPackages;
    if (blocked) {
      next.add(packageName);
    } else {
      next.remove(packageName);
    }
    blockedPackages = next;
  }

  /// Whether the user has been shown what notification access means and agreed
  /// to it. Required by Google Play before the permission is requested, and
  /// remembered so turning the feature off and on again does not re-prompt.
  bool get disclosureAccepted =>
      prefs.getBool(_keyDisclosureAccepted) ?? false;

  set disclosureAccepted(bool value) => putBool(_keyDisclosureAccepted, value);
}
