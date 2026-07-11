import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';
import 'metric_card.dart';

/// Kotlin `EntryPageSize`.
const int kEntryPageSize = 10;

/// Port of the Kotlin `PaginatedEntryList`: a titled entry list that starts at
/// [pageSize] rows and grows a page at a time. Renders nothing when empty.
class PaginatedEntryList<T> extends StatefulWidget {
  const PaginatedEntryList({
    super.key,
    required this.title,
    required this.entries,
    required this.rowBuilder,
    this.pageSize = kEntryPageSize,
  });

  final String title;
  final List<T> entries;
  final Widget Function(BuildContext context, T entry) rowBuilder;
  final int pageSize;

  @override
  State<PaginatedEntryList<T>> createState() => _PaginatedEntryListState<T>();
}

class _PaginatedEntryListState<T> extends State<PaginatedEntryList<T>> {
  late int _visibleCount = _initialCount;

  int get _effectivePageSize => widget.pageSize < 1 ? 1 : widget.pageSize;

  int get _initialCount => widget.entries.length < _effectivePageSize
      ? widget.entries.length
      : _effectivePageSize;

  @override
  void didUpdateWidget(PaginatedEntryList<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Kotlin re-keys `visibleCount` on the entry list, so a new period starts
    // collapsed again rather than inheriting the previous "load more" state.
    if (oldWidget.entries != widget.entries ||
        oldWidget.pageSize != widget.pageSize) {
      _visibleCount = _initialCount;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.entries.isEmpty) return const SizedBox.shrink();

    final l10n = AppLocalizations.of(context);
    final bounded = _visibleCount.clamp(0, widget.entries.length);
    final visible = widget.entries.take(bounded);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SectionHeader(widget.title),
        for (final entry in visible)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: widget.rowBuilder(context, entry),
          ),
        if (bounded < widget.entries.length)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: OutlinedButton(
              onPressed: () => setState(() {
                _visibleCount =
                    (bounded + _effectivePageSize).clamp(0, widget.entries.length);
              }),
              child: Text(l10n.actionLoadMoreEntries),
            ),
          ),
      ],
    );
  }
}
