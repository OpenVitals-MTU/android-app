import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/period_navigator.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/training_readiness_details_view_model.dart';

/// Training-readiness detail pushed over the shell
/// (`/daily_readiness/training_readiness/:trainingReadinessDate`). Port of the
/// Kotlin `TrainingReadinessDetailsScreen` / `ReadinessScoreDetailsScreen`
/// (`TRAINING_READINESS` kind): score summary + verdict + confidence, a
/// how-it-is-calculated explanation, the training-side signals used, guidance,
/// and caveats for the selected day.
class TrainingReadinessDetailsScreen extends ConsumerStatefulWidget {
  const TrainingReadinessDetailsScreen({super.key, required this.date});

  /// ISO-8601 date argument (`yyyy-MM-dd`).
  final String date;

  @override
  ConsumerState<TrainingReadinessDetailsScreen> createState() =>
      _TrainingReadinessDetailsScreenState();
}

class _TrainingReadinessDetailsScreenState
    extends ConsumerState<TrainingReadinessDetailsScreen> {
  @override
  void initState() {
    super.initState();
    // The route argument only says which day to open on; the view-model owns
    // every day shown after that.
    final date = parseIsoLocalDate(widget.date);
    Future.microtask(
      () => ref.read(trainingReadinessDetailsProvider.notifier).load(date),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(trainingReadinessDetailsProvider);
    final notifier = ref.read(trainingReadinessDetailsProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Training Readiness')),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readSleep,
        },
        showInlineSyncBanner: false,
        child: RefreshIndicator(
          onRefresh: notifier.refresh,
          child: _Body(
            state: state,
            onPreviousDay: notifier.previousDay,
            onNextDay: notifier.nextDay,
            onSelectDate: notifier.selectDate,
          ),
        ),
      ),
    );
  }
}

class _Body extends StatelessWidget {
  const _Body({
    required this.state,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final TrainingReadinessDetailsState state;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context) {
    final display = state.display;
    if (state.isLoading && display == null) return const FullScreenLoading();
    if (display == null) {
      return ErrorMessage(
        state.error == null
            ? 'No readiness data for this day.'
            : trainingReadinessErrorText(state.error!),
      );
    }
    return _Content(
      display: display,
      selectedDate: state.selectedDate,
      canGoForward: state.canGoForward,
      onPreviousDay: onPreviousDay,
      onNextDay: onNextDay,
      onSelectDate: onSelectDate,
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({
    required this.display,
    required this.selectedDate,
    required this.canGoForward,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final TrainingReadinessDisplay display;
  final LocalDate selectedDate;
  final bool canGoForward;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context) {
    final signals = display.signals;
    final guidance = display.guidance;

    final items = <Widget>[
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: DayNavigator(
          date: selectedDate,
          canGoForward: canGoForward,
          onPreviousDay: onPreviousDay,
          onNextDay: onNextDay,
          onOpenCalendar: () async {
            final picked = await showHealthDatePicker(
              context,
              selectedDate: selectedDate,
            );
            if (picked != null) onSelectDate(picked);
          },
        ),
      ),
      _CardPad(
        child: _ScoreCard(
          score: display.score,
          verdict: display.verdict,
          confidence: display.confidence,
          summary:
              'A training-side score for how well current recovery and load '
              'signals support exercise intensity.',
        ),
      ),
      const _CardPad(
        child: _ExplanationCard(
          body: 'Training Readiness uses training-side signals: sleep, HRV '
              'status, resting heart rate, training load, intensity minutes, '
              'physiological stress, temperature, and activity context. It '
              'estimates whether harder training fits today.',
          scale: 'Scale: 80-100 ready for hard training, 60-79 controlled '
              'training, 40-59 light training, 0-39 rest-focused.',
        ),
      ),
      _CardPad(child: _ListCard(title: 'Signals used', items: signals)),
      _CardPad(child: _ListCard(title: 'What this means', items: guidance)),
      const _CardPad(
        child: _ListCard(
          title: 'Caveats',
          items: [
            'This is a local rule-based estimate from the data currently '
                'available in OpenVitals.',
            'It is not a diagnosis, medical advice, coaching, or injury '
                'prediction.',
            'Missing permissions, sparse samples, or missing baselines lower '
                'confidence.',
          ],
        ),
      ),
      const DataSourceEducationItem(),
      const SizedBox(height: 16),
    ];

    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 1080),
        child: ListView(
          padding: screenScrollPadding(context),
          children: items,
        ),
      ),
    );
  }
}

// ── Cards ────────────────────────────────────────────────────────────────────

class _ScoreCard extends StatelessWidget {
  const _ScoreCard({
    required this.score,
    required this.verdict,
    required this.confidence,
    required this.summary,
  });

  final int score;
  final String verdict;
  final String confidence;
  final String summary;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    const accent = AppColors.workout;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.16),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.fitness_center, color: accent),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Training Readiness',
                          style: theme.textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600)),
                      Text(confidence,
                          style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant)),
                    ],
                  ),
                ),
                Text('$score/100',
                    style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold, color: accent)),
              ],
            ),
            const SizedBox(height: 12),
            Text(verdict,
                style: theme.textTheme.headlineSmall
                    ?.copyWith(fontWeight: FontWeight.w600, color: accent)),
            const SizedBox(height: 8),
            Text(summary,
                style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}

class _ExplanationCard extends StatelessWidget {
  const _ExplanationCard({required this.body, required this.scale});

  final String body;
  final String scale;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.info_outline,
                    color: theme.colorScheme.primary, size: 20),
                const SizedBox(width: 8),
                Text('How this is calculated',
                    style: theme.textTheme.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 10),
            Text(body,
                style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 10),
            Text(scale,
                style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}

class _ListCard extends StatelessWidget {
  const _ListCard({required this.title, required this.items});

  final String title;
  final List<String> items;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title,
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.w600)),
            const SizedBox(height: 10),
            for (final item in items)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 7),
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: theme.colorScheme.primary,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(item,
                          style: theme.textTheme.bodyMedium?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant)),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _CardPad extends StatelessWidget {
  const _CardPad({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: child,
      );
}

// ── Labels ───────────────────────────────────────────────────────────────────

/// Resolves a [ScreenError] into a display string.
String trainingReadinessErrorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Not found.',
      ScreenErrorMissingArgument() => 'Something went wrong.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };

/// Parses an ISO `yyyy-MM-dd` argument into a [LocalDate] (falling back to today
/// on any malformed input), matching the Kotlin nav-arg handling and the
/// sibling `BodyEnergyDetailsScreen`.
LocalDate parseIsoLocalDate(String value) {
  final parsed = DateTime.tryParse(value);
  if (parsed != null) return LocalDate(parsed.year, parsed.month, parsed.day);
  return LocalDate.now();
}
