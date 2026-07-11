import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';

/// A list row that deletes on an end-to-start swipe, revealing an
/// error-container background with a delete icon. Port of the Kotlin
/// `SwipeToDeleteEntryRow` (`SwipeToDismissBox` with
/// `enableDismissFromStartToEnd = false`).
///
/// Rows are usually rendered inside a [PaginatedEntryList]; the [Key] must
/// uniquely identify the entry so a delete removes the right row.
class SwipeToDeleteEntryRow extends StatelessWidget {
  const SwipeToDeleteEntryRow({
    required Key key,
    required this.onDelete,
    required this.child,
    this.borderRadius = const BorderRadius.all(Radius.circular(12)),
  }) : super(key: key);

  final VoidCallback onDelete;
  final Widget child;

  /// Matches the Kotlin `CardDefaults.shape` clip on both the row and its
  /// revealed background.
  final BorderRadius borderRadius;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ClipRRect(
      borderRadius: borderRadius,
      child: Dismissible(
        key: ValueKey('swipe-$key'),
        direction: DismissDirection.endToStart,
        onDismissed: (_) => onDelete(),
        background: Container(
          decoration: BoxDecoration(
            color: scheme.errorContainer,
            borderRadius: borderRadius,
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24),
          alignment: AlignmentDirectional.centerEnd,
          child: Icon(
            Icons.delete_outline,
            color: scheme.onErrorContainer,
            semanticLabel: AppLocalizations.of(context).cdDeleteEntry,
          ),
        ),
        child: child,
      ),
    );
  }
}
