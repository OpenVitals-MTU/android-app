# Flutter ↔ Kotlin test-parity matrix

Case-level comparison of every test in the Flutter build (`../mobile-app/test/`,
392 files) against the Kotlin suite (`app/src/test/`), produced 2026-08-03.
Every Flutter `test(...)`/`testWidgets(...)` case was classified by opening both
sides and comparing the actual assertions, not just names.

## Statuses

- **PORTED** — a Kotlin test exercises the same logic with equivalent assertions.
- **DIVERGED** — a Kotlin test covers the scenario but asserts less or differently.
- **MISSING** — no Kotlin coverage, and the case is portable to a JVM unit test.
- **N/A-WIDGET** — Flutter widget/golden rendering with no JVM-portable core.
- **N/A-FRAMEWORK** — Flutter-only plumbing (Riverpod wiring, plugin bridges,
  Dart re-implementations of things the JVM provides natively, the forward
  data migration, …).
- **N/A-BEHAVIOR** / **BLOCKED** — the case cannot be ported as-is because the
  Kotlin behavior itself differs; see the divergences at the end of this file.

Two of those labels are easy to over-read, so read them narrowly:

**N/A-WIDGET does not mean untestable.** It means not portable to a *JVM unit*
test. Compose has an equivalent for most of them, and these are now being
ported as instrumentation tests under `app/src/androidTest/`. They need a
device, so CI does not run them and they are run by hand instead — a widget row
marked PORTED is therefore compiled and reviewed but **not** continuously
verified, and its note says so. Treat whatever is left in this bucket as
unproven behavior rather than covered ground.

Of the original 389, **332 are ported** and passing on a Pixel 6 Pro,
including 57 goldens whose baselines are recorded and committed. The remaining
57 are reported unportable rather than merely undone, each with its reason on
its row: View-based Activities a Compose rule cannot reach, behaviour Kotlin
deliberately does not have, device state that cannot be made deterministic, and
cases whose nodes cannot be selected without adding test tags to production.

Some Flutter widget cases are pinned once here rather than per screen, because
Kotlin shares a composable where Flutter repeats one. Every metric screen's
"shows the access gate when permission missing" routes through
`HealthConnectScreenShell`, so it is covered by `HealthConnectAccessGateTest`
and the row says so. What that leaves untested is a screen that forgets to wrap
itself at all — a wiring mistake rather than a gate one, and one its own
content test catches by rendering content where a gate belongs.

**N/A-BEHAVIOR is a hypothesis, not a verdict.** It records that the two apps
behave differently — not which one is right. Auditing the bucket found four
entries that were straightforwardly Kotlin defects: a clipped DST bucket that
blanked a day of steps and hydration, a failing history drain that starved the
two behind it for the life of the process, and a reminder race that armed the
config the user had just moved away from. All four are now fixed and PORTED.
Before filing anything else here, check which side is wrong.

**N/A-FRAMEWORK was audited in full and is the least trustworthy label.** All
407 rows were re-opened against both codebases. The bucket is mostly sound —
Riverpod containers, pigeon bridges, Dart re-implementations of `Result` and
`Channel`, `org.mapsforge`'s tile and label geometry, WorkManager `doWork`
orchestration — but the dismissals were written from the Flutter side, and a
dismissal that names the Flutter mechanism ("Kotlin has no RefreshCoordinator")
tends to skip the question of whether Kotlin honours the *invariant*. Eight
shipping defects came out of asking that question; they are listed below and
fixed. Two patterns account for most of the mislabelling, and are worth
checking before trusting any remaining row:

- *"Kotlin has no <Flutter class>"* is about the mechanism, not the behaviour.
  Every metric screen's `LifecycleEventEffect(ON_RESUME)` **is** the refresh
  coordinator's counterpart, and it is assertable with a `TestLifecycleOwner`.
- *"instrumentation-only"* was applied to whole files where only the Glance or
  `AppWidgetManager` plumbing needs a device and the decision layer underneath
  is plain Kotlin over two repository interfaces. Thirty-five widget rows
  carried that note; six are now JVM tests.

## Global tally

