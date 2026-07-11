import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_range_preference_key.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/model/vitals_models.dart';
import '../../domain/preferences/metric_detail_section_id.dart';
import '../../domain/usecase/load_heart_period_use_case.dart';
import '../../health/health_permissions.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../../ui/charts/line_chart.dart';
import '../../ui/components/data_source_education_item.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import '../../ui/theme/app_colors.dart';
import '../heart/heart_metric.dart';
import '../heart/heart_metric_cards.dart';
import '../../core/stats/stats.dart';

// Vitals accent colours, ported from the Kotlin `HeartVitalsPresentation.kt`.
const Color _oxygenColor = Color(0xFF00897B);
const Color _respiratoryColor = Color(0xFF5E97F6);
const Color _temperatureColor = Color(0xFFFF7043);
const Color _vo2Color = Color(0xFF7E57C2);
const Color _glucoseColor = Color(0xFF8E5D42);

/// The combined heart & vitals overview state, port of the slice of the Kotlin
/// `HeartUiState` the overview consumes: the scaffold-driven selection, the
/// loaded [HeartPeriodLoadResult] payload and loading/error flags.
@immutable
class HeartVitalsOverviewState {
  const HeartVitalsOverviewState({
    required this.selectedDate,
    this.selectedRange = TimeRange.week,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
    this.isLoading = true,
    this.error,
    this.result,
  });

  final LocalDate selectedDate;
  final TimeRange selectedRange;

  /// The loaded period's week mode, carried on the state (as `SleepState` does)
  /// so the section summaries name the window exactly as the period navigator
  /// does — "Last 30 days" on a rolling month, not "This month".
  final WeekPeriodMode weekPeriodMode;
  final bool isLoading;
  final ScreenError? error;
  final HeartPeriodLoadResult? result;

  HeartVitalsOverviewState copyWith({
    LocalDate? selectedDate,
    TimeRange? selectedRange,
    WeekPeriodMode? weekPeriodMode,
    bool? isLoading,
    ScreenError? error,
    bool clearError = false,
    HeartPeriodLoadResult? result,
  }) =>
      HeartVitalsOverviewState(
        selectedDate: selectedDate ?? this.selectedDate,
        selectedRange: selectedRange ?? this.selectedRange,
        weekPeriodMode: weekPeriodMode ?? this.weekPeriodMode,
        isLoading: isLoading ?? this.isLoading,
        error: clearError ? null : (error ?? this.error),
        result: result ?? this.result,
      );
}

/// The Riverpod port of the overview slice of the Kotlin `HeartViewModel`: the
/// owning [MetricDetailScaffold] drives every load through [load] and
/// pull-to-refresh through [refresh]. A monotonic [_generation] guard drops
/// stale results. It always issues the combined heart + vitals load.
class HeartVitalsOverviewNotifier extends Notifier<HeartVitalsOverviewState> {
  int _generation = 0;

  @override
  HeartVitalsOverviewState build() =>
      HeartVitalsOverviewState(selectedDate: LocalDate.now());

  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadHeartPeriodUseCaseProvider);

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
      isLoading: true,
      clearError: true,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    try {
      final result = await useCase(
        query,
        const HeartPeriodLoadCombined(),
        refreshMode: refreshMode,
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, result: result, clearError: true);
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error,
            fallback: 'Unable to load heart & vitals.'),
      );
    }
  }

  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

final heartVitalsOverviewNotifierProvider =
    NotifierProvider<HeartVitalsOverviewNotifier, HeartVitalsOverviewState>(
        HeartVitalsOverviewNotifier.new);

/// The combined heart & vitals overview (`/heart_vitals`), a port of the Kotlin
/// `HeartVitalsOverviewScreen` + `VitalsOverviewContent`: one range-driven
/// scrolling screen that renders every heart and vitals metric grouped into the
/// three user-reorderable sections Kotlin uses — `VITALS_HEART_SECTION`,
/// `VITALS_CARDIOVASCULAR_SECTION` and `VITALS_RESPIRATORY_SECTION`. Each
/// section shows a 2-per-row grid of [MetricCard]s (tapping one opens its
/// `/metric/<routeName>` detail) followed by that group's trend charts.
class HeartVitalsOverviewScreen extends ConsumerWidget {
  const HeartVitalsOverviewScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(heartVitalsOverviewNotifierProvider);
    final notifier = ref.read(heartVitalsOverviewNotifierProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.screenHeartVitals),
        actions: [
          // Kotlin hoists this toggle into the host app bar through
          // `onSectionEditStateChanged`; the same affordance, wired locally.
          IconButton(
            onPressed:
                ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? l10n.cdFinishMetricSectionEditing
                : l10n.cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readHeartRate,
          HcPermissions.readRestingHeartRate,
          HcPermissions.readHrv,
        },
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          // The Kotlin `HeartViewModel` keys the remembered range on
          // `PeriodRangePreferenceKey.HEART`.
          rangePreferenceKey: PeriodRangePreferenceKey.heart,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: (selection) => notifier.load(selection),
          content: (period) => [
            _OverviewContent(
              state: state,
              period: period,
              formatter: formatter,
            ),
          ],
        ),
      ),
    );
  }
}

class _OverviewContent extends StatelessWidget {
  const _OverviewContent({
    required this.state,
    required this.period,
    required this.formatter,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final result = state.result;
    if (result == null && state.isLoading) return const _LoadingBlock();
    final data = result ?? const HeartPeriodLoadResult();

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _sections(context, data, daySelection),
    );
  }

  Widget _sections(
    BuildContext context,
    HeartPeriodLoadResult result,
    ChartDaySelection daySelection,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OrderedMetricDetailSections(
          sections: [
            MetricDetailSection(
              MetricDetailSectionId.vitalsHeartSection,
              _HeartSection(
                state: state,
                period: period,
                formatter: formatter,
                result: result,
                daySelection: daySelection,
              ),
            ),
            MetricDetailSection(
              MetricDetailSectionId.vitalsCardiovascularSection,
              _CardiovascularSection(
                state: state,
                period: period,
                formatter: formatter,
                result: result,
                daySelection: daySelection,
              ),
            ),
            MetricDetailSection(
              MetricDetailSectionId.vitalsRespiratorySection,
              _RespiratorySection(
                state: state,
                period: period,
                formatter: formatter,
                result: result,
                daySelection: daySelection,
              ),
            ),
          ],
        ),
        // Kotlin `HeartVitalsOverviewScreen` renders `dataSourceEducationItem()`
        // as a bare trailing item after the grouped sections (line 155).
        const DataSourceEducationItem(),
      ],
    );
  }
}

// ── Sections ─────────────────────────────────────────────────────────────────

