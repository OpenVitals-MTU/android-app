import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/presentation/screen_error.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import 'sleep_cards.dart';
import '../application/sleep_detail_view_model.dart';
import 'sleep_stage_chart.dart';
import '../../../ui/components/section_padding.dart';

/// Single sleep-session detail pushed over the shell (`/sleep_detail/:sleepId`).
/// Port of the Kotlin `SleepDetailScreen` + the detail cards in `SleepCards.kt`:
/// a plain list of summary card, stage-breakdown card, session-details card and
/// per-stage event rows.
class SleepDetailScreen extends ConsumerStatefulWidget {
  const SleepDetailScreen({super.key, required this.sleepId});

  final String sleepId;

  @override
  ConsumerState<SleepDetailScreen> createState() => _SleepDetailScreenState();
}

class _SleepDetailScreenState extends ConsumerState<SleepDetailScreen> {
  late final NotifierProvider<SleepDetailViewModel, SleepDetailState> _provider =
      NotifierProvider.autoDispose<SleepDetailViewModel, SleepDetailState>(
    () => SleepDetailViewModel(widget.sleepId),
  );

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(_provider);
    final formatter = ref.watch(unitFormatterProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.screenSleepDetail)),
      body: _body(context, l10n, state, formatter),
    );
  }

  Widget _body(
    BuildContext context,
    AppLocalizations l10n,
    SleepDetailState state,
    UnitFormatter formatter,
  ) {
    if (state.isLoading) return const FullScreenLoading();
    final error = state.error;
    if (error != null) return ErrorMessage(_errorText(l10n, error));
    final session = state.session;
    if (session == null) return ErrorMessage(l10n.unknownError);

    final stages = [...session.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));

    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: SleepSummaryCard(session: session, formatter: formatter),
        ),
        sectionPadded(SleepStageBreakdownCard(session: session, formatter: formatter)),
        sectionPadded(SleepSessionDetailsCard(session: session)),
        if (stages.isNotEmpty) ...[
          sectionPadded(
            SleepSectionCard(
              title: l10n.detailStageEvents,
              child: Text(
                l10n.summaryRecordedStages(formatter.count(stages.length)),
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
              ),
            ),
          ),
          for (final stage in stages)
            sectionPadded(SleepStageEventRow(stage: stage, formatter: formatter)),
        ],
        const SizedBox(height: 16),
      ],
    );
  }
}


String _errorText(AppLocalizations l10n, ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => l10n.unknownError,
      ScreenErrorMissingArgument() => l10n.unknownError,
      ScreenErrorPermissionDenied() => l10n.unknownError,
      ScreenErrorHealthConnectUnavailable() => l10n.unknownError,
    };


// ── Summary card ─────────────────────────────────────────────────────────────

/// Port of the Kotlin `SleepSummaryCard`: bed icon + title, end date, source
/// chip, then the duration headline and the full start-end range.
class SleepSummaryCard extends StatelessWidget {
  const SleepSummaryCard({
    super.key,
    required this.session,
    required this.formatter,
  });

