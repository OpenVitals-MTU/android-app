# Refactor Backlog

Prioritized improvements from the architecture code analysis. Ordered by **impact vs. effort**. Pick items when touching related files — no big-bang rewrite required.

## P0 — High impact, start here

### Split oversized screen files

**Problem:** Route, sections, charts, and permission wiring live in single files (up to ~2,000 lines).

**Targets:**

| File | Approx. lines | Suggested split |
|------|---------------|-----------------|
| `BodyScreen.kt` | ~1,116 | Route, day content, period content, cards |
| `ActivityScreen.kt` | ~277 | Route + scaffold; content in `ActivityMetricContent.kt` |
| `SleepScreen.kt` | ~936 | Route, day/period content, charts |
| `HeartScreen.kt` | ~280 | Route + scaffold; content in `HeartMetricContent.kt` |
| `HydrationScreen.kt` | ~872 | Route, reminders, charts |
| `ActivityRecordingScreen.kt` | ~401 | Route; controls, GPS tabs, dashboard, splits in sibling files |
| `ManualEntryScreen.kt` | ~210 | Route; widgets in `ManualEntryWidgets.kt` |

**Done when:** Route composable &lt; ~150 lines; sections in named files per [project-structure.md](project-structure.md).

### Move sleep/activity derived state into ViewModels

**Problem:** `SleepScreen` (and similar) compute summaries, chart points, and insights in `remember { }` on the main thread.

**Done when:** `SleepUiState` (and peers) expose display-ready fields; ViewModel tests cover derived values; screen only renders.

**References:** `HydrationViewModel`, `BodyViewModel` for patterns already closer to target.

## P1 — Medium impact, low cost

### Add `@Immutable` to `*UiState` data classes

**Problem:** Compose cannot skip recomposition aggressively.

**Action:** Annotate all feature `UiState` and display DTOs with `@Immutable`.

**Files:** `features/*/XxxViewModel.kt` (state data classes), feature display models.

### Introduce sealed `ScreenError`

**Problem:** `String?` errors are not localizable or typed.

**Action:**

1. Add `ScreenError` in `core/presentation`
2. Migrate one pilot screen (e.g. `SleepDetailViewModel`)
3. Map to strings in scaffold or composable resolver
4. Roll out to period-detail screens incrementally

See [error-handling-null-safety.md](error-handling-null-safety.md).

### Granular state collection (where profiling shows jank)

**Problem:** Large `UiState` causes full-screen recomposition.

**Action:** Use `.map { }.collectAsStateWithLifecycle()` for loading-only subtrees, or nested state objects on hot screens (`HeartScreen`, `DashboardScreen`).

## P2 — Medium impact, medium effort

### Extract use cases for dashboard and heart loads

**Problem:** `DashboardViewModel` and `HeartViewModel` are large orchestrators.

**Action:**

- `LoadDashboardDayUseCase`
- `LoadHeartPeriodUseCase`
- `LoadSleepPeriodUseCase` (optional pilot)

**Done when:** ViewModel `load()` is &lt; ~80 lines delegating to use case.

See [clean-architecture-refactor.md](clean-architecture-refactor.md).

### Repository interfaces (top 3)

**Problem:** No compile-time data/presentation boundary.

**Action:** Introduce interfaces + `@Binds` for:

1. `SleepRepository`
2. `HealthRepository`
3. `ActivityRepository`

**Done when:** ViewModels depend on interfaces; existing tests pass with mocks or fakes.

### Move period result DTOs to domain

**Problem:** `SleepPeriodData`, `HeartPeriodData`, etc. live in `data.repository`.

**Action:** Relocate to `domain/query/` or `domain/model/`; update imports.

## P3 — Lower urgency

### Slim `HealthRepository`

**Problem:** ~1,600 lines; dashboard aggregation exceeds documented “narrow” scope.

**Action:** Extract `DashboardDataLoader` or domain aggregator; leave permissions + availability on `HealthRepository`.

### Compose UI tests

**Problem:** No automated UI regression for scaffold/navigation.

**Action:** Add tests for `MetricDetailScaffold` period controls and one golden-path screen (e.g. sleep week view).

### Split broad shared UI files

**Problem:** `MetricCard.kt` mixes `MetricCard`, `SectionHeader`, `TimeRangeSelector`, etc.

**Action:** Split by component when file growth slows reviews.

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
| Dashboard | [ ] | [ ] | [ ] | [x] | LoadDashboardDayUseCase |

### Cross-cutting program phases

| Phase | Item | Status |
|-------|------|--------|
| 4 | `@Immutable` on all `*UiState` | [x] |
| 4 | `ScreenError` rollout complete | [x] |
| 5 | `domain/query/*PeriodData` | [x] |
| 6 | `LoadHeartPeriodUseCase`, `LoadDashboardDayUseCase` | [x] |
| 7 | Repository interfaces (Sleep, Activity, Health) | [x] |
| 8 | `DashboardAggregator` + slim `HealthRepository` | [x] |
| 9 | Compose UI tests for `MetricDetailScaffold` | [x] |

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
