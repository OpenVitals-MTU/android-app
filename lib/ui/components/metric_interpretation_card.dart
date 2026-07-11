import 'package:flutter/material.dart';

import '../../domain/insights/metric_interpretations.dart';
import 'ov_card.dart';

/// Port of the Kotlin `MetricInterpretationCard`: what a metric's value means,
/// with a severity-tinted border and an explicit "this is not medical advice"
/// source line.
class MetricInterpretationCard extends StatelessWidget {
  const MetricInterpretationCard({
    super.key,
    required this.title,
    required this.status,
    required this.body,
    required this.source,
    required this.icon,
    required this.accentColor,
    required this.severity,
  });

  final String title;
  final String status;
  final String body;
  final String source;
  final IconData icon;
  final Color accentColor;
  final InterpretationSeverity severity;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final severityColor = switch (severity) {
      InterpretationSeverity.positive => accentColor,
      InterpretationSeverity.info => theme.colorScheme.onSurfaceVariant,
      InterpretationSeverity.caution => theme.colorScheme.tertiary,
      InterpretationSeverity.alert => theme.colorScheme.error,
    };

    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: const BorderRadius.all(Radius.circular(12)),
        border: Border.all(color: severityColor.withValues(alpha: 0.45)),
      ),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(icon, size: 24, color: severityColor),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: theme.textTheme.titleSmall?.copyWith(
                            fontWeight: FontWeight.w600,
                            color: theme.colorScheme.onSurface,
                          ),
                        ),
                        Text(
                          status,
                          style: theme.textTheme.labelLarge?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: severityColor,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                body,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 8),
              Text(
                source,
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
