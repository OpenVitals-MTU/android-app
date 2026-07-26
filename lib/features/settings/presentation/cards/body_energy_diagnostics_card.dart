import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../ui/components/ov_card.dart';
import '../../application/body_energy_diagnostics.dart';

/// Diagnostics-only card holding the Body Energy model next to the watch's own
/// Body Battery, so a mis-calibration can be told apart from a bad input.
///
/// Body Energy over-drains on some days — the score pins at zero well before
/// bedtime. Two hypotheses explain that and the screen cannot separate them: the
/// drain constants are too hot, or the active-calorie input is doubled because
/// two apps write the same watch's data and Health Connect's aggregates sum
/// every writer. This card prints the columns that discriminate between them,
/// including the per-source split that no aggregate can produce.
///
/// English-only on purpose, like its neighbours: it is dev-facing and never
/// reaches a shipped locale. Reachable only in diagnostics-enabled builds.
///
/// Unlike the Health Connect sources card this does NOT load on open. A cold run is
/// on the order of sixty Health Connect calls, and the platform charges quota
/// per call — not something a settings screen should spend just by being
/// scrolled past.
class BodyEnergyDiagnosticsCard extends ConsumerStatefulWidget {
  const BodyEnergyDiagnosticsCard({super.key});

  @override
  ConsumerState<BodyEnergyDiagnosticsCard> createState() =>
      _BodyEnergyDiagnosticsCardState();
}

class _BodyEnergyDiagnosticsCardState
    extends ConsumerState<BodyEnergyDiagnosticsCard> {
  bool _started = false;

  Future<void> _copy(BodyEnergyDiagnosticsReport report) async {
    // A paste, not a file: this is meant to be dropped straight into a bug
    // report or a chat. The log sanitizer and the share sheet next door are for
    // logcat text, whose redaction rules do not apply to these numbers.
    await Clipboard.setData(ClipboardData(text: report.toReportText()));
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Body Energy report copied')),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final report = _started
        ? ref.watch(bodyEnergyDiagnosticsProvider)
        : const AsyncValue<BodyEnergyDiagnosticsReport>.loading();
    final loaded = report.asData?.value;

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
                      Icons.science_outlined,
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
                          'Body Energy calibration',
                          style: theme.textTheme.titleSmall,
                        ),
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            'The model against your watch\'s own Body Battery '
                            'over the last $bodyEnergyDiagnosticsDays days, with '
                            'the calories that fed it broken down per writing '
                            'app. Reads Health Connect, so run it deliberately.',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              switch (report) {
                AsyncValue(:final error?) => Text(
                    'Failed: $error',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error),
                  ),
                _ when !_started => const SizedBox.shrink(),
                AsyncValue(:final value?) => _ReportBody(report: value),
                _ => const Padding(
                    padding: EdgeInsets.symmetric(vertical: 8),
                    child: LinearProgressIndicator(),
                  ),
              },
              const SizedBox(height: 8),
              Row(
                children: [
                  FilledButton.tonalIcon(
                    onPressed: () {
                      if (_started) {
                        ref.invalidate(bodyEnergyDiagnosticsProvider);
                      } else {
                        setState(() => _started = true);
                      }
                    },
                    icon: const Icon(Icons.play_arrow_outlined, size: 18),
                    label: Text(_started ? 'Run again' : 'Run diagnostic'),
                  ),
                  const SizedBox(width: 8),
                  if (loaded case final value?)
                    TextButton.icon(
                      onPressed: () => _copy(value),
                      icon: const Icon(Icons.copy_all_outlined, size: 18),
                      label: const Text('Copy report'),
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ReportBody extends StatelessWidget {
  const _ReportBody({required this.report});

  final BodyEnergyDiagnosticsReport report;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final mono = theme.textTheme.bodySmall?.copyWith(
      fontFamily: 'monospace',
      color: theme.colorScheme.onSurfaceVariant,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (report.missingPermissions.isNotEmpty)
          _Banner(
            icon: Icons.lock_outline,
            color: theme.colorScheme.error,
            text: 'Missing read permissions: '
                '${(report.missingPermissions.toList()..sort()).join(', ')}. '
                'Those figures read as zero rather than as unavailable.',
          ),
        if (report.truncated)
          _Banner(
            icon: Icons.warning_amber_outlined,
            color: theme.colorScheme.error,
            text: 'The per-source read hit its record cap, so the split below '
                'is truncated rather than proportional.',
          ),
        if (report.hasMultipleCalorieSources)
          _Banner(
            icon: Icons.call_split_outlined,
            color: theme.colorScheme.error,
            text: 'More than one app wrote active calories. '
                '${report.secondarySourceActiveKcal.toStringAsFixed(0)} kcal '
                'came from something other than the largest source — Health '
                'Connect sums them, so the model ate the total.',
          ),
        if (report.storedWatchSampleCount == 0)
          _Banner(
            icon: Icons.watch_off_outlined,
            color: theme.colorScheme.onSurfaceVariant,
            text: 'No watch Body Battery samples stored, so there is nothing to '
                'compare against. Sync a Garmin watch first.',
          ),
        // The report text is the deliverable; the card renders it verbatim so
        // what is read on screen and what lands on the clipboard cannot drift.
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Text(report.toReportText(), style: mono),
        ),
      ],
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({
    required this.icon,
    required this.color,
    required this.text,
  });

  final IconData icon;
  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Icon(icon, size: 16, color: color),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: theme.textTheme.bodySmall?.copyWith(color: color),
            ),
          ),
        ],
      ),
    );
  }
}
