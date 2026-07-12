import 'package:flutter/material.dart';

import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/activity_splits.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import 'activity_split_distance_label.dart';

/// The splits card on the activity detail screen: one row per split (or per
/// device lap), with a bar whose length tracks the split's pace so a slow
/// kilometre is visible without reading a single number.
///
/// The header states the PROVENANCE, and the estimated case says out loud that
/// the identical pace on every row is an artefact of missing data — the whole
/// point of keeping [SplitSource] on the result.
class ActivitySplitsCard extends StatelessWidget {
  const ActivitySplitsCard({
    super.key,
    required this.splits,
    required this.formatter,
    required this.splitDistanceMeters,
    required this.slowestPaceSeconds,
    required this.fastestPaceSeconds,
  });

  final ActivitySplits splits;
  final UnitFormatter formatter;
  final double splitDistanceMeters;

  /// The activity's slowest and fastest split, in seconds per kilometre — the
  /// bar scale, folded at load time by the view-model. A ratio between splits,
  /// so the unit it is expressed in cancels out; the numbers the row PRINTS are
  /// still formatted in the user's own units.
  final double? slowestPaceSeconds;
  final double? fastestPaceSeconds;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final rows = splits.splits;
    if (rows.isEmpty) return const SizedBox.shrink();

    final unitMeters = switch (formatter.unitSystem()) {
      UnitSystem.metric => 1000.0,
      UnitSystem.imperial => 1609.344,
    };

    final (title, subtitle) = switch (splits.source) {
      SplitSource.deviceLaps => (
          l10n.activitySplitsLapsTitle,
          l10n.activitySplitsLapsBody,
        ),
      SplitSource.route || SplitSource.speedSamples => (
          l10n.activitySplitsDerivedTitle(
            splitDistanceLabel(l10n, formatter, splitDistanceMeters),
          ),
          null,
        ),
      SplitSource.estimated => (
          l10n.activitySplitsEstimatedTitle,
          l10n.activitySplitsEstimatedBody,
        ),
    };
    final columnLabel = splits.source == SplitSource.deviceLaps
        ? l10n.activitySplitsHeaderLap
        : l10n.activitySplitsHeaderSplit;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.timeline_outlined,
                  color: theme.colorScheme.onSurfaceVariant,
                  size: 20,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(title, style: theme.textTheme.titleMedium),
                ),
              ],
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
            const SizedBox(height: 12),
            for (final split in rows) ...[
              _SplitRow(
                split: split,
                columnLabel: columnLabel,
                formatter: formatter,
                unitMeters: unitMeters,
                slowestPaceSeconds: slowestPaceSeconds,
                fastestPaceSeconds: fastestPaceSeconds,
                // The estimated source gives every split the same pace: a bar
                // chart of it would be a straight line masquerading as a
                // measurement. Show the numbers, drop the bar.
                showBar: splits.source != SplitSource.estimated,
              ),
              const SizedBox(height: 10),
            ],
          ],
        ),
      ),
    );
  }
}

class _SplitRow extends StatelessWidget {
  const _SplitRow({
    required this.split,
    required this.columnLabel,
    required this.formatter,
    required this.unitMeters,
    required this.slowestPaceSeconds,
    required this.fastestPaceSeconds,
    required this.showBar,
  });

  final ActivitySplit split;
  final String columnLabel;
  final UnitFormatter formatter;
  final double unitMeters;
  final double? slowestPaceSeconds;
  final double? fastestPaceSeconds;
  final bool showBar;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final muted = theme.textTheme.bodySmall
        ?.copyWith(color: theme.colorScheme.onSurfaceVariant);

    final pace = formatter.averagePace(
      split.distanceMeters,
      split.elapsed.inMilliseconds,
    );
    final delta = split.paceDeltaSecondsPerUnit(unitMeters);

