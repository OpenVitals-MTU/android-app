# Compose UI and Recomposition Performance

## Current practices (good)

### Lifecycle-aware state collection

Screens use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`:

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

This stops collecting when the lifecycle is below `STARTED`, reducing wasted work.

### Granular collection (pilot)

On hot screens where profiling showed coarse recomposition risk, state is collected in slices wrapped in `remember(viewModel)` (required by `FlowOperatorInvokedInComposition` lint):

```kotlin
val isLoading by remember(viewModel) { uiState.map { it.isLoading } }
    .collectAsStateWithLifecycle(initialValue = true)
val display by remember(viewModel) { uiState.map { it.display } }
    .collectAsStateWithLifecycle(initialValue = null)
```

**Pilot screens:** `DashboardScreen`, `HeartScreen`. Extend to other large states only if Layout Inspector shows unnecessary recompositions.

### `@Immutable` on screen state

All feature `*UiState` data classes and display DTOs are annotated with `@Immutable` so Compose can skip recomposition more aggressively when unrelated fields change.

### Presentation mappers off main thread

Expensive chart points, summaries, and insights are prepared in ViewModels via `*PresentationMapper` on `DispatcherProvider.default` and stored in `UiState.display`. Route composables no longer run `domain/insights` in `remember { }`.

### `remember` with explicit keys

Chart and period components cache layout-only derived values:

```kotlin
val axisDates = remember(period) { datesInPeriod(period) }
val cells = remember(values, period) { periodMonthHeatmapCells(values, period) }
```

### Lazy lists

`MetricDetailScaffold` uses `LazyColumn` with a `content: LazyListScope.(DatePeriod) -> Unit` slot so off-screen items are not composed eagerly.

### Stable method references

Scaffold callbacks use method references where possible:

```kotlin
onRefresh = viewModel::load,
onSelectRange = viewModel::selectRange,
onPreviousPeriod = viewModel::previousPeriod,
```

### Preference flows in ViewModels

```kotlin
weekPeriodModeFlow
    ?.distinctUntilChanged()
    ?.onEach { /* update and maybe reload */ }
    ?.launchIn(viewModelScope)
```

Avoids redundant reloads when the value is unchanged.

### Section list drag state

`MetricDetailSectionListState` disables pull-to-refresh during section reorder — prevents gesture conflicts without extra recompositions.

## Remaining performance considerations

### 1. Whole-`UiState` collection on most screens

Metric detail routes still collect full `uiState` where slice collection is not yet justified. Large states (`HeartUiState`, pre-mapper legacy fields) can still cause broader recomposition than necessary.

**Mitigations when profiling shows jank:**

- `.map { }.collectAsStateWithLifecycle()` inside `remember(viewModel) { … }`
- Nested state objects in `UiState`
- Separate `StateFlows` (use sparingly)

### 2. Large non-metric composables

Manual-entry forms, settings, and achievements screens can still combine many concerns in one file (~400–800 lines). Splitting into section composables with minimal parameters remains the fix when touching those files.

### 3. Lists without stable keys

Use `key(item.id)` in `LazyColumn` items for sessions, workouts, and entries to preserve item state and help Compose reuse nodes.

### 4. `LocalDate.now()` in scaffold

`MetricDetailScaffold` calls `LocalDate.now()` during composition for period capping. Minor cost; if used in equality-sensitive child composables, pass `today: LocalDate` from ViewModel or `remember { LocalDate.now() }` once per screen.

## Recomposition checklist for new screens

- [x] Route composable under ~150 lines; delegate to sections (metric features)
- [x] `@Immutable` on `UiState` and display DTOs
- [x] Expensive lists and insights prepared in ViewModel / mapper (metric features)
- [ ] Section composables take primitives or small immutable types, not full `ViewModel` (apply per new section)
- [ ] `key(id)` on dynamic lazy list items (verify per list)
- [x] `collectAsStateWithLifecycle` (not raw `collectAsState`)
- [ ] Avoid creating new lambdas in hot paths; use method references or `remember(onClick) { { vm.action() } }`
- [x] Chart data classes are immutable lists, not mutable collections mutated in place

## `MetricDetailScaffold` integration

Pass only what the shell needs:

```kotlin
MetricDetailScaffold(
    isLoading = state.isLoading,
    selectedRange = state.selectedRange,
    selectedDate = state.selectedDate,
    error = state.error,
    onRefresh = viewModel::load,
    onSelectRange = viewModel::selectRange,
    onPreviousPeriod = viewModel::previousPeriod,
    onNextPeriod = viewModel::nextPeriod,
    onSelectDate = viewModel::selectDate,
    weekPeriodMode = state.weekPeriodMode,
    syncPaused = hcUx.syncPaused,
    headerItems = { /* optional header */ },
) { period ->
  sleepPeriodContent(period, state.display, /* ... */)
}
```

Keep the `content` lambda thin — call named `@Composable` extensions on `LazyListScope`.

## Health Connect shell

`WithHealthConnectFeatureScreen` centralizes:

- Access gate
- Sync banner (detail screens often set `showInlineSyncBanner = false` and pass `syncPaused` into scaffold)
- Contextual permission promotion
- Permission launcher

Do not duplicate this wiring per screen — duplicate banners cause extra composition and inconsistent UX.

## Profiling

For measurable regressions:

1. Android Studio Layout Inspector → Compose recomposition counts
2. Macrobenchmark for scroll jank on large lazy lists (optional)
3. Compare before/after granular collection on Dashboard / Heart pilots

## Related docs

- [viewmodel-stateflow.md](viewmodel-stateflow.md) — move derivation out of composables
- [project-structure.md](project-structure.md) — split large screen files
- [refactor-backlog.md](refactor-backlog.md) — migration tracker
