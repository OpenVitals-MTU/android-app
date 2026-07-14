import 'package:flutter/widgets.dart';

import '../time/local_date.dart';

/// The day a metric detail screen should OPEN on, handed down by the router.
///
/// The dashboard is a day view: you can step it back to yesterday. Tapping a card
/// there used to open the detail screen on TODAY regardless, because every detail
/// screen builds its selection from `LocalDate.now()` — so you would go back to
/// yesterday, tap Hydration, and be shown today's water. The day you were looking
/// at is context, and it has to travel with the tap.
///
/// It travels as a query parameter on the pushed location (`?day=2026-07-13`), which
/// the router turns into one of these around the screen. It is NOT read straight off
/// the route inside the scaffold: `GoRouterState.of` throws when there is no route
/// above it, and these screens are pumped bare in dozens of widget tests.
///
/// Absent — a deep link, a test, a screen opened from anywhere but a pinned day —
/// [maybeOf] returns null and the screens go on opening on today, exactly as before.
class MetricDetailInitialDay extends InheritedWidget {
  const MetricDetailInitialDay({
    super.key,
    required this.day,
    required super.child,
  });

  /// Null when the caller had no particular day in mind.
  final LocalDate? day;

  /// The day to open on, or null. Reads the ancestor WITHOUT depending on it: the
  /// value is consumed once, when the screen's period selection is first built, and
  /// a rebuild would be meaningless (the screen owns its selection from then on, and
  /// stomping it on every rebuild would fight the user's own date-stepping).
  static LocalDate? maybeOf(BuildContext context) => context
      .getInheritedWidgetOfExactType<MetricDetailInitialDay>()
      ?.day;

  @override
  bool updateShouldNotify(MetricDetailInitialDay oldWidget) =>
      day != oldWidget.day;
}
