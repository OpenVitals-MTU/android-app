import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../devices/garmin/garmin_log.dart';
import '../../../devices/garmin/garmin_settings_link.dart';
import '../../../devices/garmin/garmin_settings_model.dart';
import '../../../di/providers.dart';

/// Identifies which screen of which watch is open.
@immutable
class WatchSettingsTarget {
  const WatchSettingsTarget({required this.deviceId, required this.screenId});

  final String deviceId;
  final int screenId;

  @override
  bool operator ==(Object other) =>
      other is WatchSettingsTarget &&
      other.deviceId == deviceId &&
      other.screenId == screenId;

  @override
  int get hashCode => Object.hash(deviceId, screenId);
}

/// How long a link outlives the last screen watching it.
///
/// Long enough to walk from the Alarms list into one alarm without paying for a
/// second handshake, short enough that backing out of settings gives the radio
/// back. There is only ONE link to a watch: while this is held, a file sync
/// cannot connect.
const Duration _linkGrace = Duration(seconds: 20);

/// Which watches currently have a settings link open.
///
/// A watch has one radio, so this is not a cache — it is the record of who
/// holds it. A file sync consults it to take the link back rather than opening
/// a second one alongside, which is what silently wedged Sync: after browsing
/// Alarms the settings link stayed up, and the sync's connect never returned.
///
/// Scoped to the container rather than the library. As a top-level map it
/// outlived every `ProviderContainer` that filled it, so a widget test that
/// opened a settings screen leaked a link into the next one with no override
/// able to reach it.
class WatchSettingsLinks {
  final Map<String, GarminSettingsLink> _open = {};

  void register(String deviceId, GarminSettingsLink link) =>
      _open[deviceId] = link;

  void forget(String deviceId) => _open.remove(deviceId);

  /// Whether a link is being held for [deviceId].
  bool isHeld(String deviceId) => _open.containsKey(deviceId);

  /// Closes any link held on [deviceId], and waits for it to be gone.
  ///
  /// Awaited rather than fired off, because the caller wants the radio.
  Future<void> release(String deviceId) async {
    final link = _open.remove(deviceId);
    if (link == null) return;
    garminLog('[GARMIN-SETTINGS] releasing the link for $deviceId');
    await link.close();
  }
}

final watchSettingsLinksProvider =
    Provider<WatchSettingsLinks>((ref) => WatchSettingsLinks());

/// One open settings link per watch, shared by every screen browsing it.
///
/// The link is the expensive part — a connection plus a handshake — so walking
/// from the Alarms list into one alarm must not pay for it twice. It outlives
/// the last screen by [_linkGrace] and then goes: a watch should not be left
/// holding a connection open because somebody backed out of a menu.
final watchSettingsLinkProvider =
    FutureProvider.autoDispose.family<GarminSettingsLink, String>(
  (ref, deviceId) async {
    final device = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((d) => d.id == deviceId)
        .firstOrNull;
    if (device == null || !device.isWatch) {
      throw StateError('Not a paired watch: $deviceId');
    }
    final phone = ref.read(phoneIdentityProvider);
    final link = await GarminSettingsLink.open(
      address: device.address,
      phoneName: phone.bluetoothName,
      manufacturer: phone.manufacturer,
      model: phone.model,
    );
    final links = ref.read(watchSettingsLinksProvider)
      ..register(deviceId, link);
    ref.onDispose(() {
      links.forget(deviceId);
      link.close();
    });

    // Kept alive across the gap between one screen letting go and the next
    // subscribing — but on a timer, not forever. A permanent keepAlive() held
    // the radio for the rest of the session, so a later Sync had nothing to
    // connect with and hung with no output at all.
    final keep = ref.keepAlive();
    Timer? expiry;
    ref.onCancel(() => expiry = Timer(_linkGrace, keep.close));
    ref.onResume(() => expiry?.cancel());
    ref.onDispose(() => expiry?.cancel());
    return link;
  },
);

