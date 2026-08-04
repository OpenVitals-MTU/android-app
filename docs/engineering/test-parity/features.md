# Flutter → Kotlin test-parity matrix: features section

Scope: test/features/ minus manualentry/, hydration/, mindfulness/, caffeine/, nutrition/ (owned by another agent) and minus dashboard/, devicesync/, imports/applehealth/, body/, cycle/, recovery/ (persisted separately). Covered here: activity (incl. maps/ and export/), settings, imports/csv + route imports, sleep, heart, vitals, bodyenergy, readiness, achievements, homewidgets, onboarding.

Statuses: PORTED (same logic + equivalent assertions), DIVERGED (covered but weaker/different assertions), MISSING (no Kotlin coverage and portable to a JVM unit test), N/A-WIDGET (widget/golden case with no JVM-portable core), N/A-FRAMEWORK (Flutter-only plumbing, or an Android surface only instrumentation can drive), N/A-BEHAVIOR (the Kotlin behavior itself differs, so a 1:1 test cannot pass without changing production).

## test/features/activity/activities_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivitiesOverviewSectionsTest.kt (over `activityOverviewTotals` / `activityOverviewBuckets` / `limitActivityOverviewBuckets` and the extracted `workoutStatisticsValues` / `activityOverviewStripBuckets` / `activityTypeFilterOptions` seams), plus ActivitiesViewModelTest.kt, ui/charts/PeriodChartTest.kt, domain/insights/MetricInterpretationsTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| key-metric totals > folds steps, distance, energy, cardio load and averages HRV | PORTED | ActivitiesOverviewSectionsTest.kt: `key metric totals fold steps, distance, energy, cardio load and average HRV` | `activityOverviewTotals`/`aggregateCardioLoadConfidence` exist untested |
| key-metric totals > a day with no cardio-load reading is left out of the sum | PORTED | ActivitiesOverviewSectionsTest.kt: `a day with no cardio-load reading is left out of the sum` | also asserts the skipped day still charts as 0.0 |
| buckets > a week is one bucket per day, in date order | PORTED | ActivitiesOverviewSectionsTest.kt: `a week is one bucket per day, in date order` | `activityOverviewBuckets` untested |
| buckets > a year rolls its days up into one bucket per month | PORTED | ActivitiesOverviewSectionsTest.kt: `a year rolls its days up into one bucket per month` | HRV-average half now asserted at the overview series layer |
| buckets > more days than buckets chunk down to the cap (7) | PORTED | ActivitiesOverviewSectionsTest.kt: `more days than buckets chunk down to the cap` | `limitActivityOverviewBuckets` untested |
| buckets > the week strip marks the days that carry a workout — week only | PORTED | ActivitiesOverviewSectionsTest.kt: `the week strip marks the days that carry a workout - week only` | seam activityOverviewStripBuckets |
| statistics > folds the period total, average, longest and previous total | PORTED | ActivitiesOverviewSectionsTest.kt: `statistics fold the period total, average, longest and previous total` | seam workoutStatisticsValues; workoutsByDay has no Kotlin analog |
| statistics > the HHS guideline averages by week on a month or a year | PORTED | ActivitiesOverviewSectionsTest.kt: `the HHS guideline averages by week on a month or a year` | weekCount() 1.0 vs 4.0 and 140 to 35 min |
| statistics > the filter options are the union with the selection, by label | PORTED | ActivitiesOverviewSectionsTest.kt: `the filter options are the union with the selection, by label` | seam activityTypeFilterOptions |
| an empty period derives zeroes and nulls, not a crash | PORTED | ActivitiesOverviewSectionsTest.kt: `an empty period derives zeroes and nulls, not a crash` | no aggregate empty-period derivation test |

## test/features/activity/activities_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivitiesViewModelTest.kt, ActivityMetricsTest.kt, core/presentation/MetricDetailSectionOrderViewModelTest.kt, ActivitiesOverviewSectionsTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the Kotlin activities sections from a period | PORTED | ActivitiesOrderedContentTest: `aPeriodRendersItsWorkoutsAndTheKeyMetricCards` | Compose instrumentation; runs on a device, not in CI |
| the activity-type filter narrows the workout list | PORTED | ActivitiesViewModelTest.kt: `selectActivityType filters loaded activities without changing selected period`, `selectActivityType all restores loaded activities` | logic covered at view-model level |
| key-metric cards navigate to their metric targets | PORTED | ActivitiesOrderedContentTest: `theStepsKeyMetricCardOpensTheStepsMetric` | Compose instrumentation; runs on a device, not in CI |
| the cardio-load card opens the cardio-load detail | PORTED | ActivitiesOrderedContentTest: `theCardioLoadCardOpensTheCardioLoadDetail` | Compose instrumentation; runs on a device, not in CI |
| the goal steppers move and persist the workout goal | PORTED | ActivitiesViewModelTest.kt: `moving the daily goal re-derives the goal progress` + `decreasing the daily goal stops at the floor` | `increaseDailyGoal`/step-5/persist logic untested in Kotlin |
| a pause segment shortens moving duration and speeds moving pace | DIVERGED | ActivityMetricsTest.kt: `moving duration excludes pause segments`; ActivitiesViewModelTest.kt: `loaded activities expose aggregate stats by activity type` | per-type aggregate moving-speed comparison not asserted |
| best speed takes the max of avg speed and distance/moving duration | DIVERGED | ActivitiesViewModelTest.kt: `loaded activities expose aggregate stats by activity type` | only the recorded-average branch; derived-wins, either-alone, null and group-max branches unasserted |
| only the current window is loaded with the per-session route metrics | PORTED | ActivitiesViewModelTest.kt: `only the current window pays for the per-session route metrics` | expensive read exactly once; previous/baseline take the cheap loadWorkouts |
| the key-metric sparkline renders weekday label rows for a week | PORTED | ActivitiesOrderedContentTest: `theKeyMetricSparklineLabelsEveryWeekdayBucket` | Compose instrumentation; runs on a device, not in CI |
| the section order persists across notifier instances | DIVERGED | MetricDetailSectionOrderViewModelTest.kt: `moveSectionToTarget_reordersAndPersists`, `initialOrder_usesDefaultWhenPreferencesMissing` | persist call verified; re-read by a fresh instance not asserted |
| the week strip marks the day you trained and rings the rest | PORTED | ActivitiesOverviewSectionsTest.kt: `the week strip marks the days that carry a workout - week only` + `marker is empty when day has movement metrics but no workout` | only the rest-ring half of the marker rule |

## test/features/activity/activities_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivitiesViewModelTest.kt (partial)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | DIVERGED | ActivitiesViewModelTest.kt: `loaded activities expose aggregate stats by activity type` | Kotlin has no precomputed display; state-population asserted, totals/chart values not |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | ActivitiesViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | toScreenError() now maps Health Connect's SecurityException to ScreenError.PermissionDenied; error type, cleared loading and empty workouts asserted |
| an unexpected failure carries its message to the screen | PORTED | ActivitiesViewModelTest.kt: `an unexpected failure carries its message to the screen` | asserted on sibling ActivityViewModel, not ActivitiesViewModel |
| the type filter re-slices the display without reloading | PORTED | ActivitiesViewModelTest.kt: `the type filter re-slices the loaded result without reloading` | load count asserted exactly 1 |
| moving the daily goal re-derives the goal progress | PORTED | ActivitiesViewModelTest.kt: `moving the daily goal re-derives the goal progress` | 30 to 60 in six steps, all persisted, 0 reloads, goalMetDays 1 to 0 |
| a stale load cannot overwrite the newer one it lost to | PORTED | ActivitiesViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | gated repo; the superseded week answer is dropped |

## test/features/activity/activity_detail_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityDetailDisplayTest.kt (elevation profile, pace scale, split speed trace), plus ActivityMetricsTest.kt (cadence kinds, paused/moving) and ActivityDetailViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a pause segment splits the session into paused and moving time | PORTED | ActivityMetricsTest.kt: `moving duration excludes pause segments` | pausedDurationMs now asserted alongside movingDurationMs |
| an unpaused session is all moving time | PORTED | ActivityMetricsTest.kt: `an unpaused session is all moving time` | full-duration-moving asserted only via aggregates |
| only the cadence kinds that recorded something get a card | PORTED | ActivityMetricsTest.kt: `only the cadence kinds that recorded something get a card` | seam activityCadenceKinds |
| the pace scale is the slowest and fastest split, per kilometre | PORTED | ActivityDetailDisplayTest.kt: `the pace scale is the slowest and fastest split, per kilometre` | `slowest/fastestSplitPaceSeconds` untested except estimated-equality |
| a split with no distance leaves the scale unset, not zeroed | PORTED | ActivityDetailDisplayTest.kt: `a split with no distance leaves the scale unset, not zeroed` |  |
| a workout with no route has no route distance | N/A-FRAMEWORK | none | no routeDistanceMeters on the Kotlin detail display; the route card reads workout.totalDistanceMeters or the split sum |
| the elevation profile > comes from the route altitudes, oldest first | PORTED | ActivityDetailDisplayTest.kt: `the elevation profile comes from the route altitudes, oldest first` | `elevationProfile()` untested |
| the elevation profile > skips the points the device gave no height for | PORTED | ActivityDetailDisplayTest.kt: `the elevation profile skips the points the device gave no height for` | RouteElevationTest.kt covers null-skip for GAIN, not the profile samples |
| the elevation profile > one height is not a profile | PORTED | ActivityDetailDisplayTest.kt: `one height is not an elevation profile` |  |
| the elevation profile > a route with no altitude at all has no profile | PORTED | ActivityDetailDisplayTest.kt: `a route with no altitude at all has no elevation profile` |  |
| the elevation profile > an activity with no route has no profile | PORTED | ActivityDetailDisplayTest.kt: `an activity with no route has no elevation profile` |  |
| speed rebuilt from the splits > a split holds its speed across its window: the trace is a step | PORTED | ActivityDetailDisplayTest.kt: `a split holds its speed across its window - the trace is a step` | `splitSpeedTrace()` exists in Kotlin, untested |
| speed rebuilt from the splits > the average is distance over time — NOT the mean of the plotted points | PORTED | ActivityDetailDisplayTest.kt: `the average is distance over time, NOT the mean of the plotted points` | harmonic-vs-arithmetic assertion included |
| speed rebuilt from the splits > a recorded trace wins — a measurement beats a reconstruction | PORTED | ActivityDetailDisplayTest.kt: `a recorded trace wins - a measurement beats a reconstruction` |  |
| speed rebuilt from the splits > estimated splits draw NOTHING: they are flat by construction | PORTED | ActivityDetailViewModelTest.kt: `load cuts splits at the preferred distance` | asserts `splitSpeedTrace == null` for estimated source |
| speed rebuilt from the splits > one split is an average, not a trace | PORTED | ActivityDetailDisplayTest.kt: `one split is an average, not a trace` |  |
| speed rebuilt from the splits > device laps get a trace too | PORTED | ActivityDetailDisplayTest.kt: `device laps get a trace too` |  |
| speed rebuilt from the splits > a lap with no distance or no time is skipped, not drawn at zero | PORTED | ActivityDetailDisplayTest.kt: `a lap with no distance or no time is skipped, not drawn at zero` | 800/185 average asserted |

## test/features/activity/activity_detail_metrics_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityDetailViewModelTest.kt, ActivityMetricRelevanceTest.kt, domain/model/ActivityBackfillTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a cycling activity > renders the recorded speed and cadence it used to drop | PORTED | ActivityDetailViewModelTest.kt: `initial load backfills missing averages from samples` | sample-backfilled averages asserted; chart presence itself is widget-only |
| a cycling activity > does not offer steps, floors or wheelchair pushes | PORTED | ActivityMetricRelevanceTest.kt: `bike ride does not advertise steps floors or wheelchair pushes`, `hardware bound metrics are never relevant when absent` | |
| a cycling activity > surfaces cadence and power averages on the metric rows | DIVERGED | UnitFormatterTest.kt: `cadence uses rpm` | row wiring and power/recorded-speed formatting unasserted |
| a cycling activity > a failing cadence read costs the card, not the screen | PORTED | ActivityDetailViewModelTest.kt: `a failing cadence read costs the card, not the screen` | fixed: the cadence read now degrades to empty inside its own runCatching |
| a running activity > gets step cadence in steps per minute, not revolutions | N/A-BEHAVIOR | | blocked on behavior decision - Kotlin formats all cadence as rpm, so there is no steps-per-minute unit to assert |
| a running activity > shows pace and steps, and no crank | PORTED | ActivityMetricRelevanceTest.kt: `run prefers pace and walks the step based sets` | wheelchair-pushes and floors negatives added |
| a recorded value is shown even when the type says it is irrelevant | PORTED | ActivityMetricRelevanceTest.kt: `a recorded value is shown even when the type says it is irrelevant` | seam showsMetricRow |
| a walking activity recorded by a watch > shows the totals its session record never carried | N/A-BEHAVIOR | none | blocked on behavior decision - Kotlin has no window-aggregate session-metrics backfill at all (same gap as the activity_backfill rows in domain-data-core.md) |
| a walking activity recorded by a watch > derives a distance from speed when no distance was written | PORTED | ActivityDetailViewModelTest.kt: `derives a distance from speed when no distance was written` + `a recorded distance is never overwritten by the derived one` |  |
| a walking activity recorded by a watch > a failing metrics read costs the numbers, not the screen | PORTED | ActivityDetailViewModelTest.kt: `a failing marker read costs the marks, not the screen` | Kotlin folds the session metrics into loadWorkout, so the marker read is the degrading read that stands in for Flutter's separate metrics read; it too is guarded now |
| a strength session shows no distance metrics at all | PORTED | ActivityMetricRelevanceTest.kt: `a strength session reports none of the distance metrics` | trait covered; per-metric relevance for strength unasserted |

## test/features/activity/activity_detail_splits_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/ActivitySplitsTest.kt, features/activity/ActivityDetailViewModelTest.kt, domain/preferences/ActivitySplitDistanceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| device laps render as "Laps", credited to the recording device | PORTED | ActivitySplitsTest.kt: `device laps win over a route, and are NOT re-cut to the split distance`, `uneven device laps keep their own lengths` | header copy widget-only |
| a GPS route renders as auto-derived splits, with the split distance in the header | PORTED | ActivitySplitsTest.kt: `cut at exactly the right distance, with the crossing time INTERPOLATED between fixes`, `average heart rate covers only the samples inside the split window` | |
| the split-distance preference drives the header and the cuts | PORTED | ActivityDetailViewModelTest.kt: `load cuts splits at the preferred distance`, `split distance preference change re-cuts splits without reloading`; ActivitySplitsTest.kt: `a custom split distance (5 km, the cyclist case) is honoured` | |
| speed samples (no route) still derive real splits | PORTED | ActivitySplitsTest.kt: `integrate v dt to cut at the right times, with no route at all` | |
| with no route and no speed samples the card says ESTIMATED and explains why | PORTED | ActivitySplitsTest.kt: `every estimated split shares the activity average pace, and the source says so` | explanatory copy widget-only |
| a failing speed read degrades to estimated splits instead of blowing up the screen | PORTED | ActivityDetailViewModelTest.kt: `a failing speed read degrades to estimated splits instead of blowing up the screen` | fixed: loadSpeedSamples degrades to empty, so the splits fall back to ESTIMATED |
| an activity with no distance hides the card entirely | PORTED | ActivitySplitsTest.kt: `a session with no distance, no route and no speed has no splits`; ActivityDetailViewModelTest.kt: `non distance activity yields no splits` | |
| imperial units re-express the derived header in miles | PORTED | ActivitySplitDistanceLabelTest.kt: `imperial units re-express the derived header in miles` + `metric presets print as the round numbers the user picked` + `every imperial preset labels as a round mile fraction` | splitDistanceLabel composition now covered |

## test/features/activity/activity_detail_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityDetailViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| deletes an owned activity and invokes the pop callback | PORTED | ActivityDetailViewModelTest.kt: `deleteActivity deletes OpenVitals activity and reports completion` | |
| does not delete a foreign activity | PORTED | ActivityDetailViewModelTest.kt: `deleteActivity ignores workout not created by OpenVitals` | |
| keeps the screen and records the error when the delete fails | PORTED | ActivityDetailViewModelTest.kt: `keeps the screen and records the error when the delete fails` | delete-failure path untested |

## test/features/activity/activity_heart_rate_recovery_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/HeartRateRecoveryTest.kt, features/activity/ActivityDetailViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| LoadActivityDetailUseCase heart-rate recovery > an ordinary workout with no cessation mark is not measured | PORTED | ActivityDetailViewModelTest.kt: `no recovery window issues no extra heart rate read`; HeartRateRecoveryTest.kt: `a session with no rest segment has no recovery window` | |
| LoadActivityDetailUseCase heart-rate recovery > a guided test reads the recovery on its own window | PORTED | ActivityDetailViewModelTest.kt: `recovery window issues its own heart rate read and exposes the reading` | exact window bounds asserted |
| LoadActivityDetailUseCase heart-rate recovery > measures the fall for a strap that kept recording | PORTED | HeartRateRecoveryTest.kt: `a chest strap at 1Hz measures every mark and reads clean` | |
| LoadActivityDetailUseCase heart-rate recovery > a failed recovery read costs the card, not the screen | PORTED | ActivityDetailViewModelTest.kt: `a failed recovery read costs the card, not the screen` | fixed: loadHeartRateRecovery is guarded, leaving a null reading and the rest of the detail intact |
| LoadActivityDetailUseCase heart-rate recovery > the observed maximum is used, and is not an estimate | PORTED | HeartRateRecoveryTest.kt: `the same peak against a KNOWN max is submaximal (tighter band)` (asserts maxUsed + estimated=false), `an observed max below the trust bar is not used as a maximum` | Kotlin additionally keeps an explicit-max rung Flutter removed |
| LoadActivityDetailUseCase heart-rate recovery > with no observed maximum it falls back to the age estimate | PORTED | HeartRateRecoveryTest.kt: `the age formula is Tanaka (208 - 0,7 x age), flagged estimated` | |
| ActivityHeartRateRecoveryCard > shows the fall after a guided test | PORTED | HeartRateRecoveryCardTest: `showsTheFallAfterAGuidedTest` | Compose instrumentation; runs on a device, not in CI |
| ActivityHeartRateRecoveryCard > a watch that stopped recording says so, and shows dashes rather than numbers | PORTED | HeartRateRecoveryCardTest: `aWatchThatStoppedRecordingSaysSoRatherThanShowingNumbers` | Compose instrumentation; runs on a device, not in CI |
| ActivityHeartRateRecoveryCard > an ordinary workout shows no card at all | PORTED | ActivityDetailRecoveryCardGateTest: `anOrdinaryWorkoutShowsNoCardAtAll` | Compose instrumentation; runs on a device, not in CI |
| ActivityHeartRateRecoveryCard > a submaximal guided test still shows the card, flagged | PORTED | HeartRateRecoveryCardTest: `aSubmaximalGuidedTestStillShowsTheCardFlagged` | Compose instrumentation; runs on a device, not in CI |
| ActivityHeartRateRecoveryCard > with no maximum knowable at all, the card appears and asks for one | PORTED | HeartRateRecoveryCardTest: `withNoMaximumKnowableTheCardAppearsAndAsksForOne` | Compose instrumentation; runs on a device, not in CI |

## test/features/activity/activity_metric_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityPresentationMapperTest.kt, ActivityViewModelTest.kt, domain/insights/DailyGoalsTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| steps > sums values and counts only the days with movement | PORTED | ActivityPresentationMapperTest.kt: `steps display sums values and counts only the days with movement` | total, best, trackedDates, sampleCount and activeDays all asserted |
| steps > the daily average divides by active days, not calendar days | PORTED | ActivityPresentationMapperTest.kt: `the daily average divides by active days, not calendar days` | asserted only for the calories path (CaloriesDerivationsTest), not the metric display |
| steps > compares against the previous period total | PORTED | ActivityPresentationMapperTest.kt: `steps display compares against the previous period total` | comparison direction covered at domain level; mapper previousTotal wiring untested |
| steps > goal progress counts the days that reached the target | PORTED | ActivityPresentationMapperTest.kt: `steps display computes goal progress`; DailyGoalsTest.kt: `at least goals count tracked days and streaks` | |
| steps > a week with no rows has no data; a day always does | PORTED | ActivityPresentationMapperTest.kt: `a week with no rows has no data, a day always does` | "a day always has data" half unasserted |
| sample count > a day is described by its intraday samples | PORTED | ActivityPresentationMapperTest.kt: `a day is described by its intraday samples` | sampleCount/dayTotal derivation untested |
| sample count > a longer period is described by its active days | PORTED | ActivityPresentationMapperTest.kt: `steps display sums values and counts only the days with movement` | zero-day-excluded sampleCount now asserted |
| sample count > intraday points are dropped for a metric the device never sampled | PORTED | ActivityPresentationMapperTest.kt: `intraday points are dropped for a metric the device never sampled` |  |
| per-metric slices > calories burned reads the nutrition slice, not daily steps | PORTED | ActivityPresentationMapperTest.kt: `calories burned reads the nutrition slice, not daily steps` | contrasting dailySteps row and previousTotal asserted |
| per-metric slices > a nullable metric has no data until a row actually carries it | PORTED | ActivityPresentationMapperTest.kt: `a nullable metric has no data until a row actually carries it` | essence at state level; mapper hasData/activeDays unasserted |
| per-metric slices > steps and distance always have data when rows exist | DIVERGED | ActivityPresentationMapperTest.kt: `steps has data whenever rows exist, distance needs a positive one` | deliberate contract difference - Kotlin distance hasData requires a positive row; asserted explicitly rather than changing behavior |
| per-metric slices > each metric reads its own column | PORTED | ActivityPresentationMapperTest.kt: `each metric reads its own column` | all six columns |
| every metric maps to its own goal key | PORTED | ActivityPresentationMapperTest.kt: `every metric maps to its own goal key` | goal-key uniqueness untested |

