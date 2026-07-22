import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/source/sensors/garmin/garmin_settings_link.dart';
import '../../../data/source/sensors/garmin/garmin_settings_screen.dart';
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

/// One open settings link per watch, shared by every screen browsing it.
///
/// The link is the expensive part — a connection plus a handshake — so walking
/// from the Alarms list into one alarm must not pay for it twice. It stays
/// alive while any screen is watching and closes when the last one leaves,
/// which is what `autoDispose` gives: a watch should not be left holding a
/// connection open because somebody backed out of a menu.
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
    ref.onDispose(link.close);
    // Held while the screen is open; without this the link would be torn down
    // between a read and the change that follows it.
    ref.keepAlive();
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

/// Applies a change and re-reads the screen it belongs to.
Future<WatchSettingsChangeResult> setWatchSwitch(
  WidgetRef ref,
  WatchSettingsTarget target,
  int entryId,
  bool value,
) async {
  try {
    final link =
        await ref.read(watchSettingsLinkProvider(target.deviceId).future);
    final ok = await link.setSwitch(
      screenId: target.screenId,
      entryId: entryId,
      value: value,
    );
    ref.invalidate(watchSettingsScreenProvider(target));
    return switch (ok) {
      true => WatchSettingsChangeResult.applied,
      false => WatchSettingsChangeResult.refused,
      null => WatchSettingsChangeResult.unanswered,
    };
  } catch (error) {
    debugPrint('[GARMIN-SETTINGS] change failed: $error');
    return WatchSettingsChangeResult.unanswered;
  }
}
