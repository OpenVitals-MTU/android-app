# Sleep Tracking

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/sleep/`, `lib/data/repository/contract/sleep_repository.dart` (+ `impl/sleep_repository_impl.dart`), `lib/domain/usecase/load_sleep_period_use_case.dart`.
> **Navigation:** `/sleep`, `/sleep_detail/:sleepId`, `/metric/SLEEP`.
> **Related:** [Feature map](feature-map.md), [Sleep score and recovery](sleep-score-and-recovery.md), [Statistics](statistics.md).

The sleep feature owns the main period-based sleep detail screen and individual sleep-session detail route.

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