## test/features/activity/activity_metric_relevance_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityMetricRelevanceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| cycling > reports the absence of the metrics a bike ride can produce | PORTED | ActivityMetricRelevanceTest.kt: `bike ride does not advertise steps floors or wheelchair pushes` + `universal metrics are relevant for every exercise type` | |
| cycling > stays silent about metrics a bike ride can never produce | PORTED | ActivityMetricRelevanceTest.kt: `bike ride does not advertise steps floors or wheelchair pushes`, `hardware bound metrics are never relevant when absent` | |
| sensor-only metrics earn a row by being recorded, never by being missing | PORTED | ActivityMetricRelevanceTest.kt: `hardware bound metrics are never relevant when absent` | 3 metrics x 4 exercise types |
| running > reports steps and pace | PORTED | ActivityMetricRelevanceTest.kt: `run prefers pace and walks the step based sets` | distance and elevation added |
| running > stays silent about the crank | PORTED | ActivityMetricRelevanceTest.kt: `run prefers pace and walks the step based sets` | wheelchair-pushes and floors negatives unasserted |
| indoor activities do not report a missing elevation gain | PORTED | ActivityMetricRelevanceTest.kt: `indoor activities do not report missing elevation` | stationary bike, rowing machine, pool and the outdoor contrast added |
| floors climbed belongs to stair climbing and nothing else | PORTED | ActivityMetricRelevanceTest.kt: `single purpose metrics belong to their single exercise` | negatives for running, biking and wheelchair added |
| wheelchair pushes belong to wheelchair and nothing else | PORTED | ActivityMetricRelevanceTest.kt: `single purpose metrics belong to their single exercise` | steps-irrelevant-for-wheelchair added |
| a strength session reports none of the distance metrics | PORTED | ActivityMetricRelevanceTest.kt: `a strength session reports none of the distance metrics` | only the distance-based trait covered (ExerciseTypeTraitsTest); per-metric relevance unasserted |
| an unknown exercise type reports the universal absences and nothing invented | PORTED | ActivityMetricRelevanceTest.kt: `an unknown exercise type reports the universal absences and nothing invented` | universal positives asserted for unknown type; invented-metric negatives unasserted |

## test/features/activity/activity_route_export_buttons_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityRouteExportTest.kt (writers/filenames only)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| route card offers open-in-map, GPX/KMZ save and GPX/KMZ share actions | PORTED | ActivityRouteExportButtonsTest: `aRouteOffersOpenInMapAndBothSaveAndShareFormats` | Compose instrumentation; runs on a device, not in CI |
| no route means no export actions | PORTED | ActivityRouteExportButtonsTest: `aWorkoutWithoutARouteOffersNoExportAtAll` | Compose instrumentation; runs on a device, not in CI |

## test/features/activity/activity_screens_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityViewModelTest.kt, ActivityPresentationMapperTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Steps screen renders the Kotlin sections once loaded | PORTED | ActivityMetricSectionsTest: `stepsPeriodRendersTheChartGoalStatisticsConfidenceAndEntries` | Compose instrumentation; runs on a device, not in CI |
| the goal steppers move and persist the daily goal | PORTED | ActivityViewModelTest.kt: `the goal steppers move and persist the daily goal` + `the daily goal stops at its floor and its ceiling` + `moving the goal re-derives the goal progress without reloading` | 8000 default, 500 step, every move persisted |
| {steps,distance,caloriesOut,activeCalories,floors,elevation,wheelchair} screen renders the shared sections (7 cases) | PORTED | ActivityMetricSectionsTest: one test per metric (`steps…`, `distance…`, `caloriesBurned…`, `activeCalories…`, `floors…`, `elevation…`, `wheelchairPushes…RendersTheSharedSections`) | Compose instrumentation; runs on a device, not in CI |
| the day range shows the intraday chart, not the period chart | DIVERGED | ActivityViewModelTest.kt: `load for DAY range calls loadActivityProgress`, `load for WEEK range returns empty activityProgress` | data-selection logic covered; chart-type swap not asserted |
| Steps screen shows the access gate when permission missing | N/A-WIDGET | | permission-gate rendering |
| Steps screen shows the empty placeholder with no data | PORTED | ActivityMetricSectionsTest: `stepsWithNoDataSaysSoInsteadOfDrawingAnEmptyChart` | Compose instrumentation; runs on a device, not in CI |

## test/features/activity/calories_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/CaloriesDerivationsTest.kt, ActivityPresentationMapperTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| each series totals its own slice | PORTED | CaloriesDerivationsTest.kt: `statistics ignore days whose calories carry no data`, `statistics average over the days that reported, not the days in the window`; ActivityPresentationMapperTest.kt: `calories burned display populates values for week period` | |
| all-zero readings are no data — that is what shows the placeholder | DIVERGED | ActivityPresentationMapperTest.kt: `calories burned display has no data when nutrition has no burned calories` | active-calories all-zero half unasserted |
| an empty period derives empty series, not nulls | DIVERGED | CaloriesDerivationsTest.kt: `statistics are null for empty input rather than zero` | Kotlin's emptiness contract is the opposite (null-not-zero vs empty-not-null) |
## test/features/activity/export/activity_route_export_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/ActivityRouteExportTest.kt

Note: this Flutter file is itself documented as "Port of the Kotlin ActivityRouteExportTest" but has since grown 5 extra cases the Kotlin test never gained. All the underlying Kotlin functions exist in app/src/main/kotlin/tech/mmarca/openvitals/features/activity/ActivityRouteExport.kt.

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| kmz export writes parseable route with metadata | PORTED | ActivityRouteExportTest.kt: `kmz export writes parseable route with metadata` | Identical round-trip through RouteFileParser, same fixture values and elevation assertion (8.0 ± 0.001) |
| gpx export writes parseable route with metadata | PORTED | ActivityRouteExportTest.kt: `gpx export writes parseable route with metadata` | seam - defaulted XmlSerializer param; round-trips through RouteFileParser |
| kmz escapes markup in title and notes | PORTED | ActivityRouteExportTest.kt: `kmz escapes markup in title and notes` | `routeXmlEscaped` in ActivityRouteExport.kt untested; KML path is pure string building, JVM-portable |
| route export file names use selected format extension | PORTED | ActivityRouteExportTest.kt: `route export file names use selected format extension` | Same prefix/extension assertions |
| blank title falls back to activity-route | PORTED | ActivityRouteExportTest.kt: `blank title falls back to activity-route` | `sanitizeRouteFileName` fallback untested, pure JVM |
| sorted points require a non-empty route | PORTED | ActivityRouteExportTest.kt: `sorted points require a non-empty route` | seam private to internal; Kotlin require() throws IllegalArgumentException where Dart throws StateError |
| sorted points order by time | PORTED | ActivityRouteExportTest.kt: `sorted points order by time` | `sortedBy { it.time }` untested |

## test/features/activity/export/activity_route_sharing_test.dart
Kotlin counterpart: none (sharing implemented as `Context.shareActivityRoute` Intent/FileProvider plumbing in ActivityRouteExport.kt; no Kotlin test)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ActivityRouteSharing > sharing a route hands the sheet a real GPX file | N/A-FRAMEWORK | — | Kotlin path is ACTION_SEND Intent + FileProvider, no JVM seam; export bytes covered by ActivityRouteExportTest |
| ActivityRouteSharing > the shared file carries the format mime type so the target app recognises it | N/A-FRAMEWORK | — | `Intent.type = format.mimeType` wiring; mime constants exist on ActivityRouteExportFormat but the wiring needs instrumentation |
| ActivityRouteSharing > the chooser title and the email subject reach the share sheet | N/A-FRAMEWORK | — | Kotlin uses `Intent.createChooser` + string resource; no subject concept |
| ActivityRouteSharing > a route with no points fails instead of sharing an empty file | PORTED | ActivityRouteExportTest.kt: `sorted points require a non-empty route` | the require() is the whole essence; the Intent half stays N/A-FRAMEWORK |
| ActivityRouteSharing > a share sheet with no target surfaces as a failure to the caller | N/A-FRAMEWORK | — | `runCatching` around `startActivity`; platform plumbing |
| ActivityRouteExportCache > a staged export is named for the activity and its start time | DIVERGED | ActivityRouteExportTest.kt: `route export file names use selected format extension` | Filename covered; staging into cacheDir/route_exports (`createActivityRouteExportFile`, Context-bound) untested |
| ActivityRouteExportCache > a staged export holds the format's own bytes | DIVERGED | ActivityRouteExportTest.kt: `gpx export writes parseable route with metadata` + `kmz export writes parseable route with metadata` | both formats' bytes now covered; staging into cacheDir/route_exports remains Context-bound and untested |

## test/features/activity/maps/mapsforge_label_geometry_test.dart
Kotlin counterpart: none

The Kotlin app renders Mapsforge maps with the native `org.mapsforge` `TileRendererLayer` (MapsforgeRouteMap.kt), which draws labels inside the library; the Flutter label-layer geometry (visibleLabelTileRange / labelPaintTransform) is a Flutter-only re-implementation that has no Kotlin analog.

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| visibleLabelTileRange > a viewport sitting exactly on one tile reads that tile plus a ring | N/A-FRAMEWORK | — | Label layout done natively by mapsforge library in Kotlin |
| visibleLabelTileRange > a viewport spanning several tiles covers all of them | N/A-FRAMEWORK | — | |
| visibleLabelTileRange > a fractional zoom reads the tiles of the rounded zoom level | N/A-FRAMEWORK | — | |
| visibleLabelTileRange > a viewport at the edge of the world clamps to real tiles | N/A-FRAMEWORK | — | |
| visibleLabelTileRange > ranges compare by value, so panning within one tile reads once | N/A-FRAMEWORK | — | |
| labelPaintTransform > the reference is the map pixel at the centre of the screen, not its corner | N/A-FRAMEWORK | — | |
| labelPaintTransform > half a zoom level in, the labels scale with the tiles under them | N/A-FRAMEWORK | — | |
| labelPaintTransform > the map pixel under the top-left corner is drawn at the top-left corner | N/A-FRAMEWORK | — | |
| labelPaintTransform > the map pixel under the bottom-right corner is drawn at the bottom-right corner | N/A-FRAMEWORK | — | |
| labelPaintTransform > panning moves a label by exactly what the map moved under it | N/A-FRAMEWORK | — | |

## test/features/activity/maps/mapsforge_tile_renderer_test.dart
Kotlin counterpart: none

The tile-render coordination (single reader warm-up, render concurrency cap, byte-budget cache, empty-tile memoization) is a Flutter re-implementation; Kotlin gets all of this from native `TileRendererLayer` + `AndroidUtil.createTileCache` (MapsforgeRouteMap.kt:188-211).

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| twenty concurrent tile requests warm the renderer exactly once | N/A-FRAMEWORK | — | Fix for a mapsforge_flutter isolate bug; no Kotlin analog |
| a second request for the same tile is served from the cache and never re-renders | N/A-FRAMEWORK | — | Native tile cache in Kotlin |
| the renderer never has more than four renders in flight | N/A-FRAMEWORK | — | |
| concurrent requests for the same tile share one render | N/A-FRAMEWORK | — | |
| evicting a cached tile disposes the master but not an image already handed out | N/A-FRAMEWORK | — | dart:ui image lifecycle |
| a tile the packs do not cover is rendered once and then remembered as empty | N/A-FRAMEWORK | — | |
| a renderer failure surfaces as an empty tile, never as a thrown map | N/A-FRAMEWORK | — | |
| a failed tile is retried rather than remembered as empty | N/A-FRAMEWORK | — | |
| disposing the renderer drops its cached tiles and answers null | N/A-FRAMEWORK | — | |

## test/features/activity/maps/multimap_merge_policy_test.dart
Kotlin counterpart: none

The same one-word policy choice exists in Kotlin: `MultiMapDataStore(MultiMapDataStore.DataPolicy.DEDUPLICATE)` at MapsforgeRouteMap.kt:195. The mapsforge datastore/reader classes are pure Java, so this contract suite is JVM-portable — classified MISSING, not N/A.

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a tile at the seam draws data from whichever pack actually holds it | PORTED | MultiMapMergePolicyTest.kt: `a tile at the seam draws data from whichever pack actually holds it` | contract test over MultiMapDataStore(DEDUPLICATE) with a hand-written MapDataStore fake |
| every pack whose bounding box covers a tile is read, not just the first | PORTED | MultiMapMergePolicyTest.kt: `every pack whose bounding box covers a tile is read, not just the first` | contract test over MultiMapDataStore(DEDUPLICATE) with a hand-written MapDataStore fake |
| a tile only one pack covers reads only that pack | PORTED | MultiMapMergePolicyTest.kt: `a tile only one pack covers reads only that pack` | contract test over MultiMapDataStore(DEDUPLICATE) with a hand-written MapDataStore fake |
| a tile no pack covers stays empty rather than drawing a blank | PORTED | MultiMapMergePolicyTest.kt: `a tile no pack covers stays empty rather than drawing a blank` | contract test over MultiMapDataStore(DEDUPLICATE) with a hand-written MapDataStore fake |

## test/features/activity/maps/offline_map_import_controller_test.dart
Kotlin counterpart: (partial) app/src/test/kotlin/tech/mmarca/openvitals/features/activity/maps/OfflineMapMetadataStoreTest.kt, OfflineMapPackFormatTest.kt — the import flow itself lives untested in app/src/main/kotlin/.../maps/OfflineMapRepository.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| imports a pmtiles pack, copies the file and records metadata | DIVERGED | OfflineMapMetadataStoreTest.kt: `write and read preserves imported maps and active format` | Only metadata persistence covered; the copy/progress/active-format-on-import path (`OfflineMapRepository.importMap`, ContentResolver-bound) untested |
| rejects unsupported file extensions | DIVERGED | OfflineMapPackFormatTest.kt: `detects supported offline map file extensions` | Format detection (null for .osm.pbf) covered; the import failure path itself untested |
| rejects a mapsforge pack that is not a valid map file | PORTED | OfflineMapRepositoryTest.kt: `rejects a mapsforge pack that is not a valid map file` | drives the real importMap over a mocked ContentResolver; asserts the cleanup too |
| deleteMap removes the file and its metadata entry | PORTED | OfflineMapRepositoryTest.kt: `deleteMap removes the file and its metadata entry` | — |

## test/features/activity/maps/offline_map_metadata_store_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/maps/OfflineMapMetadataStoreTest.kt, OfflineMapPackFormatTest.kt (Flutter file is documented as ported from both)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| write then read preserves imported maps sorted newest-first | PORTED | OfflineMapMetadataStoreTest.kt: `write and read preserves imported maps and active format` | |
| read drops missing map files and clears active format | PORTED | OfflineMapMetadataStoreTest.kt: `read drops missing map files and clears active format` | |
| preserves mapsforge format and .map extension in the path | PORTED | OfflineMapMetadataStoreTest.kt: `write and read preserves mapsforge format and map extension` | |
| migrates legacy activeMapId to activeFormat | PORTED | OfflineMapMetadataStoreTest.kt: `read migrates old active map id to active format` | Same legacy JSON fixture |
| detects supported offline map file extensions | PORTED | OfflineMapPackFormatTest.kt: `detects supported offline map file extensions` | Identical four assertions |

## test/features/activity/maps/offline_map_style_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/maps/OfflineMapStyleTest.kt (the expansion was extracted from `offlineMapStyleJson` into the pure `expandPmtilesStyle` in OfflineRouteMap.kt; same asset bundled at app/src/main/assets/offline_maps/protomaps_base_style.json)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| expands one source and one templated-layer copy per pack | PORTED | OfflineMapStyleTest.kt: `expands one source and one templated-layer copy per pack` | seam added: `expandPmtilesStyle(root, packFileUrls)` takes the file URLs, so no AssetManager or android.net.Uri is needed |
| bundled style asset parses and only references the template source | PORTED | OfflineMapStyleTest.kt: `bundled style asset parses and only references the template source` | resolves the asset from either the module or the repo root working directory |

## test/features/activity/maps/route_geometry_test.dart
Kotlin counterpart: app/src/test/kotlin/.../maps/OfflineRouteGeoJsonTest.kt (segments); distance covered piecemeal by RouteFileParserTest / ActivityRecordingSplitsTest; bounds/simplification delegated to MapLibre in Kotlin

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| routeSegments > break indexes split the route into separate segments | PORTED | OfflineRouteGeoJsonTest.kt: `break indexes split the route into separate segments, tail included` | the 1-point tail segment now asserted |
| routeSegments > invalid break indexes are ignored | PORTED | OfflineRouteGeoJsonTest.kt: `invalid route break indexes are ignored` | Identical inputs ([0, 99]) and assertion |
| routeSegments > non-finite coordinates are dropped | PORTED | OfflineRouteGeoJsonTest.kt: `non-finite coordinates are dropped` | `routeSegments` finite-filter (OfflineRouteGeoJson.kt:38) untested, pure JVM |
| RouteBounds.fromPoints > computes the tightest box over finite points | N/A-FRAMEWORK | — | Kotlin delegates bounds to MapLibre `LatLngBounds.Builder` inside view-bound `fitCamera` (OfflineRouteMap.kt:379) |
| RouteBounds.fromPoints > single repeated point is flagged | N/A-FRAMEWORK | — | Single-point branch (`SinglePointZoom`) lives in MapLibre view code |
| RouteBounds.fromPoints > returns null when there are no finite points | N/A-FRAMEWORK | — | `fitCamera` size-0 branch, view-bound |
| routeTotalDistanceMeters > sums haversine distance between consecutive points | PORTED | OfflineRouteGeoJsonTest.kt: `route distance sums haversine distance between consecutive points` | exact haversine sum to 1e-6 |
| routeTotalDistanceMeters > does not bridge across a route break | PORTED | OfflineRouteGeoJsonTest.kt: `route distance does not bridge across a route break` | exact per-segment sum |
| routeTotalDistanceMeters > is zero for a single point | PORTED | OfflineRouteGeoJsonTest.kt: `route distance is zero for a single point` | Trivial JVM assert on `routeDistanceMeters`/`activityRecordingRouteDistanceMeters`, uncovered |
| simplifyRoutePoints > a route below the display cap is returned unchanged, and identical | N/A-FRAMEWORK | — | Display-time decimation exists only for flutter_map; Kotlin renders full track via MapLibre/mapsforge (import-time cap separately covered by RouteFileParserTest `parse simplifies very large route files`) |
| simplifyRoutePoints > a route longer than the display cap keeps its first and last point | N/A-FRAMEWORK | — | Same — no display decimation in Kotlin |
| simplifyRoutePoints > decimating for the map never changes the distance the screen reports | N/A-FRAMEWORK | — | Same |
| buildRouteMapGeometry > segments are split at breaks and projected for drawing | PORTED | OfflineRouteGeoJsonTest.kt: `segments are split at breaks and keep their own coordinates` | coordinate values now checked; projection stays MapLibre-side |
| buildRouteMapGeometry > a route with no finite points yields no bounds and no markers | PORTED | OfflineRouteGeoJsonTest.kt: `a route with no finite points yields no line and no markers` | portable half only; camera bounds stay MapLibre-side |
| buildRouteMapGeometry > the current point widens the camera bounds beyond the route | N/A-FRAMEWORK | — | `fitCamera` includes currentPoint in cameraPoints, but that is MapLibre view code |

## test/features/activity/maps/route_map_view_test.dart
Kotlin counterpart: none (Compose + MapLibre/Mapsforge views; GeoJSON feature-building separately covered by OfflineRouteGeoJsonTest.kt)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders a polyline route without fetching network tiles | PORTED | RoutePreviewTest: `aRecordedRouteDraws` | Compose instrumentation; runs on a device, not in CI |
| default (offline) render draws no tile layer and fetches no network tiles | PORTED | OfflineRouteMapTest: `withNoImportedPackNoMapViewIsBuiltAndNoRecenterControlIsOffered` + OfflineMapNetworkPolicyTest (JVM, config layer) | Compose instrumentation; runs on a device, not in CI |
| shows no recenter control by default | PORTED | OfflineRouteMapTest: `anImportedPackRendersAMapViewAndStillHidesTheRecenterControlByDefault` | Compose instrumentation; runs on a device, not in CI |
| recenter control re-fits the camera to the route bounds | PORTED | OfflineRouteMapTest: recenter case (stub pack installed, getMapAsync awaited) | Compose instrumentation; runs on a device, not in CI |
| recenter control handles a single-point route | PORTED | OfflineRouteMapTest: single-point recenter case | Compose instrumentation; runs on a device, not in CI |
| handles an empty route gracefully | PORTED | RoutePreviewTest: `anEmptyRouteIsHandledGracefully` | Compose instrumentation; runs on a device, not in CI |
| rebuilding with the same points reuses the polyline layer instead of re-projecting | N/A-FRAMEWORK | — | flutter_map didUpdateWidget memoization; irrelevant to MapLibre |
| scrolling the route card out of view keeps the map alive | N/A-FRAMEWORK | — | Flutter ListView keepAlive plumbing |

## test/features/activity/maps/tile_providers_test.dart
Kotlin counterpart: none

