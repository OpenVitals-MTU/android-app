import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/body_energy_timeline.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/health_date_picker.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/period_navigator.dart';
import '../../settings/presentation/cards/body_energy_calibration_card.dart';
import '../application/body_energy_display.dart';
import '../application/body_energy_view_model.dart';
import 'body_energy_timeline_chart.dart';

/// Body-energy timeline detail pushed over the shell
/// (`/daily_readiness/body_energy/:bodyEnergyDate`). Port of the Kotlin
/// `BodyEnergyDetailsScreen`: a calibration gate for first-run setup, then a
/// selected-day timeline chart + current-level and charge/drain summary +
/// "what moved it" reasons + input availability + method explainer.
class BodyEnergyDetailsScreen extends ConsumerStatefulWidget {
  const BodyEnergyDetailsScreen({super.key, required this.date});

  /// ISO-8601 date argument (`yyyy-MM-dd`).
  final String date;

  @override
  ConsumerState<BodyEnergyDetailsScreen> createState() =>
      _BodyEnergyDetailsScreenState();
}

class _BodyEnergyDetailsScreenState
    extends ConsumerState<BodyEnergyDetailsScreen> {
  @override
  void initState() {
    super.initState();
    final date = parseIsoLocalDate(widget.date);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.read(bodyEnergyProvider.notifier).load(date);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(bodyEnergyProvider);
    final notifier = ref.read(bodyEnergyProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.screenBodyEnergy)),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readHeartRate},
        showInlineSyncBanner: false,
        child: _BodyEnergyBody(
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

class _BodyEnergyBody extends ConsumerWidget {
  const _BodyEnergyBody({
    required this.state,
    required this.onRefresh,
    required this.onPreviousDay,
    required this.onNextDay,
    required this.onSelectDate,
  });

  final BodyEnergyState state;
  final Future<void> Function() onRefresh;
  final VoidCallback onPreviousDay;
  final VoidCallback onNextDay;
  final void Function(LocalDate) onSelectDate;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);

    // Calibration gate (Kotlin `BodyEnergyScreen.kt:74-84`): until setup is
    // completed the screen shows only the calibration card, and the timeline is
    // revealed after the user saves or picks automatic estimates.
    final calibration = ref.watch(bodyEnergyCalibrationCardProvider);
    if (!calibration.setupCompleted) {
      return _MaxWidth(
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: const [BodyEnergyCalibrationCard()],
        ),
      );
    }

    final result = state.result;
    if (state.isLoading && result == null) {
      return const FullScreenLoading();
    }
    if (result == null && state.error != null) {
      return ErrorMessage(_errorText(state.error!, l10n));
    }

    // Precomputed at load time by the view-model; empty until the first one
    // lands (the screen renders the day navigator over it either way).
    final display = state.display ?? const BodyEnergyDisplay();
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
      _CardPad(child: _SummaryCard(display: display)),
      _CardPad(child: _TimelineCard(display: display)),
      _CardPad(child: _ReasonsCard(display: display)),
      _CardPad(child: _InputsCard(display: display)),
      const _CardPad(child: _CalculationCard()),
      if (display.timeline?.confidence == BodyEnergyConfidence.low)
        _CardPad(
          child: _FootnoteCard(text: l10n.bodyEnergyTimelineLowConfidence),
        ),
      const SizedBox(height: 16),
    ];

    return RefreshIndicator(
      onRefresh: onRefresh,
      child: _MaxWidth(
        child: ListView(
          padding: const EdgeInsets.symmetric(vertical: 8),
          children: items,
        ),
      ),
    );
  }
}

class _MaxWidth extends StatelessWidget {
  const _MaxWidth({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 920),
          child: child,
        ),
      );
}

