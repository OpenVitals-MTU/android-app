import 'package:flutter/material.dart';

import '../../../core/presentation/measurement_input.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/caffeine_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_bar_row.dart';
import '../../../ui/theme/app_colors.dart';
import 'hydration_catalog.dart';
import 'hydration_drink_dialogs.dart';
import '../application/hydration_entry_view_model.dart';

/// Kotlin `HydrationCatalogRowHeight`.
const double _rowHeight = 76;

/// Today's hydration against the daily goal, with a progress bar. Port of the
/// Kotlin `HydrationTodayCounter`.
///
/// Compose uses a `LinearWavyProgressIndicator`; Flutter has no wavy indicator,
/// so this draws a rounded [ChartBarRow] of the same height.
class HydrationTodayCounter extends StatelessWidget {
  const HydrationTodayCounter({
    super.key,
    required this.liters,
    required this.dailyGoalLiters,
    required this.formatter,
  });

  final double liters;
  final double dailyGoalLiters;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final today = formatter.hydration(liters);
    final goal = formatter.hydration(dailyGoalLiters);
    final progress =
        dailyGoalLiters > 0 ? (liters / dailyGoalLiters).clamp(0.0, 1.0) : 0.0;

    return Container(
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '${today.value} ${today.unit} / ${goal.value} ${goal.unit}',
            style: theme.textTheme.titleMedium,
            textAlign: TextAlign.end,
          ),
          const SizedBox(height: 8),
          TweenAnimationBuilder<double>(
            tween: Tween<double>(begin: 0, end: progress.toDouble()),
            duration: const Duration(milliseconds: 650),
            builder: (context, value, _) => ChartBarRow(
              fraction: value,
              color: AppColors.hydration.withValues(alpha: 0.86),
              // The one bar in the app that sits ON `surfaceContainerHighest` (the
              // counter's own container), so its track has to be darker than the
              // track every other bar uses, or it would be invisible.
              trackColor: scheme.outlineVariant,
              height: 18,
              radius: 9,
            ),
          ),
        ],
      ),
    );
  }
}

/// The "saved nutrients for a non-hydrating drink" banner. Port of the Kotlin
/// `HydrationEntryNoticeCallout`.
class HydrationEntryNoticeCallout extends StatelessWidget {
  const HydrationEntryNoticeCallout({super.key, required this.notice});

  final HydrationEntryNotice notice;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final text = switch (notice) {
      HydrationEntryNotice.nonHydratingDrinkSaved =>
        AppLocalizations.of(context).hydrationNonHydratingDrinkSavedHint,
    };
    return Container(
      decoration: BoxDecoration(
        color: scheme.secondaryContainer,
        borderRadius: BorderRadius.circular(8),
      ),
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          Icon(Icons.info_outline,
              size: 18, color: scheme.onSecondaryContainer),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: scheme.onSecondaryContainer),
            ),
          ),
        ],
      ),
    );
  }
}

/// A drink's volume. Port of the Kotlin `hydrationAmountLabel`: metric volumes
/// below a litre read in millilitres rather than "0.33 L".
String hydrationAmountLabel(double liters, UnitFormatter formatter) {
  if (!formatter.isImperial && liters < 1.0) {
    return '${formatter.count((liters * 1000).round())} ml';
  }
  final value = formatter.hydration(liters);
  return '${value.value} ${value.unit}';
}

/// "330 ml", suffixed with the hydration impact when it is not full. Port of the
/// Kotlin `hydrationSavedDrinkAmountImpactLabel`.
String hydrationDrinkAmountImpactLabel(
  CustomHydrationDrink drink,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  final amount = hydrationAmountLabel(drink.volumeLiters, formatter);
  if (drink.hydrationMultiplier == 0.0) {
    return l10n.hydrationSavedDrinkAmountNoHydration(amount);
  }
  if ((drink.hydrationMultiplier - kFullHydrationImpactMultiplier).abs() >
      0.0001) {
    return l10n.hydrationSavedDrinkAmountPartialHydration(
      amount,
      (drink.hydrationMultiplier * 100).round(),
    );
  }
  return amount;
}