Both providers are Flutter adapters (flutter_map ImageProvider / vector_map_tiles). Kotlin renders Mapsforge packs with the native library layer and PMTiles directly through MapLibre's `pmtiles://` source URLs (OfflineRouteMap.kt:432) — no hand-written tile provider exists.

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| MapsforgeTileProvider > a tile outside pack coverage draws as transparent, not an error | N/A-FRAMEWORK | — | |
| MapsforgeTileProvider > a rendered tile reaches flutter_map as a live image | N/A-FRAMEWORK | — | dart:ui rasterization |
| MapsforgeTileProvider > tiles are keyed by coordinate, so the image cache can work | N/A-FRAMEWORK | — | Flutter ImageProvider equality contract |
| PmtilesVectorTileProvider > zoom bounds come from the archive header | N/A-FRAMEWORK | — | PMTiles reading delegated to MapLibre native in Kotlin |
| PmtilesVectorTileProvider > serves the uncompressed tile bytes | N/A-FRAMEWORK | — | |
| PmtilesVectorTileProvider > a missing tile maps to a 404 the map renders as empty | N/A-FRAMEWORK | — | |
| PmtilesVectorTileProvider > a corrupt read maps to a per-tile 500, never a crash | N/A-FRAMEWORK | — | |
## test/features/settings/activity_settings_cards_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/preferences/ActivitySplitDistanceTest.kt, domain/preferences/ActivityRecordingPreferencesTest.kt, features/settings/SettingsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ActivityRecordingPreferencesCard > renders the intro and all sub-controls | PORTED | ActivityRecordingAndCaffeineCardsTest: `activityRecordingCard_rendersTheIntroAndEverySubControl` | Compose instrumentation; runs on a device, not in CI |
| ActivityRecordingPreferencesCard > toggling a switch persists to the repository | PORTED | SettingsViewModelTest.kt: `updateActivityRecordingPreferences persists preference and updates ui state` | includes keepScreenOnDuringRecording |
| ActivityRecordingPreferencesCard > selecting a segment persists to the repository | PORTED | SettingsViewModelTest.kt: `updateActivityRecordingPreferences persists preference and updates ui state` | includes autoIdleTimeoutSeconds |
| ActivityRecordingPreferencesCard > idle-timeout choice is disabled when auto-idle is off | PORTED | ActivityRecordingAndCaffeineCardsTest: `activityRecordingCard_idleTimeoutIsDeadWhileAutoIdleIsOff` | Compose instrumentation; runs on a device, not in CI |
| FavoriteActivityCard > selecting a type persists and "latest" clears it | DIVERGED | SettingsViewModelTest.kt: `selectFavoriteActivity persists preference and updates ui state` | set is asserted; clearing back to null is not |
| ActivitySplitDistanceCard > offers metric presets and saves the choice in METERS | DIVERGED | ActivitySplitDistanceTest.kt: `nearestPreset highlights the closest chip after a unit-system switch` | preset/normalize logic covered; persistence via prefs not asserted |
| ActivitySplitDistanceCard > offers mile presets in imperial but still stores meters | PORTED | ActivitySplitDistanceTest.kt: `imperial presets are exact mile fractions, not rounded meters` | same 1609.344 exactness assertion |

## test/features/settings/apple_health_import_card_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/applehealth/AppleHealthImportServiceTest.kt, AppleHealthImportProgressTest.kt, features/settings/SettingsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders header, permission line and analyze action | PORTED | AppleHealthImportCardTest: `theCardNamesTheImporterItsAccessAndTheWayIn` | Compose instrumentation; runs on a device, not in CI |
| analyze populates the analysis result and category checklist | DIVERGED | AppleHealthImportServiceTest.kt: `service analysis detects import categories without writing` | category/route-count detection covered; card checklist state not |
| toggling a category then importing calls import with the selected set and shows the result | PORTED | AppleHealthImportServiceTest.kt: `service imports only selected categories after analysis` | |
| save report action invokes the save seam and confirms | PORTED | AppleHealthImportCardTest: `theReportActionsHandOverTheExactReportTheImportProduced` | Compose instrumentation; runs on a device, not in CI |
| share report action hands the sheet the same report the save writes | PORTED | AppleHealthImportCardTest: `theReportActionsHandOverTheExactReportTheImportProduced` | Compose instrumentation; runs on a device, not in CI |
| a share with no target shows the failure in the card | N/A-WIDGET | — | |
| analysis failure shows the error text | DIVERGED | AppleHealthImportServiceTest.kt: `failed staged analysis clears the local copy before retry` | staging-clear-on-failure covered; error-text surfacing not |
| a successful import stages the pick, then clears the staged export and its checkpoint | DIVERGED | AppleHealthImportServiceTest.kt: `staged analysis reuses its verified copy for import` (+ `staging cleanup deletes private export files but preserves selected source`) | checkpoint-clear-on-success not asserted |
| an incomplete workout-route archive shows the warning row | PORTED | AppleHealthImportCardTest: `anArchiveThatLostItsWorkoutRoutesSaysSoRatherThanReportingSuccess` | Compose instrumentation; runs on a device, not in CI |
| the import is handed to the foreground service, not run in the UI isolate | DIVERGED | SettingsViewModelTest.kt: `apple import observer uses current import work over older failures` | WorkManager enqueue carries categories + both denominators, but enqueue is stubbed, not the focus |
| progress and the result from the service isolate drive the card | DIVERGED | AppleHealthImportProgressTest.kt: `percent uses raw scan progress when analyzed element total is known` | progress math ported; worker-to-card wiring untested |
| an error from the service isolate shows in the card | PORTED | SettingsViewModelTest.kt: `apple import observer uses current import work over older failures` | worker error reaches uiState.appleHealthImportError |
| a running activity recording refuses the import instead of crashing on the single foreground service | N/A-FRAMEWORK | — | single-ForegroundService constraint is Flutter-side; WorkManager has no such conflict |
| an import still running on relaunch re-attaches to the card | DIVERGED | SettingsViewModelTest.kt: `apple import observer ignores stale finished failures without current work` | observer attach covered for failures; running-progress re-attach not |

## test/features/settings/ble_devices_screen_test.dart
Kotlin counterpart: none

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| leaving the screen with a flow open resets it without notifying the dying element | N/A-FRAMEWORK | — | Riverpod dispose/notify regression; no Compose analogue |

## test/features/settings/ble_devices_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/BleDevicesViewModelTest.kt (sensor add/edit flow) and app/src/test/kotlin/tech/mmarca/openvitals/features/watches/WatchesViewModelTest.kt + WatchDeviceViewModelTest.kt (the Garmin watch-onboarding half, which lives in features/watches in the Kotlin port); plus devices/core/DeviceClassificationTest.kt, domain/model/BleDeviceKindTest.kt, devices/garmin/OnboardGarminWatchUseCaseTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| selecting a device auto-discovers capabilities via the GATT probe | PORTED | BleDevicesViewModelTest.kt: `selecting a device auto-discovers capabilities via the GATT probe` | Kotlin VM has identical selectDiscoveredDevice, untested |
| falls back to advertised capabilities when the probe finds none | PORTED | BleDevicesViewModelTest.kt: `falls back to advertised capabilities when the probe finds none` | fallback branch exists in Kotlin VM, untested |
| flags a capability conflict against an already-paired device | PORTED | BleDevicesViewModelTest.kt: `flags a capability conflict against an already-paired device` | capabilityConflicts untested |
| saving a speed sensor persists the wheel circumference | PORTED | BleDevicesViewModelTest.kt: `saving a speed sensor persists the wheel circumference` |  |
| non-speed sensors are saved without a wheel circumference | PORTED | BleDevicesViewModelTest.kt: `non-speed sensors are saved without a wheel circumference` |  |
| saving with no capabilities surfaces an error and does not persist | PORTED | BleDevicesViewModelTest.kt: `saving with no capabilities surfaces an error and does not persist` |  |
| toggling a capability recomputes conflicts | PORTED | BleDevicesViewModelTest.kt: `toggling a capability recomputes conflicts` |  |
| edit flow loads the device and saves changes | PORTED | BleDevicesViewModelTest.kt: `edit flow loads the device and saves changes` |  |
| removing the edited device closes the edit flow | PORTED | BleDevicesViewModelTest.kt: `removing the edited device closes the edit flow` |  |
| the freezed state keeps its defaults and its derived getter | N/A-FRAMEWORK | — | freezed-conversion pin; Kotlin data-class defaults are language-level (enabledDeviceCount getter exists, untested) |
| enabledDeviceCount survives the conversion | PORTED | BleDevicesViewModelTest.kt: `enabledDeviceCount counts only the enabled devices` | the derived getter is now asserted through the VM state |
| copyWith still clears a nullable field when passed null | N/A-FRAMEWORK | — | Dart copyWith `_unset` sentinel semantics |
| the freezed state compares by value | N/A-FRAMEWORK | — | Kotlin data class equality is language-guaranteed |
| Garmin watch onboarding > selecting a watch skips the capability probe entirely | PORTED | WatchesViewModelTest.kt: `selecting a watch skips the capability probe entirely` | probe-call count asserted at 0 with the probe armed |
| Garmin watch onboarding > a sensor still goes through the probe | PORTED | BleDevicesViewModelTest.kt: `selecting a device auto-discovers capabilities via the GATT probe` | classification only |
| Garmin watch onboarding > selecting an Edge skips the probe like a watch | PORTED | WatchesViewModelTest.kt: `selecting an Edge skips the probe like a watch` | classification only |
| Garmin watch onboarding > onboarding registers the watch and closes the sheet | PORTED | WatchesViewModelTest.kt: `onboarding registers the watch and closes the sheet` | OnboardGarminWatchUseCase untested (WearOS sibling has `registers a (watch, wearos) device, no bond`) |
| Garmin watch onboarding > onboarding an Edge registers it as a bike computer | PORTED | WatchesViewModelTest.kt: `onboarding an Edge registers it as a bike computer` | plus devices/garmin/OnboardGarminWatchUseCaseTest.kt: `registers an Edge as a bike computer with no capabilities` |
| Garmin watch onboarding > a refused pairing keeps the sheet open and explains why | PORTED | WatchesViewModelTest.kt: `a refused pairing keeps the sheet open and explains why` | REFUSED branch exists in OnboardGarminWatchUseCase, untested |
| Garmin watch onboarding > a declined companion association is recorded, not failed | PORTED | WatchesViewModelTest.kt: `a declined companion association is recorded, not failed` | registry half already covered by devices/garmin/OnboardGarminWatchUseCaseTest.kt |
| Garmin watch onboarding > the no-companion flag survives the sheet closing, then clears | PORTED | WatchesViewModelTest.kt: `the no-companion flag survives the sheet closing, then clears` |  |
| Garmin watch onboarding > a blank name falls back to the advertised one | PORTED | WatchesViewModelTest.kt: `a blank name falls back to the advertised one` |  |
| Garmin watch onboarding > forgetting a watch also drops its bond and association | PORTED | WatchDeviceViewModelTest.kt: `forgetting a watch also drops its bond, association and Garmin state` | end-to-end through WatchDeviceViewModel.removeDevice incl. GarminDeviceStateStore.clear |
| Garmin watch onboarding > forgetting a sensor touches neither bond nor association | PORTED | WatchDeviceViewModelTest.kt: `forgetting a sensor touches neither bond nor association` |  |
| Garmin watch onboarding > a watch can be renamed even though it has no capabilities | PORTED | WatchDeviceViewModelTest.kt: `a watch can be renamed even though it has no capabilities` | Kotlin renames watches through WatchDeviceViewModel, not the sensor edit flow |

## test/features/settings/body_energy_diagnostics_card_test.dart
Kotlin counterpart: none (Body Energy diagnostics feature not ported; features/settings/DiagnosticsCards.kt has no such card)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| does not read anything until it is asked to | N/A-WIDGET | — | lazy-load gating; underlying feature absent in Kotlin |
| renders the report and offers a copy once run | N/A-WIDGET | — | |
| warns when more than one app wrote active calories | MISSING | none | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent) |
| says so when no watch samples are stored | MISSING | none | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent) |

## test/features/settings/body_energy_diagnostics_test.dart
Kotlin counterpart: none (buildBodyEnergyDiagnostics / BodyEnergyDiagnosticsReport has no Kotlin equivalent anywhere in main or test)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the component decomposition > activity uses the per-point max, never the sum | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| the component decomposition > a tie counts as the calorie estimate winning | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| the component decomposition > buckets with no activity drain count for neither side | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| clipping > a floored day reports its buckets and when it first pinned | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| clipping > a ledger that does not balance is flagged | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| the watch totals > are delta sums, not start minus end | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| the watch totals > are absent when the watch never synced that day | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| the watch totals > drainError is signed so an over-draining model is visible | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| per-influence errors > are signed, count-weighted, and omit unobserved influences | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| source attribution > flags a day two apps both wrote active calories for | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| source attribution > stays quiet when one app wrote them | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| source attribution > does not confuse two metrics for two calorie sources | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| toReportText > is stable, explicit, and carries the decisive figures | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| toReportText > names missing permissions rather than showing an empty report | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |
| toReportText > says so when the per-source read was truncated | MISSING | — | blocked - feature not ported to Kotlin (no buildBodyEnergyDiagnostics equivalent anywhere in app/src) |

## test/features/settings/body_settings_cards_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/BodyProfileCardTest.kt, domain/preferences/BodyEnergyCalibrationTest.kt, features/settings/SettingsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| BodyProfileCard > seeds fields from stored profile | PORTED | BodyProfileCardScreenTest: `seedsItsFieldsFromTheStoredProfile` | Compose instrumentation; needs a device, so not run in CI |
| BodyProfileCard > shows where weight came from | PORTED | BodyProfileCardScreenTest: `showsWhereAMeasuredValueCameFrom` | Compose instrumentation; needs a device, so not run in CI |
| BodyProfileCard > editing a field and saving persists via bodyProfile() | DIVERGED | SettingsViewModelTest.kt: `updateBodyProfile writes measurements only on change and with permission` | persistence covered at VM level; field parse/save-button path not |
| the Body profile screen shows each fact once > the birth year is not asked for twice | PORTED | BodyProfileCardScreenTest: `theBirthYearIsNotAskedForTwice` | Compose instrumentation; needs a device, so not run in CI |
| the Body profile screen shows each fact once > the zones and tuning live inside the Body card, not beside it | N/A-WIDGET | — | layout composition |
| the Body profile screen shows each fact once > but standalone it still carries a birth year and a Save | PORTED | BodyProfileCardScreenTest: `standaloneItStillCarriesABirthYearAndASave` | Compose instrumentation; needs a device, so not run in CI |
| BodyEnergyCalibrationCard > toggling manual zones reveals the five zone fields | PORTED | BodyEnergyCalibrationCardTest: `togglingManualZonesRevealsTheFiveZoneFields` | Compose instrumentation; runs on a device, not in CI |
| BodyEnergyCalibrationCard > editing zones and saving persists and completes setup | DIVERGED | BodyEnergyCalibrationTest.kt: `manual zones round trip through preference string` + `normalization preserves setupCompleted flag` | model round-trip only; save flow untested |
| BodyEnergyCalibrationCard > switching manual zones off keeps the typed ladder | PORTED | BodyEnergyCalibrationCardTest.kt: `switching manual zones off keeps the typed ladder` | plus `an invalid ladder is the one thing that does erase it` |

## test/features/settings/caffeine_preferences_card_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/CaffeinePreferencesCardTest.kt and SettingsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the caffeine fields seeded from preferences | PORTED | ActivityRecordingAndCaffeineCardsTest: `caffeineCard_seedsItsFieldsFromTheStoredPreferences` | Compose instrumentation; runs on a device, not in CI |
| editing a field and saving persists via the repository | PORTED | SettingsViewModelTest.kt: `updateCaffeinePreferences persists preference and updates ui state` | halfLife=360 + profileCompleted=true persisted, same essence |
| an out-of-range half-life is clamped by the repository on save | PORTED | CaffeinePreferencesCardTest.kt: `an out-of-range half-life is clamped on save` | plus SettingsViewModelTest.kt: `saving an out-of-range half-life reseeds from the clamped stored value` |
| LocalTime bedtime default matches the model | PORTED | CaffeinePreferencesCardTest.kt: `the bedtime default matches the model` | DefaultBedtime 22:30 exists in Kotlin, untested |

## test/features/settings/caffeine_preferences_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt (partial; Kotlin has no draft/seedRevision card VM)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| build seeds the draft and the body profile from preferences | PORTED | SettingsViewModelTest.kt: `build seeds the caffeine preferences and the body profile from storage` | no draft layer in Kotlin; the seeding is the settings ui state |
| save persists the whole draft with profileCompleted | DIVERGED | SettingsViewModelTest.kt: `updateCaffeinePreferences persists preference and updates ui state` | no draft-vs-disk staging in Kotlin; whole-object persist asserted |
| save reseeds the draft from the clamped stored value | PORTED | SettingsViewModelTest.kt: `saving an out-of-range half-life reseeds from the clamped stored value` | clamp-on-write + reseed untested |

## test/features/settings/csv_import_card_test.dart
Kotlin counterpart: none (CSV import logic itself covered under features/imports/csv/ tests)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the card names the importer and what it is for | PORTED | SettingsSmallCardsTest: `csvImportCard_namesTheImporterAndWhatItIsFor` | Compose instrumentation; runs on a device, not in CI |
| tapping the action opens the CSV import route | N/A-FRAMEWORK | — | navigation wiring; route exists in Kotlin AppNavigationSettingsRoutes.kt |

## test/features/settings/debug_diagnostics_card_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/diagnostics/PrivacySafeDebugLogExporterTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the title, body and the share + save actions | PORTED | SettingsSmallCardsTest: `debugDiagnosticsCard_rendersTheTitleBodyAndBothActions` | Compose instrumentation; runs on a device, not in CI |
| save flow sanitizes the logcat and reports success | DIVERGED | PrivacySafeDebugLogExporterTest.kt: `sanitize excludes unrelated tags` (+ `sanitize keeps OpenVitals operational lines`) | tag filtering covered; package=/version= header and file name unasserted |
| degrades gracefully when the native channel is unavailable | N/A-FRAMEWORK | — | Flutter platform-channel absence; Kotlin reads logcat natively |
| share flow sanitizes the logcat and reaches the share seam | DIVERGED | PrivacySafeDebugLogExporterTest.kt: `sanitize excludes unrelated tags` | sanitize covered; Android chooser seam untested |
| share failure surfaces the failure snackbar | N/A-WIDGET | — | |
| share degrades gracefully when the native channel is missing | N/A-FRAMEWORK | — | |

## test/features/settings/device_sync_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/DeviceSyncControllerTest.kt (declares itself the port of this file), plus devices/core/RadioLeasesTest.kt, devices/garmin/GarminSessionTest.kt, features/watches/WatchSettingsLinksTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a sync with nothing new still stamps the device | DIVERGED | DeviceSyncControllerTest.kt: `a successful sync ends idle with the file count` | lastSyncedAt stamping moved into GarminWatchSyncService and is unasserted |
| refuses to sync while a recording is active (shared radio) | DIVERGED | RadioLeasesTest.kt: `a held lease excludes another owner on the same address` (+ GarminWatchActionsControllerTest: `a find is refused while a sync holds the radio`) | recording guard replaced by address-scoped radio leases |
| passes the previously-synced keys down to the service | DIVERGED | GarminSessionTest.kt (runSync alreadySynced skip) | dedup honored at session layer; store-to-service wiring untested |
| reports progress scoped to the syncing device | PORTED | DeviceSyncControllerTest.kt: `reports progress scoped to the syncing device` | |
| a transport failure surfaces its message and stamps nothing | DIVERGED | DeviceSyncControllerTest.kt: `a failure surfaces its message and ends idle` | "stamps nothing" not asserted |
| refuses a second sync while one is running | PORTED | DeviceSyncControllerTest.kt: `refuses a second sync while one is running` | |
| ignores a device that is not a watch | PORTED | DeviceSyncControllerTest.kt: `ignores a device the port does not claim` | |
| ignores an unknown device id | PORTED | DeviceSyncControllerTest.kt: `ignores an unknown device id` | |
| clear() resets the banner | PORTED | DeviceSyncControllerTest.kt: `clear resets the banner but never a running sync` | Kotlin is stronger (also guards running sync) |
| an unavailable write path fails cleanly instead of hanging the row | DIVERGED | DeviceSyncControllerTest.kt: `a failure surfaces its message and ends idle` | no assertion that files are not remembered as synced / device not stamped |
| the open-link registry belongs to the container, not the library | N/A-FRAMEWORK | — | Riverpod container-scoping; Kotlin uses DI scoping, link lifecycle covered by WatchSettingsLinksTest.kt |

## test/features/settings/fit_import_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt (bulk route import block; Kotlin replaced the SAF folder-walk with an OpenMultipleDocuments picker)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| imports every FIT file the folder held | DIVERGED | SettingsViewModelTest.kt: `bulk route import writes all selected files in one batched call` | multi-select batch instead of folder pick; extension filter unasserted |
| opens the files one at a time, not the whole folder at once | N/A-FRAMEWORK | — | folder-source seam does not exist in Kotlin; per-Uri reads by design |
| a cancelled pick leaves the card exactly as it was | N/A-FRAMEWORK | — | picker cancellation handled by Android activity-result, never reaches the VM |
| a folder with no FIT files says so, and is not an error | N/A-FRAMEWORK | — | folder-scan concept absent in Kotlin |
| a folder too big to list says how much of it was taken | N/A-FRAMEWORK | — | listing truncation is folder-walk specific |
| one unreadable file fails that file, not the folder | DIVERGED | SettingsViewModelTest.kt: `a failed batch retries file by file so only the guilty file fails` | same per-file isolation principle, different mechanism |
| a failed scan surfaces, and imports nothing | DIVERGED | SettingsViewModelTest.kt: `a rate-limited batch stops the run without blaming the files` | error surfaces with nothing imported; trigger differs (no scan phase in Kotlin) |

## test/features/settings/garmin_body_energy_invalidation_test.dart
Kotlin counterpart: none (no garminEarliestAffectedDay equivalent; Kotlin instead revisits unsettled days — BodyEnergyChainSyncServiceTest.kt: `a forced pass bypasses the throttle, so a watch sync is acted on at once`, `a later pass skips settled days and revisits only unsettled ones`)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| garminEarliestAffectedDay > is the oldest dated file, whatever order they arrive in | N/A-FRAMEWORK | none | mechanism replaced by unsettled-day revisiting (BodyEnergyChainSyncService); no Kotlin analogue |
| garminEarliestAffectedDay > ignores undated files rather than guessing at them | N/A-FRAMEWORK | none | mechanism replaced by unsettled-day revisiting; no Kotlin analogue |
| garminEarliestAffectedDay > is null when the watch dated nothing | N/A-FRAMEWORK | none | mechanism replaced by unsettled-day revisiting; no Kotlin analogue |
| garminEarliestAffectedDay > is null for an empty sync | N/A-FRAMEWORK | none | mechanism replaced by unsettled-day revisiting; no Kotlin analogue |
| garminEarliestAffectedDay > resolves the local day, not the UTC one | N/A-FRAMEWORK | none | mechanism replaced by unsettled-day revisiting; no Kotlin analogue |
## test/features/settings/garmin_watch_actions_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/GarminWatchActionsControllerTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| find is a toggle: a second tap stops it | PORTED | GarminWatchActionsControllerTest.kt: `find is a toggle - a second tap stops it` | |
| stopping twice before the watch answers does not throw | PORTED | GarminWatchActionsControllerTest.kt: `stopping twice before the watch answers does not throw` | |
| a refused find is reported as a flag, not a message | PORTED | GarminWatchActionsControllerTest.kt: `a refused find is reported as a flag, not a message` | |
| a find is refused while a sync holds the radio | PORTED | GarminWatchActionsControllerTest.kt: `a find is refused while a sync holds the radio` | Kotlin adds an extra non-GFDI no-op case |

