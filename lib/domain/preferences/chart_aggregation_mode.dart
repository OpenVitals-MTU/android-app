/// How the intraday vitals charts summarise their data.
enum ChartAggregationMode {
  off,
  min5,
  min10,
  min30;

  /// Bucket width in minutes, or null when [off] (raw data).
  int? get bucketMinutes => switch (this) {
        ChartAggregationMode.off => null,
        ChartAggregationMode.min5 => 5,
        ChartAggregationMode.min10 => 10,
        ChartAggregationMode.min30 => 30,
      };
}
