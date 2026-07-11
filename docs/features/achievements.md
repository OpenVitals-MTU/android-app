# Achievements

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/achievements/`.
> **Navigation:** `/achievements` (opened from the dashboard top bar).
> **Related:** [Feature map](feature-map.md), [Dashboard](health-connect-metrics-dashboard.md).

Achievements turn long-term activity and wellness patterns into local badge progress.

## What Can Be Tracked

Implemented achievement categories include:

- Daily steps.
- Lifetime distance.
- Daily floors.
- Lifetime floors.

Planned categories include:

- Workouts.
- Hydration.
- Sleep.
- Mindfulness.

## Views

The achievement screen shows unlocked and locked badges, category filters, progress values, tracked days, best daily values, and lifetime totals where available for implemented categories.

## Data Model

Achievements are computed from Health Connect data and OpenVitals summaries. They are not manually edited and do not create Health Connect records.
