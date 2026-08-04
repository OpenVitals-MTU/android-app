## test/features/body/body_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/body/BodyDisplayDerivationsTest.kt (readings list, tracked metrics, per-day series, over the extracted `hasAnyBodyData` / `newestFirst()` / `onDate()` seams) and BodyPresentationMapperTest.kt (summary)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty period has no data, no readings and no tracked metrics | PORTED | BodyDisplayDerivationsTest.kt: `an empty period has no data, no readings and no tracked metrics` | needed the hasAnyBodyData seam |
| the summary takes the latest reading, and the first weight | PORTED | BodyDisplayDerivationsTest.kt: `the summary takes the latest reading, and the first weight` | firstWeightKg/weightChangeKg/ffmi/adjustedFfmi now asserted |
| the daily series keeps one value per day: that day's latest | PORTED | BodyDisplayDerivationsTest.kt: `the daily series keeps one value per day, that day's latest` | values + dayValues (intraday) + trackedMetrics |
| BMI has a series only when a height is known | PORTED | BodyDisplayDerivationsTest.kt: `BMI has a series only when a height is known` | series presence/absence + FFMI never gets one |
| readings are newest first, indexed by day, and only OpenVitals ones are editable | PORTED | BodyDisplayDerivationsTest.kt: `readings are newest first, indexed by day, and only OpenVitals ones are editable` | needed newestFirst()/onDate() seams |
| a period with a latest value but no entries still has data | PORTED | BodyDisplayDerivationsTest.kt: `a period with a latest value but no entries still has data` | hasAnyBodyData + empty readings + metric latest |

## test/features/body/body_metric_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/body/BodyViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | DIVERGED | BodyViewModelTest: `load success populates weight entries`, `initial load clears loading and produces no error` | entries/loading/error covered; no precomputed readings-list equivalent in Kotlin state |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | BodyViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | toScreenError() now maps a SecurityException (Health Connect's refusal) to ScreenError.PermissionDenied; error type and empty entries asserted |
| an unexpected failure carries its message to the screen | PORTED | BodyViewModelTest: `load failure sets error and clears loading` | |
| deleting an entry rebuilds the display without waiting for the reload | PORTED | BodyViewModelTest.kt: `deleting an entry rebuilds the display without waiting for the reload` | delete gated so the optimistic pre-reload state is observed, then the FORCE reload verified |
| a failed delete restores the previous display, with an error | PORTED | BodyViewModelTest.kt: `a failed delete restores the previous display, with an error` | no failed-delete rollback test |
| navigating to a new range clears the stale display mid-load | DIVERGED | BodyViewModelTest.kt: `navigating to a new range keeps the previous display until the new one lands` | blocked on behavior decision - Flutter blanks the display mid-load, Kotlin deliberately keeps it (no chart flash); the test pins Kotlin's actual behavior |
| a same-range refresh keeps the display (no loading flash) | PORTED | BodyViewModelTest.kt: `a same-range refresh keeps the display (no loading flash)` | no refresh-keeps-display test |
| a stale load cannot overwrite the newer one it lost to | PORTED | BodyViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | LoadCoordinator single-flight |

## test/features/body/body_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/presentation/MetricDetailSectionOrderViewModelTest.kt (section reorder only); otherwise none

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| aggregate renders one trend chart per tracked metric, none for empty ones | PORTED | BodyTrendsAndEntriesTest: `theAggregateChartsOnlyTheMetricsThatWereActuallyTracked` | Compose instrumentation; runs on a device, not in CI |
| statistics grid shows every metric with its latest value | PORTED | BodyContentTest: `oneTrackedMeasurementIsEnoughToCountAsData` | Compose instrumentation; runs on a device, not in CI |
| swiping an OpenVitals entry away deletes it through the repo | PORTED | BodyTrendsAndEntriesTest: `swipingAnOpenVitalsReadingAwayDeletesItThroughTheRepository` | Compose instrumentation; runs on a device, not in CI |
| read-only (non-OpenVitals) entries are not swipe-deletable | PORTED | BodyTrendsAndEntriesTest: `aReadingThisAppDidNotWriteIsNotSwipeDeletable` | Compose instrumentation; runs on a device, not in CI |
| empty period renders the body placeholder | PORTED | BodyContentTest: `anEmptyPeriodRendersTheBodyPlaceholder` | Compose instrumentation; runs on a device, not in CI |
| section reorder persists through the preferences repository | PORTED | MetricDetailSectionOrderViewModelTest: `moveSectionToTarget_reordersAndPersists`, `toggleSectionEdit_switchesEditingState` | reorder+persist+edit-toggle essence fully covered at JVM level |

## test/features/cycle/cycle_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/cycle/CyclePresentationMapperTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/features/cycle/CyclePresentationTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty period derives zeroes and no observations | PORTED | CyclePresentationMapperTest.kt: `display has no data for empty cycle data` | latestBbtCelsius-null, sampleCount, sources and observationsFor()-empty added |
| period days count the days a period covers, not the records | PORTED | CyclePresentationMapperTest.kt: `period days count the days a period covers, not the records` | exact 3-day count from one record, plus the exact active dates |
| the summary counts every record, and the latest basal temperature | PORTED | CyclePresentationMapperTest: `display has data when cycle records exist` + `summary counts period days with active menstruation` | |
| observations from every record type are merged, newest first | PORTED | CyclePresentationTest: `observationsFor maps cycle records to sorted display observations` | |

## test/features/cycle/cycle_screen_test.dart
Kotlin counterpart: none (underlying logic in CyclePresentationTest / CycleViewModelTest)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders summary + observations once loaded | PORTED | CyclePeriodContentTest: `rendersSummaryAndObservationsOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| gates the screen when the cycle permission is missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |
| shows the empty placeholder with no data | PORTED | CyclePeriodContentTest: `showsTheEmptyPlaceholderWithNoData` | Compose instrumentation; runs on a device, not in CI |

