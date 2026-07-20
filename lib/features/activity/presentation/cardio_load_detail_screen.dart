import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/reference_link.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/cardio_load.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/cardio_load_detail_view_model.dart';
import '../../../ui/components/section_padding.dart';

/// Cardio-load detail pushed over the shell (`/activity/cardio_load`), ported
/// from the Kotlin `CardioLoadDetailScreen`. Shows today's TRIMP-based cardio
/// load estimate plus the day's underlying numbers.
class CardioLoadDetailScreen extends ConsumerWidget {
  const CardioLoadDetailScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(cardioLoadProvider);
    final notifier = ref.read(cardioLoadProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Cardio load')),
      body: _body(context, state, formatter, notifier),
    );
  }

  Widget _body(
    BuildContext context,
    CardioLoadState state,
    UnitFormatter formatter,
    CardioLoadViewModel notifier,
  ) {
    if (state.isLoading && state.estimate == CardioLoadEstimate.noData) {
      return const FullScreenLoading();
    }
    if (state.error != null) {
      return ErrorMessage(_errorText(state.error!));
    }

    final estimate = state.estimate;
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: notifier.refresh,
      child: ListView(
        padding: screenScrollPadding(context),
        children: [
          sectionPadded(_SummaryCard(estimate: estimate, formatter: formatter)),
          const SectionHeader('Today\'s numbers'),
          sectionPadded(_NumbersCard(state: state, formatter: formatter)),
          SectionHeader(l10n.cardioLoadReferencesTitle),
          sectionPadded(const _CardioLoadReferencesCard()),
        ],
      ),
    );
  }
}

// Research behind the TRIMP-based cardio-load estimate, shown to the user as
// tappable links (recovered from the Kotlin CardioLoadDetailScreen; AGENTS.md
// invariant 8).
const String _banisterTrimpUrl =
    'https://pmc.ncbi.nlm.nih.gov/articles/PMC6561225/';
const String _trainingLoadReviewUrl =
    'https://pmc.ncbi.nlm.nih.gov/articles/PMC4213373/';
const String _healthConnectWorkoutUrl =
    'https://developer.android.com/health-and-fitness/health-connect/experiences/workouts';

class _CardioLoadReferencesCard extends StatelessWidget {
  const _CardioLoadReferencesCard();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ReferenceLinkButton(
              title: l10n.cardioLoadReferenceBanister,
              url: _banisterTrimpUrl,
            ),
            ReferenceLinkButton(
              title: l10n.cardioLoadReferenceTrainingLoad,
              url: _trainingLoadReviewUrl,
            ),
            ReferenceLinkButton(
              title: l10n.cardioLoadReferenceHealthConnect,
              url: _healthConnectWorkoutUrl,
            ),
          ],
        ),
      ),
    );
  }
}


class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.estimate, required this.formatter});

  final CardioLoadEstimate estimate;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scoreText = estimate.confidence == CardioLoadConfidence.noData
        ? 'No data'
        : formatter.count(estimate.score);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.favorite, color: AppColors.heart),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Cardio load',
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      scoreText,
                      style: theme.textTheme.headlineMedium
                          ?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    Text(
                      _confidenceLabel(estimate.confidence),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              _methodLabel(estimate.method),
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _NumbersCard extends StatelessWidget {
  const _NumbersCard({required this.state, required this.formatter});

  final CardioLoadState state;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final estimate = state.estimate;
    final rows = <(String, String)>[
      ('Method', _methodLabel(estimate.method)),
      (
        'TRIMP score',
        estimate.trimpScore != null
            ? formatter.decimal(estimate.trimpScore!, 1)
            : 'No data',
      ),
      ('HR coverage', '${formatter.decimal(estimate.coveredMinutes, 1)} min'),
      (
        'Expected coverage',
        '${formatter.decimal(estimate.expectedMinutes, 1)} min',
      ),
      (
        'Resting HR',
        estimate.restingHeartRateBpm != null
            ? '${formatter.count(estimate.restingHeartRateBpm!)} bpm'
            : 'No data',
      ),
      (
        'Max HR',
        estimate.maxHeartRateBpm != null
            ? '${formatter.count(estimate.maxHeartRateBpm!)} bpm'
            : 'No data',
      ),
      ('HR samples', formatter.count(estimate.heartRateSampleCount)),
      ('Activity windows', formatter.count(estimate.activityWindowCount)),
      (
        'Activity minutes',
        '${formatter.count(estimate.activityWindowMinutes.round())} min',
      ),
      ('Movement fallback', formatter.count(estimate.movementFallbackScore)),
      ('Steps', '${formatter.count(state.steps)} steps'),
      (
        'Active calories',
        state.activeCaloriesKcal != null
            ? formatter.energy(state.activeCaloriesKcal!).text
            : 'No data',
      ),
    ];
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final row in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      row.$1,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    Text(row.$2, style: theme.textTheme.bodyMedium),
                  ],
                ),
              ),
            const SizedBox(height: 8),
            Text(
              _calibrationLabel(estimate),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

String _confidenceLabel(CardioLoadConfidence confidence) {
  switch (confidence) {
    case CardioLoadConfidence.high:
      return 'High confidence';
    case CardioLoadConfidence.medium:
      return 'Medium confidence';
    case CardioLoadConfidence.low:
      return 'Low confidence';
    case CardioLoadConfidence.noData:
      return 'No data';
  }
}

String _methodLabel(CardioLoadMethod method) {
  switch (method) {
    case CardioLoadMethod.trimpActivityWindows:
      return 'From heart rate during workouts';
    case CardioLoadMethod.trimpElevatedHeartRate:
      return 'From elevated heart rate';
    case CardioLoadMethod.movementFallback:
      return 'Estimated from movement';
    case CardioLoadMethod.noData:
      return 'Not enough data';
  }
}

String _calibrationLabel(CardioLoadEstimate estimate) {
  final resting = estimate.restingHeartRateObserved
      ? 'Observed resting HR'
      : 'Estimated resting HR';
  final max = estimate.maxHeartRateObserved
      ? 'observed max HR'
      : 'estimated max HR';
  return '$resting / $max';
}

String _errorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Not found.',
      ScreenErrorMissingArgument() => 'Missing information.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };
