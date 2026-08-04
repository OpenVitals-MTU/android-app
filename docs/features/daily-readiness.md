# Daily Readiness

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/readiness`, `features/bodyenergy`, `domain/insights`.
> **Navigation:** `Screen.DailyReadiness`, `Screen.BodyEnergyDetails`, `Screen.TrainingReadinessDetails`, `Screen.StressDetails`.
> **Related:** [Feature map](feature-map.md), [Body Energy](body-energy.md), [Sleep score and recovery](sleep-score-and-recovery.md).

Daily Readiness is a local wellness view that summarizes how ready the user may be for the day based on available Health Connect signals.

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