/// One screen of the watch's settings, as it currently stands.
///
/// Invalidate it to re-read. Every change does exactly that rather than
/// assuming it worked: the watch owns these settings, and showing the value we
/// asked for instead of the one it holds would quietly disagree with the wrist.
final watchSettingsScreenProvider = FutureProvider.autoDispose
    .family<GarminSettingsScreen?, WatchSettingsTarget>((ref, target) async {
  final link = await ref.watch(watchSettingsLinkProvider(target.deviceId).future);
  return link.screen(target.screenId);
});

/// What the watch made of a change.
enum WatchSettingsChangeResult {
  applied,

  /// It answered, and said no.
  refused,

  /// It never answered. Deliberately distinct from [refused] — the request may
  /// or may not have landed, and reporting a lost message as a rejection would
  /// be a guess presented as fact.
  unanswered,
}

/// Chooses one of the options the WATCH supplied for an entry.
///
/// [index] is a position in that list, never an ordinal this app decided.
Future<WatchSettingsChangeResult> setWatchOption(
  WidgetRef ref,
  WatchSettingsTarget target,
  int entryId,
  int index,
) =>
    _change(
      ref,
      target,
      (link) => link.setOption(
        screenId: target.screenId,
        entryId: entryId,
        index: index,
      ),
    );

/// Sets a time of day, as seconds since midnight.
Future<WatchSettingsChangeResult> setWatchTime(
  WidgetRef ref,
  WatchSettingsTarget target,
  int entryId,
  Duration sinceMidnight,
) =>
    _change(
      ref,
      target,
      (link) => link.setTime(
        screenId: target.screenId,
        entryId: entryId,
        sinceMidnight: sinceMidnight,
      ),
    );

/// Activates a row that deletes something, and re-reads what is left.
///
/// A refusal is reported as a refusal rather than smoothed over, because the
/// alternative is telling somebody their alarm is gone when it is not.
Future<WatchSettingsChangeResult> deleteWatchEntry(
  WidgetRef ref,
  WatchSettingsTarget target,
  int entryId,
) =>
    _change(
      ref,
      target,
      (link) => link.delete(screenId: target.screenId, entryId: entryId),
      // A delete does not change a value on this screen — it removes the thing
      // the screen describes, and the LIST that pointed here is now stale too.
      // Re-reading only this screen left the deleted alarm sitting in the list
      // behind it, and the watch answered the dead screen's id with its
      // parent's contents.
      invalidateEverything: true,
    );

/// Applies a change and re-reads the screen it belongs to.
Future<WatchSettingsChangeResult> setWatchSwitch(
  WidgetRef ref,
  WatchSettingsTarget target,
  int entryId,
  bool value,
) =>
    _change(
      ref,
      target,
      (link) => link.setSwitch(
        screenId: target.screenId,
        entryId: entryId,
        value: value,
      ),
    );

/// The shape every change shares: apply, then RE-READ.
///
/// Re-reading is not belt and braces. The watch owns these settings and can
/// clamp, round or ignore what it is asked; showing the value we requested
/// rather than the one it now holds would quietly disagree with the wrist.
Future<WatchSettingsChangeResult> _change(
  WidgetRef ref,
  WatchSettingsTarget target,
  Future<bool?> Function(GarminSettingsLink) apply, {
  bool invalidateEverything = false,
}) async {
  try {
    final link =
        await ref.read(watchSettingsLinkProvider(target.deviceId).future);
    final ok = await apply(link);
    if (invalidateEverything) {
      // Every screen of this watch at once. Only the ones still on screen
      // actually re-read; the rest are already gone.
      ref.invalidate(watchSettingsScreenProvider);
    } else {
      ref.invalidate(watchSettingsScreenProvider(target));
    }
    return switch (ok) {
      true => WatchSettingsChangeResult.applied,
      false => WatchSettingsChangeResult.refused,
      null => WatchSettingsChangeResult.unanswered,
    };
  } catch (error) {
    garminLog('[GARMIN-SETTINGS] change failed: $error');
    return WatchSettingsChangeResult.unanswered;
  }
}
