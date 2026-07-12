import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/presentation/display_value.dart';
import '../../core/presentation/external_link.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/sleep_score.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/components/loading_state.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'recovery_detail_view_model.dart';

const String _aasmSleepDurationUrl =
    'https://aasm.org/advocacy/position-statements/adult-sleep-duration-health-advisory/';
const String _sleepHealthFrameworkUrl =
    'https://pubmed.ncbi.nlm.nih.gov/24470692/';
const String _sleepEfficiencyUrl = 'https://www.ncbi.nlm.nih.gov/medgen/1669302';
const String _sleepRegularityUrl =
    'https://www.nature.com/articles/s41598-017-03171-4';

/// Sleep-score detail pushed over the shell (`/recovery/sleep_score`).
/// Port of the Kotlin `SleepScoreDetailScreen`: summary card, "how it is
/// calculated" explanation, the day's numbers, and reference links.
class SleepScoreDetailScreen extends ConsumerStatefulWidget {
  const SleepScoreDetailScreen({super.key});

  @override
  ConsumerState<SleepScoreDetailScreen> createState() =>
      _SleepScoreDetailScreenState();
}

class _SleepScoreDetailScreenState
    extends ConsumerState<SleepScoreDetailScreen> {
  bool _showCalculation = false;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(recoveryDetailProvider);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.recoverySleepScore)),
      body: _body(context, l10n, state, formatter),
    );
  }

  Widget _body(
    BuildContext context,
    AppLocalizations l10n,
    RecoveryDetailState state,
    UnitFormatter formatter,
  ) {
    if (state.isLoading && state.days.isEmpty) {
      return const FullScreenLoading();
    }
    final error = state.error;
    if (error != null && state.days.isEmpty) {
      return ErrorMessage(
        error is ScreenErrorMessage ? error.text : l10n.unknownError,
      );
    }

    final day = state.today;
    final estimate = day.sleepScore;

    return RefreshIndicator(
      onRefresh: () => ref.read(recoveryDetailProvider.notifier).load(),
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1080),
          child: ListView(
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: [
              _cardPad(
                _SleepScoreSummaryCard(
                  day: day,
                  formatter: formatter,
                ),
              ),
              SectionHeader(l10n.sleepScoreCalculationTitle),
              _cardPad(
                _SleepScoreExplanationCard(
                  expanded: _showCalculation,
                  onToggleExpanded: () =>
                      setState(() => _showCalculation = !_showCalculation),
                ),
              ),
              SectionHeader(l10n.sleepScoreDayNumbersTitle),
              _cardPad(
                _SleepScoreNumbersCard(
                  day: day,
                  estimate: estimate,
                  formatter: formatter,
                ),
              ),
              SectionHeader(l10n.sleepScoreReferencesTitle),
              _cardPad(const _SleepScoreReferencesCard()),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}

Widget _cardPad(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

class _SleepScoreSummaryCard extends StatelessWidget {
  const _SleepScoreSummaryCard({required this.day, required this.formatter});

  final RecoveryDay day;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final estimate = day.sleepScore;

    return _DetailCard(
      children: [
        Row(
          children: [
            const Icon(Icons.dark_mode_outlined, color: AppColors.sleep),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _localizedDayTitle(context, day.date),
                    style: theme.textTheme.labelLarge
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                  Text(
                    l10n.recoverySleepScore,
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  _sleepScoreDisplayValue(l10n, estimate, formatter).value,
                  style: theme.textTheme.headlineLarge
                      ?.copyWith(fontWeight: FontWeight.bold),
                ),
                Text(
                  _sleepScoreConfidenceLabel(l10n, estimate.confidence),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 16),
        Text(
          l10n.sleepScoreNotDiagnostic,
          style: theme.textTheme.bodyMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}

class _SleepScoreExplanationCard extends StatelessWidget {
  const _SleepScoreExplanationCard({
    required this.expanded,
    required this.onToggleExpanded,
  });

  final bool expanded;
  final VoidCallback onToggleExpanded;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final bodyStyle = theme.textTheme.bodyMedium
        ?.copyWith(color: theme.colorScheme.onSurfaceVariant);

    return _DetailCard(
      children: [
        Text(l10n.sleepScoreCalculationSummary, style: bodyStyle),
        if (expanded) ...[
          const SizedBox(height: 16),
          Text(
            l10n.sleepScoreFormula,
            style: theme.textTheme.titleSmall
                ?.copyWith(fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(l10n.sleepScoreFormulaBody, style: bodyStyle),
          const SizedBox(height: 12),
          Text(l10n.sleepScoreComponentsBody, style: bodyStyle),
        ],
        const SizedBox(height: 12),
        Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton(
            onPressed: onToggleExpanded,
            child: Text(
              expanded ? l10n.actionHideCalculation : l10n.actionShowCalculation,
            ),
          ),
        ),
      ],
    );
  }
}

class _SleepScoreNumbersCard extends StatelessWidget {
  const _SleepScoreNumbersCard({
    required this.day,
    required this.estimate,
    required this.formatter,
  });

  final RecoveryDay day;
  final SleepScoreEstimate estimate;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    return _DetailCard(
      children: [
        _DetailMetricGrid(
          items: [
            _DetailMetric(
              l10n.sleepScoreComponentDuration,
              DisplayValue(formatter.decimal(estimate.durationPoints, 1), 'pts'),
            ),
            _DetailMetric(
              l10n.sleepScoreComponentEfficiency,
              DisplayValue(
                  formatter.decimal(estimate.efficiencyPoints, 1), 'pts'),
            ),
            _DetailMetric(
              l10n.sleepScoreComponentContinuity,
              DisplayValue(
                  formatter.decimal(estimate.continuityPoints, 1), 'pts'),
            ),
            _DetailMetric(
              l10n.sleepScoreComponentRegularity,
              DisplayValue(
                  formatter.decimal(estimate.regularityPoints, 1), 'pts'),
            ),
            _DetailMetric(
              l10n.sleepScoreTotalSleep,
              DisplayValue(
                formatter
                    .duration((estimate.sleepDurationMinutes * 60000).round()),
                '',
              ),
            ),
            _DetailMetric(
              l10n.sleepScoreTimeInBed,
              DisplayValue(
                formatter
                    .duration((estimate.timeInBedMinutes * 60000).round()),
                '',
              ),
            ),
            _DetailMetric(
              l10n.sleepScoreEfficiency,
              formatter.percent(estimate.sleepEfficiencyPercent, decimals: 0),
            ),
            _DetailMetric(
              l10n.sleepScoreWaso,
              DisplayValue(
                formatter.count(estimate.wakeAfterSleepOnsetMinutes.round()),
                'min',
              ),
            ),
            _DetailMetric(
              l10n.sleepScoreRegularity,
              estimate.regularityDifferenceMinutes != null
                  ? DisplayValue(
                      formatter.count(
                          estimate.regularityDifferenceMinutes!.round()),
                      'min',
                    )
                  : DisplayValue(l10n.noData, ''),
            ),
            _DetailMetric(
              l10n.sleepScoreBaselineNights,
              DisplayValue(
                  formatter.count(estimate.regularityBaselineNights), ''),
            ),
            _DetailMetric(
              l10n.sleepScoreStageRecords,
              DisplayValue(formatter.count(estimate.sleepStageCount), ''),
            ),
            _DetailMetric(
              l10n.recoverySleepSchedule,
              DisplayValue(_sleepScheduleText(context, day), ''),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          _sleepScoreDataQualityLabel(l10n, estimate),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }
}

class _SleepScoreReferencesCard extends StatelessWidget {
  const _SleepScoreReferencesCard();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return _DetailCard(
      children: [
        _ReferenceItem(
          title: l10n.sleepScoreReferenceAasm,
          url: _aasmSleepDurationUrl,
        ),
        _ReferenceItem(
          title: l10n.sleepScoreReferenceSleepHealth,
          url: _sleepHealthFrameworkUrl,
        ),
        _ReferenceItem(
          title: l10n.sleepScoreReferenceEfficiency,
          url: _sleepEfficiencyUrl,
        ),
        _ReferenceItem(
          title: l10n.sleepScoreReferenceRegularity,
          url: _sleepRegularityUrl,
        ),
      ],
    );
  }
}

/// One reference, rendered as a full-width outlined button that opens the URL
/// in the browser (Kotlin opens it via `LocalUriHandler`; the Flutter app now
/// has url_launcher through [openExternalUrl]).
class _ReferenceItem extends StatelessWidget {
  const _ReferenceItem({required this.title, required this.url});

  final String title;
  final String url;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: SizedBox(
        width: double.infinity,
        child: OutlinedButton.icon(
          onPressed: () => openExternalUrl(context, url),
          icon: const Icon(Icons.open_in_new, size: 18),
          label: Text(title, maxLines: 2, overflow: TextOverflow.ellipsis),
        ),
      ),
    );
  }
}

class _DetailMetric {
  const _DetailMetric(this.title, this.value);

  final String title;
  final DisplayValue value;
}

/// Kotlin `DetailMetricGrid`: two tiles per row.
class _DetailMetricGrid extends StatelessWidget {
  const _DetailMetricGrid({required this.items});

  final List<_DetailMetric> items;

  @override
  Widget build(BuildContext context) {
    final rows = <Widget>[];
    for (var index = 0; index < items.length; index += 2) {
      rows.add(
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: _DetailMetricTile(metric: items[index])),
            const SizedBox(width: 8),
            if (index + 1 < items.length)
              Expanded(child: _DetailMetricTile(metric: items[index + 1]))
            else
              const Expanded(child: SizedBox.shrink()),
          ],
        ),
      );
      if (index + 2 < items.length) rows.add(const SizedBox(height: 8));
    }
    return Column(children: rows);
  }
}

/// Kotlin `SharedMetricTile`: title over a bold value with an optional unit.
class _DetailMetricTile extends StatelessWidget {
  const _DetailMetricTile({required this.metric});

  final _DetailMetric metric;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest
            .withValues(alpha: 0.45),
        borderRadius: const BorderRadius.all(Radius.circular(12)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            metric.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.labelMedium
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
          const SizedBox(height: 6),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Flexible(
                child: Text(
                  metric.value.value,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold),
                ),
              ),
              if (metric.value.unit.trim().isNotEmpty) ...[
                const SizedBox(width: 4),
                Padding(
                  padding: const EdgeInsets.only(bottom: 2),
                  child: Text(
                    metric.value.unit,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

class _DetailCard extends StatelessWidget {
  const _DetailCard({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: children,
        ),
      ),
    );
  }
}

DisplayValue _sleepScoreDisplayValue(
  AppLocalizations l10n,
  SleepScoreEstimate estimate,
  UnitFormatter formatter,
) =>
    estimate.confidence == SleepScoreConfidence.noData
        ? DisplayValue(l10n.noData, '')
        : DisplayValue(formatter.count(estimate.score), '');

String _sleepScoreConfidenceLabel(
  AppLocalizations l10n,
  SleepScoreConfidence confidence,
) {
  switch (confidence) {
    case SleepScoreConfidence.high:
      return l10n.sleepScoreConfidenceHigh;
    case SleepScoreConfidence.medium:
      return l10n.sleepScoreConfidenceMedium;
    case SleepScoreConfidence.low:
      return l10n.sleepScoreConfidenceLow;
    case SleepScoreConfidence.noData:
      return l10n.sleepScoreConfidenceNoData;
  }
}

String _sleepScoreDataQualityLabel(
  AppLocalizations l10n,
  SleepScoreEstimate estimate,
) {
  if (estimate.confidence == SleepScoreConfidence.noData) {
    return l10n.sleepScoreQualityNoData;
  }
  if (estimate.usesSleepStages && estimate.usesExplicitAwakeStages) {
    return l10n.sleepScoreQualityStageAwake;
  }
  if (estimate.usesSleepStages) return l10n.sleepScoreQualityStageOnly;
  return l10n.sleepScoreQualitySessionOnly;
}

/// Kotlin `localizedDayTitle`.
String _localizedDayTitle(BuildContext context, LocalDate date) {
  final l10n = AppLocalizations.of(context);
  final today = LocalDate.now();
  if (date == today) return l10n.periodToday;
  if (date == today.minusDays(1)) return l10n.periodYesterday;
  return DateFormat(
    'EEE, d MMM',
    Localizations.localeOf(context).toString(),
  ).format(DateTime(date.year, date.month, date.day));
}

/// Kotlin `sleepScheduleText`: the main session's start - end short times.
String _sleepScheduleText(BuildContext context, RecoveryDay day) {
  final session = day.mainSleepSession;
  if (session == null) return AppLocalizations.of(context).noData;
  final time = DateFormat.jm(Localizations.localeOf(context).toString());
  return '${time.format(session.startTime.toLocal())} - '
      '${time.format(session.endTime.toLocal())}';
}