// ── Cards ────────────────────────────────────────────────────────────────────

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.display});

  final BodyEnergyDisplay display;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final timeline = display.timeline;
    final accent = _scoreColor(timeline?.currentScore, theme.colorScheme);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(Icons.battery_charging_full, color: accent, size: 28),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(l10n.screenBodyEnergy,
                          style: theme.textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w600)),
                      Text(l10n.bodyEnergyTimelineEstimated,
                          style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant)),
                    ],
                  ),
                ),
                Text(
                  timeline?.currentScore.toString() ?? '--',
                  style: theme.textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: accent,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                _stat(theme, l10n.bodyEnergyTimelineStart,
                    '${timeline?.startScore ?? '--'}'),
                _stat(theme, l10n.bodyEnergyTimelineCharged,
                    '+${timeline?.charged ?? 0}'),
                _stat(theme, l10n.bodyEnergyTimelineDrained,
                    '-${timeline?.drained ?? 0}'),
              ],
            ),
            const SizedBox(height: 12),
            Text(l10n.bodyEnergyTimelineConfidence,
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            Text(
              _confidenceLabel(
                  timeline?.confidence ?? BodyEnergyConfidence.noData),
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
            if ((timeline?.confidenceReason ?? '').isNotEmpty)
              Text(timeline!.confidenceReason,
                  style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }

  Widget _stat(ThemeData theme, String label, String value) => Expanded(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label,
                style: theme.textTheme.labelSmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            Text(value,
                style: theme.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.w600)),
          ],
        ),
      );
}

class _TimelineCard extends StatelessWidget {
  const _TimelineCard({required this.display});

  final BodyEnergyDisplay display;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.bodyEnergyTimelineDayTitle,
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.w600)),
            const SizedBox(height: 12),
            if (display.isEmpty)
              Text(l10n.bodyEnergyTimelineNoData,
                  style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant))
            else ...[
              BodyEnergyTimelineChart(
                points: display.chartPoints,
                influenceBars: display.influenceBars,
                maxMagnitude: display.maxInfluenceMagnitude,
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12,
                runSpacing: 6,
                children: [
                  for (final influence in display.legendInfluences)
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          color:
                              influenceColor(influence, theme.colorScheme),
                        ),
                        const SizedBox(width: 6),
                        Text(_influenceLabel(l10n, influence),
                            style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant)),
                      ],
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

class _ReasonsCard extends StatelessWidget {
  const _ReasonsCard({required this.display});

  final BodyEnergyDisplay display;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.bodyEnergyWhyTitle,
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.w600)),
            const SizedBox(height: 10),
            if (display.isEmpty || display.topReasons.isEmpty)
              Text(l10n.bodyEnergyWhyEmpty,
                  style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant))
            else
              for (final reason in display.topReasons)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        margin: const EdgeInsets.only(top: 4),
                        width: 10,
                        height: 10,
                        color: influenceColor(
                            reason.influence, theme.colorScheme),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(_influenceLabel(l10n, reason.influence),
                                style: theme.textTheme.bodyMedium
                                    ?.copyWith(fontWeight: FontWeight.w600)),
                            Text(_reasonDetail(l10n, reason.influence),
                                style: theme.textTheme.bodySmall?.copyWith(
                                    color:
                                        theme.colorScheme.onSurfaceVariant)),
                          ],
                        ),
                      ),
                      const SizedBox(width: 10),
                      Text(
                        reason.direction == BodyEnergyReasonDirection.charge
                            ? '+${reason.roundedAmount}'
                            : '-${reason.roundedAmount}',
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w600,
                          color: influenceColor(
                              reason.influence, theme.colorScheme),
                        ),
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

class _InputsCard extends StatelessWidget {
  const _InputsCard({required this.display});

  final BodyEnergyDisplay display;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final summary = display.inputSummary;
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
                Text(l10n.bodyEnergyInputsTitle,
                    style: theme.textTheme.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 10),
            if (summary != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Text(
                  l10n.bodyEnergyInputsSummary(
                      summary.algorithmVersion, summary.bucketMinutes),
                  style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant),
                ),
              ),
            for (final row in display.inputRows)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(
                  children: [
                    Container(
                      width: 9,
                      height: 9,
                      color: _statusColor(row.status, theme.colorScheme),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(_inputLabel(l10n, row.kind),
                          style: theme.textTheme.bodyMedium),
                    ),
                    Text(_inputStatusText(l10n, row),
                        style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant)),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// The "How Body Energy is estimated" explainer (Kotlin
/// `BodyEnergyCalculationCard`): a title plus three method paragraphs.
class _CalculationCard extends StatelessWidget {
  const _CalculationCard();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final bodyStyle = theme.textTheme.bodySmall
        ?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.bodyEnergyCalculationTitle,
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.w600)),
            const SizedBox(height: 10),
            Text(l10n.bodyEnergyCalculationBody, style: bodyStyle),
            const SizedBox(height: 10),
            Text(l10n.bodyEnergyCalculationInputsBody, style: bodyStyle),
            const SizedBox(height: 10),
            Text(l10n.bodyEnergyCalculationLimitsBody, style: bodyStyle),
          ],
        ),
      ),
    );
  }
}