/// The searchable, sectioned drink catalog. Port of the Kotlin
/// `HydrationCatalogDrinkCarousel`.
///
/// A pencil toggles edit mode: rows stop logging on tap and expose edit, move
/// and delete actions instead. Category sections start collapsed (except
/// "Other") and are forced open while a search is active. Frequent rows are
/// ranked, so they cannot be dragged.
class HydrationCatalogCarousel extends StatefulWidget {
  const HydrationCatalogCarousel({
    super.key,
    required this.savedDrinks,
    required this.frequentDrinks,
    required this.formatter,
    required this.canEditSavedDrinks,
    required this.canSelectDrink,
    required this.onSelectDrink,
    required this.onEditDrink,
    required this.onDeleteDrink,
    required this.onMoveDrinkToTarget,
    required this.onMoveDrinkToCategory,
  });

  final List<CustomHydrationDrink> savedDrinks;
  final List<CustomHydrationDrink> frequentDrinks;
  final UnitFormatter formatter;
  final bool canEditSavedDrinks;
  final bool Function(CustomHydrationDrink drink) canSelectDrink;
  final void Function(CustomHydrationDrink drink) onSelectDrink;
  final void Function(CustomHydrationDrink drink) onEditDrink;
  final void Function(CustomHydrationDrink drink) onDeleteDrink;
  final void Function(String drinkId, String targetDrinkId) onMoveDrinkToTarget;
  final void Function(String drinkId, CaffeineSourceCategory? category)
      onMoveDrinkToCategory;

  @override
  State<HydrationCatalogCarousel> createState() =>
      _HydrationCatalogCarouselState();
}

class _HydrationCatalogCarouselState extends State<HydrationCatalogCarousel> {
  final TextEditingController _search = TextEditingController();
  String _query = '';
  bool _editing = false;

  /// Session-local overrides, as the Kotlin composable keeps them: a drag lands
  /// immediately while the repository round-trips underneath.
  Map<String, HydrationCatalogSectionKey> _savedDrinkCategories = {};
  List<String> _unassignedOrder = const [];
  Map<HydrationCatalogSectionKey, List<String>> _sectionOrders = const {};

  /// "Other" starts open; the rest start collapsed.
  final Set<HydrationCatalogSectionKey> _expanded = {
    HydrationCatalogSectionKey.other,
  };

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _moveWithin(
    List<String> rowKeys,
    String rowKey,
    String targetRowKey,
    void Function(List<String> next) commit,
  ) {
    if (rowKey == targetRowKey) return;
    final from = rowKeys.indexOf(rowKey);
    final target = rowKeys.indexOf(targetRowKey);
    if (from < 0 || target < 0) return;
    final next = [...rowKeys];
    next.insert(target.clamp(0, next.length - 1), next.removeAt(from));
    commit(next);

    final movedId = catalogDrinkIdFromRowKey(rowKey);
    final targetId = catalogDrinkIdFromRowKey(targetRowKey);
    if (movedId != null && targetId != null) {
      widget.onMoveDrinkToTarget(movedId, targetId);
    }
  }

