# ViewModel, StateFlow, and UI Separation

## State exposure pattern

All major screens follow the same contract:

```kotlin
private val _uiState = MutableStateFlow(XxxUiState(/* initial */))
val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()
```

Screens collect with lifecycle awareness:

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

This avoids collection when the screen is stopped and is the recommended Compose + ViewModel integration.

## UiState design

### Typical fields

| Field | Purpose |
|-------|---------|
| `isLoading` | Pull-to-refresh and initial load |
| `selectedRange` | `TimeRange` (Day / Week / Month / Year) |
| `selectedDate` | Anchor date for period |
| `weekPeriodMode` | Monday–Sunday vs. rolling week |
| `error` / `errorMessage` | User-visible failure (currently `String?`) |
| Payload lists | Sessions, samples, entries, summaries |

### Preference wiring

Detail ViewModels use a **dual constructor** pattern:

1. **Hilt `@Inject constructor`** — reads initial range/goals from `PreferencesRepository`, wires `onRangeSelected` to persist
2. **Package/test constructor** — accepts `initialRange`, flows, and callbacks for unit tests without Hilt

Example (`SleepViewModel`):

```kotlin
@HiltViewModel
class SleepViewModel(
    private val repository: SleepRepository,
    // ...
    private val onRangeSelected: (TimeRange) -> Unit = {},
) : ViewModel() {

    @Inject
    constructor(
        repository: SleepRepository,
        heartRepository: HeartRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        repository = repository,
        initialRange = preferencesRepository.timeRangeFor(PeriodRangePreferenceKey.SLEEP),
        onRangeSelected = { range ->
            preferencesRepository.setTimeRangeFor(PeriodRangePreferenceKey.SLEEP, range)
        },
    )
}
```

Keep this pattern for any new period-based screen.

## ViewModel responsibilities

ViewModels **should**:

- Own loading, range, and date state
- Use `PeriodSelectionDriver` for period math
- Call repositories and combine results
- Apply `LoadCoordinator` for concurrent load safety
- Expose UI-ready data (sorted lists, counts, flags)
- React to preference flows with `distinctUntilChanged().onEach { }.launchIn(viewModelScope)`

ViewModels **should not**:

- Hold `Context` or Compose state
- Contain large formatting blocks (prefer `core/presentation` formatters)
- Duplicate period window calculation (use `PeriodLoadQuery`)
- Mirror raw Health Connect record types in `UiState`

## Screen responsibilities

Screens **should**:

- Collect `uiState` and pass primitives/callbacks to scaffold and sections
- Use `MetricDetailScaffold` for period shell
- Use `WithHealthConnectFeatureScreen` for Health Connect gates and sync UX
- Wire navigation callbacks (`onOpenSleepSession`, etc.)
- Use `LifecycleEventEffect(ON_RESUME)` to call `resumeCurrentPeriod()` where needed

Screens **should not**:

- Launch coroutines for data loading (except truly UI-local work like permission launchers)
- Reimplement period comparison, sleep scoring, or chart point building at scale

## Separation gap: derivation in Compose

Several large screens still compute display data in composables with `remember { }`, for example `SleepScreen`:

```kotlin
val dailySessions = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
    sleepSessionsForRange(sessions = state.sessions, /* ... */)
}
val dailySummary = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
    dailySleepSummary(/* ... */)
}
val durationPoints = remember(state.sessions, selectedPeriod, state.sleepRangeMode) {
    sleepDurationPoints(/* ... */)
}
```

The [feature playbook](../feature-playbook.md) states:

> Put expensive derived display values in the ViewModel state, not in composable getters.

### Why this matters

1. **Testability** — derived values are not covered by ViewModel tests
2. **Main thread** — first composition after state change runs domain logic on the UI thread
3. **Recomposition** — many `remember` blocks still rerun when parent state changes broadly

### Target pattern

```kotlin
data class SleepUiState(
    // ...
    val dailySummary: DailySleepSummary? = null,
    val durationPoints: List<DurationPoint> = emptyList(),
    val overviewDays: List<SleepOverviewDay> = emptyList(),
)

// In ViewModel load() success path, on dispatchers.default:
val derived = withContext(dispatchers.default) {
    buildSleepDisplayState(sessions, query, sleepRangeMode)
}
_uiState.value = _uiState.value.copy(/* raw + derived */)
```

Hydration, nutrition, and parts of heart/body already move further in this direction — use them as templates when refactoring sleep/activity screens.

## Coarse-grained UiState

`HeartUiState` carries many list fields (samples, resting HR, HRV, vitals types, baselines). Any `copy()` update can invalidate the entire screen subtree.

**Mitigations:**

- Split into nested immutable types (`HeartPeriodPayload`, `HeartDayPayload`)
- Expose secondary `StateFlow`s for independent sections (advanced)
- Prepare section-specific display models in the ViewModel so composables take small parameter lists

## DashboardViewModel complexity

`DashboardViewModel` combines:

- Day navigation and pinned-past-day behavior
- Deferred widget loading and coalescing
- Widget editing and ordering
- Permission prompt state
- Multi-repository refresh and delete

It is functionally correct but harder to test than period-detail ViewModels. Long term, extract:

- `DashboardWidgetLoader` or use case for deferred loads
- Widget edit state into a smaller sub-state or child ViewModel (only if product needs it)

## Action naming convention

Use imperative verbs matching user intent:

| Action | ViewModel method |
|--------|------------------|
| Pull to refresh | `load()` or `load(RefreshMode.FORCE)` |
| Change range chip | `selectRange(TimeRange)` |
| Period arrows | `previousPeriod()`, `nextPeriod()` |
| Calendar pick | `selectDate(LocalDate)` |
| Return from background | `resumeCurrentPeriod(refreshCurrent = true)` |

Pass method references to scaffold where possible: `onRefresh = viewModel::load`.
