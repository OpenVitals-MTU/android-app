# Flutter-to-Kotlin test-parity matrix — dashboard + devicesync

## test/features/dashboard/dashboard_app_open_refresh_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt (partial); refresh wiring lives in `DashboardScreen.kt` LifecycleEventEffect + `DashboardViewModel.resumeCurrentDay`/drain block

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the app-open refresh recovers a dashboard stuck behind the Health Connect access gate | N/A-FRAMEWORK | none | Riverpod provider re-resolution on lifecycle resume; Kotlin gate/availability path (MainActivity/Compose) has no JVM test |
| a permission granted outside the app is picked up on the next app open | N/A-FRAMEWORK | none | provider invalidation on resume; Kotlin permission re-read on resume untested |
| returning to the foreground re-reads the day | PORTED | DashboardViewModelTest.kt: `resumeCurrentDay advances unpinned past date to today` | resume triggers a real reload of today, verified via loader |
| the history caches drain after the app-open read settles, not alongside it | PORTED | DashboardViewModelTest.kt: `the history caches drain after the dashboard read settles` | ordering pinned (load before drain, exactly one drain); Kotlin's once-per-open guard lives in HistorySyncScheduler |
| a resume inside the guard interval does not re-read | DIVERGED | none (deliberate) | behavior decision taken: the user chose to keep Kotlin's behavior - every resume re-reads the day (`resumeCurrentDay`), with no guard interval to test |

## test/features/dashboard/dashboard_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardPresentationMapperTest.kt, DashboardContentLayoutTest.kt (carousel partition / activities precedence, over the extracted `dashboardVisibleWidgetIds` + `dashboardActivitiesForDay`), DashboardViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a fully-supported day maps both rings and every tile | PORTED | DashboardPresentationMapperTest.kt: `build_fullySupportedDay_mapsBothRingsAndEveryTile` | whole-map assertion - both CIRCLE rings plus every widget id except the deliberate WORKOUT exclusion |
| empty data still renders the rings and the empty tiles | PORTED | DashboardPresentationMapperTest.kt: `build_emptyDay_stillRendersTheRingsAndTheEmptyTiles` | rings-always-present plus empty tiles carry no progress |
| the saved order and hidden set are already applied | DIVERGED | DashboardViewModelTest.kt: `dashboard widgets restore saved order`, `dashboard widgets ignore unknown saved ids` | order restore covered; legacy title→id migration is Flutter-only |
| tiles with no data sink below the ones with some > empty tiles come last in the carousel, in saved order | PORTED | DashboardContentLayoutTest.kt: `empty tiles come last in the carousel in saved order` | actual partition and stable saved order now asserted |
| tiles with no data sink below the ones with some > the edit grid keeps the true saved order | PORTED | DashboardContentLayoutTest.kt: `the edit grid keeps the true saved order` | edit-mode-skips-partition rule lives in DashboardContent.kt, untested |
| tiles with no data sink below the ones with some > a tile still loading holds its place instead of sinking | PORTED | DashboardContentLayoutTest.kt: `a tile still loading holds its place instead of sinking` | now asserted at placement level, not just the flag |
| tiles with no data sink below the ones with some > a saved order that leads with an empty tile still sinks it | PORTED | DashboardContentLayoutTest.kt: `a saved order that leads with an empty tile still sinks it` | partition-over-saved-order interaction untested |
| a hidden hero ring leaves the row and joins the tray | PORTED | DashboardContentLayoutTest.kt: `a hidden hero ring leaves the row and joins the tray` | Kotlin model - tray is the complement of the saved list |
| edit mode materialises an unsupported metric into the tray, not the carousel | PORTED | DashboardContentLayoutTest.kt: `edit mode materialises an unsupported metric into the tray not the carousel` (+ `adding an unsupported metric back is not a dead end`) | supported-metrics gating ported: outside edit mode an unsupported metric has no tile; edit mode materialises it into the tray unless the user deliberately placed it |
| caffeine tile: active-now headlines today, consumed rides subtitle | PORTED | DashboardPresentationMapperTest.kt: `build_caffeineWidget_activeOnlyHeadlinesWithoutASubtitle` + `build_caffeineWidget_withNeitherFigureShowsTheEmptyMessage` | active-only and empty-message branches added |
| the goals reach the ring, not the defaults | PORTED | DashboardPresentationMapperTest.kt: `build_stepsRing_fillsAgainstTheUsersGoalNotTheDefault` | ring fraction and goal label against a custom goal |
| activities > the workout list wins when it has entries | PORTED | DashboardContentLayoutTest.kt: `the workout list wins when it has entries` | workouts-over-workout precedence for the activities section untested in Kotlin |
| activities > a lone workout is the fallback | PORTED | DashboardContentLayoutTest.kt: `a lone workout is the fallback` | lone-workout fallback untested |

