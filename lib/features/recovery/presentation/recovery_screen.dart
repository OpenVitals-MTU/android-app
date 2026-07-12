import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/stress_tracking.dart';
import '../../../data/source/health/health_permissions.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/period_navigator.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/recovery_view_model.dart';

/// Stress-tracking (physiological stress) detail pushed over the shell
/// (`/daily_readiness/stress/:stressDate`). The Flutter port's recovery feature:
/// a selected-day physiological-stress estimate — level, score, contributing
/// signals, data coverage, and caveats — derived by
/// [calculatePhysiologicalStress]. Port of the Kotlin `StressDetailsScreen`.
class StressDetailsScreen extends ConsumerStatefulWidget {
  const StressDetailsScreen({super.key, required this.date});

  /// ISO-8601 date argument (`yyyy-MM-dd`).
  final String date;

  @override
  ConsumerState<StressDetailsScreen> createState() =>
      _StressDetailsScreenState();
}

class _StressDetailsScreenState extends ConsumerState<StressDetailsScreen> {
  @override
  void initState() {
    super.initState();
    final date = _parseIsoLocalDate(widget.date);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.read(recoveryProvider.notifier).load(date);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(recoveryProvider);
    final notifier = ref.read(recoveryProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Stress')),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readHrv,
        },
        showInlineSyncBanner: false,
        child: _StressBody(
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

class _StressBody extends StatelessWidget {
  const _StressBody({
    required this.state,
    required this.onRefresh,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final RecoveryState state;
  final Future<void> Function() onRefresh;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context) {
    final stress = state.stress;
    if (state.isLoading && stress == null) {
      return const FullScreenLoading();
    }
    if (stress == null && state.error != null) {
      return ErrorMessage(_stressScreenErrorText(state.error!));
    }

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
      if (stress != null) ...[
        _CardPad(child: _StressScoreCard(stress: stress)),
        _CardPad(child: _StressExplanationCard(stress: stress)),
        _CardPad(
          child: _StressListCard(
            title: 'Inputs',
            items: stress.contributingFactors.isEmpty
                ? const ['No stress inputs are available for this day.']
                : stress.contributingFactors,
          ),
        ),
        _CardPad(
          child: _StressListCard(
            title: 'Data coverage',
            items: stress.dataCoverage.isEmpty
                ? const ['No same-day sample coverage was available.']
                : stress.dataCoverage,
          ),
        ),
        _CardPad(
          child: _StressListCard(title: 'Caveats', items: stress.caveats),
        ),
        const DataSourceEducationItem(),
      ] else
        const _CardPad(
          child: ErrorMessage('No stress estimate for this day.'),
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
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: items,
          ),
        ),
      ),
    );
  }
}

class _StressScoreCard extends StatelessWidget {
  const _StressScoreCard({required this.stress});

  final PhysiologicalStressEstimate stress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final accent = _levelColor(stress.level, scheme);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: accent.withValues(alpha: 0.16),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(Icons.psychology_outlined, color: accent),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Stress tracking',
                          style: theme.textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600)),
                      Text(_confidenceText(stress),
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: scheme.onSurfaceVariant)),
                    ],
                  ),
                ),
                Text(
                  stress.score != null ? '${stress.score}/100' : '--',
                  style: theme.textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.bold, color: accent),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(stress.label,
                style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w600, color: accent)),
            const SizedBox(height: 6),
            Text(stress.detail,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: scheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}

class _StressExplanationCard extends StatelessWidget {
  const _StressExplanationCard({required this.stress});

  final PhysiologicalStressEstimate stress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final showGuidance = stress.level == PhysiologicalStressLevel.medium ||
        stress.level == PhysiologicalStressLevel.high;
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.info_outline, size: 20, color: scheme.primary),
                const SizedBox(width: 8),
                Text('How this is tracked',
                    style: theme.textTheme.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              'Health Connect does not provide a stress score, so OpenVitals '
              'estimates physiological strain locally from HRV, resting heart '
              'rate, and average heart-rate context.',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            const SizedBox(height: 8),
            Text(
              'The 0-100 scale runs Resting (0-25), Low (26-50), Medium (51-75), '
              'and High (76-100).',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: scheme.onSurfaceVariant),
            ),
            if (showGuidance) ...[
              const SizedBox(height: 10),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 28,
                    height: 28,
                    decoration: BoxDecoration(
                      color: AppColors.mindfulness.withValues(alpha: 0.12),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.self_improvement,
                        color: AppColors.mindfulness, size: 17),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'A short breathing or mindfulness session may help bring '
                      'this back down.',
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(color: scheme.onSurfaceVariant),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StressListCard extends StatelessWidget {
  const _StressListCard({required this.title, required this.items});

  final String title;
  final List<String> items;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
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
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 7),
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: scheme.primary,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(item,
                          style: theme.textTheme.bodyMedium
                              ?.copyWith(color: scheme.onSurfaceVariant)),
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

// ── Labels & colours ─────────────────────────────────────────────────────────

Color _levelColor(PhysiologicalStressLevel level, ColorScheme scheme) {
  switch (level) {
    case PhysiologicalStressLevel.resting:
      return AppColors.sleep;
    case PhysiologicalStressLevel.low:
      return AppColors.workout;
    case PhysiologicalStressLevel.medium:
      return AppColors.heart;
    case PhysiologicalStressLevel.high:
      return AppColors.vitals;
    case PhysiologicalStressLevel.needsMoreData:
      return scheme.onSurfaceVariant;
  }
}

String _confidenceText(PhysiologicalStressEstimate stress) {
  final String label;
  switch (stress.confidence) {
    case PhysiologicalStressConfidence.high:
      label = 'High confidence';
    case PhysiologicalStressConfidence.medium:
      label = 'Medium confidence';
    case PhysiologicalStressConfidence.low:
      label = 'Low confidence';
    case PhysiologicalStressConfidence.noData:
      label = 'No stress estimate';
  }
  final String reason;
  switch (stress.confidenceReason) {
    case 'hrv_resting_hr_average_hr':
      reason = 'HRV, resting HR, and average HR';
    case 'partial_hrv_or_heart_rate_context':
      reason = 'partial HRV or heart-rate context';
    case 'activity_may_influence':
      reason = 'activity may influence';
    case 'single_signal':
      reason = 'single local signal';
    default:
      reason = 'needs more local data';
  }
  return '$label · $reason';
}

LocalDate _parseIsoLocalDate(String value) {
  final parsed = DateTime.tryParse(value);
  if (parsed != null) return LocalDate(parsed.year, parsed.month, parsed.day);
  return LocalDate.now();
}

String _stressScreenErrorText(ScreenError error) {
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
