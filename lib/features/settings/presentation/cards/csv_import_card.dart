import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../navigation/app_routes.dart';
import '../../../../ui/components/ov_card.dart';
import 'route_import_card.dart';

/// The Settings "Data Importers" CSV card.
///
/// Deliberately has NO view-model: it holds no repository and does no work — it
/// opens a route. A `Notifier` here would add a file and no information. All the
/// state lives in `CsvImportViewModel`, on the screen it pushes.
class CsvImportCard extends ConsumerWidget {
  const CsvImportCard({super.key, this.onNavigate});

  /// Test seam for navigation, matching `FitImportCard.onNavigateToEntry`.
  final void Function(BuildContext context)? onNavigate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              RouteImportCardHeader(
                icon: Icons.table_chart_outlined,
                title: l10n.settingsCsvImportTitle,
                body: l10n.settingsCsvImportBody,
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () {
                    final navigate = onNavigate ??
                        (BuildContext ctx) =>
                            ctx.push(AppRoutes.settingsCsvImport);
                    navigate(context);
                  },
                  icon: const Icon(Icons.description_outlined, size: 18),
                  label: Text(l10n.settingsCsvImportAction),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
