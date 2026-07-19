# Statistics

> **Status:** Current implemented shared behavior across metric detail screens.
> **Audience:** Users and contributors.
> **Implementation:** `lib/core/period/`, `lib/ui/components/metric_detail_scaffold.dart`, `lib/ui/charts/`, `lib/domain/insights/` (period comparison, personal baselines, data confidence), per-feature Riverpod notifiers and presentation files.
> **Navigation:** metric detail screens opened from dashboard tiles (`/metric/:metricId` and the aggregate routes).
> **Related:** [Feature map](feature-map.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md), [Metric detail customization](metric-detail-customization.md).

Statistics help turn raw records into context across time.

## How to use it

Every metric-detail screen is built on the same frame, so these controls work the same everywhere. This is the canonical reference other feature guides point back to.

1. **Choose a range.** Use the **Day / Week / Month / Year** selector near the top. Your choice is remembered per screen.
2. **Move through time.** The period bar below shows the current day or range. Tap the back/forward chevrons to step one period at a time (**forward is capped at the current period** — you can't go into the future), or tap the title to open a **calendar** and jump to any date. On most screens you can also swipe the period bar left/right.
3. **Read a specific day inside a range.** In **Week** and **Month** views, **tap a bar or point on the chart** to pin that day — a "selected day entries" section appears with that day's readings. Tap again to clear. (Day and Year charts don't pin.)
4. **Refresh.** Pull down to re-sync from Health Connect. A banner appears inline while a refresh runs or when sync is paused.
5. **Reorder the sections.** Tap the **Edit sections** icon in the app bar to drag the cards into the order you prefer; the order is shared across all metric screens.

### What the numbers mean

Depending on the metric you may see: **Total**, **Daily average**, **Best day**, **Active days** / **Logged days**, a **previous-period comparison** (how this period compares with the one before), and a **personal baseline** (your own recent normal). A **data-confidence** note appears on multi-day views when missing permissions, sparse samples, or partial records could affect interpretation.

### If a screen is empty

- **"No readings in this period."** — there's no data for the range; try a wider range or a different day.
- **A Grant button instead of the screen** — the required Health Connect read permission isn't granted, or sync is paused. Tap **Grant**, or turn sync back on under **Settings › Health Connect**.
- **"Building history for this range… check back shortly."** — long-range history is still syncing from Health Connect; wait and refresh.

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
