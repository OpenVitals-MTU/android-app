# Refactor Plan

This document tracks architecture cleanup work that should happen incrementally without blocking normal feature delivery.

## Progress

- 2026-04-26: Step 1 completed. Navigation-created ViewModels now use lifecycle-owned Compose `viewModel` factories while keeping manual dependency wiring.
- 2026-04-26: Step 2 completed. Period types, period formatting, and period selection math now live in `core/period`, and metric ViewModels use `PeriodSelection` for range/date actions.
- 2026-04-26: Step 3 completed. Health Connect record-list reads now use `readRecordsPaged(...)`.
- 2026-04-26: Step 4 completed. `HealthConnectManager` now delegates permissions, availability, diagnostics, paging, feature reads, and record mapping to focused Health Connect boundary classes.
- 2026-04-26: Step 5 completed for metric and record surfaces. Activity, activity detail, sleep, sleep detail, heart, heart vitals, body, nutrition, cycle, and browse UI now split screen wiring from feature-local cards, rows, charts, and presentation helpers.
- 2026-04-26: Step 6 completed. `HealthData.kt` was split into feature-oriented model files under `data/model`.
- 2026-04-26: Step 7 completed. Removed inactive Room/WorkManager dependencies and stale WorkManager manifest cleanup, and updated stale docs.

## Refactor Order

### 1. Use lifecycle-owned ViewModels in navigation

Status: done.

Current issue: `AppNavigation` manually constructs ViewModels with `remember { ... }`.

Target:

- create ViewModels through AndroidX `viewModel(factory = ...)`
- keep manual dependency wiring for now
- scope destination ViewModels to their navigation back stack entries
- move route arguments toward `SavedStateHandle` when the factories are stable

Why first: this is a lifecycle correctness issue. `viewModelScope` is only fully correct when the instance is owned by a `ViewModelStore`.

### 2. Extract period state and period math

Status: done.

Current issue: `selectRange`, `previousPeriod`, `nextPeriod`, `selectDate`, and `periodFor` usage are duplicated across metric ViewModels.

Target:

- add `core/period`
- move `TimeRange`, `DatePeriod`, `periodFor`, `periodTitle`, and related helpers there
- add a small reusable period-state helper
- avoid a giant base ViewModel

### 3. Add shared Health Connect pagination helpers

Status: done.

Current issue: record reads use repeated `readRecords` calls, and some record types can silently truncate when data exceeds a page.

Target:

- add a reusable `readRecordsPaged(...)` helper inside the Health Connect boundary
- use it for all list/readings APIs
- keep repository APIs feature-oriented

### 4. Split `HealthConnectManager`

Status: done.

Current issue: `HealthConnectManager` owns permissions, availability, feature flags, reads, mapping, logging, and route handling.

Target:

- `HealthConnectPermissionService`
- `HealthConnectAvailabilityService`
- feature readers such as `ActivityHealthReader`, `SleepHealthReader`, `HeartHealthReader`, and `BodyHealthReader`
- mapper helpers near the Health Connect boundary

### 5. Split large feature UI files by responsibility

Status: done for metric/detail feature surfaces. App-shell flows such as dashboard, settings, and onboarding can still be split separately if they become a maintenance bottleneck.

Current issue: large screen files mix screen wiring, charts, row components, and chart math.

Target:

- `*Contract.kt` for state/action contracts where useful
- `*Screen.kt` for scaffold wiring
- `*Charts.kt` for charts
- `*Rows.kt` for row/card components
- pure summary/bucketing helpers outside composables

### 6. Split the central health model file

Status: done.

Current issue: `HealthData.kt` contains models for dashboard, activity, sleep, body, vitals, nutrition, cycle, and preferences-related enums.

Target:

- feature model files under `data/model` or feature-owned model files
- keep Health Connect response details below the feature/repository layer
- keep display formatting out of raw data models where possible

### 7. Clean inactive dependencies and stale docs

Status: done.

Original issue: Room and WorkManager were declared but not active architecture pillars, and some docs still referenced old package paths.

Target:

- remove unused dependencies unless an active cache/job design is introduced
- update docs to the current `tech.mmarca.openvitals` namespace
- keep docs aligned with the implemented architecture

## Guardrails

- Make one architectural improvement at a time.
- Preserve feature behavior during each refactor.
- Prefer small shared helpers over broad inheritance.
- Keep metric-specific chart semantics inside each feature.
- Run `testDebugUnitTest` after each meaningful refactor step.
