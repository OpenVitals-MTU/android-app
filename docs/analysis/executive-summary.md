# Executive Summary

OpenVitals is a mature, single-module health app with a **documented target architecture** that aligns well with MVVM + Repository. A **P0–P3 gap-closure program** (June 2026) brought period-detail screens, dashboard, and cross-cutting boundaries much closer to that target without a multi-module split or MVI migration.

## Grades at a glance

| Area | Grade | Notes |
|------|-------|-------|
| MVVM + Repository | **Strong** | Feature repositories + interfaces; `HealthRepository` narrowed to permissions/availability |
| UI / business separation | **Strong** | Presentation mappers + `*DisplayState`; metric routes are thin composables |
| Testability | **Strong** | ~100+ JVM tests, use-case tests, repository interfaces; Compose UI pilot added |
| Error handling / null safety | **Good** | Sealed `ScreenError` on period-detail screens; Kotlin null safety used well |
| Project structure | **Strong** | Feature-first packages, shared shell, split screen files |
| Compose performance | **Good** | `@Immutable` on `*UiState`; granular collection pilot on Dashboard + Heart |
| Clean Architecture | **Good (pragmatic)** | Use cases for heavy loads; `domain/query` period DTOs; not full ceremony |

## What is working well

- **Feature-first organization** under `features/<metric>/` with shared shell in `ui/components`
- **Consistent MVVM**: `@Immutable` `UiState` data classes, `StateFlow`, Hilt `@HiltViewModel`
- **Repository interfaces** in `data/repository/contract/` with `*Impl` classes bound in Hilt (`Sleep`, `Activity`, `Health`, `Heart`, `Hydration`, `Body`)
- **Narrow `HealthRepository`** (~55 lines) — permissions and availability only; dashboard reads via `DashboardDataLoader` + `DashboardAggregator`
- **`PeriodSelectionDriver`** centralizes Day/Week/Month/Year navigation across detail screens
- **`LoadCoordinator`** cancels stale loads when period or date changes quickly
- **Use cases** for complex orchestration: `LoadDashboardDayUseCase`, `LoadHeartPeriodUseCase`, `LoadSleepPeriodUseCase`
- **Presentation mappers** (`SleepPresentationMapper`, `DashboardPresentationMapper`, etc.) prepare display-ready state off the main thread
- **`MetricDetailScaffold`** + `WithHealthConnectFeatureScreen` provide reusable period and permission UX
- **Domain layer** is mostly pure Kotlin (`domain/model`, `domain/insights`, `domain/query`)
- **Unit test culture**: ViewModels, mappers, use cases, repositories, and domain insights are tested
- **Project docs** (`architecture.md`, `feature-playbook.md`, `AGENTS.md`) match the code direction

## Completed in P0–P3 program

See [refactor-backlog.md](refactor-backlog.md) migration tracker for full checklist.

1. **Split oversized screen files** — metric routes under ~150 lines; content in sibling composables (e.g. `DashboardContent`, `SleepPeriodContent`, `HeartMetricContent`)
2. **Moved derived display state into ViewModels/mappers** — screens render `display` payloads; no `domain/insights` imports in routes
3. **`@Immutable` on `*UiState`** — all feature screen state classes annotated
4. **Sealed `ScreenError`** — rolled out to period-detail and dashboard screens
5. **Granular state collection** — pilot on `DashboardScreen` and `HeartScreen` via `remember(viewModel) { uiState.map { … } }`
6. **Use cases + repository interfaces** — see cross-cutting phases 6–7 in backlog
7. **`domain/query/*PeriodData`** — period result DTOs moved out of `data.repository`
8. **Slim `HealthRepository`** — dashboard aggregation extracted to `DashboardDataLoader` / `DashboardAggregator`
9. **Compose UI test pilot** — `SleepScreenWeekTest` + `compileDebugAndroidTestKotlin` in `verifyLocalApp`
10. **Split `MetricCard.kt`** — `SectionHeader`, `TimeRangeSelector` extracted

## Remaining gaps (deferred or incremental)

1. **Large non-metric screens** — manual-entry forms, settings, achievements, and activity recording setup still exceed ~400 lines in places
2. **Repository interfaces** — `NutritionRepository`, `MindfulnessRepository`, `CycleRepository`, `VitalsRepository` remain concrete-only (add when touching those boundaries)
3. **Granular collection** — only piloted on Dashboard + Heart; extend if profiling shows jank elsewhere
4. **Compose UI coverage** — one golden-path androidTest; expand scaffold/navigation tests incrementally
5. **Multi-module split, MVI, universal charts, Room HC mirror** — explicitly deferred per [architecture.md](../architecture.md)

## Reference implementations

| Pattern | Best example | Notes |
|---------|--------------|-------|
| Period detail ViewModel | `SleepViewModel`, `HydrationViewModel` | `LoadSleepPeriodUseCase` for cross-repo orchestration |
| Presentation mapper | `SleepPresentationMapper`, `DashboardPresentationMapper` | Unit-tested; builds `*DisplayState` |
| Feature repository | `SleepRepository` (interface) | Permission guard + period bundle |
| App-level repository | `HealthRepository` (interface) | Permissions/availability only |
| Shared shell | `MetricDetailScaffold` | Period controls + error slot |
| Thin route | `SleepScreen.kt`, `DashboardScreen.kt` | Delegate to content/section composables |
| Use case | `LoadDashboardDayUseCase` | Dashboard load orchestration |
| ViewModel tests | `SleepViewModelTest`, `DashboardViewModelTest` | Stale-load + period navigation |

## Related docs

- [refactor-backlog.md](refactor-backlog.md) — prioritized list and migration tracker
- [clean-architecture-refactor.md](clean-architecture-refactor.md) — layer mapping and phased plan status
- [architecture.md](../architecture.md) — source of truth for new work
