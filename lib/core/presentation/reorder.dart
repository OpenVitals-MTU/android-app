/// Moves the entry at [from] onto the drop target at [to], returning the new
/// order. Both indices address the *same* pre-move list, as the edit-mode
/// `DragTarget`s report them: [to] is the target entry's own index, not a
/// ReorderableListView-style insertion gap. Removing [from] first shifts
/// everything after it down one, so a plain insert at [to] lands the moved entry
/// on the target for both forward and backward drags — no index adjustment.
///
/// Port of the Kotlin `moveWidgetToTarget`.
List<T> reorderOntoDropTarget<T>(List<T> items, int from, int to) {
  if (from == to ||
      from < 0 ||
      to < 0 ||
      from >= items.length ||
      to >= items.length) {
    return List<T>.of(items);
  }
  final next = List<T>.of(items);
  next.insert(to, next.removeAt(from));
  return next;
}
