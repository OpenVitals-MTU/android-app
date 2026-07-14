import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../domain/insights/caffeine_drink_profile.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_zoom.dart';
import '../../../ui/charts/charts.dart';
import '../../../ui/charts/time_axis.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/components/section_padding.dart';
import '../application/caffeine_drink_provider.dart';

/// One drink, and what it is doing to you.
///
/// The caffeine screen can tell you there are 240mg in you. It cannot tell you which of
/// the three coffees that is, or which one is still going to be there at bedtime. This
/// can: the same model that builds the day's curve, asked about one drink.
class CaffeineDrinkScreen extends ConsumerWidget {
  const CaffeineDrinkScreen({super.key, required this.entryId});

  final String entryId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final profile = ref.watch(caffeineDrinkProfileProvider(entryId));

    return Scaffold(
      appBar: AppBar(
        title: Text(profile?.entry.name ?? l10n.caffeineDrinkTitle),
      ),
      body: profile == null
          // Deleted while its own screen was open, or followed from something stale.
          ? ErrorMessage(l10n.noData)
          : _body(context, l10n, profile),
    );
  }

  Widget _body(
    BuildContext context,
    AppLocalizations l10n,
    CaffeineDrinkProfile profile,
  ) {
    final locale = Localizations.localeOf(context).toLanguageTag();
    final timeFormat = DateFormat.jm(locale);
    final entry = profile.entry;
    final drankOver = entry.endTime.difference(entry.startTime);

    return ListView(
      padding: screenScrollPadding(context),
      children: [
        sectionPadded(_HeadlineCard(
          profile: profile,
          timeFormat: timeFormat,
          drankOver: drankOver,
        )),
        sectionPadded(_StatsCard(profile: profile, timeFormat: timeFormat)),
        sectionPadded(_CurveCard(profile: profile, timeFormat: timeFormat)),
      ],
    );
  }
}

class _HeadlineCard extends StatelessWidget {
  const _HeadlineCard({
    required this.profile,
    required this.timeFormat,
    required this.drankOver,
  });

  final CaffeineDrinkProfile profile;
  final DateFormat timeFormat;
  final Duration drankOver;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final entry = profile.entry;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _mg(entry.caffeineMg),
              style: theme.textTheme.headlineMedium
                  ?.copyWith(color: theme.colorScheme.primary),
            ),
            Text(
              drankOver.inMinutes > 0
                  ? '${timeFormat.format(entry.startTime.toLocal())} · '
                      '${drankOver.inMinutes} min'
                  : timeFormat.format(entry.startTime.toLocal()),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 10),
            // Without this the screen looks broken: you drank 95mg and the peak says 62.
            Text(
              l10n.caffeineDrinkPeakExplainer,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _CurveCard extends StatelessWidget {
  const _CurveCard({required this.profile, required this.timeFormat});

  final CaffeineDrinkProfile profile;
  final DateFormat timeFormat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final curve = profile.curve;
    if (curve.length < 2) return const SizedBox.shrink();

    final start = curve.first.time;
    final span = curve.last.time.difference(start).inMilliseconds;

    double fractionOf(DateTime time) =>
        span <= 0 ? 0 : time.difference(start).inMilliseconds / span;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.caffeineDrinkCurveTitle,
                style: theme.textTheme.titleSmall),
            const SizedBox(height: 12),
            // Pinch it. The curve runs 36 hours -- long enough to answer "when is this
            // gone", and far too long to read the rise and the peak off, which all happen
            // in the first two. Zoom is what makes one chart do both.
            ChartZoom(
              builder: (context, viewport) => Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  MetricLinePlot(
              points: [
                for (final point in curve)
                  MetricLinePlotPoint(
                    xFraction: fractionOf(point.time),
                    value: point.valueMg,
                  ),
              ],
              minValue: 0,
              viewport: viewport,
              // This drink's own scale, so a small tea fills its chart the same way a
              // double espresso fills its own. The comparison between drinks is what the
              // list is for; this chart is about the SHAPE of one of them.
              maxValue: profile.peakMg <= 0 ? 1 : profile.peakMg,
              accentColor: theme.colorScheme.primary,
              valueFormatter: _mg,
              scrubLabelBuilder: (point) => (
                _mg(point.value),
                timeFormat.format(
                  curve[
                          (point.xFraction * (curve.length - 1))
                              .round()
                              .clamp(0, curve.length - 1)]
                      .time
                      .toLocal(),
                ),
              ),
                  ),
                  const SizedBox(height: 8),
                  // The x axis this chart never had. A drink's curve with no hours under
                  // it can tell you it fades, and not when -- which is the only thing
                  // anyone opened it to find out.
                  TimeAxisLabels(
                    start: curve.first.time,
                    end: curve.last.time,
                    inset: kChartPlotInset,
                    viewport: viewport,
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

/// The four numbers the request asked for: what it peaked at, what is left, and the two
/// moments that decide whether it is a sleep problem.
class _StatsCard extends StatelessWidget {
  const _StatsCard({required this.profile, required this.timeFormat});

  final CaffeineDrinkProfile profile;
  final DateFormat timeFormat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    String moment(DateTime? time) => time == null
        ? l10n.caffeineDrinkStillGoing('${caffeineProfileHorizon.inHours}')
        : timeFormat.format(time.toLocal());

    final rows = <(String, String)>[
      (l10n.caffeineDrinkPeak,
          '${_mg(profile.peakMg)} · ${timeFormat.format(profile.peakTime.toLocal())}'),
      (l10n.caffeineDrinkNow, _mg(profile.currentMg)),
      (l10n.caffeineDrinkHalfGone, moment(profile.halfGoneTime)),
      (l10n.caffeineDrinkGone, moment(profile.goneTime)),
    ];

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            for (final (label, value) in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(label, style: theme.textTheme.bodyMedium),
                    ),
                    Text(
                      value,
                      style: theme.textTheme.bodyMedium
                          ?.copyWith(fontWeight: FontWeight.w600),
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

String _mg(double value) => '${value.round()} mg';
