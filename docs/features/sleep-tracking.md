# Sleep Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/sleep/`, `lib/data/repository/contract/sleep_repository.dart` (+ `impl/sleep_repository_impl.dart`), `lib/domain/usecase/load_sleep_period_use_case.dart`.
> **Navigation:** `/sleep`, `/sleep_detail/:sleepId`, `/metric/SLEEP`.
> **Related:** [Feature map](feature-map.md), [Sleep score and recovery](sleep-score-and-recovery.md), [Statistics](statistics.md).

The sleep feature owns the main period-based sleep detail screen and individual sleep-session detail route.

## How to use it

1. **Open Sleep.** Tap the **Sleep** tile on the dashboard (its subtitle shows your sleep score and rating). The Sleep screen opens.
2. **Pick a range and night.** Use the **Day / Week / Month / Year** selector, then the chevrons or calendar to move between nights and periods (see [Statistics](statistics.md) for the shared controls).
3. **Read a night (Day view).** You'll see the **stage timeline** (hypnogram), a stage-share card, and a **Naps** section if you had daytime sleep. The overview tiles — **Sleep score**, **Time in bed**, **Sleep schedule**, **Sleep efficiency** — summarize the night at a glance.
4. **Open a session.** Tap the timeline card or any row in the **Sleep sessions** list to open the session detail, which shows the full stage timeline, per-stage durations and shares, the recording source and device, and start/end times.
5. **Compare across time (Week/Month/Year).** The schedule or duration chart shows the period; tap a day to pin it and reveal that night's sessions.
6. **Set your goal.** Use the **− / +** on the daily-goal card to change your target sleep hours.
7. **Refresh.** Pull down to re-sync from Health Connect.

Sleep data is read from whatever app or device writes it into Health Connect — OpenVitals doesn't record sleep itself. If you see **"No sleep recorded"**, another app or your wearable needs to be writing sleep sessions. When a night was only partly staged, the detail shows the note **"This night was only partly staged, so the sleep graph isn't shown."**

## What It Shows

Sleep can show:

- Sleep sessions for the selected period.
- Day summaries and period summaries.
- Sleep stage timelines where Health Connect stage data is available.
- Duration, efficiency, score, continuity, goal progress, and recovery context.
- Session lists and session details.
- Previous-period comparisons, baselines, confidence, and source labels.

## Detail Pattern

Sleep follows the canonical period-detail model:

- Day, week, month, and year ranges.
- Selected anchor date.
- Previous/next navigation and calendar selection.
- Pull to refresh.
- Day-specific content when a daily summary is available.
- Week/month/year content when sessions exist in the selected period.
- Reorderable sections.

The sleep session detail route is separate from the period overview and focuses on one session's timing, stages, and source context.

## Related Features

- [`sleep-score-and-recovery.md`](sleep-score-and-recovery.md): sleep score, sleep efficiency, recovery details, and confidence.
- [`caffeine-aware-sleep-insights.md`](../proposals/caffeine-aware-sleep-insights.md): current standalone caffeine feature and planned direct sleep integration.
