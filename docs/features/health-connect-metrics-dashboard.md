# Health Connect Metrics Dashboard

OpenVitals treats Health Connect as the source of truth. The dashboard reads granted Health Connect records, groups them into scan-friendly widgets, and links each widget to a focused detail screen.

## What It Shows

- Activity metrics such as steps, distance, calories, active calories, floors, elevation, wheelchair pushes, and workouts.
- Recovery, intake, and body areas such as sleep, heart, HRV, body composition, vitals, beverages, hydration, caffeine, nutrition, mindfulness, and optional cycle data.
- Data source labels, empty states, permission states, and confidence context where they help explain why a number is present or missing.

## Steps

Steps are the clearest Health Connect dashboard example. When the steps read permission is granted, OpenVitals can show the current day summary, longer period charts, goal progress, daily totals, best day, active days, previous-period comparison, and personal baseline context.

The steps widget stays read-only. If the user wants to write data, they use an explicit entry, import, or recording workflow instead of editing dashboard values directly.

## Detail Screens

Most Health Connect-backed metrics use the same period pattern:

- Day, week, month, and year ranges.
- Previous and next period navigation.
- Calendar date picking.
- Pull to refresh.
- Charts, entry rows, statistics, comparisons, and confidence notes.
- Reorderable sections so frequently used cards can stay near the top.

## Privacy Model

The local app does not request app-level internet permission. Health records remain in Health Connect, and OpenVitals reads only the categories the user grants.