Counted from the case rows themselves, not the per-file summary tables (which
is where an earlier hand-tally drifted). One row is an exception: the
`csv_row_converter_test.dart` row in `features.md` collapses 43 cases that map
1:1 by name, and is counted here at its true size.

| Section | Cases | PORTED | DIVERGED | MISSING | N/A-WIDGET | N/A-FRAMEWORK | N/A-BEHAVIOR | BLOCKED |
|---|---|---|---|---|---|---|---|---|
| [Manual entry / hydration / mindfulness / caffeine](manual-entry.md) | 537 | 454 | 38 | 0 | 19 | 9 | 17 | 0 |
| [Other features (settings, activity, sleep, heart, imports/csv+route, bodyenergy, readiness, widgets, onboarding, achievements)](features.md) | 872 | 624 | 69 | 26 | 23 | 123 | 7 | 0 |
| [Dashboard + device sync](dashboard-devicesync.md) | 89 | 70 | 9 | 0 | 4 | 6 | 0 | 0 |
| [Apple Health import](apple-health-import.md) | 98 | 72 | 3 | 0 | 0 | 23 | 0 | 0 |
| [Body / cycle / recovery](body-cycle-recovery.md) | 48 | 44 | 2 | 0 | 0 | 2 | 0 | 0 |
| [Domain / data / core](domain-data-core.md) | 1,053 | 736 | 75 | 10 | 5 | 222 | 5 | 0 |
| [Devices (GFDI, notifications, sync) / UI / goldens](devices-ui.md) | 744 | 646 | 65 | 9 | 6 | 10 | 0 | 8 |
| **Total** | **3,441** | **2,646** | **261** | **45** | **57** | **395** | **29** | **8** |

Of the 2,952 portable cases (PORTED + DIVERGED + MISSING), **2,646 are ported
1:1** — up from 1,365 when the matrix was first built. Each section ends with a
`### Portable gaps` list of what it still has outstanding and a "blocked on a
behavior decision" section for rows no test can fix. The 45 remaining MISSING
are 26 features rows (Body Energy diagnostics, never ported, and the
features/readiness cases whose seams are still private to their screens), 10
domain/data rows, and 9 devices/UI rows.

The Kotlin suite is at 3,126 unit tests, up from 2,103 before this work, plus
402 instrumentation tests that run on a device rather than in CI.

## Behavior divergences surfaced by the comparison

These are cases where a 1:1 test CANNOT be ported because the Kotlin behavior
itself differs from Flutter — they need a product decision, not a test:

- **Dashboard supported-metrics gating** — Kotlin has no device-support
  concept for tiles at all (drives ~9 dashboard MISSING rows).
- **Dashboard sensor status** — BLE registry is sensors only; no watch-kind filtering
  filtering: a paired watch is counted like a sensor.
- **Dashboard resume guard** — Kotlin reloads on every resume; Flutter has a
  guard interval.
- **Dashboard tile layout model** — Kotlin persists an authoritative saved
  list (absent = hidden); Flutter persists order + hidden-set (several
  DIVERGED rows follow from this, not from missing tests).
- **Home-widget metric catalog** — Kotlin excludes CAFFEINE + CARDIO_LOAD;
  Flutter excludes caffeine + intensityMinutes.
- **Mid-load display** — Kotlin deliberately keeps the previous display while a
  new range loads; Flutter clears it.

Fixed during the parity work rather than deferred:

- All six daily aggregate reads now fold same-date buckets through
  `byLocalDate`. Health Connect clips a range's final bucket, and a 24h slicer
  is instant-aligned, so a range containing a 25-hour local day returned the
  leftover hour as a second bucket on the final date — which the readers either
  emitted twice or let overwrite the whole day.
- `HistorySyncScheduler` guards each drain individually. Its once-per-open latch
  is claimed before the first drain runs, so an escaping throw starved the other
  two for the life of the process.
- `HydrationReminderController` holds a mutex across the read and the arm, so
  two rapid config changes can no longer leave the earlier one scheduled.
- `HcFixture.allRecords` seeds the fixture's `hrv`, `restingHeartRate` and
  `basalMetabolicRate` arrays, which it had never parsed — 777 records that no
  corpus test had ever seen.
