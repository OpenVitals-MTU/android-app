import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/period/period_calculations.dart';
import '../../core/period/period_range_preference_key.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/period_selection_driver.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import 'health_connect_gate.dart';
import 'health_date_picker.dart';
import 'loading_state.dart';
import 'metric_card.dart';
import 'period_navigator.dart';

/// The canonical detail-screen frame, ported from the Kotlin
/// `MetricDetailScaffold`. It owns period selection (via a
/// [PeriodSelectionDriver]) and remembers the selected range per screen (keyed
/// by [rangePreferenceKey] in the [PreferencesRepository]), and lays out —
/// inside a pull-to-refresh [CustomScrollView]/[ListView] capped at 920dp —
/// header items, an optional sync banner, the [TimeRangeSelector], the
/// [PeriodNavigator] (forward-capped at the current period, tap-to-open date
/// picker), an error block, and the metric [content] for the current period.
///
/// Metric-specific visuals stay OUT: [content] and [headerItems] are supplied by
/// each feature screen, which loads its data in response to [onSelectionChanged].
class MetricDetailScaffold extends ConsumerStatefulWidget {
  const MetricDetailScaffold({
    super.key,
    required this.rangePreferenceKey,
    required this.onRefresh,
    required this.content,
    this.isLoading = false,
    this.screenError,
    this.errorText,
    this.headerItems = const <Widget>[],
    this.onSelectionChanged,
    this.weekPeriodMode = WeekPeriodMode.mondayToSunday,
    this.showTimeRangeSelector = true,
    this.syncPaused = false,
    this.showInlineSyncBanner = true,
    this.periodTitleBuilder,
    this.initialDate,
  });

  /// The persisted-range key for this screen (e.g. `PeriodRangePreferenceKey.
  /// heart`). Its stored value seeds the initial range, and range changes are
  /// written back through it.
  final PeriodRangePreferenceKey rangePreferenceKey;

  /// Pull-to-refresh action. Completing the returned future ends the spinner.
  final Future<void> Function() onRefresh;

  /// Builds the metric content items for the given [DatePeriod].
  final List<Widget> Function(DatePeriod period) content;

  final bool isLoading;
  final ScreenError? screenError;
  final String? errorText;

  /// Items rendered above the range selector (e.g. a hero summary).
  final List<Widget> headerItems;

  /// Called with the initial selection and after every selection change, so the
  /// host screen can (re)load its data.
  final void Function(PeriodSelection selection)? onSelectionChanged;

  final WeekPeriodMode weekPeriodMode;
  final bool showTimeRangeSelector;
  final bool syncPaused;
  final bool showInlineSyncBanner;

  /// Optional override for the [PeriodNavigator] title.
  final String Function(TimeRange range, DatePeriod period)? periodTitleBuilder;

  /// Optional initial anchor date (defaults to today).
  final LocalDate? initialDate;

  @override
  ConsumerState<MetricDetailScaffold> createState() =>
      _MetricDetailScaffoldState();
}

class _MetricDetailScaffoldState extends ConsumerState<MetricDetailScaffold> {
  late final PeriodSelectionDriver _driver;
  late PeriodSelection _selection;

  @override
  void initState() {
    super.initState();
    final prefs = ref.read(preferencesRepositoryProvider);
    _driver = PeriodSelectionDriver(
      initialRange: prefs.timeRangeFor(widget.rangePreferenceKey),
      initialDate: widget.initialDate,
      weekPeriodMode: widget.weekPeriodMode,
      onRangeSelected: (range) =>
          prefs.setTimeRangeFor(widget.rangePreferenceKey, range),
    );
    _selection = _driver.selection;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) widget.onSelectionChanged?.call(_selection);
    });
  }

  void _apply(PeriodSelection? next) {
    if (next == null) return; // forward-capped no-op
    setState(() => _selection = next);
    widget.onSelectionChanged?.call(next);
  }

  Future<void> _openCalendar() async {
    final picked = await showHealthDatePicker(
      context,
      selectedDate: _selection.selectedDate,
    );
    if (picked != null) _apply(_driver.selectDate(picked));
  }

  @override
  Widget build(BuildContext context) {
    final today = LocalDate.now();
    final period = displayPeriodFor(
      _selection.selectedRange,
      _selection.selectedDate,
      weekPeriodMode: widget.weekPeriodMode,
    );
    final canGoForward = period.end.isBefore(today);
    final errorMessage = _resolveScreenError(widget.screenError) ?? widget.errorText;

    final items = <Widget>[
      ...widget.headerItems,
      if (widget.showInlineSyncBanner && (widget.syncPaused || widget.isLoading))
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: HealthConnectSyncStatusBanner(
            syncPaused: widget.syncPaused,
            syncInProgress: widget.isLoading && !widget.syncPaused,
          ),
        ),
      if (widget.showTimeRangeSelector)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: TimeRangeSelector(
            selected: _selection.selectedRange,
            onSelect: (range) => _apply(_driver.selectRange(range)),
          ),
        ),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: PeriodNavigator(
          selectedRange: _selection.selectedRange,
          period: period,
          canGoForward: canGoForward,
          title: widget.periodTitleBuilder
              ?.call(_selection.selectedRange, period),
          weekPeriodMode: widget.weekPeriodMode,
          onPreviousPeriod: () => _apply(_driver.previousPeriod()),
          onNextPeriod: () => _apply(_driver.nextPeriod()),
          onOpenCalendar: _openCalendar,
        ),
      ),
      if (errorMessage != null) ErrorMessage(errorMessage),
      ...widget.content(period),
      const SizedBox(height: 16),
    ];

    return RefreshIndicator(
      onRefresh: widget.onRefresh,
      child: Align(
        alignment: Alignment.topCenter,
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 920),
          child: ListView(
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: items,
          ),
        ),
      ),
    );
  }
}

/// Resolves a [ScreenError] into a display string. The Kotlin `resolve()` maps
/// each case to a localized string resource; l10n lands in a later phase, so the
/// port uses literal English fallbacks.
String? _resolveScreenError(ScreenError? error) {
  switch (error) {
    case null:
      return null;
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
