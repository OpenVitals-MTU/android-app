import 'package:flutter/material.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_card.dart';
import 'activity_recording.dart';

/// What the rider has to do next, and how long for.
///
/// During a recovery test this is the only thing on the screen that matters — they are at
/// their limit, the phone is on a bar mount or in a pocket, and everything else is
/// scenery. It leads with the phase, in words, and gives the countdown underneath.
///
/// The End effort button is always there through the warmup and the effort. The
/// heart-rate target is a convenience; on a day when the legs are not there, the rider
/// has to be able to say so, and the test still works — the measurement only needs the
/// stop to be abrupt, not to have been reached at any particular heart rate.
class ActivityHeartRateRecoveryPhaseBanner extends StatelessWidget {
  const ActivityHeartRateRecoveryPhaseBanner({
    super.key,
    required this.state,
    required this.now,
    required this.onEndEffort,
  });

  final ActivityRecordingState state;
  final DateTime now;
  final VoidCallback onEndEffort;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    final remaining = state.hrrPhaseRemaining(now);
    final canEndEffort = state.hrrPhase == ActivityRecordingHrrPhase.warmup ||
        state.hrrPhase == ActivityRecordingHrrPhase.effort;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              _phaseText(l10n, state.hrrPhase),
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w700,
                color: _phaseColor(state.hrrPhase, scheme),
              ),
            ),
            if (remaining != null) ...[
              const SizedBox(height: 4),
              Text(
                _countdown(remaining),
                style: theme.textTheme.displaySmall
                    ?.copyWith(fontWeight: FontWeight.w600),
              ),
            ],
            if (state.currentHeartRateBpm != null) ...[
              const SizedBox(height: 4),
              Text(
                '${state.currentHeartRateBpm} bpm',
                style: theme.textTheme.titleMedium
                    ?.copyWith(color: scheme.onSurfaceVariant),
              ),
            ],
            if (canEndEffort) ...[
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: onEndEffort,
                icon: const Icon(Icons.stop, size: 18),
                label: Text(l10n.activityRecordingHrrEndEffort),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

String _phaseText(AppLocalizations l10n, ActivityRecordingHrrPhase phase) =>
    switch (phase) {
      ActivityRecordingHrrPhase.warmup => l10n.activityRecordingHrrPhaseWarmup,
      ActivityRecordingHrrPhase.effort => l10n.activityRecordingHrrPhaseEffort,
      ActivityRecordingHrrPhase.recovery =>
        l10n.activityRecordingHrrPhaseRecovery,
      ActivityRecordingHrrPhase.complete =>
        l10n.activityRecordingHrrPhaseComplete,
      ActivityRecordingHrrPhase.none => '',
    };

Color _phaseColor(ActivityRecordingHrrPhase phase, ColorScheme scheme) =>
    switch (phase) {
      ActivityRecordingHrrPhase.effort => scheme.error,
      ActivityRecordingHrrPhase.recovery => scheme.primary,
      ActivityRecordingHrrPhase.complete => scheme.primary,
      _ => scheme.onSurface,
    };

String _countdown(Duration remaining) {
  final minutes = remaining.inMinutes;
  final seconds = remaining.inSeconds % 60;
  return '$minutes:${seconds.toString().padLeft(2, '0')}';
}
