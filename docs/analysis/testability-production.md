# Testability and Production Readiness

## Test inventory

The project has **100+ JVM unit tests** under `app/src/test/kotlin`, including:

| Category | Examples |
|----------|----------|
| ViewModels | `SleepViewModelTest`, `HydrationViewModelTest`, `DashboardViewModelTest`, `HeartViewModelTest` |
| Use cases | `LoadSleepPeriodUseCaseTest`, `LoadHeartPeriodUseCaseTest`, `LoadDashboardDayUseCaseTest` |
| Presentation mappers | `SleepPresentationMapperTest`, `DashboardPresentationMapperTest`, `HeartPresentationMapperTest` |
| Repositories | `ActivityRepositoryTest`, `HeartRepositoryTest`, `DashboardDataLoaderTest`, `CycleRepositoryTest` |
| Domain | `DashboardAggregatorTest`, `DailyGoalsTest`, `SleepScoreDateTest`, `CrossMetricInsightsTest` |
| Period math | `PeriodLoadQueryTest`, `PeriodSelectionTest`, `PeriodTitleTest` |
| Health Connect | `HealthConnectFeatureTest`, `ActivityHealthReaderTest`, `HydrationHealthReaderTest` |
| Cache | `MetricSummaryCacheStoreTest`, `PeriodResultCodecsTest` |
| Manual entry | `HydrationEntryViewModelTest`, `ActivityEntryViewModelTest` |
| Core | `ScreenErrorTest`, `PeriodNavigatorTest` |

Infrastructure:

- `MainDispatcherRule` — replaces Main dispatcher for coroutine tests
- MockK for repository mocking (against interfaces where bound)
- `runTest` from `kotlinx-coroutines-test`

## Compose UI tests

Instrumented tests live under `app/src/androidTest/`:

| Test | Coverage |
|------|----------|
| `SleepScreenWeekTest` | Sleep week period content inside `MetricDetailScaffold` (testTag on week content) |

CI / local verification:

- `verifyLocalApp` includes `:app:compileDebugAndroidTestKotlin` (compile androidTest on every local verify)
- Optional `verifyAndroidTest` runs `connectedDebugAndroidTest` when `ANDROID_SERIAL` is set

Expand UI tests incrementally for scaffold period navigation and other golden-path screens.

## What makes ViewModels testable

1. **Test constructor** with injectable repository mocks and initial state
2. **`LoadCoordinator`** — tests can assert stale loads are ignored after rapid navigation
3. **`PeriodSelectionDriver`** — period behavior tested independently in `PeriodSelectionTest`
4. **Use cases** — orchestration tested without `UiState` (`LoadSleepPeriodUseCaseTest`, etc.)
5. **Presentation mappers** — pure mapping tested without ViewModel
6. **No Android framework in ViewModel** — except `SavedStateHandle` for nav args (mockable)

Example test pattern (`SleepViewModelTest`):

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()

private fun emptyRepo() = mockk<SleepRepository>().also { repo ->
    coEvery { repo.loadSleepPeriod(any(), any()) } returns SleepPeriodData(/* ... */)
}

@Test fun `initial load clears loading`() = runTest {
    val vm = SleepViewModel(emptyRepo())
    assertFalse(vm.uiState.value.isLoading)
}
```

## Production patterns in use

| Pattern | Location | Purpose |
|---------|----------|---------|
| `LoadCoordinator` | `core/performance` | Cancel superseded loads |
| `DispatcherProvider` | `core/performance` | Inject IO/Default for tests |
| `MetricSummaryCacheStore` | `data/cache` | Cache derived summaries with invalidation |
| `RefreshMode` | `domain/model` | Normal vs. force refresh |
| Permission fingerprint | `healthconnect` | Cache key includes granted permissions |
| `HealthConnectScreenUxCoordinator` | `healthconnect` | Central sync/access UX |
| `DashboardDeferredLoadCoordinator` | `features/dashboard` | Coalesce dashboard widget loads |
| WorkManager | Apple Health import | Long-running user-visible jobs |
| Hilt singleton graph | `di/AppModule.kt`, `RepositoryModule.kt` | Room, dispatchers, repository binds |

## Scalability strengths

- **Feature-first packages** — teams can own `features/sleep`, `features/activity`, etc.
- **Shared period shell** — new metrics do not copy navigator/range UI
- **Query-oriented repositories** — one `loadXPeriod` replaces many ad hoc overloads
- **Repository interfaces** — compile-time boundary for top repositories
- **Documentation** — `architecture.md`, `feature-playbook.md`, `AGENTS.md` reduce architectural drift

## Scalability risks

### Large non-metric screen files

Largest `*Screen.kt` files after metric refactor (approximate line counts):

| File | Lines |
|------|-------|
| `MindfulnessEntryScreen.kt` | ~823 |
| `HeartVitalsOverviewScreen.kt` | ~716 |
| `HydrationEntryScreen.kt` | ~689 |
| `AchievementsScreen.kt` | ~556 |
| `SettingsScreen.kt` | ~534 |

Metric detail routes are now thin (e.g. `SleepScreen.kt` ~100, `DashboardScreen.kt` ~147, `HeartScreen.kt` ~290). Remaining large files are mostly manual-entry, settings, or secondary detail flows.

### Limited Compose UI coverage

Business logic is well covered at JVM layer. Navigation, scaffold integration, and visual regressions beyond the sleep-week pilot still rely mostly on manual QA and compile-time androidTest checks.

### `DashboardViewModel` deferred loading

Sophisticated coalescing (`DashboardDeferredLoadCoordinator`, widget tokens) is powerful but difficult to test exhaustively. Document invariants when changing dashboard load behavior. `DashboardViewModelTest` and `DashboardDataLoaderTest` cover core paths.

## Recommendations for new code

### Always add

- ViewModel tests for: initial load, error path, period navigation, stale-load cancellation
- Repository tests when adding a new `loadXPeriod` bundle or permission guard
- Mapper tests when adding non-trivial display derivation

### Prefer

- Pure functions in `domain/insights` with dedicated tests
- `DispatcherProvider` when doing CPU work in ViewModels
- Test constructors on new `@HiltViewModel` classes
- Repository interfaces when introducing or significantly changing a repository

### Avoid

- Loading data in composable `LaunchedEffect` for feature screens
- Static `LocalDate.now()` in test-sensitive logic without injection
- Untested permission-edge cases in new repositories

## CI and verification

See [development.md](../development.md) for local build and CI commands.

Before large refactors, run:

```powershell
.\gradlew.bat verifyLocalApp
```

This runs unit tests, lint, and compiles androidTest sources.

## Related docs

- [refactor-backlog.md](refactor-backlog.md) — migration tracker
- [mvvm-repository.md](mvvm-repository.md) — repository boundaries
