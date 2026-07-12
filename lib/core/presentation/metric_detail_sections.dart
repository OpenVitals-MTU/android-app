import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../period/time_range.dart';
import '../time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/preferences/metric_detail_section_id.dart';
import '../../ui/components/widget_edit_controls.dart';
import 'reorder.dart';

/// Port of the Kotlin `MetricDetailSections.kt` + `MetricDetailSectionOrdering.kt`:
/// a metric detail screen's sections are user-reorderable, and the order is
/// shared across every metric screen and persisted.

/// The persisted section order, defaulting to [defaultMetricDetailSectionOrder]
/// and merging in any section the stored list predates.
class MetricDetailSectionOrderNotifier
    extends Notifier<List<MetricDetailSectionId>> {
  @override
  List<MetricDetailSectionId> build() => metricDetailSectionOrderFromStored(
        ref.watch(preferencesRepositoryProvider).metricDetailSectionOrder(),
      );

  void _persist(List<MetricDetailSectionId> order) {
    ref
        .read(preferencesRepositoryProvider)
        .setMetricDetailSectionOrder([for (final id in order) id.storageName]);
    state = order;
  }

  /// Kotlin `moveSectionToTarget`: drop-on-target, sharing the dashboard's rule.
  void moveSectionToTarget(
    MetricDetailSectionId section,
    MetricDetailSectionId target,
  ) {
    final from = state.indexOf(section);
    final to = state.indexOf(target);
    if (from < 0 || to < 0 || from == to) return;
    _persist(reorderOntoDropTarget(state, from, to));
  }

  /// Kotlin `moveSection(section, delta)`: nudge one place, for accessibility.
  void moveSection(MetricDetailSectionId section, int delta) {
    final from = state.indexOf(section);
    if (from < 0) return;
    final to = from + delta;
    if (to < 0 || to >= state.length) return;
    _persist(reorderOntoDropTarget(state, from, to));
  }
}

final metricDetailSectionOrderProvider = NotifierProvider<
    MetricDetailSectionOrderNotifier,
    List<MetricDetailSectionId>>(MetricDetailSectionOrderNotifier.new);

/// Whether the user is currently rearranging sections. Screen-local in Kotlin
/// (a ViewModel flag toggled from the app bar); app-wide here so the toggle can
/// live on whichever screen is showing.
final metricDetailSectionEditProvider =
    NotifierProvider<MetricDetailSectionEditNotifier, bool>(
        MetricDetailSectionEditNotifier.new);

class MetricDetailSectionEditNotifier extends Notifier<bool> {
  @override
  bool build() => false;

  void toggle() => state = !state;
  void stop() => state = false;
}

/// Kotlin `ChartDaySelection`: tapping a bar in the week/month chart pins that
/// day; tapping it again clears it. Day and year charts do not support it.
@immutable
class ChartDaySelection {
  const ChartDaySelection({required this.selectedDate, required this.onDateSelected});

  final LocalDate? selectedDate;
  final ValueChanged<LocalDate> onDateSelected;
}

/// Kotlin `TimeRange.supportsChartDaySelection()`.
bool supportsChartDaySelection(TimeRange range) =>
    range == TimeRange.week || range == TimeRange.month;

/// Kotlin `rememberChartDaySelection`. Resets whenever the range or the period
/// anchor changes, so a pinned day never leaks into a different period.
class ChartDaySelectionScope extends StatefulWidget {
  const ChartDaySelectionScope({
    super.key,
    required this.selectedRange,
    required this.selectedDate,
    required this.builder,
  });

  final TimeRange selectedRange;
  final LocalDate selectedDate;
  final Widget Function(BuildContext context, ChartDaySelection selection) builder;

  @override
  State<ChartDaySelectionScope> createState() => _ChartDaySelectionScopeState();
}

class _ChartDaySelectionScopeState extends State<ChartDaySelectionScope> {
  LocalDate? _chartSelectedDate;

  @override
  void didUpdateWidget(ChartDaySelectionScope oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.selectedRange != widget.selectedRange ||
        oldWidget.selectedDate != widget.selectedDate) {
      _chartSelectedDate = null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final isActiveRange = supportsChartDaySelection(widget.selectedRange);
    return widget.builder(
      context,
      ChartDaySelection(
        selectedDate: isActiveRange ? _chartSelectedDate : null,
        onDateSelected: (date) {
          if (!isActiveRange) return;
          setState(() {
            _chartSelectedDate = _chartSelectedDate == date ? null : date;
          });
        },
      ),
    );
  }
}

/// Kotlin `MetricDetailSectionBuilder.section(id, visible) { ... }`.
@immutable
class MetricDetailSection {
  const MetricDetailSection(this.id, this.child, {this.visible = true});

  final MetricDetailSectionId id;
  final Widget child;
  final bool visible;
}

/// Lays the visible [sections] out in the persisted order. While
/// [metricDetailSectionEditProvider] is on, each section becomes a
/// [ReorderableEditTile] so it can be dragged onto another — the same gesture
/// the dashboard and add-entry grids use.
class OrderedMetricDetailSections extends ConsumerWidget {
  const OrderedMetricDetailSections({super.key, required this.sections});

  final List<MetricDetailSection> sections;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final order = ref.watch(metricDetailSectionOrderProvider);
    final isEditing = ref.watch(metricDetailSectionEditProvider);

    final available = {
      for (final section in sections)
        if (section.visible) section.id: section.child,
    };
    final visibleOrder = [
      for (final id in order)
        if (available.containsKey(id)) id,
    ];
    if (visibleOrder.isEmpty) return const SizedBox.shrink();

    if (!isEditing) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          for (final id in visibleOrder) available[id]!,
        ],
      );
    }

    final notifier = ref.read(metricDetailSectionOrderProvider.notifier);
    return LayoutBuilder(
      builder: (context, constraints) => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const EditModeHint(),
          for (var index = 0; index < visibleOrder.length; index++)
            ReorderableEditTile(
              // The index addresses `visibleOrder`, but the order the notifier
              // rewrites is the full one, so translate through the section id.
              index: index,
              onReorder: (from, to) => notifier.moveSectionToTarget(
                visibleOrder[from],
                visibleOrder[to],
              ),
              feedbackSize: Size(constraints.maxWidth, 120),
              child: _SectionEditFrame(
                onMovePrevious: index > 0
                    ? () => notifier.moveSection(visibleOrder[index], -1)
                    : null,
                onMoveNext: index < visibleOrder.length - 1
                    ? () => notifier.moveSection(visibleOrder[index], 1)
                    : null,
                child: available[visibleOrder[index]]!,
              ),
            ),
        ],
      ),
    );
  }
}

/// A section while editing: outlined, with the accessible move actions Kotlin
/// exposes alongside its drag handle.
class _SectionEditFrame extends StatelessWidget {
  const _SectionEditFrame({
    required this.child,
    required this.onMovePrevious,
    required this.onMoveNext,
  });

  final Widget child;
  final VoidCallback? onMovePrevious;
  final VoidCallback? onMoveNext;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      onIncrease: onMoveNext,
      onDecrease: onMovePrevious,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        decoration: BoxDecoration(
          borderRadius: const BorderRadius.all(Radius.circular(16)),
          border: Border.all(color: scheme.outlineVariant),
        ),
        // Drag, not tap: swallow taps so a card's own onTap cannot fire while
        // the section is being rearranged.
        child: AbsorbPointer(child: child),
      ),
    );
  }
}