## test/features/settings/health_connect_sources_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/HealthConnectSourcesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| aggregateHealthConnectSources > folds observations per package, counting and dating them | DIVERGED | HealthConnectSourcesTest.kt: `aggregates counts and metrics per package` + `sorts most recent contributor first` | "OpenVitals (this app)" self-package label (present in Kotlin HealthConnectSources.kt) never asserted |
| aggregateHealthConnectSources > blank sources collapse to a single "unknown" entry | PORTED | HealthConnectSourcesTest.kt: `blank sources fold into the unknown bucket` | |
| aggregateHealthConnectSources > an empty read yields no sources | PORTED | HealthConnectSourcesTest.kt: `empty input yields an empty list` | |
| aggregateHealthConnectSources > an unknown package keeps its raw name | PORTED | HealthConnectSourcesTest.kt: `sorts most recent contributor first` | raw-name fallback asserted there |

## test/features/settings/heart_rate_threshold_guard_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the high threshold cannot be stepped below the low one | PORTED | SettingsViewModelTest.kt: `high threshold cannot drop within the gap of the low threshold` + `threshold gap clamp lands inside the repository bounds` | latter proves gap (95 = 90 + 5) distinct from repo floor |
| the low threshold cannot be stepped above the high one | PORTED | SettingsViewModelTest.kt: `low threshold gap clamp lands inside the repository bounds` | high=90 then low=95 yields 85, distinct from the repo ceiling of 100 |
| a legitimate change still lands unchanged | PORTED | SettingsViewModelTest.kt: `a legitimate threshold change still lands unchanged` | 150/45 land exactly |

## test/features/settings/metabolism_card_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/data/migration/FlutterPrefsKeyTableTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a physiological flag persists through the caffeine store | DIVERGED | SettingsViewModelTest.kt: `updateCaffeinePreferences persists preference and updates ui state` | persists via mocked PreferencesRepository; smoker flag landing under the original `caffeine_*` keys not asserted |
| reads values written under the original caffeine keys | DIVERGED | FlutterPrefsKeyTableTest.kt: `caffeine enums transcode` | covers key-level migration (incl. `caffeine_hormonal_status`), not the UI read-back path |
| surfaces pregnancy, which used to be buried under caffeine | PORTED | SettingsSmallCardsTest: `metabolismCard_surfacesHormonalStatus_whichUsedToBeBuriedUnderCaffeine` | Compose instrumentation; runs on a device, not in CI |

## test/features/settings/offline_maps_card_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/activity/maps/OfflineMapMetadataStoreTest.kt, OfflineMapPackFormatTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the empty state when no packs are imported | PORTED | OfflineMapsCardTest: `withNoPacksTheCardSaysSoAndStillOffersTheImport` | Compose instrumentation; runs on a device, not in CI |
| renders the pack list and the active render format | PORTED | OfflineMapsCardTest: `aPackIsListedWithItsFormatAndOnlyItsOwnRendererCanBeChosen` | Compose instrumentation; runs on a device, not in CI |
| selecting an enabled format chip calls setActiveFormat | DIVERGED | OfflineMapMetadataStoreTest.kt: `write and read preserves imported maps and active format` | activeFormat persistence covered at store level; OfflineMapRepository.setActiveFormat and its state flow untested |
| the delete affordance deletes the pack | DIVERGED | OfflineMapRepositoryTest.kt: `deleteMap removes the file and its metadata entry` | the deletion itself is covered at repository level; the card's affordance is Compose |
| import shows progress, disables the button, then reports | PORTED | OfflineMapsCardTest: `anImportInFlightLocksTheButtonAndReportsItsProgress`, `aFinishedImportNamesWhatLandedAndGivesTheButtonBack` | Compose instrumentation; runs on a device, not in CI |
| a rejected file surfaces the import error in error color | DIVERGED | OfflineMapPackFormatTest.kt: `detects supported offline map file extensions` | `.osm.pbf` rejection covered; import-error message/color surfacing untested |

