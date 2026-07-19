# Health Connect Metrics Dashboard

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/dashboard/`, `lib/data/repository/dashboard/dashboard_data_loader.dart`, `lib/data/repository/contract/health_repository.dart`.
> **Navigation:** `/dashboard`; tiles link to `/metric/:metricId` and to the aggregate routes (`/body`, `/heart_vitals`, `/calories`, `/nutrition`, `/activity/cardio_load`, `/daily_readiness/body_energy/:date`). Metric route ids are `DashboardMetricId`; the read set is `DashboardMetric`.
> **Related:** [Feature map](feature-map.md), [Metric detail customization](metric-detail-customization.md), [Permissions](../app/permissions.md).

OpenVitals treats Health Connect as the source of truth. The dashboard reads granted Health Connect records, groups them into scan-friendly widgets, and links each widget to a focused detail screen.

## How to use it

The dashboard (`Summary`) is the app's home screen — a single **selected-day** view. There is no bottom navigation; everything is reached from here.

1. **Pick a day.** Use the day navigator at the top: tap the left/right chevrons (**Previous day** / **Next day**), swipe the date left or right, or tap the date (or the **calendar** button) to jump to any day. **Next day** is disabled once you are on today.
2. **Read the summary.** Two hero **rings** (Steps and Weekly cardio load) sit above a paged grid of **metric tiles**. Swipe the tile grid left/right to move between pages (the dots show your position).
3. **Open a detail screen.** Tap any ring or tile to open its detail, which carries the day you were viewing. Tiles route to the right place automatically — body tiles open the Body overview, heart/vitals tiles open Heart & vitals, calorie tiles open Calories, nutrient tiles open Nutrition, and the cardio ring opens Cardio load.
4. **Log or record.** Tap **Log** to open the add-entry hub, or **Start workout** to begin activity recording.
5. **See today's activities.** The **Activities** section lists workouts recorded for the day; tap one to open its detail, or tap the section header to open the full activity list.
6. **Refresh.** Pull down to re-read Health Connect. If a permission is missing you will see a **"Some permissions are missing"** callout (with a Grant action), or a **"Set up your health data"** card with **Get started** on first run.

To rearrange the dashboard, tap the **pencil (Edit dashboard)** button in the action row — see [Metric detail customization](metric-detail-customization.md).

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
