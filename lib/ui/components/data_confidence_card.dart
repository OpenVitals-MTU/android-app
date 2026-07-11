import 'package:flutter/material.dart';

import '../../domain/insights/data_confidence.dart';
import '../../l10n/app_localizations.dart';
import 'ov_card.dart';

/// Port of the Kotlin `DataConfidenceCard.kt`: how much this period's numbers
/// can be trusted — coverage, sample count, source, value kind, and up to three
/// caveats.

/// Kotlin `displaySourceName`: turns a Health Connect package name into
/// something a person recognizes.
String displaySourceName(String packageName) {
  if (packageName.contains('samsung')) return 'Samsung Health';
  if (packageName.contains('fitbit')) return 'Fitbit';
  if (packageName.contains('opentracks')) return 'OpenTracks';
  if (packageName.contains('strava')) return 'Strava';
  if (packageName.contains('polar')) return 'Polar';
  if (packageName.contains('google.android.apps.fitness')) return 'Google Fit';
  final tail = packageName.split('.').last;
  if (tail.isEmpty) return packageName;
  return tail[0].toUpperCase() + tail.substring(1);
}

String dataConfidenceLevelText(DataConfidenceLevel level, AppLocalizations l10n) =>
    switch (level) {
      DataConfidenceLevel.high => l10n.dataConfidenceHigh,
      DataConfidenceLevel.medium => l10n.dataConfidenceMedium,
      DataConfidenceLevel.low => l10n.dataConfidenceLow,
    };

String dataSourceText(DataConfidence confidence, AppLocalizations l10n) =>
    switch (confidence.sourceConsistency) {
      DataSourceConsistency.notAvailable => l10n.dataConfidenceSourceUnavailable,
      DataSourceConsistency.singleSource =>
        l10n.dataConfidenceSourceSingle(displaySourceName(confidence.sources.first)),
      DataSourceConsistency.mixedSources => l10n.dataConfidenceSourceMixed(
          confidence.sources.take(3).map(displaySourceName).join(', '),
        ),
    };

String dataValueKindText(DataValueKind kind, AppLocalizations l10n) =>
    switch (kind) {
      DataValueKind.measured => l10n.dataConfidenceKindMeasured,
      DataValueKind.aggregated => l10n.dataConfidenceKindAggregated,
      DataValueKind.calculated => l10n.dataConfidenceKindCalculated,
      DataValueKind.estimated => l10n.dataConfidenceKindEstimated,
      DataValueKind.mixed => l10n.dataConfidenceKindMixed,
    };

String dataWarningText(DataConfidenceWarning warning, AppLocalizations l10n) =>
    switch (warning) {
      DataConfidenceWarning.lowCoverage => l10n.dataConfidenceWarningLowCoverage,
      DataConfidenceWarning.sparseData => l10n.dataConfidenceWarningSparse,
      DataConfidenceWarning.mixedSources =>
        l10n.dataConfidenceWarningMixedSources,
      DataConfidenceWarning.manualEntries => l10n.dataConfidenceWarningManual,
      DataConfidenceWarning.calculatedValue =>
        l10n.dataConfidenceWarningCalculated,
      DataConfidenceWarning.noSourceDetails =>
        l10n.dataConfidenceWarningNoSources,
    };

class DataConfidenceCard extends StatelessWidget {
  const DataConfidenceCard({
    super.key,
    required this.confidence,
    required this.accentColor,
  });

  final DataConfidence confidence;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final levelColor = switch (confidence.level) {
      DataConfidenceLevel.high => accentColor,
      DataConfidenceLevel.medium => theme.colorScheme.tertiary,
      DataConfidenceLevel.low => theme.colorScheme.error,
    };
    final levelText = dataConfidenceLevelText(confidence.level, l10n);
    final coverageText = l10n.dataConfidenceCoverage(
      confidence.trackedDays,
      confidence.expectedDays,
      confidence.coveragePercent,
    );
    final samplesText = l10n.dataConfidenceSamples(confidence.sampleCount);
    final sourceText = dataSourceText(confidence, l10n);
    final valueKindText = dataValueKindText(confidence.valueKind, l10n);
    // Kotlin shows at most three caveats; more would bury the card.
    final warnings = [
      for (final warning in confidence.warnings.take(3))
        dataWarningText(warning, l10n),
    ];

    final bodyStyle = theme.textTheme.bodyMedium
        ?.copyWith(color: theme.colorScheme.onSurfaceVariant);

    return Semantics(
      container: true,
      label: '${l10n.dataConfidenceTitle}: $levelText',
      value: [coverageText, samplesText, sourceText, valueKindText, ...warnings]
          .join('. '),
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.all(Radius.circular(12)),
          border: Border.all(color: levelColor.withValues(alpha: 0.25)),
        ),
        child: OpenVitalsCard(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.verified_outlined, size: 22, color: levelColor),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            l10n.dataConfidenceTitle,
                            style: theme.textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w600,
                              color: theme.colorScheme.onSurface,
                            ),
                          ),
                          Text(
                            levelText,
                            style: theme.textTheme.labelLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: levelColor,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(coverageText, style: bodyStyle),
                Text(samplesText, style: bodyStyle),
                Text(sourceText, style: bodyStyle),
                Text(valueKindText, style: bodyStyle),
                for (final warning in warnings) ...[
                  const SizedBox(height: 6),
                  Text(
                    '- $warning',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
