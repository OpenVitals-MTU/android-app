# Sleep Score And Recovery

> **Status:** Current implemented behavior; caffeine sleep integration remains a proposal.
> **Audience:** Users and contributors.
> **Implementation:** `features/recovery`, `features/sleep`, `domain/insights/SleepScore.kt`.
> **Navigation:** opened from the sleep detail experience; widget `SLEEP`.
> **Related:** [Feature map](feature-map.md), [Sleep tracking](sleep-tracking.md), [Caffeine sleep proposal](../proposals/caffeine-aware-sleep-insights.md).

Sleep score and recovery views explain sleep quality using local, non-diagnostic calculations aligned with Garmin’s three-pillar framing and the sleep-science references that framing cites.

## Sleep Score

OpenVitals computes a 0–100 wellness estimate from Health Connect sleep (and optional overnight HRV):

| Pillar | Weight | Inputs |
| --- | --- | --- |
| Duration | 40 | Total sleep vs NSF age-banded targets (adults 7–9 h; older adults 7–8 h; AASM/SRS ≥7 h adult consensus) |
| Quality | 40 | Efficiency (≥85% good), WASO / continuity, deep + REM stage balance when stages exist |
| Overnight recovery | 20 | Night RMSSD vs personal recent baseline (neutral contribution when HRV is missing) |

Regularity (sleep midpoint vs recent nights) is shown as context only and is not scored.

Confidence rises with staged sleep, explicit awake stages, overnight HRV, and a multi-night timing baseline. Missing inputs stay neutral and lower confidence rather than inventing values. The score is not a Health Connect record and not medical advice.

Primary references:

- [Garmin Sleep Score and Sleep Insights](https://www.garmin.com/en-US/blog/health/garmin-sleep-score-and-sleep-insights/)
- NSF sleep duration recommendations (Hirshkowitz et al., Sleep Health 2015)
- AASM/SRS adult sleep duration consensus (Watson et al., Sleep 2015)
- NSF sleep quality recommendations (Ohayon et al., Sleep Health 2017)
- Habitual short sleep and cardiometabolic risk (Grandner 2014; Liu 2013)

## Sleep Efficiency

Sleep efficiency focuses on time asleep compared with the broader sleep window. It helps explain whether a long sleep session was mostly restful or interrupted. Efficiency also feeds the sleep-score quality pillar.

## Stage Timeline

Sleep detail views can show a time-based stage graph when stage data is available. Supported stage labels include asleep or sleeping, light, deep, REM, awake, awake in bed, and out of bed.

## Caffeine Context

Direct caffeine-aware sleep context is planned, not currently part of the sleep score implementation. Today, caffeine timing and active-caffeine guidance live in the standalone caffeine feature. Missing caffeine data is neutral and does not reduce sleep scores.

## Detail Screens

Recovery detail screens show formula context, pillar and component values, confidence, overnight HRV when available, and science references so users can see why a score or efficiency value changed.

Age for duration targets comes from the body-profile birth year when set; otherwise adults default to the 7–9 h band.