- Three stress-factor strings escaped their literal percent. `HRV is %1$d%
  above your usual baseline.` parsed the bare `%` as an `%a` hex-float
  conversion against the `Int` it was given, so the stress details screen threw
  whenever HRV sat above baseline; its neighbours printed `%b` as "true" and
  `%o` as octal. `StringFormatSpecifierTest` now checks every source string, and
  the test task declares `strings.xml` as an input so an edit cannot leave the
  guard stale.
- The readiness confidence line resolves through `strings.xml`. The daily panel
  and the details screen each carried their own copy of the mapping, both built
  from English literals, so the line stayed English in all sixteen languages.
- The route backfill now runs altitudes through `RouteElevation`'s smoothing +
  hysteresis filter, like a live recording and an imported route already did, so
  Flutter's fixtures pass with their exact expectations.
- `ActivityDetailViewModel.load()` guards the speed / cadence / marker / recovery
  reads individually, so one failing read costs its card rather than the screen.
- `HydrationReminderController.handleQuickAdd` swallows a failing re-anchor: a
  reminder is a nicety and must never undo a drink that already landed.
- `Float.dragSteps` short-circuits a zero step instead of dividing by it.
- `BleDeviceRepository.updateBatteryLevel` early-returns on an unchanged percent,
  so an identical reading neither re-persists nor advances `batteryUpdatedAt`.
- The skin-temperature card reads the newest entry that actually CARRIES a delta
  — the same population its chart draws — so a delta-less newest reading no
  longer blanks it.
- `DailyGoalProgress.currentStreakDays(today)` skips an unmet day that has not
  finished yet; only a PAST unmet day breaks the run.
- `PeriodTitles.kt` renders a past rolling window as the dated span it covers
  ("12 Apr – 11 May 2026"), with both years when it straddles one.
- The widget launch-route allow-list parses the `body_energy` segment strictly as
  an ISO date, so `yesterday` and `2026-13-45` are dropped.

### From the N/A-FRAMEWORK audit

- A home widget no longer overwrites itself with `--` and "No data" when its
  read fails or times out. The 30-minute tick regularly lands while Health
  Connect is mid-update or Doze has slowed the read past its budget, and the
  failure snapshot was byte-identical to a genuinely empty day — so the home
  screen asserted the user had slept nothing and walked nowhere, and held that
  until a later tick happened to succeed. A read that did not happen now leaves
  the last good snapshot alone. Timeouts had been falling out of the loaders as
  the same null an empty day produces, which is why the two were drawn the same.
- Watch syncs, phone-to-phone syncs and finished Apple Health imports tell every
  placed widget to redraw. `updatePeriodMillis` is honoured at the system's
  convenience and not at all in Doze, so a sync at 08:05 left the morning's
  tiles on their pre-sync numbers while the user watched it succeed.
- A Garmin sync invalidates and rebuilds the Body Energy chain from the earliest
  day it back-filled. Body Energy chains across midnight, so a week of recovered
  sleep invalidated every day after it too — and days past the 7-day settling
  window are never revisited, so they stayed frozen at their pre-sync scores
  permanently. `syncAll(force = true)` existed for exactly this caller and had
  none; `invalidateForward` was reachable only from the repository's own ripple.
- Both reminder schedules are re-planned whenever the app comes to the
  foreground. A reminder that fires without `POST_NOTIFICATIONS` cancels its own
  alarm and returns before rescheduling, and that permission is auto-revoked for
  unused apps by default — so re-granting it in system settings armed nothing
  and the toggle went on reading "on" forever. The boot receivers only covered
  reboot, reinstall and clock changes, none of which is that sequence.
- `MindfulnessReminderController` holds the same scheduling mutex its hydration
  twin already had. Its `HydrationReminderControllerTest` counterpart was also
  vacuous: it probed for the interleaving with `yield()`, which does not suspend
  on an unconfined dispatcher with an empty queue, so the overlap it asserted
  against could never have been observed.