## test/features/dashboard/dashboard_goals_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt (goal plumbing lives in `DashboardViewModel.dashboardDailyGoals()`)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the steps ring uses the user's goal, not the 8,000 default | PORTED | DashboardPresentationMapperTest.kt: `build_stepsRing_fillsAgainstTheUsersGoalNotTheDefault` | ring fill now asserted, not just the goal on state |
| every goal the summary shows comes from preferences | PORTED | DashboardViewModelTest.kt: `dashboard daily goals follow preferences` | all 14 goals asserted, plus the none-equals-default premise |
| an untouched install still gets the documented defaults | PORTED | DashboardViewModelTest.kt: `an untouched install still gets the documented defaults` | `DashboardDailyGoals()` defaults from MetricDailyGoalKey never asserted |

## test/features/dashboard/dashboard_resume_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| resumeCurrentDay reloads today by default | PORTED | DashboardViewModelTest.kt: `resumeCurrentDay advances unpinned past date to today` | Kotlin uses mocks vs Flutter's real graph, but same logic + load verification |
| resumeCurrentDay honours a day the user pinned in the past | DIVERGED | DashboardViewModelTest.kt: `resumeCurrentDay keeps user selected past date pinned` | strengthened with an explicit no-extra-read assertion pinning Kotlin's deliberate divergence (no in-place refresh of a pinned day) |
| selectDate on a past day pins it; selecting today clears the pin | PORTED | DashboardViewModelTest.kt: `selectDate on a past day pins it and selecting today clears the pin` | pin-set covered; clear-on-select-today exists in code (line 504) but untested |
| nextDay onto a still-past day keeps the pin | PORTED | DashboardViewModelTest.kt: `nextDay onto a still-past day keeps the pin` | pin recomputation in nextDay (line 496) untested |
| nextDay back onto today clears the pin | PORTED | DashboardViewModelTest.kt: `nextDay back onto today clears the pin` | `nextDay advances from yesterday to today` doesn't assert pin state after resume |

