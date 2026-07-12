import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/result/result.dart';
import '../../di/providers.dart';
import '../../l10n/app_localizations.dart';

/// A "Manage data sources" link that opens Health Connect's data-source /
/// permission settings. Port of the Kotlin `DataSourceEducationItem`
/// (`DataSourceAttribution.kt`), which wires the button to
/// `openHealthConnectPermissionSettings`. The Flutter side already exposes that
/// intent end-to-end via [HealthRepository.openHealthConnectSettings].
class DataSourceEducationItem extends ConsumerWidget {
  const DataSourceEducationItem({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Align(
        alignment: AlignmentDirectional.centerStart,
        child: TextButton.icon(
          onPressed: () async => (await ref
                  .read(healthRepositoryProvider)
                  .openHealthConnectSettings())
              .orThrow(),
          icon: const Icon(Icons.info_outline, size: 18),
          label: Text(l10n.healthConnectDataSourceManage),
        ),
      ),
    );
  }
}
