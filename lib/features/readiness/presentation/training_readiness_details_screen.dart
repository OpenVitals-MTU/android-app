import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/daily_readiness.dart';
import '../../../domain/model/dashboard_query.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../data/source/health/health_permissions.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/period_navigator.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/daily_readiness_view_model.dart';

/// Loads the [DailyReadinessInsight] for an arbitrary [LocalDate] (keyed by
/// date so navigating days does not disturb the shared
/// [dailyReadinessProvider] that backs the daily-readiness dashboard).
/// Mirrors the load performed by [DailyReadinessViewModel.load].
final trainingReadinessInsightProvider =
    FutureProvider.autoDispose.family<DailyReadinessInsight, LocalDate>(
  (ref, date) async {
    final clamped = date.coerceAtMost(LocalDate.now());
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadDashboardDayUseCaseProvider);
    final data = await useCase(
      DashboardQuery(
        date: clamped,
        sleepRangeMode: prefs.sleepRangeMode,
        activityWeekMode: prefs.activityWeekMode,
        visibleMetrics: dailyReadinessMetrics,
        refreshMode: RefreshMode.normal,
      ),
    );
    return calculateDailyReadiness(
      data,
      goals: DailyReadinessGoalInputs(
        stepsGoal: prefs.dailyGoalFor(MetricDailyGoalKey.steps),
        hydrationLitersGoal: prefs.hydrationDailyGoalLiters,
        activeMinutesGoal:
            prefs.dailyGoalFor(MetricDailyGoalKey.activeCaloriesKcal) / 10.0,
      ),
    );
  },
);

/// The training-side factor kinds shown on the training-readiness detail (Kotlin
/// `TrainingReadinessFactorKinds`).
const Set<ReadinessFactorKind> trainingReadinessFactorKinds =
    <ReadinessFactorKind>{
  ReadinessFactorKind.sleepBelowBaseline,
  ReadinessFactorKind.sleepAboveBaseline,
  ReadinessFactorKind.restingHrElevated,
  ReadinessFactorKind.restingHrNormal,
  ReadinessFactorKind.hrvBelowBaseline,
  ReadinessFactorKind.hrvAboveBaseline,
  ReadinessFactorKind.hrvNormal,
  ReadinessFactorKind.trainingLoadHigh,
  ReadinessFactorKind.trainingLoadNormal,
  ReadinessFactorKind.intensityMinutesOnTarget,
  ReadinessFactorKind.intensityMinutesBehind,
  ReadinessFactorKind.physiologicalStressHigh,
  ReadinessFactorKind.physiologicalStressLow,
  ReadinessFactorKind.stressHigh,
  ReadinessFactorKind.temperatureElevated,
  ReadinessFactorKind.missingSleepData,
  ReadinessFactorKind.missingHrvData,
  ReadinessFactorKind.missingIntensityMinutes,
  ReadinessFactorKind.missingStressData,
  ReadinessFactorKind.newUserNotEnoughBaseline,
};

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
  late LocalDate _selectedDate;

  @override
  void initState() {
    super.initState();
    _selectedDate = parseIsoLocalDate(widget.date);
  }

  void _select(LocalDate date) {
    setState(() => _selectedDate = date.coerceAtMost(LocalDate.now()));
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(trainingReadinessInsightProvider(_selectedDate));

    return Scaffold(
      appBar: AppBar(title: const Text('Training Readiness')),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readSleep,
        },
        showInlineSyncBanner: false,
        child: RefreshIndicator(
          onRefresh: () async =>
              ref.invalidate(trainingReadinessInsightProvider(_selectedDate)),
          child: async.when(
            loading: () => const FullScreenLoading(),
            error: (error, _) => ErrorMessage(error.toString()),
            data: (insight) => _Content(
              insight: insight,
              selectedDate: _selectedDate,
              canGoForward: _selectedDate.isBefore(LocalDate.now()),
              onPreviousDay: () => _select(_selectedDate.minusDays(1)),
              onNextDay: () => _select(_selectedDate.plusDays(1)),
              onSelectDate: _select,
            ),
          ),
        ),
      ),
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({
    required this.insight,
    required this.selectedDate,
    required this.canGoForward,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final DailyReadinessInsight insight;
  final LocalDate selectedDate;
  final bool canGoForward;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context) {
    final isUnknown = insight.state == ReadinessState.unknown;
    final factors = insight.factors
        .where((factor) => trainingReadinessFactorKinds.contains(factor.kind))
        .toList();
    final signals = factors.isEmpty
        ? const ['No usable training-side signals were available.']
        : [for (final f in factors) '${f.label}: ${f.detail}'];
    final strain = [
      insight.strainTarget,
      if (insight.currentStrain != null) insight.currentStrain!,
    ].join(' · ');
    final guidance = <String>[
      'Recommended: ${insight.suggestedWorkout}',
      'Avoid: ${insight.avoid}',
      if (strain.trim().isNotEmpty) 'Strain target: $strain',
    ];

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
          score: insight.trainingReadinessScore,
          verdict: _scoreBandLabel(insight.trainingReadinessScore, isUnknown),
          confidence: _confidenceText(
            insight.confidence,
            insight.confidenceReason,
          ),
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
          padding: const EdgeInsets.symmetric(vertical: 8),
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

/// Verdict band for a readiness score (Kotlin `scoreBandLabel`).
String _scoreBandLabel(int score, bool isUnknown) {
  if (isUnknown) return 'Needs more data';
  if (score >= 80) return 'Strong';
  if (score >= 60) return 'Steady';
  if (score >= 40) return 'Limited';
  return 'Low';
}

/// Confidence line (Kotlin `readinessConfidenceText`).
String _confidenceText(ReadinessConfidence confidence, String reason) {
  final label = switch (confidence) {
    ReadinessConfidence.high => 'High confidence',
    ReadinessConfidence.medium => 'Medium confidence',
    ReadinessConfidence.low => 'Low confidence',
  };
  final reasonLabel = switch (reason) {
    'complete_data' => 'complete local data',
    'missing_sleep_data' => 'sleep data missing',
    'missing_hrv_data' => 'HRV data missing',
    'new_user_not_enough_baseline' => 'baseline still building',
    _ => 'partial local data',
  };
  return '$label · $reasonLabel';
}

/// Parses an ISO `yyyy-MM-dd` argument into a [LocalDate] (falling back to today
/// on any malformed input), matching the Kotlin nav-arg handling and the
/// sibling `BodyEnergyDetailsScreen`.
LocalDate parseIsoLocalDate(String value) {
  final parsed = DateTime.tryParse(value);
  if (parsed != null) return LocalDate(parsed.year, parsed.month, parsed.day);
  return LocalDate.now();
}
