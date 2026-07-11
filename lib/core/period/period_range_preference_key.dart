import 'time_range.dart';

/// The persisted last-selected [TimeRange] per detail/list screen.
enum PeriodRangePreferenceKey {
  steps('detail_range_steps', TimeRange.week),
  calories('detail_range_calories', TimeRange.week),
  activities('detail_range_activities', TimeRange.week),
  sleep('detail_range_sleep', TimeRange.week),
  heart('detail_range_heart', TimeRange.week),
  body('detail_range_body', TimeRange.month),
  hydration('detail_range_hydration', TimeRange.week),
  nutrition('detail_range_nutrition', TimeRange.week),
  mindfulness('detail_range_mindfulness', TimeRange.week),
  cycle('detail_range_cycle', TimeRange.month);

  const PeriodRangePreferenceKey(this.storageKey, this.defaultRange);

  final String storageKey;
  final TimeRange defaultRange;
}