/// Kotlin `VitalsHeartOverviewSectionContent`.
class _HeartSection extends StatelessWidget {
  const _HeartSection({
    required this.state,
    required this.period,
    required this.formatter,
    required this.result,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartPeriodLoadResult result;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final isDay = state.selectedRange == TimeRange.day;

    final daySamples = [...result.daySamples]
      ..sort((a, b) => a.time.compareTo(b.time));
    final summaries = [...result.dailySummaries]
      ..sort((a, b) => a.date.compareTo(b.date));
    final restingHr = [...result.dailyRestingHR]
      ..sort((a, b) => a.date.compareTo(b.date));
    final hrv = [...result.dailyHrv]..sort((a, b) => a.date.compareTo(b.date));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionHeart),
        _MetricCardGrid(cards: [
          _OverviewCard(
            metric: HeartMetric.averageHeartRate,
            title: l10n.metricAverageHeartRate,
            value: _averageHeartRateValue(result),
            icon: Icons.favorite,
            color: AppColors.heart,
            source:
                isDay ? _singleSource(daySamples.map((s) => s.source)) : null,
          ),
          _OverviewCard(
            metric: HeartMetric.restingHeartRate,
            title: l10n.metricRestingHeartRate,
            value: _restingHeartRateValue(result),
            icon: Icons.favorite_border,
            color: AppColors.heart,
          ),
          _OverviewCard(
            metric: HeartMetric.hrv,
            title: l10n.metricHrv,
            value: _hrvValue(result),
            icon: Icons.speed,
            color: AppColors.heart.withValues(alpha: 0.85),
          ),
        ]),
        // Kotlin `HeartOverviewChartsContent`.
        if (isDay && daySamples.length > 1)
          _padded(_dayTimeline(context))
        else if (!isDay && summaries.isNotEmpty)
          _padded(_heartRateChart(context, summaries)),
        if (!isDay && restingHr.isNotEmpty)
          _padded(_restingChart(context, restingHr)),
        if (!isDay && hrv.isNotEmpty) _padded(_hrvChart(context, hrv)),
      ],
    );
  }

  Widget _dayTimeline(BuildContext context) {
    final bpm = result.daySamples.map((s) => s.beatsPerMinute.toDouble());
    final minBpm = _min(bpm).round();
    final maxBpm = _max(bpm).round();
    return HeartTimelineCard(
      date: state.selectedDate,
      points: [
        for (final s in result.daySamples)
          (s.time, s.beatsPerMinute.toDouble()),
      ],
      averageText: formatter.heartRate(_avg(bpm).round()).text,
      rangeText:
          '${formatter.heartRate(minBpm).text}-${formatter.heartRate(maxBpm).text}',
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
      minValue: math.max(30, minBpm - 5).toDouble(),
      maxValue: (maxBpm + 5).toDouble(),
    );
  }

  Widget _heartRateChart(
      BuildContext context, List<HeartRateSummary> summaries) {
    final l10n = AppLocalizations.of(context);
    final average = _avg(summaries.map((s) => s.avgBpm.toDouble())).round();
    final lowest = summaries.map((s) => s.minBpm).reduce(math.min);
    final highest = summaries.map((s) => s.maxBpm).reduce(math.max);
    final hasRange = summaries.any((s) => s.minBpm != s.maxBpm);
    return MetricLineChart(
      title: l10n.metricAverageHeartRate,
      series: [
        MetricLineSeries(
          points: [
            for (final s in summaries)
              MetricLinePoint(date: s.date, value: s.avgBpm.toDouble()),
          ],
          color: AppColors.heart,
          label: l10n.summaryAverage,
        ),
        if (hasRange) ...[
          MetricLineSeries(
            points: [
              for (final s in summaries)
                MetricLinePoint(date: s.date, value: s.minBpm.toDouble()),
            ],
            color: AppColors.heart.withValues(alpha: 0.55),
            label: l10n.statLowest,
          ),
          MetricLineSeries(
            points: [
              for (final s in summaries)
                MetricLinePoint(date: s.date, value: s.maxBpm.toDouble()),
            ],
            color: AppColors.heart.withValues(alpha: 0.9),
            label: l10n.statHighest,
          ),
        ],
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart,
      summaryText: _summary(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.heartRate(average).text,
          formatter.heartRate(lowest).text,
          formatter.heartRate(highest).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
    );
  }

  Widget _restingChart(BuildContext context, List<DailyRestingHR> resting) {
    final l10n = AppLocalizations.of(context);
    final bpm = resting.map((e) => e.bpm.toDouble()).toList();
    final average = _avg(bpm).round();
    return MetricLineChart(
      title: l10n.metricRestingHeartRate,
      series: [
        MetricLineSeries(
          points: [
            for (final e in resting)
              MetricLinePoint(date: e.date, value: e.bpm.toDouble()),
          ],
          color: AppColors.heart,
        ),
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart,
      summaryText: _summary(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.heartRate(average).text,
          formatter.heartRate(_min(bpm).round()).text,
          formatter.heartRate(_max(bpm).round()).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.heartRate(value.round()).text,
    );
  }

  Widget _hrvChart(BuildContext context, List<DailyHrv> hrv) {
    final l10n = AppLocalizations.of(context);
    final ms = hrv.map((e) => e.rmssdMs).toList();
    return MetricLineChart(
      title: l10n.metricHrv,
      series: [
        MetricLineSeries(
          points: [
            for (final e in hrv)
              MetricLinePoint(date: e.date, value: e.rmssdMs),
          ],
          color: AppColors.heart.withValues(alpha: 0.85),
        ),
      ],
      selectedRange: state.selectedRange,
      period: period,
      accentColor: AppColors.heart.withValues(alpha: 0.85),
      summaryText: _summary(
        l10n,
        state,
        period,
        l10n.summaryAvgValueRange(
          formatter.hrv(_avg(ms)).text,
          formatter.hrv(_min(ms)).text,
          formatter.hrv(_max(ms)).text,
        ),
      ),
      selectedDate: daySelection.selectedDate,
      onDateSelected: daySelection.onDateSelected,
      valueFormatter: (value) => formatter.hrv(value).text,
    );
  }

  DisplayValue? _averageHeartRateValue(HeartPeriodLoadResult result) {
    if (state.selectedRange == TimeRange.day) {
      if (result.daySamples.isEmpty) return null;
      return formatter
          .heartRate(_avg(result.daySamples.map((s) => s.beatsPerMinute.toDouble())).round());
    }
    if (result.dailySummaries.isEmpty) return null;
    return formatter.heartRate(
        _avg(result.dailySummaries.map((s) => s.avgBpm.toDouble())).round());
  }

  DisplayValue? _restingHeartRateValue(HeartPeriodLoadResult result) {
    if (state.selectedRange == TimeRange.day) {
      final bpm = result.dayRestingBpm;
      return bpm == null ? null : formatter.heartRate(bpm);
    }
    if (result.dailyRestingHR.isEmpty) return null;
    return formatter
        .heartRate(_avg(result.dailyRestingHR.map((e) => e.bpm.toDouble())).round());
  }

  DisplayValue? _hrvValue(HeartPeriodLoadResult result) {
    if (state.selectedRange == TimeRange.day) {
      final ms = result.dayHrvMs;
      return ms == null ? null : formatter.hrv(ms);
    }
    if (result.dailyHrv.isEmpty) return null;
    return formatter.hrv(_avg(result.dailyHrv.map((e) => e.rmssdMs)));
  }
}

/// Kotlin `VitalsCardiovascularOverviewSectionContent`.
class _CardiovascularSection extends StatelessWidget {
  const _CardiovascularSection({
    required this.state,
    required this.period,
    required this.formatter,
    required this.result,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartPeriodLoadResult result;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final vitals = result.vitalsSummary();
    final range = state.selectedRange;

    final bloodPressure = [...result.bloodPressure]
      ..sort((a, b) => a.time.compareTo(b.time));
    final spO2 = [...result.spO2]..sort((a, b) => a.time.compareTo(b.time));
    final vo2Max = [...result.vo2Max]..sort((a, b) => a.time.compareTo(b.time));
    final bloodGlucose = [...result.bloodGlucose]
      ..sort((a, b) => a.time.compareTo(b.time));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionCardiovascular),
        _MetricCardGrid(cards: [
          _OverviewCard(
            metric: HeartMetric.bloodPressure,
            title: l10n.metricBloodPressure,
            value: vitals.latestBloodPressure == null
                ? null
                : formatter.bloodPressure(
                    vitals.latestBloodPressure!.systolicMmHg,
                    vitals.latestBloodPressure!.diastolicMmHg),
            icon: Icons.favorite,
            color: AppColors.vitals,
            source: vitals.latestBloodPressure?.source,
          ),
          _OverviewCard(
            metric: HeartMetric.spo2,
            title: l10n.metricSpo2,
            value: vitals.latestSpO2 == null
                ? null
                : formatter.percent(vitals.latestSpO2!.percent),
            icon: Icons.favorite,
            color: _oxygenColor,
            source: vitals.latestSpO2?.source,
          ),
          _OverviewCard(
            metric: HeartMetric.vo2Max,
            title: l10n.metricVo2Max,
            value: vitals.latestVo2Max == null
                ? null
                : formatter.vo2Max(vitals.latestVo2Max!.vo2MaxMlPerKgPerMin),
            icon: Icons.speed,
            color: _vo2Color,
            source: vitals.latestVo2Max?.source,
          ),
          _OverviewCard(
            metric: HeartMetric.bloodGlucose,
            title: l10n.metricBloodGlucose,
            value: vitals.latestBloodGlucose == null
                ? null
                : formatter
                    .bloodGlucose(vitals.latestBloodGlucose!.millimolesPerLiter),
            icon: Icons.favorite,
            color: _glucoseColor,
            source: vitals.latestBloodGlucose?.source,
          ),
        ]),
        // Kotlin `CardiovascularOverviewChartsContent`.
        if (_hasRenderableChartData(bloodPressure, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricBloodPressure,
            series: _bloodPressureSeries(bloodPressure, l10n, range),
            selectedRange: range,
            period: period,
            accentColor: AppColors.vitals,
            summaryText: _summary(l10n, state, period,
                l10n.summaryReadings(formatter.count(bloodPressure.length))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => '${value.round()} mmHg',
          )),
        if (_hasRenderableChartData(spO2, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricSpo2,
            series: _singleSeries(
              [for (final e in spO2) (e.time, e.percent)],
              _oxygenColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: _oxygenColor,
            summaryText: _summary(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                  formatter.percent(_avg(spO2.map((e) => e.percent))).text),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.percent(value).text,
          )),
        if (_hasRenderableChartData(vo2Max, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricVo2Max,
            series: _singleSeries(
              [for (final e in vo2Max) (e.time, e.vo2MaxMlPerKgPerMin)],
              _vo2Color,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: _vo2Color,
            summaryText: _summary(l10n, state, period,
                l10n.summaryReadings(formatter.count(vo2Max.length))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.vo2Max(value).text,
          )),
        if (_hasRenderableChartData(bloodGlucose, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricBloodGlucose,
            series: _singleSeries(
              [for (final e in bloodGlucose) (e.time, e.millimolesPerLiter)],
              _glucoseColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: _glucoseColor,
            summaryText: _summary(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(formatter
                  .bloodGlucose(_avg(bloodGlucose.map((e) => e.millimolesPerLiter)))
                  .text),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.bloodGlucose(value).text,
          )),
      ],
    );
  }
}

/// Kotlin `VitalsRespiratoryOverviewSectionContent`.
class _RespiratorySection extends StatelessWidget {
  const _RespiratorySection({
    required this.state,
    required this.period,
    required this.formatter,
    required this.result,
    required this.daySelection,
  });

  final HeartVitalsOverviewState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final HeartPeriodLoadResult result;
  final ChartDaySelection daySelection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final vitals = result.vitalsSummary();
    final range = state.selectedRange;
    final isDay = range == TimeRange.day;

    final respiratoryRate = [...result.respiratoryRate]
      ..sort((a, b) => a.time.compareTo(b.time));
    final bodyTemperature = [...result.bodyTemperature]
      ..sort((a, b) => a.time.compareTo(b.time));
    final skinTemperature = [
      for (final e in result.skinTemperature)
        if (e.averageDeltaCelsius != null) e,
    ]..sort((a, b) => a.time.compareTo(b.time));

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(l10n.sectionRespiratory),
        _MetricCardGrid(cards: [
          _OverviewCard(
            metric: HeartMetric.respiratoryRate,
            title: l10n.metricRespiratoryRate,
            value: _respiratoryRateValue(respiratoryRate),
            icon: Icons.air,
            color: _respiratoryColor,
            source: isDay
                ? vitals.latestRespiratoryRate?.source
                : _singleSource(respiratoryRate.map((e) => e.source)),
          ),
          _OverviewCard(
            metric: HeartMetric.bodyTemperature,
            title: l10n.metricBodyTemp,
            value: vitals.latestBodyTemperature == null
                ? null
                : formatter
                    .temperature(vitals.latestBodyTemperature!.temperatureCelsius),
            icon: Icons.device_thermostat,
            color: _temperatureColor,
            source: vitals.latestBodyTemperature?.source,
          ),
          _OverviewCard(
            metric: HeartMetric.skinTemperature,
            title: l10n.metricSkinTemperature,
            value: vitals.latestSkinTemperature?.averageDeltaCelsius == null
                ? null
                : formatter.temperatureDelta(
                    vitals.latestSkinTemperature!.averageDeltaCelsius!),
            icon: Icons.device_thermostat,
            color: _temperatureColor,
            source: vitals.latestSkinTemperature?.source,
          ),
        ]),
        // Kotlin `RespiratoryOverviewChartsContent`.
        if (_hasRenderableChartData(respiratoryRate, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricRespiratoryRate,
            series: _respiratoryRateSeries(respiratoryRate, l10n, range),
            selectedRange: range,
            period: period,
            accentColor: _respiratoryColor,
            summaryText: _summary(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                formatter
                    .respiratoryRate(_avg(respiratoryRateDaySummaries(respiratoryRate)
                        .map((s) => s.average)))
                    .text,
              ),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.respiratoryRate(value).text,
          )),
        if (_hasRenderableChartData(bodyTemperature, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricBodyTemp,
            series: _singleSeries(
              [for (final e in bodyTemperature) (e.time, e.temperatureCelsius)],
              _temperatureColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: _temperatureColor,
            summaryText: _summary(l10n, state, period,
                l10n.summaryReadings(formatter.count(bodyTemperature.length))),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.temperature(value).text,
          )),
        if (_hasRenderableChartData(skinTemperature, range, (e) => e.time))
          _padded(MetricLineChart(
            title: l10n.metricSkinTemperature,
            series: _singleSeries(
              [for (final e in skinTemperature) (e.time, e.averageDeltaCelsius!)],
              _temperatureColor,
              range,
            ),
            selectedRange: range,
            period: period,
            accentColor: _temperatureColor,
            summaryText: _summary(
              l10n,
              state,
              period,
              l10n.summaryValueAvg(
                formatter
                    .temperatureDelta(_avg(
                        skinTemperature.map((e) => e.averageDeltaCelsius!)))
                    .text,
              ),
            ),
            selectedDate: daySelection.selectedDate,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => formatter.temperatureDelta(value).text,
          )),
      ],
    );
  }

  DisplayValue? _respiratoryRateValue(List<RespiratoryRateEntry> entries) {
    if (entries.isEmpty) return null;
    if (state.selectedRange == TimeRange.day) {
      final latest = entries.last;
      return formatter.respiratoryRate(latest.breathsPerMinute);
    }
    final summaries = respiratoryRateDaySummaries(entries);
    if (summaries.isEmpty) return null;
    return formatter.respiratoryRate(_avg(summaries.map((s) => s.average)));
  }
}

/// Kotlin `respiratoryRateSeries`: raw within a day; daily average plus
/// min/max range series otherwise.
List<MetricLineSeries> _respiratoryRateSeries(
  List<RespiratoryRateEntry> sorted,
  AppLocalizations l10n,
  TimeRange range,
) {
  if (range == TimeRange.day) {
    return _singleSeries(
      [for (final e in sorted) (e.time, e.breathsPerMinute)],
      _respiratoryColor,
      range,
      label: l10n.metricRespiratoryRate,
    );
  }
    final byDate = <LocalDate, List<double>>{};
    for (final e in sorted) {
      byDate
          .putIfAbsent(instantToLocalDate(e.time), () => <double>[])
          .add(e.breathsPerMinute);
    }
    final dates = byDate.keys.toList()..sort((a, b) => a.compareTo(b));
    final average = <MetricLinePoint>[];
    final min = <MetricLinePoint>[];
    final max = <MetricLinePoint>[];
    for (final date in dates) {
      final values = byDate[date]!;
      average.add(MetricLinePoint(date: date, value: _avg(values)));
      min.add(MetricLinePoint(date: date, value: _min(values)));
      max.add(MetricLinePoint(date: date, value: _max(values)));
    }
    final hasRange = [
      for (var i = 0; i < min.length; i++) min[i].value != max[i].value,
    ].any((different) => different);
    return [
      MetricLineSeries(
        points: average,
        color: _respiratoryColor,
        label: l10n.summaryAverage,
      ),
      if (hasRange) ...[
        MetricLineSeries(
          points: min,
          color: _respiratoryColor.withValues(alpha: 0.55),
          label: l10n.statLowest,
        ),
        MetricLineSeries(
          points: max,
          color: AppColors.vitals.withValues(alpha: 0.75),
          label: l10n.statHighest,
        ),
      ],
    ];
  }

/// Kotlin `bloodPressureSeries`: systolic (VitalsColor) + diastolic
/// (HeartColor); raw within a day, daily averages otherwise.
List<MetricLineSeries> _bloodPressureSeries(
  List<BloodPressureEntry> sorted,
  AppLocalizations l10n,
  TimeRange range,
) {
  final isDay = range == TimeRange.day;
    final systolic = [
      for (final e in sorted)
        MetricLinePoint(
          date: instantToLocalDate(e.time),
          value: e.systolicMmHg.toDouble(),
          time: e.time,
        ),
    ];
    final diastolic = [
      for (final e in sorted)
        MetricLinePoint(
          date: instantToLocalDate(e.time),
          value: e.diastolicMmHg.toDouble(),
          time: e.time,
        ),
    ];
    return [
      MetricLineSeries(
        points: isDay ? systolic : dailyAverageLinePoints(systolic),
        color: AppColors.vitals,
        label: l10n.vitalsEntrySystolicLabel,
      ),
      MetricLineSeries(
        points: isDay ? diastolic : dailyAverageLinePoints(diastolic),
        color: AppColors.heart,
        label: l10n.vitalsEntryDiastolicLabel,
      ),
    ];
  }

/// Raw points within a day; daily averages otherwise.
List<MetricLineSeries> _singleSeries(
  List<(DateTime, double)> raw,
  Color color,
  TimeRange range, {
  String? label,
}) {
  final base = [
    for (final (time, value) in raw)
      MetricLinePoint(
        date: instantToLocalDate(time),
        value: value,
        time: time,
      ),
  ];
  final points =
      range == TimeRange.day ? base : dailyAverageLinePoints(base);
  return [MetricLineSeries(points: points, color: color, label: label)];
}

// ── Card grid ────────────────────────────────────────────────────────────────

/// A metric summary card, port of Kotlin `OverviewMetricCardData`.
@immutable
class _OverviewCard {
  const _OverviewCard({
    required this.metric,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
    this.source,
  });

  final HeartMetric metric;
  final String title;
  final DisplayValue? value;
  final IconData icon;
  final Color color;
  final String? source;
}

/// Kotlin `OverviewMetricRowsContent`: the metric cards laid out two per row,
/// each an [MetricCard]/[MetricCardPlaceholder] that opens its detail screen.
class _MetricCardGrid extends StatelessWidget {
  const _MetricCardGrid({required this.cards});

  final List<_OverviewCard> cards;

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[];
    for (var i = 0; i < cards.length; i += 2) {
      final first = cards[i];
      final second = i + 1 < cards.length ? cards[i + 1] : null;
      rows.add(Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: _card(context, first)),
            const SizedBox(width: 12),
            Expanded(
              child: second == null
                  ? const SizedBox.shrink()
                  : _card(context, second),
            ),
          ],
        ),
      ));
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: rows);
  }

  Widget _card(BuildContext context, _OverviewCard card) {
    final l10n = AppLocalizations.of(context);
    void open() =>
        context.push(AppRoutes.metricLocation(card.metric.routeName));
    final value = card.value;
    if (value == null) {
      return MetricCardPlaceholder(
        title: card.title,
        icon: card.icon,
        accentColor: card.color,
        message: l10n.messageNoReadingsPeriod,
        onTap: open,
      );
    }
    return MetricCard(
      title: card.title,
      value: value.value,
      unit: value.unit,
      icon: card.icon,
      accentColor: card.color,
      source: card.source,
      onTap: open,
    );
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: child,
    );

String _summary(
  AppLocalizations l10n,
  HeartVitalsOverviewState state,
  DatePeriod period,
  String extra,
) =>
    '${periodTitle(
      l10n,
      state.selectedRange,
      period,
      weekPeriodMode: state.weekPeriodMode,
    )} · $extra';

/// Kotlin `hasRenderableChartData`: within a day, needs more than one distinct
/// timestamp; otherwise any reading renders.
bool _hasRenderableChartData<T>(
  List<T> entries,
  TimeRange range,
  DateTime Function(T) time,
) {
  if (range == TimeRange.day) {
    return entries.map((e) => time(e).millisecondsSinceEpoch).toSet().length > 1;
  }
  return entries.isNotEmpty;
}

String? _singleSource(Iterable<String> sources) {
  final distinct = sources.toSet();
  return distinct.length == 1 ? distinct.first : null;
}

/// Zero on empty is preserved from the hand-rolled originals, but it is dead code:
/// every call site is already guarded on `isNotEmpty`. Same for the bang on
/// [minOf]/[maxOf], whose ancestors threw on empty for the same unreachable case.
double _avg(Iterable<double> values) => averageOrZero(values);

double _min(Iterable<double> values) => minOf(values)!;

double _max(Iterable<double> values) => maxOf(values)!;

class _LoadingBlock extends StatelessWidget {
  const _LoadingBlock();

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      );
}
