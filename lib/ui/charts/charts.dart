/// Every chart the app draws, and everything a chart needs to know.
///
/// Two tiers, inherited from the Kotlin app and finished here:
///
/// **Complete cards** — one call, one card. Reach for these first.
/// - [MetricDayChart] — one metric across one day (steps, water, weight, heart rate)
/// - [MetricSessionChart] — one metric across one recorded session (pace, cadence)
/// - [MetricBarChart] / [MetricLineChart] / [PeriodHistoryChart] — a metric across
///   a week, month or year
///
/// **Primitives** — for a chart that genuinely does not fit the cards above. The
/// hypnogram and the caffeine curve are the only real examples; be sure yours is
/// another before you drop down here, because everything that ever did ended up
/// re-deriving [DayAxis] and getting it wrong.
/// - [MetricLinePlot], [SparklineChart], [YAxisChart], [drawYAxisGuides]
///
/// **The knowledge** — where a moment sits on an axis, and where the axis starts.
/// - [DayAxis] / [DayAxisLabels] — the x axis is the whole day, always
/// - [SessionAxis] / [SessionAxisLabels] — the x axis is the whole session
/// - [ChartRange] — y bounds, padded
/// - [kChartPlotInset] — how far a plot with a y axis starts from the card's edge;
///   an x-axis row that ignores it describes a chart that is not there
library;

export 'bar_chart.dart';
export 'chart_axis.dart';
export 'day_axis.dart';
export 'heatmap_chart.dart';
export 'line_chart.dart';
export 'metric_day_chart.dart';
export 'metric_line_plot.dart';
export 'metric_session_chart.dart';
export 'period_chart.dart';
export 'session_axis.dart';
export 'sparkline_chart.dart';
