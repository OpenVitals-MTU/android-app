# Feature Playbook

Follow this checklist when adding or extending a metric screen.

Background and rationale: [architecture.md](architecture.md). Hard invariants that have already been broken once: [AGENTS.md](../../AGENTS.md). This page is the checklist; it does not repeat either.

## 1. Define The Contract

- Put the screen state in the feature directory: a `freezed` class named `<Feature>State`, in `<feature>_notifier.dart`, next to its `Notifier`.
- Hold the **raw** payload (a `*PeriodLoadResult`, or a `*PeriodData` from `lib/domain/query/`) plus `isLoading`, a `ScreenError?`, the selection the scaffold reports back, and any feature-owned preference the screen mutates (a goal, a threshold).
- Derive the display model in the screen, not in the notifier — that is the current house style across every feature (see the sleep table below, and *Known Seams* §1 in architecture.md). **Exception, and it matters:** if a derived value requires sorting, grouping or scanning a list, or is recomputed on every rebuild of a scrolling screen, move it into the notifier's state.
- Keep metric-specific formatting in the feature. Shared, repository-free formatting goes in `lib/core/presentation/`.
- Every user-visible string goes through `AppLocalizations` — add it to `lib/l10n/app_en.arb` and run `flutter gen-l10n`.

## 2. Use Shared Period State

- Use `TimeRange`, `DatePeriod`, `PeriodSelection`, `PeriodLoadQuery` and `PeriodWindows` from `lib/core/period/`. Never write a screen-local period helper.
- Support `Day / Week / Month / Year`. Forward navigation is capped at the current period, and the date picker opens from the navigator title.
- **The scaffold owns the period, not your notifier.** `MetricDetailScaffold` holds the `PeriodSelectionDriver`, seeds it from the persisted range, and hands you a `PeriodSelection` through `onSelectionChanged`. Do not construct a `PeriodSelectionDriver` in a notifier.
- Add a `PeriodRangePreferenceKey` for the new screen and pass it as `rangePreferenceKey`. The scaffold reads and writes the remembered range through it; you do not touch `PreferencesRepository` for this. Only the range persists — the selected date is screen state.

## 3. Use Feature-Oriented Repository APIs

- Add the method to the `contract/` class and implement it in `impl/`. A feature imports the contract, never the impl and never `lib/health/native/`.
- Prefer one bundled period call returning current, previous and baseline data over several granular ones. Take a `DatePeriod` or a `PeriodLoadQuery`, not another `loadX(start, end)` overload.
- Guard the required permissions inside the repository. Health Connect permission strings and record types stay below the repository layer.
- Reach for `lib/domain/usecase/` only when a screen genuinely needs two repositories combined (as sleep does, for the HRV correlation). Do not add one per screen out of symmetry.
- There is **no summary cache** to write into. drift holds one table (`beverages`). If a period load becomes genuinely expensive, follow `BodyEnergyTimelineCacheStore`: a versioned envelope keyed by a signature that includes the permission fingerprint, so a permission change invalidates it.

## 4. Wire Through Riverpod

Riverpod replaces Hilt; there is no annotation processor and no component.

- Declare the notifier as a plain `NotifierProvider` at the bottom of the notifier file:
  ```dart
  final sleepNotifierProvider =
      NotifierProvider<SleepNotifier, SleepState>(SleepNotifier.new);
  ```
  For a screen parameterized by a route argument (a metric id, an entry id), build the provider per argument so stacked routes stay independent — see `HeartMetricNotifier(metric)` and `SleepDetailNotifier(sleepId)`.
- Take dependencies from `ref.read(...)` inside the notifier, not from constructor arguments reaching into globals. Repository and service providers live in `lib/di/providers.dart`; app-shell preference providers (`unitFormatterProvider`, `unitSystemProvider`, `weekPeriodModeProvider`, …) live in `lib/state/app_providers.dart`. Register anything new in whichever of the two it belongs to.
- Route arguments come from go_router (`state.pathParameters` in `lib/navigation/app_router.dart`), not from a `SavedStateHandle` analogue. Add the path and its typed `…Location(...)` helper to `lib/navigation/app_routes.dart`.
- Guard every load against staleness with a monotonic `_generation` counter, and check `ref.mounted` after every `await` — copy the shape in `sleep_notifier.dart`.
- **The test seam is provider overrides.** Everything a test needs to fake is a provider:
  ```dart
  ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),   // required — throws otherwise
      unitSystemProvider.overrideWithValue(UnitSystem.metric), // required if any unit is shown
      sleepRepositoryProvider.overrideWithValue(FakeSleepRepository()),
      grantedHealthPermissionsProvider.overrideWith((ref) async => {HcPermissions.readSleep}),
    ],
    child: ...,
  )
  ```
  `unitSystemProvider` defaults to the **host locale** — leave it unpinned and the test asserts different numbers on different machines. A health-gated screen also needs `healthConnectAvailabilityProvider` and `grantedHealthPermissionsProvider`, or `HealthConnectGate` replaces your screen with a permission prompt.