## test/features/dashboard/dashboard_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt (for portable essences); no Compose UI tests for the dashboard screen

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows a loader then renders the summary dashboard | PORTED | DashboardContentTest: `rendersTheSummaryDashboardOnceLoaded` | Compose instrumentation; Kotlin has no in-body prompt or sensor row, so the assertion is that the body stays the dashboard |
| carousel tiles fill their grid cell in both modes | N/A-WIDGET | — | pure layout measurement |
| edge-scroll keeps paging while a drag is held at the edge | N/A-WIDGET | — | gesture/pager timer behavior |
| the carousel still swipes after a tile is dragged to another page | N/A-WIDGET | — | drag-lifecycle regression in Flutter's PageView |
| edit mode enters/exits and reorder+remove render without error | PORTED | DashboardContentTest: `editModeEntersAndExitsWithoutLosingTheGrid` | Compose instrumentation; Kotlin has no in-body prompt or sensor row, so the assertion is that the body stays the dashboard |
| removing a widget moves it to the add tray and back | PORTED | DashboardViewModelTest.kt: `dashboard widget remove add and move persist order` | essence (remove→absent, add→back) at VM level; tray text is UI |
| a removed hero ring can be added back from the tray | PORTED | DashboardViewModelTest.kt: `dashboard widget remove add and move persist order` | rings aren't special-cased in Kotlin's widget list, same code path |
| missing permissions produce no prompt, just the dashboard | PORTED | DashboardContentTest: `missingPermissionsProduceNoPromptJustTheDashboard` | Compose instrumentation; Kotlin has no in-body prompt or sensor row, so the assertion is that the body stays the dashboard |
| previous-day navigation moves the selected day back | PORTED | DashboardViewModelTest.kt: `previousDay decrements selectedDate by one day` | label rendering ("Yesterday") is UI-only |
| shows the access gate when Health Connect is unavailable | N/A-WIDGET | — | gate rendering; Kotlin `healthConnectAvailability` state exposure untested |
| nothing is granted at all and the dashboard still just renders | PORTED | DashboardContentTest: `missingPermissionsProduceNoPromptJustTheDashboard` | Compose instrumentation; Kotlin has no in-body prompt or sensor row, so the assertion is that the body stays the dashboard |
| sensor status > is never rendered in the dashboard body, even with sensors | PORTED | DashboardContentTest: `missingPermissionsProduceNoPromptJustTheDashboard` | Compose instrumentation; Kotlin has no in-body prompt or sensor row, so the assertion is that the body stays the dashboard |
| edit mode offers a metric the device does not support | PORTED | DashboardViewModelTest.kt: `edit mode offers a metric the device does not support` (+ `an unsupported metric leaves the display again when edit mode ends`) | ViewModel level: no tile outside edit mode, tray entry inside it, and adding it back lands it in the grid; tray rendering itself is Compose |

## test/features/dashboard/dashboard_semantics_test.dart
Kotlin counterpart: none

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| tiles and actions announce themselves to the screen reader | PORTED | DashboardSemanticsTest: `tilesAndActionsAnnounceThemselvesToTheScreenReader` | Compose instrumentation; runs on a device, not in CI |

## test/features/dashboard/dashboard_sensor_status_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardSensorStatusTest.kt (direct tests over the now-`internal` `toDashboardSensorStatus`), plus DashboardViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| toDashboardSensorStatus > the live battery wins over the persisted one | PORTED | DashboardSensorStatusTest.kt: `the live battery wins over the persisted one` | direct unit test as well as the ViewModel one |
| toDashboardSensorStatus > the persisted battery is the fallback when no live one is reported | PORTED | DashboardSensorStatusTest.kt: `the persisted battery is the fallback when no live one is reported` | live-status-with-null-battery case now covered |
| toDashboardSensorStatus > the lookup falls back from device id to address | PORTED | DashboardSensorStatusTest.kt: `the lookup falls back from device id to address` | address fallback exists in Kotlin (line 607) but untested |
| toDashboardSensorStatus > a device with no live status at all reads as disconnected | PORTED | DashboardSensorStatusTest.kt: `a device with no live status at all reads as disconnected` | scenario exercised but DISCONNECTED default never asserted |
| derived getters > a paired watch alone puts up no icon | REMOVED | — | Watch-kind filtering left the BLE registry; leftover WATCH rows are dropped on read instead |
| derived getters > a watch beside a sensor adds neither counts nor battery | PORTED | DashboardSensorStatusTest.kt: `a watch beside a sensor adds neither counts nor battery` | the watch's lower battery no longer headlines the top-bar action |
| derived getters > a bike computer still counts: the screen lists it | PORTED | DashboardSensorStatusTest.kt: `a bike computer still counts and the screen lists it` | an Edge broadcasts standard GATT, so it belongs to the screen the icon opens |
| derived getters > an empty status has no devices and no battery | PORTED | DashboardSensorStatusTest.kt: `an empty status has no devices and no battery` | empty-status defaults untested |
| derived getters > counts enabled/connected devices and the lowest battery | PORTED | DashboardSensorStatusTest.kt: `counts enabled and connected devices and the lowest battery` | three devices - enabledCount, connecting-is-not-connected, multi-device min |
| derived getters > the lowest battery ignores devices that never reported one | PORTED | DashboardSensorStatusTest.kt: `the lowest battery ignores devices that never reported one` | mapNotNull/minOrNull behavior untested |

