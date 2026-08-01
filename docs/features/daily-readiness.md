# Daily Readiness

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/readiness/` (the card), `lib/features/bodyenergy/` (the host view), `lib/features/recovery/` (the stress detail screen), `lib/domain/insights/` (`daily_readiness.dart`, `stress_tracking.dart`, `body_energy_timeline.dart`).
> **Navigation:** lives inside [Body Energy](body-energy.md) (`/daily_readiness/body_energy/:bodyEnergyDate`); sub-details at `/daily_readiness/training_readiness/:trainingReadinessDate` and `/daily_readiness/stress/:stressDate`.
> **Related:** [Feature map](feature-map.md), [Body Energy](body-energy.md), [Sleep score and recovery](sleep-score-and-recovery.md).

Daily Readiness is a local wellness verdict that summarizes how ready the user may be for the day based on available Health Connect signals. Since 2.4.1 it is not a screen of its own: it rides along inside the [Body Energy](body-energy.md) view as the day's verdict, following the battery card it draws on. The old standalone screen, its `/daily_readiness` route and the dashboard app-bar icon are gone; a Daily Readiness home-screen widget placed before the merge keeps working — its stored route is retargeted to the merged view at launch.

## How to use it

1. **Open Body Energy.** Tap the **Body Energy** tile (battery icon) on the dashboard, or the Daily Readiness home-screen widget. The screen reads as a story: the battery (score and timeline), the readiness verdict, what moved it, what it means for today, then the method.
2. **Read the score.** The readiness card shows an overall **Score /100** with a confidence line, a status title, a recommendation, and a short explanation of what drove it.
3. **Drill into the drivers.** Tap the **Training** tile to open Training Readiness, or the **Stress level** row to open the Stress detail. Each sub-screen explains its 0–100 scale, the signals it used, what it means, and its caveats. (Body energy needs no tile: it is the screen the card lives in.)
4. **Follow the guidance.** The **Recommended / Avoid / Alternative** rows, plus **Strain**, **Goal**, and the **Why** factor list, translate the score into concrete suggestions for the day.
5. **Change the day / refresh.** Use the host view's day navigator and calendar; the readiness card stays pointed at the selected day. Pull down to recompute from Health Connect. Readiness is a **per-day** verdict — there is no Day/Week/Month/Year toggle.

If you see **"No readiness data for this day"**, the underlying signals (sleep, heart rate, HRV, activity) weren't available for that day. Readiness improves as more of those are present — wearing a heart-rate device overnight and granting sleep and heart permissions has the biggest effect.

## What It Includes

- Daily Readiness score.
- Body Energy — the **measured** battery, when calibrated: the readiness load includes the body-energy timeline, whose current score replaces the estimate the deltas approximate. Arriving at the day drained pulls an otherwise perfect morning out of "ready" — deliberately weighted so no combination of good sleep and calm vitals can outvote an empty battery — and a low start still counts after a mid-day recharge. Without a calibrated timeline the estimate stands.
- Training Readiness.
- HRV status.
- Intensity minutes.
- Physiological stress.
- Recommended activity, activity to avoid, alternatives, and adaptive goal context.

## How It Works

OpenVitals combines available sleep, heart, activity, HRV, and stress-related signals using local rules. The card explains which signals were available and how missing data affected confidence.

## Caveat

Daily Readiness is not medical advice. It is a local, rule-based estimate intended for general wellness context.