## test/features/settings/permission_categories_card_test.dart
Kotlin counterpart: none (logic partially in app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders a row per category with granted/optional status | PORTED | PermissionCategoryCardTest: `rendersARowPerCategoryWithGrantedOptionalAndUnsupportedStatus` | Compose instrumentation; runs on a device, not in CI |
| a grant button requests the category permissions | PORTED | PermissionCategoryCardTest: `aGrantButtonRequestsThatCategorysPermissions` | Compose instrumentation; runs on a device, not in CI |

## test/features/settings/permission_categories_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| build exposes the category taxonomy once the gates resolve | DIVERGED | SettingsViewModelTest.kt: `refresh includes cycle permissions with visible permissions` | asserts only the union of category permissions; ordered category taxonomy (SettingsViewModel.permissionCategories) never asserted |
| a granted request succeeds and asks for exactly the missing set | DIVERGED | SettingsViewModelTest.kt: `missingVisiblePermissions excludes already granted visible permissions` | missing-set derivation covered; the request itself is an ActivityResult launch, untested |
| a manual-only category opens the Health Connect settings screen | DIVERGED | SettingsViewModelTest.kt: `missingVisiblePermissions excludes already granted visible permissions` | `missingManualVisiblePermissions` asserted; opening HC settings is a platform intent, untested |
| a refused request lands as CommandFailure carrying the ScreenError | N/A-FRAMEWORK | | Kotlin permission refusal returns via ActivityResult (no throwing path); ScreenError.PermissionDenied exists but this command plumbing is Riverpod-specific |

## test/features/settings/reminder_test_card_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the title, body and the show-reminder action | PORTED | SettingsSmallCardsTest: `reminderTestCard_rendersTheTitleBodyAndShowReminderAction` | Compose instrumentation; runs on a device, not in CI |
| a posted reminder confirms with a snackbar | DIVERGED | SettingsViewModelTest.kt: `the test reminder is posted through the hydration reminder controller` | the post is asserted at VM level; the snackbar confirmation is Compose |
| disabled notifications are reported, not silently swallowed | N/A-BEHAVIOR | none | blocked on behavior decision - showTestReminder returns silently when the permission is absent |
| a failed post reports the failure | N/A-BEHAVIOR | none | blocked on behavior decision - the Kotlin controller has no failure-reporting path |

## test/features/settings/route_import_cards_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/ActivityEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| single route import stores the pending handle and navigates | DIVERGED | ActivityEntryViewModelTest.kt: `route import fills distance and elevation fields in current unit system` | receiving-end import handling covered; pending-handle handoff/navigation is Android nav plumbing, untested |
| FIT import stores the pending handle and navigates | DIVERGED | ActivityEntryViewModelTest.kt: `FIT import without route fills manual activity fields` | same: handoff/navigation untested |
| bulk import writes one activity per file | PORTED | SettingsViewModelTest.kt: `bulk route import writes all selected files in one batched call` | Kotlin additionally asserts single batched HC call |
| bulk import tolerates a bad file (imported/failed counts) | PORTED | SettingsViewModelTest.kt: `a failed batch retries file by file so only the guilty file fails` | equivalent imported=1/failed=1 assertions |

## test/features/settings/route_import_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| build derives the permission counts from the activity repository | DIVERGED | SettingsViewModelTest.kt: `missingVisiblePermissions excludes already granted visible permissions` + `missingVisiblePermissions is empty when all visible permissions are granted` | global missing/granted derivation covered; route-import-card-specific counts/availability flags not modeled as a distinct state |
| grantPermissions requests exactly the missing set | DIVERGED | SettingsViewModelTest.kt: `missingVisiblePermissions excludes already granted visible permissions` | missing-set computation covered; the launch of the request contract untested |
| a failed grant lands as CommandFailure with the error message | N/A-FRAMEWORK | | Kotlin grants return via ActivityResult contract, no throwing grant command; generic error mapping covered by core/presentation ScreenErrorTest |

## test/features/settings/settings_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| settings root renders the section cards | PORTED | SettingsRootTest: `everySectionRendersACardThatOpensIt` | Compose instrumentation; runs on a device, not in CI |
| settings root shows the support card and version footer | PORTED | SettingsRootTest: `theSupportCardOffersItsThreeRoutesOut`, `theVersionFooterNamesTheBuildABugReportWouldQuote` | Compose instrumentation; runs on a device, not in CI |
| selecting a theme mode persists through the repository | PORTED | SettingsViewModelTest.kt: `selectAppThemeMode persists preference and updates ui state` | |

## test/features/settings/watch_data_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/WatchMetricsTest.kt, SleepCoachReadingTest.kt (over the extracted `sleepCoachReading()` seam)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows only the metrics the watch has actually sent | DIVERGED | WatchMetricsTest.kt: `resolves the latest reading of each stored metric` | presence/absence logic covered; seconds→"17 min" display formatting not asserted anywhere |
| names what is missing once, at the foot | PORTED | WatchMetricsTest.kt: `names what the watch never sent, in declaration order` | |
| Sleep Coach reads as a comparison, not a bare number | PORTED | SleepCoachReadingTest.kt: `Sleep Coach reads as a comparison, not a bare number` | seam - sleepCoachReading() extracted from WatchDataScreen.kt |
| an empty table says so instead of rendering empty sections | PORTED | WatchMetricsTest.kt: `an empty table is empty, not a map of blanks` | |
| vigorous intensity minutes count double, as Garmin counts them | PORTED | WatchMetricsTest.kt: `vigorous intensity minutes count double, as Garmin counts them` | |
| the weekly goal counts the whole week, not just today | PORTED | WatchMetricsTest.kt: `the weekly total sums each day's FINAL running total` | Kotlin also excludes last week's samples |

## test/features/settings/watch_device_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/WatchDeviceViewModelTest.kt (rename/forget), devices/core/DeviceClassificationTest.kt, features/watches/WatchSettingsLinksTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| opening and dismissing the rename dialog does not throw | N/A-FRAMEWORK | | Flutter TextEditingController dispose-during-route-exit regression |
| renaming applies the new name | PORTED | WatchDeviceViewModelTest.kt: `renaming trims the new name and persists it` | plus `a blank rename is refused rather than blanking the row` |
| the device view offers Data and Sync, and nothing measured | PORTED | WatchDeviceRowsTest: `aWatchOffersBothDataAndSync` (actions row only; the "nothing measured" half needs WatchDeviceViewModel) | Compose instrumentation; runs on a device, not in CI |
| a bike computer syncs, offers its live-sensor role, and has no wellness Data view | PORTED | WatchDeviceRowsTest: `aBikeComputerSyncsButHasNoWellnessDataView`, `aBikeComputerOffersItsLiveSensorRole` | Compose instrumentation; runs on a device, not in CI |
| Alarms, Find and the whole settings tree are live | PORTED | WatchDeviceRowsTest: `alarmsAndFindAreLiveActions`, `theOnDeviceSettingsRowOpensTheWatchsOwnSettingsTree` | Compose instrumentation; runs on a device, not in CI |

## test/features/settings/watch_notification_apps_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/WatchNotificationAppsViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a phone with no launchable apps says so rather than showing an empty list | PORTED | WatchNotificationAppsScreenTest: `aPhoneWithNoLaunchableAppsSaysSoRatherThanShowingAnEmptyList` | Compose instrumentation; runs on a device, not in CI |
| every app reads as sending to the watch until it is silenced | PORTED | WatchNotificationAppsViewModelTest.kt: `the app list carries the stored blocklist` | default-unblocked (blocklist, not allowlist) asserted |
| silencing an app reaches the native filter | PORTED | WatchNotificationAppsViewModelTest.kt: `blocking an app persists it and pushes the config` | Kotlin filter reads the store natively; push asserted |
| an app that cannot be listed leaves the screen usable rather than stuck loading | PORTED | WatchNotificationAppsViewModelTest.kt: `an app list that cannot be read leaves the screen usable, not stuck loading` | no Kotlin test for loadApps with a throwing gateway (error-tolerant loading) |

## test/features/settings/watch_notifications_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/watches/WatchNotificationAppsViewModelTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/data/repository/WatchNotificationPrefsStoreTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the switch reads off until notification access is granted | PORTED | WatchNotificationAppsViewModelTest.kt: `active means both gates open, not just the switch` | accessGranted also asserted in `access is re-read at enable time` |
| turning it on without access opens system settings and does NOT enable | PORTED | WatchNotificationAppsViewModelTest.kt: `without notification access the system screen opens and nothing is enabled` | |
| access granted while the screen was open still lets the switch turn on | PORTED | WatchNotificationAppsViewModelTest.kt: `access is re-read at enable time, not trusted from stale state` | |
| enabling pushes the config the native filter runs on | DIVERGED | WatchNotificationAppsViewModelTest.kt: `refresh reads both gates and mirrors the config to the filter` | push asserted on refresh and on block, not specifically on the enable transition |
| turning it off pushes the config again, so capture stops at once | DIVERGED | WatchNotificationAppsViewModelTest.kt: `disabling needs no gates and takes effect at once` | disable outcome asserted, but no config-push assertion on disable |
| the choice survives a rebuild of the view-model | PORTED | WatchNotificationPrefsStoreTest.kt: `enabled round-trips through storage` | rebuild = fresh store over the same prefs |
| the disclosure is shown before notification access is requested | DIVERGED | WatchNotificationAppsViewModelTest.kt: `enabling without prior consent shows the disclosure and enables nothing` | disclosure-first is structural in the Kotlin flow, but settings-not-yet-opened ordering is not explicitly asserted |
| declining the disclosure enables nothing and opens nothing | DIVERGED | WatchNotificationAppsViewModelTest.kt: `declining the disclosure leaves the feature off and consent unset` | does not assert the settings screen was not opened / config not pushed |
| the disclosure is shown once, not on every toggle | PORTED | WatchNotificationAppsViewModelTest.kt: `consent is remembered, so a second enable does not re-prompt` | plus store test `disclosure acceptance is remembered independently of enabled` |
| switching off never asks for consent | DIVERGED | WatchNotificationAppsViewModelTest.kt: `disabling needs no gates and takes effect at once` | Kotlin disables with consent already granted, so never-prompts-on-disable is not distinguishably proven |
| the build's diagnostics flag travels to the native side | N/A-FRAMEWORK | | Flutter-plugin BuildConfig plumbing; the Kotlin filter is in-process (devices/notifications/NotificationFilterTest covers filtering itself) |
| the blocklist > every app is listed as sending until it is silenced | PORTED | WatchNotificationAppsViewModelTest.kt: `the app list carries the stored blocklist` | plus `blocked count counts only blocked apps` |
| the blocklist > silencing an app reaches the native filter | PORTED | WatchNotificationAppsViewModelTest.kt: `blocking an app persists it and pushes the config` | |
| the blocklist > a silenced app stays silenced across a rebuild | PORTED | WatchNotificationPrefsStoreTest.kt: `the blocklist round-trips as a set` | |
| the blocklist > un-silencing removes it again | PORTED | WatchNotificationAppsViewModelTest.kt: `blocking an app persists it and pushes the config` (unblock half) + WatchNotificationPrefsStoreTest.kt: `setBlocked adds and removes one package` | |
| revoking access in system settings turns the switch off again | PORTED | WatchNotificationAppsViewModelTest.kt: `active means both gates open, not just the switch` | active=false when access is gone |
## test/features/imports/csv/csv_datetime_format_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvDateTimeFormatTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| resolveCsvInstant > a timezone-less timestamp at a fixed +02:00 resolves two hours earlier in UTC | PORTED | CsvDateTimeFormatTest.kt: `a timezone-less timestamp at a fixed plus two resolves two hours earlier in UTC` | |
| resolveCsvInstant > a timestamp read as UTC keeps its wall clock and a zero offset | PORTED | CsvDateTimeFormatTest.kt: `a timestamp read as UTC keeps its wall clock and a zero offset` | |
| resolveCsvInstant > an ISO timestamp carrying +05:30 overrides the selected UTC mode | PORTED | CsvDateTimeFormatTest.kt: `an ISO timestamp carrying an offset overrides the selected UTC mode` | |
| resolveCsvInstant > an ISO timestamp ending in Z resolves to that instant with no offset | PORTED | CsvDateTimeFormatTest.kt: `an ISO timestamp ending in Z resolves to that instant with no offset` | |
| resolveCsvInstant > the device zone reports an offset that maps the instant back to the wall clock in the file | PORTED | CsvDateTimeFormatTest.kt: `the device zone reports an offset that maps the instant back to the wall clock in the file` | |
| resolveCsvInstant > epoch seconds resolve to the matching UTC instant | PORTED | CsvDateTimeFormatTest.kt: `epoch seconds resolve to the matching UTC instant` | |
| resolveCsvInstant > a date-only cell resolves to midnight of that day | PORTED | CsvDateTimeFormatTest.kt: `a date-only cell resolves to midnight of that day` | |
| resolveCsvInstant > a cell that does not match the chosen format resolves to null | PORTED | CsvDateTimeFormatTest.kt: `a cell that does not match the chosen format resolves to null` | |
| resolveCsvInstant > a custom pattern parses a shape none of the families cover | PORTED | CsvDateTimeFormatTest.kt: `a custom pattern parses a shape none of the families cover` | |
| parseCsvWallClock > a date-only pattern does not silently swallow a trailing time | PORTED | CsvDateTimeFormatTest.kt: `a date-only pattern does not silently swallow a trailing time` | |
| parseCsvWallClock > a ten-digit epoch value is not misread as milliseconds | PORTED | CsvDateTimeFormatTest.kt: `a ten-digit epoch value is not misread as milliseconds` | |
| parseCsvWallClock > a small counting number is not accepted as an epoch timestamp | PORTED | CsvDateTimeFormatTest.kt: `a small counting number is not accepted as an epoch timestamp` | |
| parseCsvWallClock > a real epoch second inside the plausible window still parses | PORTED | CsvDateTimeFormatTest.kt: `a real epoch second inside the plausible window still parses` | |
| csvTimestampHasExplicitOffset > a bare ISO date is not mistaken for carrying an offset | PORTED | CsvDateTimeFormatTest.kt: `a bare ISO date is not mistaken for carrying an offset` | |
| csvTimestampHasExplicitOffset > an offset suffix on a full timestamp is detected | PORTED | CsvDateTimeFormatTest.kt: `an offset suffix on a full timestamp is detected` | |
| csvTimestampHasExplicitOffset > a timestamp with no offset suffix is reported as carrying none | PORTED | CsvDateTimeFormatTest.kt: `a timestamp with no offset suffix is reported as carrying none` | |
| detectCsvDateTimeFormat > a year-first sample is detected as year-first | PORTED | CsvDateTimeFormatTest.kt: `a year-first sample is detected as year-first` | |
| detectCsvDateTimeFormat > a sample where both day-first and month-first parse every row is reported ambiguous rather than guessed | PORTED | CsvDateTimeFormatTest.kt: `a sample where both day-first and month-first parse every row is reported ambiguous rather than guessed` | |
| detectCsvDateTimeFormat > a day above twelve resolves the ordering to day-first | PORTED | CsvDateTimeFormatTest.kt: `a day above twelve resolves the ordering to day-first` | |
| detectCsvDateTimeFormat > an unparsable sample reports that nothing matched | PORTED | CsvDateTimeFormatTest.kt: `an unparsable sample reports that nothing matched` | |
| detectCsvDateTimeFormat > an empty sample reports that nothing matched | PORTED | CsvDateTimeFormatTest.kt: `an empty sample reports that nothing matched` | |

## test/features/imports/csv/csv_import_done_pop_test.dart
Kotlin counterpart: none

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| tapping Done leaves the importer and returns to Data Importers | N/A-FRAMEWORK | — | go_router pop plumbing; Kotlin Done is a compose nav callback in CsvImportScreen, untested |

## test/features/imports/csv/csv_import_report_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvImportReportTest.kt (1:1 port of `buildCsvImportReport`)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildCsvImportReport > the report names the file and the outcome | PORTED | CsvImportReportTest.kt: `the report names the file and the outcome` |  |
| buildCsvImportReport > an unnamed file is reported rather than left blank | PORTED | CsvImportReportTest.kt: `an unnamed file is reported rather than left blank` |  |
| buildCsvImportReport > every tally from the run appears | PORTED | CsvImportReportTest.kt: `every tally from the run appears` |  |
| buildCsvImportReport > the parsing settings that produced the run are recorded | PORTED | CsvImportReportTest.kt: `the parsing settings that produced the run are recorded` |  |
| buildCsvImportReport > a fixed offset is written out in full | PORTED | CsvImportReportTest.kt: `a fixed offset is written out in full` |  |
| buildCsvImportReport > a custom date pattern is recorded so a bad one can be spotted | PORTED | CsvImportReportTest.kt: `a custom date pattern is recorded so a bad one can be spotted` |  |
| buildCsvImportReport > every column is listed with the role it was given | PORTED | CsvImportReportTest.kt: `every column is listed with the role it was given` |  |
| buildCsvImportReport > a derived body fat says what it was derived from, not just its unit | PORTED | CsvImportReportTest.kt: `a derived body fat says what it was derived from, not just its unit` |  |
| buildCsvImportReport > rejection counts are grouped by reason | PORTED | CsvImportReportTest.kt: `rejection counts are grouped by reason` |  |
| buildCsvImportReport > individual rejected rows name the row, column and value | PORTED | CsvImportReportTest.kt: `individual rejected rows name the row, column and value` |  |
| buildCsvImportReport > a capped per-row log says how many were dropped and that the counts are not | PORTED | CsvImportReportTest.kt: `a capped per-row log says how many were dropped and that the counts are not` |  |
| buildCsvImportReport > a clean run has no rejection sections at all | PORTED | CsvImportReportTest.kt: `a clean run has no rejection sections at all` |  |
| buildCsvImportReport > a failed run carries its error text | PORTED | CsvImportReportTest.kt: `a failed run carries its error text` |  |
| buildCsvImportReport > a rate-limited run says so rather than reading as a plain stop | PORTED | CsvImportReportTest.kt: `a rate-limited run says so rather than reading as a plain stop` |  |

## test/features/imports/csv/csv_import_screen_test.dart
Kotlin counterpart: none (compose screen app/src/main/kotlin/.../csv/CsvImportScreen.kt has no androidTest)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the importer opens on the pick step with its explainer | PORTED | CsvImportStepsTest: `theImporterOpensOnThePickStepWithItsExplainer` | Compose instrumentation; runs on a device, not in CI |
| picking a file lists one row per column with its header | PORTED | CsvImportFlowTest: `pickingAFileListsOneRowPerColumnWithItsHeader` | Compose instrumentation; runs on a device, not in CI |
| a file with only a header row shows the empty-file message | PORTED | CsvImportStepsTest: `aFileWithOnlyAHeaderRowShowsTheEmptyFileMessage` | Compose instrumentation; runs on a device, not in CI |
| a freshly picked file cannot continue until a metric column is chosen | PORTED | CsvImportFlowTest: `aFreshlyPickedFileCannotContinueUntilAMetricColumnIsChosen` | Compose instrumentation; runs on a device, not in CI |
| mapping body fat as a mass with no weight column shows the needs-weight error and keeps Continue disabled | PORTED | CsvImportFlowTest: `bodyFatAsAMassWithNoWeightColumnSaysSoAndKeepsContinueDisabled` | Compose instrumentation; runs on a device, not in CI |
| the confirm step shows the observed range for each metric | PORTED | CsvImportFlowTest: `theConfirmStepShowsTheObservedRangeForEachMetric` | Compose instrumentation; runs on a device, not in CI |
| the confirm step shows the date span the import will write | PORTED | CsvImportFlowTest: `theConfirmStepShowsTheDateSpanTheImportWillWrite` | Compose instrumentation; runs on a device, not in CI |
| reading an ambiguous file month-first instead of day-first is visible in the date span | PORTED | CsvImportFlowTest: `readingAnAmbiguousFileMonthFirstIsVisibleInTheDateSpan` | Compose instrumentation; runs on a device, not in CI |
| a finished import reports what was written | PORTED | CsvImportResultViewTest: `aFinishedImportReportsWhatWasWritten` | Compose instrumentation; runs on a device, not in CI |
| an import that rejects every row says nothing was imported | PORTED | CsvImportResultViewTest: `anImportThatRejectsEveryRowSaysNothingWasImportedAndWhy` | Compose instrumentation; runs on a device, not in CI |
| the finished import offers to save a report | PORTED | CsvImportResultViewTest: `theFinishedImportOffersToTakeTheReportAway` | Compose instrumentation; runs on a device, not in CI |
| saving the report writes it and confirms once | N/A-WIDGET | — | Kotlin saves via SAF launcher + Toast; report content untested (see report MISSING) |
| sharing the report hands the sheet the report and stays quiet | N/A-WIDGET | — | Kotlin shares via system share sheet |
| a share with no target tells the user instead of failing silently | N/A-WIDGET | — | |
| tapping Choose CSV file invokes the picker | PORTED | CsvImportStepsTest: `tappingChooseCsvFileInvokesThePicker` | Compose instrumentation; runs on a device, not in CI |

## test/features/imports/csv/csv_import_service_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvImportServiceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| every row of a clean file is written and the run completes | PORTED | CsvImportServiceTest.kt: `every row of a clean file is written and the run completes` | |
| records already in Health Connect are counted as present and still written, so a corrected value upserts | PORTED | CsvImportServiceTest.kt: `records already in Health Connect are counted as present and still written so a corrected value upserts` | |
| a duplicated row inside one file is written once | PORTED | CsvImportServiceTest.kt: `a duplicated row inside one file is written once` | |
| a refused batch is retried record by record and only the bad record is counted as rejected | PORTED | CsvImportServiceTest.kt: `a refused batch is retried record by record and only the bad record is counted as rejected` | |
| a rate-limited run stops and reports how far it got | PORTED | CsvImportServiceTest.kt: `a rate-limited run stops and reports how far it got` | |
| cancelling mid-run keeps what was written and stops reading | PORTED | CsvImportServiceTest.kt: `cancelling mid-run keeps what was written and stops reading` | |
| a malformed row is skipped with a diagnostic and the rest imports | PORTED | CsvImportServiceTest.kt: `a malformed row is skipped with a diagnostic and the rest imports` | |
| a missing file fails the run instead of throwing | PORTED | CsvImportServiceTest.kt: `a missing file fails the run instead of throwing` | |
| a file with only a header writes nothing and still completes | PORTED | CsvImportServiceTest.kt: `a file with only a header writes nothing and still completes` | |
| the retained diagnostic log is capped while the counts stay complete | PORTED | CsvImportServiceTest.kt: `the retained diagnostic log is capped while the counts stay complete` | |
| progress reports a fraction of the file once bytes are known | PORTED | CsvImportServiceTest.kt: `progress reports a fraction of the file once bytes are known` | |

## test/features/imports/csv/csv_import_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvImportViewModelTest.kt; further logic coverage in CsvMappingValidationTest.kt / CsvRowConverterTest.kt / CsvImportServiceTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| picking a file advances to the mapping step and exposes its columns | PORTED | CsvImportViewModelTest.kt: `picking a file advances to the mapping step and exposes its columns` | pickFile(uri) portable with mocked Context/ContentResolver |
| the app-open permission refresh does not discard a file already picked | N/A-FRAMEWORK | — | Riverpod build/invalidate regression; Kotlin VM has no rebuild-on-invalidate hazard |
| a permission granted after the file was picked still reaches the state | PORTED | CsvImportViewModelTest.kt: `a permission granted after the file was picked still reaches the state` | Kotlin refreshPermissions() is JVM-testable |
| cancelling the file picker leaves the importer on the pick step | N/A-FRAMEWORK | — | Kotlin picker cancel never reaches the VM (ActivityResult) |
| the date column is pre-selected but no metric is guessed | PORTED | CsvImportViewModelTest.kt: `the date column is pre-selected but no metric is guessed` | now asserted through VM pickFile |
| a freshly picked file cannot continue until a metric is mapped | PORTED | CsvImportViewModelTest.kt: `a freshly picked file cannot continue until a metric is mapped` | canContinue state property untested |
| mapping a column defaults its unit from the column's own header | PORTED | CsvImportViewModelTest.kt: `mapping a column defaults its unit from the column's own header` | setColumnRole defaulting now asserted |
| mapping fat mass in kg to body fat without a weight column blocks continuing | PORTED | CsvImportViewModelTest.kt: `mapping fat mass in kg to body fat without a weight column blocks continuing` | canContinue=false now asserted at VM level |
| mapping the weight column too clears the derivation issue | PORTED | CsvImportViewModelTest.kt: `mapping the weight column too clears the derivation issue` |  |
| only the mapped metrics permissions are reported missing | PORTED | CsvImportViewModelTest.kt: `only the mapped metrics permissions are reported missing` | VM missingPermissions now asserted |
| a permission the installed provider does not define is never requested | PORTED | CsvImportViewModelTest.kt: `a permission the installed provider does not define is never requested` | supportedWritePermissions filter in CsvImportState untested |
| a supported permission is still requested when another is unsupported | PORTED | CsvImportViewModelTest.kt: `a supported permission is still requested when another is unsupported` |  |
| an already-granted permission is not reported missing | PORTED | CsvImportViewModelTest.kt: `an already-granted permission is not reported missing` |  |
| a completed import writes one record per mapped metric per row | PORTED | CsvImportViewModelTest.kt: `a completed import writes one record per mapped metric per row` | VM step DONE transition now asserted |
| the Withings fat-mass derivation writes body-fat percentages | PORTED | CsvImportViewModelTest.kt: `the Withings fat-mass derivation writes body-fat percentages` | now asserted via VM import |
| saveReport > the report describes the run that just finished | PORTED | CsvImportViewModelTest.kt: `the report describes the run that just finished` | Kotlin reportText() is portable, untested |
| saveReport > there is nothing to report before an import has run | PORTED | CsvImportViewModelTest.kt: `there is nothing to report before an import has run` | reportText() null-guard untested |
| saveReport > a saved report ends the command in success carrying true | N/A-FRAMEWORK | — | no CommandState in Kotlin; save is a SAF launcher in the screen |
| saveReport > a cancelled save succeeds carrying false, not a failure | N/A-FRAMEWORK | — | |
| saveReport > a throwing save lands as a command failure rather than escaping | N/A-FRAMEWORK | — | |
| saveReport > clearing returns the command to idle so it cannot replay | N/A-FRAMEWORK | — | |
| saveReport > saving before an import has run does nothing | N/A-FRAMEWORK | — | guard lives in screen (reportText() null → return) |
| saveReport > sharing hands the sheet the same report the save writes | N/A-FRAMEWORK | — | both flows read reportText(); no share command in Kotlin VM |
| saveReport > a share with no target lands as a command failure rather than escaping | N/A-FRAMEWORK | — | |
| saveReport > clearing the share returns the command to idle so it cannot replay | N/A-FRAMEWORK | — | |
| saveReport > sharing before an import has run does nothing | N/A-FRAMEWORK | — | |
| an empty file lands on the mapping step with no mapping to edit | PORTED | CsvImportViewModelTest.kt: `an empty file lands on the mapping step with no mapping to edit` |  |
| resetting returns to the pick step and drops the previous run | PORTED | CsvImportViewModelTest.kt: `resetting returns to the pick step and drops the previous run` | Kotlin reset() exists, untested |
| changing the separator re-reads the file under the new dialect | PORTED | CsvImportViewModelTest.kt: `changing the separator re-reads the file under the new dialect` | Kotlin setDialect() exists, untested |

## test/features/imports/csv/csv_mapping_validation_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvMappingValidationTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| validateCsvMapping > a complete mapping reports no issues | PORTED | CsvMappingValidationTest.kt: `a complete mapping reports no issues` | |
| validateCsvMapping > a mapping with no timestamp column reports it | PORTED | CsvMappingValidationTest.kt: `a mapping with no timestamp column reports it` | |
| validateCsvMapping > two timestamp columns report the conflict | PORTED | CsvMappingValidationTest.kt: `two timestamp columns report the conflict` | |
| validateCsvMapping > a mapping with no metric column reports it | PORTED | CsvMappingValidationTest.kt: `a mapping with no metric column reports it` | |
| validateCsvMapping > two columns mapped to the same metric report the duplicate | PORTED | CsvMappingValidationTest.kt: `two columns mapped to the same metric report the duplicate` | |
| validateCsvMapping > body fat as a mass with no weight column reports that it needs one | PORTED | CsvMappingValidationTest.kt: `body fat as a mass with no weight column reports that it needs one` | |
| validateCsvMapping > body fat as a percentage needs no weight column | PORTED | CsvMappingValidationTest.kt: `body fat as a percentage needs no weight column` | |
| validateCsvMapping > a date format matching no sampled row reports it | PORTED | CsvMappingValidationTest.kt: `a date format matching no sampled row reports it` | |
| validateCsvMapping > an undecidable day/month order is reported while the format is still automatic | PORTED | CsvMappingValidationTest.kt: `an undecidable day month order is reported while the format is still automatic` | |
| validateCsvMapping > choosing day-first answers the ambiguity and clears the issue | PORTED | CsvMappingValidationTest.kt: `choosing day-first answers the ambiguity and clears the issue` | |
| initialCsvMapping > the first column that parses as a date is pre-selected | PORTED | CsvMappingValidationTest.kt: `the first column that parses as a date is pre-selected` | |
| initialCsvMapping > no metric is guessed from a header name | PORTED | CsvMappingValidationTest.kt: `no metric is guessed from a header name` | |
| initialCsvMapping > a file with no date-like column selects no timestamp | PORTED | CsvMappingValidationTest.kt: `a file with no date-like column selects no timestamp` | |
| requiredWritePermissions > only the mapped metrics permissions are required | PORTED | CsvMappingValidationTest.kt: `only the mapped metrics permissions are required` | |
| requiredWritePermissions > a body-composition mapping requires one permission per metric | PORTED | CsvMappingValidationTest.kt: `a body-composition mapping requires one permission per metric` | |
| detectCsvUnitInHeader > a parenthesised unit is read off the header | PORTED | CsvMappingValidationTest.kt: `a parenthesised unit is read off the header` | |
| detectCsvUnitInHeader > a unit word inside the label is not read as the unit | PORTED | CsvMappingValidationTest.kt: `a unit word inside the label is not read as the unit` | |
| detectCsvUnitInHeader > a header with no unit reads as none | PORTED | CsvMappingValidationTest.kt: `a header with no unit reads as none` | |

## test/features/imports/csv/csv_row_converter_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvRowConverterTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| (all 43 cases: convertCsvRow x14, buildCsvClientRecordId x5, vitals metrics x12, previewInstantRange x6, parseCsvNumber x7 — omitted here individually; each maps 1:1 by name) | PORTED | CsvRowConverterTest.kt (same test names) | 1:1 port; Kotlin additionally pins a byte-identical cross-build client-record-id |

## test/features/imports/csv/csv_table_reader_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvTableReaderTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| sniffDialect > a comma file with quoted headers is sniffed as comma-delimited | PORTED | CsvTableReaderTest.kt: `a comma file with quoted headers is sniffed as comma-delimited` | |
| sniffDialect > a semicolon file whose quoted headers contain commas is sniffed as semicolon-delimited | PORTED | CsvTableReaderTest.kt: `a semicolon file whose quoted headers contain commas is sniffed as semicolon-delimited` | |
| sniffDialect > a CRLF file is sniffed as CRLF | PORTED | CsvTableReaderTest.kt: `a CRLF file is sniffed as CRLF` | |
| sniffDialect > an LF file is sniffed as LF | PORTED | CsvTableReaderTest.kt: `an LF file is sniffed as LF` | |
| sample > a quoted header containing a comma reads as a single column | PORTED | CsvTableReaderTest.kt: `a quoted header containing a comma reads as a single column` | |
| sample > a UTF-8 BOM does not leak into the first header cell | PORTED | CsvTableReaderTest.kt: `a UTF-8 BOM does not leak into the first header cell` | |
| sample > a quoted field containing newlines survives a chunk boundary intact | PORTED | CsvTableReaderTest.kt: `a quoted field containing newlines survives a chunk boundary intact` | |
| sample > sampling a file with thousands of rows stops at the preview limit | PORTED | CsvTableReaderTest.kt: `sampling a file with thousands of rows stops at the preview limit` | |
| sample > a file with no header row gets synthesised column labels | PORTED | CsvTableReaderTest.kt: `a file with no header row gets synthesised column labels` | |
| sample > a file containing only a header row samples as empty | PORTED | CsvTableReaderTest.kt: `a file containing only a header row samples as empty` | |
| sample > columnValues skips blank cells in the requested column | PORTED | CsvTableReaderTest.kt: `columnValues skips blank cells in the requested column` | |
| rows > the header row is not emitted as data | PORTED | CsvTableReaderTest.kt: `the header row is not emitted as data` | |
| rows > bytes read grow as rows are emitted | DIVERGED | CsvTableReaderTest.kt: `bytes read grow as rows are emitted` | strengthened to monotonic growth plus reaching EOF; still no byteLength() API in Kotlin (totalBytes comes from the SAF cursor) |
| rows > a missing file reports a read failure rather than hanging | PORTED | CsvTableReaderTest.kt: `a missing file reports a read failure rather than hanging` | Kotlin adds a sample() variant too |
| CsvRow.cell > a short row reports null rather than throwing | PORTED | CsvTableReaderTest.kt: `a short row reports null rather than throwing` | |
| CsvRow.cell > a blank cell reads as null so a gap is not parsed as zero | PORTED | CsvTableReaderTest.kt: `a blank cell reads as null so a gap is not parsed as zero` | |

## test/features/imports/route_bulk_import_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/RouteBulkImportTest.kt (bulk route import lives in SettingsViewModel.importRouteFiles in the Kotlin port), plus features/settings/SettingsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| imports every file and reports the counts | PORTED | SettingsViewModelTest.kt: `bulk route import writes all selected files in one batched call` | |
| progress counts the files as they land | PORTED | RouteBulkImportTest.kt: `progress counts the files as they land` | snapshots read inside the importer (uiState is conflated); Flutter's queued 0-of-N tick is unobservable in Kotlin |
| a refused write permission fails one file, not the batch | PORTED | RouteBulkImportTest.kt: `a refused write permission fails one file, not the batch` | error text plus zero writes reaching the repository |
| a failed write surfaces the failure message | PORTED | RouteBulkImportTest.kt: `a failed write surfaces the failure message` | exact routeImportError text asserted |
| a malformed file is tolerated and its parse error reported | PORTED | RouteBulkImportTest.kt: `a malformed file is tolerated and its parse error reported` | only the wellness-FIT fallback path of a parse exception is tested |
| opens files as it reaches them, never the whole folder up front | PORTED | RouteBulkImportTest.kt: `opens files as it reaches them, never the whole folder up front` | 60 files; read/write interleave ordering asserted |
| writes activities in batches, not one Health Connect call per file | PORTED | RouteBulkImportTest.kt: `writes activities in batches, not one Health Connect call per file` | Flutter's [25,25,10] chunking now asserted |
| a rejected batch is retried file by file, so one bad file fails alone | PORTED | SettingsViewModelTest.kt: `a failed batch retries file by file so only the guilty file fails` | |
| a spent Health Connect quota stops the run instead of failing every file | PORTED | RouteBulkImportTest.kt: `a spent Health Connect quota stops the run instead of failing every file` | 60 files, one batch attempted, no single-write retry, remaining files never opened |
| a file that cannot be opened fails that file, not the batch | PORTED | RouteBulkImportTest.kt: `a file that cannot be opened fails that file, not the batch` |  |
| an activity FIT file is imported as an activity, not skipped as wellness | PORTED | RouteBulkImportTest.kt: `an activity file is imported as an activity, not skipped as wellness` | Kotlin has no skippedFiles field and reaches the wellness fallback only from the failure branch; asserts the fallback is never consulted |
| an empty pick does nothing | PORTED | RouteBulkImportTest.kt: `an empty pick does nothing` |  |

## test/features/imports/route_import_intent_test.dart
Kotlin counterpart: none (Kotlin handles this natively in MainActivity via ExternalRouteImportRequest; no channel exists and no tests)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| RouteImportIntentChannel > maps the native payload to an ActivityRouteFileHandle | N/A-FRAMEWORK | — | MethodChannel plumbing; Kotlin receives ACTION_VIEW intents directly (untested) |
| RouteImportIntentChannel > returns null when nothing is pending | N/A-FRAMEWORK | — | |
| RouteImportIntentChannel > a missing plugin (non-Android host) is not an error | N/A-FRAMEWORK | — | Flutter-only host concern |
| RouteImportIntentBootstrap > a pending file is parked in the seam and opens the form | N/A-FRAMEWORK | — | go_router navigation; Kotlin equivalent (MainActivity → ActivityEntryScreen pending-import) untested |
| RouteImportIntentBootstrap > nothing pending leaves the seam empty and does not navigate | N/A-FRAMEWORK | — | |
## test/features/sleep/application/sleep_asleep_hours_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepSessionMergingTest.kt, features/sleep/SleepPresentationMapperTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| excludes awake time within the session from the sleep duration | PORTED | SleepSessionMergingTest.kt: `sleepDurationMsFromStages excludes awake stages when sleep stages are present` (also SleepPresentationMapperTest.kt: `duration points are asleep hours with awake stages excluded`) | |
| falls back to time in bed when the night has no stage data | PORTED | SleepPresentationMapperTest.kt: `duration points fall back to time in bed when the night has no stage data` | `sleepDurationMsFromStages(stages, fallbackDurationMs)` empty-stages fallback path never exercised |
| is zero when there is no night for the date | PORTED | SleepPresentationMapperTest.kt: `asleep hours are zero when there is no night for the date` | no Kotlin test asserts zero asleep hours for a date with no sessions |

## test/features/sleep/presentation/sleep_stage_scrubber_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/sleep/SleepStageScrubberTest.kt (over `sleepScrubTimeAt` / `sleepStageTypeAt`), plus ui/charts/ChartScrubberTest.kt for the line-chart rule
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| inert until touched — no crosshair, no tooltip | PORTED | SleepStageScrubberTest: `theHypnogramStaysSilentUntilAFingerIsOnIt` | Compose instrumentation; runs on a device, not in CI |
| a horizontal drag reveals the clock time and stage at the finger | PORTED | SleepStageScrubberTest.kt: `a horizontal drag reveals the clock time and stage at the finger` | core only; the gesture itself is widget-level |
| the time tracks the finger across the night | PORTED | SleepStageScrubberTest.kt: `the time tracks the finger across the night` | same uncovered fraction→clock-time mapping |
| a VERTICAL drag starting on the chart still scrolls the page | PORTED | SleepStageScrubberTest: `aVerticalDragStartingOnTheHypnogramStillScrollsThePage` | Compose instrumentation; runs on a device, not in CI |

## test/features/sleep/sleep_detail_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/sleep/SleepDetailViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders summary, breakdown, details and stage events | PORTED | SleepDetailScreenTest: `theDetailScreenShowsTheNightItsBreakdownItsMetadataAndItsStageEvents` | Compose instrumentation; runs on a device, not in CI |
| the stage lane chart paints the cross-lane connector without throwing | PORTED | SleepDetailScreenTest: `aNightOfBackToBackStagesDrawsWithoutTearingTheChart` | Compose instrumentation; runs on a device, not in CI |
| shows the no-stages message when a session has no stages | PORTED | SleepDetailScreenTest: `aNightWithoutStagesSaysSoRatherThanShowingAnEmptyChart` | Compose instrumentation; runs on a device, not in CI |
| shows an error when the session is missing | PORTED | SleepDetailViewModelTest.kt: `missing sleep session sets not found error` | VM-level equivalent (ScreenError.NotFound vs error text) |

## test/features/sleep/sleep_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/sleep/SleepPresentationMapperTest.kt and SleepMetricSectionsTest.kt (stage shares, entry ordering, manual-entry confidence, schedule-chart rule, over the extracted `sleepPeriodTotals` seam), plus domain/insights tests
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty period derives zeroes, not nulls | PORTED | SleepPresentationMapperTest.kt: `an empty period derives zeroes not nulls` + SleepMetricSectionsTest.kt: `an empty period derives zeroes not NaN` | mapper never tested with an empty period |
| only the nights that recorded sleep count as nights | PORTED | SleepPresentationMapperTest.kt + SleepMetricSectionsTest.kt: `only the nights that recorded sleep count as nights` | totals, average, longest and night-count now asserted |
| the stage shares split the recorded stage time, and only it | PORTED | SleepMetricSectionsTest.kt: `the stage shares split the recorded stage time and only it` | stage-share percents/fractions computed in SleepStageShareCard composable, untested |
| the entry lists come out newest night first | PORTED | SleepMetricSectionsTest.kt: `the entry lists come out newest night first` | ordering of period nights vs raw sessions untested |
| a week whose nights know their bedtimes gets the schedule chart | PORTED | SleepMetricSectionsTest.kt: `a week whose nights know their bedtimes gets the schedule chart` | useScheduleChart + scheduleDays now asserted |
| a night short of the goal reads as below target | DIVERGED | MetricInterpretationsTest.kt: `interpretsSleepAgainstUserTarget` | classification covered at domain level; display wiring and goalMetDays not |
| sleep and HRV only correlate once enough nights pair up | PORTED | CrossMetricInsightsTest.kt: `returnsNullWhenThereAreNotEnoughPairs` + `calculatesPositiveCorrelationForPairedDays` | at domain `crossMetricInsight` level |
| manual-entry confidence > an actively-recorded night is not a manual entry | PORTED | SleepMetricSectionsTest.kt: `an actively-recorded night is not a manual entry` | recordingMethod→manualEntryCount derivation lives untested in SleepMetricOrderedSections.kt:516; DataConfidenceTest only covers downstream warning |
| manual-entry confidence > a hand-typed night is | PORTED | SleepMetricSectionsTest.kt: `a hand-typed night is a manual entry` | same |

## test/features/sleep/sleep_overview_card_test.dart
Kotlin counterpart: none (card itself); underlying data in SleepPresentationMapperTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| highlights Sleep (asleep) and demotes Time in bed | PORTED | SleepScreenSectionsTest: `theOverviewKeepsAsleepTimeInBedAndAwakeAsThreeDistinctFigures` | Compose instrumentation; runs on a device, not in CI |

## test/features/sleep/sleep_schedule_chart_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/sleep/SleepScheduleAxisTest.kt and SleepScheduleDaysTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| anchoredMinutes > the 18:00 anchor is minute zero | PORTED | SleepScheduleAxisTest.kt: `the anchor itself is minute zero` | anchoredMinutes asserted at one point only; anchor→0 identity not pinned |
| anchoredMinutes > an evening bedtime sits early on the axis | PORTED | SleepScheduleAxisTest.kt: `normalized end minutes stay monotone across the anchor wrap` | 23:30 → 90 with 22:00 anchor |
| anchoredMinutes > a morning wake-up wraps past midnight, not back to the top | PORTED | SleepScheduleAxisTest.kt: `normalized end minutes stay monotone across the anchor wrap` | 07:15 → 555 exercises the wrap |
| anchoredMinutes > is always inside a single day | PORTED | SleepScheduleAxisTest.kt: `anchoredMinutes is always inside a single day` | 0 ≤ value < 1440 sweep untested |
| normalizedEndMinutes > a wake-up after the bedtime stays after it | PORTED | SleepScheduleAxisTest.kt: `normalized end minutes stay monotone across the anchor wrap` | |
| normalizedEndMinutes > an afternoon nap that crosses the anchor still moves forward | PORTED | SleepScheduleAxisTest.kt: `normalized end minutes stay monotone across the anchor wrap` | end-before-start wrap-forward branch asserted |
| anchoredMinuteToClock > round-trips the anchor and a wrapped morning | PORTED | SleepScheduleAxisTest.kt: `clockTime round-trips the anchor and a wrapped morning` | Kotlin `SleepScheduleAxis.clockTime` untested |
| anchoredMinuteToClock > minuteOfDayToAnchored is its inverse | PORTED | SleepScheduleAxisTest.kt: `anchoredClockMinute is the inverse of clockTime` | Kotlin `SleepScheduleAxis.anchoredClockMinute` untested |
| scheduleAxisRange > is null when no night has a bedtime | PORTED | SleepScheduleAxisTest.kt: `no plausible nights yields a null axis` | |
| scheduleAxisRange > spans every night, padded to whole hours | PORTED | SleepScheduleAxisTest.kt: `range pads partial hours out to whole hours` | |
| scheduleAxisRange > one impossible night does not stretch the axis past a day | PORTED | SleepScheduleAxisTest.kt: `a 20h night is excluded from the range while a 7h night sets it` | |
| scheduleAxisRange > a long but possible lie-in still counts | PORTED | SleepScheduleAxisTest.kt: `a long but possible lie-in still counts` | 14 h positive case added |
| scheduleAxisRange > is null when every night is impossible | PORTED | SleepScheduleAxisTest.kt: `no plausible nights yields a null axis` | |
| scheduleAxisRange > label ticks are hourly, thinning to two-hourly over eight hours | PORTED | SleepScheduleAxisTest.kt: `tick step is hourly at an 8h span and 2-hourly above it` | |
| toSleepScheduleDays > maps a merged night to its span and stages | PORTED | SleepScheduleDaysTest.kt: `maps a merged night to its span and stages` | Kotlin `List<SleepOverviewDay>.toSleepScheduleDays()` (SleepPresentationMapper.kt:185) untested |
| toSleepScheduleDays > a night with no stages carries an empty stage list | PORTED | SleepScheduleDaysTest.kt: `a night with no stages carries an empty stage list` |  |
| toSleepScheduleDays > a date with no night has no bedtime | PORTED | SleepScheduleDaysTest.kt: `a date with no night has no bedtime` |  |
| toSleepScheduleDays > days come out in date order | PORTED | SleepScheduleDaysTest.kt: `days come out in date order` |  |
| widget > renders nothing when no night has a bedtime | PORTED | SleepChartsTest: `theScheduleChartRendersNothingWhenNoNightHasABedtime` | Compose instrumentation; runs on a device, not in CI |
| widget > draws the chart and its summary | PORTED | SleepChartsTest: `theScheduleChartDrawsItsTitleAndSummary` | Compose instrumentation; runs on a device, not in CI |
| widget > an impossible night still renders, it just does not scale | PORTED | SleepChartsTest: `anImpossibleNightStillRenders_itJustDoesNotScale` | Compose instrumentation; runs on a device, not in CI |
| widget > tapping a night reports that night, not its neighbour | PORTED | SleepChartsTest: `tappingANightReportsThatNightAndNotItsNeighbour` | Compose instrumentation; runs on a device, not in CI |

## test/features/sleep/sleep_screen_test.dart
Kotlin counterpart: app/src/androidTest/kotlin/tech/mmarca/openvitals/features/sleep/SleepScreenWeekTest.kt (partial)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Sleep screen renders the Kotlin ordered sections | DIVERGED | SleepScreenWeekTest.kt: `sleepWeekView_showsPeriodNavigatorAndWeekContent` | instrumentation test asserts only navigator + week-content tag, not goal/confidence/sessions/target sections |
| the goal steppers move and persist the sleep target | PORTED | SleepViewModelTest.kt: `the goal steppers move and persist the sleep target` | quarter-hour step 8.0 -> 8.25 -> 7.75, and every step persisted |
| the sleep-vs-HRV card needs enough paired nights | PORTED | SleepScreenSectionsTest: `aCoupleOfNightsIsNotEnoughToClaimASleepHrvCorrelation` | Compose instrumentation; runs on a device, not in CI |
| a week of nights paired with HRV shows the correlation card | PORTED | SleepScreenSectionsTest: `aWeekOfNightsPairedWithHrvShowsTheCorrelationCard` | Compose instrumentation; runs on a device, not in CI |
| the day view closes with the data-source education link | PORTED | SleepScreenSectionsTest: `theDayViewClosesWithTheDataSourceEducationLink` | Compose instrumentation; runs on a device, not in CI |
| a period view renders when the selected day has no sleep but the period does | PORTED | SleepScreenSectionsTest: `aPeriodViewStillRendersWhenTheSelectedDayHasNoSleep` | Compose instrumentation; runs on a device, not in CI |
| the day view still says so when the selected day has no sleep | PORTED | SleepScreenSectionsTest: `theDayEmptyStateNamesTheSelectedDayAndNotThePeriod` | Compose instrumentation; runs on a device, not in CI |
| Sleep screen shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |

## test/features/sleep/sleep_stage_share_card_test.dart
Kotlin counterpart: none (rendering); merged-id logic in domain/model/SleepSessionMergingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the stage fills are actually painted — non-zero HEIGHT | N/A-WIDGET | | Flutter Stack/loose-constraints layout regression |
| a bigger stage gets a wider bar than a smaller one | N/A-WIDGET | | pixel-width assertion |
| no stage data hides the card rather than drawing empty bars | PORTED | SleepChartsTest: `noStageDataHidesTheShareCardRatherThanDrawingEmptyBars` | Compose instrumentation; runs on a device, not in CI |
| the night is drawn as a lane chart, not a flat bar | PORTED | SleepSessionTimelineCardTest: `theNightIsDrawnAsALaneChartAndNotAFlatBar` | Compose instrumentation; runs on a device, not in CI |
| tapping the card opens that night, and it says so | PORTED | SleepSessionTimelineCardTest: `tappingTheCardOpensThatNightAndSaysSoFirst` | Compose instrumentation; runs on a device, not in CI |
| a merged night offers no detail to open | PORTED | SleepSessionTimelineCardTest: `aMergedNightOffersNoDetailToOpen` | Compose instrumentation; runs on a device, not in CI |

## test/features/sleep/sleep_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/sleep/SleepViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | SleepViewModelTest.kt: `a loaded period lands with its display precomputed` | 7 points, night count, average and goal target asserted |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | SleepViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | error type, cleared loading and empty sessions asserted |
| an unexpected failure carries its message to the screen | PORTED | SleepViewModelTest.kt: `load failure sets error message` | |
| refresh reloads the current selection in force mode | PORTED | SleepViewModelTest.kt: `refresh reloads the current selection in force mode` | `resumeCurrentPeriod(refreshCurrent = true)` / RefreshMode.FORCE untested |
| moving the goal rebuilds the display without reloading | PORTED | SleepViewModelTest.kt: `moving the goal rebuilds the display without reloading` | goal-step-without-repo-reload untested (Kotlin computes goal progress in composable) |
| navigating to a new range clears the stale display mid-load | N/A-BEHAVIOR | none | blocked on behavior decision - Kotlin keeps the previous display through a load and only flips isLoading; there is no nullable display to clear |
| a same-range refresh keeps the display (no loading flash) | PORTED | SleepViewModelTest.kt: `a same-range refresh keeps the display` | no coverage |
| a stale load cannot overwrite the newer one it lost to | PORTED | SleepViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | single-flight/staleness guard untested |
## test/features/bodyenergy/body_energy_chart_zoom_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartViewportTest.kt (zoom math only)

Note: Kotlin bodyenergy belongs to a separate workstream; classified against what exists today.

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| pinching the Body Energy chart zooms the line, strip and hours | PORTED | BodyEnergyChartZoomTest: `pinchingTheChartZoomsTheLineStripAndHoursTogether` | Compose instrumentation; runs on a device, not in CI |

## test/features/bodyenergy/body_energy_details_screen_test.dart
Kotlin counterpart: none (BodyEnergyScreen.kt has no Compose/androidTest; mapper covered separately by BodyEnergyPresentationMapperTest.kt)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Body Energy renders the timeline chart once loaded | PORTED | BodyEnergyCardsTest: `theDayCardRendersTheTimelineOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| the Daily Readiness card rides along on the same day | N/A-WIDGET | — | day-follow guard in DailyReadinessCard.kt/DailyReadinessViewModel.kt is untested in Kotlin |
| Body Energy renders the "how it is estimated" card | PORTED | BodyEnergyCardsTest: `theExplainerCardsSayWhatMovedItAndHowItIsEstimated` | Compose instrumentation; runs on a device, not in CI |
| Body Energy shows only the calibration card until setup completes | N/A-WIDGET | — | setupCompleted flag round-trip covered by BodyEnergyCalibrationTest (`automatic calibration defaults to setup not completed`, `normalization preserves setupCompleted flag`); the screen gating itself untested |
| Body Energy reveals the timeline after calibration is saved | N/A-WIDGET | — | BodyEnergyViewModel.completeSetup untested (no BodyEnergyViewModelTest exists) |
| Body Energy setup refuses to complete without a birth year | PORTED | BodyEnergyCalibrationCardTest: `setupRefusesToCompleteWithoutABirthYear` | Compose instrumentation; runs on a device, not in CI |
| Body Energy shows the access gate when permission missing | N/A-WIDGET | — | generic error mapping covered by core/presentation ScreenErrorTest; bodyenergy permission path untested |
| Body Energy shows the empty state with no timeline | PORTED | BodyEnergyCardsTest: `withNoTimelineAtAllTheCardShowsNoScoreRatherThanAZero` | Compose instrumentation; runs on a device, not in CI |

## test/features/bodyenergy/body_energy_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/bodyenergy/BodyEnergyPresentationMapperTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no timeline at all is an empty display | PORTED | BodyEnergyPresentationMapperTest.kt: `no timeline at all is an empty display` | nullable-receiver extension |
| a timeline with no points still explains its inputs | PORTED | BodyEnergyPresentationMapperTest.kt: `missing input rows expose sparse body energy inputs` | Kotlin asserts more row statuses (sleep/workouts/calibration) though not the isEmpty flag |
| points become day fractions, and the strip scales to its tallest bar | PORTED | BodyEnergyPresentationMapperTest.kt: `influence bars preserve bucket x fractions` + `max influence magnitude floors at one so an empty day divides by something` | split across two tests; same xFraction and tallest-bar assertions |
| the reasons rank charge and drain together, and drop the trivial ones | PORTED | BodyEnergyPresentationMapperTest.kt: `top reasons summarize largest charge and drain contributors` + `reasons below the minimum amount are dropped` | full rank order now pinned as a list |
| the legend lists only the influences that actually moved the score | PORTED | BodyEnergyPresentationMapperTest.kt: `the legend lists only the influences that actually moved the score` | `legendInfluences` is computed in BodyEnergyPresentationMapper.kt:124 but never asserted |

## test/features/bodyenergy/body_energy_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/bodyenergy/BodyEnergyViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded day lands with its display precomputed | PORTED | BodyEnergyViewModelTest.kt: `a loaded day lands with its display precomputed` | standard coroutine VM test, portable |
| a day with no timeline at all still gives the screen a display | PORTED | BodyEnergyViewModelTest.kt: `a day with no timeline at all still gives the screen a display` |  |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | BodyEnergyViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | the former message-path test now asserts the typed error, as in the Dart original |
| an unexpected failure carries its message to the screen | PORTED | BodyEnergyViewModelTest.kt: `an unexpected failure carries its message to the screen` |  |
| a future day is clamped to today | PORTED | BodyEnergyViewModelTest.kt: `a future day is clamped to today` |  |
| a stale load cannot overwrite the newer one it lost to | PORTED | BodyEnergyViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` |  |

## test/features/readiness/daily_readiness_card_test.dart
Kotlin counterpart: none (DailyReadinessCard.kt untested)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the readiness verdict for the host day | PORTED | DailyReadinessPanelTest: `rendersTheReadinessVerdictForTheHostDay` | Compose instrumentation; unblocked by the readinessInsight() fixture |
| no self-link: the card offers Training but not Body energy | PORTED | DailyReadinessPanelTest: `noSelfLink_theCardOffersTrainingButNotBodyEnergy` | Compose instrumentation; unblocked by the readinessInsight() fixture |
| a day the provider has not reached yet shows a placeholder | N/A-WIDGET | — | day-guard placeholder logic in the Kotlin card is untested |

## test/features/readiness/readiness_display_test.dart
Kotlin counterpart: none for the display compositions (panel lines live as private functions in DailyReadinessPanel.kt, detail spec in ReadinessScoreDetailsScreen.kt); partial neighbors: features/readiness/StressL10nTest.kt (stress strings), domain/insights/DailyReadinessTest.kt (insight computation)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| daily readiness panel > composes every line the panel used to build inline | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |
| daily readiness panel > an empty insight still composes: no strain, no score, no factors | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |
| daily readiness panel > an unrecognised confidence reason falls back to partial data | PORTED | ReadinessConfidenceTextTest: `an unrecognised confidence reason falls back to partial data` | The mapping moved out of the two screens into `ReadinessConfidenceText`; each had its own copy, both built from hardcoded English that never reached strings.xml |
| daily readiness panel > the factor list is capped at five | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |
| training readiness detail > score, verdict, confidence, signals and guidance | MISSING | none | blocked - readinessDetailSpec is private in ReadinessScoreDetailsScreen.kt (other workstream) |
| training readiness detail > no training-side factors falls back to the no-signals message | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |
| training readiness detail > an unknown state reads as needs-more-data, whatever the score | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |
| training readiness detail > the verdict bands | MISSING | none | blocked - scoreBandLabel is private in ReadinessScoreDetailsScreen.kt (other workstream) |
| training readiness detail > the strain bullet always renders, from the state | MISSING | none | blocked - needs a production seam in features/readiness (owned by another workstream) |

## test/features/readiness/training_readiness_details_screen_test.dart
Kotlin counterpart: none (ReadinessScoreDetailsScreen.kt has no test); date-arg parsing covered by navigation/SelectedDayArgTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders score, verdict, and training factor group | PORTED | TrainingReadinessDetailsTest: `rendersTheScoreVerdictAndTheTrainingSignalsBehindIt` | Compose instrumentation; runs on a device, not in CI |
| falls back to the no-signals message when factors are empty | PORTED | TrainingReadinessDetailsTest: `withNoTrainingSideSignalsItSaysSoRatherThanShowingAnEmptyCard` | Compose instrumentation; runs on a device, not in CI |
| shows the needs-more-data verdict for an unknown state | PORTED | TrainingReadinessDetailsTest: `anUnknownStateReadsAsNeedsMoreDataHoweverHighTheScore` | Compose instrumentation; runs on a device, not in CI |
| an invalid date argument falls back to today and still renders | N/A-WIDGET | — | arg-fallback core covered by SelectedDayArgTest.kt: `absent or malformed values read as no pin` |

## test/features/readiness/training_readiness_details_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/readiness/DailyReadinessViewModelTest.kt (the detail *display spec* still lives inside ReadinessScoreDetailsScreen.kt and is untestable without a seam there)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| load publishes the insight and its precomputed display | DIVERGED | DailyReadinessViewModelTest.kt: `load publishes the insight for the loaded day` | insight covered; the detail display spec is built inside ReadinessScoreDetailsScreen.kt (other workstream) |
| a future day is clamped to today | PORTED | DailyReadinessViewModelTest.kt: `a future day is clamped to today` |  |
| day navigation loads the day it moves to, and stops at today | PORTED | DailyReadinessViewModelTest.kt: `day navigation loads the day it moves to, and stops at today` |  |
| refresh reloads the selected day, forcing it | PORTED | DailyReadinessViewModelTest.kt: `refresh reloads the selected day, forcing it` |  |
| a stale day cannot overwrite the day that overtook it | PORTED | DailyReadinessViewModelTest.kt: `a stale day cannot overwrite the day that overtook it` |  |
| a failed load becomes a ScreenError, not an exception | MISSING | — | blocked - the seam lives in features/readiness (owned by another workstream) |
## test/features/heart/heart_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/heart/HeartMetricStatsTest.kt (BP / SpO2 / skin-temp / axis-floor stats), HeartPresentationMapperTest.kt, HeartViewModelTest.kt, features/vitals/HeartVitalsSummariesTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| heart rate > a day of samples sorts oldest first and takes its extremes | PORTED | HeartMetricStatsTest.kt: `a day of samples sorts oldest first and takes its extremes` | flags/counts and sorting asserted; min/max/avg and axis padding values not |
| heart rate > a period keeps both orders: oldest first, and newest first | PORTED | HeartViewModelTest.kt: `load success populates display metric for average heart rate` | both orders and extremes now asserted |
| heart rate > the threshold checks count the days that crossed the line | PORTED | HeartViewModelTest.kt: `heart rate checks count days in multi-day ranges` (+ `heart rate checks count samples in day range using configured thresholds`) | |
| resting heart rate > the day falls back to the mean of its samples with no aggregate | DIVERGED | data/repository/HeartRepositoryTest.kt: `DAY resting heart rate uses raw full samples for selected day graph` | Kotlin derives dayRestingBpm = sample mean in the repository; low/high-from-samples display values unasserted |
| resting heart rate > an aggregate with no samples still yields one reading | DIVERGED | HeartPresentationMapperTest.kt: `resting heart rate display populates day value` | day value + sampleCount 1 asserted; low/high collapse onto the aggregate not |
| vitals > blood pressure keeps the latest reading and the highest one | PORTED | HeartMetricStatsTest.kt: `blood pressure keeps the latest reading and the highest one` | latest asserted at VM level; highest/averageSystolic/sort (HeartMetricSharedSections.kt:487) untested |
| vitals > SpO2 averages every reading and sorts the series | PORTED | HeartMetricStatsTest.kt: `spO2 averages every reading and keeps its extremes` | SpO2 average/sort/stats computed in composables, untested |
| vitals > skin temperature excludes deltaless entries from the arithmetic | PORTED | HeartMetricStatsTest.kt: `skin temperature excludes deltaless entries from the arithmetic` | filter at HeartVitalDetailContent.kt:564 untested |
| vitals > skin temperature with no delta anywhere has no statistics at all | PORTED | HeartMetricStatsTest.kt: `skin temperature with no delta anywhere has no statistics at all` |  |
| an empty period leaves every section null | DIVERGED | HeartPresentationMapperTest.kt: `average heart rate display has no data for empty week` | only the HR metric's hasData=false; other sections and the empty threshold checks unasserted |
| a day average never sits outside its own range > resting heart rate averages the samples it also ranges | PORTED | HeartMetricStatsTest.kt: `resting heart rate averages the samples it also ranges` | avg-inside-own-range invariant not asserted anywhere in Kotlin |
| a day average never sits outside its own range > with no samples the provider aggregate is all there is | PORTED | HeartMetricStatsTest.kt: `with no samples the provider aggregate is all there is` | aggregate-only day covered; low/high collapse not |
| respiratory rate reports ONE average, the one under its chart | PORTED | HeartVitalsSummariesTest.kt: `week respiratory summary averages daily buckets` (+ `day respiratory summary uses latest reading`) | same mean-of-daily-means rule |

## test/features/heart/heart_metric_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/heart/HeartViewModelTest.kt, core/presentation/MetricDetailSectionOrderViewModelTest.kt (screen itself has no Compose test; androidTest MetricDetailScaffoldTest covers the shared scaffold)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Heart rate screen renders the ordered period sections | PORTED | HeartMetricContentTest: `aLoadedPeriodRendersTheOrderedSections` | Compose instrumentation; runs on a device, not in CI |
| Average heart rate period view renders the data-source education item | PORTED | HeartMetricContentTest: `aLoadedPeriodCarriesTheDataSourceEducationLink` | Compose instrumentation; runs on a device, not in CI |
| Heart rate screen shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |
| Heart rate screen shows the empty placeholder with no data | PORTED | HeartMetricContentTest: `showsTheEmptyPlaceholderWithNoData` | Compose instrumentation; runs on a device, not in CI |
| Threshold steppers persist to SharedPreferences | PORTED | HeartViewModelTest.kt: `updating high heart rate threshold persists and recalculates checks` | persist + recalculation covered; stepper tap itself is widget-only |
| Section reorder persists the new order | DIVERGED | MetricDetailSectionOrderViewModelTest.kt: `moveSectionToTarget_reordersAndPersists` | persist call verified; re-read by a fresh instance not asserted |

## test/features/heart/heart_metric_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/heart/HeartViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | HeartViewModelTest.kt: `load success populates display metric for average heart rate` | display populated asserted; order/avg/low/high values not |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | HeartViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | a refused heart read maps to ScreenError.PermissionDenied; the missing-permission state stays a separate signal |
| an unexpected failure carries its message to the screen | PORTED | HeartViewModelTest.kt: `load failure sets error and clears loading` | ScreenError.Message("error") asserted |
| refresh reloads the current selection in force mode | PORTED | HeartViewModelTest.kt: `refresh reloads the current selection in force mode` | no refresh()/RefreshMode.FORCE reload test (FORCE only asserted on the delete-reload path) |
| a moved threshold rebuilds the checks against the loaded samples | PORTED | HeartViewModelTest.kt: `updating high heart rate threshold persists and recalculates checks` | |
| a stale load cannot overwrite the newer one it lost to | PORTED | HeartViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | no staleness/single-flight guard test |

## test/features/heart/heart_rate_recovery_period_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/recovery/HeartRateRecoveryViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a strap gives a chartable point per workout | DIVERGED | HeartRateRecoveryViewModelTest.kt: `only comparable readings feed the chart and the rest are counted as unmeasured` | chartable/comparable covered; ordering contract is reversed by design (Kotlin newest-first vs Flutter oldest-first) |
| a watch that stops recording gives workouts with NO chartable point — and they are still counted | PORTED | HeartRateRecoveryViewModelTest.kt: `only comparable readings feed the chart and the rest are counted as unmeasured` | |
| an ordinary workout with no cessation mark is never even read | PORTED | HeartRateRecoveryViewModelTest.kt: `only sessions of five minutes or more with a stop mark cost a heart rate read` (+ `a period with no candidates issues no heart rate reads at all`) | |
| a period bigger than the cap says so rather than quietly showing less | PORTED | HeartRateRecoveryViewModelTest.kt: `sessions come back newest first and a period over the cap is truncated and says so` | |

## test/features/heart/heart_rate_recovery_screen_test.dart
Kotlin counterpart: none (screen); VM covered by features/recovery/HeartRateRecoveryViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the period view without an error for a normal week | PORTED | HeartRateRecoveryEmptyCardTest: `theEmptyPeriodCardSaysBothThatThereIsNothingAndWhy` | Compose instrumentation; runs on a device, not in CI |

## test/features/vitals/heart_vitals_overview_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/vitals/HeartVitalsOverviewCardsTest.kt (per-card stats), HeartVitalsSummariesTest.kt, HeartVitalsRangeSummaryTest.kt, features/heart/HeartMetricStatsTest.kt, HeartPresentationMapperTest.kt, HeartViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| heart rate > a day of samples sorts oldest first and averages them | PORTED | HeartVitalsOverviewCardsTest.kt: `a day of samples sorts oldest first and averages them` | sorting covered; day average/min/max values unasserted |
| heart rate > the axis floors at 30, never at min-5 below it | PORTED | HeartMetricStatsTest.kt: `the intraday axis floors at 30 never at min minus five below it` | generic padding floor covered; the HR-specific 30 bpm floor (HeartCards.kt:48) untested |
| heart rate > a lone day sample still fills the card but draws no timeline | PORTED | HeartPresentationMapperTest.kt: `average heart rate display hides timeline for single day sample` | |
| heart rate > the day card names its source only when the samples agree | PORTED | HeartVitalsOverviewCardsTest.kt: `the day card names its source only when the samples agree` | source-agreement (distinct().singleOrNull()) asserted only for respiratory, not heart rate |
| heart rate > a period takes its extremes across every daily summary | PORTED | HeartVitalsOverviewCardsTest.kt: `a period takes its extremes across every daily summary` | rangeSummary non-null only; extremes/mean values unasserted |
| resting heart rate and HRV > a day reads the provider aggregate, not the daily series | PORTED | HeartVitalsOverviewCardsTest.kt: `a day reads the provider aggregate not the daily series` | aggregate path covered; aggregate-beats-series contrast not asserted |
| resting heart rate and HRV > a period averages the daily series and keeps its extremes | PORTED | HeartVitalsRangeSummaryTest.kt: `resting heart rate summary reports the real average and range` + `hrv summary reports the real average and range` (+ mapper week-trend average) | |
| resting heart rate and HRV > a day with no resting aggregate has no resting card | PORTED | HeartVitalsOverviewCardsTest.kt: `a day with no resting aggregate has no resting card` | null aggregate covered; no-card rule despite a present daily series not |
| cardiovascular > blood pressure sorts, counts and takes the latest reading | PORTED | HeartVitalsOverviewCardsTest.kt: `blood pressure sorts counts and takes the latest reading` | latest (122) asserted; sort/readings/hasChart untested |
| cardiovascular > SpO2 and blood glucose average every reading | DIVERGED | HeartViewModelTest.kt: `load success populates selected vitals and latest values` | latest covered per selected metric; every-reading averages computed in composables, unasserted |
| cardiovascular > a long-range overview reads native daily aggregates | PORTED | HeartVitalsOverviewCardsTest.kt: `a long-range overview reads native daily aggregates` | count-weighted mean and raw-count total now asserted |
| cardiovascular > within a day one timestamp draws no chart, two do | PORTED | HeartVitalsOverviewCardsTest.kt: `within a day one timestamp draws no chart and two do` | hasChart threshold for vitals untested (HR analog covered by the mapper single-sample test) |
| respiratory > a period card and chart both print the mean of the daily means | PORTED | HeartVitalsSummariesTest.kt: `week respiratory summary averages daily buckets` | |
| respiratory > a day card prints the latest reading, the chart the daily mean | PORTED | HeartVitalsSummariesTest.kt: `day respiratory summary uses latest reading` + `respiratoryRateDaySummaries groups readings by date` | |
| respiratory > a long-range card names the latest reading source | DIVERGED | HeartVitalsSummariesTest.kt: `day respiratory summary uses latest reading` (source asserted) | week variant reports a "3 readings" count as source instead; long-range latest-source not asserted |
| respiratory > skin temperature charts a daily delta point per day, weighted mean | PORTED | HeartVitalsOverviewCardsTest.kt: `skin temperature charts a daily delta point per day with a weighted mean` | skin-temp daily aggregation in composable, untested |
| respiratory > day view charts only the raw entries that carry a delta | PORTED | HeartVitalsOverviewCardsTest.kt: `day view charts only the raw entries that carry a delta` | delta filter (HeartVitalDetailContent.kt:564) untested |
| respiratory > a delta-less newest entry does not blank the card (day) | PORTED | HeartVitalsOverviewCardsTest.kt: `a delta-less newest entry does not blank the card (day)` | fixed: the card reads `skinTemperatureCardDeltaCelsius()` - the newest entry that carries a delta, the same population the chart draws |
| respiratory > a day with no delta anywhere shows nothing, card or chart | PORTED | HeartVitalsOverviewCardsTest.kt: `a day with no delta anywhere shows nothing card or chart` |  |
| respiratory > body temperature counts its readings and takes the latest (day) | PORTED | HeartVitalsOverviewCardsTest.kt: `body temperature counts its readings and takes the latest` | latest pattern covered; readings count untested |
| respiratory > body temperature over a long range totals its daily reading counts | PORTED | HeartVitalsOverviewCardsTest.kt: `body temperature over a long range totals its daily reading counts` |  |
| an empty period derives an empty display, section by section | PORTED | HeartVitalsOverviewCardsTest.kt: `an empty period derives an empty display section by section` | no all-sections-empty derivation test |
| vo2 max sorts, counts and takes the latest reading (day) | PORTED | HeartVitalsOverviewCardsTest.kt: `vo2 max sorts counts and takes the latest reading` | latest pattern covered; sort/count untested |
| vo2 max over a long range totals its daily reading counts | PORTED | HeartVitalsOverviewCardsTest.kt: `vo2 max over a long range totals its daily reading counts` |  |

## test/features/vitals/heart_vitals_overview_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/vitals/HeartVitalsSummariesTest.kt, features/heart/HeartViewModelTest.kt, core/presentation/MetricDetailSectionOrderViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| respiratory flat mean and daily-bucket mean stay distinct | PORTED | HeartVitalsSummariesTest.kt: `week respiratory summary averages daily buckets` | uneven reading counts pin the equal-weighting rule |
| renders the three reorderable group sections | PORTED | HeartVitalsOverviewContentTest: `theThreeGroupSectionsRenderAndTheStoredOrderDecidesWhichComesFirst` | Compose instrumentation; runs on a device, not in CI |
| renders the data-source education item after the sections | PORTED | HeartVitalsOverviewContentTest: `theDataSourceEducationItemComesAfterTheSections` | Compose instrumentation; runs on a device, not in CI |
| renders the three heart-section MetricLineCharts | PORTED | HeartVitalsOverviewContentTest: `theHeartSectionDrawsAChartUnderEachOfItsThreeCards` | Compose instrumentation; runs on a device, not in CI |
| lays the summary metrics out two per row | N/A-WIDGET | — | pure layout |
| includes the skin temperature card in the respiratory section | N/A-WIDGET | — | |
| changing the range selector reloads the period | PORTED | HeartViewModelTest.kt: `selectRange updates selectedRange` + `WEEK range calls loadDailyHeartRateSummaries` / `DAY range calls loadHeartRateSamples` | range-driven reload covered at VM level |
| tapping the skin temperature row opens its metric route | N/A-WIDGET | — | navigation wiring |
| reordering a section persists across a rebuild | DIVERGED | MetricDetailSectionOrderViewModelTest.kt: `moveSectionToTarget_reordersAndPersists` | persist verified; fresh-instance re-read not asserted |

## test/features/vitals/heart_vitals_overview_view_model_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/heart/HeartViewModelTest.kt (Kotlin has no separate vitals-overview VM; HeartViewModel serves the overview)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | HeartViewModelTest.kt: `load success populates display metric for average heart rate` | populated-state asserted; display values not |
| the display follows the range the load carried | PORTED | HeartViewModelTest.kt: `WEEK range does not call loadHeartRateSamples` + `DAY range calls loadHeartRateSamples` + `switching from DAY to WEEK clears HR samples` | |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | HeartViewModelTest.kt: `a permission failure on the overview becomes ScreenError PermissionDenied` | the refused vitals half fails the combined load and keeps the permission type |
| an unexpected failure carries its message to the screen | PORTED | HeartViewModelTest.kt: `load failure sets error and clears loading` | |
| either half failing fails the combined load | PORTED | HeartViewModelTest.kt: `either half failing fails the combined load` | no combined heart+vitals failure test |
| refresh reloads the current selection in force mode | PORTED | HeartViewModelTest.kt: `refresh reloads the current selection in force mode` |  |
| a stale load cannot overwrite the newer one it lost to | PORTED | HeartViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` |  |

## test/features/vitals/vitals_screens_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/heart/HeartViewModelTest.kt (delete path); screens untested

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Blood pressure screen renders the ordered sections once loaded | PORTED | VitalsContentTest: `bloodPressureRendersItsSectionsOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| Blood pressure screen shows the access gate when missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |
| Blood pressure screen shows placeholder with no readings | PORTED | VitalsContentTest: `bloodPressureShowsThePlaceholderWithNoReadings` | Compose instrumentation; runs on a device, not in CI |
| Swipe-deleting a manual entry calls the vitals repository | PORTED | HeartViewModelTest.kt: `deleteVitalsMeasurementEntry removes OpenVitals blood pressure and reloads` (+ `ignores blood pressure not created by OpenVitals`) | swipe gesture itself widget-only |
## test/features/achievements/achievements_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/achievements/AchievementsViewModelTest.kt (badge evaluation lives inside AchievementsViewModel; no separate display test)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty history unlocks nothing and reports no activity | PORTED | AchievementsViewModelTest.kt: `an empty history unlocks nothing and reports no activity` | no empty-history invariants test (unlockedCount 0, ratios, hasActivityHistory, per-badge zeros) |
| the stats aggregate the whole window | PORTED | AchievementsViewModelTest.kt: `the stats aggregate the whole window` | blank-day exclusion, totalDistance, maxDailyFloors, window dates all asserted |
| a badge is earned on the first day that reaches its target | PORTED | AchievementsViewModelTest.kt: `a badge is earned on the first day that reaches its target` | achievedOn/timesEarned/currentValue/completionRatio asserted |
| a locked badge carries its partial progress, clamped | PORTED | AchievementsViewModelTest.kt: `a locked badge carries its partial progress, clamped` | 0.5 ratio plus whole-catalog clamping |
| the category filter is precomputed per chip | PORTED | AchievementsViewModelTest.kt: `the category filter is precomputed per chip` | per-category filter untested (catalog category counts covered by `catalog includes legacy activity badge set`, not the filter) |
| a history with no floor data leaves the floor badges unearned | PORTED | AchievementsViewModelTest.kt: `a history with no floor data leaves the floor badges unearned` | no floorless-history case |

## test/features/achievements/achievements_screen_test.dart
Kotlin counterpart: none (AchievementsScreen.kt untested); VM covered by AchievementsViewModelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders summary + earned badge once loaded | PORTED | AchievementsContentTest: `theSummaryCardCountsUnlockedAgainstTotal`, `anEarnedBadgeNamesTheDayItWasEarned` | Compose instrumentation; runs on a device, not in CI |
| shows the no-activity message with empty history | PORTED | AchievementsEmptyHistoryTest: `anEmptyHistorySaysSoRatherThanLeavingEveryBadgeUnexplained` | Compose instrumentation; runs on a device, not in CI |

## test/features/homewidgets/home_widget_beverage_configure_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/homewidgets/HomeQuickBeverageWidgetDrinkOrderingTest.kt (ordering + picker labels), HomeQuickBeverageWidgetStateTest.kt (cached snapshot round trip), HomeQuickBeverageWidgetReceiverTest.kt (receiver resolution); the Activity's persistence path stays instrumentation-only

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| configure route > carries the widget type and the appWidgetId | N/A-FRAMEWORK | — | Flutter deep-link configure route; Kotlin uses a native config Activity receiving appWidgetId in the launch intent |
| configure route > an ordinary launch is not a configure launch | N/A-FRAMEWORK | — | |
| configure route > a route it cannot fully understand is refused | N/A-FRAMEWORK | — | |
| configure route > a status widget routes to the exact-alarm gate | N/A-FRAMEWORK | — | no exact-alarm gate exists in the Kotlin app |
| widget-type resolution > a beverage configure route opens the beverage picker | PORTED | HomeWidgetConfigurationActivityTest: `aBeverageWidgetOpensTheBeveragePicker` | Compose instrumentation; runs on a device, not in CI |
| widget-type resolution > the 1x1 opens the same beverage picker | PORTED | HomeWidgetConfigurationActivityTest: `theOneTapWidgetOpensTheSameBeveragePicker` | Compose instrumentation; runs on a device, not in CI |
| widget-type resolution > a metric configure route NEVER opens the beverage picker | PORTED | HomeWidgetConfigurationActivityTest: `aMetricWidgetNeverOpensTheBeveragePicker` | Compose instrumentation; runs on a device, not in CI |
| widget-type resolution > a beverage route NEVER opens the metric picker, either | PORTED | HomeWidgetConfigurationActivityTest: `aBeverageWidgetNeverOpensTheMetricPicker` | Compose instrumentation; runs on a device, not in CI |
| beverage picker > lists the drinks as "name - amount", in catalog order | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `the picker lists the drinks as name - amount, in catalog order` | label extracted to quickBeverageWidgetPickerLabels; amount formatting now asserted |
| beverage picker > an empty catalog says so rather than showing a blank list | N/A-WIDGET | — | empty-state rendering; ordering helper's empty-input behavior also untested |
| beverage picker > picking a drink persists the selection and the payload, pushes, finishes | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; none |
| beverage picker > the 1x1 pick pushes to the 1x1 receiver | PORTED | HomeQuickBeverageWidgetReceiverTest.kt: `a one-tap instance is redrawn through the one-tap receiver` | receiver resolution covered; the setDrink half is instrumentation-only |
| beverage picker > backing out without picking never finishes the configuration | PORTED | HomeWidgetConfigurationActivityTest: `backingOutOfTheBeveragePickerNeverFinishesTheConfiguration` | Compose instrumentation; runs on a device, not in CI |
| refresh > re-pushes a configured instance from its cached payload | PORTED | HomeQuickBeverageWidgetStateTest.kt: `re-pushes a configured instance from its cached payload` | cached-payload round trip covered; refreshHomeQuickBeverageWidget orchestration still untested |
| refresh > leaves an unconfigured instance on its native state | PORTED | HomeQuickBeverageWidgetStateTest.kt: `leaves an unconfigured instance on its native state` | null-snapshot path untested |

## test/features/homewidgets/home_widget_beverage_log_test.dart
Kotlin counterpart: none for the log path (HomeQuickBeverageLogAction + features/manualentry/hydration/HydrationDrinkLogger.kt `logCustomHydrationDrinkEntry` have no tests); reminder rules partially in features/hydration/reminders/HydrationReminderControllerTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| quickBeverageLogAppWidgetId > reads the appWidgetId off the background URI | N/A-FRAMEWORK | — | Flutter background-callback URI; Kotlin ActionCallback receives the GlanceId directly |
| quickBeverageLogAppWidgetId > ignores anything that is not a beverage-log broadcast | N/A-FRAMEWORK | — | |
| resolves Health Connect access before writing | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| logs the hydration AND the nutrition entry for a drink with caffeine | PORTED | HomeQuickBeverageLogTest.kt | the "no JVM seam" note was a category error — the Glance plumbing needs a device, the decision layer is plain Kotlin over two repository interfaces |
| re-anchors the hydration reminder once water is logged | PORTED | HydrationReminderControllerTest.kt (re-anchor-on-log rules); HomeQuickBeverageWidget.kt | was a real gap, not a testing one: the widget path hid the notification but left the alarm armed from the previous drink. It now calls applyConfig() like the in-app save and the notification quick-add both did |
| leaves the reminder alone for a nutrition-only drink | PORTED | HomeQuickBeverageLogTest.kt | a 0-multiplier drink writes no hydration; wroteHydration=false and effectiveLiters=0.0 are the flags the reminder gate reads |
| confirms with "Saved now", then falls back to "Tap to log" | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| a nutrition-only drink confirms with "Saved as nutrition" | PORTED | HomeQuickBeverageLogTest.kt | same outcome object selects the string |
| a missing permission writes nothing and is not auto-cleared | PORTED | HomeQuickBeverageLogTest.kt | MISSING_WRITE_PERMISSION, zero writes to either repository |
| a missing nutrition permission blocks the whole drink | PORTED | HomeQuickBeverageLogTest.kt | no hydration write either — the water is not logged with the caffeine lost |
| a failed write reports "Unable to update" and does not throw | PORTED | HomeQuickBeverageLogTest.kt | the write propagates (the contract the action's runCatching depends on) and the nutrition half is never attempted |
| an unconfigured instance is told to pick a beverage, and writes nothing | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| a stale payload naming another drink is refused | N/A-FRAMEWORK | — | payload staleness is bridge-specific; Kotlin resolves the drink by id from the repository |
| redraws the 2x1 receiver when the 2x1 owns the instance | PORTED | HomeQuickBeverageWidgetReceiverTest.kt: `redraws the 2x1 receiver when the 2x1 owns the instance` | receiver-class selection helper untested |
| an appWidgetId belonging to no beverage widget is ignored | DIVERGED | HomeQuickBeverageWidgetReceiverTest.kt: `an appWidgetId belonging to no placed widget falls back to the 2x1` | the hasAppWidgetInfo guard is covered; drinkIdFor's null early-return is instrumentation-only (SharedPreferences) |

## test/features/homewidgets/home_widget_beverage_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/homewidgets/HomeQuickBeverageWidgetDrinkOrderingTest.kt and HomeQuickBeverageAmountLabelTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| quickBeverageWidgetDrinkOptions > frequent drinks come first, in their own ranking | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `orders frequent then custom then catalog drinks by category and name` | frequent order (cola, still-water) preserved |
| quickBeverageWidgetDrinkOptions > a frequent drink that is no longer in the catalog is dropped | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `a frequent drink that is no longer in the catalog is dropped` | not exercised by the Kotlin ordering test |
| quickBeverageWidgetDrinkOptions > user drinks come before the preloaded catalog | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `user drinks come before the preloaded catalog` | Dart-faithful isolated case added alongside the combined one |
| quickBeverageWidgetDrinkOptions > sorts each group by category, then name, then id | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `sorts each group by category, then name, then id` | all seven category ranks plus the case-insensitive name compare |
| quickBeverageWidgetDrinkOptions > breaks a same-name tie on the id | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `breaks a same-name tie on the id` |  |
| quickBeverageWidgetDrinkOptions > an empty catalog yields no options | PORTED | HomeQuickBeverageWidgetDrinkOrderingTest.kt: `an empty catalog yields no options` |  |
| quickBeverageAmountLabel > metric sub-litre volumes read in millilitres, without a space | PORTED | HomeQuickBeverageAmountLabelTest.kt: `metric sub-litre volumes read in millilitres, without a space` | quickBeverageAmountLabel (HomeQuickBeverageWidget.kt:553) untested |
| quickBeverageAmountLabel > metric volumes of a litre and up read in litres | PORTED | HomeQuickBeverageAmountLabelTest.kt: `metric volumes of a litre and up read in litres` |  |
| quickBeverageAmountLabel > imperial always reads through the formatter | PORTED | HomeQuickBeverageAmountLabelTest.kt: `imperial always reads through the formatter` | UnitFormatterTest covers volume units generally, not this label helper |
| drink payload > round-trips the fields the log callback needs | N/A-FRAMEWORK | — | no payload in Kotlin; the action resolves the drink by id |
| drink payload > refuses a malformed, empty or unloggable payload | N/A-FRAMEWORK | — | Kotlin validates via isValidCustomHydrationDrink instead |
| drink payload > drops an unknown nutrient rather than the whole drink | N/A-FRAMEWORK | — | payload nutrient parsing is bridge-specific |

## test/features/homewidgets/home_widget_configure_test.dart
Kotlin counterpart: none (HomeMetricWidgetConfigurationActivity.kt and HomeMetricWidgetSelection untested)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the prompt and the metric catalog | PORTED | HomeWidgetConfigurationActivityTest: `theMetricPickerRendersThePromptAndTheMetricCatalog` | Compose instrumentation; runs on a device, not in CI |
| excludes what the catalog excludes | N/A-BEHAVIOR | none | blocked on behavior decision - Kotlin drops CARDIO_LOAD + CAFFEINE, Flutter drops caffeine + intensityMinutes |
| picking a metric persists its selection_id and pushes the tile | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| backing out without picking never finishes the configuration | PORTED | HomeWidgetConfigurationActivityTest: `backingOutOfTheMetricPickerNeverFinishesTheConfiguration` | Compose instrumentation; runs on a device, not in CI |
| a failed load still records the pick and finishes | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |

## test/features/homewidgets/home_widget_exact_alarm_gate_test.dart
Kotlin counterpart: none — the Kotlin app schedules no exact alarms for widgets (grep finds no ExactAlarm usage); refresh is receiver/system driven

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a granted permission finishes invisibly | N/A-FRAMEWORK | — | exact-alarm gate does not exist in Kotlin |
| "Not now" keeps the widget: RESULT_OK, never a cancel | N/A-FRAMEWORK | — | |
| "Allow" that grants re-arms the refresh chain and finishes | N/A-FRAMEWORK | — | |

## test/features/homewidgets/home_widget_launch_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/homewidgets/HomeWidgetLaunchRouteTest.kt (over the now-`internal` MainActivity.kt openVitalsRoute / migratedOpenVitalsRoute / isSupportedOpenVitalsRoute)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| homeWidgetRouteLocation > parses the native openvitals://widget?route=... form | N/A-FRAMEWORK | — | Kotlin passes the route as an intent extra; no URI form |
| homeWidgetRouteLocation > maps every allowed widget route to its go_router location | PORTED | HomeWidgetLaunchRouteTest.kt: `maps every allowed widget route to its destination` + `every route the snapshot builders write is on the allow-list` | Kotlin additionally allows manual_entry/activity, which Flutter rejects |
| homeWidgetRouteLocation > accepts a path-only uri (no authority) | N/A-FRAMEWORK | — | |
| homeWidgetRouteLocation > rejects a route that is not on the allow-list | PORTED | HomeWidgetLaunchRouteTest.kt: `rejects a route that is not on the allow-list` |  |
| homeWidgetRouteLocation > rejects a malformed argument | PORTED | HomeWidgetLaunchRouteTest.kt: `rejects a malformed argument` | fixed: the body_energy segment is now parsed strictly as an ISO date, so `yesterday` and `2026-13-45` are dropped like Flutter's |
| homeWidgetRouteLocation > rejects nothing to open | PORTED | HomeWidgetLaunchRouteTest.kt: `rejects nothing to open` | null, blank and unsupported extra all yield null |
| homeWidgetRouteLocationOf > maps the raw route string the snapshot carries | PORTED | HomeWidgetLaunchRouteTest.kt: `maps the raw route string the snapshot carries` | snapshot route strings vs allow-list consistency untested |
| homeWidgetRouteLocationOf > a readiness widget placed before the merge lands on Body Energy | PORTED | HomeWidgetLaunchRouteTest.kt: `a readiness widget placed before the merge lands on Body Energy` | migratedOpenVitalsRoute (LegacyDailyReadinessRoute → BodyEnergyDetails) exists verbatim, untested |

## test/features/homewidgets/home_widget_refresher_test.dart
Kotlin counterpart: none (refreshHomeMetricWidget / refreshDailyReadinessWidget / refreshBodyEnergyWidget / refreshTodayVitalsWidget and UpdatingHomeWidgetReceiver are untested)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| refresh resolves Health Connect access before loading | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| pushes all three shared widgets from one load | N/A-FRAMEWORK | — | Kotlin widgets refresh independently per receiver; no one-load fan-out |
| pushes one snapshot per placed metric instance, keyed by its id | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| leaves an unconfigured or unknown instance alone | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| a beverage instance is not touched (a later phase) | N/A-FRAMEWORK | — | phase ordering is a Flutter-refresher concern; Kotlin widgets are independent |
| loads today, forcing a fresh read | N/A-FRAMEWORK | none | instrumentation-only - the Glance action/receiver runs on AppWidgetManager + SharedPreferences with no JVM seam; — |
| refreshIfPlaced skips the dashboard load when no widget is placed | PORTED | HomeWidgetRefreshTriggerTest.kt | the note answered the wrong question — the invariant is that a refresh happens AT ALL after out-of-band data lands, and nothing provided it. refreshPlacedHomeWidgets now runs after watch sync, phone sync and import, and returns early when no widget is placed |
| refreshIfPlaced refreshes when a widget is placed | PORTED | HomeWidgetRefreshTriggerTest.kt (manifest-vs-list completeness) | the broadcast shape needs instrumentation (ComponentName/Intent are throwing stubs on the JVM); what is pinned is that no declared widget is left out of the list |
| a failed load never throws | PORTED | HomeStatusWidgetSnapshotTest.kt; HomeMetricWidget.kt / HomeReadinessWidgets.kt | Kotlin met the never-throws half and broke the second assertion: it WROTE the failure snapshot, which was byte-identical to an empty day, over real numbers. A failed or timed-out read now leaves the last good snapshot alone |
| a failing client cannot stop the other widgets updating | N/A-FRAMEWORK | — | isolation is structural (independent receivers) |
| push reuses the caller's data without loading | N/A-FRAMEWORK | — | no push bridge in Kotlin |

## test/features/homewidgets/home_widget_service_test.dart
Kotlin counterpart: mostly none — the SharedPreferences key bridge these tests pin does not exist in the Kotlin app (widgets keep per-instance Glance state and read repositories directly); the row cap is covered by HomeMetricWidgetSnapshotTest.kt

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| homeWidgetKeyPrefix > namespaces a shared widget by its storage key | N/A-FRAMEWORK | — | bridge-specific |
| homeWidgetKeyPrefix > namespaces a per-instance widget by appWidgetId too | N/A-FRAMEWORK | — | Glance state is per-instance natively |
| homeWidgetKeyPrefix > the two beverage widgets share one storage namespace | N/A-FRAMEWORK | — | |
| homeWidgetDataMap > a plot series rides as one comma-separated value | N/A-FRAMEWORK | — | comma-encoding is bridge-specific |
| homeWidgetDataMap > a widget with no plot writes an empty series, not a missing key | N/A-FRAMEWORK | — | |
| homeWidgetDataMap > maps the snapshot to the Kotlin key layout, under the prefix | N/A-FRAMEWORK | — | |
| homeWidgetDataMap > omits selection_id when not provided | N/A-FRAMEWORK | — | |
| homeWidgetDataMap > caps rows at maxHomeWidgetRows | PORTED | HomeMetricWidgetSnapshotTest.kt: `writing a snapshot caps the rows the widget can draw` | write path extracted to MutablePreferences.putHomeWidgetSnapshot |
| homeWidgetDataMap > two widgets pushed in turn do not clobber each other | N/A-FRAMEWORK | — | per-widget Glance state by construction |
| HomeWidgetService > persists every key then updates the qualified receiver | N/A-FRAMEWORK | — | |
| HomeWidgetService > a per-instance push is keyed by appWidgetId | N/A-FRAMEWORK | — | |
| HomeWidgetService > instancesOf returns only that widget's placed instances | N/A-FRAMEWORK | — | GlanceAppWidgetManager native |
| release receiver names > instancesOf matches them | N/A-FRAMEWORK | — | |
| release receiver names > widgetOfInstance resolves them | N/A-FRAMEWORK | — | |
| release receiver names > the fully-qualified debug form keeps working | N/A-FRAMEWORK | — | |

## test/features/homewidgets/home_widget_snapshots_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/homewidgets/HomeStatusWidgetSnapshotTest.kt and HomeMetricWidgetSnapshotTest.kt, over the pure builders extracted from HomeReadinessWidgets.kt (`buildDailyReadinessSnapshot` / `buildBodyEnergySnapshot` / `buildTodayVitalsSnapshot`) and HomeMetricWidget.kt (`buildMetricWidgetSnapshot` / `homeMetricWidgetRoute`)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildDailyReadinessSnapshot > reports the score, status and recommendation | PORTED | HomeStatusWidgetSnapshotTest.kt: `daily readiness reports the score, status and recommendation` |  |
| buildDailyReadinessSnapshot > falls back to "--" when no signal is loaded (UNKNOWN) | PORTED | HomeStatusWidgetSnapshotTest.kt: `daily readiness falls back to dashes when no signal is loaded` + `daily readiness falls back to dashes when nothing loaded at all` |  |
| buildDailyReadinessSnapshot > honours the caller's goals | PORTED | HomeStatusWidgetSnapshotTest.kt: `daily readiness honours the caller's goals` |  |
| buildBodyEnergySnapshot > reports the score with start/charged/drained rows | PORTED | HomeStatusWidgetSnapshotTest.kt: `body energy reports the score with start, charged and drained rows` |  |
| buildBodyEnergySnapshot > maps every status threshold | PORTED | HomeStatusWidgetSnapshotTest.kt: `body energy maps every status threshold` | all seven Dart boundaries (80/79/60/59/40/39/0) |
| buildBodyEnergySnapshot > carries the day as a series the widget can plot | N/A-WIDGET | none | HomeMetricWidgetSnapshot has no series field; no Kotlin widget plots a line |
| buildBodyEnergySnapshot > a full day is thinned, and still ends where the number says | N/A-WIDGET | none | no series to thin; maxHomeWidgetSeriesPoints has no Kotlin counterpart |
| buildBodyEnergySnapshot > falls back to "--" with no rows when the timeline is absent | PORTED | HomeStatusWidgetSnapshotTest.kt: `body energy falls back to dashes with no rows when the timeline is absent` |  |
| buildTodayVitalsSnapshot > lists the rows in the Kotlin order, values joined with their unit | PORTED | HomeStatusWidgetSnapshotTest.kt: `today lists the rows in order, values joined with their unit` | all nine row labels in order plus every Dart value assertion |
| buildTodayVitalsSnapshot > drops the readiness row and shows "No data" rows when empty | PORTED | HomeStatusWidgetSnapshotTest.kt: `today drops the readiness row and shows no-data rows when empty` |  |
| buildMetricSnapshot > formats a metric with its unit and routes to the metric screen | PORTED | HomeMetricWidgetSnapshotTest.kt: `formats a metric with its unit and routes to the metric screen` |  |
| buildMetricSnapshot > body energy routes to its dated detail screen, not /metric | PORTED | HomeMetricWidgetSnapshotTest.kt: `body energy routes to its dated detail screen, not the metric screen` | route, score and the three rows asserted; Kotlin's tile subtitle is the status word, not "+30 / -12" |
| buildMetricSnapshot > reports "--" / "No data" for an absent reading | PORTED | HomeMetricWidgetSnapshotTest.kt: `reports dashes and no data for an absent reading` |  |
| buildMetricSnapshot > a missing permission wins over the reading | PORTED | HomeMetricWidgetSnapshotTest.kt: `a missing permission wins over the reading` |  |
| buildMetricSnapshot > weekly cardio load reports progress and percent | N/A-FRAMEWORK | — | CARDIO_LOAD is excluded from the Kotlin widget catalog |
| buildMetricSnapshot > every catalog metric has a title, a route and a no-data snapshot | PORTED | HomeMetricWidgetSnapshotTest.kt: `every catalog metric has a title, a route and a no-data snapshot` | homeMetricTitleRes/route coverage sweep is a pure JVM test |
| homeMetricWidgetCatalog > drops caffeine and intensity minutes (the Kotlin catalog) | N/A-BEHAVIOR | none | blocked on behavior decision - the two catalogs disagree on membership |

## test/features/onboarding/onboarding_display_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/onboarding/OnboardingViewModelTest.kt (row derivation lives inside OnboardingViewModel)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a fresh install: nothing granted, everything outstanding | PORTED | OnboardingViewModelTest.kt: `a fresh install derives every row outstanding` | per-row counts, missingRequestableFor, opt-in-not-folded rule |
| a partially granted category reports the count, not just the flag | PORTED | OnboardingViewModelTest.kt: `partial rows report their counts` | |
| the required set being granted turns the button into Continue, even with the opt-in rows still outstanding | PORTED | OnboardingViewModelTest.kt: `next walks the applicable steps in order` (canAdvance with only activity+sleep) + `additional access step still applies for the routes walkthrough alone` (routesOutstanding) | |
| a manual-only category cannot be granted by the runtime dialog | PORTED | OnboardingViewModelTest.kt: `a manual-only permission cannot be granted by the runtime dialog` | Kotlin models the manual grant as catalog.routeReadPermission + routesOutstanding |
| an unsupported category is never "granted", whatever is in the set | PORTED | OnboardingViewModelTest.kt: `an unsupported category is never granted, whatever is in the set` | unsupported-but-granted row rule untested (step skipping is covered, the row flag is not) |
| an empty catalog derives an empty display | PORTED | OnboardingViewModelTest.kt: `an empty catalog derives an empty display` |  |

## test/features/onboarding/onboarding_screen_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/onboarding/OnboardingViewModelTest.kt (screen itself untested)

| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| step 1 lists the five Health Connect categories | PORTED | OnboardingViewModelTest.kt: `checkState loads catalog and granted permissions` | 5 rows, first ACTIVITY |
| Next is refused until Activity and Sleep are granted | PORTED | OnboardingViewModelTest.kt: `step one gates advancing on the required set` | |
| granting only Activity and Sleep is enough to move on | PORTED | OnboardingViewModelTest.kt: `next walks the applicable steps in order` | |
| a category row requests exactly its own permissions | PORTED | OnboardingViewModelTest.kt: `missingRequestableFor subtracts the granted set` | |
| the mindfulness step is skipped when the device lacks it | PORTED | OnboardingViewModelTest.kt: `steps that do not apply on this device are skipped` | |
| the mindfulness step appears where the device has it | PORTED | OnboardingViewModelTest.kt: `next walks the applicable steps in order` + `mindfulness step stays unsatisfied with the opt-in off` | |
| the forward button stops saying "Not now" once a step is done | DIVERGED | OnboardingViewModelTest.kt: `mindfulness step stays unsatisfied with the opt-in off` | currentStepSatisfied covered; label mapping is widget-side |
| back walks the steps, and only exits from the first | PORTED | OnboardingViewModelTest.kt: `back walks the steps, and only exits from the first` | isFirstStep + back() is a no-op on step one |
| the last step walks the user to exercise routes by hand | PORTED | OnboardingViewModelTest.kt: `additional access step still applies for the routes walkthrough alone` | |
| finishing persists the prefs and the permission-set version | DIVERGED | OnboardingViewModelTest.kt: `completeOnboarding stamps privacy policy and the done flag` | architecture divergence - Kotlin stamps lastPromptedPermissionSetVersion in healthconnect/HealthConnectScreenUxCoordinator.kt, not in OnboardingViewModel |
| shows the unavailable message when Health Connect is missing | PORTED | OnboardingViewModelTest.kt: `unavailable short-circuits without reading the catalog` | message rendering widget-side |
| needsProviderUpdate offers an install action | PORTED | OnboardingScreenTest: `anOutdatedHealthConnectExplainsItselfAndOffersTheInstall` | Compose instrumentation; runs on a device, not in CI |
| the header renders the wide logo and the language dropdown | PORTED | OnboardingScreenTest: `theHeaderIdentifiesTheAppAndLetsTheLanguageBeChangedBeforeAnythingElse` | Compose instrumentation; runs on a device, not in CI |
| picking a language persists the app-language preference | PORTED | OnboardingViewModelTest.kt: `selectAppLanguage persists and updates state` | |
| the catalog is Health Connect's categories, in wizard order | PORTED | OnboardingViewModelTest.kt: `the catalog is Health Connect's categories, in wizard order` | full 8-category order plus non-empty permission sets |
| the additional-access row counts only what its button can grant | PORTED | OnboardingViewModelTest.kt: `the additional-access row counts only what its button can grant` | route read excluded from the row total |
| the required set is Activity and Sleep, and nothing that cannot be granted | PORTED | OnboardingViewModelTest.kt: `a fresh install derives every row outstanding` | set EQUALITY plus route_read/mindfulness/cycle exclusion |

## Summary

Counts are per Flutter test case. The csv_row_converter_test.dart row is collapsed in its table but counted at its true size (43 cases, all PORTED).

| Status | Count |
|---|---|
| PORTED | 502 |
| DIVERGED | 70 |
| MISSING | 27 |
| N/A-WIDGET | 134 |
| N/A-FRAMEWORK | 132 |
| N/A-BEHAVIOR | 7 |
| Total cases | 872 |

### Portable gaps

Remaining MISSING cases. Every one is blocked on a **feature that was never ported**
(Body Energy diagnostics) or a **seam owned by another workstream** (features/readiness).
Rows that were blocked on a *behavior decision* or on an **instrumentation-only** Android
surface are no longer listed here — they now carry N/A-BEHAVIOR / N/A-FRAMEWORK with the
reason in their Note column.

- test/features/settings/body_energy_diagnostics_card_test.dart: warns when more than one app wrote active calories
- test/features/settings/body_energy_diagnostics_card_test.dart: says so when no watch samples are stored
- test/features/settings/body_energy_diagnostics_test.dart: the component decomposition > activity uses the per-point max, never the sum
- test/features/settings/body_energy_diagnostics_test.dart: the component decomposition > a tie counts as the calorie estimate winning
- test/features/settings/body_energy_diagnostics_test.dart: the component decomposition > buckets with no activity drain count for neither side
- test/features/settings/body_energy_diagnostics_test.dart: clipping > a floored day reports its buckets and when it first pinned
- test/features/settings/body_energy_diagnostics_test.dart: clipping > a ledger that does not balance is flagged
- test/features/settings/body_energy_diagnostics_test.dart: the watch totals > are delta sums, not start minus end
- test/features/settings/body_energy_diagnostics_test.dart: the watch totals > are absent when the watch never synced that day
- test/features/settings/body_energy_diagnostics_test.dart: the watch totals > drainError is signed so an over-draining model is visible
- test/features/settings/body_energy_diagnostics_test.dart: per-influence errors > are signed, count-weighted, and omit unobserved influences
- test/features/settings/body_energy_diagnostics_test.dart: source attribution > flags a day two apps both wrote active calories for
- test/features/settings/body_energy_diagnostics_test.dart: source attribution > stays quiet when one app wrote them
- test/features/settings/body_energy_diagnostics_test.dart: source attribution > does not confuse two metrics for two calorie sources
- test/features/settings/body_energy_diagnostics_test.dart: toReportText > is stable, explicit, and carries the decisive figures
- test/features/settings/body_energy_diagnostics_test.dart: toReportText > names missing permissions rather than showing an empty report
- test/features/settings/body_energy_diagnostics_test.dart: toReportText > says so when the per-source read was truncated
- test/features/readiness/readiness_display_test.dart: daily readiness panel > composes every line the panel used to build inline
- test/features/readiness/readiness_display_test.dart: daily readiness panel > an empty insight still composes: no strain, no score, no factors
- test/features/readiness/readiness_display_test.dart: daily readiness panel > the factor list is capped at five
- test/features/readiness/readiness_display_test.dart: training readiness detail > score, verdict, confidence, signals and guidance
- test/features/readiness/readiness_display_test.dart: training readiness detail > no training-side factors falls back to the no-signals message
- test/features/readiness/readiness_display_test.dart: training readiness detail > an unknown state reads as needs-more-data, whatever the score
- test/features/readiness/readiness_display_test.dart: training readiness detail > the verdict bands
- test/features/readiness/readiness_display_test.dart: training readiness detail > the strain bullet always renders, from the state
- test/features/readiness/training_readiness_details_view_model_test.dart: a failed load becomes a ScreenError, not an exception
