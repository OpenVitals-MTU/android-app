import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../ui/components/metric_card.dart';
import '../../../../ui/components/ov_card.dart';
import '../../application/health_connect_sources.dart';

/// Diagnostics-only card listing the apps/devices contributing to Health
/// Connect over the last week (heart rate + sleep). Its purpose is the WearOS
/// viability check: after pairing a watch, this is where its data shows up (or
/// does not) — attributed to the bridging app's package.
///
/// English-only on purpose, like the other diagnostics-gated surfaces: it is
/// dev-facing and never reaches a shipped locale. Reachable only in
/// diagnostics-enabled builds (the section is gated on `kDiagnosticsEnabled`).
class HealthConnectSourcesCard extends ConsumerWidget {
  const HealthConnectSourcesCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final sources = ref.watch(healthConnectSourcesProvider);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Icon(
                      Icons.hub_outlined,
                      size: 20,
                      color: theme.colorScheme.primary,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Health Connect sources',
                          style: theme.textTheme.titleSmall,
                        ),
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            'Apps and devices that wrote heart-rate or sleep '
                            'data in the last 7 days. After pairing a watch, '
                            'its data appears here under the bridging app.',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Refresh',
                    visualDensity: VisualDensity.compact,
                    icon: const Icon(Icons.refresh, size: 18),
                    onPressed: () =>
                        ref.invalidate(healthConnectSourcesProvider),
                  ),
                ],
              ),
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: sources.when(
                  loading: () => const Center(
                    child: Padding(
                      padding: EdgeInsets.all(8),
                      child: SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    ),
                  ),
                  error: (error, _) => Text(
                    'Could not read Health Connect: $error',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error),
                  ),
                  data: (list) => list.isEmpty
                      ? Text(
                          'No heart-rate or sleep data seen yet. Check read '
                          'permissions, or pair a watch and let it sync.',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        )
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            for (final source in list)
                              _SourceRow(source: source),
                          ],
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SourceRow extends StatelessWidget {
  const _SourceRow({required this.source});

  final HealthConnectSource source;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  source.displayName,
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
              SourceChip(source: source.package),
            ],
          ),
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Text(
              '${source.recordCount} records · '
              '${source.metrics.join(", ")} · '
              'last ${source.lastSeen.toLocal()}',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
        ],
      ),
    );
  }
}
