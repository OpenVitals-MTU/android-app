import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../di/providers.dart';
import '../../../domain/model/ble_sensor_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/theme/app_colors.dart';
import '../../../ui/components/metric_stat_card.dart';
import '../../settings/application/garmin_sync_view_model.dart';
import '../../settings/presentation/watch_common.dart';

/// The carousel id for a watch's tile. Namespaced so it can never collide with
/// a metric's enum name.
String watchTileId(String deviceId) => 'watch_$deviceId';

/// The device id inside a [watchTileId], or null when it is not a watch tile.
String? watchTileDeviceId(String tileId) =>
    tileId.startsWith('watch_') ? tileId.substring('watch_'.length) : null;

/// How long a watch may go unsynced before the tile says so.
///
/// A watch that has stopped syncing is silently missing data, and nothing else
/// on the summary would ever mention it — which is the whole reason this tile
/// earns a place among the health metrics.
const Duration watchStaleAfter = Duration(days: 1);

/// One paired watch on the summary.
///
/// Two tap targets: the Sync control acts in place, everywhere else opens the
/// device view. Sync is the only watch action allowed outside that view, and it
/// shares the device view's state rather than reimplementing it — same
/// disabled-while-busy rule, same progress — so the two cannot drift.
class WatchSummaryTile extends ConsumerWidget {
  const WatchSummaryTile({required this.device, super.key});

  final BleSensorDevice device;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final sync = ref.watch(garminSyncViewModelProvider);
    final syncingThis = sync.isSyncingDevice(device.id);
    final syncedAt = device.lastSyncedAt?.toLocal();
    final staleDays =
        syncedAt == null ? null : watchDaysSince(syncedAt, DateTime.now());
    final stale = staleDays != null && staleDays >= watchStaleAfter.inDays;
    final battery = device.batteryPercent;

    final String value;
    final String? unit;
    final String? subtitle;
    if (syncingThis) {
      value = l10n.settingsWatchSyncing;
      unit = null;
      subtitle = sync.filesTotal > 0
          ? l10n.settingsWatchSyncDownloading(sync.filesDone, sync.filesTotal)
          : null;
    } else if (stale) {
      value = l10n.settingsWatchStaleDays(staleDays);
      unit = null;
      subtitle = null;
    } else {
      // Battery is the headline: it is the one number that predicts whether the
      // next sync works at all. Body Battery would be the wrong choice here —
      // that is health data, and this tile is about the device.
      value = battery != null ? '$battery' : '—';
      unit = battery != null ? '%' : null;
      subtitle = syncedAt == null
          ? l10n.settingsWatchNeverSynced
          : l10n.settingsWatchLastSynced(formatWatchSyncTime(context, syncedAt));
    }

    return MetricStatCard(
      title: device.displayName,
      value: value,
      unit: unit,
      subtitle: subtitle,
      icon: Icons.watch_outlined,
      accentColor: stale ? theme.colorScheme.error : AppColors.workout,
      progress: battery != null && !stale && !syncingThis
          ? battery / 100
          : null,
      onTap: () => context.push(AppRoutes.watchDeviceLocation(device.id)),
      trailing: IconButton(
        // A full touch target, not a bare glyph: a small control inside a
        // tappable card is a mis-tap generator, and the mis-tap here navigates.
        onPressed: sync.isSyncing
            ? null
            : () => ref
                .read(garminSyncViewModelProvider.notifier)
                .syncDevice(device.id),
        tooltip: l10n.settingsWatchSyncNow,
        visualDensity: VisualDensity.compact,
        icon: syncingThis
            ? const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : const Icon(Icons.sync, size: 18),
      ),
    );
  }
}

/// The watches that should appear on the summary, in registry order.
///
/// Watches only: a chest strap has no data of its own and no screen worth
/// opening, and four paired sensors would push the real metrics off the
/// carousel. The top-bar battery action already covers sensors.
/// Watches the reactive registry, not a one-shot read: the tile has to notice a
/// finished sync (a new `lastSyncedAt`) and a watch being paired or removed.
final summaryWatchesProvider = Provider<List<BleSensorDevice>>((ref) {
  final devices = ref.watch(bleDevicesProvider).value ?? const [];
  return [for (final d in devices) if (d.isWatch) d];
});
