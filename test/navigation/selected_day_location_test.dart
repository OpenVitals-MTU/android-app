import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/navigation/app_routes.dart';

/// The dashboard is a DAY view. Stepping it back to yesterday and tapping a card is
/// a request for YESTERDAY's data — but every metric detail screen builds its
/// selection from `LocalDate.now()`, so the day the user was looking at used to be
/// dropped on the way through the tap and the detail opened on today.
///
/// It rides along as a query parameter instead.
void main() {
  test('a past day is pinned onto the location', () {
    final yesterday = LocalDate.now().minusDays(1);

    expect(
      AppRoutes.withSelectedDay(AppRoutes.metricLocation('HYDRATION'), yesterday),
      '/metric/HYDRATION?day=$yesterday',
    );
  });

  test('today adds nothing — it is what the screens already do', () {
    // Keeps the ordinary case producing the ordinary location, rather than
    // decorating every tap with a parameter that changes no behaviour.
    const location = '/metric/HYDRATION';

    expect(
      AppRoutes.withSelectedDay(location, LocalDate.now()),
      location,
    );
  });

  test('it joins a location that already carries a query', () {
    final yesterday = LocalDate.now().minusDays(1);

    expect(
      AppRoutes.withSelectedDay('/metric/STEPS?foo=bar', yesterday),
      '/metric/STEPS?foo=bar&day=$yesterday',
    );
  });
}
