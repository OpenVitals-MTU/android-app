# Testability and Production Readiness

## Test inventory

The project has **~92 JVM unit tests** under `app/src/test/kotlin`, including:

| Category | Examples |
|----------|----------|
| ViewModels | `SleepViewModelTest`, `HydrationViewModelTest`, `DashboardViewModelTest`, `ActivitiesViewModelTest` |
| Repositories | `ActivityRepositoryTest`, `HeartRepositoryTest`, `CycleRepositoryTest`, `HealthRepositoryDashboardTest` |
| Domain insights | `DailyGoalsTest`, `SleepScoreDateTest`, `CrossMetricInsightsTest`, `DailyReadinessTest` |
| Period math | `PeriodLoadQueryTest`, `PeriodSelectionTest`, `PeriodTitleTest` |
| Health Connect | `HealthConnectFeatureTest`, `ActivityHealthReaderTest`, `HydrationHealthReaderTest` |
| Cache | `MetricSummaryCacheStoreTest`, `PeriodResultCodecsTest` |
| Manual entry | `HydrationEntryViewModelTest`, `ActivityEntryViewModelTest` |

Infrastructure:

- `MainDispatcherRule` — replaces Main dispatcher for coroutine tests
- MockK for repository mocking
- `runTest` from `kotlinx-coroutines-test`

## What makes ViewModels testable

1. **Test constructor** with injectable repository mocks and initial state
2. **`LoadCoordinator`** — tests can assert stale loads are ignored after rapid navigation
3. **`PeriodSelectionDriver`** — period behavior tested independently in `PeriodSelectionTest`
4. **No Android framework in ViewModel** — except `SavedStateHandle` for nav args (mockable)

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
| WorkManager | Apple Health import | Long-running user-visible jobs |
| Hilt singleton graph | `di/AppModule.kt` | Room, dispatchers, formatters |

## Scalability strengths

- **Feature-first packages** — teams can own `features/sleep`, `features/activity`, etc.
- **Shared period shell** — new metrics do not copy navigator/range UI
- **Query-oriented repositories** — one `loadXPeriod` replaces many ad hoc overloads
- **Documentation** — `architecture.md`, `feature-playbook.md`, `AGENTS.md` reduce architectural drift

## Scalability risks

### Monolithic screen files

Largest `*Screen.kt` files (approximate line counts):

| File | Lines |
|------|-------|
| `ActivityRecordingScreen.kt` | ~2,082 |
| `BodyScreen.kt` | ~1,116 |
| `ActivityScreen.kt` | ~958 |
| `SleepScreen.kt` | ~936 |
| `HeartScreen.kt` | ~920 |
| `HydrationScreen.kt` | ~872 |
| `DashboardScreen.kt` | ~849 |

Large files increase merge conflicts, review time, and accidental coupling.

### No Compose UI tests observed

Business logic is well covered at JVM layer. Navigation, scaffold integration, and visual regressions rely on manual QA.

**Optional addition:** Robolectric or Compose UI tests for `MetricDetailScaffold` period navigation and one golden-path screen.

### Concrete dependency graph

Every repository is a Hilt `@Singleton` concrete class. Fine for single-module apps; becomes friction if multiple teams need shared contracts.

### `DashboardViewModel` deferred loading

Sophisticated coalescing (`DeferredDashboardLoadContext`, mutex, widget tokens) is powerful but difficult to test exhaustively. Document invariants when changing dashboard load behavior.

## Recommendations for new code

### Always add

- ViewModel tests for: initial load, error path, period navigation, stale-load cancellation
- Repository tests when adding a new `loadXPeriod` bundle or permission guard

### Prefer

- Pure functions in `domain/insights` with dedicated tests
- `DispatcherProvider` when doing CPU work in ViewModels
- Test constructors on new `@HiltViewModel` classes

### Avoid

- Loading data in composable `LaunchedEffect` for feature screens
- Static `LocalDate.now()` in test-sensitive logic without injection
- Untested permission-edge cases in new repositories

## CI and verification

See [development.md](../development.md) for local build and CI commands. Analysis does not replace running `./gradlew test` before large refactors.