  final SleepData session;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();
    final title = session.title?.trim().isNotEmpty == true
        ? session.title!
        : l10n.detailSleepSession;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.bed_outlined, color: AppColors.sleep),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              title,
                              style: theme.textTheme.titleLarge
                                  ?.copyWith(fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ),
                      Text(
                        DateFormat.yMMMd(locale)
                            .format(session.endTime.toLocal()),
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                SourceChip(source: session.source),
              ],
            ),
            const SizedBox(height: 16),
            Text(
              formatter.duration(session.durationMs),
              style: theme.textTheme.headlineMedium
                  ?.copyWith(color: AppColors.sleep),
            ),
            Text(
              '${_mediumDateTime(locale, session.startTime)} - '
              '${_mediumDateTime(locale, session.endTime)}',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Stage breakdown card ─────────────────────────────────────────────────────

/// Port of the Kotlin `SleepStageBreakdownCard`: the per-lane stage timeline
/// chart plus the per-stage totals (duration · share).
class SleepStageBreakdownCard extends StatelessWidget {
  const SleepStageBreakdownCard({
    super.key,
    required this.session,
    required this.formatter,
  });

  final SleepData session;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    if (session.stages.isEmpty) {
      return SleepSectionCard(
        title: l10n.detailStages,
        child: Text(
          l10n.messageNoStages,
          style: theme.textTheme.bodyMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      );
    }

    final orderedStages = [...session.stages]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    final stageTotalMs = orderedStages.fold<int>(
      0,
      (sum, stage) => sum + (stage.durationMs > 0 ? stage.durationMs : 0),
    );
    final totals = _stageTotals(orderedStages);

    return SleepSectionCard(
      title: l10n.detailStages,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SleepStagesLaneChart(
            stages: orderedStages,
            formatter: formatter,
            timelineStart: session.startTime,
            timelineEnd: session.endTime,
          ),
          const SizedBox(height: 12),
          for (final total in totals)
            _DetailRow(
              label: localizedSleepStageLabel(l10n, total.$1),
              value: '${formatter.duration(total.$2)} · '
                  '${formatter.decimal(stageTotalMs > 0 ? total.$2 * 100.0 / stageTotalMs : 0.0, 0)}%',
            ),
        ],
      ),
    );
  }
}

List<(int, int)> _stageTotals(List<SleepStage> stages) {
  final totals = <int, int>{};
  for (final stage in stages) {
    totals[stage.stageType] = (totals[stage.stageType] ?? 0) + stage.durationMs;
  }
  final entries = totals.entries.map((e) => (e.key, e.value)).toList()
    ..sort((a, b) => b.$2.compareTo(a.$2));
  return entries;
}

/// metadata as label/value rows.
class SleepSessionDetailsCard extends StatelessWidget {
  const SleepSessionDetailsCard({super.key, required this.session});

  final SleepData session;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();
    final device = session.device;
    final notAvailable = l10n.notAvailable;

    return SleepSectionCard(
      title: l10n.detailSessionDetails,
      child: Column(
        children: [
          _DetailRow(
            label: l10n.detailStarted,
            value: _mediumDateTime(locale, session.startTime),
          ),
          _DetailRow(
            label: l10n.detailEnded,
            value: _mediumDateTime(locale, session.endTime),
          ),
          _DetailRow(
            label: l10n.detailStartZone,
            value: _zoneOffsetId(session.startZoneOffset) ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailEndZone,
            value: _zoneOffsetId(session.endZoneOffset) ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailRecording,
            value: _recordingMethodLabel(l10n, session.recordingMethod),
          ),
          _DetailRow(label: l10n.detailSourcePackage, value: session.source),
          _DetailRow(
            label: l10n.detailDeviceType,
            value: _deviceTypeLabel(l10n, device?.type),
          ),
          _DetailRow(
            label: l10n.detailDeviceMaker,
            value: device?.manufacturer ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailDeviceModel,
            value: device?.model ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailLastModified,
            value: session.lastModifiedTime != null
                ? _mediumDateTime(locale, session.lastModifiedTime!)
                : notAvailable,
          ),
          _DetailRow(label: l10n.detailRecordId, value: session.id),
          _DetailRow(
            label: l10n.detailClientRecordId,
            value: session.clientRecordId ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailClientVersion,
            value: session.clientRecordVersion?.toString() ?? notAvailable,
          ),
          _DetailRow(
            label: l10n.detailTitle,
            value: session.title?.trim().isNotEmpty == true
                ? session.title!
                : notAvailable,
          ),
          _DetailRow(
            label: l10n.detailNotes,
            value: session.notes?.trim().isNotEmpty == true
                ? session.notes!
                : notAvailable,
          ),
        ],
      ),
    );
  }
}

// ── Stage event row ──────────────────────────────────────────────────────────

/// Port of the Kotlin `SleepStageEventRow`: one recorded stage with its time
/// range and duration.
class SleepStageEventRow extends StatelessWidget {
  const SleepStageEventRow({
    super.key,
    required this.stage,
    required this.formatter,
  });

