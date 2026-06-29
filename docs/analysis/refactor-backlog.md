# Refactor Backlog

Prioritized improvements from the architecture code analysis. Ordered by **impact vs. effort**. Pick items when touching related files — no big-bang rewrite required.

**Status (June 2026):** P0–P3 items below are **complete** for metric detail screens, dashboard, and cross-cutting phases. See [Migration tracker](#migration-tracker-gap-closure-program) for per-feature checklist. Remaining large files are mostly manual-entry, settings, and secondary flows.

## P0 — High impact ✅ Complete

### Split oversized screen files

**Problem:** Route, sections, charts, and permission wiring lived in single files (up to ~2,000 lines).

**Outcome:** Metric routes are thin (~100–200 lines); content lives in sibling composables (`*MetricContent`, `*PeriodContent`, `DashboardContent`, etc.). Activity recording split into setup + recording screens.

| Route (after) | Approx. lines |
|---------------|---------------|
| `DashboardScreen.kt` | ~147 |
| `SleepScreen.kt` | ~100 |
| `HydrationScreen.kt` | ~104 |
| `BodyScreen.kt` | ~197 |
| `HeartScreen.kt` | ~290 |
| `ActivityRecordingScreen.kt` | ~401 |

**Done when:** Route composable &lt; ~150 lines; sections in named files per [project-structure.md](project-structure.md). ✅

### Move sleep/activity derived state into ViewModels

**Problem:** Screens computed summaries, chart points, and insights in `remember { }` on the main thread.

**Outcome:** `*PresentationMapper` + `*DisplayState` on metric ViewModels; routes render `display` only. See per-feature migration table.

## P1 — Medium impact, low cost ✅ Complete

### Add `@Immutable` to `*UiState` data classes

**Problem:** Compose could not skip recomposition aggressively.

**Outcome:** All feature `UiState` and display DTOs annotated with `@Immutable`.

### Introduce sealed `ScreenError`

**Outcome:** `ScreenError` in `core/presentation`; rolled out to period-detail and dashboard ViewModels. See [error-handling-null-safety.md](error-handling-null-safety.md).

### Granular state collection (where profiling shows jank)

**Outcome:** Pilot on `DashboardScreen` and `HeartScreen` using `remember(viewModel) { uiState.map { … } }`. Extend only if profiling shows jank on other screens.

## P2 — Medium impact, medium effort ✅ Complete

### Extract use cases for dashboard and heart loads

**Outcome:** `LoadDashboardDayUseCase`, `LoadHeartPeriodUseCase`, `LoadSleepPeriodUseCase` in `domain/usecase/` with unit tests. ViewModels delegate orchestration.

See [clean-architecture-refactor.md](clean-architecture-refactor.md).

### Repository interfaces

**Outcome:** Interfaces + `@Binds` for `SleepRepository`, `ActivityRepository`, `HealthRepository`, `HeartRepository`, `HydrationRepository`, `BodyRepository`. Implementations in `*Impl` classes.

### Move period result DTOs to domain

**Outcome:** `*PeriodData` types in `domain/query/`; repositories return domain query types.

## P3 — Lower urgency ✅ Complete

### Slim `HealthRepository`

**Outcome:** `HealthRepositoryImpl` ~55 lines (permissions/availability). Dashboard reads via `DashboardDataLoader` + `DashboardAggregator`.

### Compose UI tests

**Outcome:** `SleepScreenWeekTest` androidTest; `compileDebugAndroidTestKotlin` in `verifyLocalApp`; optional `verifyAndroidTest` when device connected.

### Split broad shared UI files

**Outcome:** `SectionHeader.kt` and `TimeRangeSelector.kt` extracted from `MetricCard.kt`.

## Explicitly deferred (do not schedule without new requirements)

- Multi-module Gradle split (`:domain`, `:data`, `:feature`)
- Universal chart abstraction
- `BasePeriodViewModel` hierarchy
- MVI / reducer framework for standard detail screens
- Room mirror of raw Health Connect records
- General background sync beyond cache + import workers

## Migration tracker (gap closure program)

Verify before each PR: `.\gradlew.bat verifyLocalApp` (see [development.md](../development.md)).

### Sleep pilot baseline (before refactor)

| Metric | Value |
|--------|-------|
| `SleepScreen.kt` lines | 936 |
| `remember(` derivation blocks | 10 |
| `SleepViewModelTest` areas | initial state, load success/failure, period navigation, stale loads |

### Sleep pilot acceptance criteria

- [x] `SleepPresentationMapper` + `SleepDisplayState` with unit tests
- [x] `SleepViewModel` builds `display` on `DispatcherProvider.default`
- [x] `SleepScreen` route has no `domain/insights` imports
- [x] `ScreenError` on sleep ViewModels
- [x] `SleepScreen.kt` route &lt; 150 lines; sections split into named files
- [x] `./gradlew verifyLocalApp` green

### Per-feature migration

| Feature | Mapper + VM display | Thin screen | File split | ScreenError | Notes |
|---------|---------------------|-------------|------------|-------------|-------|
| Sleep | [x] | [x] | [x] | [x] | Pilot complete |
| Activity | [x] | [x] | [x] | [x] | Second pilot complete |
| Heart | [x] | [x] | [x] | [x] | LoadHeartPeriodUseCase + display pilot |
| Body | [x] | [x] | [x] | [x] | Display mapper + thin BodyScreen routes + BodyMetricContent |
| Hydration | [x] | [x] | [x] | [x] | Display mapper + period content split |
| Nutrition | [x] | [x] | [x] | [x] | Display mapper + period content split + metric route wiring |
| Mindfulness | [x] | [x] | [x] | [x] | Display mapper + period content split |
| Cycle | [x] | [x] | [x] | [x] | Display mapper + period content split |
| Manual entry | n/a | [x] | [x] | [x] | Hub + 5 write forms; activity entry ScreenError |
| Activity recording | n/a | [x] | [x] | n/a | Recording screen split; live `errorMessage` stays localized in controller |
| Dashboard | [x] | [x] | [x] | [x] | Presentation mapper + screen split + deferred-load coordinator |
| Daily readiness | n/a | [x] | [x] | [x] | Route split; insight from ViewModel |

### Cross-cutting program phases

| Phase | Item | Status |
|-------|------|--------|
| 4 | `@Immutable` on all `*UiState` | [x] |
| 4 | `ScreenError` rollout complete | [x] |
| 5 | `domain/query/*PeriodData` | [x] |
| 6 | `LoadHeartPeriodUseCase`, `LoadDashboardDayUseCase`, `LoadSleepPeriodUseCase` | [x] |
| 7 | Repository interfaces (Sleep, Activity, Health, Heart, Hydration, Body) | [x] |
| 8 | `DashboardAggregator` + slim `HealthRepository` | [x] |
| 9 | Compose UI tests for `MetricDetailScaffold` | [x] |

## Documented exceptions

| Area | Decision |
|------|----------|
| Activity recording errors | `errorMessage` stays in recording controller; localized at source; `ScreenError` n/a |
| Activity entry | `ScreenError` on ViewModel; form/recording split into sibling composables |
| androidTest | `compileDebugAndroidTestKotlin` in `verifyLocalApp`; `connectedDebugAndroidTest` optional via `verifyAndroidTest` when `ANDROID_SERIAL` is set |
| Period screens | Keep existing ViewModel period navigation; MVI/reducer deferred |
| Charts | Feature-owned semantics; shared primitives stay in `ui/components` until a second consumer appears |

## Tracking template

When starting a backlog item, note:

```markdown
## [Item name]
- Owner:
- Branch:
- Acceptance criteria: (from above)
- Files touched:
- Tests added/updated:
```

## Related index

- [Executive summary](executive-summary.md)
- [Compose performance](compose-performance.md)
- [Feature playbook](../feature-playbook.md)
