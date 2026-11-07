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

### Learning from a Garmin watch

If a Garmin watch is paired, its own Body Battery is used as evidence. The watch
measures what this app models, so where the two disagree the difference is
attributed to whichever gain was driving that moment, and the gain is nudged.
This happens automatically after a sync — there is nothing to turn on.

Three rules keep it honest:

- **An hour counts once.** The watch emits roughly a sample a minute; at most one
  per hour is used, and a watermark records how far the evidence has been
  consumed. Syncing ten times in an hour teaches the model exactly as much as
  syncing once, because otherwise the learning rate would depend on how often
  the user tapped Sync rather than on the data.
- **Nothing is learned from a stale prediction.** The fit runs after the day's
  timeline has been rebuilt with the newly-synced data. Comparing the watch
  against a model that has not yet seen the sleep the same sync just delivered
  would teach it from an artefact of when data arrived.
- **A day is never skipped silently.** If the timeline for a day is not ready,
  its readings are kept for the next run rather than discarded. A day that stays
  unusable for more than two days is given up on, so one permanently-cold day
  cannot block every day behind it.

A first run looks back at most a week, so installing with months of watch
history does not try to fit all of it at once. The whole path is best-effort: a
failure to calibrate never fails the sync that triggered it.

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

Each component is scaled by a personal gain that the watch fit nudges — see
[Calibration](#calibration). The gains are the only per-user tuning; everything
else above is the same model for everyone.

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
  worse nights less. This is not only taste — the sleep gain is fitted against
  watch readings, and a factor that sat above neutral on the ordinary night
  would inflate every night rather than tell them apart.
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
