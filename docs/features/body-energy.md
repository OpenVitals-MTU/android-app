# Body Energy

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/bodyenergy/`, `lib/features/readiness/`, `lib/domain/insights/body_energy_timeline.dart`, `lib/data/repository/body_energy_timeline_cache_store.dart` (the app's only derived-value cache; it lives in `SharedPreferences`, not drift).
> **Navigation:** `/daily_readiness/body_energy/:bodyEnergyDate` (the dashboard Body Energy tile links here); calibration under `/settings/recovery`.
> **Related:** [Feature map](feature-map.md), [Daily readiness](daily-readiness.md), [Home screen widgets](home-widgets.md).

Body Energy is a local derived view that estimates available energy across the day from supported wellness signals. It is intentionally a selected-day experience rather than a canonical `Day / Week / Month / Year` metric-detail screen. (Some fitness trackers call the same idea "Body Battery".)

## How to use it

### Turn it on

1. **Open Body Energy.** Tap the **Body Energy** tile (battery icon) on the dashboard, or the **Body energy** tile inside [Daily Readiness](daily-readiness.md). Before setup the tile reads **"Not set up"**.
2. **Set it up.** On first open the screen shows only the **"Turn on Body Energy"** card. Either accept **"Use automatic estimates"**, or turn on **Manual heart zones** and enter your **Zone 1–5 lower bpm** bounds, then **Save**. The timeline appears once setup is complete.

### Read the day

1. **Summary card** — your current energy score, with **Start**, **Charged (+)**, and **Drained (−)** for the day and a confidence label (High / Medium / Low / No data) with the reason.
2. **Timeline** — the curve across the day with an influence legend showing what raised or lowered it.
3. **"What moved it"** — the specific events (sleep, workouts, stress) with their ± contribution.
4. **Inputs** — which signals were available (heart rate, sleep, workouts, resting HR, HRV, respiratory rate, previous score, calibration) and which were missing.
5. **Change the day / refresh.** Use the day navigator and calendar to look at other days; pull down to recompute (this needs heart-rate read access).

### Teach it your body

On today's view, the **"How's your energy?"** feel-check offers **0–10** buttons — tap the one that matches how you feel and it tunes your personal model over time.

### Recalibrate or reset

Go to **Settings › Recovery** and open the **Body Energy** card (the last card, after Sleep range and the heart-rate steppers):

- Toggle **Manual heart zones** and edit the **Zone 1–5 lower bpm** fields, then **Save** — or tap **Use automatic estimates** to hand zone detection back to the app.
- If feel-checks have built up a **Personal tuning** profile, you'll see the learned multipliers and a **Reset personal tuning** button to start over.

If confidence stays **Low**, it usually means incomplete calibration or sparse Health Connect data — wear a heart-rate device and grant sleep and heart permissions for the biggest improvement.

## What It Shows

Body Energy can appear in:

- Daily Readiness.
- The dashboard.
- A dedicated Body Energy detail flow.
- Android home screen widgets.

The detail view can show a summary, timeline, confidence, and context for the selected day.

## Calibration

Body Energy supports calibration during onboarding and in Recovery settings. Calibration helps the local estimate better fit the user, and can be reset when needed.

## Signals

Body Energy is calculated locally from available Health Connect-backed signals and app preferences. Missing or sparse source data lowers confidence instead of pretending the estimate is complete.

## Data Model

Body Energy is not a raw Health Connect record. It is an OpenVitals-derived wellness estimate and should be treated as general guidance, not medical advice.

## Privacy

The calculation runs on device. OpenVitals does not upload Body Energy inputs or results to an OpenVitals server.
