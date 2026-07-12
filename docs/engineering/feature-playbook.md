# Feature Playbook

Follow this checklist when adding or extending a metric screen.

Background and rationale: [architecture.md](architecture.md). Hard invariants that have already been broken once: [AGENTS.md](../../AGENTS.md). This page is the checklist; it does not repeat either.

## 1. Define The Contract

- A feature is two directories: `application/` (the view-model, its state, its display builder) and `presentation/` (the screen, its cards and charts).
- Put the screen state in `application/<feature>_view_model.dart`: a `freezed` class named `<Feature>State`, next to the `<Feature>ViewModel` that owns it.
- The state holds the **raw** payload (a `*PeriodLoadResult`, or a `*PeriodData` from `lib/domain/query/`), the **precomputed** `<Feature>Display`, `isLoading`, a `ScreenError?`, the selection the scaffold reports back, and any feature-owned preference the screen mutates (a goal, a threshold).
- Write the derivation as a **pure top-level function** in `application/<feature>_display.dart`:
  ```dart
  @freezed
  abstract class SleepDisplay with _$SleepDisplay { ... }

  SleepDisplay buildSleepDisplay(SleepPeriodLoadResult result, {...}) { ... }
  ```
  No `ref`, no clock, no I/O, no `BuildContext`, no `AppLocalizations`, no `UnitFormatter`. It takes data and returns data — which is what makes it unit-testable without a widget, and that is the whole point of the seam.
- **The view-model calls it once, at load time.** The screen renders `state.display`. A widget must not sort, fold, group, scan, or unit-convert in a build path. (If the view-model mutates its loaded data *without* reloading — an optimistic delete, a goal nudge — rebuild the display there too.)
- Keep metric-specific *formatting* in the presentation layer: `formatter.minutes(display.totalMinutes)` in the widget is right, because formatting is a view concern and depends on the user's unit system. Shared, repository-free formatting goes in `lib/core/presentation/`.
- Every user-visible string goes through `AppLocalizations` — add it to `lib/l10n/app_en.arb` and run `flutter gen-l10n`.

## 2. Use Shared Period State

- Use `TimeRange`, `DatePeriod`, `PeriodSelection`, `PeriodLoadQuery` and `PeriodWindows` from `lib/core/period/`. Never write a screen-local period helper.
- Support `Day / Week / Month / Year`. Forward navigation is capped at the current period, and the date picker opens from the navigator title.
- **The scaffold owns the period, not your view-model.** `MetricDetailScaffold` holds the `PeriodSelectionDriver`, seeds it from the persisted range, and hands you a `PeriodSelection` through `onSelectionChanged`. Do not construct a `PeriodSelectionDriver` in a view-model.
- Add a `PeriodRangePreferenceKey` for the new screen and pass it as `rangePreferenceKey`. The scaffold reads and writes the remembered range through it; you do not touch `PreferencesRepository` for this. Only the range persists — the selected date is screen state.

## 3. Use Feature-Oriented Repository APIs

- Add the method to the `contract/` class and implement it in `impl/`. A feature imports the contract, never the impl and never `lib/data/source/health/native/`.
- **It returns `Future<Result<T>>`, and it does not throw.** Wrap the impl's body in `runCatching` — that is the single place in the app where an exception becomes an `AppFailure`. Synchronous probes over cached state (a permission-set getter, an `is…Available()`) stay bare: they cannot fail.
- Prefer one bundled period call returning current, previous and baseline data over several granular ones. Take a `DatePeriod` or a `PeriodLoadQuery`, not another `loadX(start, end)` overload.
- Guard the required permissions inside the repository. Health Connect permission strings and record types stay below the repository layer.
- Use `lib/domain/usecase/` when a screen needs repositories combined. Decide each secondary read's strictness deliberately, and say which in the doc comment: a read the screen is *about* short-circuits the load (`flatMap`), and a read that merely *enriches* it degrades to nothing (`getOrNull()`). Getting this backwards is how a failed HRV lookup blanks a sleep screen that loaded fine.
- There is **no summary cache** to write into. drift holds one table (`beverages`). If a period load becomes genuinely expensive, follow `BodyEnergyTimelineCacheStore`: a versioned envelope keyed by a signature that includes the permission fingerprint, so a permission change invalidates it.

## 4. Wire Through Riverpod

Riverpod replaces Hilt; there is no annotation processor and no component. **The Riverpod `Notifier` IS the view-model** — that is the MVVM the Flutter app-architecture guide asks for, in Riverpod's idiom.

- Declare the provider at the bottom of the view-model file (feature providers do **not** go in `lib/di/`):
  ```dart
  final sleepProvider =
      NotifierProvider<SleepViewModel, SleepState>(SleepViewModel.new);
  ```
  For a screen parameterized by a route argument (a metric id, an entry id), build the provider per argument so stacked routes stay independent — see `HeartMetricViewModel(metric)` and `SleepDetailViewModel(sleepId)`.
