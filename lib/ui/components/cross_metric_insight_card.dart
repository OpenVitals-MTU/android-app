import 'package:flutter/material.dart';

import '../../domain/insights/cross_metric_insights.dart';
import '../../l10n/app_localizations.dart';
import 'ov_card.dart';

/// Port of the Kotlin `CrossMetricInsightCard`: how two metrics moved together
/// over the period, as a correlation with a plain-language reading.

/// Kotlin `signedPercent`.
String signedPercent(int value) {
  if (value > 0) return '+${value.abs()}%';
  if (value < 0) return '-${value.abs()}%';
  return '0%';
}

class CrossMetricInsightCard extends StatelessWidget {
  const CrossMetricInsightCard({
    super.key,
    required this.insight,
    required this.title,
    required this.positiveMessage,
    required this.negativeMessage,
    required this.neutralMessage,
    required this.accentColor,
  });

  final CrossMetricInsight insight;
  final String title;
  final String positiveMessage;
  final String negativeMessage;
  final String neutralMessage;
  final Color accentColor;

  /// A weak correlation reads as "no clear pattern" whatever its sign — the
  /// direction is only trusted once the link is strong enough.
  bool get _isWeak => insight.strength == CrossMetricStrength.weak;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final message = _isWeak
        ? neutralMessage
        : switch (insight.direction) {
            CrossMetricDirection.positive => positiveMessage,
            CrossMetricDirection.negative => negativeMessage,
            CrossMetricDirection.neutral => neutralMessage,
          };
    final relationship = _isWeak
        ? l10n.crossMetricWeakLink
        : switch (insight.direction) {
            CrossMetricDirection.positive => l10n.crossMetricPositiveLink,
            CrossMetricDirection.negative => l10n.crossMetricNegativeLink,
            CrossMetricDirection.neutral => l10n.crossMetricWeakLink,
          };
    final icon = _isWeak
        ? Icons.trending_flat
        : switch (insight.direction) {
            CrossMetricDirection.positive => Icons.trending_up,
            CrossMetricDirection.negative => Icons.trending_down,
            CrossMetricDirection.neutral => Icons.trending_flat,
          };

    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: const BorderRadius.all(Radius.circular(12)),
        border: Border.all(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.7),
        ),
      ),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Row(
                      children: [
                        Icon(icon, size: 24, color: accentColor),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                title,
                                style: theme.textTheme.titleSmall
                                    ?.copyWith(fontWeight: FontWeight.w600),
                              ),
                              Text(
                                relationship,
                                style: theme.textTheme.bodySmall?.copyWith(
                                    color: theme.colorScheme.onSurfaceVariant),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    l10n.crossMetricCorrelation(
                      signedPercent((insight.correlation * 100.0).round()),
                    ),
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.onSurface,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                message,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 8),
              Text(
                l10n.crossMetricPairedDays(insight.pairedDays),
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