## test/features/dashboard/dashboard_summary_visibility_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardPresentationMapperTest.kt (device-support gating now lives in `DashboardData.supportedMetrics` -> `DashboardPresentationMapper.build(includeUnsupported =)`)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a supported metric with no reading still gets an empty tile | PORTED | DashboardPresentationMapperTest.kt: `build_supportedMetricWithNoReading_stillGetsAnEmptyTile` (+ `build_emptyDay_stillRendersTheRingsAndTheEmptyTiles`) | now asserted against an explicit `supportedMetrics` set as well: progress null, showsNoDataMessage true |
| a supported metric with a reading renders its value, not a message | PORTED | DashboardPresentationMapperTest.kt: `build_supportedMetricWithAReading_rendersItsValueNotAMessage` (+ `showsNoDataMessage_sinksEmptyTilesButNotLoadingOnes`) | with-data widget not marked no-data; value formatting asserted elsewhere |
| an unsupported metric gets no tile at all | PORTED | DashboardPresentationMapperTest.kt: `build_unsupportedMetric_getsNoTileAtAll` | the dropped metric's supported neighbours are still there, empty |
| required metrics show a zero reading rather than a no-data message | PORTED | DashboardPresentationMapperTest.kt: `build_requiredMetrics_showAZeroReadingRatherThanANoDataMessage` | zero steps reads 0; calories with NO_DATA reads no-data |
| metrics the mapper previously dropped now appear when supported | PORTED | DashboardPresentationMapperTest.kt: `build_fullySupportedDay_mapsBothRingsAndEveryTile` | glucose, skin temp, BMR, bone mass, body water presence now asserted |
| no tiles at all when the device supports nothing | PORTED | DashboardPresentationMapperTest.kt: `build_deviceSupportsNothing_producesNoTilesAtAll` | Kotlin is stricter than Flutter: its two hero rings are ordinary widgets, so nothing renders at all rather than two empty rings |
| includeUnsupported > materialises metrics absent from supportedMetrics | PORTED | DashboardPresentationMapperTest.kt: `build_includeUnsupported_materialisesMetricsAbsentFromSupportedMetrics` | `DashboardDisplayState.unsupportedIds` carries the materialised ids (widget ids, where Flutter uses metric names) |
| includeUnsupported > materialises every metric when the device supports nothing | PORTED | DashboardPresentationMapperTest.kt: `build_includeUnsupported_materialisesEveryMetricWhenTheDeviceSupportsNothing` | every id except the deliberate WORKOUT exclusion, all of them unsupported |
| includeUnsupported > defaults to false: unsupported metrics stay dropped | PORTED | DashboardPresentationMapperTest.kt: `build_includeUnsupported_defaultsToFalseSoUnsupportedMetricsStayDropped` | the ViewModel only passes true while editing |
| Body Energy tile > renders currentScore and the Start/+/- subtitle when set up | PORTED | DashboardPresentationMapperTest.kt: `build_bodyEnergyWidget_rendersCurrentScoreAndStartChargedDrainedSubtitle` | dashboard tile mapping only; no bodyenergy production file touched |
| Body Energy tile > shows "Not set up" when the timeline is absent | PORTED | DashboardPresentationMapperTest.kt: `build_bodyEnergyWidget_isNotSetUpUntilCalibrationCompletes` + `build_bodyEnergyWidget_showsNoDataWhenTimelineIsAbsent` | Kotlin gates on the calibration flag rather than timeline presence; both branches pinned |
| tile destinations match Kotlin > heart and vitals tiles each open their own metric screen | PORTED | DashboardTileDestinationTest: `heartAndVitalsTilesEachOpenTheirOwnMetricScreen` | Compose instrumentation; runs on a device, not in CI |
| tile destinations match Kotlin > body tiles each open their own metric screen | PORTED | DashboardTileDestinationTest: `bodyTilesEachOpenTheirOwnMetricScreen` | Compose instrumentation; runs on a device, not in CI |
| tile destinations match Kotlin > hydration, mindfulness and caffeine tiles open their detail views | PORTED | DashboardTileDestinationTest: `hydrationMindfulnessAndCaffeineTilesOpenTheirDetailViews` | Compose instrumentation; runs on a device, not in CI |

