# Feature Playbook

This is the implementation checklist for adding a new metric feature.

Use this before writing code.

## Goal

A new feature should feel native to the app from day one:

- same period navigation model
- same dashboard-first flow
- clear feature ownership
- minimal duplication

## First Decision: What Kind Of Feature Is It?

Before creating files, classify the feature.

### Type A: Period-based metric detail screen

Use this for metrics like:

- blood pressure
- oxygen saturation
- resting heart rate
- body fat
- hydration details

This is the default path.

### Type B: Session/list feature

Use this for:

- workouts
- sleep sessions
- raw record browsing

These still use the same period shell, but the content is more list-driven.

### Type C: Dashboard-only card

Use this only when there is not yet enough detail UX to justify a full feature screen.

## The Required Shape For New Detail Features

Every new detail feature should include:

1. One route
2. One ViewModel
3. One `UiState`
4. One period-based screen using `MetricDetailScaffold`
5. `Day / Week / Month / Year`
6. Previous/next period navigation
7. Calendar selection
8. Empty state
9. Error state
10. Dashboard entry point if the metric is visible on the dashboard

## File Checklist

For a new feature called `hydration`, the target file shape should look like:

```text
features/hydration/
  HydrationScreen.kt
  HydrationViewModel.kt
```

Optional additions if the feature gets larger:

```text
features/hydration/
  HydrationContract.kt
  HydrationScreen.kt
  HydrationViewModel.kt
  HydrationCharts.kt
  HydrationRows.kt
  HydrationFormatters.kt
```

Keep files split by responsibility, not by arbitrary size.

## Implementation Steps

### 1. Define the feature contract

Create the screen state first.

The state should include:

- `isLoading`
- `selectedRange`
- `selectedDate`
- metric-specific content
- `error`

Add derived values only if they clearly reduce UI complexity.

### 2. Decide the data query model

Prefer a period-based query.

Target shape:

```kotlin
data class DatePeriod(
    val start: LocalDate,
    val end: LocalDate,
)
```

The feature should load data for:

- one selected day for `Day`
- one selected period for `Week / Month / Year`

Avoid inventing feature-specific date navigation rules unless the metric truly requires them.

### 3. Add Health Connect support

**a. Manifest** — add `<uses-permission android:name="android.permission.health.READ_*" />` in `AndroidManifest.xml`.

**b. HealthConnectManager** — add the new record type to `phase2Permissions` (or phase 3/4 if opt-in) and add the read method(s).

**c. Feature repository** — add a load method with a permission guard. Follow the pattern from existing repos (`ActivityRepository`, `HeartRepository`, etc.).

### 4. Add the data model

Add new data classes to the relevant feature-oriented file under `data/model`, such as `ActivityModels.kt`, `SleepModels.kt`, `VitalsModels.kt`, or `NutritionModels.kt`.

If the metric also belongs on the dashboard, add a non-nullable field with a `0` default to `DashboardData` and set it in `HealthRepository.loadDashboard()`.

### 5. Implement the ViewModel

The ViewModel should:

- own selected range/date state
- compute the selected period
- load metric data in parallel with `async`
- expose UI-ready state

The ViewModel should not:

- contain long formatting strings
- know about icon choices or display colors
- duplicate period logic if a shared helper already exists

### 6. Implement the screen

Use `MetricDetailScaffold` from `ui/components/MetricDetailScaffold.kt` as the screen shell. It handles:

- pull-to-refresh
- time range selector
- period navigator + date picker
- error block

Your screen only needs the feature-specific content lambda:

```kotlin
@Composable
fun HydrationScreen(viewModel: HydrationViewModel) {
    val state by viewModel.uiState.collectAsState()

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
    ) { period ->
        // feature-specific items here
    }
}
```

If the feature needs items *before* the time range selector (e.g. category filter chips), use the `headerItems` slot.

Then split the content into:

- `Day` content
- `Week / Month / Year` content
- optional list/breakdown

### 7. Add the dashboard card

Dashboard metrics always appear even when data is 0 — do not gate them with null checks.

Add a non-nullable field to `DashboardData` with default `0` / `0.0`. Add a `MetricCard` to the relevant section of `DashboardScreen`. Add a new accent color to `ui/theme/Color.kt` if needed.

### 8. Register navigation

Update:

- route in `navigation/Screen.kt`
- destination in `navigation/AppNavigation.kt`
- dashboard card `onClick` routing if relevant

### 9. Update docs if the pattern changes

If the feature introduces a better reusable pattern:

- update `docs/architecture.md`
- update `docs/metrics-roadmap.md` to mark the item done

## Reuse Rules

### Reuse these

- `MetricDetailScaffold` for the screen shell
- `periodFor`, `periodTitle`, `periodSubtitle` from `core/period`
- `SectionHeader`, `SourceChip`, `InlineLoading`, `ErrorMessage` from `ui/components/`
- common loading/error/empty patterns

### Keep these feature-specific

- metric-specific charts
- metric-specific rows
- metric-specific summaries
- metric-specific formatting language when domain semantics differ

## Pull Request / Change Checklist

Before finishing a new feature, verify:

- the feature uses `MetricDetailScaffold`
- the screen can navigate backward and forward correctly
- navigation does not move past the current period
- the dashboard card is always visible (defaults to 0, not hidden when null)
- empty/error states exist
- the feature did not introduce another copy of shared period logic
- `docs/metrics-roadmap.md` is updated