  void _moveToSection(String drinkId, HydrationCatalogSectionKey? sectionKey) {
    setState(() {
      final rowKey = savedCatalogRowKey(drinkId);
      _savedDrinkCategories = {
        for (final entry in _savedDrinkCategories.entries)
          if (entry.key != drinkId) entry.key: entry.value,
        drinkId: ?sectionKey,
      };
      _unassignedOrder = [
        for (final key in _unassignedOrder)
          if (key != rowKey) key,
      ];
      _sectionOrders = {
        for (final entry in _sectionOrders.entries)
          entry.key: [
            for (final key in entry.value)
              if (key != rowKey) key,
          ],
      };
    });
    widget.onMoveDrinkToCategory(
      drinkId,
      sectionKey == null ? null : sectionCategory(sectionKey),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final isSearching = _query.trim().isNotEmpty;
    final grouped = hydrationCatalogGroupedDrinks(
      savedDrinks: widget.savedDrinks,
      frequentDrinks: widget.frequentDrinks,
      savedDrinkCategories: _savedDrinkCategories,
      unassignedSavedOrder: _unassignedOrder,
      sectionOrders: _sectionOrders,
      normalizedQuery: _query,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                l10n.hydrationCatalogDrinksTitle,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ),
            if (widget.savedDrinks.isNotEmpty)
              IconButton(
                onPressed: widget.canEditSavedDrinks
                    ? () => setState(() => _editing = !_editing)
                    : null,
                tooltip: _editing
                    ? l10n.cdDoneEditingSavedDrinks
                    : l10n.cdEditSavedDrinks,
                icon: Icon(_editing ? Icons.check : Icons.edit_outlined),
              ),
          ],
        ),
        TextField(
          controller: _search,
          maxLines: 1,
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            labelText: l10n.hydrationCatalogSearch,
          ),
          onChanged: (value) =>
              setState(() => _query = value.trim().toLowerCase()),
        ),
        if (grouped.frequentRows.isNotEmpty) ...[
          const SizedBox(height: 8),
          _RowGroup(
            label: l10n.hydrationCatalogFrequentlyConsumed,
            rows: grouped.frequentRows,
            // Ranked by usage, so dragging them would mean nothing.
            onReorder: null,
            builder: _row,
          ),
        ],
        if (grouped.unassignedSavedRows.isNotEmpty) ...[
          const SizedBox(height: 8),
          _RowGroup(
            label: l10n.hydrationCatalogSavedOutside,
            rows: grouped.unassignedSavedRows,
            onReorder: (rowKey, targetRowKey) => _moveWithin(
              [for (final row in grouped.unassignedSavedRows) row.rowKey],
              rowKey,
              targetRowKey,
              (next) => setState(() => _unassignedOrder = next),
            ),
            builder: _row,
          ),
        ],
        for (final section in grouped.sections)
          if (section.rows.isNotEmpty) ...[
            const SizedBox(height: 8),
            _CatalogSection(
              title: caffeineCategoryLabel(sectionCategory(section.key), l10n),
              rows: section.rows,
              expanded: isSearching || _expanded.contains(section.key),
              onToggle: () => setState(() {
                if (!_expanded.remove(section.key)) _expanded.add(section.key);
              }),
              onReorder: (rowKey, targetRowKey) => _moveWithin(
                [for (final row in section.rows) row.rowKey],
                rowKey,
                targetRowKey,
                (next) => setState(() => _sectionOrders = {
                      ..._sectionOrders,
                      section.key: next,
                    }),
              ),
              builder: _row,
            ),
          ],
      ],
    );
  }

  Widget _row(HydrationCatalogRowItem row) => HydrationCatalogDrinkRow(
        key: ValueKey(row.rowKey),
        drink: row.drink,
        formatter: widget.formatter,
        editing: _editing,
        enabled: widget.canSelectDrink(row.drink),
        onSelect: () => widget.onSelectDrink(row.drink),
        onEdit: () => widget.onEditDrink(row.drink),
        onDelete: () => widget.onDeleteDrink(row.drink),
        onMoveToSection: (key) => _moveToSection(row.drink.id, key),
      );
}

/// A labelled, flat list of rows (frequent / uncategorized).
class _RowGroup extends StatelessWidget {
  const _RowGroup({
    required this.label,
    required this.rows,
    required this.onReorder,
    required this.builder,
  });

  final String label;
  final List<HydrationCatalogRowItem> rows;

  /// Null when the group's order is derived rather than user-defined.
  final void Function(String rowKey, String targetRowKey)? onReorder;
  final Widget Function(HydrationCatalogRowItem row) builder;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        const SizedBox(height: 6),
        for (final row in rows)
          _DraggableRow(row: row, onReorder: onReorder, builder: builder),
      ],
    );
  }
}

/// A bordered, collapsible category. Port of the Kotlin
/// `HydrationCatalogDrinkSection`.
class _CatalogSection extends StatelessWidget {
  const _CatalogSection({
    required this.title,
    required this.rows,
    required this.expanded,
    required this.onToggle,
    required this.onReorder,
    required this.builder,
  });

  final String title;
  final List<HydrationCatalogRowItem> rows;
  final bool expanded;
  final VoidCallback onToggle;
  final void Function(String rowKey, String targetRowKey) onReorder;
  final Widget Function(HydrationCatalogRowItem row) builder;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Container(
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: theme.colorScheme.outlineVariant),
      ),
      padding: const EdgeInsets.all(10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          InkWell(
            onTap: onToggle,
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title, style: theme.textTheme.labelLarge),
                      Text(
                        l10n.hydrationCatalogSectionCount(rows.length),
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: onToggle,
                  tooltip: expanded
                      ? l10n.cdCollapseDrinkCategory(title)
                      : l10n.cdExpandDrinkCategory(title),
                  icon: Icon(expanded ? Icons.expand_less : Icons.expand_more),
                ),
              ],
            ),
          ),
          if (expanded) ...[
            const SizedBox(height: 8),
            for (final row in rows)
              _DraggableRow(row: row, onReorder: onReorder, builder: builder),
          ],
        ],
      ),
    );
  }
}

class _DraggableRow extends StatelessWidget {
  const _DraggableRow({
    required this.row,
    required this.onReorder,
    required this.builder,
  });