- Take dependencies from `ref.read(...)` inside the view-model, not from constructor arguments reaching into globals — and never from a widget. A screen that constructs its own dependencies is the bug this refactor removed twice (`ActivityEntryController` was built in `initState` from seven repositories).
- Repository, use-case and service providers live behind the `lib/di/providers.dart` barrel (`data_providers` / `usecase_providers` / `service_providers`); app-shell preference providers (`unitFormatterProvider`, `unitSystemProvider`, `weekPeriodModeProvider`, …) live in `lib/state/app_providers.dart`.
- Route arguments come from go_router (`state.pathParameters` in `lib/navigation/app_router.dart`). Add the path and its typed `…Location(...)` helper to `lib/navigation/app_routes.dart`.
- **The load shape**, copied from `application/mindfulness_view_model.dart`:
  ```dart
  final generation = ++_generation;
  state = state.copyWith(isLoading: true, error: null, ...);
  final result = await ref.read(loadXUseCaseProvider)(query, refreshMode: refreshMode);
  if (!ref.mounted || generation != _generation) return;   // stale or gone
  switch (result) {
    case Ok(:final value):
      state = state.copyWith(isLoading: false, data: value, display: buildXDisplay(value));
    case Err(:final failure):
      state = state.copyWith(isLoading: false, error: failure.toScreenError(fallback: '…'));
  }
  ```
- **A user action that can fail is a command.** Give it a `CommandState<T>` field (`idle / running / success / failure(ScreenError)`) rather than an `isSaving` + `saveCompleted` + `error` triplet — the screen consumes `CommandSuccess` once and hands the command back to `idle`. Two actions that can be in flight at once get two commands. A validation *refusal* is not a command failure: it never ran.
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
- A widget renders and formats. It does not derive, and it does not touch a repository. Ephemeral UI state (a `TextEditingController`, a focus node, an expansion flag, the draft text mid-typing) *does* stay in the widget — that is not business logic.
- Keep charts, rows and cards inside the feature directory. Use `lib/ui/charts/` only for "a value per day" — `PeriodHistoryChart` and friends carry no metric semantics, and nothing that does belongs there.
- Split the file when a screen gets hard to scan: route widget → content widget → sections → cards → chart.

## 6. Update Tests And Docs

- **A pure display test** for `build<Feature>Display`: fixtures in, values out, including the empty period. No widget, no container — this is the cheapest test in the codebase and it covers the maths that used to hide in a build method.
- **A view-model test**: `ProviderContainer` + a fake repository, asserting the display is precomputed on `Ok`, that an `Err` maps to the right `ScreenError`, and that a stale load cannot overwrite the newer one that overtook it.
- **A widget test** for the screen. Period navigation is exercised through `MetricDetailScaffold`, since the scaffold owns the driver.
- Every widget-test `MaterialApp` needs `localizationsDelegates` and `supportedLocales`, or you get `Null check operator used on a null value` with a stack pointing at the screen. That is a harness bug, not a screen bug.
- Repository tests for a new bundled query assert `Ok`/`Err`, not a throw.
- Update [architecture.md](architecture.md) if the feature changes a shared pattern, and [docs/features/feature-map.md](../features/feature-map.md) with the new route.
- Before pushing: `flutter test`, `flutter analyze lib test`, `dart run tool/verify_l10n.dart`, and `flutter gen-l10n && git diff --exit-code lib/l10n`. **Never `dart format`** — this repo predates Dart's "tall" style and it rewrites whole files.

## Sleep Reference Implementation

The sleep feature is the template for a period-based detail screen. These are the files that actually exist in `lib/features/sleep/`:

| File | Responsibility |
|------|----------------|
| `application/sleep_view_model.dart` | `SleepState` (`freezed`) + `SleepViewModel` + `sleepProvider`: loading, the raw `SleepPeriodLoadResult`, the precomputed `SleepDisplay`, the sleep-hours goal, the `_generation` staleness guard |
| `application/sleep_display.dart` | The pure derivation — `buildSleepDisplay(...)` turns the raw payload into `SleepDisplay` (statistics, stage shares, schedule days, chart values, the comparison/baseline/HRV/confidence insights). **Called by the view-model, never by a widget** |
| `presentation/sleep_screen.dart` | Route widget: watch the view-model, wrap in `HealthConnectGate`, hand `MetricDetailScaffold` a `rangePreferenceKey` and an `onSelectionChanged`, compose the ordered sections |
| `presentation/sleep_metric_sections.dart` | The orderable section bodies (statistics, target context, HRV insight, data confidence, session list), each a widget over a `SleepDisplay` |
| `presentation/sleep_cards.dart` | Feature cards: session timeline, stage-share, overview tiles |
| `presentation/sleep_schedule_chart.dart` | The week/month clock-aligned, stage-coloured schedule chart (a `CustomPainter`) |
| `presentation/sleep_detail_screen.dart` | The single-session route (`/sleep_detail/:sleepId`) |
| `application/sleep_detail_view_model.dart` | `SleepDetailState` + `SleepDetailViewModel`, built per `sleepId` so stacked detail routes stay independent |
| `*.freezed.dart` | Generated. Never edit by hand |

One thing to notice, because it differs from the Kotlin sleep feature this was ported from:

- **There is no Day/Period content split.** Kotlin splits `SleepDayContent.kt` / `SleepPeriodContent.kt` / `SleepCharts.kt` / `SleepSharedSections.kt` because `LazyListScope` extension functions are how you contribute list items in Compose. Flutter's `content` slot takes a widget list, so day-vs-period is a branch inside `_SleepContent` in `sleep_screen.dart`, and the sections are widgets in `sleep_metric_sections.dart`. Do not recreate the four-file Kotlin split.

(The port originally dropped Kotlin's `SleepDisplayState` and derived in the screen instead. That is no longer true — the display is precomputed again, as Kotlin had it. If you find a doc or a comment that still says otherwise, it is stale.)
