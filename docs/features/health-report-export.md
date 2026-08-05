# Health Report Export

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/reports`, `data/repository/report`, `domain/report`.
> **Navigation:** `Screen.SettingsDataImport`, then `Screen.SettingsReportExport`.
> **Related:** [Feature map](feature-map.md), [CSV import](csv-import.md), [Health Connect metrics dashboard](health-connect-metrics-dashboard.md).

Health report export builds a PDF of your health data straight from Health Connect: pick the metrics, a detail level, and how far back to go, and OpenVitals produces a document you can hand to a doctor or keep for your records. The report is generated entirely on the device with the platform's own PDF engine — nothing leaves the phone unless you share it.

## Building a Report

The builder lives in Settings, Import & export, Health report.

1. **Pick metrics.** All exportable metrics the installed Health Connect provider supports are listed, grouped by section (Activity, Sleep, Nutrition, Body, Heart, Vitals, Mindfulness). Select all, clear, or tick individual metrics.
2. **Pick detail and range.** Detail is daily, weekly, or monthly buckets. The range is a preset lookback (30, 90, 180, or 365 days ending today) or a custom start and end date. Custom ranges are capped at two years so reads stay fast.
3. **Build.** Progress shows the metric currently being read, and the build can be cancelled. Metrics are read one group at a time; a metric that fails or times out costs its own section, never the report.
4. **Share or save.** The finished PDF can be shared to any app or saved through the system file picker. Changing anything in the configuration invalidates the finished file, so the PDF on disk always matches the selection on screen.

## What the PDF Contains

For each metric with data:

- A chart — bars for cumulative metrics (steps, calories, hydration), a trend line with a min–max band for averaged metrics (weight, heart rate, vitals). Blood pressure draws systolic and diastolic as two lines.
- A stats strip: average, min, max, total (for cumulative metrics), and days with data — always computed from daily values, so the bucketing never changes what the numbers mean. Body metrics add the change over the range.
- A table with one row per bucket. Long tables flow across pages with their header repeated.

Some metrics carry more structure than a daily number, and their sections show it:

- **Blood pressure**: separate systolic and diastolic stats, morning / after-lunch / before-sleep averages, and every reading listed. Exact duplicate records (the same reading written twice to Health Connect) are collapsed.
- **Blood glucose**: averages by meal context — fasting first — plus every reading with its context. Ranges with continuous-monitor volumes of data fall back to the daily chart.
- **Workouts**: totals per activity type and a session list (date, activity, duration, distance) instead of an anonymous bucket table.
- **Sleep**: average bedtime and wake-up time (averaged correctly across midnight), the deep/REM/light/awake stage mix over nights with reliable stage data, and one row per night. Naps under three hours stay out of the clinical numbers.
- **Body temperature**: every individual reading, listed — a fever diary, not just a trend.

The first page carries the OpenVitals masthead, when the report was generated, and the exact range and detail level. Every page is numbered.

## Honest About What's Missing

The report says so, in print, when something could not be included:

- Metrics whose read permission is not granted are listed in a notice and marked in their section. The builder offers to request exactly the missing permissions, but a partial grant still builds.
- Without the Health Connect history permission, providers only serve the last 30 days; the report clamps its range and says so.
- A cancelled build marks unread sections as skipped rather than pretending they were empty.
- A metric whose read failed or timed out gets a "could not be read" line instead of silently vanishing.

## Metrics Not Offered

BMI and FFMI are display-time derivations with no stored history. Weekly cardio load and intensity minutes are estimates without an exportable daily series. Cycle tracking data is a set of event logs rather than a numeric series and is not part of the numeric report.

## Units and Language

Values render with the user's unit settings (including per-quantity overrides) and locale number formats. Dates follow the device locale.