    return Semantics(
      container: true,
      label: _semanticsLabel(l10n),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              SizedBox(
                width: 24,
                child: Text(
                  '${split.index}',
                  style: theme.textTheme.titleSmall,
                ),
              ),
              Expanded(
                child: Text(
                  _distanceText(l10n),
                  style: theme.textTheme.bodyMedium,
                ),
              ),
              Text(
                formatter.duration(split.elapsed.inMilliseconds),
                style: muted,
              ),
              const SizedBox(width: 12),
              SizedBox(
                width: 92,
                child: Text(
                  pace?.text ?? '--',
                  textAlign: TextAlign.end,
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
          if (showBar) ...[
            const SizedBox(height: 4),
            Padding(
              padding: const EdgeInsets.only(left: 24),
              child: _PaceBar(
                // Per kilometre, like the scale it is measured against.
                paceSeconds: split.paceSecondsPerUnit(1000.0),
                slowestPaceSeconds: slowestPaceSeconds,
                fastestPaceSeconds: fastestPaceSeconds,
                fasterThanAverage: delta != null && delta < 0,
              ),
            ),
          ],
          const SizedBox(height: 4),
          Padding(
            padding: const EdgeInsets.only(left: 24),
            child: Text(_detailLine(delta), style: muted),
          ),
        ],
      ),
    );
  }

  String _distanceText(AppLocalizations l10n) {
    final distance = formatter.distance(split.distanceMeters).text;
    // A partial split is short ON PURPOSE — say so, or it reads as a bad fix.
    return split.isPartial ? '$distance (${l10n.activitySplitsPartial})' : distance;
  }

  /// avg HR · elevation ± · pace delta. Each piece is dropped when its datum is
  /// missing rather than rendered as a zero: a treadmill split has NO elevation,
  /// which is not the same claim as "flat".
  String _detailLine(double? delta) {
    final parts = <String>[];
    final bpm = split.averageHeartRateBpm;
    if (bpm != null) parts.add(formatter.heartRate(bpm).text);

    final gain = split.elevationGainMeters;
    final loss = split.elevationLossMeters;
    if (gain != null && loss != null) {
      parts.add(
        '↑ ${formatter.elevation(gain).text}  ↓ ${formatter.elevation(loss).text}',
      );
    }
    if (delta != null) parts.add(_formatDelta(delta));
    return parts.join('  ·  ');
  }

  String _semanticsLabel(AppLocalizations l10n) {
    final pace = formatter.averagePace(
      split.distanceMeters,
      split.elapsed.inMilliseconds,
    );
    final delta = split.paceDeltaSecondsPerUnit(unitMeters);
    final buffer = StringBuffer(
      '$columnLabel ${split.index}, '
      '${formatter.distance(split.distanceMeters).text}, '
      '${formatter.duration(split.elapsed.inMilliseconds)}',
    );
    if (pace != null) buffer.write(', ${pace.text}');
    if (delta != null && delta.round() != 0) {
      final magnitude = _formatSeconds(delta.abs());
      buffer.write(
        ', ${delta < 0 ? l10n.activitySplitsFaster(magnitude) : l10n.activitySplitsSlower(magnitude)}',
      );
    }
    return buffer.toString();
  }
}

/// `-0:08` / `+0:12` — signed minutes:seconds against the activity average.
String _formatDelta(double deltaSeconds) {
  final rounded = deltaSeconds.round();
  if (rounded == 0) return '±0:00';
  final sign = rounded < 0 ? '−' : '+';
  return '$sign${_formatSeconds(rounded.abs().toDouble())}';
}

String _formatSeconds(double seconds) {
  final total = seconds.round();
  final minutes = total ~/ 60;
  final rest = total % 60;
  return '$minutes:${rest.toString().padLeft(2, '0')}';
}

/// A pace bar: the slowest split in the activity fills the track, the fastest
/// leaves it visibly shorter. Deliberately NOT zero-based — the interesting
/// range of a run is the few percent between its fastest and slowest kilometre,
/// and a zero-based bar squashes that into invisibility. The floor is 25% of
/// the track so the fastest split still reads as a bar, not as nothing.
class _PaceBar extends StatelessWidget {
  const _PaceBar({
    required this.paceSeconds,
    required this.slowestPaceSeconds,
    required this.fastestPaceSeconds,
    required this.fasterThanAverage,
  });

  final double? paceSeconds;
  final double? slowestPaceSeconds;
  final double? fastestPaceSeconds;
  final bool fasterThanAverage;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final pace = paceSeconds;
    final slowest = slowestPaceSeconds;
    final fastest = fastestPaceSeconds;
    if (pace == null || slowest == null || fastest == null) {
      return const SizedBox(height: 6);
    }

    const minFraction = 0.25;
    final span = slowest - fastest;
    final fraction = span <= 0
        ? 1.0
        : minFraction + (1 - minFraction) * ((pace - fastest) / span);

    return ClipRRect(
      borderRadius: BorderRadius.circular(3),
      child: LinearProgressIndicator(
        value: fraction.clamp(minFraction, 1.0),
        minHeight: 6,
        backgroundColor: theme.colorScheme.surfaceContainerHighest,
        valueColor: AlwaysStoppedAnimation<Color>(
          fasterThanAverage
              ? AppColors.workout
              : theme.colorScheme.primary.withValues(alpha: 0.55),
        ),
      ),
    );
  }
}
