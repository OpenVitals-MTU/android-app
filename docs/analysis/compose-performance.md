# Compose UI and Recomposition Performance

## Current practices (good)

### Lifecycle-aware state collection

Screens use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose`:

```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

This stops collecting when the lifecycle is below `STARTED`, reducing wasted work.

### `remember` with explicit keys

Chart and period components cache expensive derived values:

```kotlin
val axisDates = remember(period) { datesInPeriod(period) }
val cells = remember(values, period) { periodMonthHeatmapCells(values, period) }
```

`SleepScreen` uses many keyed `remember` blocks for sessions, summaries, and chart points.

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

## Performance concerns

### 1. Whole-`UiState` collection

Any field change on `uiState` recomposes the entire screen composable subtree. Large states amplify cost:

- `HeartUiState` — dozens of list fields
- `DashboardUiState` — widgets, goals, permissions, deferred load tokens

**Mitigations:**

```kotlin
// Option A: map to a slice
val isLoading by viewModel.uiState
    .map { it.isLoading }
    .collectAsStateWithLifecycle(initialValue = true)

// Option B: nested state in UiState
data class HeartUiState(
    val period: PeriodUiState = PeriodUiState(),
    val heartPayload: HeartPayload = HeartPayload(),
)

// Option C: separate StateFlows (use sparingly)
```

### 2. Missing stability annotations

No `@Immutable` or `@Stable` annotations were found on `UiState` data classes. Compose cannot skip recomposition as aggressively for unknown stability.

**Low-cost fix:**

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class SleepUiState(
    val isLoading: Boolean = true,
    val sessions: List<SleepData> = emptyList(),
    // ...
)
```

Apply to `UiState` and feature display models passed deep into charts.

### 3. Heavy derivation on main thread

Even with `remember`, the **first** computation after a relevant state change runs during composition on the main thread:

- `calculateSleepScoreForDate`
- `periodComparison`, `personalBaselineInsight`
- `sleepDurationPoints`, list filtering/sorting

**Fix:** compute in ViewModel inside `withContext(dispatchers.default)` and store results in `UiState`. See [viewmodel-stateflow.md](viewmodel-stateflow.md).

### 4. Monolithic composables

`HydrationScreen` (~872 lines) combines:

- Notification permission launcher state
- Reminder toggles
- `MetricDetailScaffold`
- Multiple chart and list sections

A state change at the top forces recomposition of the entire function unless split into child composables with minimal parameters.

**Fix:**

```kotlin
@Composable
fun HydrationScreen(viewModel: HydrationViewModel, /* ... */) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HydrationScreenContent(
        state = state,
        onRefresh = viewModel::load,
        // ...
    )
}

@Composable
private fun HydrationDaySection(
    summary: HydrationDaySummary,
    unitFormatter: UnitFormatter,
) { /* only recomposes when summary changes */ }
```

### 5. Lists without stable keys

Use `key(item.id)` in `LazyColumn` items for sessions, workouts, and entries to preserve item state and help Compose reuse nodes.

### 6. `LocalDate.now()` in scaffold

`MetricDetailScaffold` calls `LocalDate.now()` during composition for period capping. Minor cost; if used in equality-sensitive child composables, pass `today: LocalDate` from ViewModel or `remember { LocalDate.now() }` once per screen.

## Recomposition checklist for new screens

- [ ] Route composable under ~150 lines; delegate to sections
- [ ] `@Immutable` on `UiState` and display DTOs
- [ ] Expensive lists and insights prepared in ViewModel
- [ ] Section composables take primitives or small immutable types, not full `ViewModel`
- [ ] `key(id)` on dynamic lazy list items
- [ ] `collectAsStateWithLifecycle` (not raw `collectAsState`)
- [ ] Avoid creating new lambdas in hot paths; use method references or `remember(onClick) { { vm.action() } }`
- [ ] Chart data classes are immutable lists, not mutable collections mutated in place

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
  sleepPeriodContent(period, state, /* ... */)
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
3. Compare before/after moving derivation from `SleepScreen` into `SleepViewModel`

## Related docs

- [viewmodel-stateflow.md](viewmodel-stateflow.md) — move derivation out of composables
- [project-structure.md](project-structure.md) — split large screen files
- [refactor-backlog.md](refactor-backlog.md) — prioritized UI performance work