## test/features/cycle/cycle_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/cycle/CycleViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | CycleViewModelTest: `load success populates cycle data and missing permissions` + `initial load clears loading and sets empty data` | |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | CycleViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | a refused read now maps to ScreenError.PermissionDenied; the missingPermissions state stays a separate, successful-load signal |
| an unexpected failure carries its message to the screen | PORTED | CycleViewModelTest: `load failure sets error and clears loading` | |
| refresh reloads the current selection in force mode | PORTED | CycleViewModelTest.kt: `resuming the current period refreshes the current selection in force mode` | Kotlin's refresh entry point is resumeCurrentPeriod(refreshCurrent = true) |
| a stale load cannot overwrite the newer one it lost to | PORTED | CycleViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | no single-flight/staleness test for cycle |

## test/features/recovery/recovery_detail_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/recovery/RecoveryViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildRecoveryDetailDisplay > a day the lookback never reached is blank, not an error | PORTED | RecoveryViewModelTest.kt: `a day the lookback never reached is blank, not an error` | blank fallback day + mainSleepSession-null + NO_DATA + no error |
| buildRecoveryDetailDisplay > the selected day is picked out of the week, and scored | PORTED | RecoveryViewModelTest: `load builds seven day recovery overview from sleep sessions` | Kotlin computes the score rather than injecting it |
| buildRecoveryDetailDisplay > the main session of a night is the one with the most sleep in it | PORTED | RecoveryViewModelTest: `main sleep session uses longest session for sleep schedule` | same `maxBy(sleepDurationMsFromStages)` selection logic |
| RecoveryDetailViewModel > a loaded week lands with the selected day precomputed | PORTED | RecoveryViewModelTest: `load builds seven day recovery overview from sleep sessions` | 7-day lookback, selectedDate, main session all asserted |
| RecoveryDetailViewModel > a permission failure becomes ScreenErrorPermissionDenied | PORTED | RecoveryViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | error type, cleared loading and empty week asserted, as in the Dart original |
| RecoveryDetailViewModel > a failed reload keeps the week already on screen | PORTED | RecoveryViewModelTest.kt: `a failed reload keeps the week already on screen` | Kotlin onFailure does keep `days`, but no test asserts it |

## test/features/recovery/recovery_screen_test.dart
Kotlin counterpart: none

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Recovery renders the stress score card once loaded | PORTED | StressDetailsContentTest: `theStressCardRendersItsScoreLevelAndConfidenceOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| Recovery shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |

## test/features/recovery/sleep_efficiency_detail_screen_test.dart
Kotlin counterpart: none (SleepEfficiencyDetailScreen.kt composable has no test)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the four sleep-efficiency cards from a fixed estimate | PORTED | RecoveryDetailScreensTest: `theSleepEfficiencyScreenRendersItsFourCardsFromAFixedEstimate` | Compose instrumentation; runs on a device, not in CI |
| tapping a reference button opens the link without throwing | N/A-FRAMEWORK | none | url_launcher platform-channel fallback behavior |
| the calculation card expands and collapses | PORTED | RecoveryDetailScreensTest: `theSleepEfficiencyCalculationCardExpandsAndCollapses` | Compose instrumentation; runs on a device, not in CI |
| a no-data day renders the no-data placeholders | PORTED | RecoveryDetailScreensTest: `aNoDataDaySleepEfficiencyStillRendersItsCards` | Compose instrumentation; runs on a device, not in CI |

## test/features/recovery/sleep_score_detail_screen_test.dart
Kotlin counterpart: none (SleepScoreDetailScreen.kt composable has no test)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the four sleep-score cards from a fixed estimate | PORTED | RecoveryDetailScreensTest: `theSleepScoreScreenRendersItsFourCardsFromAFixedEstimate` | Compose instrumentation; runs on a device, not in CI |
| tapping a reference button opens the link without throwing | N/A-FRAMEWORK | none | url_launcher platform-channel fallback behavior |
| the calculation card expands and collapses | PORTED | RecoveryDetailScreensTest: `theSleepScoreCalculationCardExpandsAndCollapses` | Compose instrumentation; runs on a device, not in CI |
| a no-data day renders the no-data placeholders | PORTED | RecoveryDetailScreensTest: `aNoDataDaySleepScoreStillRendersItsCards` | Compose instrumentation; runs on a device, not in CI |

### Counts
PORTED: 28, DIVERGED: 2, MISSING: 0, N/A-WIDGET: 16, N/A-FRAMEWORK: 2

### Missing list

None outstanding.
