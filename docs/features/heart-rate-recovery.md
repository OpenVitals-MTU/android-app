# Heart Rate Recovery

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/domain/insights/heart_rate_recovery.dart` (the measurement), `lib/domain/usecase/load_activity_detail_use_case.dart` and `lib/domain/usecase/load_heart_rate_recovery_period_use_case.dart` (loading), `lib/features/activity/presentation/activity_heart_rate_recovery_card.dart` (the card), `lib/features/heart/presentation/heart_rate_recovery_screen.dart` (the trend), `lib/features/manualentry/activity/recording/` (the guided test).
> **Navigation:** the guided test is started from the activity recorder setup; the result appears on the activity detail screen (`/activity_detail/:activityId`); the history is at `/heart/recovery`, opened from the heart detail screen.
> **Related:** [Feature map](feature-map.md), [Heart and vitals](heart-and-vitals.md), [Recording of activity](activity-recording.md).

Heart-rate recovery (HRR) is how far your heart rate falls in the minutes after hard effort stops. A fitter heart falls faster, so the one-minute drop is a simple, well-studied fitness marker. OpenVitals computes it locally, as a wellness estimate — it is not a Health Connect record and not medical advice.

## Why it needs a deliberate test

HRR only means something when you drive your heart rate near its maximum and then **stop abruptly and rest**. If you ease off gradually — slow down but keep moving — the number is measuring your cool-down, not your recovery, and it flatters you.

Because an ordinary recorded workout gives no guarantee that effort actually stopped at the end, OpenVitals does **not** derive HRR from arbitrary sessions. It is measured **only for the app's guided recovery test**, which records the exact instant effort stopped. A normal run, ride, or walk shows no recovery card and does not appear in the recovery history.

## Recording a test

Start it from the activity recorder, before recording:

1. Open the activity recorder and turn on **Heart rate recovery test**. You will need a connected heart-rate sensor (a chest strap or an arm/wrist optical monitor — see [Bluetooth LE sensors](ble-sensors.md)).
2. Optionally set a **Warmup (min)** (default 3) and a **Target (bpm)**. Leave the target empty to end the effort yourself with **End effort**.
3. Start recording. The test runs through four phases, each shown as a banner with a countdown, and spoken/haptic cues:
   - **Warming up** — get moving.
   - **Go hard** ("Go hard now") — push toward your target, or until you tap End effort.
   - **Recovering — keep still** ("Slow down and stop. Stay still.") — stop dead and rest. Staying still is what makes the measurement work; walking it off invalidates it.
   - **Recovery complete — save the workout** — save when the recovery window is done.

Saving writes an ordinary Health Connect exercise session, plus a trailing **rest** segment spanning from the moment effort stopped to the end of the session. That segment is the mark the reader later recognises as a deliberate stop; nothing else about the record is special.

## What you see

### On the activity's detail screen

Open the saved test from the activity list. The **Heart rate recovery** card shows:

- **Peak** — the highest heart rate in the last ten seconds before you stopped.
- The headline **"Down N bpm in a minute"** — the one-minute fall, the figure to lead with.
- A row of marks at **30s, 1, 2, 3, 4, and 5 minutes**, each the heart rate then and the drop from the peak. A mark with no sample close enough shows **"Not measured"** rather than an invented number.
- Plain-language notes when something limits the reading (see [Quality and validity](#quality-and-validity)).

The card appears only for a guided test. A submaximal test still shows the card, flagged as not comparable, rather than being hidden.

### The history and trend

The heart detail screen has a **Heart rate recovery** row that opens the history at `/heart/recovery`. It charts the **one-minute fall** across the selected period (day / week / month / year), with the average and:

- **"N tests could not be compared"** — tests that were measured but not chart-worthy (for example, submaximal).
- **"No recovery test in this period"** — with a prompt to record one — when there are none.

Only guided tests are read, so the trend is a like-for-like comparison over time.

## How the measurement works

Nothing is stored: each reading is computed on read from the heart-rate samples Health Connect already holds, and no value is ever interpolated or guessed.

- **Recovery start** — the instant effort stopped, taken from the test's trailing rest segment.
- **Peak** — the highest heart rate in a hard **10-second** window immediately before the stop. The window does not widen: a wider one could pick a peak from while you were still going and inflate the drop.
- **Marks** — at 30s, 1, 2, 3, 4, 5 min. Each snaps to the nearest sample within a tight tolerance (**±3s** at 30s, **±5s** for the minute marks); outside that, the mark is blank.
- **Effort check** — the peak is judged against your maximum heart rate:
  - your stated maximum (from your profile) if set;
  - otherwise the highest trustworthy heart rate seen in the last 90 days;
  - otherwise the age estimate **208 − 0.7 × age** (Tanaka).

  A peak within a fixed band below that maximum counts as near-maximal — **22 bpm** when the maximum is an age estimate (its ~95% confidence interval), **10 bpm** when the maximum is known. Below the band the reading is marked submaximal and "not comparable".

## Quality and validity

Each reading carries a verdict and, where relevant, a note:

- **Cool-down before the stop** — the heart rate was already falling more than ~4 bpm before the marked stop, so the test is invalid (you eased off first).
- **Heart rate did not fall** — it was as high or higher at a mark than at the peak; the recording ended before the effort did, so there is no recovery. Invalid.
- **Submaximal effort** — a real drop, but the peak was too far below your maximum to compare across days. Shown, not comparable.
- **Maximum unknown** — no maximum could be resolved, so effort could not be judged. Set your maximum heart rate in your profile to remove this.
- **Peak from a single reading** — only one sample stood behind the peak.
- **Device stopped recording** — no samples after the stop, so nothing to measure (common with a watch that quits recording when a workout ends; a strap that records every second avoids it).

Invalid readings are never charted.

## Calculation reference

The measurement is a pure, deterministic function — `calculateHeartRateRecovery` in `lib/domain/insights/heart_rate_recovery.dart` — over the heart-rate samples read for the test's window. Nothing is interpolated; a value that was not measured is left blank.

### 1. Inputs

- `recoveryStart` — the instant effort stopped (the start of the trailing rest segment).
- `samples` — heart-rate samples over the read window `[recoveryStart − 60s, recoveryStart + 5min + 30s]`.
- The maximum-heart-rate context (see step 5).

### 2. Prepare the samples

- Collapse samples that share the same instant to the single **higher** bpm. A strap and a watch can both stamp the same second; keeping the higher one is conservative in both directions (a higher peak is harder to clear the near-max band, and a higher recovery reading reports a *smaller* drop).
- Sort ascending by time.
- If there are no samples → **noData**.

### 3. Peak

- `peak = max(bpm)` over the hard window `[recoveryStart − 10s, recoveryStart]` (inclusive).
- If no sample sits in that window → **noData**. The window never widens: a wider one could take the peak from while effort was still going and inflate the drop.
- If exactly one sample stands behind it → flag `peakFromSingleSample`.

### 4. Recovery samples and marks

- `recoverySamples` = samples **strictly after** `recoveryStart`, up to `recoveryStart + 5min`. (A sample exactly at `recoveryStart` is the value *at* cessation — the thing the fall is measured from — not part of the fall.)
- If there are none → **noData** with `noRecoverySamples`.
- For each offset `o` in {30s, 1, 2, 3, 4, 5 min}:
  - `target = recoveryStart + o`.
  - Take the sample nearest `target` within `tolerance(o)` (30s → ±3s, the minute marks → ±5s). A tie goes to the **earlier** sample (on a falling curve the higher reading, so the smaller drop).
  - If a sample `s` is found: `heartRateBpm = s.bpm`, `dropBpm = peak − s.bpm`, `sampleSkew = |s.time − target|`.
  - Otherwise the mark is blank (`heartRateBpm = null`) — never invented.
- **Headline** = the one-minute mark's `dropBpm`.

### 5. Maximum heart rate

Resolved in order (`_resolveMaxHeartRate`):

1. The **stated** maximum from the profile (if `> 0`) → *known*.
2. The highest **observed** heart rate in the last 90 days, if trustworthy: `observed ≥ max(150, resting + 60)` (or `observed ≥ 150` when the resting rate is unknown) → *known*.
3. The **age estimate** `round(208 − 0.7 × age)`, floored at 1 → *estimated*.
4. Otherwise none → flag `unknownMaxHeartRate` (the marks still compute; only the effort judgement is lost).

### 6. Effort strength (the near-max band)

- `band = 22` bpm when the maximum is estimated (the ~95% confidence interval of the age formula), `10` bpm when it is known.
- If `peak < max − band` → flag `submaximalEffort`.
- `peakFractionOfMax = peak / max` is stored for display.

### 7. Cool-down before the stop

- `recentHigh = max(bpm)` over `[recoveryStart − 60s, recoveryStart]`.
- `atStop` = the sample nearest `recoveryStart` within ±15s.
- If `recentHigh − atStop.bpm > 4` → flag `cooldownBeforeStop`. (Compared against the last-minute high, not the peak, so an easing-off before a hard-window peak is still caught.)

### 8. Heart rate did not fall

- If any mark has `dropBpm ≤ 0` → flag `heartRateDidNotFall` (the recording ended before the effort did).

### 9. Verdict (`quality`)

Evaluated in order:

1. `cooldownBeforeStop` or `heartRateDidNotFall` → **invalid**.
2. All marks blank → **noData**.
3. `submaximalEffort` → **notComparable**.
4. One-minute mark blank, or `peakFromSingleSample`, or `unknownMaxHeartRate` → **approximate**.
5. Otherwise → **clean**.

### 10. Chartable

`isComparable = quality ∈ {clean, approximate} AND the one-minute drop is present`. Only comparable readings enter the trend; invalid ones never do.

### Constants

| Constant | Value | Meaning |
|---|---|---|
| offsets | 30s, 1, 2, 3, 4, 5 min | the recovery marks |
| headline | 1 min | the figure to lead with |
| tolerance | 30s → ±3s; 1–5 min → ±5s | nearest-sample snap per mark |
| peak window | 10s (hard) | run-up before the stop |
| read padding | head 60s, tail 30s | window read around the marks |
| cool-down gate | > 4 bpm | pre-stop fall that invalidates |
| cool-down lookback | 60s | the recent-high window |
| stop tolerance | ±15s | "heart rate at the stop" |
| near-max band | 22 bpm estimated / 10 bpm known | submaximal below it |
| age formula | `round(208 − 0.7·age)`, min 1 | estimated maximum |
| observed-max trust | `≥ max(150, resting + 60)` | when to believe an observed max |
| min rest segment | 90s | qualifies the cessation mark |
| trailing slack | 30s | the rest must end near the session end |

### Worked example

A 40-year-old with no stated maximum runs a guided test that peaks at 178 bpm and reads 145 bpm one minute after stopping:

- Maximum = `round(208 − 0.7 × 40)` = **180 bpm** (estimated).
- Near-max band = 22 → `178 ≥ 180 − 22 = 158` → **not** submaximal.
- One-minute drop = `178 − 145` = **33 bpm** (the headline).
- No pre-stop cool-down and no non-positive drop → verdict **clean**, and it charts.

## Data and privacy

HRR is a local, derived wellness estimate. It is not written to Health Connect and is not stored by the app — only the guided test's exercise session (and its rest segment) is saved, like any other recording. See [Privacy, support, and diagnostics](privacy-support-diagnostics.md).
