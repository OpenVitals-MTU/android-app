# Body Energy

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/bodyenergy/`, `lib/features/readiness/`, `lib/domain/insights/body_energy_timeline.dart`, `lib/data/repository/body_energy_timeline_store.dart` (the day summaries + 5-minute buckets, in drift), `lib/data/sync/body_energy_chain_sync_service.dart` (the background chain warm), `lib/data/repository/body_energy_baseline_cache_store.dart` (the 28-day baselines, still in `SharedPreferences`).
> **Navigation:** `/daily_readiness/body_energy/:bodyEnergyDate` (the dashboard Body Energy tile links here); calibration under `/settings/recovery`.
> **Related:** [Feature map](feature-map.md), [Daily readiness](daily-readiness.md), [Home screen widgets](home-widgets.md).

Body Energy is a local derived view that estimates available energy across the day from supported wellness signals. It is intentionally a selected-day experience rather than a canonical `Day / Week / Month / Year` metric-detail screen. (Some fitness trackers call the same idea "Body Battery".)

## How to use it

### Turn it on

1. **Open Body Energy.** Tap the **Body Energy** tile (battery icon) on the dashboard, or the **Body energy** tile inside [Daily Readiness](daily-readiness.md). Before setup the tile reads **"Not set up"**.
2. **Set it up.** On first open the screen shows only the **"Turn on Body Energy"** card. Either accept **"Use automatic estimates"**, or turn on **Manual heart zones** and enter your **Zone 1–5 lower bpm** bounds, then **Save**. The timeline appears once setup is complete.

### Read the day

1. **Summary card** — your current energy score, with **Start**, **Charged (+)**, and **Drained (−)** for the day and a confidence label (High / Medium / Low / No data) with the reason. **Start** is where the previous day ended: energy carries across midnight rather than resetting, and the row reads as a ledger — `Start + Charged − Drained` is exactly the score shown. Because the score cannot fall below 0 or rise above 100, a day that runs empty reports the drain that actually landed, not the larger figure the model would otherwise have subtracted; "What moved it" below is scaled to match, so its entries always sum to the headline.
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

## Continuity across days

Body Energy is a continuous measure, not a per-day one. Each day opens on the score the previous day ended with, so a day that finished at 20 starts the next morning at 20 rather than at a fixed midpoint.

Three things follow from that:

- **A day you never opened still counts.** The app keeps a rolling window of recent days computed in the background, so the chain stays connected even if you go a week without looking.
- **A gap is stated, not papered over.** If the last stored day is too far back to bridge, the day starts from the neutral midpoint and the **Previous score** input row says the score was unavailable — rather than silently presenting a week-old number as yesterday's.
- **A fully drained day cannot trap the estimate at zero.** A carried-over score is floored at a small minimum; when that happens the **Previous score** row shows both the raw and the floored value.

**Quiet time recharges you, not just sleep.** Sitting with your heart rate in the bottom sixth of your range counts as recovery, at a slower rate than sleep. It stops as soon as your heart rate rises or you start moving, and it does not apply in the hours after a hard workout — that window is what the recovery-debt drain exists to model, and you are not recovering yet.

Days are stored locally in 5-minute buckets. Recent days keep their full buckets; older ones keep their daily summary so long-range history stays intact.

**Once a day has settled, it stops being recalculated.** For about a week after it ends a day is still recomputed on open, because a watch synced late can add to it. After that the stored copy is what you see: older days open instantly, without re-reading Health Connect. Pull down to refresh if you want to force a recalculation anyway, and changing your calibration or Body Energy settings rebuilds affected days automatically.

**Syncing a watch rebuilds the days it back-filled**, however far back they go. A Garmin sync that hands over a week of sleep and heart-rate data recalculates that week and everything after it, without waiting for the settling window — so a watch you only sync occasionally still produces correct history. Data arriving in Health Connect from *other* apps is not detected this way; pull down to refresh a day if you know something else has written to it.

That also protects your history. If you have not granted Health Connect's **Health history** access, it only serves the app about 30 days of data — so a day past that point could not be rebuilt even if it were tried. A recalculation that comes back with nothing now leaves the stored day alone rather than replacing it.

## For contributors: the calibration diagnostic

Body Energy pins at zero on some days — the model wants to drain more energy than the day started with. Two things explain that, and the screen cannot tell them apart:

- the **drain constants** (`_activeKcalToPoints`, `_drainRateForZone`, `_basalPointsPerMinute`, the stress rates) are too hot, or
- the **active-calorie input is doubled**, because Health Connect aggregates sum every app that writes a metric and nothing in the read path filters by source. If Garmin Connect and OpenVitals' own FIT import both write the same watch's calories, the model eats the total.

**Settings › Debug diagnostics › Body Energy calibration** (debug builds only) prints what distinguishes them, for the last 7 days. Run it deliberately — it is a button, not an auto-load, because a cold run costs roughly sixty Health Connect calls.

How to read it:

| what you see | what it means |
|---|---|
| the `kcal` half of the activity drain dominates **and** two packages each wrote roughly half the day's calories | the input is doubled; the fix is source filtering, not a constant |
| the `kcal` half dominates, one package, and its total matches the watch's own | `_activeKcalToPoints` is too hot |
| the `zone` half dominates | `_drainRateForZone` is the culprit and calories are a red herring |

Two columns exist because the headline can no longer show over-draining. Since the day totals became *applied* rather than gross, a day that wants 250 points of drain and one that wants 60 read identically once both pin at zero — so the report prints `floor Nb from HH:MM` instead. And the watch comparison uses **delta sums** rather than start-minus-end, because both models clip at 0 and 100 and endpoint arithmetic understates a day that pinned.

One caveat when reading the per-influence table: a paired error is the *accumulated* divergence since the two models last agreed, not an instantaneous attribution to that bucket's influence. It explains why the gains drifted where they did; it is not a per-component residual.

## Data Model

Body Energy is not a raw Health Connect record. It is an OpenVitals-derived wellness estimate and should be treated as general guidance, not medical advice.

## Privacy

The calculation runs on device. OpenVitals does not upload Body Energy inputs or results to an OpenVitals server.
