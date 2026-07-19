# Achievements

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/achievements/`.
> **Navigation:** `/achievements` (opened from the dashboard top bar).
> **Related:** [Feature map](feature-map.md), [Dashboard](health-connect-metrics-dashboard.md).

Achievements turn long-term activity and wellness patterns into local badge progress.

## How to use it

1. **Open Achievements.** Tap the **medal** icon in the dashboard's top bar.
2. **Read your progress.** The summary card at the top shows **"X of Y unlocked"** with a completion bar, the date range covered, and how many days are tracked. A row of stat cards (**Tracked days**, **Best steps**, **Total distance**, **Best floors**, **Total floors**) summarizes your records.
3. **Filter by category.** Use the chips — **All · Daily steps · Lifetime distance · Daily floors · Lifetime floors** — to focus on one kind of badge.
4. **Browse the badges.** Each badge shows its name, the requirement (for example, "Reach 10,000 steps in a single day"), a progress bar, and a status: **Locked**, **Earned**, **Achieved &lt;date&gt;**, or **Earned Nx** for repeatable badges. Unlocked badges are tinted with the category color.
5. **Refresh.** Tap the **Refresh** button on the summary card to recompute from the latest Health Connect data.

Achievements are read-only and unlock automatically — there is nothing to edit or claim. If you see **"No activity history yet"** or **"No floor data"**, start recording steps or use a device that tracks floors, and the badges will fill in over time.

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
