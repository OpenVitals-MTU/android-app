import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/daily_readiness.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/period_navigator.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/daily_readiness_view_model.dart';

/// Daily-readiness overview pushed over the shell (`/daily_readiness`). Port of
/// the Kotlin `DailyReadinessScreen` + `DailyReadinessPanel`: the overall /
/// body-energy / training-readiness scores, state, recommendation, factor list,
/// and confidence for the selected day.
class DailyReadinessScreen extends ConsumerWidget {
  const DailyReadinessScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(dailyReadinessProvider);
    final notifier = ref.read(dailyReadinessProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Daily readiness')),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readSleep,
        },
        showInlineSyncBanner: false,
        child: _ReadinessBody(
          state: state,
          onRefresh: notifier.refresh,
          onPreviousDay: notifier.previousDay,
          onNextDay: notifier.nextDay,
          onSelectDate: notifier.selectDate,
        ),
      ),
    );
  }
}

class _ReadinessBody extends StatelessWidget {
  const _ReadinessBody({
    required this.state,
    required this.onRefresh,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final DailyReadinessState state;
  final Future<void> Function() onRefresh;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context) {
    final insight = state.insight;
    final display = state.display;
    if (state.isLoading && insight == null) {
      return const FullScreenLoading();
    }
    if (insight == null && state.error != null) {
      return ErrorMessage(readinessScreenErrorText(state.error!));
    }

    final date = _isoDate(state.selectedDate);
    final items = <Widget>[
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: DayNavigator(
          date: state.selectedDate,
          canGoForward: state.canGoForward,
          onPreviousDay: onPreviousDay,
          onNextDay: onNextDay,
          onOpenCalendar: () async {
            final picked = await showHealthDatePicker(
              context,
              selectedDate: state.selectedDate,
            );
            if (picked != null) onSelectDate(picked);
          },
        ),
      ),
      if (insight != null && display != null)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: _ReadinessPanel(
            insight: insight,
            display: display,
            onOpenBodyEnergy: () =>
                context.push(AppRoutes.bodyEnergyDetailsLocation(date)),
            onOpenStress: () =>
                context.push(AppRoutes.stressDetailsLocation(date)),
            onOpenTrainingReadiness: () => context
                .push(AppRoutes.trainingReadinessDetailsLocation(date)),
          ),
        )
      else
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: ErrorMessage('No readiness data for this day.'),
        ),
      const SizedBox(height: 16),
    ];

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1080),
          child: ListView(
            padding: screenScrollPadding(context),
            children: items,
          ),
        ),
      ),
    );
  }
}

class _ReadinessPanel extends StatelessWidget {
  const _ReadinessPanel({
    required this.insight,
    required this.display,
    required this.onOpenBodyEnergy,
    required this.onOpenStress,
    required this.onOpenTrainingReadiness,
  });

  final DailyReadinessInsight insight;
  final DailyReadinessDisplay display;
  final VoidCallback onOpenBodyEnergy;
  final VoidCallback onOpenStress;
  final VoidCallback onOpenTrainingReadiness;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final accent = _stateColor(insight.state, scheme);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.self_improvement, color: accent, size: 24),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Daily readiness',
                          style: theme.textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600)),
                      Text(display.confidenceText,
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: scheme.onSurfaceVariant)),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text('Score',
                        style: theme.textTheme.labelSmall
                            ?.copyWith(color: scheme.onSurfaceVariant)),
                    Text('${insight.score}/100',
                        style: theme.textTheme.titleLarge?.copyWith(
                            fontWeight: FontWeight.bold, color: accent)),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text(insight.statusTitle,
                style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w600, color: accent)),
            const SizedBox(height: 6),
            Text(insight.recommendation, style: theme.textTheme.bodyLarge),
            const SizedBox(height: 4),
            Text(insight.explanation,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: scheme.onSurfaceVariant)),
            const SizedBox(height: 14),
            Row(
              children: [
                Expanded(
                  child: _ScoreTile(
                    label: 'Body energy',
                    value: '${insight.bodyEnergyScore}/100',
                    icon: Icons.favorite_border,
                    color: AppColors.heart,
                    onTap: onOpenBodyEnergy,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _ScoreTile(
                    label: 'Training',
                    value: '${insight.trainingReadinessScore}/100',
                    icon: Icons.fitness_center,
                    color: AppColors.workout,
                    onTap: onOpenTrainingReadiness,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            _InlineInfo(
              label: 'HRV status',
              value: display.hrvStatusValue,
            ),
            const SizedBox(height: 8),
            _InlineInfo(
              label: 'Intensity minutes',
              value: display.intensityMinutesValue,
            ),
            const SizedBox(height: 8),
            _TappableInfo(
              label: 'Stress level',
              value: display.stressValue,
              onTap: onOpenStress,
            ),
            const Divider(height: 24),
            _GuidanceRow(
              label: 'Recommended',
              value: insight.suggestedWorkout,
              icon: Icons.directions_run,
              color: accent,
            ),
            const SizedBox(height: 8),
            _GuidanceRow(
              label: 'Avoid',
              value: insight.avoid,
              icon: Icons.close,
              color: scheme.error,
            ),
            const SizedBox(height: 8),
            _GuidanceRow(
              label: 'Alternative',
              value: insight.alternative,
              icon: Icons.self_improvement,
              color: AppColors.mindfulness,
            ),
            const SizedBox(height: 14),
            _InlineInfo(label: 'Strain', value: display.strainValue),
            const SizedBox(height: 8),
            _InlineInfo(label: 'Goal', value: insight.adaptiveGoal),
            if (display.topFactors.isNotEmpty) ...[
              const SizedBox(height: 14),
              Text('Why',
                  style: theme.textTheme.labelMedium
                      ?.copyWith(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              for (final factor in display.topFactors)
                _FactorRow(factor: factor),
            ],
            const DataSourceEducationItem(),
          ],
        ),
      ),
    );
  }
}

class _ScoreTile extends StatelessWidget {
  const _ScoreTile({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
    required this.onTap,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: color.withValues(alpha: 0.14),
      borderRadius: const BorderRadius.all(Radius.circular(12)),
      clipBehavior: Clip.hardEdge,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Icon(icon, color: color, size: 22),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label,
                        style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant)),
                    Text(value,
                        style: theme.textTheme.titleSmall
                            ?.copyWith(fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
              Icon(Icons.chevron_right,
                  size: 18, color: theme.colorScheme.onSurfaceVariant),
            ],
          ),
        ),
      ),
    );
  }
}