## test/features/dashboard/dashboard_tile_layout_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt (layout model differs: Kotlin's saved list is authoritative — absent = hidden — vs Flutter's order+hidden-set; the user decided to keep Kotlin's model, so the rows below stay deliberate divergences)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| migrateDashboardLayoutKeys > translates a legacy title to its id | N/A-FRAMEWORK | — | Flutter-only legacy title persistence; Kotlin persists enum names (its own legacy case: `dashboard widgets ignore legacy browse saved id`) |
| migrateDashboardLayoutKeys > leaves ids alone, so migrating twice is a no-op | N/A-FRAMEWORK | — | Flutter-only migration |
| migrateDashboardLayoutKeys > keeps a key it cannot resolve | N/A-FRAMEWORK | — | Flutter-only; note Kotlin *drops* unknown ids instead |
| migrateDashboardLayoutKeys > collapses a title and its id to one entry | N/A-FRAMEWORK | — | Flutter-only migration |
| applyDashboardTileLayout > no order/hidden returns tiles in default order | PORTED | DashboardViewModelTest.kt: `dashboard widgets default to full widget set` | |
| applyDashboardTileLayout > reorders known tiles; unknown tiles keep default order at the end | DIVERGED | DashboardViewModelTest.kt: `dashboard widgets restore saved order` | Kotlin semantics differ: unsaved widgets are dropped (hidden), not appended |
| applyDashboardTileLayout > drops hidden tiles by default | DIVERGED | DashboardViewModelTest.kt: `dashboard widget remove add and move persist order` | hidden = removed-from-list in Kotlin; covered via removal membership |
| applyDashboardTileLayout > includeHidden keeps hidden tiles but still applies order | DIVERGED | none (deliberate) | behavior decision taken: the user chose to keep Kotlin's layout model - the saved list is authoritative (absent = hidden), so there is no order+hidden-set pair and no `includeHidden` to test |
| applyDashboardTileLayout > order + hidden together (hidden removed, rest ordered) | DIVERGED | DashboardViewModelTest.kt: `dashboard widgets restore saved order` + `dashboard widget remove add and move persist order` | covered only as separate concerns |
| reorderOntoDropTarget > forward drag lands the moved card on the drop target | PORTED | DashboardViewModelTest.kt: `dashboard widget moves to target drop position` | same insert-at-target semantics |
| reorderOntoDropTarget > backward drag lands the moved card on the drop target | PORTED | DashboardViewModelTest.kt: `backward drag lands the moved card on the drop target` | within-carousel drag |
| reorderOntoDropTarget > adjacent drags swap neighbours | PORTED | DashboardViewModelTest.kt: `adjacent drags swap neighbours` | both drag directions land the same way round |
| reorderOntoDropTarget > dropping onto itself or out of range leaves the order untouched | PORTED | DashboardViewModelTest.kt: `dropping onto itself or out of range leaves the order untouched` | also asserts a no-op never rewrites the saved layout |

