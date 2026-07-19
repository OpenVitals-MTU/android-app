# Sleep Score And Recovery

> **Status:** Current implemented behavior; caffeine sleep integration remains a proposal.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/recovery/`, `lib/features/sleep/`, `lib/domain/insights/sleep_score.dart`.
> **Navigation:** `/recovery/sleep_score` and `/recovery/sleep_efficiency` are registered and fully built, but **no in-app control currently opens them** — the score and efficiency values reach the user only as tiles on the Sleep screen (`/sleep`, `/sleep_detail/:sleepId`, `/metric/SLEEP`) and the dashboard Sleep tile subtitle.
> **Related:** [Feature map](feature-map.md), [Sleep tracking](sleep-tracking.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

Sleep score and recovery views explain sleep quality using local, non-diagnostic calculations.

## How to use it

1. **Find your sleep score.** It appears as the **Sleep score** overview tile on the Sleep screen and in the dashboard **Sleep** tile's subtitle. Open Sleep from the dashboard to see it in context (see [Sleep tracking](sleep-tracking.md)).
2. **Find your sleep efficiency.** It appears as the **Sleep efficiency** overview tile on the Sleep screen, alongside time in bed and schedule.
3. **Understand a change.** Both values are recomputed from the night's Health Connect sleep data each time you open the screen or pull to refresh. Duration, efficiency, continuity, and regularity all feed the score; a night with missing stage data or a short baseline lowers confidence rather than inventing a number.
4. **Improve confidence.** Use a sleep source that writes staged sessions into Health Connect, and log sleep consistently — the regularity component and personal baseline both need a few nights of history.

> **Note:** the dedicated `/recovery/sleep_score` and `/recovery/sleep_efficiency` detail pages (with the full formula breakdown and research links) are built but not linked from anywhere in the current app, so today these values are surfaced only as the tiles above. This is a known gap, not intended behavior.

## Sleep Score

The sleep score summarizes recent sleep using available Health Connect sleep data. The score can include context such as:

- Duration.
- Efficiency.
- Continuity.
- Regularity.
- Confidence and missing-data notes.

The score is an OpenVitals wellness estimate, not a Health Connect record and not medical advice.

## Sleep Efficiency

Sleep efficiency focuses on time asleep compared with the broader sleep window. It helps explain whether a long sleep session was mostly restful or interrupted.

## Stage Timeline

Sleep detail views can show a time-based stage graph when stage data is available. Supported stage labels include asleep or sleeping, light, deep, REM, awake, awake in bed, and out of bed.

## Caffeine Context

Direct caffeine-aware sleep context is planned, not currently part of the sleep score implementation. Today, caffeine timing and active-caffeine guidance live in the standalone caffeine feature. Missing caffeine data is neutral and does not reduce sleep scores.

## Detail Screens

Recovery detail screens show formula context, component values, confidence, and references so users can see why a score or efficiency value changed.
