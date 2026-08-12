# Feature Playbook

Follow this checklist when adding or extending a metric screen.

For the recipes that are not "a metric screen" — a settings section, a Room table, a device integration — see [Other Recipes](#other-recipes) at the end.

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
- When a period load becomes expensive, cache derived summaries through the Room summary cache instead of storing raw Health Connect records.

## 4. Wire Through Hilt

- Annotate screen ViewModels with `@HiltViewModel`.
- Use constructor injection for repositories and services.
- Use `SavedStateHandle` for route arguments.
- Keep direct constructors usable in unit tests when a screen needs custom initial state.

## 5. Keep UI Responsibilities Clear

- Use `MetricDetailScaffold` for the shell.
- Keep charts, rows, and cards inside the feature package.
- Split route/container/content files when a screen becomes hard to scan.

## 6. Surface Errors Through `ScreenError`

- Convert throwables with `toScreenError()` / `onScreenError()` from `core/presentation`, never by reading `throwable.message` yourself.
- Do not special-case `SecurityException`. `isPermissionFailure()` already maps it to `ScreenError.PermissionDenied`, which the scaffold renders as a grant affordance instead of red text.
- Pass `screenError` to the scaffold rather than rendering an error block in the feature.

## 7. Update Tests And Docs

- Add or update ViewModel tests for period navigation and stale-load behavior.
- Add repository tests when introducing a new bundled query.
- Update architecture docs if the feature changes a shared pattern.
- If the work closes a gap listed in [test-parity](test-parity/README.md), update that row.

## Sleep Reference Implementation

Use the sleep feature as the template for splitting a period-based detail screen:

| File | Responsibility |
|------|----------------|
| `SleepScreen.kt` | Route only: collect state, Health Connect shell, `MetricDetailScaffold`, delegate to content extensions |
| `SleepDayContent.kt` | `LazyListScope` extensions for `Day` mode (timeline, sessions, education) |
| `SleepPeriodContent.kt` | `LazyListScope` extensions for `Week / Month / Year` (bar chart, drill-down list, sessions) |
| `SleepCharts.kt` | Overview top cards, metric cards, sparklines, and `sleepOverview` list section |
| `SleepSharedSections.kt` | Reusable insight sections (confidence, goal, statistics, target context, HRV, caffeine) |
| `SleepDisplayState.kt` | UI-ready display models produced by the ViewModel |
| `SleepPresentationMapper.kt` | Maps repository/domain data, including optional cross-metric signals, into `SleepDisplayState` |
| `SleepViewModel.kt` | Period selection, loading, and `display` state |
| `SleepCards.kt` | Session timeline and list row composables |

Keep `screenError` on the scaffold and read presentation values from `state.display`, not raw repository models in composables.

## Other Recipes

### Add A Settings Section

A settings section is five edits, and missing any one of them fails the build or leaves a dead card:

1. Add an entry to the `SettingsSection` enum in `features/settings/SettingsSection.kt`, with a `titleRes` and a `summaryRes`.
2. Add the two strings to `values/strings.xml`. Do not add them to any `values-*/strings.xml`; those are Weblate-owned.
3. Add an icon branch to `SettingsSection.icon` in `features/settings/SettingsCards.kt`. It is an exhaustive `when`, so this is compulsory.
4. Add a content branch to the `when` in `features/settings/SettingsScreenContent.kt`. Sections that are a bespoke screen rather than a card list map to `Unit` there (see `WATCHES` and `DEVICE_SYNC`).
5. Add a `Screen` entry in `navigation/Screen.kt`, a `composable` in `navigation/AppNavigationSettingsRoutes.kt`, and the mapping in `settingsSectionRoute`.

Sections hidden outside diagnostics builds are filtered in `SettingsScreenContent` against `BuildConfig.OPENVITALS_DIAGNOSTICS`; follow `DEBUG_DIAGNOSTICS` if the new section is developer-only.

### Add A Room Entity

The database is at version 9. A new entity means:

1. Add the `@Entity` and its DAO under `data/local/<area>/`.
2. Add the entity to the `entities` array in `OpenVitalsDatabase`, add the abstract DAO accessor, and bump `version` to 10.
3. Add a `MIGRATION_9_10` in the companion object that creates the table, and register it in the `addMigrations(...)` call in `di/AppModule.kt`.
4. Give the migration a KDoc saying whether it copies data or only creates the table, and why. Every existing migration does.
5. Prefer a natural composite primary key that makes a re-import idempotent, as `garmin_wellness_samples` does with `(metric, time_millis)`.

Only add a table for data Health Connect cannot hold, or for a derived summary cache. Do not mirror Health Connect records.

### Add A Repository Contract

When a repository is a seam that another subsystem writes through, split it:

1. Declare the interface in `data/repository/contract/`.
2. Put the implementation in `data/repository/` as `<Name>Impl`.
3. `@Binds` it in `di/RepositoryModule.kt`.

Keep the contract free of windowing, aggregation, and interpretation, as `GarminWellnessRepository` is.

### Add A Device Integration

1. Implement the ports in `devices/core`: `DeviceClassifier` and `DeviceScanClassifier` for discovery, `WatchPairingPort` for bonding, `DeviceSyncPort` for sync.
2. Register the classifier where the others are constructed, in `sensors/ble/BleSensorCoordinator`.
3. Take a lease via `withRadioLease(address, RadioLeaseOwner.*)` around every BLE link. Do not invent a fifth owner tag without a reason.
4. Keep protocol code transport-free so it is testable over an in-memory pipe; only the GATT client should touch `android.bluetooth`.
5. Bind the new port in `di/DevicesModule.kt`. Do not add a Hilt module inside `devices/`.
6. Write imported data through `AppleHealthImportRepository.insertImportedRecords` with a deterministic `clientRecordId`, so a re-sync upserts.
