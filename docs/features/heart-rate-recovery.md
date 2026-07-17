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

## Data and privacy

HRR is a local, derived wellness estimate. It is not written to Health Connect and is not stored by the app — only the guided test's exercise session (and its rest segment) is saved, like any other recording. See [Privacy, support, and diagnostics](privacy-support-diagnostics.md).
