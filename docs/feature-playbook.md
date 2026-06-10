# Feature Playbook

Follow this checklist when adding or extending a metric screen.

## 1. Define The Contract

- Keep the screen state in the feature package.
- Put expensive derived display values in the ViewModel state, not in composable getters.
- Keep metric-specific formatting local unless more than one feature needs it.

## 2. Use Shared Period State

- Use `TimeRange`, `DatePeriod`, `PeriodLoadQuery`, and `PeriodSelectionDriver` from `core/period`.
- Support `Day / Week / Month / Year`.
- Clamp future navigation to the current period.
- Add a `PeriodRangePreferenceKey` when the screen needs a remembered range.

## 3. Use Feature-Oriented Repository APIs

- Prefer bundled period APIs that return current, previous, and baseline data from one public call.
- Keep Health Connect permissions and record types below the repository layer.
- Keep granular APIs only for real entry-list/detail reads.

## 4. Wire Through Hilt

- Annotate screen ViewModels with `@HiltViewModel`.
- Use constructor injection for repositories and services.
- Use `SavedStateHandle` for route arguments.
- Keep direct constructors usable in unit tests when a screen needs custom initial state.

## 5. Keep UI Responsibilities Clear

- Use `MetricDetailScaffold` for the shell.
- Keep charts, rows, and cards inside the feature package.
- Split route/container/content files when a screen becomes hard to scan.

## 6. Update Tests And Docs

- Add or update ViewModel tests for period navigation and stale-load behavior.
- Add repository tests when introducing a new bundled query.
- Update architecture docs if the feature changes a shared pattern.