  final HydrationCatalogRowItem row;
  final void Function(String rowKey, String targetRowKey)? onReorder;
  final Widget Function(HydrationCatalogRowItem row) builder;

  @override
  Widget build(BuildContext context) {
    final child = Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: builder(row),
    );
    if (onReorder == null) return child;
    return DragTarget<String>(
      onWillAcceptWithDetails: (details) => details.data != row.rowKey,
      onAcceptWithDetails: (details) => onReorder!(details.data, row.rowKey),
      builder: (context, candidate, rejected) => AnimatedScale(
        scale: candidate.isNotEmpty ? 1.01 : 1.0,
        duration: const Duration(milliseconds: 120),
        child: LongPressDraggable<String>(
          data: row.rowKey,
          feedback: Material(
            color: Colors.transparent,
            elevation: 10,
            child: SizedBox(width: 320, child: builder(row)),
          ),
          childWhenDragging: Opacity(opacity: 0.25, child: child),
          child: child,
        ),
      ),
    );
  }
}

/// One drink. Tapping it logs it; in edit mode it exposes edit / move / delete
/// instead. Port of the Kotlin `HydrationCatalogDrinkRow`.
class HydrationCatalogDrinkRow extends StatelessWidget {
  const HydrationCatalogDrinkRow({
    super.key,
    required this.drink,
    required this.formatter,
    required this.editing,
    required this.enabled,
    required this.onSelect,
    required this.onEdit,
    required this.onDelete,
    required this.onMoveToSection,
  });

  final CustomHydrationDrink drink;
  final UnitFormatter formatter;
  final bool editing;

  /// False when the drink writes something the user has not granted.
  final bool enabled;
  final VoidCallback onSelect;
  final VoidCallback onEdit;
  final VoidCallback onDelete;
  final void Function(HydrationCatalogSectionKey? sectionKey) onMoveToSection;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final nutrients = drink.nutrientValues.length;

    return Opacity(
      opacity: (enabled || editing) ? 1.0 : 0.48,
      child: SizedBox(
        height: _rowHeight,
        child: Material(
          color: theme.colorScheme.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: BorderSide(color: theme.colorScheme.outlineVariant),
          ),
          child: InkWell(
            // Tapping a row in edit mode must not log it.
            onTap: (enabled && !editing) ? onSelect : null,
            borderRadius: BorderRadius.circular(8),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Row(
                children: [
                  const Icon(Icons.local_drink_outlined,
                      size: 20, color: AppColors.hydration),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          drink.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelLarge,
                        ),
                        Text(
                          hydrationDrinkAmountImpactLabel(
                              drink, formatter, l10n),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelMedium,
                        ),
                        Text(
                          nutrients == 0
                              ? l10n.hydrationCustomDrinkLiquidOnly
                              : l10n.hydrationCustomDrinkNutrientCount(nutrients),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.labelSmall,
                        ),
                      ],
                    ),
                  ),
                  if (editing) ...[
                    IconButton(
                      onPressed: onEdit,
                      tooltip: l10n.cdEditDrink,
                      icon: const Icon(Icons.edit_outlined, size: 20),
                    ),
                    _MoveMenuButton(onMoveToSection: onMoveToSection),
                    IconButton(
                      onPressed: onDelete,
                      tooltip: l10n.cdDeleteDrink,
                      icon: const Icon(Icons.delete_outline, size: 20),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MoveMenuButton extends StatelessWidget {
  const _MoveMenuButton({required this.onMoveToSection});

  final void Function(HydrationCatalogSectionKey? sectionKey) onMoveToSection;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return PopupMenuButton<_SectionChoice>(
      tooltip: l10n.cdMoveDrinkCategory,
      icon: const Icon(Icons.more_vert, size: 20),
      onSelected: (choice) => onMoveToSection(choice.value),
      itemBuilder: (context) => [
        PopupMenuItem(
          value: const _SectionChoice(null),
          child: Text(l10n.hydrationCatalogSavedOutside),
        ),
        for (final key in HydrationCatalogSectionKey.values)
          PopupMenuItem(
            value: _SectionChoice(key),
            child: Text(caffeineCategoryLabel(sectionCategory(key), l10n)),
          ),
      ],
    );
  }
}

/// Wraps the nullable section so "uncategorized" is a real choice.
class _SectionChoice {
  const _SectionChoice(this.value);
  final HydrationCatalogSectionKey? value;

  @override
  bool operator ==(Object other) =>
      other is _SectionChoice && other.value == value;

  @override
  int get hashCode => value.hashCode;
}
