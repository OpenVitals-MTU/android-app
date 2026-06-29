# Executive Summary

OpenVitals is a mature, single-module health app with a **documented target architecture** that already aligns well with MVVM + Repository. The codebase is **production-oriented** but sits between pragmatic Android layering and full Clean Architecture.

## Grades at a glance

| Area | Grade | Notes |
|------|-------|-------|
| MVVM + Repository | **Strong** | Consistent `ViewModel` + `StateFlow` + feature repositories |
| UI / business separation | **Good, uneven** | ViewModels own orchestration; some screens still derive heavily in Compose |
| Testability | **Good** | ~92 unit tests, test constructors, MockK; no repository abstractions |
| Error handling / null safety | **Adequate** | `runCatching` + `String?` errors; Kotlin null safety used well |
| Project structure | **Strong** | Feature-first packages, shared shell, clear docs |
| Compose performance | **Mixed** | `collectAsStateWithLifecycle` + `remember` used; large monolithic screens hurt recomposition |
| Clean Architecture | **Partial** | Logical layers exist; not use-case/domain-interface driven |

## What is working well

- **Feature-first organization** under `features/<metric>/` with shared shell in `ui/components`
- **Consistent MVVM**: `UiState` data classes, `MutableStateFlow` / `StateFlow`, Hilt `@HiltViewModel`
- **Feature repositories** (`SleepRepository`, `HeartRepository`, etc.) that guard permissions and hide Health Connect
- **`PeriodSelectionDriver`** centralizes Day/Week/Month/Year navigation across detail screens
- **`LoadCoordinator`** cancels stale loads when period or date changes quickly
- **`MetricDetailScaffold`** + `WithHealthConnectFeatureScreen` provide reusable period and permission UX
- **Domain layer** is mostly pure Kotlin (`domain/model`, `domain/insights`)
- **Unit test culture**: ViewModels, repositories, domain insights, and period math are tested
- **Project docs** (`architecture.md`, `feature-playbook.md`, `AGENTS.md`) match the code direction

## Main technical debt

1. **Oversized screen files** — `ActivityRecordingScreen.kt` (~2,000+ lines), `BodyScreen.kt`, `SleepScreen.kt`, `HeartScreen.kt`, `HydrationScreen.kt`
2. **Business derivation in Compose** — especially `SleepScreen` and similar screens that compute chart/summary data in `remember { }` blocks instead of ViewModel state
3. **No repository interfaces** — concrete singletons only; tests rely on MockK
4. **`HealthRepository` is still very large** despite being documented as narrow (dashboard aggregation lives there)
5. **Unstructured errors** — `String?` on `UiState` instead of sealed error types
6. **No `@Immutable` / `@Stable`** on `UiState` data classes for Compose skip optimization
7. **Large monolithic `UiState`** objects (e.g. `HeartUiState`) cause coarse-grained recomposition

## Highest-value improvements

These are **not** a multi-module split or MVI migration:

1. Move derived display state from large screens into ViewModels (on `Default` dispatcher where expensive)
2. Split route composables from section/card/chart files
3. Add `@Immutable` to `*UiState` data classes
4. Introduce a small sealed `ScreenError` hierarchy for user-facing errors
5. Optionally add use cases and repository interfaces at the busiest boundaries (dashboard, heart)

See [refactor-backlog.md](refactor-backlog.md) for a prioritized list.

## Reference implementations

| Pattern | Best example | Known outlier |
|---------|--------------|---------------|
| Period detail ViewModel | `SleepViewModel`, `HydrationViewModel` | `DashboardViewModel` (complex orchestration) |
| Feature repository | `SleepRepository` | `HealthRepository` (too broad) |
| Shared shell | `MetricDetailScaffold` | — |
| Screen composition | `BodyCards.kt`, `ActivitiesOverviewSections.kt` | `SleepScreen.kt` (derivation in UI) |
| ViewModel tests | `SleepViewModelTest` | — |
