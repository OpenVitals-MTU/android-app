# Sleep Score And Recovery

> **Status:** Current implemented behavior; caffeine sleep integration remains a proposal.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/recovery/`, `lib/features/sleep/`, `lib/domain/insights/sleep_score.dart`.
> **Navigation:** `/recovery/sleep_score` and `/recovery/sleep_efficiency`, opened from the sleep detail experience (`/sleep`, `/sleep_detail/:sleepId`, `/metric/SLEEP`).
> **Related:** [Feature map](feature-map.md), [Sleep tracking](sleep-tracking.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

Sleep score and recovery views explain sleep quality using local, non-diagnostic calculations.

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
