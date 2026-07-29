import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../l10n/app_localizations.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../application/watch_notifications_view_model.dart';

/// Which apps may send notifications to the watch.
///
/// A BLOCKLIST: every app is on until it is switched off. An allow-list would
/// contradict what the master switch says it does, and would leave a
/// newly-installed messaging app silent for no visible reason.
///
/// The list is every app with a launcher entry, resolved through a `<queries>`
/// MAIN/LAUNCHER declaration rather than QUERY_ALL_PACKAGES — the latter is a
/// Play-restricted permission whose mere presence blocks upload.
class WatchNotificationAppsScreen extends ConsumerStatefulWidget {
  const WatchNotificationAppsScreen({super.key});

  @override
  ConsumerState<WatchNotificationAppsScreen> createState() =>
      _WatchNotificationAppsScreenState();
}

class _WatchNotificationAppsScreenState
    extends ConsumerState<WatchNotificationAppsScreen> {
  @override
  void initState() {
    super.initState();
    // Resolving every installed app's label is a platform round trip per app,
    // so it happens when this screen is opened rather than when the card that
    // links to it is built.
    Future.microtask(
      () => ref.read(watchNotificationsViewModelProvider.notifier).loadApps(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final state = ref.watch(watchNotificationsViewModelProvider);
    final viewModel = ref.read(watchNotificationsViewModelProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settingsWatchNotificationsAppsTitle)),
      body: state.loadingApps
          ? const Center(child: CircularProgressIndicator())
          : state.apps.isEmpty
              ? Center(child: Text(l10n.settingsWatchNotificationsAppsEmpty))
              : ListView.builder(
                  padding: screenScrollPadding(context),
                  itemCount: state.apps.length + 1,
                  itemBuilder: (context, index) {
                    if (index == 0) {
                      return Padding(
                        padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                        child: Text(
                          l10n.settingsWatchNotificationsAppsIntro,
                          style: theme.textTheme.bodySmall,
                        ),
                      );
                    }
                    final app = state.apps[index - 1];
                    return SwitchListTile(
                      title: Text(app.label),
                      subtitle: Text(
                        app.packageName,
                        style: theme.textTheme.bodySmall,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      // Inverted on purpose: the switch reads "sends to the
                      // watch", which is what the user is deciding, while the
                      // stored state is the set of apps that do not.
                      value: !app.blocked,
                      onChanged: (sends) => viewModel.setBlocked(
                        app.packageName,
                        blocked: !sends,
                      ),
                    );
                  },
                ),
    );
  }
}