class _FootnoteCard extends StatelessWidget {
  const _FootnoteCard({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(text,
            style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant)),
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

Color _scoreColor(int? score, ColorScheme scheme) {
  if (score == null) return scheme.onSurfaceVariant;
  if (score >= 80) return scheme.primary;
  if (score >= 60) return scheme.tertiary;
  if (score >= 40) return scheme.secondary;
  return scheme.error;
}

Color _statusColor(BodyEnergyInputStatus status, ColorScheme scheme) {
  switch (status) {
    case BodyEnergyInputStatus.available:
      return scheme.primary;
    case BodyEnergyInputStatus.missing:
      return scheme.error;
    case BodyEnergyInputStatus.optional:
      return scheme.outline;
  }
}

/// The confidence chip value. Matches the Kotlin `confidenceText`, which renders
/// these labels as literals (the screen never localizes the confidence value).
String _confidenceLabel(BodyEnergyConfidence confidence) {
  switch (confidence) {
    case BodyEnergyConfidence.high:
      return 'High';
    case BodyEnergyConfidence.medium:
      return 'Medium';
    case BodyEnergyConfidence.low:
      return 'Low';
    case BodyEnergyConfidence.noData:
      return 'No data';
  }
}

String _influenceLabel(
    AppLocalizations l10n, BodyEnergyPrimaryInfluence influence) {
  switch (influence) {
    case BodyEnergyPrimaryInfluence.sleepRecovery:
      return l10n.bodyEnergyInfluenceSleepRecovery;
    case BodyEnergyPrimaryInfluence.quietRest:
      return l10n.bodyEnergyInfluenceQuietRest;
    case BodyEnergyPrimaryInfluence.exertion:
      return l10n.bodyEnergyInfluenceExertion;
    case BodyEnergyPrimaryInfluence.elevatedHeartRate:
      return l10n.bodyEnergyInfluenceElevatedHr;
    case BodyEnergyPrimaryInfluence.recoveryDebt:
      return l10n.bodyEnergyInfluenceRecoveryDebt;
    case BodyEnergyPrimaryInfluence.noData:
      return l10n.bodyEnergyInfluenceNoData;
    case BodyEnergyPrimaryInfluence.steady:
      return l10n.bodyEnergyInfluenceSteady;
  }
}

String _reasonDetail(
    AppLocalizations l10n, BodyEnergyPrimaryInfluence influence) {
  switch (influence) {
    case BodyEnergyPrimaryInfluence.sleepRecovery:
      return l10n.bodyEnergyReasonSleepRecoveryDetail;
    case BodyEnergyPrimaryInfluence.quietRest:
      return l10n.bodyEnergyReasonQuietRestDetail;
    case BodyEnergyPrimaryInfluence.exertion:
      return l10n.bodyEnergyReasonExertionDetail;
    case BodyEnergyPrimaryInfluence.elevatedHeartRate:
      return l10n.bodyEnergyReasonElevatedHrDetail;
    case BodyEnergyPrimaryInfluence.recoveryDebt:
      return l10n.bodyEnergyReasonRecoveryDebtDetail;
    case BodyEnergyPrimaryInfluence.noData:
      return l10n.bodyEnergyReasonNoDataDetail;
    case BodyEnergyPrimaryInfluence.steady:
      return l10n.bodyEnergyReasonSteadyDetail;
  }
}

String _inputLabel(AppLocalizations l10n, BodyEnergyInputKind kind) {
  switch (kind) {
    case BodyEnergyInputKind.heartRate:
      return l10n.bodyEnergyInputHeartRate;
    case BodyEnergyInputKind.sleep:
      return l10n.bodyEnergyInputSleep;
    case BodyEnergyInputKind.workouts:
      return l10n.bodyEnergyInputWorkouts;
    case BodyEnergyInputKind.restingHeartRate:
      return l10n.bodyEnergyInputRestingHr;
    case BodyEnergyInputKind.heartRateBaseline:
      return l10n.bodyEnergyInputHrBaseline;
    case BodyEnergyInputKind.hrv:
      return l10n.bodyEnergyInputHrv;
    case BodyEnergyInputKind.respiratoryRate:
      return l10n.bodyEnergyInputRespiratory;
    case BodyEnergyInputKind.previousScore:
      return l10n.bodyEnergyInputPreviousScore;
    case BodyEnergyInputKind.calibration:
      return l10n.bodyEnergyInputCalibration;
  }
}

String _inputStatusText(AppLocalizations l10n, BodyEnergyInputRow row) {
  switch (row.kind) {
    case BodyEnergyInputKind.heartRate:
    case BodyEnergyInputKind.hrv:
    case BodyEnergyInputKind.respiratoryRate:
      final count = row.count;
      if (count != null) return l10n.bodyEnergyInputRecords(count);
      return _statusText(l10n, row.status);
    case BodyEnergyInputKind.sleep:
      final count = row.count;
      if (count != null) return l10n.bodyEnergyInputSessions(count);
      return _statusText(l10n, row.status);
    case BodyEnergyInputKind.workouts:
      final count = row.count;
      if (count != null) return l10n.bodyEnergyInputWorkoutsValue(count);
      return _statusText(l10n, row.status);
    case BodyEnergyInputKind.previousScore:
      final value = row.value;
      if (value != null) return l10n.bodyEnergyInputPreviousScoreValue(value);
      return _statusText(l10n, row.status);
    case BodyEnergyInputKind.calibration:
      return _calibrationModeLabel(l10n, row.value);
    case BodyEnergyInputKind.restingHeartRate:
    case BodyEnergyInputKind.heartRateBaseline:
      return _statusText(l10n, row.status);
  }
}

String _statusText(AppLocalizations l10n, BodyEnergyInputStatus status) {
  switch (status) {
    case BodyEnergyInputStatus.available:
      return l10n.bodyEnergyInputAvailable;
    case BodyEnergyInputStatus.missing:
      return l10n.bodyEnergyInputMissing;
    case BodyEnergyInputStatus.optional:
      return l10n.bodyEnergyInputOptional;
  }
}

/// Resolves the calibration-mode enum name into its localized label (Kotlin
/// `calibrationModeLabel`), defaulting to automatic on an unknown value.
String _calibrationModeLabel(AppLocalizations l10n, String? name) {
  final mode = BodyEnergyCalibrationMode.values.firstWhere(
    (m) => m.name == name,
    orElse: () => BodyEnergyCalibrationMode.automatic,
  );
  switch (mode) {
    case BodyEnergyCalibrationMode.automatic:
      return l10n.bodyEnergyCalibrationModeAuto;
    case BodyEnergyCalibrationMode.manualValues:
      return l10n.bodyEnergyCalibrationModeManualValues;
    case BodyEnergyCalibrationMode.manualZones:
      return l10n.bodyEnergyCalibrationModeManualZones;
  }
}

/// Resolves a [ScreenError] into a display string. Mirrors the Kotlin
/// `ScreenError.resolve() ?: stringResource(R.string.unknown_error)`.
String _errorText(ScreenError error, AppLocalizations l10n) =>
    error is ScreenErrorMessage ? error.text : l10n.unknownError;

/// Parses an ISO `yyyy-MM-dd` argument into a [LocalDate] (falling back to today
/// on any malformed input).
LocalDate parseIsoLocalDate(String value) {
  final parsed = DateTime.tryParse(value);
  if (parsed != null) return LocalDate(parsed.year, parsed.month, parsed.day);
  return LocalDate.now();
}
