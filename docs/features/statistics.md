# Statistics

> **Status:** Current implemented shared behavior across metric detail screens.
> **Audience:** Users and contributors.
> **Implementation:** `lib/core/period/`, `lib/ui/components/metric_detail_scaffold.dart`, `lib/ui/charts/`, `lib/domain/insights/` (period comparison, personal baselines, data confidence), per-feature Riverpod notifiers and presentation files.
> **Navigation:** metric detail screens opened from dashboard tiles (`/metric/:metricId` and the aggregate routes).
> **Related:** [Feature map](feature-map.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Metric detail customization](metric-detail-customization.md).

Statistics help turn raw records into context across time.

## Period Ranges

Most metric detail screens support:

- Day.
- Week.
- Month.
- Year.

Users can move between periods, choose a calendar date, and refresh Health Connect data.

## Common Statistics

Depending on the metric, OpenVitals can show:

- Total and daily average.
- Latest, lowest, highest, and best day.
- Active days or logged days.
- Reading counts and sample counts.
- Previous-period comparison.
- Personal baseline.
- Goal progress and streaks.

## Charts And Entries

Statistics often sit next to charts and selected-day entry rows. Day and week views may use intraday or daily data, while month and year views use longer-range summaries.

## Confidence

OpenVitals shows confidence and coverage notes where missing permissions, missing source data, sparse samples, or partial records can affect interpretation.
