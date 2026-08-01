import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/reference_link.dart';
import '../../../core/presentation/refresh_on_signal.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/cardio_load.dart';
import '../../../domain/refresh/data_domain.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../state/refresh_coordinator.dart';
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
class CardioLoadDetailScreen extends ConsumerStatefulWidget {
  const CardioLoadDetailScreen({super.key});

  @override
  ConsumerState<CardioLoadDetailScreen> createState() =>
      _CardioLoadDetailScreenState();
}

class _CardioLoadDetailScreenState extends ConsumerState<CardioLoadDetailScreen>
    with RefreshOnSignal {
  @override
  Set<DataDomain> get refreshDomains =>
      const {DataDomain.activities, DataDomain.heart};

  @override
  void onRefreshSignal(RefreshSignal signal) =>
      unawaited(ref.read(cardioLoadProvider.notifier).refresh());

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(cardioLoadProvider);
    final notifier = ref.read(cardioLoadProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).metricCardioLoad)),
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
      return ErrorMessage(_errorText(AppLocalizations.of(context), state.error!));
    }

    final estimate = state.estimate;
    final l10n = AppLocalizations.of(context);
    return RefreshIndicator(
      onRefresh: notifier.refresh,
      child: ListView(
        padding: screenScrollPadding(context),
        children: [
          sectionPadded(_SummaryCard(estimate: estimate, formatter: formatter)),
          SectionHeader(AppLocalizations.of(context).cardioLoadDayNumbersTitle),
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
    final l10n = AppLocalizations.of(context);
    final scoreText = estimate.confidence == CardioLoadConfidence.noData
        ? l10n.noData
        : formatter.count(estimate.score);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.favorite_border, color: AppColors.heart),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    l10n.metricCardioLoad,
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
                      _confidenceLabel(l10n, estimate.confidence),
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
              _methodLabel(l10n, estimate.method),
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
    final l10n = AppLocalizations.of(context);
    final estimate = state.estimate;
    final rows = <(String, String)>[
      (l10n.cardioLoadMethod, _methodLabel(l10n, estimate.method)),
      (
        l10n.cardioLoadTrimpScore,
        estimate.trimpScore != null
            ? formatter.decimal(estimate.trimpScore!, 1)
            : l10n.noData,
      ),
      (
        l10n.cardioLoadHrCoverage,
        '${formatter.decimal(estimate.coveredMinutes, 1)} min',
      ),
      (
        l10n.cardioLoadExpectedCoverage,
        '${formatter.decimal(estimate.expectedMinutes, 1)} min',
      ),
      (
        l10n.cardioLoadRestingHr,
        estimate.restingHeartRateBpm != null
            ? '${formatter.count(estimate.restingHeartRateBpm!)} bpm'
            : l10n.noData,
      ),
      (
        l10n.cardioLoadMaxHr,
        estimate.maxHeartRateBpm != null
            ? '${formatter.count(estimate.maxHeartRateBpm!)} bpm'
            : l10n.noData,
      ),
      (l10n.cardioLoadHrSamples, formatter.count(estimate.heartRateSampleCount)),
      (
        l10n.cardioLoadActivityWindows,
        formatter.count(estimate.activityWindowCount),
      ),
      (
        l10n.cardioLoadActivityMinutes,
        '${formatter.count(estimate.activityWindowMinutes.round())} min',
      ),
      (
        l10n.cardioLoadMovementFallback,
        formatter.count(estimate.movementFallbackScore),
      ),
      (l10n.metricSteps, '${formatter.count(state.steps)} ${l10n.unitSteps}'),
      (
        l10n.metricActiveCalories,
        state.activeCaloriesKcal != null
            ? formatter.energy(state.activeCaloriesKcal!).text
            : l10n.noData,
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
              _calibrationLabel(l10n, estimate),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

String _confidenceLabel(AppLocalizations l10n, CardioLoadConfidence confidence) {
  switch (confidence) {
    case CardioLoadConfidence.high:
      return l10n.cardioLoadConfidenceHigh;
    case CardioLoadConfidence.medium:
      return l10n.cardioLoadConfidenceMedium;
    case CardioLoadConfidence.low:
      return l10n.cardioLoadConfidenceLow;
    case CardioLoadConfidence.noData:
      return l10n.cardioLoadConfidenceNoData;
  }
}

String _methodLabel(AppLocalizations l10n, CardioLoadMethod method) {
  switch (method) {
    case CardioLoadMethod.trimpActivityWindows:
      return l10n.cardioLoadMethodActivityWindows;
    case CardioLoadMethod.trimpElevatedHeartRate:
      return l10n.cardioLoadMethodElevatedHr;
    case CardioLoadMethod.movementFallback:
      return l10n.cardioLoadMethodMovementFallback;
    case CardioLoadMethod.noData:
      return l10n.cardioLoadMethodNoData;
  }
}

String _calibrationLabel(AppLocalizations l10n, CardioLoadEstimate estimate) {
  final resting = estimate.restingHeartRateObserved
      ? l10n.cardioLoadCalibrationObservedResting
      : l10n.cardioLoadCalibrationEstimatedResting;
  final max = estimate.maxHeartRateObserved
      ? l10n.cardioLoadCalibrationObservedMax
      : l10n.cardioLoadCalibrationEstimatedMax;
  return '$resting / $max';
}

String _errorText(AppLocalizations l10n, ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => l10n.screenErrorNotFound,
      ScreenErrorMissingArgument() => l10n.screenErrorMissingArgument,
      ScreenErrorPermissionDenied() => l10n.screenErrorPermissionDenied,
      ScreenErrorHealthConnectUnavailable() =>
        l10n.screenErrorHealthConnectUnavailable,
    };
