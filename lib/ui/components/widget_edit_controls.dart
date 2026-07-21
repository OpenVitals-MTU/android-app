import 'package:flutter/material.dart';

import '../../l10n/app_localizations.dart';

/// One customizable widget in edit mode: long-press to pick it up, drop it on
/// another to take that one's place.
///
/// Shared by the dashboard summary carousel, the dashboard hero rings, the
/// add-entry hub and the activity-recording dashboard, so a drag feels the same
/// everywhere. Pair the reported `(from, to)` with [reorderOntoDropTarget] —
/// [to] is the *target's own index*, not an insertion gap.
class ReorderableEditTile extends StatelessWidget {
  const ReorderableEditTile({
    super.key,
    required this.index,
    required this.onReorder,
    required this.feedbackSize,
    required this.child,
    this.feedbackBorderRadius = const BorderRadius.all(Radius.circular(20)),
    this.highlightScale = 1.04,
    this.onDragStarted,
    this.onDragEnd,
  });

  /// This tile's position in the list being reordered.
  final int index;

  /// Called with the dragged tile's index and this tile's index.
  final void Function(int from, int to) onReorder;

  /// The size of the card that follows the finger.
  final Size feedbackSize;
  final BorderRadius feedbackBorderRadius;

  /// How much a tile swells when a drag hovers over it.
  final double highlightScale;

  /// Hooks for a host that tracks a drag in flight (e.g. to suspend its own
  /// scroll physics). [onDragEnd] is guaranteed to fire exactly once per drag,
  /// including when this tile was unmounted mid-drag — see [build].
  ///
  /// There is deliberately no `onDragUpdate` hook: Flutter mutes it for an
  /// unmounted draggable (`drag_target.dart`, `if (mounted && ...)`), and a
  /// host that pages or scrolls *will* unmount the tile the finger is holding.
  /// Track the pointer from the host instead (a [Listener] around the viewport
  /// stays mounted for the whole drag).
  final VoidCallback? onDragStarted;
  final VoidCallback? onDragEnd;

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DragTarget<int>(
      onWillAcceptWithDetails: (details) => details.data != index,
      onAcceptWithDetails: (details) => onReorder(details.data, index),
      builder: (context, candidate, rejected) => AnimatedScale(
        scale: candidate.isNotEmpty ? highlightScale : 1.0,
        duration: const Duration(milliseconds: 120),
        child: LongPressDraggable<int>(
          data: index,
          onDragStarted: onDragStarted,
          // `onDragEnd` alone is NOT enough: Flutter only calls it `if (mounted)`,
          // and a host that pages the dragged tile off-screen (the dashboard
          // carousel's edge-scroll) unmounts it before the drop. `onDragCompleted`
          // (accepted) and `onDraggableCanceled` (rejected) carry no such guard,
          // and exactly one of them fires for every drag — so together they always
          // report the end. Without them the host's "a drag is in flight" flag
          // stuck on, and the dashboard carousel stopped scrolling for good.
          onDragEnd: onDragEnd == null ? null : (_) => onDragEnd!(),
          onDragCompleted: onDragEnd,
          onDraggableCanceled:
              onDragEnd == null ? null : (_, _) => onDragEnd!(),
          feedback: SizedBox.fromSize(
            size: feedbackSize,
            child: Material(
              color: Colors.transparent,
              elevation: 8,
              borderRadius: feedbackBorderRadius,
              child: child,
            ),
          ),
          childWhenDragging: Opacity(opacity: 0.25, child: child),
          child: child,
        ),
      ),
    );
  }
}

/// The ✕ overlaid on a customizable widget in edit mode, which removes it from
/// the screen. Port of the Kotlin `onRemove` affordance; shared by the dashboard
/// summary and the add-entry hub.
class RemoveWidgetButton extends StatelessWidget {
  const RemoveWidgetButton({super.key, required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return IconButton(
      visualDensity: VisualDensity.compact,
      iconSize: 18,
      padding: EdgeInsets.zero,
      // A 44 dp hit area (icon stays 18 dp): the old 32x32 was below the Material
      // minimum, and mis-taps on this drag-enabled surface started a drag.
      constraints: const BoxConstraints.tightFor(width: 44, height: 44),
      tooltip: AppLocalizations.of(context).cdRemoveWidget,
      onPressed: onPressed,
      icon: Icon(Icons.close, color: scheme.onSurfaceVariant),
    );
  }
}

/// The edit-mode "Add widgets" tray: every widget the user has removed, as a
/// tap-to-restore row. Port of the Kotlin `DashboardHiddenWidgets` /
/// `hiddenManualEntryWidgets`.
///
/// [titles] must already exclude widgets the device cannot support — those can
/// never be restored, so offering them would be a dead end.
class HiddenWidgetsSection extends StatelessWidget {
  const HiddenWidgetsSection({
    super.key,
    required this.titles,
    required this.onAdd,
    this.padding = const EdgeInsets.fromLTRB(16, 20, 16, 0),
    this.heading,
  });

  final List<String> titles;
  final void Function(String title) onAdd;
  final EdgeInsetsGeometry padding;

  /// Overrides the "Add widgets" heading. The recording dashboard names the
  /// same affordance "Add widget".
  final String? heading;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Padding(
      padding: padding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            heading ?? l10n.dashboardAddWidgets,
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
              letterSpacing: 1,
            ),
          ),
          const SizedBox(height: 8),
          if (titles.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                l10n.dashboardAllWidgetsAdded,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            )
          else
            for (final title in titles)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: OutlinedButton.icon(
                  onPressed: () => onAdd(title),
                  icon: const Icon(Icons.add, size: 18),
                  label: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(title),
                  ),
                ),
              ),
        ],
      ),
    );
  }
}

/// The "Hold to drag & reorder · tap ✕ to remove" hint shown above a
/// customizable grid while editing.
class EditModeHint extends StatelessWidget {
  const EditModeHint({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(Icons.drag_indicator, size: 16, color: scheme.onSurfaceVariant),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            'Hold to drag & reorder · tap ✕ to remove',
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ),
      ],
    );
  }
}