  final SleepStage stage;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toString();

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              localizedSleepStageLabel(l10n, stage.stageType),
              style: theme.textTheme.titleSmall,
            ),
            const SizedBox(height: 6),
            _DetailRow(
              label: l10n.detailTime,
              value: _timeRange(locale, stage.startTime, stage.endTime),
            ),
            _DetailRow(
              label: l10n.detailDuration,
              value: formatter.duration(stage.durationMs),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Shared bits ──────────────────────────────────────────────────────────────

/// A label/value line, port of the Kotlin `DetailRow`.
class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: theme.textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}

String _mediumDateTime(String locale, DateTime value) =>
    DateFormat.yMMMd(locale).add_jm().format(value.toLocal());

/// Kotlin `formatTimeRange`: times only inside one day, full date-times across
/// midnight.
String _timeRange(String locale, DateTime start, DateTime end) {
  final localStart = start.toLocal();
  final localEnd = end.toLocal();
  final sameDay = localStart.year == localEnd.year &&
      localStart.month == localEnd.month &&
      localStart.day == localEnd.day;
  if (sameDay) {
    final time = DateFormat.jm(locale);
    return '${time.format(localStart)} - ${time.format(localEnd)}';
  }
  return '${_mediumDateTime(locale, start)} - ${_mediumDateTime(locale, end)}';
}

/// The `ZoneOffset.id`-style rendering of a stored zone offset (`Z`, `+02:00`).
String? _zoneOffsetId(Duration? offset) {
  if (offset == null) return null;
  if (offset == Duration.zero) return 'Z';
  final sign = offset.isNegative ? '-' : '+';
  final absolute = offset.abs();
  final hours = absolute.inHours.toString().padLeft(2, '0');
  final minutes = (absolute.inMinutes % 60).toString().padLeft(2, '0');
  return '$sign$hours:$minutes';
}

// Health Connect `Metadata.RECORDING_METHOD_*` constants.
const int _recordingMethodUnknown = 0;
const int _recordingMethodActivelyRecorded = 1;
const int _recordingMethodAutomaticallyRecorded = 2;
const int _recordingMethodManualEntry = 3;

String _recordingMethodLabel(AppLocalizations l10n, int? method) {
  switch (method) {
    case _recordingMethodActivelyRecorded:
      return l10n.recordingActivelyRecorded;
    case _recordingMethodAutomaticallyRecorded:
      return l10n.recordingAutomaticallyRecorded;
    case _recordingMethodManualEntry:
      return l10n.recordingManualEntry;
    case _recordingMethodUnknown:
      return l10n.recordingUnknown;
    default:
      return l10n.notAvailable;
  }
}

// Health Connect `Device.TYPE_*` constants.
const int _deviceTypeUnknown = 0;
const int _deviceTypeWatch = 1;
const int _deviceTypePhone = 2;
const int _deviceTypeScale = 3;
const int _deviceTypeRing = 4;
const int _deviceTypeHeadMounted = 5;
const int _deviceTypeFitnessBand = 6;
const int _deviceTypeChestStrap = 7;
const int _deviceTypeSmartDisplay = 8;

String _deviceTypeLabel(AppLocalizations l10n, int? type) {
  switch (type) {
    case _deviceTypeWatch:
      return l10n.deviceWatch;
    case _deviceTypePhone:
      return l10n.devicePhone;
    case _deviceTypeScale:
      return l10n.deviceScale;
    case _deviceTypeRing:
      return l10n.deviceRing;
    case _deviceTypeHeadMounted:
      return l10n.deviceHeadMounted;
    case _deviceTypeFitnessBand:
      return l10n.deviceFitnessBand;
    case _deviceTypeChestStrap:
      return l10n.deviceChestStrap;
    case _deviceTypeSmartDisplay:
      return l10n.deviceSmartDisplay;
    case _deviceTypeUnknown:
      return l10n.recordingUnknown;
    default:
      return l10n.notAvailable;
  }
}