- The quick-beverage widget re-anchors the hydration reminder after logging.
  It hid the notification but left the alarm armed from the previous drink, so
  the nag arrived minutes after the user drank — the in-app save and the
  notification quick-add both re-anchored, and only the tile did not.
- `findMatchingImportedClientRecordIds` no longer filters candidates through a
  hardcoded `apple_health_` prefix. CSV ids are minted `csv_<slug>_<hex>`, so
  every one was discarded before the wanted-set check and the CSV importer could
  never find a duplicate: re-importing an identical file reported "already
  present 0" for records Health Connect was holding. The fake in
  `CsvImportServiceTest` returned the intersection, so it passed against
  behaviour production could not produce.
- The phone-to-phone sync codec reads back the blood-pressure body position and
  cuff site it has always written. Both were encoded and neither decoded, so a
  reading taken sitting down on the left upper arm arrived on the other phone
  with both fields erased — into a store other apps read.
- The language picker names every language in that language. It resolved each
  entry through a translatable resource, so a German UI offered "Estnisch" and
  an Estonian UI offered "Hispaania" — exonyms, shown to the one user who by
  definition cannot read the current UI language. The five `settings_language_*`
  name strings are retired; only "System default" is still translated.
- `verify-translations.py` sees specifiers that carry flags, width or precision.
  Its pattern matched only `%2$s` in `%1$.1f C · %2$s`, so dropping the
  temperature or retyping it `%1$.1d` — which throws when the card draws — both
  passed the gate. Plural branches a locale adds beyond the base (Spanish's
  `many`, Polish's `few`) were compared against nothing at all and are now
  checked against the base's `other`; `values-es` already ships such a branch.
  Both holes were confirmed by sabotaging a scratch copy of `res/` and watching
  the old script pass it.

### Found while verifying the mapsforge rows

The 19 mapsforge rows are correctly N/A — tile indexing, label collision and the
render transform are all `org.mapsforge`'s. Checking that turned up three
defects in the ~200 lines of Kotlin around it, which no parity row tracked:

- `AndroidGraphicFactory.createInstance` ran inside the composable's
  `remember(context, mapPacksKey)`, so importing or deleting a map pack re-ran
  it. It writes a public static `INSTANCE` with no null check and no
  synchronization, on a non-volatile field read from the `MapWorkerPool` render
  threads — it now runs once per process. The `clearResourceMemoryCache()` on
  dispose went with it: it is global, so one map going away stripped the symbols
  from another still on screen.
- The tile-cache directory was named from the pack **ids** while the `MapView`
  is rebuilt on ids **and paths**. Re-importing a pack under the same id at a new
  path gave the new map the old map's cache name — and the cache is created with
  `persistent = false`, so the outgoing map's `destroyAll()` deleted the
  directory the incoming one had just opened. `String.hashCode` could also
  collide two genuinely different pack sets onto one cache. It is now a digest
  of the same string the map is keyed on.
- `zoomForBounds` was a hand-rolled ladder over the lat/lon span in **degrees**.
  It ignored the viewport, so a 540px preview and a 2160px tablet map got the
  same zoom when they are three levels apart; and it ignored latitude, when
  Mercator stretches latitude into y by roughly `1 / cos(latitude)` — the same
  0.2° box is 583px tall at the equator, 1169px at 60°N and 2266px at 75°N. A
  northern route was drawn one to two levels too close, with its ends off
  screen. It now delegates to `LatLongUtils.zoomForBounds` and clamps to the
  zoom range the packs actually carry, rather than a fixed 7..16 that stopped a
  city extract reaching the detail it was imported for.

### A missing feature, not a missing test

`FitBodyEnergyFromWatchUseCase` had no Kotlin counterpart. Both halves of it
were already ported — `BodyEnergyCalibrationFit` computes the gain update and
`BodyEnergyWatchObservations` pairs watch samples against a timeline — but
nothing joined them and nothing called them, so a Garmin watch's Body Battery
was only ever drawn on the watch-data screen. The gains stayed at their defaults
however much watch data accumulated. The use case now runs after a sync, once
the Body Energy chain has been rebuilt: Flutter fits *before* the rebuild, and
ordering it after avoids teaching the model from a prediction that had not yet
seen the sync's own data.
