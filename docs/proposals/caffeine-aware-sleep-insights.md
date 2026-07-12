# Caffeine Detail And Future Sleep Insights

> **Status:** Mixed current/proposed note, still accurate after the Flutter port. The standalone caffeine feature is implemented; direct sleep-detail integration is **still not implemented** — verified 2026-07-11: nothing under `lib/features/sleep/`, `lib/domain/insights/sleep_score.dart` or `lib/domain/usecase/load_sleep_period_use_case.dart` references caffeine, and `lib/domain/insights/caffeine_insight_calculator.dart` has exactly one consumer, `lib/features/caffeine/application/caffeine_view_model.dart`.
> **Current behavior source:** [Beverage logging and caffeine](../features/beverage-logging-and-caffeine.md), [Sleep score and recovery](../features/sleep-score-and-recovery.md).
> **Implementation map:** [Feature map](../features/feature-map.md).

OpenVitals currently treats caffeine as its own detail experience fed by caffeine nutrition records from beverage logging and other Health Connect sources. The caffeine screen helps users understand timing, active caffeine, intake distribution, limits, sensitivity settings, and bedtime guidance.

## Current User Experience

The caffeine detail screen can show:

- Today's caffeine intake.
- Estimated active caffeine.
- A modeled caffeine curve.
- Sleep-impact guidance based on configured bedtime and sensitivity.
- Daily impact and time-of-day distribution.
- Recent caffeine entries.
- Setup controls for daily limit, sensitivity, bedtime, and pre-sleep window.

This is intentionally not a canonical period-detail screen. It uses caffeine-specific ranges and science/context sections rather than the shared `Day / Week / Month / Year` metric scaffold.

## Data Flow

Caffeine records are written through beverage logging when a selected drink includes caffeine values. Health Connect remains the source of truth for the saved nutrition records.

The standalone caffeine feature loads entries and preferences, estimates active caffeine, and renders caffeine-specific analytics. It does not currently attach caffeine signals to the sleep detail screen, change sleep scores, or add a caffeine insight card inside sleep.

## Relationship To Sleep

The current app can provide bedtime-oriented caffeine guidance inside the caffeine screen. Direct caffeine-aware sleep insights are planned for a future iteration.

Planned sleep integration should keep these rules:

- Missing caffeine records should not lower a sleep score.
- Days without caffeine coverage should be treated as missing coverage, not zero-caffeine days.
- Sleep UI should render precomputed insight models instead of calculating caffeine thresholds or correlations inside widgets. Threshold and correlation logic belongs in `lib/domain/insights/`.
- Any future numeric sleep-score adjustment should be bounded, explicit, and documented.

## Current Implementation

Relevant implemented pieces include:

- [`lib/features/caffeine/application/caffeine_view_model.dart`](../../lib/features/caffeine/application/caffeine_view_model.dart): the Riverpod notifier that loads caffeine entries and preferences, estimates active caffeine, and drives the analytics ranges.
- [`lib/features/caffeine/presentation/caffeine_screen.dart`](../../lib/features/caffeine/presentation/caffeine_screen.dart): renders setup, overview, caffeine curve, sleep-impact guidance, daily impact, distribution, and recent entries. Reached at `/metric/CAFFEINE`.
- [`lib/domain/insights/caffeine_insight_calculator.dart`](../../lib/domain/insights/caffeine_insight_calculator.dart): the active-caffeine / bedtime-impact model. This is the piece a future sleep integration should consume — it is already outside the feature, in the shared insights layer.
- [`lib/data/repository/contract/caffeine_repository.dart`](../../lib/data/repository/contract/caffeine_repository.dart): the caffeine read API over Health Connect nutrition records.
- [`beverage-logging-and-caffeine.md`](../features/beverage-logging-and-caffeine.md): describes the beverage logging flow that can write caffeine nutrition values.

## Planned Work

Future caffeine-aware sleep work should add the domain signal, sleep presentation mapping, sleep UI card, and focused tests before this document is promoted from planned integration to implemented sleep behavior.
