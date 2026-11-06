# Body Energy

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/bodyenergy`, `features/readiness`, `domain/insights/BodyEnergyTimeline.kt`, `domain/insights/BodyEnergyCalibrationFit.kt`.
> **Navigation:** `Screen.BodyEnergyDetails`, widget `BODY_ENERGY`, settings section `RECOVERY`.
> **Related:** [Feature map](feature-map.md), [Daily readiness](daily-readiness.md), [Sleep score and recovery](sleep-score-and-recovery.md), [Home screen widgets](home-widgets.md).

Body Energy is a local derived view that estimates available energy across the day from supported wellness signals. It is intentionally a selected-day experience rather than a canonical `Day / Week / Month / Year` metric-detail screen.

## What It Shows

Body Energy can appear in:

- Daily Readiness.
- The dashboard.
- A dedicated Body Energy detail flow.
- Android home screen widgets.

The detail view can show a summary, timeline, confidence, and context for the selected day.

## Calibration

Body Energy supports calibration during onboarding and in Recovery settings. Calibration helps the local estimate better fit the user, and can be reset when needed.

## How The Battery Moves

The score is a running balance, not a daily reset: a day opens where the
previous one closed. Two things push it around.

**Charge** comes from sleep, and — far more slowly — from genuinely quiet waking
time. Sleep charges per minute slept, scaled by three things:

- **How well the night went.** See below; this is usually the biggest of the
  three.
- **Overnight HRV**, against the personal baseline.
- **Breathing rate**, which penalises a night spent above the usual.

**Drain** comes from basal metabolism (a gentle downward slope whenever nothing
else is happening, scaled by measured BMR), from active energy expenditure, from
heart-rate-derived stress, and from recovery debt after a hard session.

Each component is scaled by a personal gain — see [Calibration](#calibration).
The gains are the only per-user tuning; everything else above is the same model
for everyone.

## Sleep Quality And Charge

A night's charge depends on how well it was slept, not only how long.

This is worth stating plainly because it did not used to be true. The charge
counted the minutes and read overnight HRV, and never asked how those minutes
went — so an unbroken eight hours with a healthy deep and REM share charged
within a couple of points of a shallow, repeatedly interrupted eight hours. Two
mornings that feel nothing alike arrived at nearly the same number.

What the charge now reads is the **quality pillar** of the
[sleep score](sleep-score-and-recovery.md): sleep efficiency, time awake after
falling asleep, and the deep/REM share of the night. That pillar alone, not the
whole sleep score — the score's other two pillars are duration and overnight
HRV, and the charge already counts both, so folding in the whole score would
count them twice and make the factor a duration multiplier under another name.

The rules that keep it honest:

- **An ordinary night charges exactly what it always did.** The factor is
  centred, not added: a middling night is neutral, better nights charge more,
  worse nights less. This is not only taste — a factor that sat above neutral
  on the ordinary night would inflate every night rather than tell them apart.
- **The swing is bounded to ±20%.** No single night of broken staging can undo
  hours actually slept, and duration stays the dominant term.
- **A night with no deep/REM staging is left alone.** A source that writes only
  a start and an end has its sleep duration *equal* its time in bed, so
  efficiency reads 100% and time awake zero. Read literally that is a flawless
  night, and it would be handed the full bonus for recording nothing at all.
  Those nights charge exactly what they charged before.
- **A nap is not judged.** Under an hour of sleep says nothing about quality.

The charge reads a **continuous** quality, which is deliberately not the same
reading the sleep score displays. The score answers "was this a healthy night"
and cites NSF and AASM thresholds to do it: efficiency at or above 85% is good,
twenty minutes awake or less is good, and both earn full marks the moment they
clear. That is the right shape for a score and the wrong shape for recovery,
because it makes a flawless night and a merely good one identical. So the same
pillar is also read as a continuous ramp — efficiency all the way to 100%, time
awake all the way to none — and the charge uses that. Stage architecture is
deliberately *not* stretched the same way: more deep sleep past the healthy band
is not more recovery.

Over eight hours in bed, the charge before other factors runs roughly:

| Night | Charge |
| --- | --- |
| Flawless: 90 min deep, 115 REM, none awake | ~56 |
| Good: 75 deep, 95 REM, 15 min awake | ~54 |
| Ordinary: 50 deep, 70 REM, 35 min awake | ~48 |
| Poor: 25 deep, 45 REM, 60 min awake | ~39 |
| Broken: 10 deep, 20 REM, 150 min awake | ~38 |

The worst two meet at the floor, which is the bound doing its job.

Changing any of this changes every stored day, so
`BodyEnergyTimelineAlgorithmVersion` is bumped and the cached timelines
recompute. See `domain/insights/BodyEnergyTimeline.kt`.

## Signals

Body Energy is calculated locally from available Health Connect-backed signals and app preferences. Missing or sparse source data lowers confidence instead of pretending the estimate is complete.

## Data Model

Body Energy is not a raw Health Connect record. It is an OpenVitals-derived wellness estimate and should be treated as general guidance, not medical advice.

## Privacy

The calculation runs on device. OpenVitals does not upload Body Energy inputs or results to an OpenVitals server.
