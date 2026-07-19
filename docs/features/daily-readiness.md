# Daily Readiness

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/readiness/`, `lib/features/bodyenergy/`, `lib/features/recovery/` (the stress detail screen), `lib/domain/insights/` (`daily_readiness.dart`, `stress_tracking.dart`, `body_energy_timeline.dart`).
> **Navigation:** `/daily_readiness`, `/daily_readiness/body_energy/:bodyEnergyDate`, `/daily_readiness/training_readiness/:trainingReadinessDate`, `/daily_readiness/stress/:stressDate`.
> **Related:** [Feature map](feature-map.md), [Body Energy](body-energy.md), [Sleep score and recovery](sleep-score-and-recovery.md).

Daily Readiness is a local wellness view that summarizes how ready the user may be for the day based on available Health Connect signals.

## How to use it

1. **Open Daily Readiness.** Tap the **Daily Readiness** icon (top-left of the dashboard app bar), or add the Daily Readiness home-screen widget.
2. **Read the score.** The panel shows an overall **Score /100** with a confidence line, a status title, a recommendation, and a short explanation of what drove it.
3. **Drill into the drivers.** Tap the **Body energy** tile to open [Body Energy](body-energy.md), the **Training** tile to open Training Readiness, or the **Stress level** row to open the Stress detail. Each sub-screen explains its 0–100 scale, the signals it used, what it means, and its caveats.
4. **Follow the guidance.** The **Recommended / Avoid / Alternative** rows, plus **Strain**, **Goal**, and the **Why** factor list, translate the score into concrete suggestions for the day.
5. **Change the day / refresh.** Use the day navigator and calendar to look at other days, and pull down to recompute from Health Connect. Daily Readiness is a **per-day** view — there is no Day/Week/Month/Year toggle.

If you see **"No readiness data for this day"**, the underlying signals (sleep, heart rate, HRV, activity) weren't available for that day. Readiness improves as more of those are present — wearing a heart-rate device overnight and granting sleep and heart permissions has the biggest effect.

## What It Includes

- Daily Readiness score.
- Body Energy.
- Training Readiness.
- HRV status.
- Intensity minutes.
- Physiological stress.
- Recommended activity, activity to avoid, alternatives, and adaptive goal context.

## How It Works

OpenVitals combines available sleep, heart, activity, HRV, and stress-related signals using local rules. The screen explains which signals were available and how missing data affected confidence.

## Navigation

Daily Readiness is day-based. Users can move between days, open the calendar, and refresh data from Health Connect.

## Caveat

Daily Readiness is not medical advice. It is a local, rule-based estimate intended for general wellness context.