class _GuidanceRow extends StatelessWidget {
  const _GuidanceRow({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 28,
          height: 28,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.12),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: color, size: 17),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label,
                  style: theme.textTheme.labelMedium
                      ?.copyWith(fontWeight: FontWeight.w600)),
              Text(value,
                  style: theme.textTheme.bodyMedium
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            ],
          ),
        ),
      ],
    );
  }
}

class _InlineInfo extends StatelessWidget {
  const _InlineInfo({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: theme.textTheme.labelMedium
                ?.copyWith(fontWeight: FontWeight.w600)),
        const SizedBox(height: 2),
        Text(value,
            style: theme.textTheme.bodyMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
      ],
    );
  }
}

class _TappableInfo extends StatelessWidget {
  const _TappableInfo({
    required this.label,
    required this.value,
    required this.onTap,
  });

  final String label;
  final String value;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: onTap,
      borderRadius: const BorderRadius.all(Radius.circular(8)),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Expanded(child: _InlineInfo(label: label, value: value)),
            Icon(Icons.chevron_right,
                size: 20, color: theme.colorScheme.onSurfaceVariant),
          ],
        ),
      ),
    );
  }
}

class _FactorRow extends StatelessWidget {
  const _FactorRow({required this.factor});

  final DailyReadinessFactor factor;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            margin: const EdgeInsets.only(top: 6),
            width: 8,
            height: 8,
            decoration: BoxDecoration(
              color: _impactColor(factor.impact, theme.colorScheme),
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(factor.label,
                    style: theme.textTheme.bodyMedium
                        ?.copyWith(fontWeight: FontWeight.w600)),
                Text(factor.detail,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ── Labels & colours ─────────────────────────────────────────────────────────

Color _stateColor(ReadinessState state, ColorScheme scheme) {
  switch (state) {
    case ReadinessState.ready:
      return AppColors.workout;
    case ReadinessState.moderate:
      return AppColors.heart;
    case ReadinessState.recover:
      return AppColors.sleep;
    case ReadinessState.rest:
      return AppColors.vitals;
    case ReadinessState.unknown:
      return scheme.onSurfaceVariant;
  }
}

Color _impactColor(ReadinessFactorImpact impact, ColorScheme scheme) {
  switch (impact) {
    case ReadinessFactorImpact.positive:
      return AppColors.workout;
    case ReadinessFactorImpact.neutral:
      return scheme.onSurfaceVariant;
    case ReadinessFactorImpact.negative:
      return AppColors.sleep;
    case ReadinessFactorImpact.warning:
      return scheme.error;
  }
}

String _isoDate(LocalDate date) => date.toString();

/// Resolves a [ScreenError] into a display string.
String readinessScreenErrorText(ScreenError error) {
  switch (error) {
    case ScreenErrorMessage(:final text):
      return text;
    case ScreenErrorNotFound():
      return 'Not found.';
    case ScreenErrorMissingArgument():
      return 'Something went wrong.';
    case ScreenErrorPermissionDenied():
      return 'Permission denied.';
    case ScreenErrorHealthConnectUnavailable():
      return 'Health Connect is unavailable.';
  }
}
