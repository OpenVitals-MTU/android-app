# Body Energy

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/bodyenergy`, `features/readiness`.
> **Navigation:** `Screen.BodyEnergyDetails`, widget `BODY_ENERGY`, settings section `RECOVERY`.
> **Related:** [Feature map](feature-map.md), [Daily readiness](daily-readiness.md), [Home screen widgets](home-widgets.md).

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

## Signals

Body Energy is calculated locally from available Health Connect-backed signals and app preferences. Missing or sparse source data lowers confidence instead of pretending the estimate is complete.

## Data Model

Body Energy is not a raw Health Connect record. It is an OpenVitals-derived wellness estimate and should be treated as general guidance, not medical advice.

## Privacy

The calculation runs on device. OpenVitals does not upload Body Energy inputs or results to an OpenVitals server.