## test/features/dashboard/dashboard_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModelTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/domain/dashboard/DashboardAggregatorTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the load publishes a precomputed display, not just the data | PORTED | DashboardViewModelTest.kt: `load success populates display widgets` (+ `dashboard daily goals follow preferences`) | display + goals-on-state both asserted, split across two tests |
| the two passes merge, and the display is rebuilt from each | DIVERGED | DashboardViewModelTest.kt: `background dashboard query loads remaining configured widget metrics` | quick/background query split asserted; merged values surviving + display rebuild not (DashboardAggregatorTest `merge derived projection…` covers calories merge only) |
| a stale pass cannot overwrite the day that overtook it | PORTED | DashboardViewModelTest.kt: `newer load wins when navigation requests overlap` | same delayed-stale-load essence |
| a failed day load becomes a ScreenError | PORTED | DashboardViewModelTest.kt: `load failure sets error and clears loading` (+ `load failure with null message uses Unknown error fallback`) | |
| toggling edit mode rebuilds the display without reloading | PORTED | DashboardViewModelTest.kt: `toggling edit mode rebuilds the display without reloading` | both directions; load count unchanged, display still populated |
| hiding a tile drops it from the display and offers it in the tray | DIVERGED | DashboardViewModelTest.kt: `dashboard widget remove add and move persist order` | list membership asserted; display/tray projection not |

## test/features/devicesync/device_sync_screen_test.dart
Kotlin counterpart: none for the wizard screen/view-model (app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/protocol/SyncSessionTest.kt etc. cover the protocol layer only)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| role step offers host and guest options | PORTED | DeviceSyncStepsTest: `theRoleStepOffersHostAndGuest` | Compose instrumentation; runs on a device, not in CI |
| the report step shows a failure, not a success checkmark | PORTED | DeviceSyncStepsTest: `theReportStepShowsAFailureNotASuccessCheckmark`, `aCompletedReportShowsWhatWasImported` | Compose instrumentation; runs on a device, not in CI |
| a finished sync offers to share the report, not only save it | PORTED | DeviceSyncStepsTest: `aFinishedSyncOffersToShareTheReportNotOnlyToCopyIt` (Kotlin has Copy + Share, no Save) | Compose instrumentation; runs on a device, not in CI |
| a connect timeout surfaces a connection message | PORTED | DeviceSyncStepsTest: `aConnectTimeoutSaysTheConnectionFailedRatherThanSomethingGeneric` | Compose instrumentation; runs on a device, not in CI |
| the role step renders a permission error banner | PORTED | DeviceSyncStepsTest: `theRoleStepBannersAnUnavailableRadio` | Compose instrumentation; runs on a device, not in CI |
| the scan step shows a spinner while scanning | PORTED | DeviceSyncStepsTest: `aScanInProgressDoesNotYetOfferARescan` | Compose instrumentation; runs on a device, not in CI |
| a finished scan with no devices offers a rescan | PORTED | DeviceSyncStepsTest: `aFinishedScanWithNoDevicesOffersARescan` | Compose instrumentation; runs on a device, not in CI |

### Counts
PORTED: 55, DIVERGED: 9, MISSING: 0, N/A-WIDGET: 19, N/A-FRAMEWORK: 6

### Missing list

None for the sensor-status mapping. Watch-kind filtering is gone from the BLE
registry; leftover `"kind": "WATCH"` rows are dropped on read. The loader derives
`DashboardData.supportedMetrics` from `managedPermissions`, and the tile mapper
drops what the provider cannot serve and materialises it for the edit-mode add
tray.

The remaining two are deliberate divergences, decided rather than deferred — the
user chose to keep Kotlin's behavior:
- test/features/dashboard/dashboard_app_open_refresh_test.dart: a resume inside the guard interval does not re-read (Kotlin has no resume guard interval; every resume re-reads)
- test/features/dashboard/dashboard_tile_layout_test.dart: applyDashboardTileLayout > includeHidden keeps hidden tiles but still applies order (Kotlin's saved list is authoritative — absent = hidden — instead of Flutter's order+hidden-set pair)
