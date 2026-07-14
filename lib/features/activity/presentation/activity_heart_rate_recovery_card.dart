import 'package:flutter/material.dart';

import '../../../domain/insights/heart_rate_recovery.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';

/// How far the heart rate fell once the effort stopped, for one workout.
///
/// The card's job is as much to say what it does NOT know as what it does. A watch that
/// reverts to a reading a minute the moment a workout ends cannot produce the ten- and
/// thirty-second marks, and those come back blank here rather than interpolated — with a
/// line underneath saying why, because a blank with no explanation reads as a bug.
class ActivityHeartRateRecoveryCard extends StatelessWidget {
  const ActivityHeartRateRecoveryCard({super.key, required this.reading});

  final HeartRateRecoveryReading reading;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final accent = _accentColor(reading.quality, scheme);

    final headlineDrop = reading.headlineDropBpm;
    final peakBpm = reading.peakBpm;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.trending_down, color: accent),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    l10n.heartRateRecoveryTitle,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                if (peakBpm != null)
                  Text(
                    l10n.heartRateRecoveryPeak('$peakBpm'),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: scheme.onSurfaceVariant),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              headlineDrop != null
                  ? l10n.heartRateRecoveryHeadline('$headlineDrop')
                  : l10n.heartRateRecoveryHeadlineUnavailable,
              style: theme.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w600,
                color: headlineDrop != null ? accent : scheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 12),
            _MarksRow(reading: reading),
            ..._explanations(l10n).map(
              (text) => Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Text(
                  text,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Why the reading looks the way it does. Ordered so the most consequential comes
  /// first: what makes the number meaningless, then what makes it uncomparable, then
  /// what merely makes it rough.
  List<String> _explanations(AppLocalizations l10n) {
    final issues = reading.issues;
    return [
      if (issues.contains(HeartRateRecoveryIssue.noRecoverySamples))
        l10n.heartRateRecoveryNoRecoverySamples,
      if (issues.contains(HeartRateRecoveryIssue.cooldownBeforeStop))
        l10n.heartRateRecoveryCooldownBeforeStop,
      if (issues.contains(HeartRateRecoveryIssue.effortNotVigorous))
        l10n.heartRateRecoveryEffortNotVigorous,
      if (issues.contains(HeartRateRecoveryIssue.submaximalEffort))
        l10n.heartRateRecoverySubmaximalEffort,
      if (issues.contains(HeartRateRecoveryIssue.coarseSampling))
        l10n.heartRateRecoveryCoarseSampling,
      if (issues.contains(HeartRateRecoveryIssue.peakWindowWidened))
        l10n.heartRateRecoveryPeakWindowWidened,
      if (issues.contains(HeartRateRecoveryIssue.peakFromSingleSample))
        l10n.heartRateRecoveryPeakFromSingleSample,
      if (issues.contains(HeartRateRecoveryIssue.unknownMaxHeartRate))
        l10n.heartRateRecoveryUnknownMaxHeartRate,
    ];
  }
}

/// The seven marks. A mark with no reading behind it shows a dash, not a number.
class _MarksRow extends StatelessWidget {
  const _MarksRow({required this.reading});

  final HeartRateRecoveryReading reading;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return Row(
      children: [
        for (final mark in reading.marks)
          Expanded(
            child: Column(
              children: [
                Text(
                  _markLabel(l10n, mark.offset),
                  style: theme.textTheme.labelSmall
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
                const SizedBox(height: 4),
                Text(
                  mark.dropBpm != null ? '${mark.dropBpm}' : '—',
                  semanticsLabel:
                      mark.dropBpm == null ? l10n.heartRateRecoveryNoSample : null,
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w600,
                    color: mark.dropBpm != null
                        ? scheme.onSurface
                        : scheme.onSurfaceVariant.withValues(alpha: 0.5),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

String _markLabel(AppLocalizations l10n, Duration offset) =>
    offset.inSeconds < 60
        ? l10n.heartRateRecoveryMarkSeconds('${offset.inSeconds}')
        : l10n.heartRateRecoveryMarkMinutes('${offset.inMinutes}');

Color _accentColor(HeartRateRecoveryQuality quality, ColorScheme scheme) =>
    switch (quality) {
      HeartRateRecoveryQuality.clean => scheme.primary,
      HeartRateRecoveryQuality.approximate => scheme.primary,
      HeartRateRecoveryQuality.notComparable => scheme.tertiary,
      HeartRateRecoveryQuality.invalid => scheme.onSurfaceVariant,
      HeartRateRecoveryQuality.noData => scheme.onSurfaceVariant,
    };