- After editing an annotated class (`freezed`, `json_serializable`, `drift`), regenerate:
  ```bash
  dart run build_runner build --delete-conflicting-outputs
  ```

## 5. Keep UI Responsibilities Clear

- Use `MetricDetailScaffold` for the shell, inside a `HealthConnectGate` with the screen's `requiredPermissions`. Set `showInlineSyncBanner: false` on the gate so the sync banner is not drawn twice.
- Register the screen's sections with `MetricDetailSectionId` + `OrderedMetricDetailSections` so the user can reorder them, and expose the edit toggle in the app bar.
- Keep charts, rows and cards inside the feature directory. Use `lib/ui/charts/` only for "a value per day" — `PeriodHistoryChart` and friends carry no metric semantics, and nothing that does belongs there.
- Split the file when a screen gets hard to scan: route widget → content widget → sections → cards → chart.

## 6. Update Tests And Docs

- Add a widget test for the screen (period navigation is exercised through `MetricDetailScaffold`, since the scaffold owns the driver — a notifier test drives `load(PeriodSelection(...))` directly instead).
- Every widget-test `MaterialApp` needs `localizationsDelegates` and `supportedLocales`, or you get `Null check operator used on a null value` with a stack pointing at the screen. That is a harness bug, not a screen bug.
- Add notifier tests for stale-load behaviour and error mapping, and repository tests for a new bundled query.
- Update [architecture.md](architecture.md) if the feature changes a shared pattern, and [docs/features/feature-map.md](../features/feature-map.md) with the new route.
- Before pushing: `flutter test`, `flutter analyze lib test`, `dart run tool/verify_l10n.dart`, and `flutter gen-l10n && git diff --exit-code lib/l10n`.

## Sleep Reference Implementation

The sleep feature is the template for a period-based detail screen. These are the files that actually exist in `lib/features/sleep/`:

| File | Responsibility |
|------|----------------|
| `sleep_screen.dart` | Route widget: watch the notifier, wrap in `HealthConnectGate`, hand `MetricDetailScaffold` a `rangePreferenceKey` and an `onSelectionChanged`, compose the ordered sections |
| `sleep_notifier.dart` | `SleepState` (`freezed`) + `SleepNotifier` + `sleepNotifierProvider`: loading, the raw `SleepPeriodLoadResult`, the sleep-hours goal, the `_generation` staleness guard |
| `sleep_presentation.dart` | The presentation mapper — `buildSleepDisplay(...)` turns the raw payload into `SleepDisplay` / `SleepOverviewSummary` / `SleepDurationPoint`. **Called by the screen, not the notifier** |
| `sleep_metric_sections.dart` | The orderable section bodies (statistics, target context, HRV insight, data confidence, session list), each a widget over a `SleepDisplay` |
| `sleep_cards.dart` | Feature cards: session timeline, stage-share, overview tiles, statistics |
| `sleep_schedule_chart.dart` | The week/month clock-aligned, stage-coloured schedule chart (a `CustomPainter`) |
| `sleep_detail_screen.dart` | The single-session route (`/sleep_detail/:sleepId`): summary card, stage breakdown, the stage-lane chart, per-stage event rows |
| `sleep_detail_notifier.dart` | `SleepDetailState` + `SleepDetailNotifier`, built per `sleepId` so stacked detail routes stay independent |
| `sleep_notifier.freezed.dart` | Generated. Never edit by hand |

Two things to notice, because they differ from the Kotlin sleep feature this was ported from:

- **There is no `SleepDisplayState` on the state object.** Kotlin's `SleepViewModel` precomputes `display: SleepDisplayState` and its playbook says to read presentation values from `state.display`. Here the notifier holds `result` (the raw payload) and the screen calls `buildSleepDisplay(...)`. Follow the Dart shape.
- **There is no Day/Period content split.** Kotlin splits `SleepDayContent.kt` / `SleepPeriodContent.kt` / `SleepCharts.kt` / `SleepSharedSections.kt` because `LazyListScope` extension functions are how you contribute list items in Compose. Flutter's `content` slot takes a widget list, so day-vs-period is a branch inside `_SleepContent` in `sleep_screen.dart`, and the sections are widgets in `sleep_metric_sections.dart`. Do not recreate the four-file Kotlin split.
