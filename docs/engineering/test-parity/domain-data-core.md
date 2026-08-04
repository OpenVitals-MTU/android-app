# Flutter -> Kotlin test parity: domain/data/core/contract/state/navigation/di/bootstrap/l10n/widget_test

## test/domain/insights/activity_splits_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/ActivitySplitsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an activity that does not travel has no splits > a strength session with GPS drift is not cut into "laps" | PORTED | ActivitySplitsTest.kt: `a strength session with GPS drift is not cut into laps` | — |
| an activity that does not travel has no splits > ...and neither is one carrying a route it never meant to record | PORTED | ActivitySplitsTest.kt: `neither is a strength session carrying a route it never meant to record` | — |
| an activity that does not travel has no splits > a run with the same distance IS cut, so the gate is on the KIND | PORTED | ActivitySplitsTest.kt: `a run with the same distance IS cut, so the gate is on the KIND` | — |
| the equator-line fixture > really does put the requested number of meters between fixes | PORTED | ActivitySplitsTest.kt: `the fixture really does put the requested number of meters between fixes` | — |
| source priority > device laps win over a route, and are NOT re-cut to the split distance | PORTED | ActivitySplitsTest.kt: `device laps win over a route, and are NOT re-cut to the split distance` | — |
| source priority > uneven device laps keep their own lengths | PORTED | ActivitySplitsTest.kt: `uneven device laps keep their own lengths` | — |
| source priority > a route beats speed samples | PORTED | ActivitySplitsTest.kt: `a route beats speed samples` | — |
| route splits > cut at exactly the right distance, with the crossing time INTERPOLATED between fixes | PORTED | ActivitySplitsTest.kt: `cut at exactly the right distance, with the crossing time INTERPOLATED between fixes` | — |
| route splits > several boundaries inside one long segment are each interpolated | PORTED | ActivitySplitsTest.kt: `several boundaries inside one long segment are each interpolated` | — |
| route splits > a custom split distance (5 km, the cyclist case) is honoured | PORTED | ActivitySplitsTest.kt: `a custom split distance (5 km, the cyclist case) is honoured` | — |
| route splits > a trailing partial split is kept and flagged | PORTED | ActivitySplitsTest.kt: `a trailing partial split is kept and flagged` | — |
| route splits > elevation gain and loss come from the route altitudes | PORTED | ActivitySplitsTest.kt: `elevation gain and loss come from the route altitudes` | — |
| route splits > altitude at a mid-segment boundary is interpolated, and no interior fix is lost from the next split | PORTED | ActivitySplitsTest.kt: `altitude at a mid-segment boundary is interpolated, and no interior fix is lost from the next split` | — |
| route splits > a route without altitudes reports null elevation, not zero | PORTED | ActivitySplitsTest.kt: `a route without altitudes reports null elevation, not zero` | — |
| speed-sample splits (the treadmill case) > integrate v.dt to cut at the right times, with no route at all | PORTED | ActivitySplitsTest.kt: `integrate v dt to cut at the right times, with no route at all` | — |
| speed-sample splits (the treadmill case) > a changing belt speed integrates trapezoidally | PORTED | ActivitySplitsTest.kt: `a changing belt speed integrates trapezoidally` | — |
| the estimated fallback > every split shares the activity average pace, and the source says so | PORTED | ActivitySplitsTest.kt: `every estimated split shares the activity average pace, and the source says so` | — |
| the estimated fallback > an odd total distance still yields a flagged trailing partial | PORTED | ActivitySplitsTest.kt: `an odd total distance still yields a flagged trailing partial` | — |
| the estimated fallback > a single speed sample cannot be integrated, so it falls back to estimated | PORTED | ActivitySplitsTest.kt: `a single speed sample cannot be integrated, so it falls back to estimated` | — |
| average heart rate > covers only the samples inside the split window | PORTED | ActivitySplitsTest.kt: `average heart rate covers only the samples inside the split window` | — |
| average heart rate > the split window is half-open: a sample exactly on the boundary belongs to the NEXT split, never to both | PORTED | ActivitySplitsTest.kt: `the split window is half-open - a sample exactly on the boundary belongs to the NEXT split, never to both` | — |
| average heart rate > is null, not zero, when no sample falls inside the split | PORTED | ActivitySplitsTest.kt: `average heart rate is null, not zero, when no sample falls inside the split` | — |
| average heart rate > unsorted heart-rate samples are still bucketed correctly | PORTED | ActivitySplitsTest.kt: `unsorted heart-rate samples are still bucketed correctly` | — |
| paceDeltaSeconds > is negative for a faster split and positive for a slower one | PORTED | ActivitySplitsTest.kt: `pace delta is negative for a faster split and positive for a slower one` | — |
| paceDeltaSeconds > measures against the ACTIVITY average, not the mean of the split paces, so a short partial cannot skew the baseline | PORTED | ActivitySplitsTest.kt: `pace delta measures against the ACTIVITY average, not the mean of the split paces` | — |
| no distance > a strength session (no distance, no route, no speed) has no splits | PORTED | ActivitySplitsTest.kt: `a session with no distance, no route and no speed has no splits` | — |
| no distance > a zero total distance has no splits | PORTED | ActivitySplitsTest.kt: `a zero total distance has no splits` | — |
| no distance > heart-rate samples alone do not conjure splits | PORTED | ActivitySplitsTest.kt: `heart-rate samples alone do not conjure splits` | — |
| degenerate input does not crash or divide by zero > a single route point | PORTED | ActivitySplitsTest.kt: `a single route point` | — |
| degenerate input does not crash or divide by zero > duplicated route points (zero-length segments) | PORTED | ActivitySplitsTest.kt: `duplicated route points (zero-length segments)` | — |
| degenerate input does not crash or divide by zero > zero duration | PORTED | ActivitySplitsTest.kt: `zero duration` | — |
| degenerate input does not crash or divide by zero > a route whose fixes all share one timestamp | PORTED | ActivitySplitsTest.kt: `a route whose fixes all share one timestamp` | — |
| degenerate input does not crash or divide by zero > unsorted route points and speed samples are sorted first | PORTED | ActivitySplitsTest.kt: `unsorted route points and speed samples are sorted first` | — |
| degenerate input does not crash or divide by zero > a zero or negative split distance falls back to 1 km rather than looping forever | PORTED | ActivitySplitsTest.kt: `a zero or negative split distance falls back to 1 km rather than looping forever` | — |
| degenerate input does not crash or divide by zero > an absurdly small split distance is capped instead of building a million rows | PORTED | ActivitySplitsTest.kt: `an absurdly small split distance is capped instead of building a million rows` | — |
| degenerate input does not crash or divide by zero > a lap that ends before it starts is discarded | PORTED | ActivitySplitsTest.kt: `a lap that ends before it starts is discarded` | — |
| degenerate input does not crash or divide by zero > a lap with no recorded length borrows the route distance | PORTED | ActivitySplitsTest.kt: `a lap with no recorded length borrows the route distance` | — |
| paused recordings > a pause inside a split does not count as time spent covering it | PORTED | ActivitySplitsTest.kt: `a pause inside a split does not count as time spent covering it` | — |
| paused recordings > a pause outside a split leaves it alone | PORTED | ActivitySplitsTest.kt: `a pause outside a split leaves it alone` | — |
| paused recordings > a split cannot come out negative however the pauses overlap | PORTED | ActivitySplitsTest.kt: `a split cannot come out negative however the pauses overlap` | — |

## test/domain/insights/body_energy_calibration_fit_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/BodyEnergyCalibrationFitTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no readings leaves the gains at their defaults | PORTED | BodyEnergyCalibrationFitTest.kt: `no readings leaves the gains at their defaults` | — |
| reading lower than predicted after activity raises the activity gain | PORTED | BodyEnergyCalibrationFitTest.kt: `reading lower than predicted after activity raises the activity gain` | — |
| reading higher than predicted after sleep raises the sleep gain | PORTED | BodyEnergyCalibrationFitTest.kt: `reading higher than predicted after sleep raises the sleep gain` | — |
| gains never escape the bounded range | PORTED | BodyEnergyCalibrationFitTest.kt: `gains never escape the bounded range` | — |
| an observation moves the gain that scales the drain it blames > recovery debt moves the activity gain, not basal | PORTED | BodyEnergyCalibrationFitTest.kt: `recovery debt moves the activity gain, not basal` | — |
| an observation moves the gain that scales the drain it blames > steady still moves basal — the one influence it answers for | PORTED | BodyEnergyCalibrationFitTest.kt: `steady still moves basal, the one influence it answers for` | — |
| an observation moves the gain that scales the drain it blames > elevated heart rate moves the stress gain alone | PORTED | BodyEnergyCalibrationFitTest.kt: `elevated heart rate moves the stress gain alone` | — |
| an observation moves the gain that scales the drain it blames > quiet rest moves the sleep gain, which scales the rest charge | PORTED | BodyEnergyCalibrationFitTest.kt: `quiet rest moves the sleep gain, which scales the rest charge` | — |

## test/domain/insights/body_energy_timeline_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/BodyEnergyTimelineTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| manual zones classify sustained exercise as high confidence drain | PORTED | BodyEnergyTimelineTest.kt: `manual zones classify sustained exercise as high confidence drain` | — |
| long continuous activity adds fatigue beyond simple duration | PORTED | BodyEnergyTimelineTest.kt: `long continuous activity adds fatigue beyond simple duration` | — |
| sleep charges body energy from the previous score | PORTED | BodyEnergyTimelineTest.kt: `sleep charges body energy from the previous score` | — |
| awake elevated heart rate suppresses charging and adds stress drain | PORTED | BodyEnergyTimelineTest.kt: `awake elevated heart rate suppresses charging and adds stress drain` | — |
| recovery debt drain is reported after harder effort | PORTED | BodyEnergyTimelineTest.kt: `recovery debt drain is reported after harder effort` | — |
| an idle waking day declines rather than staying flat | PORTED | BodyEnergyTimelineTest.kt: `an idle waking day declines rather than staying flat` | — |
| a data gap after the day has shown life keeps draining basal | PORTED | BodyEnergyTimelineTest.kt: `a data gap after the day has shown life keeps draining basal` | — |
| a gap before the first signal of the day stays frozen | PORTED | BodyEnergyTimelineTest.kt: `a gap before the first signal of the day stays frozen` | — |
| steps without active calories still drain through a gap | PORTED | BodyEnergyTimelineTest.kt: `steps without active calories still drain through a gap` | — |
| a low-heart-rate high-step day out-drains a sedentary day | PORTED | BodyEnergyTimelineTest.kt: `a low-heart-rate high-step day out-drains a sedentary day` | — |
| a run out-drains a walk of the same duration | PORTED | BodyEnergyTimelineTest.kt: `a run out-drains a walk of the same duration` | — |
| a higher activity-drain gain drains more | PORTED | BodyEnergyTimelineTest.kt: `a higher activity-drain gain drains more` | — |
| the carry-over seed > a carried score below the floor is raised, and says so | PORTED | BodyEnergyTimelineTest.kt: `a carried score below the floor is raised, and says so` | — |
| the carry-over seed > a carried score above the floor passes through untouched | PORTED | BodyEnergyTimelineTest.kt: `a carried score above the floor passes through untouched` | — |
| the carry-over seed > no previous day starts neutral, and the floor does not apply | PORTED | BodyEnergyTimelineTest.kt: `no previous day starts neutral, and the floor does not apply` | — |
| the carry-over seed > a day with no usable data carries the seed instead of resetting | PORTED | BodyEnergyTimelineTest.kt: `a day with no usable data carries the seed instead of resetting` | Kotlin adds an extra reason-code assertion |
| the carry-over seed > a data-less day with a sub-floor seed still floors it | PORTED | BodyEnergyTimelineTest.kt: `a data-less day with a sub-floor seed still floors it` | — |
| the day totals reconcile > an ordinary day adds up | PORTED | BodyEnergyTimelineTest.kt: `an ordinary day adds up` | — |
| the day totals reconcile > a day that bottoms out reports the fall, not the model | PORTED | BodyEnergyTimelineTest.kt: `a day that bottoms out reports the fall, not the model` | — |
| the day totals reconcile > a day that tops out reports the rise, not the model | PORTED | BodyEnergyTimelineTest.kt: `a day that tops out reports the rise, not the model` | — |
| the day totals reconcile > the breakdown sums to the headline on a clamped day | PORTED | BodyEnergyTimelineTest.kt: `the breakdown sums to the headline on a clamped day` | — |
| the day totals reconcile > a bucket that both charges and drains still feeds both totals | PORTED | BodyEnergyTimelineTest.kt: `a bucket that both charges and drains still feeds both totals` | — |
| the day totals reconcile > a fully clamped bucket keeps a truthful driver at zero magnitude | PORTED | BodyEnergyTimelineTest.kt: `a fully clamped bucket keeps a truthful driver at zero magnitude` | — |
| the recovery-debt drain is correctable > it scales with the activity gain | PORTED | BodyEnergyTimelineTest.kt: `recovery debt scales with the activity gain` | — |
| the recovery-debt drain is correctable > and does not drag the basal drain with it | PORTED | BodyEnergyTimelineTest.kt: `recovery debt does not drag the basal drain with it` | — |
| no bucket is ever labelled quiet rest | PORTED | BodyEnergyTimelineTest.kt: `a sleep-then-workout day never labels a bucket quiet rest` | — |
| the waking-rest charge > a quiet waking day now recovers instead of only declining | PORTED | BodyEnergyTimelineTest.kt: `a quiet waking day now recovers instead of only declining` | — |
| the waking-rest charge > the ceiling is a share of reserve, not a fixed offset | PORTED | BodyEnergyTimelineTest.kt: `the rest ceiling is a share of reserve, not a fixed offset` | — |
| the waking-rest charge > it does not fire once the heart rate leaves the resting band | PORTED | BodyEnergyTimelineTest.kt: `the rest charge does not fire once the heart rate leaves the resting band` | — |
| the waking-rest charge > a trickle of activity drain does not block it | PORTED | BodyEnergyTimelineTest.kt: `a trickle of activity drain does not block the rest charge` | — |
| the waking-rest charge > it is suppressed while recovery debt is still being billed | PORTED | BodyEnergyTimelineTest.kt: `the rest charge is suppressed while recovery debt is still being billed` | — |
| the waking-rest charge > a charging waking bucket reports quiet rest | PORTED | BodyEnergyTimelineTest.kt: `a charging waking bucket reports quiet rest` | — |
| the waking-rest charge > but a larger drain still outranks it | PORTED | BodyEnergyTimelineTest.kt: `but a larger drain still outranks quiet rest` | — |
| the age-derived max heart rate uses Tanaka, like the rest of the app | PORTED | BodyEnergyTimelineTest.kt: `the age-derived max heart rate uses Tanaka, like the rest of the app` | — |

## test/domain/insights/body_energy_watch_calibration_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/BodyEnergyWatchCalibrationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildWatchObservations > downsamples to one reading per bucket | PORTED | BodyEnergyWatchCalibrationTest.kt: `buildWatchObservations downsamples to one reading per bucket` | — |
| buildWatchObservations > pairs each reading with the model score at that moment | PORTED | BodyEnergyWatchCalibrationTest.kt: `buildWatchObservations pairs each reading with the model score at that moment` | — |
| buildWatchObservations > drops readings with no nearby point | PORTED | BodyEnergyWatchCalibrationTest.kt: `buildWatchObservations drops readings with no nearby point` | — |
| buildWatchObservations > skips points the model could not measure | PORTED | BodyEnergyWatchCalibrationTest.kt: `buildWatchObservations skips points the model could not measure` | — |
| buildWatchObservations > no samples, or no timeline, yields nothing | PORTED | BodyEnergyWatchCalibrationTest.kt: `no samples, or no timeline, yields nothing` | — |
| the influence comes from the timeline, not a re-derivation | PORTED | BodyEnergyWatchCalibrationTest.kt: `the influence comes from the timeline, not a re-derivation` | — |
| fitBodyEnergyGains with watch readings > no readings leaves the gains untouched | PORTED | BodyEnergyWatchCalibrationTest.kt: `no readings leaves the gains untouched` | — |
| fitBodyEnergyGains with watch readings > a watch reading below prediction raises the drain gain | PORTED | BodyEnergyWatchCalibrationTest.kt: `a watch reading below prediction raises the drain gain` | — |
| fitBodyEnergyGains with watch readings > one reading barely moves a gain | PORTED | BodyEnergyWatchCalibrationTest.kt: `one reading barely moves a gain` | — |
| fitBodyEnergyGains with watch readings > watch readings are counted | PORTED | BodyEnergyWatchCalibrationTest.kt: `watch readings are counted` | — |
| fitBodyEnergyGains with watch readings > a realistic day of disagreement converges without saturating | PORTED | BodyEnergyWatchCalibrationTest.kt: `a realistic day of disagreement converges without saturating` | — |
| fitBodyEnergyGains with watch readings > a day of MAXIMAL disagreement does reach the clamp | PORTED | BodyEnergyWatchCalibrationTest.kt: `a day of MAXIMAL disagreement does reach the clamp` | — |
| fitBodyEnergyGains with watch readings > gains stay within their bounds however extreme the disagreement | PORTED | BodyEnergyWatchCalibrationTest.kt: `gains stay within their bounds however extreme the disagreement` | — |

## test/domain/insights/caffeine_drink_profile_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/CaffeineDrinkProfileTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the peak is LESS than the dose, and comes after the drink | PORTED | CaffeineDrinkProfileTest.kt: `peak is below the dose and is the highest point of the curve` | Kotlin additionally pins the peak to the curve maximum |
| it fades: half gone, then gone | PORTED | CaffeineDrinkProfileTest.kt: `half gone and gone are read after the peak` | — |
| the thresholds are read AFTER the peak, not on the way up | PORTED | CaffeineDrinkProfileTest.kt: `half gone and gone are read after the peak` | Kotlin uses non-strict `!isBefore(peak)` vs Flutter strict `isAfter` — same bug caught either way |
| right now is zero before the drink was drunk | DIVERGED | CaffeineDrinkProfileTest.kt: `isActive follows the negligible threshold` | Kotlin asserts only `isActive == false` before intake; `currentMg == 0` not asserted |
| a drink still in you is active; a day-old one is not | PORTED | CaffeineDrinkProfileTest.kt: `isActive follows the negligible threshold` | Kotlin checks inactivity at the 36h horizon instead of 30h |
| a bigger drink peaks higher and lasts longer | DIVERGED | CaffeineDrinkProfileTest.kt: `shared peak is the largest of the profiles` | Peak ordering only implied via shared-peak max; "lasts longer" (goneTime ordering) unasserted |
| a drink of nothing does nothing | PORTED | CaffeineDrinkProfileTest.kt: `a zero dose has no half to be gone and is over before it started` | Kotlin asserts peak 0 / halfGone null plus goneTime==start; currentMg==0 implied |
| the drink profile agrees with the calculator it is built on | PORTED | CaffeineDrinkProfileTest.kt: `the profile agrees with the model the day curve is built from` | — |

## test/domain/insights/caffeine_insight_calculator_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/CaffeineInsightCalculatorTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| contribution is zero before intake and positive after absorption | PORTED | CaffeineInsightCalculatorTest.kt: `contribution is zero before intake and positive after absorption` | — |
| active caffeine decays over time | PORTED | CaffeineInsightCalculatorTest.kt: `active caffeine decays over time` | — |
| build returns bedtime safety source and time bucket insights | DIVERGED | CaffeineInsightCalculatorTest.kt: `build returns bedtime safety source and time bucket insights` | Kotlin omits the `dailyStats.size == 3` assertion |
| a midnight bedtime minutes away projects tonight, not last night | DIVERGED | CaffeineInsightCalculatorTest.kt: `a bedtime before noon belongs to the night that ENDS the day` | Same anchoring rule pinned via dailyStats only; live `insights.bedtimeMg`/`currentMg` projection never asserted |
| the morning after, a midnight bedtime projects the coming night | DIVERGED | CaffeineInsightCalculatorTest.kt: `a bedtime before noon belongs to the night that ENDS the day` | Same anchoring rule pinned via dailyStats only; forward-looking `bedtimeMg` near-zero check unported |
| a day's safe-for-sleep stat uses the midnight that ENDS the day | DIVERGED | CaffeineInsightCalculatorTest.kt: `a bedtime before noon belongs to the night that ENDS the day` + `an evening bedtime stays on its own date` | Kotlin pins the anchor arithmetic exactly but does not assert `safeForSleep == false` for a lived unsafe night |
| an un-lived night is neither safe nor unsafe until it happens | PORTED | CaffeineInsightCalculatorTest.kt: `nights that have not been lived yet are not counted` + `an unlived night neither breaks nor extends the safe sleep streak` | Coverage split across two Kotlin tests |
| caffeine health catalog matches health connect names without local entries | PORTED | CaffeineInsightCalculatorTest.kt: `caffeine health catalog matches health connect names without local entries` | — |

## test/domain/insights/cross_metric_insights_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/CrossMetricInsightsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| calculatesPositiveCorrelationForPairedDays | PORTED | CrossMetricInsightsTest.kt: `calculatesPositiveCorrelationForPairedDays` | — |
| calculatesNegativeCorrelationForPairedDays | PORTED | CrossMetricInsightsTest.kt: `calculatesNegativeCorrelationForPairedDays` | — |
| ignoresUnpairedAndEmptyValues | PORTED | CrossMetricInsightsTest.kt: `ignoresUnpairedAndEmptyValues` | — |
| returnsNullWhenThereAreNotEnoughPairs | PORTED | CrossMetricInsightsTest.kt: `returnsNullWhenThereAreNotEnoughPairs` | — |

## test/domain/insights/daily_goals_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/DailyGoalsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| at least goals count tracked days and streaks | PORTED | DailyGoalsTest.kt: `at least goals count tracked days and streaks` | |
| at most goals ignore missing days and count only logged values | PORTED | DailyGoalsTest.kt: `at most goals ignore missing days and count only logged values` | — |
| an unmet today is skipped by the current streak, not a break | PORTED | DailyGoalsTest.kt: `an unmet today is skipped by the current streak, not a break` | fixed: `currentStreakDays(today)` skips an unmet day that has not finished yet; only a PAST unmet day breaks the run |
| a met today still counts toward the current streak | PORTED | DailyGoalsTest.kt: `a met today still counts toward the current streak` | — |
| values on the same day are summed before goal evaluation | PORTED | DailyGoalsTest.kt: `values on the same day are summed before goal evaluation` | — |

## test/domain/insights/daily_readiness_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/DailyReadinessTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| readyWhenSleepAndRecoverySignalsAreStrong | PORTED | DailyReadinessTest.kt: `readyWhenSleepAndRecoverySignalsAreStrong` | — |
| arrivingAtTheDayDrainedHoldsTrainingBack | DIVERGED | DailyReadinessTest.kt: `drainedBodyEnergyHoldsTrainingBack` | Kotlin pins exact score/trainingReadiness deltas and factor detail, but does not assert the verdict state flips away from READY |
| theMeasuredBatteryReplacesTheEstimate | PORTED | DailyReadinessTest.kt: `measuredBodyEnergyReplacesTheEstimate` | — |
| aLowStartStillCountsAfterAMidDayRecovery | PORTED | DailyReadinessTest.kt: `aDrainedStartCountsEvenWhenTheDayHasRecovered` | — |
| aChargedBatteryLiftsTheVerdict | PORTED | DailyReadinessTest.kt: `chargedBodyEnergyIsAPositiveFactor` | Kotlin asserts an exact +6 score delta instead of a greater-than |
| recoveryDayWhenSleepHrvAndRestingHeartRateArePoor | PORTED | DailyReadinessTest.kt: `recoveryDayWhenSleepHrvAndRestingHeartRateArePoor` | — |
| hrvStatusUsesPersonalBaselineThresholds | PORTED | DailyReadinessTest.kt: `hrvStatusUsesPersonalBaselineThresholds` | — |
| checkSymptomsWhenTemperatureSignalIsUnusual | PORTED | DailyReadinessTest.kt: `checkSymptomsWhenTemperatureSignalIsUnusual` | — |
| intensityMinutesReadinessUsesWeeklyPace | PORTED | DailyReadinessTest.kt: `intensityMinutesReadinessUsesWeeklyPace` | — |
| unknownWhenNoSignalsAreAvailable | PORTED | DailyReadinessTest.kt: `unknownWhenNoSignalsAreAvailable` | — |
| explanationJoinsFactorDetailsWithoutDoublePunctuation | PORTED | DailyReadinessTest.kt: `explanationJoinsFactorDetailsWithoutDoublePunctuation` | — |
| nutritionFactorNotShownWhenOnlyHydrationIsLogged | PORTED | DailyReadinessTest.kt: `nutritionFactorNotShownWhenOnlyHydrationIsLogged` | — |
| nutritionFactorShownWhenMealDataIsPresent | PORTED | DailyReadinessTest.kt: `nutritionFactorShownWhenMealDataIsPresent` | — |
| lowConfidenceWhenBaselinesAreMissing | PORTED | DailyReadinessTest.kt: `lowConfidenceWhenBaselinesAreMissing` | — |

## test/domain/insights/data_confidence_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/DataConfidenceTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| confidence calculates coverage inside the selected period | PORTED | DataConfidenceTest.kt: `confidence calculates coverage inside the selected period` | — |
| mixed sources are reported as medium confidence | PORTED | DataConfidenceTest.kt: `mixed sources are reported as medium confidence` | — |
| calculated values include a calculated warning | PORTED | DataConfidenceTest.kt: `calculated values include a calculated warning` | — |
| empty samples are low confidence | PORTED | DataConfidenceTest.kt: `empty samples are low confidence` | — |
| manual entries are reported | PORTED | DataConfidenceTest.kt: `manual entries are reported` | — |

## test/domain/insights/heart_rate_recovery_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/HeartRateRecoveryTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| calculateHeartRateRecovery > the offsets are 30s..5min, no 10s mark | PORTED | HeartRateRecoveryTest.kt: `the offsets are 30s to 5min, no 10s mark` | — |
| calculateHeartRateRecovery > a chest strap at 1Hz measures every mark and reads clean | PORTED | HeartRateRecoveryTest.kt: `a chest strap at 1Hz measures every mark and reads clean` | — |
| calculateHeartRateRecovery > a watch that samples once a minute after the workout leaves the 30s mark BLANK rather than interpolated | PORTED | HeartRateRecoveryTest.kt: `a watch that samples once a minute after the workout leaves the 30s mark BLANK rather than interpolated` | — |
| calculateHeartRateRecovery > a watch every 5 seconds keeps all six marks | PORTED | HeartRateRecoveryTest.kt: `a watch every 5 seconds keeps all six marks` | — |
| calculateHeartRateRecovery > a watch that stops recording at the workout end measures nothing | PORTED | HeartRateRecoveryTest.kt: `a watch that stops recording at the workout end measures nothing` | — |
| calculateHeartRateRecovery > no samples at all is noData, not a crash | PORTED | HeartRateRecoveryTest.kt: `no samples at all is noData, not a crash` | — |
| calculateHeartRateRecovery > nothing in the hard last-10s window means no peak, and noData | PORTED | HeartRateRecoveryTest.kt: `nothing in the hard last-10s window means no peak, and noData` | — |
| calculateHeartRateRecovery > easing off before pressing stop is caught, not rewarded | PORTED | HeartRateRecoveryTest.kt: `easing off before pressing stop is caught, not rewarded` | — |
| calculateHeartRateRecovery > a fall of just five bpm before the stop still counts as a cool-down | PORTED | HeartRateRecoveryTest.kt: `a fall of just five bpm before the stop still counts as a cool-down` | — |
| calculateHeartRateRecovery > a heart rate that ROSE after the stop is not a recovery | PORTED | HeartRateRecoveryTest.kt: `a heart rate that ROSE after the stop is not a recovery` | — |
| calculateHeartRateRecovery > a reading with no one-minute mark cannot be charted | PORTED | HeartRateRecoveryTest.kt: `a reading with no one-minute mark cannot be charted` | — |
| calculateHeartRateRecovery > a submaximal effort is shown, flagged not-comparable, never hidden | PORTED | HeartRateRecoveryTest.kt: `a submaximal effort is shown, flagged not-comparable, never hidden` | — |
| calculateHeartRateRecovery > near-max is an absolute band, wider for an ESTIMATED max | PORTED | HeartRateRecoveryTest.kt: `near-max is an absolute band, wider for an ESTIMATED max` | — |
| calculateHeartRateRecovery > the same peak against a KNOWN max is submaximal (tighter band) | PORTED | HeartRateRecoveryTest.kt: `the same peak against a KNOWN max is submaximal (tighter band)` | — |
| calculateHeartRateRecovery > an unknown max heart rate still reports every mark | PORTED | HeartRateRecoveryTest.kt: `an unknown max heart rate still reports every mark` | — |
| calculateHeartRateRecovery > the age formula is Tanaka (208 - 0.7*age), flagged estimated | PORTED | HeartRateRecoveryTest.kt: `the age formula is Tanaka (208 - 0,7 x age), flagged estimated` | — |
| calculateHeartRateRecovery > an observed max below the trust bar is not used as a maximum | PORTED | HeartRateRecoveryTest.kt: `an observed max below the trust bar is not used as a maximum` | — |
| calculateHeartRateRecovery > two sources on the same instant collapse to the higher reading | PORTED | HeartRateRecoveryTest.kt: `two sources on the same instant collapse to the higher reading` | — |
| calculateHeartRateRecovery > samples arriving out of order are sorted, not trusted | PORTED | HeartRateRecoveryTest.kt: `samples arriving out of order are sorted, not trusted` | — |
| calculateHeartRateRecovery > a sample exactly on the tighter 1-minute tolerance boundary counts | PORTED | HeartRateRecoveryTest.kt: `a sample exactly on the tighter 1-minute tolerance boundary counts` | — |
| calculateHeartRateRecovery > a sample one second beyond the tolerance does not | PORTED | HeartRateRecoveryTest.kt: `a sample one second beyond the tolerance does not` | — |
| calculateHeartRateRecovery > a tie between two samples goes to the earlier, higher one | PORTED | HeartRateRecoveryTest.kt: `a tie between two samples goes to the earlier, higher one` | — |
| heartRateRecoveryWindowFor > a session with no rest segment has no recovery window | PORTED | HeartRateRecoveryTest.kt: `a session with no rest segment has no recovery window` | — |
| heartRateRecoveryWindowFor > a qualifying trailing rest segment is the moment effort stopped | PORTED | HeartRateRecoveryTest.kt: `a qualifying trailing rest segment is the moment effort stopped` | — |
| heartRateRecoveryWindowFor > the rest segment after the last set of a strength workout is NOT a recovery | PORTED | HeartRateRecoveryTest.kt: `the rest segment after the last set of a strength workout is NOT a recovery` | — |
| heartRateRecoveryWindowFor > a long rest that is not at the end is not a recovery either | PORTED | HeartRateRecoveryTest.kt: `a long rest that is not at the end is not a recovery either` | — |
| heartRateRecoveryWindowFor > a rest ending just shy of the session end still qualifies | PORTED | HeartRateRecoveryTest.kt: `a rest ending just shy of the session end still qualifies` | — |

## test/domain/insights/intensity_minutes_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/IntensityMinutesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| heartRateReserveCountsModerateMinutes | PORTED | IntensityMinutesTest.kt: `heartRateReserveCountsModerateMinutes` | — |
| heartRateReserveCountsVigorousMinutesDouble | PORTED | IntensityMinutesTest.kt: `heartRateReserveCountsVigorousMinutesDouble` | — |
| workoutActiveCaloriesFallbackIsLowConfidence | PORTED | IntensityMinutesTest.kt: `workoutActiveCaloriesFallbackIsLowConfidence` | — |
| cardioLoadFallbackProvidesLowConfidenceEstimate | PORTED | IntensityMinutesTest.kt: `cardioLoadFallbackProvidesLowConfidenceEstimate` | — |
| noInputsReturnNoData | PORTED | IntensityMinutesTest.kt: `noInputsReturnNoData` | — |

## test/domain/insights/metric_interpretations_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/MetricInterpretationsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| classifiesBloodPressureUsingHighestApplicableCategory | PORTED | MetricInterpretationsTest.kt: `classifiesBloodPressureUsingHighestApplicableCategory` | — |
| classifiesAdultBmiBoundaries | PORTED | MetricInterpretationsTest.kt: `classifiesAdultBmiBoundaries` | — |
| classifiesAdjustedFfmiBoundaries | PORTED | MetricInterpretationsTest.kt: `classifiesAdjustedFfmiBoundaries` | — |
| interpretsSleepAgainstUserTarget | PORTED | MetricInterpretationsTest.kt: `interpretsSleepAgainstUserTarget` | — |
| calculatesMacroSplitFromLoggedMacroCalories | PORTED | MetricInterpretationsTest.kt: `calculatesMacroSplitFromLoggedMacroCalories` | — |
| flagsMacroSplitOutsideReferenceWithoutRejectingData | PORTED | MetricInterpretationsTest.kt: `flagsMacroSplitOutsideReferenceWithoutRejectingData` | — |
| interpretsWorkoutProgressAgainstWeeklyReference | PORTED | MetricInterpretationsTest.kt: `interpretsWorkoutProgressAgainstWeeklyReference` | — |
| interpretsVitalsWithBroadAdultReferenceRanges | PORTED | MetricInterpretationsTest.kt: `interpretsVitalsWithBroadAdultReferenceRanges` | — |
| interpretsOxygenSaturationSeparatelyFromSimpleReferenceRanges | PORTED | MetricInterpretationsTest.kt: `interpretsOxygenSaturationSeparatelyFromSimpleReferenceRanges` | — |
| returnsNullForInvalidInputs | PORTED | MetricInterpretationsTest.kt: `returnsNullForInvalidInputs` | — |

## test/domain/insights/period_comparison_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/PeriodComparisonTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| comparison reports upward percent change | PORTED | PeriodComparisonTest.kt: `comparison reports upward percent change` | — |
| comparison reports downward percent change | PORTED | PeriodComparisonTest.kt: `comparison reports downward percent change` | — |
| comparison omits percent when previous value is zero | PORTED | PeriodComparisonTest.kt: `comparison omits percent when previous value is zero` | — |

## test/domain/insights/personal_baseline_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/PersonalBaselineTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| calculatesTrailingWindowAverages | PORTED | PersonalBaselineTest.kt: `calculatesTrailingWindowAverages` | — |
| marksValuesInsideStandardDeviationAsUsual | PORTED | PersonalBaselineTest.kt: `marksValuesInsideStandardDeviationAsUsual` | — |
| marksValuesOutsideUsualRangeButBelowAnomalyThreshold | PORTED | PersonalBaselineTest.kt: `marksValuesOutsideUsualRangeButBelowAnomalyThreshold` | — |
| marksTwoStandardDeviationsAsAnomaly | PORTED | PersonalBaselineTest.kt: `marksTwoStandardDeviationsAsAnomaly` | — |
| returnsNullWhenThereAreNotEnoughSamples | PORTED | PersonalBaselineTest.kt: `returnsNullWhenThereAreNotEnoughSamples` | — |

## test/domain/insights/route_elevation_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/RouteElevationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| elevationGainFromAltitudes > a flat route reports essentially no climb | PORTED | RouteElevationTest.kt: `a flat route reports essentially no climb` | Same seeded Box-Muller fixture reproduced in Kotlin |
| elevationGainFromAltitudes > a real climb is reported accurately, not inflated | PORTED | RouteElevationTest.kt: `a real climb is reported accurately not inflated` | — |
| elevationGainFromAltitudes > accuracy does not decay with route length | PORTED | RouteElevationTest.kt: `accuracy does not decay with route length` | — |
| elevationGainFromAltitudes > a clean staircase is measured, and descent is not counted as gain | PORTED | RouteElevationTest.kt: `a clean staircase is measured and descent is not counted as gain` | — |
| elevationGainFromAltitudes > a sparse imported route is not under-reported | PORTED | RouteElevationTest.kt: `a sparse imported route is not under-reported` | — |
| elevationGainFromAltitudes > a two-point climb is not swallowed by the smoothing lag | PORTED | RouteElevationTest.kt: `a two point climb is not swallowed by the smoothing lag` | — |
| elevationGainFromAltitudes > movement below the step threshold never accumulates | PORTED | RouteElevationTest.kt: `movement below the step threshold never accumulates` | — |
| elevationGainFromAltitudes > null and non-finite altitudes are skipped, not treated as zero | PORTED | RouteElevationTest.kt: `null and non-finite altitudes are skipped not treated as zero` | — |

## test/domain/insights/sleep_score_date_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/SleepScoreDateTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| date score uses previous daily sleep summaries as regularity baseline | PORTED | SleepScoreDateTest.kt: `date score uses previous daily sleep summaries as regularity baseline` | — |

## test/domain/insights/stress_tracking_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/insights/StressTrackingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| lowHrvAndElevatedRestingHeartRateProduceHighStress | PORTED | StressTrackingTest.kt: `lowHrvAndElevatedRestingHeartRateProduceHighStress` | — |
| balancedHrvAndNormalRestingHeartRateProduceRestingStress | PORTED | StressTrackingTest.kt: `balancedHrvAndNormalRestingHeartRateProduceRestingStress` | — |
| workoutsLowerConfidenceAndAddActivityCaveat | PORTED | StressTrackingTest.kt: `workoutsLowerConfidenceAndAddActivityCaveat` | — |
| noStressSignalsNeedMoreData | PORTED | StressTrackingTest.kt: `noStressSignalsNeedMoreData` | — |
| oneHrvPointIsUsedButReportedAsThinCoverage | PORTED | StressTrackingTest.kt: `oneHrvPointIsUsedButReportedAsThinCoverage` | — |
| dayContextCanRaiseStressEstimateAroundHeartSignals | PORTED | StressTrackingTest.kt: `dayContextCanRaiseStressEstimateAroundHeartSignals` | — |

## test/domain/dashboard/dashboard_aggregator_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/dashboard/DashboardAggregatorTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| weekly cardio target prefers recent history median | PORTED | DashboardAggregatorTest.kt: `weekly cardio target prefers recent history median` | — |
| merge derived projection keeps base calories unless estimated loaded | PORTED | DashboardAggregatorTest.kt: `merge derived projection keeps base calories unless estimated projection loaded` | — |
| median long returns middle value | PORTED | DashboardAggregatorTest.kt: `median long returns middle value` | — |

## test/domain/model/activity_backfill_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/ActivityBackfillTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| route backfill fills missing distance and elevation | PORTED | ActivityBackfillTest.kt: `route backfill fills missing distance and elevation` | was blocked: the backfill summed per-segment ascents instead of calling `RouteElevation.routeElevationGain` like recording and route import already did. Fixed; Flutter's fixture and its exact 70.0 expectation now pass unchanged, plus a new noise-floor rejection case |
| route backfill replaces empty zero summaries with route values | PORTED | ActivityBackfillTest.kt: `route backfill replaces empty zero summaries with route values` | — |
| route backfill preserves recorded summaries | PORTED | ActivityBackfillTest.kt: `route backfill preserves recorded summaries` | — |
| route backfill leaves elevation missing without altitude data | PORTED | ActivityBackfillTest.kt: `route backfill leaves elevation missing without altitude data` | — |
| sample backfill fills missing averages | PORTED | ActivityBackfillTest.kt: `sample backfill fills missing averages` | — |
| sample backfill preserves recorded averages | PORTED | ActivityBackfillTest.kt: `sample backfill preserves recorded averages` | — |
| session-metrics backfill fills the totals the session never carried | MISSING | — | blocked on behavior decision: Kotlin has no `ExerciseSessionMetrics` model and no `withSessionMetricsBackfilled`; the aggregate totals are applied inline inside `ActivityHealthReader.readExerciseSession`, so porting needs a prod extraction first |
| session-metrics backfill preserves what the session did record | MISSING | — | blocked on behavior decision: no `withSessionMetricsBackfilled` seam in Kotlin (see above) |
| an ungranted or unrecorded metric stays missing, never zero | MISSING | — | blocked on behavior decision: no `withSessionMetricsBackfilled` seam in Kotlin (see above) |
| sample backfill integrates a distance from speed when none was written | PORTED | ActivityBackfillTest.kt: `sample backfill integrates a distance from speed when none was written` | — |
| a recorded distance beats one integrated from speed | PORTED | ActivityBackfillTest.kt: `a recorded distance beats one integrated from speed` | — |

## test/domain/model/activity_session_deduplication_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/ActivitySessionDeduplicationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| deduplicateExerciseSessions keeps richer overlapping same type session | PORTED | ActivitySessionDeduplicationTest.kt: `deduplicateExerciseSessions keeps richer overlapping same type session` | — |
| deduplicateExerciseSessions keeps separate non overlapping sessions | PORTED | ActivitySessionDeduplicationTest.kt: `deduplicateExerciseSessions keeps separate non overlapping sessions` | — |
| the most recently edited of two identical duplicates wins | PORTED | ActivitySessionDeduplicationTest.kt: `the most recently edited of two identical duplicates wins` | — |

## test/domain/model/ble_recording_sample_buffer_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/BleRecordingSampleBufferTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| trimmed keeps latest samples per series | PORTED | BleRecordingSampleBufferTest.kt: `trimmed_keepsLatestSamplesPerSeries` | — |
| isEmpty when no samples | PORTED | BleRecordingSampleBufferTest.kt: `isEmpty_whenNoSamples` | — |

## test/domain/model/health_data_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/HealthDataTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| durationMinutes truncates sub-minute remainder | PORTED | HealthDataTest.kt: `durationMinutes truncates sub-minute remainder` | — |
| durationMinutes is zero for sub-minute duration | PORTED | HealthDataTest.kt: `durationMinutes is zero for sub-minute duration` | — |
| durationMinutes is exact for whole-minute duration | PORTED | HealthDataTest.kt: `durationMinutes is exact for whole-minute duration` | — |
| durationHours returns fractional hours | PORTED | HealthDataTest.kt: `durationHours returns fractional hours` | — |
| durationHours is zero for zero duration | PORTED | HealthDataTest.kt: `durationHours is zero for zero duration` | — |
| SleepStage durationMs equals end minus start epoch millis | PORTED | HealthDataTest.kt: `SleepStage durationMs equals end minus start epoch millis` | — |
| DailySteps defaults all optional fields to null | PORTED | HealthDataTest.kt: `DailySteps defaults all optional fields to null` | — |
| DailySteps stores all optional fields when provided | PORTED | HealthDataTest.kt: `DailySteps stores all optional fields when provided` | — |
| ActivityProgressPoint defaults detailed optional fields to null | PORTED | HealthDataTest.kt: `ActivityProgressPoint defaults detailed optional fields to null` | — |
| ActivityProgressPoint stores detailed optional fields | PORTED | HealthDataTest.kt: `ActivityProgressPoint stores detailed optional fields` | — |
| DashboardData defaults weight to null | PORTED | HealthDataTest.kt: `DashboardData defaults weight to null` | — |
| DashboardData stores latest weight with time when provided | PORTED | HealthDataTest.kt: `DashboardData stores latest weight with time when provided` | — |
| DashboardData stores latest height with time when provided | PORTED | HealthDataTest.kt: `DashboardData stores latest height with time when provided` | — |
| DashboardData defaults floorsClimbed to null | PORTED | HealthDataTest.kt: `DashboardData defaults floorsClimbed to null` | — |
| DashboardData stores floorsClimbed when provided | PORTED | HealthDataTest.kt: `DashboardData stores floorsClimbed when provided` | — |
| DashboardData defaults elevationGainedMeters to null | PORTED | HealthDataTest.kt: `DashboardData defaults elevationGainedMeters to null` | — |
| DashboardData stores elevationGainedMeters when provided | PORTED | HealthDataTest.kt: `DashboardData stores elevationGainedMeters when provided` | — |
| DailySteps floorsClimbed zero is non-null — permission granted no data | PORTED | HealthDataTest.kt: `DailySteps floorsClimbed zero is non-null — permission granted no data` | — |
| DailySteps elevationGainedMeters zero is non-null — permission granted no data | PORTED | HealthDataTest.kt: `DailySteps elevationGainedMeters zero is non-null — permission granted no data` | — |

## test/domain/model/heart_rate_aggregated_samples_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/HeartRateAggregatedSamplesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shouldUseAggregatedHeartRateSamples is true for day ranges | PORTED | HeartRateAggregatedSamplesTest.kt: `shouldUseAggregatedHeartRateSamples is true for day ranges` | — |
| shouldUseAggregatedHeartRateSamples is false for workout-length ranges | PORTED | HeartRateAggregatedSamplesTest.kt: `shouldUseAggregatedHeartRateSamples is false for workout-length ranges` | — |
| heartRateSampleFromAggregateBucket maps bucket start and average bpm | PORTED | HeartRateAggregatedSamplesTest.kt: `heartRateSampleFromAggregateBucket maps bucket start and average bpm` | — |

## test/domain/model/heart_rate_sample_reduction_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/HeartRateSampleReductionTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| reducedForChart keeps small lists unchanged | PORTED | HeartRateSampleReductionTest.kt: `reducedForChart keeps small lists unchanged` | — |
| reducedForChart caps large lists | PORTED | HeartRateSampleReductionTest.kt: `reducedForChart caps large lists` | — |

## test/domain/model/import_client_record_id_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/imports/csv/CsvRowConverterTest.kt (partial; Kotlin has no shared buildImportClientRecordId — csv and apple_health keep separate builders in CsvRowConverter.kt / AppleHealthImportConversionSupport.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildImportClientRecordId > an id is namespace, slugged prefix and 32 hex characters | DIVERGED | CsvRowConverterTest.kt: `the id is namespaced to csv so it cannot collide with apple_health` | asserts only the `csv_weightrecord_` prefix (32-hex tail implied only by the golden test); no regex / shared-builder assertion |
| buildImportClientRecordId > the same parts always produce the same id | DIVERGED | CsvRowConverterTest.kt: `the same measurement in pounds and kilograms yields the same id` | determinism asserted only for the csv builder via convertCsvRow, not a generic shared builder |
| buildImportClientRecordId > a csv id never collides with an apple_health id for the same parts | DIVERGED | CsvRowConverterTest.kt: `the id is namespaced to csv so it cannot collide with apple_health` | asserts csv prefix only; never builds the apple_health id for the same parts and compares |
| buildImportClientRecordId > different parts produce different ids | DIVERGED | CsvRowConverterTest.kt: `a different instant yields a different id`, `two metrics at the same instant get different ids` | csv-only, via converter API rather than the id builder |
| buildImportClientRecordId > an empty prefix still yields a three-part id | PORTED | ImportClientRecordIdTest.kt: `an empty prefix still yields a three-part id` | — |
| toStableIdSegment > a mixed-case type name slugs to lowercase | PORTED | ImportClientRecordIdTest.kt: `a mixed-case type name slugs to lowercase` | — |
| toStableIdSegment > runs of punctuation collapse to a single underscore | PORTED | ImportClientRecordIdTest.kt: `runs of punctuation collapse to a single underscore` | — |
| toStableIdSegment > leading and trailing separators are dropped | PORTED | ImportClientRecordIdTest.kt: `leading and trailing separators are dropped` | — |
| toStableIdSegment > a segment with nothing usable becomes "record" | PORTED | ImportClientRecordIdTest.kt: `a segment with nothing usable becomes record` | — |
| buildStableClientRecordId > the apple_health namespace still produces the ids it always has | PORTED | ImportClientRecordIdTest.kt: `the apple_health namespace still produces the ids it always has` | golden ids verified byte-for-byte against the Dart expectations |
| buildStableClientRecordId > it delegates to the shared builder under the apple_health namespace | N/A-FRAMEWORK | — | guards a Dart-side refactor (extraction into a shared builder); Kotlin deliberately keeps per-importer builders, so there is no delegation to assert |

## test/domain/model/sleep_day_attribution_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepDayAttributionTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a cross-midnight night is filed under the wake-up date | PORTED | SleepDayAttributionTest.kt: `a cross-midnight night is filed under the wake-up date` | — |
| a sleep-in past the morning hour stays that night, not a nap | PORTED | SleepDayAttributionTest.kt: `a sleep-in past the morning hour stays that night, not a nap` | — |
| a daytime session becomes a nap on its date and is not dropped | PORTED | SleepDayAttributionTest.kt: `a daytime session becomes a nap on its date and is not dropped` | — |
| custom window hours move the night boundary | PORTED | SleepDayAttributionTest.kt: `custom window hours move the night boundary` | — |

## test/domain/model/sleep_night_split_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepNightSplitTest.kt (+ features/sleep/SleepDailySummaryTest.kt for the presentation-level summary)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| splitNightAndNaps > keeps a night broken by a short early-morning wake together | PORTED | SleepNightSplitTest.kt: `splitNightAndNaps keeps a night broken by a short early-morning wake together` | — |
| splitNightAndNaps > splits an afternoon nap from the night | PORTED | SleepNightSplitTest.kt: `splitNightAndNaps splits an afternoon nap from the night` | — |
| splitNightAndNaps > a single session is the night | PORTED | SleepNightSplitTest.kt: `splitNightAndNaps treats a single session as the night` | — |
| splitNightAndNaps > empty input yields no night and no naps | PORTED | SleepNightSplitTest.kt: `splitNightAndNaps on empty input yields no night and no naps` | — |
| dailySleepSummary — night only, wall-clock > sums the night segments (wall-clock) and excludes a nap | PORTED | SleepNightSplitTest.kt: `dailySleepSummary sums the night segments in wall-clock and excludes a nap` | — |
| dailySleepSummary — night only, wall-clock > duration is wall-clock, not the stored time-asleep durationMs | PORTED | SleepNightSplitTest.kt: `dailySleepSummary duration is wall-clock, not the stored time-asleep durationMs` | — |
| dailySleepSummary — night only, wall-clock > overlapping night sessions count shared time once (union, not sum) | PORTED | SleepNightSplitTest.kt: `dailySleepSummary counts overlapping night sessions once (union, not sum)` | — |
| sleepSessionsUnionMs > overlapping intervals count their shared time once | PORTED | SleepNightSplitTest.kt: `sleepSessionsUnionMs counts overlapping intervals shared time once` | — |
| sleepSessionsUnionMs > disjoint intervals equal the sum of their spans | PORTED | SleepNightSplitTest.kt: `sleepSessionsUnionMs of disjoint intervals equals the sum of their spans` | — |
| sleepSessionsUnionMs > adjacent (touching) intervals merge without a gap | PORTED | SleepNightSplitTest.kt: `sleepSessionsUnionMs merges adjacent (touching) intervals without a gap` | — |
| sleepSessionsUnionMs > a fully-contained interval adds nothing | PORTED | SleepNightSplitTest.kt: `sleepSessionsUnionMs adds nothing for a fully-contained interval` | — |
| sleepSessionsUnionMs > empty input is zero | PORTED | SleepNightSplitTest.kt: `sleepSessionsUnionMs of empty input is zero` | — |

## test/domain/model/sleep_session_merging_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepSessionMergingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| mergeSleepSessions merges same-source sessions separated by a short gap | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions merges same-source sessions separated by a short gap` | — |
| mergeSleepSessions merges same-source sessions separated by up to sixty minutes | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions merges same-source sessions separated by up to sixty minutes` | — |
| mergeSleepSessions carries a pre-midnight split into the final sleep-ending day | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions carries a pre-midnight split into the final sleep-ending day` | — |
| mergeSleepSessions excludes bridged gaps from displayed sleep duration | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions excludes bridged gaps from displayed sleep duration` | — |
| sleepDurationMsFromStages excludes awake stages when sleep present | PORTED | SleepSessionMergingTest.kt: `sleepDurationMsFromStages excludes awake stages when sleep stages are present` | — |
| mergeSleepSessions does not merge sessions from different sources | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions does not merge sessions from different sources` | — |
| mergeSleepSessions removes overlapping duplicate sessions from different sources | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions removes overlapping duplicate sessions from different sources` | — |
| mergeSleepSessions removes a near-total cross-source duplicate whose end drifts past the old tolerance | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions removes a near-total cross-source duplicate whose end drifts past the old tolerance` | — |
| mergeSleepSessions keeps two different-source sessions that overlap under the ratio | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions keeps two different-source sessions that overlap under the ratio` | — |
| mergeSleepSessions does not merge sessions beyond the max gap | PORTED | SleepSessionMergingTest.kt: `mergeSleepSessions does not merge sessions beyond the max gap` | — |
| mergedSleepSessionComponentIds returns null for raw or invalid ids | PORTED | SleepSessionMergingTest.kt: `mergedSleepSessionComponentIds returns null for raw or invalid ids` | — |
| mergedSleepSessionComponentIds returns encoded raw ids | PORTED | SleepSessionMergingTest.kt: `mergedSleepSessionComponentIds returns encoded raw ids` | — |

## test/domain/model/sleep_split_night_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepSplitNightTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| combineNightStages fills the wake gap with Awake | PORTED | SleepSplitNightTest.kt: `combineNightStages fills the wake gap with Awake` | — |
| a gap larger than maxGap (a daytime nap) is not bridged | PORTED | SleepSplitNightTest.kt: `a gap larger than maxGap (a daytime nap) is not bridged` | — |
| the split night is reliable once its gap is filled | PORTED | SleepSplitNightTest.kt: `the split night is reliable once its gap is filled` | — |

## test/domain/model/sleep_stage_coverage_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/model/SleepStageCoverageTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a fully-staged night is reliable | PORTED | SleepStageCoverageTest.kt: `a fully-staged night is reliable` | — |
| a tail-only session is not reliable | PORTED | SleepStageCoverageTest.kt: `a tail-only session is not reliable` | — |
| a session with no stages is not reliable | PORTED | SleepStageCoverageTest.kt: `a session with no stages is not reliable` | — |
| coverage is measured against the span, not the stages own extent | PORTED | SleepStageCoverageTest.kt: `coverage is measured against the span, not the stages own extent` | — |
| a zero-length session never divides by zero | PORTED | SleepStageCoverageTest.kt: `a zero-length session never divides by zero` | — |

## test/domain/preferences/body_energy_calibration_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/preferences/BodyEnergyCalibrationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| manual zones round trip through preference string | PORTED | BodyEnergyCalibrationTest.kt: `manual zones round trip through preference string` | — |
| invalid manual zones are ignored and manual zone mode is disabled | PORTED | BodyEnergyCalibrationTest.kt: `invalid manual zones are ignored and manual zone mode is disabled` | — |
| automatic calibration has no manual zones | PORTED | BodyEnergyCalibrationTest.kt: `automatic calibration has no manual zones` | — |
| automatic calibration defaults to setup not completed | PORTED | BodyEnergyCalibrationTest.kt: `automatic calibration defaults to setup not completed` | — |
| normalization preserves setupCompleted flag | PORTED | BodyEnergyCalibrationTest.kt: `normalization preserves setupCompleted flag` | — |

## test/domain/preferences/body_profile_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/preferences/BodyProfileTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| normalization keeps optional values in safe ranges | PORTED | BodyProfileTest.kt: `normalization keeps optional values in safe ranges` | Kotlin is a superset (also clamps max/resting HR) |
| age is derived from birth year | PORTED | BodyProfileTest.kt: `age is derived from birth year` | — |
| empty profile has no age and automatic signature | PORTED | BodyProfileTest.kt: `empty profile has no age and automatic signature` | — |

## test/domain/preferences/metric_detail_section_id_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/preferences/MetricDetailSectionIdTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| returns default order when stored is null | PORTED | MetricDetailSectionIdTest.kt: `metricDetailSectionOrderFromStored_returnsDefaultWhenNull` | — |
| merges missing sections after the stored ones | PORTED | MetricDetailSectionIdTest.kt: `metricDetailSectionOrderFromStored_mergesMissingSections` | — |
| ignores unknown values | PORTED | MetricDetailSectionIdTest.kt: `metricDetailSectionOrderFromStored_ignoresUnknownValues` | — |

## test/domain/usecase/fit_body_energy_from_watch_use_case_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/usecase/FitBodyEnergyFromWatchUseCaseTest.kt + BodyEnergyWatchCalibrationTest.kt (the pure observation/fold pieces). The orchestrator was genuinely MISSING, not merely untested: both halves were ported but nothing joined them, so watch Body Battery never reached the gains. FitBodyEnergyFromWatchUseCase now runs after a Garmin sync, once the chain has been rebuilt.
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| with no samples it does nothing | DIVERGED | BodyEnergyWatchCalibrationTest.kt: `no samples, or no timeline, yields nothing`, `no readings leaves the gains untouched` | domain-level only; no assertion that persisted watchObservationCount stays 0 |
| folds new readings in and moves the gains | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `an hour of watch samples counts once, however many samples it holds` | the DAO-to-prefs persistence path is now exercised end to end, not just the fold |
| a second run does not re-count the same samples | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `syncing the same hour twice does not teach the model twice` | |
| only samples newer than the watermark are fitted | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a resumed run starts at the bucket after the watermark` | starts at watermark + one bucket, so the fitted hour is not re-read |
| a failing timeline leaves the gains and watermark untouched | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a throwing timeline read is treated as a day with no timeline`, `a failing sample read is swallowed and changes nothing` | |
| samples that pair to nothing leave the watermark alone | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a day whose timeline pairs nothing is held, then retired with the rest` | held while recent, retired past the two-day grace so it cannot wedge the watermark |
| an hour contributes ONE observation however often you sync | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `an hour of watch samples counts once, however many samples it holds`, `syncing the same hour twice does not teach the model twice` | the multi-sync accumulation scenario is now covered across use-case runs |
| successive hours each contribute one observation | DIVERGED | BodyEnergyWatchCalibrationTest.kt: `buildWatchObservations downsamples to one reading per bucket` | one-per-hour bucketing covered at domain level, not across successive use-case runs |
| a cold day must not retire the days behind it > an unexamined older day blocks the run instead of being burned | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a recent day the chain has not reached yet is waited for, not skipped`, `days are retired oldest first, and a cold recent day stops the run` | |
| a cold day must not retire the days behind it > a cold day mid-window does not block the days behind it | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a cold day older than the grace period is retired so the days behind it can move` | |
| a cold day must not retire the days behind it > but a day past the lookback window is retired, not wedged | PORTED | FitBodyEnergyFromWatchUseCaseTest.kt: `a cold day older than the grace period is retired so the days behind it can move` | |
| the gains stay in bounds across many runs | DIVERGED | BodyEnergyWatchCalibrationTest.kt: `gains stay within their bounds however extreme the disagreement`, `a day of MAXIMAL disagreement does reach the clamp` | bounds property covered per-fold, not across 24 persisted runs |

## test/domain/usecase/load_heart_period_use_case_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/domain/usecase/LoadHeartPeriodUseCaseTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| merge combines heart and vitals result halves | DIVERGED | LoadHeartPeriodUseCaseTest.kt: `combined request merges heart and vitals` | merge exercised via the Combined request; asserts one scalar (restingBpm) + permission set only, not sample/spO2 list halves |
| merge prefers the left-hand scalar when both are present | PORTED | LoadHeartPeriodUseCaseTest.kt: `merge prefers the left-hand scalar when both are present` | seam: `HeartPeriodLoadResult.merge` private -> internal |
| vitalsSummary picks the latest entry per series | PORTED | LoadHeartPeriodUseCaseTest.kt: `vitalsSummary picks the latest entry per series` | — |

## test/domain/usecase/save_hydration_entry_use_case_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryViewModelTest.kt (partial; use case ported as HydrationDrinkLogger.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| rolls the hydration half back when the nutrition write fails | PORTED | HydrationDrinkLoggerTest.kt: `rolls the hydration half back when the nutrition write fails` | — |
| does not roll back when both halves succeed | DIVERGED | HydrationEntryViewModelTest.kt: `saved custom drink entry writes hydration nutrients` | happy path writes both halves, but absence of the rollback delete is not asserted |

## test/data/local/beverage/beverage_entity_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/local/beverage/BeverageEntityTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| BeverageEntity > preloaded defaults include water category drinks | PORTED | BeverageEntityTest.kt: `preloaded defaults include water category drinks` | — |
| BeverageEntity > preloaded defaults seed the caffeine catalog after the waters | PORTED | BeverageEntityTest.kt: `preloaded defaults seed the caffeine catalog after the waters` | — |

## test/data/local/beverage/beverage_store_test.dart
Kotlin counterpart: none (Kotlin BeverageStore/BeverageDao have no tests; androidTest has none either)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| beverages() seeds preloaded defaults on first access | PORTED | BeverageStoreTest.kt: `beverages seeds preloaded defaults on first access` | — |
| save() inserts a new active drink with the next sort order | PORTED | BeverageStoreTest.kt: `save inserts a new active drink with the next sort order` | — |
| delete() soft-deletes and hides the drink from active listing | PORTED | BeverageStoreTest.kt: `delete soft-deletes and hides the drink from active listing` | — |
| moveToCategory() updates the persisted category | PORTED | BeverageStoreTest.kt: `moveToCategory updates the persisted category` | — |
| reorder() reindexes provided ids first, keeping the rest after | PORTED | BeverageStoreTest.kt: `reorder reindexes provided ids first, keeping the rest after` | — |
| DAO nextSortOrder returns max+1 | N/A-FRAMEWORK | — | Room `@Query` SQL (COALESCE(MAX)+1); instrumented-only concern, no androidTest DAO coverage exists |
| DAO insertDefaults ignores conflicts on existing ids | N/A-FRAMEWORK | — | Room `OnConflictStrategy.IGNORE` SQL behavior; instrumented-only, no androidTest coverage |
| DAO upsert replaces the row and can clear the delete flag | N/A-FRAMEWORK | — | Room `@Upsert` SQL behavior; instrumented-only, no androidTest coverage |

## test/data/local/body_energy_timeline_dao_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BodyEnergyTimelineStoreTest.kt (via FakeBodyEnergyTimelineDao; the DAO's `@Transaction` default methods run for real)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| stores a day with its buckets and reads both back | DIVERGED | BodyEnergyTimelineStoreTest.kt: `a saved timeline round-trips every field the summary carries` / `a saved bucket round-trips the new drain components` | round-trip covered at store level, but the SQL `ORDER BY time_millis` (out-of-order insert read back sorted) is only the fake's reimplementation |
| re-storing a day replaces its buckets rather than merging them | PORTED | BodyEnergyTimelineStoreTest.kt: `saving a day replaces its buckets rather than accumulating them` | real `upsertDay` @Transaction delete-then-insert logic runs against the fake |
| daysBetween is inclusive, ordered, and excludes days out of range | DIVERGED | BodyEnergyTimelineStoreTest.kt: `storedDaysBetween returns the window oldest first without decoding buckets` | window inclusivity/ordering asserted, but via the fake's reimplementation of the `BETWEEN` query, not real SQL |
| bucketsBetweenDays spans days in primary-key order | N/A-FRAMEWORK | — | Kotlin DAO has no cross-day bucket query by design (per-day reads; chain walk lives in repository) |
| deleteDays clears both tables in range and leaves neighbours intact | PORTED | BodyEnergyTimelineStoreTest.kt: `invalidateForward drops the range from both tables` | real `deleteDays` default-method logic exercised |
| deleteDays is a no-op when the range is empty | PORTED | BodyEnergyTimelineStoreTest.kt: `invalidateForward is a no-op when the range is empty` | driven through the store wrapper over dao.deleteDays |
| purgeBucketsBefore drops buckets but keeps the day summaries | PORTED | BodyEnergyTimelineStoreTest.kt: `retention drops old buckets but keeps their day summaries walkable` | — |
| purgeAll empties both tables and the chain cursor | PORTED | BodyEnergyTimelineStoreTest.kt: `purgeAll clears the chain and its cursor` | — |
| latestDay returns the newest stored day | N/A-FRAMEWORK | — | Kotlin DAO deliberately omits `latestDay` (KDoc: the signature-aware walk lives in the repository, covered by BodyEnergyChainTest) |
| writeChainCursor leaves omitted fields untouched | PORTED | BodyEnergyTimelineStoreTest.kt: `recording a pass leaves the global signature intact` | real `writeChainCursor` null-preserving default-method logic exercised |

## test/data/local/garmin_wellness_dao_test.dart
Kotlin counterpart: none for the DAO itself (GarminWellnessDao referenced by no test); schema-version case maps to data/local/OpenVitalsDatabaseMigrationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| stores and reads back a window, oldest first | N/A-FRAMEWORK | — | Room `@Query` SQL; instrumented-only, no androidTest DAO coverage exists |
| re-syncing the same window overwrites rather than duplicating | N/A-FRAMEWORK | — | Room `OnConflictStrategy.REPLACE` on PK(metric, time_millis); PK shape is asserted in OpenVitalsDatabaseMigrationTest's MIGRATION_5_6 SQL check |
| the two metrics do not collide at the same instant | N/A-FRAMEWORK | — | composite-PK SQLite behavior; PK shape asserted only via MIGRATION_5_6 SQL string |
| latest returns the newest sample, or null when empty | N/A-FRAMEWORK | — | Room `@Query ORDER BY ... LIMIT 1`; instrumented-only |
| an empty batch is a no-op | N/A-FRAMEWORK | — | Room insert of empty list; framework behavior |
| the window is half-open: start inclusive, end exclusive | N/A-FRAMEWORK | — | the `>= <` predicate exists verbatim in the Kotlin `@Query` but is untestable on JVM; no androidTest |
| the schema version was bumped for this table | DIVERGED | OpenVitalsDatabaseMigrationTest.kt: `version five migrates to the garmin wellness table` | Kotlin asserts MIGRATION_5_6 endVersion == 6, not the `@Database` version constant itself |

## test/data/local/open_vitals_database_migration_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/local/OpenVitalsDatabaseMigrationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| legacy version one migrates to beverage schema version three | DIVERGED | OpenVitalsDatabaseMigrationTest.kt: `legacy version one migrates to beverage schema version three` | Kotlin verifies execSQL on a mockk db (start/end version + CREATE TABLE string); Flutter also runs it against real SQLite and checks the table exists |
| legacy version two migrates to beverage schema version three | DIVERGED | OpenVitalsDatabaseMigrationTest.kt: `legacy version two migrates to beverage schema version three` | same mock-based verification, no real-SQLite execution |
| v6 -> v7 (the Body Energy chain) > the migration creates both chain tables and reaches the current version | DIVERGED | OpenVitalsDatabaseMigrationTest.kt: `version four migrates to the body energy chain tables` | Kotlin (MIGRATION_4_5) mock-verifies both chain-table CREATEs and the cursor-table reuse; no real upgrade run or current-version assertion |
| v6 -> v7 > the upgrade drops the retired feel-check log | N/A-FRAMEWORK | — | `feel_checks` never existed in the Kotlin schema history |
| v6 -> v7 > dropping it does not disturb the tables beside it | N/A-FRAMEWORK | — | feel_checks-specific Drift schema-history concern |
| v6 -> v7 > the pre-v7 tables and their rows survive the upgrade | N/A-FRAMEWORK | — | data-survival across a real upgrade needs Room MigrationTestHelper (instrumented); no androidTest migration coverage exists |
| v6 -> v7 > the hand-written v7 SQL produces the same schema as onCreate | N/A-FRAMEWORK | — | migration-SQL-vs-generated-schema comparison needs Room schema export + instrumented verifier; MIGRATION_5_6 test pins column shapes by string only |

## test/data/local/vitals_daily_cache_dao_test.dart
Kotlin counterpart: none direct (VitalsDailyCacheDao appears in VitalsRepositoryTest.kt / VitalsHistorySyncServiceTest.kt only as a mockk stub)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| upsert then read back a day range, ordered by day | N/A-FRAMEWORK | — | Room `@Query`/REPLACE SQL; instrumented-only, no androidTest DAO coverage exists |
| upsert overwrites the same (metric, day) | N/A-FRAMEWORK | — | REPLACE-on-PK SQLite behavior; instrumented-only |
| replaceMetric atomically swaps every day for that metric only | PORTED | VitalsDailyCacheDaoTest.kt: `replaceMetric atomically swaps every day for that metric only` | — |
| blood pressure carries a secondary (diastolic) sum | N/A-FRAMEWORK | — | Drift column round-trip; Kotlin entity has `secondarySum` but storage is Room-only, and no JVM test reconstructs the diastolic mean |
| deleteDay removes only that day | N/A-FRAMEWORK | — | plain SQL DELETE; instrumented-only |
| sync cursor > writeFullSync sets token and stamp; writeToken preserves the stamp | PORTED | VitalsDailyCacheDaoTest.kt: `writeFullSync sets token and stamp - writeToken preserves the stamp` | — |
| sync cursor > writeToken inserts a row when none exists yet | PORTED | VitalsDailyCacheDaoTest.kt: `writeToken inserts a row when none exists yet` | — |
| sync cursor > cursor is null for an unsynced metric | N/A-FRAMEWORK | — | trivial `@Query` miss; instrumented-only |

## test/data/migration/kotlin_data_migration_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/migration/ (FlutterPrefsKeyTableTest.kt, FlutterPrefsCodecTest.kt, BleDeviceRepositoryFlutterPayloadTest.kt — the INVERSE migrator's tests)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| acceptance > goals, unit system, theme and the caffeine profile all round-trip | PORTED | FlutterPrefsKeyTableTest.kt: enum-transcode + typed-copy regions | mirrored by the inverse migrator: same key tables, enums, and typed copies exercised Dart->Kotlin |
| type fidelity > a Kotlin Float goal survives as a Dart double | PORTED | FlutterPrefsKeyTableTest.kt: `doubles become kotlin floats` | inverse mirror of the same float/double bridge |
| type fidelity > a Kotlin Set<String> survives as getStringList | PORTED | FlutterPrefsKeyTableTest.kt: `string lists become string sets` | inverse mirror of the set/list bridge |
| type fidelity > a Kotlin Long survives as a Dart int | PORTED | FlutterPrefsKeyTableTest.kt: `keys kotlin reads with getLong stay long` / `dart ints become kotlin ints` | inverse mirror of the long/int bridge |
| enum value transcoding > SCREAMING_SNAKE enum names become the Dart lowerCamelCase names | PORTED | FlutterPrefsKeyTableTest.kt: `unit system transcodes`, `activity week mode transcodes multi word names`, `caffeine enums transcode`, `detail ranges transcode time range names` | inverse mirror of the same name transcoding |
| enum value transcoding > app_language maps the Kotlin BCP-47 tag onto the Dart enum name | PORTED | FlutterPrefsKeyTableTest.kt: `app language maps dart names to kotlin storage values` | inverse mirror |
| enum value transcoding > Kotlin's tagless SYSTEM language maps onto AppLanguage.system | PORTED | FlutterPrefsKeyTableTest.kt: `app language maps dart names to kotlin storage values` / `app language tolerates stale kotlin storage values` | SYSTEM sentinel covered in both directions |
| enum value transcoding > an enum value with no Dart counterpart is skipped, not written | PORTED | FlutterPrefsKeyTableTest.kt: `unknown enum values are skipped not written` | inverse mirror |
| enum value transcoding > dashboard_widget_order is dropped: Dart keys that list by tile title | PORTED | FlutterPrefsKeyTableTest.kt: `unportable and bookkeeping keys are dropped` | inverse migrator drops the same vocabulary-mismatched key; neighbour copy covered by `strings with kotlin compatible wire formats copy verbatim` |
| enum value transcoding > the verbatim-compatible composite payloads pass through untouched | PORTED | FlutterPrefsKeyTableTest.kt: `strings with kotlin compatible wire formats copy verbatim` / `string lists become string sets` | inverse mirror of the same composite payload keys |
| enum value transcoding > the mindfulness sounds are already wire-compatible and pass through | DIVERGED | FlutterPrefsKeyTableTest.kt: `strings with kotlin compatible wire formats copy verbatim` | inverse table copies verbatim generally, but the two mindfulness sound keys are never named in a Kotlin test |
| BLE devices > the `devices` key is renamed to `ble_sensor_devices` | PORTED | FlutterPrefsKeyTableTest.kt: `ble registry routes to its own file under the devices key verbatim` | exact inverse rename asserted (plus BleDeviceRepositoryFlutterPayloadTest for payload tolerance) |
| activity markers > marker notes keep their key names | PORTED | FlutterPrefsKeyTableTest.kt: `activity markers route to the marker file keeping their key` | inverse mirror |
| database > is copied, with its -wal sidecar, when the destination is absent | N/A-FRAMEWORK | — | the Kotlin inverse imports rows out of the drift db via android.database.sqlite (no JVM seam) and never copies the file or its sidecars |
| database > never clobbers an existing drift database | N/A-FRAMEWORK | — | the inverse has no no-clobber guard by design (BeverageDao.replaceAll replaces the Room catalog wholesale) and the path is android.database.sqlite-only |
| offline maps > the pack directory is moved and its metadata lands in prefs | DIVERGED | FlutterPrefsKeyTableTest.kt: `offline maps metadata is not written as a preference` | inverse only asserts the metadata key is dropped from prefs; `migrateOfflineMaps` (dir move + metadata.json rewrite) untested |
| offline maps > an existing destination is left alone | PORTED | FlutterDataMigratorTest.kt: `an existing destination is left alone` | — |
| home-screen widgets > metric and beverage selections are re-pointed at the Dart key scheme | DIVERGED | FlutterPrefsKeyTableTest.kt: `known metric widget ids pass through` | id table mirrored, but `migrateHomeWidgets` key re-pointing (prefix scheme, pending-key skip) untested |
| home-screen widgets > the 1x1 one-tap widget shares the 2x1 widget's key namespace | N/A-FRAMEWORK | — | Flutter `homeWidgetKeyPrefix` internals; Kotlin has its own widget prefs scheme |
| home-screen widgets > a metric id with no Dart counterpart is skipped, not written | PORTED | FlutterPrefsKeyTableTest.kt: `unknown metric widget ids are rejected` / `dart only intensity minutes maps back to cardio load` | inverse mirror of the id-vocabulary guard |
| run conditions > is a no-op when there is no legacy data | PORTED | FlutterDataMigratorTest.kt: `is a no-op when there is no legacy data` | — |
| run conditions > is skipped when the Flutter side has already been used | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin inverse deliberately overwrites stale Kotlin-era values, so there is no anti-clobber guard to assert |
| run conditions > is idempotent: a second run changes nothing | PORTED | FlutterDataMigratorTest.kt: `is idempotent - a second run changes nothing` | — |
| run conditions > the one-shot flag is set even when every step fails | PORTED | FlutterDataMigratorTest.kt: `the one-shot flag is set even when every step fails` | — |
| robustness > never throws when the native channel is missing (iOS, tests) | N/A-FRAMEWORK | — | MissingPluginException is Flutter-channel-specific; Kotlin migrator has no channel |
| robustness > a value of an unsupported type is skipped, not fatal | PORTED | FlutterPrefsCodecTest.kt: `unsupported raw types are skipped` | mirrored by the inverse codec |
| robustness > a missing legacy database and files dir are simply skipped | PORTED | FlutterDataMigratorTest.kt: `a missing legacy database and files dir are simply skipped` | — |
| robustness > a null HomeWidgetClient is tolerated | N/A-FRAMEWORK | — | Flutter-specific optional plugin client; no Kotlin analog |

## test/data/prefs/preferences_repository_codec_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/PreferencesRepositoryCodecTest.kt (+ domain/preferences/ActivityRecordingPreferencesTest.kt for the model-level normalization)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| activity recording dashboard layout codec > every template normalizes to largeTop — the others are storage-only | PORTED | PreferencesRepositoryCodecTest.kt: `every template normalizes to LARGE_TOP - the others are storage-only` | — |
| activity recording dashboard layout codec > round-trips the field order | PORTED | PreferencesRepositoryCodecTest.kt: `round-trips the field order` | — |
| activity recording dashboard layout codec > round-trips per-field sizes | PORTED | PreferencesRepositoryCodecTest.kt: `round-trips per-field sizes` | — |
| activity recording dashboard layout codec > layouts are per activity type, not global | PORTED | PreferencesRepositoryCodecTest.kt: `layouts are per activity type, not global` | — |
| activity recording dashboard layout codec > an unknown activity type falls back to the default layout | PORTED | PreferencesRepositoryCodecTest.kt: `an unknown activity type falls back to the default layout` | — |
| activity recording dashboard layout codec > a corrupt stored string degrades to the default, never throws | PORTED | PreferencesRepositoryCodecTest.kt: `a corrupt stored string degrades to the default, never throws` | — |
| activity recording dashboard layout codec > an unknown field in a stored layout is dropped, not fatal | PORTED | PreferencesRepositoryCodecTest.kt: `an unknown field in a stored layout is dropped, not fatal` | — |
| activity recording preferences — the null sentinels > null route gap survives a round-trip as null, not zero | PORTED | PreferencesRepositoryCodecTest.kt: `null route gap survives a round-trip as null, not zero` | — |
| activity recording preferences — the null sentinels > null distance interval survives as null, not zero | PORTED | PreferencesRepositoryCodecTest.kt: `null distance interval survives as null, not zero` | — |
| activity recording preferences — the null sentinels > null voice intervals survive as null, not zero | PORTED | PreferencesRepositoryCodecTest.kt: `null voice intervals survive as null, not zero` | — |
| activity recording preferences — the null sentinels > a real value round-trips as itself, and is not read as null | PORTED | PreferencesRepositoryCodecTest.kt: `a real value round-trips as itself, and is not read as null` | — |
| activity recording preferences — the null sentinels > the booleans and the timeout round-trip | PORTED | PreferencesRepositoryCodecTest.kt: `the booleans and the timeout round-trip` | — |

## test/data/prefs/preferences_repository_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/PreferencesRepositoryTest.kt (over a FakeSharedPreferences; + domain/preferences/BodyProfileTest.kt and BodyEnergyCalibrationTest.kt at model level)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| scalar keys > onboardingDone defaults false and round-trips | PORTED | PreferencesRepositoryTest.kt: `onboardingDone defaults false and round-trips` | — |
| scalar keys > healthConnectSyncEnabled defaults true | PORTED | PreferencesRepositoryTest.kt: `healthConnectSyncEnabled defaults true` | — |
| scalar keys > permission cancel count coerces to at least zero | PORTED | PreferencesRepositoryTest.kt: `permission cancel count coerces to at least zero` | — |
| scalar keys > accepted privacy version can be cleared | PORTED | PreferencesRepositoryTest.kt: `accepted privacy version can be cleared` | — |
| scalar keys > nullable exercise types round-trip and clear | PORTED | PreferencesRepositoryTest.kt: `nullable exercise types round-trip and clear` | — |
| scalar keys > hydration daily goal defaults 2.0 and clamps | PORTED | PreferencesRepositoryTest.kt: `hydration daily goal defaults 2 liters and clamps` | — |
| scalar keys > heart-rate thresholds default and clamp | PORTED | PreferencesRepositoryTest.kt: `heart-rate thresholds default and clamp` | — |
| the unit-system default is a function of the locale, not the host > a US device starts out imperial | PORTED | PreferencesRepositoryTest.kt: `a US device starts out imperial` | — |
| the unit-system default is a function of the locale, not the host > the rest of the world starts out metric | PORTED | PreferencesRepositoryTest.kt: `the rest of the world starts out metric` | — |
| the unit-system default is a function of the locale, not the host > a locale with no country is metric, not a crash | PORTED | PreferencesRepositoryTest.kt: `a locale with no country is metric, not a crash` | — |
| the unit-system default is a function of the locale, not the host > a stored choice wins over the locale | PORTED | PreferencesRepositoryTest.kt: `a stored choice wins over the locale` | — |
| enum-backed reactive values > unitSystem set/read and notifies the listenable | PORTED | PreferencesRepositoryTest.kt: `unitSystem set and read notifies the flow` | Kotlin analog of the Dart ValueListenable is a StateFlow |
| enum-backed reactive values > appThemeMode / sleep window round-trip via a fresh instance | PORTED | PreferencesRepositoryTest.kt: `appThemeMode and sleep window round-trip via a fresh instance` | — |
| enum-backed reactive values > sleep window defaults to 18:00-10:00 and clamps out-of-range hours | PORTED | PreferencesRepositoryTest.kt: `sleep window defaults to 18 to 10 and clamps out-of-range hours` | — |
| time ranges and daily goals > timeRangeFor default then override | PORTED | PreferencesRepositoryTest.kt: `timeRangeFor default then override` | — |
| time ranges and daily goals > dailyGoalFor default then normalized override | PORTED | PreferencesRepositoryTest.kt: `dailyGoalFor default then normalized override` | — |
| structured configs > bodyProfile round-trips and normalizes | PORTED | PreferencesRepositoryTest.kt: `bodyProfile round-trips and normalizes` | — |
| structured configs > bodyEnergyCalibration round-trips manual zones | PORTED | PreferencesRepositoryTest.kt: `bodyEnergyCalibration round-trips manual zones` | — |
| structured configs > caffeinePreferences round-trips every field | PORTED | PreferencesRepositoryTest.kt: `caffeinePreferences round-trips every field` | — |
| structured configs > hydration reminder config round-trips and normalizes interval | PORTED | PreferencesRepositoryTest.kt: `hydration reminder config round-trips and normalizes interval` | — |
| structured configs > mindfulness reminder + timer config round-trip | PORTED | PreferencesRepositoryTest.kt: `mindfulness reminder and timer config round-trip` | — |
| structured configs > legacy mindfulness bell sound values map forward | PORTED | PreferencesRepositoryTest.kt: `legacy mindfulness bell sound values map forward` | — |
| hydration containers and custom drinks > container volumes accumulate and reject invalid input | PORTED | PreferencesRepositoryTest.kt: `container volumes accumulate and reject invalid input` | — |
| hydration containers and custom drinks > last custom hydration amount round-trips | PORTED | PreferencesRepositoryTest.kt: `last custom hydration amount round-trips` | — |
| hydration containers and custom drinks > recent hydration amounts keep the last two, newest first | PORTED | PreferencesRepositoryTest.kt: `recent hydration amounts keep the last two, newest first` | — |
| hydration containers and custom drinks > recent hydration amounts filter corrupt stored values on read | PORTED | PreferencesRepositoryTest.kt: `recent hydration amounts filter corrupt stored values on read` | — |
| hydration containers and custom drinks > custom drinks save, reorder, and delete preserving order | PORTED | PreferencesRepositoryTest.kt: `custom drinks save, reorder, and delete preserving order` | — |
| hydration containers and custom drinks > custom drinks with special characters survive encoding | PORTED | PreferencesRepositoryTest.kt: `custom drinks with special characters survive encoding` | — |
| ordered widget lists and acknowledged permissions > dashboard/manual/section order round-trip | PORTED | PreferencesRepositoryTest.kt: `dashboard, manual and section order round-trip` | — |
| ordered widget lists and acknowledged permissions > acknowledged permissions union | PORTED | PreferencesRepositoryTest.kt: `acknowledged permissions union` | — |
| legacy body-profile values migrate on first read | PORTED | PreferencesRepositoryTest.kt: `legacy body-profile values migrate on first read` | — |
| the Body Energy setup re-gate > reopens setup for an install with no age source | N/A-FRAMEWORK | — | Flutter-only behavior: Kotlin retains the manual max-HR profile field, so no re-gate exists in its PreferencesRepository |
| the Body Energy setup re-gate > leaves an install with a birth year alone | N/A-FRAMEWORK | — | no Kotlin re-gate logic |
| the Body Energy setup re-gate > leaves an install with manual zones alone | N/A-FRAMEWORK | — | no Kotlin re-gate logic |
| the Body Energy setup re-gate > runs once, so setup completed afterwards is not undone | N/A-FRAMEWORK | — | no Kotlin re-gate/epoch logic |

## test/data/repository/activity_repository_activity_progress_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Day range reads the intraday progress by default | PORTED | ActivityRepositoryTest.kt: `DAY activity metric progress uses raw full data for selected day graph` | Kotlin also asserts the summarised read is never used |
| includeActivityProgress:false skips the intraday read on Day | PORTED | ActivityRepositoryGatingTest.kt: `includeActivityProgress false skips the intraday read on Day` | — |

## test/data/repository/activity_repository_calories_cache_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryCaloriesCacheTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a synced, in-window calories day is served from the cache | PORTED | ActivityRepositoryCaloriesCacheTest.kt: `cache hit serves the range zero-filled without touching Health Connect` | Driven via loadDailyNutrition (the same path loadActivityPeriod uses); adds zero-fill assertions |
| without a cursor the cache is skipped and the live read runs | PORTED | ActivityRepositoryCaloriesCacheTest.kt: `no cursor means the cache is not trusted and the live read runs` | — |

## test/data/repository/activity_repository_daily_steps_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryTest.kt + ActivityRepositoryGatingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| clamps the scan to the last 30 days when history perm is ungranted | PORTED | ActivityRepositoryTest.kt: `loadDailySteps clamps to recent days when history permission is required but missing` | — |
| scans from the full start when the history perm is granted | PORTED | ActivityRepositoryTest.kt: `loadDailySteps keeps full range when history permission is granted` | — |
| scans from the full start when history access is not gated | PORTED | ActivityRepositoryGatingTest.kt: `loadDailySteps scans from the full start when history access is not gated` | — |
| requests floors when the floors permission is granted | PORTED | ActivityRepositoryGatingTest.kt: `loadDailySteps requests floors when the floors permission is granted` | — |
| omits floors when the floors permission is ungranted | PORTED | ActivityRepositoryTest.kt: `loadDailySteps reads steps when distance permission is missing` | coVerify includeFloors=false with floors ungranted |
| returns empty without the steps permission | PORTED | ActivityRepositoryGatingTest.kt: `loadDailySteps returns empty without the steps permission` | also asserts the data source is never queried |

## test/data/repository/activity_repository_period_flags_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryGatingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| period read requests floors and elevation when granted | PORTED | ActivityRepositoryGatingTest.kt: `period read requests floors and elevation when granted` | — |
| period read omits floors and elevation when ungranted | DIVERGED | ActivityRepositoryTest.kt: `DAY activity metric progress uses raw full data for selected day graph` | Flags asserted false only on the raw-progress/direct daily-steps reads, not the period readDailySteps flags |
| period read forwards wheelchair pushes when asked and granted | PORTED | ActivityRepositoryGatingTest.kt: `period read forwards wheelchair pushes when asked and granted` | — |
| period read omits wheelchair pushes when the metric never asked | DIVERGED | ActivityRepositoryTest.kt: `DAY activity metric progress uses raw full data for selected day graph` | Asserts includeWheelchairPushes=false, but without the permission granted — the granted-yet-unasked distinction is untested |

## test/data/repository/activity_repository_workouts_with_metrics_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryGatingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| forwards both metrics when distance and speed are granted | PORTED | ActivityRepositoryGatingTest.kt: `loadWorkoutsWithMetrics forwards both metrics when distance and speed are granted` | — |
| degrades to null metrics when distance/speed are not granted | PORTED | ActivityRepositoryGatingTest.kt: `loadWorkoutsWithMetrics degrades to null metrics when distance and speed are not granted` | — |
| gates distance and speed independently | PORTED | ActivityRepositoryGatingTest.kt: `loadWorkoutsWithMetrics gates distance and speed independently` | — |
| skips the read entirely without the exercise permission | PORTED | ActivityRepositoryGatingTest.kt: `loadWorkoutsWithMetrics skips the read entirely without the exercise permission` | — |
| reads the local-day span of the requested range | PORTED | ActivityRepositoryGatingTest.kt: `loadWorkoutsWithMetrics reads the local-day span of the requested range` | — |

## test/data/repository/activity_repository_write_permissions_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepositoryWritePermissionsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a bare session asks only for exercise | PORTED | ActivityRepositoryWritePermissionsTest.kt: `a bare session asks only for exercise` | — |
| a recording with heart rate asks to write heart rate | PORTED | ActivityRepositoryWritePermissionsTest.kt: `a recording with heart rate asks to write heart rate` | — |
| each series is asked for only when it has samples | PORTED | ActivityRepositoryWritePermissionsTest.kt: `each series is asked for only when it has samples` | speed + steps-cadence absence asserted; the cycling-cadence write permission is an alias of WRITE_EXERCISE in the Health Connect client, so its absence is unassertable |
| a permission the device does not define is never demanded | N/A-BEHAVIOR | — | blocked on behavior decision - Kotlin has no unsupported-permission filter at all (no filterSupportedPermissions equivalent) |
| a device that defines the permission is still asked for it | PORTED | ActivityRepositoryWritePermissionsTest.kt: `a device that defines the permission is still asked for it` | — |

## test/data/repository/body_energy_baseline_cache_store_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BodyEnergyBaselineCacheStoreTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| missing entry returns null | PORTED | BodyEnergyBaselineCacheStoreTest.kt: `missing entry returns null` | — |
| baseline entry round-trips including nulls | PORTED | BodyEnergyBaselineCacheStoreTest.kt: `baseline entry round-trips including nulls` | — |
| a blank signature is not persisted | PORTED | BodyEnergyBaselineCacheStoreTest.kt: `a blank signature is not persisted` | — |
| purgeLegacyTimelineEntries > removes the retired timeline keys and nothing else | PORTED | BodyEnergyBaselineCacheStoreTest.kt: `purgeLegacyTimelineEntries removes the retired timeline keys and nothing else` | — |
| purgeLegacyTimelineEntries > runs once — a key written afterwards survives | PORTED | BodyEnergyBaselineCacheStoreTest.kt: `purgeLegacyTimelineEntries runs once - a key written afterwards survives` | — |

## test/data/repository/body_energy_chain_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BodyEnergyChainTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| continuity across midnight > a day opens where the stored previous day closed | PORTED | BodyEnergyChainTest.kt: `a day opens where the stored previous day closed` | — |
| continuity across midnight > the warm path costs no Health Connect read | PORTED | BodyEnergyChainTest.kt: `the warm path costs no Health Connect read beyond today` | — |
| continuity across midnight > a cold chain starts neutral, because there is nothing to carry | PORTED | BodyEnergyChainTest.kt: `a cold chain starts neutral, because there is nothing to carry` | — |
| continuity across midnight > a multi-day query threads each day into the next | PORTED | BodyEnergyChainTest.kt: `a multi-day query threads each day into the next` | — |
| the gap fill > a one-day gap is closed and the filled day is persisted | PORTED | BodyEnergyChainTest.kt: `a one-day gap is closed and the filled day is persisted` | — |
| the gap fill > a gap wider than the foreground bound is reported, not walked | PORTED | BodyEnergyChainTest.kt: `a gap wider than the foreground bound is reported, not walked` | — |
| the per-day signature > a stored day under a foreign signature is not used as an anchor | PORTED | BodyEnergyChainTest.kt: `a stored day under a foreign signature is not used as an anchor` | — |
| the per-day signature > a gain the watch learner nudged still seeds the next day | PORTED | BodyEnergyChainTest.kt: `a gain the watch learner nudged still seeds the next day` | — |
| the per-day signature > but editing the heart zones does break the chain | PORTED | BodyEnergyChainTest.kt: `but editing the heart zones does break the chain` | — |
| the forward ripple > recomputing a past day drops the days that followed it | PORTED | BodyEnergyChainTest.kt: `recomputing a past day drops the days that followed it` | — |
| the forward ripple > a recompute landing on the same score keeps the chain intact | PORTED | BodyEnergyChainTest.kt: `a recompute landing on the same score keeps the chain intact` | — |
| a day with no data > passes the seed through instead of resetting to 50 | PORTED | BodyEnergyChainTest.kt: `a day with no data passes the seed through instead of resetting to 50` | — |
| without a drift store (the alarm isolate) > falls back to the prefs seed mirror for the previous day | PORTED | BodyEnergyChainTest.kt: `without a store the chain falls back to the prefs seed mirror` | — |
| without a drift store (the alarm isolate) > ignores a mirror that is not for the immediately previous day | PORTED | BodyEnergyChainTest.kt: `a mirror that is not for the immediately previous day is ignored` | — |
| without a drift store (the alarm isolate) > the mirror only moves forward, so a backfill cannot rewind it | PORTED | BodyEnergyChainTest.kt: `the mirror only moves forward, so a backfill cannot rewind it` | — |
| settled days are served, not recomputed > a settled day is served from storage with no Health Connect read | PORTED | BodyEnergyChainTest.kt: `a settled day is served from storage with no Health Connect read` | — |
| settled days are served, not recomputed > a day still inside the settling window recomputes once it ages | PORTED | BodyEnergyChainTest.kt: `a day still inside the settling window recomputes once it ages` | — |
| settled days are served, not recomputed > today still follows the 15-minute rule | PORTED | BodyEnergyChainTest.kt: `today still follows the 15-minute rule` | — |
| settled days are served, not recomputed > a forced refresh still recomputes a settled day | PORTED | BodyEnergyChainTest.kt: `a forced refresh still recomputes a settled day` | — |
| settled days are served, not recomputed > a signature change still rebuilds a settled day | PORTED | BodyEnergyChainTest.kt: `a signature change still rebuilds a settled day` | — |
| settled days are served, not recomputed > a day whose buckets retention purged is recomputed, not served blank | PORTED | BodyEnergyChainTest.kt: `a day whose buckets retention purged is recomputed, not served blank` | — |
| an empty recompute never destroys a stored day > the stored timeline survives and is returned | PORTED | BodyEnergyChainTest.kt: `an empty recompute returns the stored timeline it could not replace` | — |
| an empty recompute never destroys a stored day > the days after it are not rippled away | PORTED | BodyEnergyChainTest.kt: `an empty recompute does not ripple the days after it away` | — |
| an empty recompute never destroys a stored day > a genuinely data-less day with nothing stored is still recorded | PORTED | BodyEnergyChainTest.kt: `a genuinely data-less day with nothing stored is still recorded` | — |
| the algorithm-change gain reset > runs on any load, not only when the chain sync happens to fire | PORTED | BodyEnergyChainTest.kt: `the gain reset runs on any load, not only when the chain sync fires` | Kotlin additionally asserts watchObservationCount reset |
| the algorithm-change gain reset > rewinds the watch fit watermark so the gains can actually relearn | PORTED | BodyEnergyChainTest.kt: `the reset rewinds the watch fit watermark so the gains can relearn` | — |
| the algorithm-change gain reset > rewinds the watermark on an install already at this algorithm version | PORTED | BodyEnergyChainTest.kt: `the watermark rewinds on an install already at this algorithm version` | — |
| the algorithm-change gain reset > and does not rewind it again once that epoch is recorded | PORTED | BodyEnergyChainTest.kt: `the watermark is not rewound again once that epoch is recorded` | — |
| the algorithm-change gain reset > rewinds the watermark even when there were no personal gains to reset | PORTED | BodyEnergyChainTest.kt: `the watermark rewinds even when there were no personal gains to reset` | — |
| the algorithm-change gain reset > leaves the manual heart zones and profile alone | PORTED | BodyEnergyChainTest.kt: `the reset leaves the manual heart zones alone` | Same assertions (zones + gain); Flutter name mentions profile but asserts none |
| the algorithm-change gain reset > does not undo a gain learned after it ran | PORTED | BodyEnergyChainTest.kt: `the reset does not undo a gain learned after it ran` | — |

## test/data/repository/body_energy_repository_impl_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BodyEnergyRepositoryTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a fresh cached timeline is served without recomputing | PORTED | BodyEnergyRepositoryTest.kt: `a fresh cached timeline is served without recomputing` | — |
| a stale timeline recomputes but reuses the fresh baseline | PORTED | BodyEnergyRepositoryTest.kt: `a stale timeline recomputes but reuses the fresh baseline` | — |
| a forced refresh recomputes even within the freshness window | PORTED | BodyEnergyRepositoryTest.kt: `a forced refresh recomputes even within the freshness window` | — |

## test/data/repository/body_profile_resolution_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/BodyRepositoryTest.kt (resolveBodyProfile cases)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a measured weight beats the declared one | PORTED | BodyRepositoryTest.kt: `a measured weight beats the declared one` | — |
| the declared value survives when nothing is recorded | PORTED | BodyRepositoryTest.kt: `the declared value survives when nothing is recorded` | — |
| a missing permission falls back rather than blanking the value | PORTED | BodyRepositoryTest.kt: `a missing permission falls back rather than blanking the value` | — |
| the rest of the profile is untouched | PORTED | BodyRepositoryTest.kt: `the rest of the profile is untouched by resolution` | — |
| a measured value out of range is normalised, not trusted blindly | PORTED | BodyRepositoryTest.kt: `a measured value out of range is normalised, not trusted blindly` | — |

## test/data/repository/dashboard/dashboard_data_loader_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/DashboardDataLoaderTest.kt + app/src/test/kotlin/tech/mmarca/openvitals/data/repository/dashboard/DashboardDataLoaderParityTest.kt (active caffeine, permission set, device-support gating)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| assembles granted metrics and reports missing permissions | PORTED | DashboardDataLoaderTest.kt: `loadDashboard reports missing permissions only for visible metrics` | loadedMetrics assertion lives in `loadDashboard skips hidden dashboard metrics` |
| overnight vitals are read from the night window, not the day | PORTED | DashboardDataLoaderTest.kt: `overnight vitals read back to the night-window start` | Kotlin is stronger: five overnight metrics plus window-honoring fakes and boundary tests |
| active caffeine > morning carryover from last night is reported for today | PORTED | DashboardDataLoaderParityTest.kt: `morning carryover from last night is reported for today` | drives the yesterday+today point-in-time read and asserts a positive active figure |
| active caffeine > matches the caffeine screen's currentMg for the same inputs | PORTED | DashboardDataLoaderParityTest.kt: `active caffeine matches the caffeine screen's currentMg for the same inputs` | tile vs `CaffeineInsightCalculator.build(...).currentMg`, same entries, 0.1 mg tolerance |
| active caffeine > a past day keeps intake semantics: no PK read at all | PORTED | DashboardDataLoaderParityTest.kt: `a past day keeps intake semantics - no point-in-time read at all` | `date == LocalDate.now()` gate: no nutrition-entry read, null activeCaffeineMg |
| active caffeine > hidden metric or missing permission skips the read | PORTED | DashboardDataLoaderParityTest.kt: `a hidden metric or a missing permission skips the caffeine read` | both halves now driven at the caffeine read itself, not only generically for steps/distance |
| active caffeine > a throwing caffeine read nulls the field, not the dashboard | PORTED | DashboardDataLoaderParityTest.kt: `a throwing caffeine read nulls the field, not the dashboard` | the caffeine read throws; steps still land |
| active caffeine > mergeLoaded carries activeCaffeineMg across the two-pass load | PORTED | DashboardDataLoaderParityTest.kt: `mergeLoaded carries activeCaffeineMg across the two-pass load` | — |
| omits permissions the installed provider cannot grant | PORTED | DashboardDataLoaderParityTest.kt: `omits permissions the installed provider cannot grant` | the loader now intersects the dashboard's permission set with `managedPermissions`, as Flutter does |
| supportedMetrics drops metrics the provider cannot serve | PORTED | DashboardDataLoaderParityTest.kt: `supportedMetrics drops metrics the provider cannot serve` | `DashboardData.supportedMetrics` added; computed for every metric, not just the queried ones |
| a multi-permission metric needs all of its permissions supported | PORTED | DashboardDataLoaderParityTest.kt: `a multi-permission metric needs all of its permissions supported` | dropping height alone unsupports BMI and FFMI |
| returns empty granted set when Health Connect is unavailable | PORTED | DashboardDataLoaderParityTest.kt: `returns an empty granted set when Health Connect is unavailable` | availability gate short-circuits: no `grantedPermissions()` call, zero steps, permission reported missing |
| body energy timeline > populates the timeline when set up and heart-rate is granted | DIVERGED | DashboardViewModelTest.kt: `body energy populates the timeline when the widget is on the dashboard` | deliberate divergence: Kotlin loads the timeline from the ViewModel after the day settles, gated on the BODY_ENERGY widget being on the dashboard rather than on calibration + the heart-rate grant |
| body energy timeline > skips the load when calibration is not set up | DIVERGED | DashboardViewModelTest.kt: `body energy skips the load when the widget is not on the dashboard` | same divergence: calibration gates the tile's rendering (`isNotSetUp`), not the load |
| body energy timeline > skips the load when heart-rate read is not granted | DIVERGED | DashboardViewModelTest.kt: `body energy leaves the day alone when the timeline load fails` | same divergence: an ungranted heart rate surfaces as a failing/empty timeline load, which must not become a dashboard error |
| bounds how many metric reads run at once | MISSING | — | blocked on behavior decision: Kotlin's loader fans out with unbounded `async` (no semaphore); a bound is a prod change nobody has taken |

## test/data/repository/heart_repository_impl_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/HeartRepositoryTest.kt (partial)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| loadHeartRateSamplesInstant > asks for exactly the window it was given | DIVERGED | HeartRepositoryTest.kt: `instant range includes samples from a heart rate series starting before the workout` | blocked on behavior decision: opposite contract — Kotlin still widens the read by a 1 h look-back (`start.minus(HeartRateSeriesLookback)`) while Flutter moved that native-side and asserts no look-back |
| loadHeartRateSamplesInstant > returns the samples the native reader found | DIVERGED | HeartRepositoryTest.kt: `instant range includes samples from a heart rate series starting before the workout` | blocked on behavior decision: Kotlin asserts window-clipped, sorted samples from the widened read rather than a pass-through |
| loadHeartRateSamplesInstant > returns empty for an inverted or empty window | PORTED | HeartRepositoryTest.kt: `instant range returns empty for an inverted or empty window` | — |
| loadHeartRateSamplesInstant > returns empty without the heart-rate permission | PORTED | HeartRepositoryTest.kt: `instant range returns empty without the heart-rate permission` | — |

## test/data/repository/run_catching_test.dart
Kotlin counterpart: none (the Kotlin port dropped the Dart Result/AppFailure wrapper; repositories throw and view models map via toScreenError, covered in core/presentation/ScreenErrorTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| wraps a successful body in Ok | N/A-FRAMEWORK | — | Dart Result-type architecture; Kotlin uses plain returns/exceptions |
| maps MissingHealthPermissionException to PermissionFailure | N/A-FRAMEWORK | — | Kotlin throws SecurityException and maps at the presentation layer (ScreenErrorTest.kt) without a permission-specific failure type |
| maps any other exception to UnexpectedFailure | N/A-FRAMEWORK | — | Analogous mapping covered by ScreenErrorTest.kt `toScreenError uses throwable message when present` |
| catches Errors too, matching the pre-Result bare catch in notifiers | N/A-FRAMEWORK | — | Dart Error-vs-Exception distinction has no JVM counterpart; Kotlin instead pins CancellationException rethrow |

## test/data/repository/sleep_repository_impl_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/SleepRepositoryTest.kt (partial; merged-id reconstruction moved to healthconnect/SleepHealthReader.kt:135)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SleepRepositoryImpl.loadSleepSession > reconstructs a merged night from its component records | DIVERGED | domain/model/SleepSessionMergingTest.kt: `mergedSleepSessionComponentIds returns encoded raw ids` | Component-id codec and merge are tested at domain level only; the SleepHealthReader.readSleepSession reconstruction path itself is untested |
| SleepRepositoryImpl.loadSleepSession > reads a plain record id straight through | PORTED | SleepRepositoryTest.kt: `loadSleepSession reads a plain record id straight through` | — |
| SleepRepositoryImpl.loadSleepSession > is not-found when every component record has since vanished | PORTED | SleepRepositoryTest.kt: `loadSleepSession is not-found when every component record has since vanished` | drives the real SleepHealthReader reconstruction over an empty fake client |

## test/data/repository/vitals_repository_impl_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/repository/VitalsRepositoryTest.kt (+ data/sync/VitalsHistorySyncServiceTest.kt for the cache patch layer)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| loadVitalsPeriod (ALL) > fans the seven vitals reads out concurrently, not serially | DIVERGED | VitalsRepositoryTest.kt: `slow metric does not block the others` | Proves parallel isolation but never asserts the seven-way high-water mark |
| loadVitalsPeriod (ALL) > a stuck read times out into a failure instead of hanging forever | DIVERGED | VitalsRepositoryTest.kt: `daily read that blows its budget lands in timedOutMetrics and stays empty` | Only the per-metric budget is tested; no whole-load budget surfacing a retryable failure |
| loadVitalsPeriod (ALL) > a metric too large to read degrades to empty and is flagged, not fatal | PORTED | VitalsRepositoryTest.kt: `daily read that blows its budget lands in timedOutMetrics and stays empty` | — |
| loadVitalsPeriod (ALL) > a synced metric reads daily points from the cache, not live | PORTED | VitalsRepositoryTest.kt: `cached daily points are served without hitting Health Connect` | — |
| daily-cache write-through > a write refreshes the affected day in the cache | DIVERGED | VitalsHistorySyncServiceTest.kt: `patchDays recomputes the given day when a cursor exists` | Recompute tested at sync-service layer; repository write-to-patch wiring untested |
| daily-cache write-through > a delete that empties the day removes its cached row | DIVERGED | VitalsHistorySyncServiceTest.kt: `a day whose recompute comes back empty is deleted` | Empty-recompute deletion tested via incremental sync, not the repository delete path |
| daily-cache write-through > a delete leaving other readings recomputes the day | DIVERGED | VitalsHistorySyncServiceTest.kt: `patchDays recomputes the given day when a cursor exists` | Same layer gap: not driven through deleteVitalsMeasurementEntry |
| daily-cache write-through > an edit across midnight recomputes both the old and new day | PORTED | VitalsRepositoryTest.kt: `an edit across midnight recomputes both the old and new day` | — |
| daily-cache write-through > a blood-pressure write carries diastolic into secondarySum | PORTED | VitalsRepositoryTest.kt: `a blood-pressure write carries diastolic into secondarySum` | — |
| daily-cache write-through > a write is not cached until the metric has had its first sync | PORTED | VitalsHistorySyncServiceTest.kt: `patchDays is a no-op without a cursor` | Guard lives in patchDays in Kotlin; same logic and assertion |
| daily-cache write-through > with no cache wired, a write still succeeds | PORTED | VitalsRepositoryTest.kt: `with no cache wired, a write still succeeds` | — |
| daily-cache write-through > a cache-patch failure never fails the write | PORTED | VitalsRepositoryTest.kt: `a cache-patch failure never fails the write` | — |

## test/data/repository/write_signal_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| writing a body measurement announces the body domain | N/A-FRAMEWORK | — | Riverpod refresh-coordinator/DataChangeSink wiring; Kotlin app has no data-change signal subsystem (no DataDomain/RefreshSignal equivalent) |
| logging a drink announces nutrition and caffeine as well as hydration | N/A-FRAMEWORK | — | Same |
| a vitals write announces the vitals domain | N/A-FRAMEWORK | — | Same |
| a write refused for a missing permission announces nothing | N/A-FRAMEWORK | — | Same |
| several writes in a row collapse into one signal | N/A-FRAMEWORK | — | Debounce is a property of the Riverpod coordinator |
| the default sink lets a repository write with no container at all | N/A-FRAMEWORK | — | Background-isolate/no-container shape is Flutter-specific |

## test/data/source/health/health_connect_native_data_source_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/ (HealthConnectReadMappingTest.kt, HealthConnectAggregateReadTest.kt, HealthConnectPermissionServiceTest.kt, ActivityHealthReaderTest.kt, FixtureReaderTest.kt, SwallowingRecordTest.kt, HealthConnectAvailabilityServiceTest.kt) + domain/model/ActivityBackfillTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| availability > maps SDK status ints and caches the result | DIVERGED | HealthConnectAvailabilityServiceTest.kt | status mapping covered via install-state scenarios; caching of the result not asserted |
| availability > resolveFeatureFlags reads optional-feature availability | PORTED | HealthConnectPermissionServiceTest.kt: `optional-feature availability is read per feature`, `planned exercise permissions appear once the provider reports the feature` | Kotlin has no resolve step — the getters ask the provider on demand — so the flags are read straight off the service |
| availability > getFeatureStatus surfaces the tri-state per feature | N/A-FRAMEWORK | — | pigeon FeatureStatusMsg tri-state; Kotlin consumes SDK status ints directly |
| availability > UNKNOWN feature status resolves the flag to unavailable | PORTED | HealthConnectPermissionServiceTest.kt: `a feature status this SDK has no name for resolves to unavailable` | the SDK has no UNKNOWN constant, so the case is fed a status int it has no name for; the gate is `== FEATURE_STATUS_AVAILABLE` either way |
| availability > resolveSupportedPermissions drops permissions the provider does not recognize from every set | MISSING | — | blocked on behavior decision: Kotlin has no supported-permission filtering seam at all — nothing resolves or filters against what the provider recognizes, so there is no behavior to assert |
| permissions > grantedPermissions returns the plugin-reported subset | N/A-FRAMEWORK | — | bridge passthrough |
| permissions > requestPermissions returns false for an empty set | MISSING | — | blocked on behavior decision: Kotlin has no `requestPermissions` function — callers launch `permissionContract()` (an ActivityResultContract) directly, so there is no guard to test |
| permissions > requestPermissions never forwards an unsupported permission | MISSING | — | blocked on behavior decision: no request function and no supported-permission filter in Kotlin (see above) |
| permissions > a request of nothing BUT unsupported permissions never reaches the plugin | MISSING | — | blocked on behavior decision: no request function and no supported-permission filter in Kotlin (see above) |
| reads > ExerciseSession msg maps to ExerciseData with segments/laps/route | DIVERGED | FixtureReaderTest.kt: `sessions come from more than one writer...` / provenance test | real sessions read incl. source/provenance; segments/laps/route field mapping unasserted |
| reads > readExerciseSessionsWithMetrics maps aggregate distance/speed through | PORTED | HealthConnectReadMappingTest.kt: `readExerciseSessionsWithMetrics maps aggregate distance and speed through` | — |
| reads > readExerciseSessionsWithMetrics degrades to null without the perms | PORTED | HealthConnectReadMappingTest.kt: `readExerciseSessionsWithMetrics degrades to null metrics without the permissions` | — |
| reads > readExerciseSessionsWithMetrics backfills distance from the route | PORTED | ActivityBackfillTest.kt: `route backfill fills missing distance and elevation`, `route backfill replaces empty zero summaries with route values` | — |
| reads > HeartRate raw samples map from typed msgs (short range) | DIVERGED | FixtureReaderTest.kt: `but the real reader finds it anyway`; SwallowingRecordTest.kt | window/flattening logic tested against the real corpus; bpm/source field mapping not directly asserted |
| reads > BloodPressure entries map systolic/diastolic and ownership | PORTED | HealthConnectReadMappingTest.kt: `BloodPressure entries map systolic, diastolic and ownership` | Kotlin additionally pins a foreign writer's reading as readable-but-not-ours |
| reads > readSteps / readDistanceMeters use the aggregate API | PORTED | HealthConnectAggregateReadTest.kt: `readSteps, readDistanceMeters and readFloorsClimbed use the aggregate API` | — |
| reads > readDailySteps slices 24h duration buckets over an instant range | PORTED | HealthConnectAggregateReadTest.kt: `readDailySteps slices a day bucket over the local instant range` | Dart asserts the query it issued; Kotlin issues it with typed AggregateMetrics, so the case asserts the bucket that comes back |
| reads > readDailySteps chunks a multi-year range into <=366-day queries | DIVERGED | ActivityHealthReaderTest.kt: `dailyStepDateChunks splits long ranges into inclusive chunks` | helper tiling tested at small scale; 366-day default, per-query span cap, metric set not asserted |
| reads > readDailySteps dates a drifted bucket by its midpoint, not its start (DST fall-back day) | PORTED | HealthConnectAggregateReadTest.kt: `a drifted bucket is dated by its midpoint, not its start (the fall-back day)` | — |
| reads > readDailySteps keeps the spring-forward day a start-dated bucket would skip | PORTED | HealthConnectAggregateReadTest.kt: `the midpoint keeps the spring-forward day a start-dated bucket would skip` | — |
| reads > readDailySteps sums a clipped tail bucket onto its date instead of overwriting | PORTED | HealthConnectDstBucketTest: `readDailySteps sums a clipped tail bucket onto its date instead of duplicating it` | Was filed as a behavior divergence; it was a defect. All six daily reads now fold same-date buckets through `byLocalDate` |
| reads > readDailyHydration sums same-date buckets instead of keeping the last | PORTED | HealthConnectDstBucketTest: `readDailyHydration sums same-date buckets instead of keeping the last` | The `associate {}` let a clipped one-hour DST tail bucket overwrite the whole day; now summed |
| reads > readDailySteps maps floors when requested | PORTED | HealthConnectAggregateReadTest.kt: `readDailySteps maps floors when requested and leaves elevation null when not` | — |
| reads > single Sleep session maps stages from a typed msg | PORTED | HealthConnectReadMappingTest.kt: `a single Sleep session maps its stages` | — |
| reads > session metrics carry average power, asked for by wire name | DIVERGED | HealthConnectReadMappingTest.kt: `session metrics carry average power` | POWER_AVG aggregate path ported end to end; the wire-name half is pigeon-specific — Kotlin asks with the typed AggregateMetric, so there is no name to mistype |
| reads > an Exercise session carries the record provenance across the bridge | DIVERGED | FixtureReaderTest.kt: `every record keeps the provenance the Pigeon messages kept dropping` | asserts non-null recordingMethod/lastModifiedTime/zone offset; exact values and clientRecordVersion not |
| reads > a Sleep session carries the record provenance the detail screen shows | PORTED | HealthConnectReadMappingTest.kt: `a Sleep session carries the record provenance the detail screen shows` | — |
| reads > a session with no zone offsets keeps them null, not zero | DIVERGED | HealthConnectReadMappingTest.kt: `a session with no zone offsets keeps them null, not zero` | the null-vs-UTC distinction is pinned for both zone offsets; Dart's companion `lastModifiedTime isNull` has no counterpart — Health Connect's Kotlin Metadata carries a non-null lastModifiedTime the provider stamps |
| reads > Weight entries map from typed msgs and preserve ownership | PORTED | HealthConnectReadMappingTest.kt: `Weight entries map from records and preserve ownership` | — |
| writes > writeHydrationEntry forwards a typed request | N/A-FRAMEWORK | — | asserts pigeon forwarding only; record build lives in Kotlin reader |
| writes > writeBodyMeasurementEntry forwards a typed request | N/A-FRAMEWORK | — | pigeon forwarding only |
| writes > writeVitalsMeasurementEntry forwards a typed request | N/A-FRAMEWORK | — | pigeon forwarding only |
| writes > writeNutritionEntry forwards a typed request keyed by storageName | N/A-FRAMEWORK | — | storageName keying is the Dart↔Kotlin wire contract |
| writes > writeActivityEntry forwards a typed ExerciseSession request | N/A-FRAMEWORK | — | forwarding only; the segment build itself is tested in ActivityHealthReaderTest |
| writes > deleteHydrationEntry propagates provider failures (not swallowed) | PORTED | HealthConnectReadMappingTest.kt: `deleteHydrationEntry propagates provider failures rather than swallowing them` | — |
| writes > deleteHydrationEntry still returns null for no paired clientRecordId | PORTED | HealthConnectReadMappingTest.kt: `deleteHydrationEntry still returns null for a record with no clientRecordId` | the null-means-success contract is now asserted at the reader; HydrationRepositoryTest still covers the pairing behavior above it |
| writes > deleteNutritionEntry propagates provider failures (not swallowed) | PORTED | HealthConnectReadMappingTest.kt: `deleteNutritionEntry propagates provider failures rather than swallowing them` | — |
| writes > deleteActivityEntry delegates by id | N/A-FRAMEWORK | — | pure passthrough |
| apple health import > insertImportedRecords converts every record to canonical JSON | N/A-FRAMEWORK | — | pigeon ImportRecordMsg codec; Kotlin import writes records directly (AppleHealthImportServiceTest covers its own layer) |
| apple health import > findMatchingImportedClientRecordIds maps targetType and filters | PORTED | ImportedClientRecordIdLookupTest.kt | the targetType half is wire-only, but the FILTERING half was Kotlin logic with a bug: a hardcoded `apple_health_` prefix guard discarded every `csv_` id, so the CSV importer could never find a duplicate. Guard removed; the wanted-set is the authority |
| readRawActivityProgress > accumulates each hourly bucket into a running total | DIVERGED | HealthConnectAggregateReadTest.kt: `readRawActivityProgress accumulates each contribution into a running total` | the running total is ported exactly; Kotlin accumulates raw RECORD contributions grouped by end time rather than hourly aggregate buckets, so there are no buckets to count |
| readRawActivityProgress > a metric the device never reports stays null, not a zero line | DIVERGED | HealthConnectAggregateReadTest.kt: `an unrequested metric stays null, while a requested one reads zero` | blocked on behavior decision: Kotlin's nullness follows what was REQUESTED (the granted permissions), not what came back — a requested metric the device never wrote reads 0, not null. Pinned as it stands |
| readRawActivityProgress > a metric stays non-null from the bucket it first appears in | DIVERGED | HealthConnectAggregateReadTest.kt: `a metric's running total carries forward through contributions that had none` | the carry-forward half is ported; the first half cannot hold, since a requested metric is non-null from point one (see the row above) |
| readRawActivityProgress > asks for hourly buckets across the whole of a past day | DIVERGED | HealthConnectAggregateReadTest.kt: `a past day is read across the whole of it, and nothing outside it` | the window is ported (local midnight to the next, with neither neighbouring day leaking in); Kotlin issues no hourly bucket query, so the 60-minute slicer has no counterpart |
| readRawActivityProgress > today stops at now rather than running on to midnight | PORTED | HealthConnectAggregateReadTest.kt: `today stops at now rather than running on to midnight` | asserted through the observable half — a record stamped for later today stays off the line |
| readRawActivityProgress > no buckets means no points | PORTED | HealthConnectAggregateReadTest.kt: `no contributions means no points` | — |
| elevation + wheelchair aggregates > return the aggregated value | PORTED | HealthConnectAggregateReadTest.kt: `elevation and wheelchair aggregates return the aggregated value` | — |
| elevation + wheelchair aggregates > stay null when the device records neither | DIVERGED | HealthConnectAggregateReadTest.kt: `elevation and wheelchair read zero, not null, when the device records neither` | blocked on behavior decision: the Kotlin day readers fall back to 0.0/0L, so a day the device never measured is indistinguishable from one it measured as zero. The divergence is now pinned rather than untested |
| elevation + wheelchair aggregates > a wheelchair count is rounded to whole pushes | N/A-FRAMEWORK | — | rounding is a Dart double-bridge artifact; Kotlin aggregate returns Long natively |
| readActivityCadenceSamples > maps cycling and steps samples to their own kinds | PORTED | HealthConnectReadMappingTest.kt: `readActivityCadenceSamples maps cycling and steps samples to their own kinds` | — |
| readActivityCadenceSamples > is empty when the device records no cadence | PORTED | HealthConnectReadMappingTest.kt: `readActivityCadenceSamples is empty when the device records no cadence` | — |
| planned exercise sessions > reads sessions with their blocks, steps and completion goals | DIVERGED | ActivityHealthReaderTest.kt: `planned exercise blocks preserve set repetitions and rest duration` | reps+duration completions mapped; manual/unknown kinds and session-level fields not |
| planned exercise sessions > a write round-trips every completion kind | DIVERGED | ActivityHealthReaderTest.kt: `planned exercise step data writes repetitions goal` | only the repetitions goal write tested; duration/manual not |
| planned exercise sessions > an unsupported provider reads empty and refuses to write | MISSING | — | blocked on behavior decision: Kotlin's ActivityHealthReader is ungated — the plannedExercise gate lives one layer up in ActivityRepository.kt:422/435, outside this cluster. The permission half is pinned in HealthConnectPermissionServiceTest.kt `optional-feature availability is read per feature` |

## test/data/source/health/health_permissions_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectPermissionServiceTest.kt (Kotlin folds Dart's HealthPermissionService and its HealthConnectFeatureFlags into one class: a Dart flag becomes a `getFeatureStatus` answer, the opt-in becomes the `mindfulnessIntegrationEnabled` lambda)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| phased permission sets > PERMISSION_SET_VERSION is 3 | DIVERGED | HealthConnectPermissionServiceTest.kt: `PERMISSION_SET_VERSION is pinned` | same intent (a bump is a deliberate act), different value: the two apps bumped on different schedules and Kotlin's constant is 2 |
| phased permission sets > phase1 == core == steps/distance/exercise/sleep reads | PORTED | HealthConnectPermissionServiceTest.kt: `phase1 == core == steps, distance, exercise and sleep reads` | — |
| phased permission sets > phase2 covers heart, body, activity-extras and nutrition/hydration | PORTED | HealthConnectPermissionServiceTest.kt: `phase2 covers heart, body, activity-extras and nutrition-hydration` | Kotlin also asserts the activity-extras containment the Dart title names but its body omits |
| phased permission sets > phase3 == vitals reads; phase4 == cycle reads | PORTED | HealthConnectPermissionServiceTest.kt: `phase3 == vitals reads, phase4 == cycle reads` | — |
| phased permission sets > manual-only == route permissions and drives grant mode | PORTED | HealthConnectPermissionServiceTest.kt: `manual-only == route permissions, and drives the grant mode` | Kotlin additionally pins the READ_EXERCISE_ROUTES wire string, which is a private const in the service |
| phased permission sets > managed permissions include reads, writes and route | PORTED | HealthConnectPermissionServiceTest.kt: `managed permissions include reads, writes and the route` | — |
| feature gating > mindfulness excluded from phase2 / requestable writes when unavailable | PORTED | HealthConnectPermissionServiceTest.kt: `mindfulness is excluded from phase2 and the requestable writes when unavailable` | — |
| feature gating > mindfulness included when the feature flag is set | PORTED | HealthConnectPermissionServiceTest.kt: `mindfulness is included when the provider reports the feature available` | — |
| feature gating > mindfulness permissions are empty when the provider lacks it | PORTED | HealthConnectPermissionServiceTest.kt: `mindfulness permissions are empty when the provider lacks the feature` | — |
| feature gating > an unavailable mindfulness leaks into NO permission set | PORTED | HealthConnectPermissionServiceTest.kt: `an unavailable mindfulness leaks into NO permission set` | the 1.9.0 regression is now pinned on its own side: eight composed sets plus the onboarding catalog, which drops the category rather than offering an empty row |
| feature gating > an AVAILABLE mindfulness still stays out of the required set | PORTED | HealthConnectPermissionServiceTest.kt: `an AVAILABLE mindfulness still stays out of the required set` | asserted against both required sets Kotlin has — minimumOnboardingPermissions and the catalog's requiredPermissions |
| feature gating > the device answer and the opt-in are separate flags | DIVERGED | HealthConnectPermissionServiceTest.kt: `the device answer and the opt-in are folded into one flag` | blocked on behavior decision: isMindfulnessSessionAvailable() folds the opt-in in FIRST and the catalog's mindfulnessSupportedByDevice reports that same folded answer, so a supporting device whose user has not opted in is indistinguishable from an unsupporting one. The fold is pinned instead of the Dart split |
| feature gating > cycle tracking stays out of the required set, both directions | PORTED | HealthConnectPermissionServiceTest.kt: `cycle tracking stays out of the required set, both directions` | Kotlin has no named cycleWritePermissions set (the cycle writes live inline in dataImportWritePermissions), so the test spells the set out |
| feature gating > skin temperature gated on the feature flag | PORTED | HealthConnectPermissionServiceTest.kt: `skin temperature is gated on the feature flag` | — |

## test/data/source/health/import_record_mapper_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/store/SyncRecordCodecTest.kt (different layer — sync codec, not a pigeon mapper)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| round-trip > StepsRecord survives a round trip | N/A-FRAMEWORK | — | pigeon ImportRecordMsg mapper; Kotlin has no msg layer (same family round-trips at sync-codec layer in SyncRecordCodecTest) |
| round-trip > DistanceRecord survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > Weight survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > HeartRateSeries survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > Sleep survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > Nutrition survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > Workout survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > CervicalMucusRecord survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > BloodPressure survives a round trip | PORTED | SyncRecordCodecFieldLossTest.kt | the pigeon mapper is Flutter-only, but the invariant it protects — every field written comes back — was broken at the equivalent Kotlin layer: the sync codec encoded bodyPosition and measurementLocation and decoded neither, so posture and cuff site were erased on the receiving phone. Now decoded, with the same unknown-value guard the peer-supplied skin-temperature location uses |
| round-trip > OvulationTestRecord survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > Hydration survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > TotalCaloriesBurned survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > PowerSeries survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > SkinTemperature survives a round trip | N/A-FRAMEWORK | — | — |
| round-trip > MenstruationPeriodRecord survives a round trip | N/A-FRAMEWORK | — | — |
| out-of-range peer data > an out-of-range completionKind maps to unknown, not RangeError | PORTED | SyncRecordCodecTest.kt: `an out-of-range completionKind maps to unknown, not RangeError` | decodes a payload whose ck is 99 and asserts every step degrades to UnknownGoal |
| out-of-range peer data > every valid completionKind ordinal maps through unchanged | DIVERGED | SyncRecordCodecTest.kt: `every sample record survives an encode-decode round trip` | reps+duration goals round-trip in the planned-exercise sample; manual/unknown kinds not exercised |

## test/data/source/health/mindfulness_opt_in_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectPermissionServiceTest.kt (the gate is healthconnect/MindfulnessIntegrationGate.kt feeding HealthConnectPermissionService.isMindfulnessSessionAvailable)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| with the integration off, mindfulness is never asked for | PORTED | HealthConnectPermissionServiceTest.kt: `with the integration off, mindfulness is never asked for` | — |
| with it on, and a device that supports it, we ask as before | PORTED | HealthConnectPermissionServiceTest.kt: `with it on, and a device that supports it, we ask as before` | — |
| turning it off costs mindfulness and nothing else | PORTED | HealthConnectPermissionServiceTest.kt: `turning it off costs mindfulness and nothing else` | — |
| fold into device answer > a device that says YES is still refused while the user has not | DIVERGED | HealthConnectPermissionServiceTest.kt: `a device that says YES is still refused while the user has not` | the refusal is ported exactly, along with Dart's "only mindfulness is withheld" check via skin temperature; Dart's companion `mindfulnessSupportedByDevice isTrue` cannot hold — see the folded-flag row above |
| fold into device answer > a device that says NO is not offered the opt-in at all | PORTED | HealthConnectPermissionServiceTest.kt: `a device that says NO is not offered the opt-in at all` | both flags read false here, so the fold makes no difference to this case |
| fold into device answer > both halves say yes, and the feature comes back | PORTED | HealthConnectPermissionServiceTest.kt: `both halves say yes, and the feature comes back` | — |
| fold into device answer > the user says yes but the device does not — still no | PORTED | HealthConnectPermissionServiceTest.kt: `the user says yes but the device does not, still no` | — |

## test/data/source/sync/bluetooth_sync_service_test.dart
Kotlin counterpart: none direct (transport is features/devicesync/bluetooth/, socket-based)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a disconnect event closes the transport inbound stream | N/A-FRAMEWORK | — | pigeon-event→Dart-stream plumbing; the session-unblocks-on-drop semantic is covered by SyncSessionTest `a dropped transport ends the session as an abort` |
| a connectFailed event also closes the inbound stream | N/A-FRAMEWORK | — | same |

## test/data/source/sync/device_sync_report_store_test.dart
Kotlin counterpart: none (buildSyncReportText in protocol/SyncReport.kt and store/DeviceSyncReportStore.kt exist, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildSyncReportText > renders the summary and per-type lines for a completed sync | PORTED | DeviceSyncReportStoreTest.kt: `renders the summary and per-type lines for a completed sync` | — |
| buildSyncReportText > renders the abort reason for an aborted sync | PORTED | DeviceSyncReportStoreTest.kt: `renders the abort reason for an aborted sync` | — |
| DeviceSyncReportStore > round-trips a report through a file | PORTED | DeviceSyncReportStoreTest.kt: `round-trips a report through a file` | — |

## test/data/source/sync/health_connect_sync_store_test.dart
Kotlin counterpart: none (features/devicesync/store/HealthConnectSyncStore.kt exists, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| readItems keys each record by its content fingerprint | PORTED | HealthConnectSyncStoreTest.kt: `readItems keys each record by its content fingerprint` | — |
| writeItems reconstructs typed records under the fingerprint id | PORTED | HealthConnectSyncStoreTest.kt: `writeItems reconstructs typed records under the fingerprint id` | — |
| writing the same items twice upserts rather than duplicating | PORTED | HealthConnectSyncStoreTest.kt: `writing the same items twice upserts rather than duplicating` | — |
| writeItems ignores a peer-chosen key and writes under the content fingerprint | PORTED | HealthConnectSyncStoreTest.kt: `writeItems ignores a peer-chosen key and writes under the content fingerprint` | — |
| writeItems returns the keys that actually landed | PORTED | HealthConnectSyncStoreTest.kt: `writeItems returns the keys that actually landed` | — |
| writeItems excludes a rejected type from its written-keys result | PORTED | HealthConnectSyncStoreTest.kt: `writeItems excludes a rejected type from its written-keys result` | — |

## test/data/source/sync/import_record_sync_codec_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/store/SyncRecordCodecTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| round-trip > StepsRecord survives a round trip | PORTED | SyncRecordCodecTest.kt: `every sample record survives an encode-decode round trip` | Kotlin loops the same families in one test (plus Mindfulness) |
| round-trip > Weight survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > HeartRateSeries survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > Sleep survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > Nutrition survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > Workout survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > CervicalMucusRecord survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > BloodPressure survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > TotalCaloriesBurned survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > PowerSeries survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > StepsCadenceSeries survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > CyclingPedalingCadenceSeries survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > SkinTemperature survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > MenstruationPeriodRecord survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| round-trip > PlannedExerciseSessionRecord survives a round trip | PORTED | SyncRecordCodecTest.kt: same | — |
| fingerprint > is stable and prefixed sync_ | PORTED | SyncRecordCodecTest.kt: `fingerprint is stable and prefixed sync_` | — |
| fingerprint > differs when identifying content differs | PORTED | SyncRecordCodecTest.kt: `fingerprint differs when identifying content differs` | — |
| fingerprint > ignores the current clientRecordId (content-only) | PORTED | SyncRecordCodecTest.kt: `fingerprint ignores the current clientRecordId (content-only)` | — |
| fingerprint > a whole-second instant has no trailing .000 in its parts | PORTED | SyncRecordCodecTest.kt: `fingerprint is stable and prefixed sync_` | Flutter case only asserts self-stability, subsumed; Kotlin adds a stronger unit-quantization case |

## test/data/source/sync/sync_frame_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/protocol/SyncFrameTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SyncFrame.encode > lays out big-endian length, type byte, then payload | PORTED | SyncFrameTest.kt: `encode lays out big-endian length, type byte, then payload` | — |
| SyncFrame.encode > encodes an empty payload as a 5-byte header | PORTED | SyncFrameTest.kt: `encode writes an empty payload as a 5-byte header` | — |
| SyncFrameReader > round-trips a single frame | PORTED | SyncFrameTest.kt: `round-trips a single frame` | — |
| SyncFrameReader > reassembles a frame split across many chunks | PORTED | SyncFrameTest.kt: `reassembles a frame split across many chunks` | — |
| SyncFrameReader > splits multiple frames coalesced into one chunk | PORTED | SyncFrameTest.kt: `splits multiple frames coalesced into one chunk` | — |
| SyncFrameReader > holds a partial trailing frame until the rest arrives | PORTED | SyncFrameTest.kt: `holds a partial trailing frame until the rest arrives` | — |
| SyncFrameReader > rejects an unknown frame type byte | PORTED | SyncFrameTest.kt: `rejects an unknown frame type byte` | — |
| SyncFrameReader > rejects an oversized length prefix | PORTED | SyncFrameTest.kt: `rejects an oversized length prefix` | — |

## test/data/source/sync/sync_pairing_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/protocol/SyncPairingTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| generatePairingCode > is always six digits, zero-padded | PORTED | SyncPairingTest.kt: `pairing code is always six digits, zero-padded` | — |
| generateSyncNonce > returns 32 bytes | PORTED | SyncPairingTest.kt: `nonce is 32 bytes` | — |
| deriveSessionKey > both phones derive the same key from the same inputs | PORTED | SyncPairingTest.kt: `both phones derive the same key from the same inputs` | — |
| deriveSessionKey > a different code yields a different key | PORTED | SyncPairingTest.kt: `a different code yields a different key` | — |
| deriveSessionKey > nonce order is fixed, so host/guest roles agree | PORTED | SyncPairingTest.kt: `nonce order is fixed, so host and guest roles agree` | — |
| auth proof exchange > matching codes: each side verifies the peer proof | PORTED | SyncPairingTest.kt: `matching codes - each side verifies the peer proof` | — |
| auth proof exchange > a reflected proof does not validate (role binding) | PORTED | SyncPairingTest.kt: `a reflected proof does not validate (role binding)` | — |
| auth proof exchange > wrong code on the guest fails verification | PORTED | SyncPairingTest.kt: `wrong code on the guest fails verification` | — |
| constantTimeEquals > true only for identical byte lists | PORTED | SyncPairingTest.kt: `constantTimeEquals is true only for identical byte arrays` | — |

## test/data/source/sync/sync_session_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/devicesync/protocol/SyncSessionTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| bidirectional merge > each side imports what it lacked and skips shared records | PORTED | SyncSessionTest.kt: `each side imports what it lacked and skips shared records` | — |
| bidirectional merge > per-type summaries split the tallies correctly | PORTED | SyncSessionTest.kt: `per-type summaries split the tallies correctly` | — |
| idempotency > a second sync writes nothing new | PORTED | SyncSessionTest.kt: `a second sync writes nothing new` | — |
| within-session dedup > a key sent twice in one direction is written once | PORTED | SyncSessionTest.kt: `a key sent twice in one direction is written once` | — |
| authentication > mismatched codes abort both sides before any data moves | PORTED | SyncSessionTest.kt: `mismatched codes abort both sides before any data moves` | — |
| link failure > a dropped transport ends the session as an abort | PORTED | SyncSessionTest.kt: `a dropped transport ends the session as an abort` | — |
| type negotiation > only the intersection of supported+selected types syncs | PORTED | SyncSessionTest.kt: `only the intersection of supported+selected types syncs` | — |
| write accounting > a received record whose write fails is not counted as imported | PORTED | SyncSessionTest.kt: `a received record whose write fails is not counted as imported` | — |
| hostile peer > a record frame before authentication aborts the session | PORTED | SyncSessionTest.kt: `a record frame before authentication aborts the session` | — |
| hostile peer > a malformed hello frame aborts cleanly instead of crashing | PORTED | SyncSessionTest.kt: `a malformed hello frame aborts cleanly instead of crashing` | — |

## test/data/sync/body_energy_chain_sync_service_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/sync/BodyEnergyChainSyncServiceTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a cold window is walked oldest first, and today is left alone | PORTED | BodyEnergyChainSyncServiceTest.kt: `a cold window is walked oldest first, and today is left alone` | exact-list equality implies today excluded |
| the walked days form a connected chain | PORTED | BodyEnergyChainSyncServiceTest.kt: `the walked days form a connected chain` | — |
| a second pass inside the throttle window does no work | PORTED | BodyEnergyChainSyncServiceTest.kt: `a second pass inside the throttle window does no work` | — |
| past the throttle, already-stored fresh days are still skipped | PORTED | BodyEnergyChainSyncServiceTest.kt: `past the throttle, already-stored fresh days are still skipped` | — |
| a changed calibration purges the chain rather than ageing it out | PORTED | BodyEnergyChainSyncServiceTest.kt: `a changed calibration purges the chain rather than ageing it out` | — |
| without the heart-rate permission it does nothing | PORTED | BodyEnergyChainSyncServiceTest.kt: `without the heart-rate permission it does nothing` | — |
| concurrent calls share a single run | PORTED | BodyEnergyChainSyncServiceTest.kt: `concurrent calls share a single run` | — |
| a throwing repository is swallowed, not surfaced | PORTED | BodyEnergyChainSyncServiceTest.kt: `a throwing repository is swallowed, not surfaced` | — |
| the legacy prefs timelines are purged on the first pass | PORTED | BodyEnergyChainSyncServiceTest.kt: `the legacy prefs timelines are purged on the first pass` | real baseline store over a fake SharedPreferences, so the retired key really disappears |
| retention drops old buckets but keeps their day summaries | PORTED | BodyEnergyChainSyncServiceTest.kt: `retention drops old buckets but keeps their day summaries` | — |
| a gain the watch learner nudged does not purge the stored history | PORTED | BodyEnergyChainSyncServiceTest.kt: `a gain the watch learner nudged does not purge the stored history` | — |
| a later pass skips settled days and revisits only unsettled ones | PORTED | BodyEnergyChainSyncServiceTest.kt: `a later pass skips settled days and revisits only unsettled ones` | — |
| a forced pass > bypasses the throttle, so a watch sync is acted on immediately | PORTED | BodyEnergyChainSyncServiceTest.kt: `a forced pass bypasses the throttle, so a watch sync is acted on at once` | — |
| a forced pass > an unforced call inside the throttle leaves the holes alone | PORTED | BodyEnergyChainSyncServiceTest.kt: `an unforced call inside the throttle leaves the holes alone` | — |
| a forced pass > force does not override the freshness skip | PORTED | BodyEnergyChainSyncServiceTest.kt: `force does not override the freshness skip` | — |

## test/data/sync/calories_history_sync_service_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/sync/CaloriesHistorySyncServiceTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| full sync (no cursor) stores each day total and a token | PORTED | CaloriesHistorySyncServiceTest.kt: `full sync purges legacy keys and stores only positive-burn days` | value+sampleCount+token asserted |
| a full sync purges rows and cursor left under the legacy cache key | PORTED | CaloriesHistorySyncServiceTest.kt: same | verifies purgeMetric("totalCaloriesBurned") |
| a zero-burn day is not stored | PORTED | CaloriesHistorySyncServiceTest.kt: same | 0-kcal day excluded from replaced rows |
| incremental sync recomputes only the changed day and advances the token | DIVERGED | CaloriesHistorySyncServiceTest.kt: `incremental recompute deletes a day that dropped to zero burn` | token advance asserted, but only the delete path; positive-value upsert on incremental not tested for calories |
| a day that drops to zero is deleted on incremental sync | PORTED | CaloriesHistorySyncServiceTest.kt: `incremental recompute deletes a day that dropped to zero burn` | — |
| a deletion triggers a full rebuild from the current truth | DIVERGED | CaloriesHistorySyncServiceTest.kt: `expired token abandons the delta and rebuilds` | same `tokenExpired \|\| hasDeletions` branch exercised, but via tokenExpired only; hasDeletions flag itself untested for calories |
| two buckets on the same day are summed, not a duplicate-key crash | PORTED | CaloriesHistorySyncServiceTest.kt: `duplicate dates from a DST-clipped tail bucket sum instead of colliding` | — |
| no total-calories permission skips the sync entirely | PORTED | CaloriesHistorySyncServiceTest.kt: `ungranted permission is a no-op` | — |

## test/data/sync/history_sync_scheduler_test.dart
Kotlin counterpart: none (data/sync/HistorySyncScheduler.kt exists, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the drains run one after another, never at the same time | PORTED | HistorySyncSchedulerTest.kt: `the drains run one after another, never at the same time` | — |
| no drain starts a first full sync | PORTED | HistorySyncSchedulerTest.kt: `no drain starts a first full sync` | — |
| a failing drain does not starve the ones after it | PORTED | HistorySyncSchedulerTest: `a failing drain does not starve the ones after it`, `a cancelled drain still unwinds` | Was filed as a behavior divergence; the latch is claimed before the first drain, so a throw starved the rest for the life of the process. Each drain now fails alone |
| concurrent calls share one run | PORTED | HistorySyncSchedulerTest.kt: `concurrent calls share one run` | — |
| a later app open drains again | N/A-BEHAVIOR | — | blocked on behavior decision - Kotlin drains once per process (AtomicBoolean one-shot), so a later app open is a no-op |

## test/data/sync/vitals_history_sync_service_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/data/sync/VitalsHistorySyncServiceTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| full sync (no cursor) buckets the range and stores a token | PORTED | VitalsHistorySyncServiceTest.kt: `first sync registers the token before the read and replaces the metric` | mean*count sum, sampleCount and token asserted |
| full sync registers the changes token BEFORE the history read | DIVERGED | VitalsHistorySyncServiceTest.kt: same | test name claims the ordering but no coVerifyOrder/call-order assertion exists |
| incremental sync recomputes only the changed day and advances the token | PORTED | VitalsHistorySyncServiceTest.kt: `incremental sync recomputes only the upserted days` | — |
| a deletion triggers a full rebuild from the current truth | PORTED | VitalsHistorySyncServiceTest.kt: `deletions force a full rebuild because they carry no date` | — |
| an expired token triggers a full rebuild | DIVERGED | VitalsHistorySyncServiceTest.kt: `deletions force a full rebuild because they carry no date` | same `tokenExpired \|\| hasDeletions` branch, but tokenExpired itself only tested in the calories service |
| a day that lost all its records is deleted from the cache | PORTED | VitalsHistorySyncServiceTest.kt: `a day whose recompute comes back empty is deleted` | — |

## test/core/diagnostics/debug_log_sanitizer_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/diagnostics/PrivacySafeDebugLogExporterTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| DebugLogSanitizer.sanitizeLogLine > redacts MAC, email and UUID in an allowed-tag line | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize redacts MAC, email and UUID in an allowed-tag line` | — |
| DebugLogSanitizer.sanitizeLogLine > redacts key=value identifiers keeping the key | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize redacts key=value identifiers keeping the key` | — |
| DebugLogSanitizer.sanitizeLogLine > drops a line containing a location keyword | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize drops a line containing a location keyword` | — |
| DebugLogSanitizer.sanitizeLogLine > drops a line containing a token keyword | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize drops a line containing a token keyword` | — |
| DebugLogSanitizer.sanitizeLogLine > keeps AppleHealthImporter E/W/A/F lines verbatim (unsanitized) | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize keeps Apple Health importer warnings and errors unsanitized` | — |
| DebugLogSanitizer.sanitizeLogLine > sanitizes AppleHealthImporter non-W/E/A/F lines normally | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize handles AppleHealthImporter non-W E A F lines normally` | — |
| DebugLogSanitizer.sanitizeLogLine > passes an allowed tag through | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize keeps OpenVitals operational lines` | — |
| DebugLogSanitizer.sanitizeLogLine > drops a non-allowed tag | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize excludes unrelated tags` | asserted via sanitizeLogcat (D/OkHttp dropped) |
| DebugLogSanitizer.sanitizeLogLine > drops a non-log-format line | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize drops a non-log-format line` | — |
| DebugLogSanitizer.sanitizeLogLine > drops a blank message | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize drops a blank message` | — |
| DebugLogSanitizer.sanitizeLogLine > redacts ISO instants and dates | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitize redacts ISO instants and dates` | — |
| DebugLogSanitizer.sanitizeLogcat > caps output at maxLines keeping the most recent lines | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitizeLogcat caps output at maxLines keeping the most recent lines` | seam: `PrivacySafeDebugLogExporter.MaxLines` private -> internal |
| DebugLogSanitizer.sanitizeLogcat > counts dropped lines across the whole input | PORTED | PrivacySafeDebugLogExporterTest.kt: `sanitizeLogcat counts dropped lines across the whole input` | — |
| DebugLogSanitizer.buildExportText > emits the header block then the sanitized lines | MISSING | — | blocked on behavior decision: Kotlin has no pure `buildExportText`; the header is built inside `currentProcessLogcatPayload`, which is gated on `BuildConfig.OPENVITALS_DIAGNOSTICS` and shells out to logcat. Porting needs a prod extraction first |

## test/core/diagnostics/diagnostics_build_config_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| is enabled under the test environment (a debug build) | N/A-FRAMEWORK | — | Dart const-folding of kDiagnosticsEnabled; Kotlin gate is Gradle BuildConfig.OPENVITALS_DIAGNOSTICS |
| mirrors Kotlin BuildConfig.OPENVITALS_DIAGNOSTICS, not BuildConfig.DEBUG | N/A-FRAMEWORK | — | dart-define gate; the Kotlin side is generated BuildConfig, no runtime logic to test |
| the gate sites consult kDiagnosticsEnabled, not kDebugMode > lib/navigation/app_router.dart | N/A-FRAMEWORK | — | source-level guard over Flutter files |
| the gate sites consult kDiagnosticsEnabled, not kDebugMode > lib/features/settings/presentation/settings_screen.dart | N/A-FRAMEWORK | — | source-level guard over Flutter files |

## test/core/export/export_staging_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a staged file lands under the feature directory, named as asked | PORTED | ExportStagingTest.kt: `a staged file lands under the feature directory, named as asked` | — |
| two features staging the same name do not collide | PORTED | ExportStagingTest.kt: `two features staging the same name do not collide` | asserts the two production cache-dir constants differ and same-named files under each stay distinct |
| bytes are staged verbatim, not decoded | N/A-FRAMEWORK | — | Kotlin writes OutputStream directly (no byte-staging API that could string-round-trip) |
| staging prunes copies older than a day and keeps fresh ones | PORTED | ExportStagingTest.kt: `staging prunes copies older than a day and keeps fresh ones` | — |
| pruning runs for byte exports too, not only text | N/A-FRAMEWORK | — | single write path in Kotlin; no text/bytes split to diverge |
| re-staging the same name overwrites rather than stacking up | PORTED | ExportStagingTest.kt: `re-staging the same name overwrites rather than stacking up` | — |
| a locked or vanished file does not abort the export | PORTED | ExportStagingTest.kt: `a locked or vanished file does not abort the export` | — |

## test/core/export/export_surface_guard_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no lib/ code saves an export through file_selector | N/A-FRAMEWORK | — | source guard against a Dart plugin with no Android save picker |
| ShareParams is built in exactly one place | N/A-FRAMEWORK | — | share_plus plugin usage guard; Kotlin builds ACTION_SEND intents per feature |
| exports are staged through the shared cache, not hand-rolled | N/A-FRAMEWORK | — | guard over Dart getTemporaryDirectory usage |

## test/core/period/period_load_query_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/period/PeriodLoadQueryTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| query clamps future anchor date before creating windows | PORTED | PeriodLoadQueryTest.kt: `query clamps future anchor date before creating windows` | — |
| query creates current previous and baseline windows | PORTED | PeriodLoadQueryTest.kt: `query creates current previous and baseline windows` | — |
| query uses rolling last seven days when requested | PORTED | PeriodLoadQueryTest.kt: `query uses rolling last seven days when week period mode requests it` | — |
| query uses rolling last thirty days for month when rolling dates are selected | PORTED | PeriodLoadQueryTest.kt: `query uses rolling last thirty days for month when rolling dates are selected` | — |
| query uses rolling last three hundred sixty five days for year when rolling dates are selected | PORTED | PeriodLoadQueryTest.kt: `query uses rolling last three hundred sixty five days for year when rolling dates are selected` | — |
| query clips current Monday to Sunday load window to today | PORTED | PeriodLoadQueryTest.kt: `query clips current Monday to Sunday load window to today` | — |
| selection driver persists range and clamps next period | PORTED | PeriodLoadQueryTest.kt: `selection driver persists range and clamps next period` | — |
| selection driver advances unpinned stale day to today on resume | PORTED | PeriodLoadQueryTest.kt: `selection driver advances unpinned stale day to today on resume` | — |
| selection driver keeps user pinned past day on resume | PORTED | PeriodLoadQueryTest.kt: `selection driver keeps user pinned past day on resume` | — |

## test/core/period/period_selection_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/period/PeriodSelectionTest.kt (plus ui/components/PeriodNavigatorTest.kt for periodFor)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| selectRange keeps future selected date capped at today | PORTED | PeriodSelectionTest.kt: `selectRange keeps future selected date capped at today` | — |
| previousPeriod moves by selected range | PORTED | PeriodSelectionTest.kt: `previousPeriod moves by selected range` | — |
| previousPeriod moves month by thirty days for rolling dates | PORTED | PeriodSelectionTest.kt: `previousPeriod moves month by thirty days for rolling dates` | — |
| previousPeriod moves year by three hundred sixty five days for rolling dates | PORTED | PeriodSelectionTest.kt: `previousPeriod moves year by three hundred sixty five days for rolling dates` | — |
| nextPeriod moves month by thirty days for rolling dates | PORTED | PeriodSelectionTest.kt: `nextPeriod moves month by thirty days for rolling dates` | — |
| nextPeriod does not move beyond current period | PORTED | PeriodSelectionTest.kt: `nextPeriod does not move beyond current period` | — |
| nextPeriod moves when the next period is not in the future | PORTED | PeriodSelectionTest.kt: `nextPeriod moves when the next period is not in the future` | — |
| previousPeriodFor returns previous calendar period | PORTED | PeriodSelectionTest.kt: `previousPeriodFor returns previous calendar period` | — |
| previousPeriodFor returns the previous rolling month window | PORTED | PeriodSelectionTest.kt: `previousPeriodFor returns the previous rolling month window` | — |
| displayPeriodFor keeps full Monday to Sunday week mid week | PORTED | PeriodSelectionTest.kt: `displayPeriodFor keeps full Monday to Sunday week even when today is mid week` | — |
| displayPeriodFor supports rolling last seven days | PORTED | PeriodSelectionTest.kt: `displayPeriodFor supports rolling last seven days` | — |
| displayPeriodFor supports rolling last thirty days | PORTED | PeriodSelectionTest.kt: `displayPeriodFor supports rolling last thirty days` | — |
| displayPeriodFor supports rolling last three hundred sixty five days | PORTED | PeriodSelectionTest.kt: `displayPeriodFor supports rolling last three hundred sixty five days` | — |
| calendar mode keeps calendar month and year windows | PORTED | PeriodSelectionTest.kt: `calendar mode keeps calendar month and year windows` | — |

## test/core/period/period_title_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/period/PeriodTitleTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| day titles use relative labels for today and yesterday | PORTED | PeriodTitleTest.kt: `dayTitlesUseRelativeLabelsForTodayAndYesterday` | — |
| period titles use current labels when period contains today | PORTED | PeriodTitleTest.kt: `periodTitlesUseCurrentLabelsWhenPeriodContainsToday` | — |
| past period titles use dated labels | PORTED | PeriodTitleTest.kt: `pastPeriodTitlesUseDatedLabels` | — |
| rolling period titles use fixed day window labels | PORTED | PeriodTitleTest.kt: `rollingPeriodTitlesUseFixedDayWindowLabels` | — |
| past rolling periods read as the dated span they cover | PORTED | PeriodTitleTest.kt: `pastRollingPeriodsReadAsTheDatedSpanTheyCover` | fixed: `rollingSpanTitle` in PeriodTitles.kt (and the Composable `localizedPeriodTitle`) renders a past rolling window as "12 Apr – 11 May 2026" |
| a past rolling span that straddles a year shows both years | PORTED | PeriodTitleTest.kt: `aPastRollingSpanThatStraddlesAYearShowsBothYears` | fixed: the year rides on the start too when the window crosses one |

## test/core/presentation/command_state_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| variants of the same shape and value are equal | N/A-FRAMEWORK | — | no CommandState type in Kotlin; data/sealed classes get equality from the language |
| distinct variants are not equal | N/A-FRAMEWORK | — | same |
| switch exhaustively covers the command lifecycle | N/A-FRAMEWORK | — | Dart sealed-class exhaustiveness; Kotlin `when` over sealed types is compiler-checked |

## test/core/presentation/file_picking_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no lib/ code picks an INPUT file through file_selector | N/A-FRAMEWORK | — | source guard against a Dart plugin OOM; Kotlin uses SAF document pickers natively |
| the pick helpers never ask the platform for the file contents | N/A-FRAMEWORK | — | file_picker withData flags; no Kotlin analog |

## test/core/presentation/measurement_input_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryFormContentTest.kt, features/manualentry/nutrition/CarbsEntryViewModelTest.kt (no shared measurement-input module in Kotlin)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| parseDecimalInput > accepts a comma as the decimal separator, and trims | DIVERGED | HydrationEntryFormContentTest.kt: `hydration input accepts comma decimal separator`, `invalid hydration input returns null` | no shared parser in Kotlin (comma parsing duplicated per screen); trim and empty-string cases untested |
| volume > labels the field in the user's unit | N/A-FRAMEWORK | — | unit labels come from Compose string resources in Kotlin |
| volume > typed input always canonicalizes to millilitres | PORTED | HydrationEntryFormContentTest.kt: `metric hydration input is milliliters`, `imperial hydration input converts fluid ounces to milliliters` | — |
| volume > a stored volume seeds the field in the user's unit | DIVERGED | HydrationEntryFormContentTest.kt: `imperial initial hydration amount displays fluid ounces`, `metric initial hydration amount displays milliliters` | null → empty-field seeding untested |
| volume > round-trips a typed imperial amount back to the same text | PORTED | MeasurementInputTest.kt: `volume round-trips a typed imperial amount back to the same text` | — |
| volume > bounds are rendered in the field's unit | PORTED | HydrationAmountBoundsTest: `theAllowedAmountRangeIsStatedInTheFieldsOwnUnit` | Compose instrumentation; runs on a device, not in CI |
| carbs > labels grams or ounces | N/A-FRAMEWORK | — | labels in Compose resources |
| carbs > ounces convert to grams | DIVERGED | CarbsEntryViewModelTest.kt: `metric carbs input stays grams`, `imperial carbs input converts ounces to grams` | invalid-input → null case untested |
| body > weight: pounds convert to kilograms | PORTED | MeasurementInputTest.kt: `body weight pounds convert to kilograms` | seam: canonicalBodyMeasurementValue private->internal |
| body > height: inches convert to centimetres | PORTED | MeasurementInputTest.kt: `body height inches convert to centimetres` | — |
| temperature > labels degrees without the degree sign, as Kotlin does | N/A-FRAMEWORK | — | labels in Compose resources |
| temperature > Fahrenheit converts to Celsius | PORTED | MeasurementInputTest.kt: `temperature Fahrenheit converts to Celsius` | seam: canonicalVitalsValue private->internal |
| a metric formatter never rewrites what the user typed | DIVERGED | HydrationEntryFormContentTest.kt: `metric hydration input is milliliters`; CarbsEntryViewModelTest.kt: `metric carbs input stays grams` | metric identity covered only for hydration and carbs, not weight/height/temperature |

## test/core/presentation/metric_detail_sections_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/presentation/MetricDetailSectionOrderViewModelTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/domain/preferences/MetricDetailSectionIdTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders in the default order and hides invisible sections | PORTED | OrderedMetricDetailSectionsTest: `rendersInTheDefaultOrderAndHidesInvisibleSections` | Compose instrumentation; runs on a device, not in CI |
| honours the persisted order | DIVERGED | MetricDetailSectionIdTest.kt: `metricDetailSectionOrderFromStored_mergesMissingSections` | stored-order parsing covered; rendering in that order is widget-side |
| a stored order missing newer sections still shows them | DIVERGED | MetricDetailSectionIdTest.kt: `metricDetailSectionOrderFromStored_mergesMissingSections` | asserts size + set membership but not that missing sections append in default relative order |
| edit mode wraps sections in the shared reorderable tile | PORTED | OrderedMetricDetailSectionsTest: `editModeExposesEachSectionWithItsMoveActions` | Compose instrumentation; runs on a device, not in CI |
| moveSectionToTarget persists a drop-on-target reorder | PORTED | MetricDetailSectionOrderViewModelTest.kt: `moveSectionToTarget_reordersAndPersists` | — |
| moveSection nudges one place in the full order | PORTED | MetricDetailSectionOrderViewModelTest.kt: `moveSection_nudgesOnePlaceInTheFullOrder` | — |
| moveSection cannot push a section past either end | PORTED | MetricDetailSectionOrderViewModelTest.kt: `moveSection_cannotPushASectionPastEitherEnd` | — |

## test/core/presentation/period_metric_loader_test.dart
Kotlin counterpart: none (no shared period-load orchestrator in Kotlin; per-feature ViewModels)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a navigation clears the stale display so the loading state shows | N/A-BEHAVIOR | — | blocked on behavior decision - Kotlin ViewModels never blank the display on navigation, they only set isLoading |
| a same-window refresh keeps the display (no loading flash) | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin core loader holds no display at all; retention is a per-ViewModel concern and Kotlin never clears it |
| rapid navigations coalesce: one fetch in flight, latest wins | PORTED | PeriodMetricLoaderTest.kt: `rapid navigations coalesce - one fetch in flight, latest wins` | drives core/performance/LoadCoordinator; Kotlin coalesces by cancel-and-relaunch rather than parking, so the assertion is one dispatched fetch and newest selection wins |
| an error sets the error and clears loading | DIVERGED | HydrationViewModelTest.kt: `load failure sets error and clears loading` | covered per-feature, not in a shared loader |

## test/core/presentation/report_sharing_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| TextReportSharing > the sheet is handed a real file holding the report | N/A-FRAMEWORK | — | Kotlin shares via ACTION_SEND + FileProvider intents (DebugLogSharing.kt pattern); platform plumbing, would need Robolectric |
| TextReportSharing > the attachment keeps the report file name and a text mime type | N/A-FRAMEWORK | — | Intent type/ClipData plumbing |
| TextReportSharing > the chooser title and the email subject reach the share sheet | N/A-FRAMEWORK | — | Intent.createChooser plumbing |
| TextReportSharing > the report goes as a file, never as inline share text | N/A-FRAMEWORK | — | EXTRA_STREAM vs EXTRA_TEXT is Intent-level |
| TextReportSharing > a share sheet with no target surfaces as a failure to the caller | N/A-FRAMEWORK | — | ActivityNotFound handling is platform-side |
| TextReportSharing > the report is staged in its own directory, not another export's | N/A-FRAMEWORK | — | per-feature cache directory wiring is inside Context extension functions |

## test/core/presentation/screen_error_mapping_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/presentation/ScreenErrorTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| PermissionFailure maps to ScreenErrorPermissionDenied | PORTED | ScreenErrorTest.kt: `a permission failure becomes ScreenError PermissionDenied`, `a wrapped permission failure still becomes PermissionDenied` | Kotlin has no AppFailure hierarchy, so the boundary is `Throwable.toScreenError()`: a SecurityException (direct or wrapped) is the permission failure and maps to ScreenError.PermissionDenied |
| HealthConnectUnavailableFailure maps to its ScreenError | N/A-FRAMEWORK | — | same — no failure-type-to-error mapping layer |
| NotFoundFailure maps to ScreenErrorNotFound | N/A-FRAMEWORK | — | same |
| UnexpectedFailure maps to a trimmed ScreenErrorMessage | DIVERGED | ScreenErrorTest.kt: `toScreenError uses throwable message when present` | message propagation covered; whitespace trimming not asserted |
| blank or null-ish UnexpectedFailure falls back | DIVERGED | ScreenErrorTest.kt: `toScreenError uses fallback when message blank` | blank covered; the literal "null" string case untested |

## test/core/presentation/unit_formatter_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/presentation/UnitFormatterTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| count uses locale grouping | PORTED | UnitFormatterTest.kt: `count uses locale grouping` | — |
| metric distance uses meters below one kilometer | PORTED | UnitFormatterTest.kt: `metric distance uses meters below one kilometer` | — |
| metric distance uses kilometers from one kilometer | PORTED | UnitFormatterTest.kt: `metric distance uses kilometers from one kilometer` | — |
| imperial distance uses miles above threshold | PORTED | UnitFormatterTest.kt: `imperial distance uses miles above threshold` | — |
| imperial distance uses feet below threshold | PORTED | UnitFormatterTest.kt: `imperial distance uses feet below threshold` | — |
| imperial elevation uses feet | PORTED | UnitFormatterTest.kt: `imperial elevation uses feet` | — |
| imperial weight uses pounds | PORTED | UnitFormatterTest.kt: `imperial weight uses pounds` | — |
| metric height uses centimeters | PORTED | UnitFormatterTest.kt: `metric height uses centimeters` | — |
| imperial height uses feet and inches | PORTED | UnitFormatterTest.kt: `imperial height uses feet and inches` | — |
| imperial hydration uses fluid ounces | PORTED | UnitFormatterTest.kt: `imperial hydration uses fluid ounces` | — |
| metric hydration uses liters | PORTED | UnitFormatterTest.kt: `metric hydration uses liters` | — |
| metric hydration keeps two decimals below one liter | PORTED | UnitFormatterTest.kt: `metric hydration keeps two decimals below one liter` | — |
| imperial temperature uses fahrenheit | PORTED | UnitFormatterTest.kt: `imperial temperature uses fahrenheit` | — |
| metric temperature delta keeps celsius delta | PORTED | UnitFormatterTest.kt: `metric temperature delta keeps celsius delta` | — |
| imperial temperature delta converts to fahrenheit delta | PORTED | UnitFormatterTest.kt: `imperial temperature delta converts to fahrenheit delta` | — |
| metric blood glucose uses mmol per liter | PORTED | UnitFormatterTest.kt: `metric blood glucose uses mmol per liter` | — |
| imperial blood glucose uses milligrams per deciliter | PORTED | UnitFormatterTest.kt: `imperial blood glucose uses milligrams per deciliter` | — |
| blood pressure is not converted | PORTED | UnitFormatterTest.kt: `blood pressure is not converted` | — |
| duration formats hours and padded minutes | PORTED | UnitFormatterTest.kt: `duration formats hours and padded minutes` | — |
| metric average speed uses kilometers per hour | PORTED | UnitFormatterTest.kt: `metric average speed uses kilometers per hour` | — |
| imperial average speed uses miles per hour | PORTED | UnitFormatterTest.kt: `imperial average speed uses miles per hour` | — |
| metric recorded speed uses kilometers per hour | PORTED | UnitFormatterTest.kt: `metric recorded speed uses kilometers per hour` | — |
| imperial recorded speed uses miles per hour | PORTED | UnitFormatterTest.kt: `imperial recorded speed uses miles per hour` | — |
| power uses watts | PORTED | UnitFormatterTest.kt: `power uses watts` | — |
| cadence uses rpm | PORTED | UnitFormatterTest.kt: `cadence uses rpm` | — |
| metric average pace uses minutes per kilometer | PORTED | UnitFormatterTest.kt: `metric average pace uses minutes per kilometer` | — |
| imperial average pace uses minutes per mile | PORTED | UnitFormatterTest.kt: `imperial average pace uses minutes per mile` | — |
| average pace needs distance and duration | PORTED | UnitFormatterTest.kt: `average pace needs distance and duration` | — |

## test/core/reminders/alarm_manager_reminder_scheduler_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| arms a wake-up, doze-proof alarm that survives reboot | N/A-FRAMEWORK | — | Kotlin arms via AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP) + BootReceivers; thin platform wrapper (HydrationReminderAlarmManager.kt), untested |
| arms EXACT when the exact-alarm permission is granted | N/A-FRAMEWORK | — | Kotlin design always uses inexact setAndAllowWhileIdle; no exact-alarm path |
| degrades to INEXACT when the permission is not granted | N/A-FRAMEWORK | — | no exact/inexact gate in Kotlin |
| arms INEXACT when no exact-alarm gate is wired | N/A-FRAMEWORK | — | same |
| consults the gate on every schedule, not just the first | N/A-FRAMEWORK | — | same |
| cancels its own alarm id | N/A-FRAMEWORK | — | PendingIntent-based cancel, platform plumbing |
| the home-widget refresh alarm is wired to a vm:entry-point callback | N/A-FRAMEWORK | — | Dart isolate callback-handle concern |

## test/core/reminders/reminder_controller_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderControllerTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/reminders/MindfulnessReminderControllerTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| goal progress > is met only at or above a positive target | DIVERGED | HydrationReminderControllerTest.kt: `alarm trigger does not notify after goal is met` / `…when goal is not met…` | goal-met threshold exercised only indirectly (1.0 vs 2.0, 2.0 vs 2.0); no boundary unit test |
| goal progress > a zero or absent target is never met, however much is logged | PORTED | ReminderControllerTest.kt: `goal progress - a zero or absent target is never met, however much is logged` | — |
| apply > a disabled reminder clears and schedules nothing | PORTED | HydrationReminderControllerTest.kt: `disabled config clears alarm and notification` (also Mindfulness) | — |
| apply > missing notification permission clears, even when enabled | PORTED | ReminderControllerTest.kt: `apply - missing notification permission clears, even when enabled` | Build.VERSION.SDK_INT is an unsettable static final on the JVM, so the API gate itself is not faked |
| apply > schedules a batch whose first fire is the next interval | DIVERGED | HydrationReminderControllerTest.kt: `enabled config schedules next reminder` | verifies schedule(any()) without asserting the trigger instant; instant computed and tested separately in HydrationReminderScheduleTest |
| apply > every scheduled time falls inside the active window | N/A-FRAMEWORK | — | batch property; Kotlin one-shot window compliance covered by HydrationReminderScheduleTest |
| apply > a met goal pushes the whole batch past today | PORTED | HydrationReminderScheduleTest.kt: `goal met schedules tomorrow after active start interval`; HydrationReminderControllerTest.kt: `alarm trigger does not notify after goal is met` | behavioral equivalent of goal-met suppression + tomorrow scheduling |
| apply > passes today's progress to the scheduler for the notification | PORTED | HydrationReminderControllerTest.kt: `alarm trigger shows notification when goal is not met and active hours allow it` | verifies showHydrationReminder(1.0, 2.0) |
| apply > anchors the first fire to the last logged time | PORTED | HydrationReminderScheduleTest.kt: `anchor measures the countdown from the last drink` | also `quick add logs water and reschedules after a real write` re-anchors |
| apply > a daily schedule notifies at any time (no quiet hours) | PORTED | MindfulnessReminderScheduleTest.kt: `next reminder before configured time schedules today` | mindfulness schedule has no active-hours gate in either app |
| apply > explicit settings override the persisted ones | N/A-FRAMEWORK | — | Kotlin applyConfig always takes the explicit config; no persisted-vs-explicit precedence to test |
| restoreSchedule > re-plans an enabled reminder | PORTED | ReminderControllerTest.kt: `restoreSchedule re-plans an enabled reminder` | — |
| restoreSchedule > clears a disabled one | PORTED | ReminderControllerTest.kt: `restoreSchedule clears a disabled one` | — |
| clear cancels the batch | DIVERGED | HydrationReminderControllerTest.kt: `disabled config clears alarm and notification` | cancel covered only via the disabled-config apply path; no direct clear test |
| concurrent applies serialize instead of interleaving | PORTED | HydrationReminderControllerTest: `concurrent applies serialize instead of interleaving` | Was filed as a behavior divergence; interleaving let the config the user moved away from win the race. A mutex now spans the read and the arm |

## test/core/reminders/reminder_notification_spec_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| base notification ids are unique across reminder features | PORTED | ReminderNotificationSpecTest.kt: `base notification ids are unique across reminder features` | seam: per-service NotificationId private->internal |
| reserved id ranges do not overlap across features | N/A-FRAMEWORK | — | batch id ranges are Flutter-only; Kotlin uses one id per feature |
| channel ids are unique across reminder features | PORTED | ReminderNotificationSpecTest.kt: `channel ids are unique across reminder features` | seam: per-service ChannelId private->internal |
| every spec sets a monochrome small icon | N/A-FRAMEWORK | — | Kotlin setSmallIcon(R.drawable.…) is a compile-time resource reference |
| every spec has a non-empty scheduled body | N/A-FRAMEWORK | — | no pre-scheduled body; Kotlin builds the notification at fire time with live progress |
| same-day progress body > hydration shows "x.x L / y.y L" | N/A-FRAMEWORK | — | progress text built from Android string resources via context.getString |
| same-day progress body > mindfulness shows whole minutes | N/A-FRAMEWORK | — | same |
| the hydration reminder opens the hydration entry when tapped | N/A-FRAMEWORK | — | PendingIntent navigation, platform plumbing |

## test/core/reminders/reminder_schedule_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderScheduleTest.kt, app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/reminders/MindfulnessReminderScheduleTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| IntervalWindowReminderSchedule.nextTrigger > inside active hours adds the interval | PORTED | HydrationReminderScheduleTest.kt: `next reminder inside active hours adds interval` | — |
| IntervalWindowReminderSchedule.nextTrigger > before active hours waits until active start plus the interval | PORTED | HydrationReminderScheduleTest.kt: `next reminder before active hours waits until active start plus interval` | — |
| IntervalWindowReminderSchedule.nextTrigger > crossing active end moves to the next active start plus interval | PORTED | HydrationReminderScheduleTest.kt: `next reminder crossing active end moves to next active start plus interval` | — |
| IntervalWindowReminderSchedule.nextTrigger > a met goal schedules tomorrow after active start plus interval | PORTED | HydrationReminderScheduleTest.kt: `goal met schedules tomorrow after active start interval` | — |
| IntervalWindowReminderSchedule.isWithinActiveHours > an overnight window includes times after midnight, before the end | PORTED | HydrationReminderScheduleTest.kt: `overnight active hours include times after midnight before end` | — |
| IntervalWindowReminderSchedule.isWithinActiveHours > an equal start and end means always active | PORTED | ReminderScheduleTest.kt: `an equal start and end means always active` | — |
| IntervalWindowReminderSchedule.isWithinActiveHours > allowsNotificationAt gates on the window | DIVERGED | HydrationReminderScheduleTest.kt: `overnight active hours include times after midnight before end` | window gate tested at LocalTime granularity via isWithinHydrationReminderActiveHours; no instant-level delivery-gate test |
| DailyTimeReminderSchedule > fires later today when the time is still ahead | PORTED | MindfulnessReminderScheduleTest.kt: `next reminder before configured time schedules today` | — |
| DailyTimeReminderSchedule > rolls to tomorrow once the time has passed | PORTED | MindfulnessReminderScheduleTest.kt: `next reminder after configured time schedules tomorrow` | — |
| DailyTimeReminderSchedule > rolls to tomorrow when the goal is already met | PORTED | MindfulnessReminderScheduleTest.kt: `goal met schedules tomorrow` | — |
| DailyTimeReminderSchedule > has no quiet hours — it may always notify | N/A-FRAMEWORK | — | delivery-gate API is Flutter-batch-side; Kotlin AlarmManager fires the mindfulness alarm with no hour gate by construction |
| IntervalWindowReminderSchedule.plan > lists the upcoming fires at the interval cadence | N/A-FRAMEWORK | — | batch pre-scheduling; Kotlin schedules one next alarm and reschedules on fire |
| IntervalWindowReminderSchedule.plan > is strictly ascending and never leaves the window | N/A-FRAMEWORK | — | batch-only property |
| IntervalWindowReminderSchedule.plan > snaps across the window end to the next active start | N/A-FRAMEWORK | — | single-shot equivalent PORTED via `next reminder crossing active end…` |
| IntervalWindowReminderSchedule.plan > anchors the first fire to the anchor plus the interval | PORTED | HydrationReminderScheduleTest.kt: `anchor measures the countdown from the last drink` | behavior-level match (plan first fire = one-shot next trigger) |
| IntervalWindowReminderSchedule.plan > rolls a stale anchor forward past now | PORTED | HydrationReminderScheduleTest.kt: `stale anchor rolls forward past now in interval steps` | — |
| IntervalWindowReminderSchedule.plan > with no anchor the first fire matches nextTrigger | N/A-FRAMEWORK | — | plan/nextTrigger consistency is Flutter-internal |
| IntervalWindowReminderSchedule.plan > a met goal lists only tomorrow onward | N/A-FRAMEWORK | — | batch listing; single-shot goal-met behavior covered by `goal met schedules tomorrow after active start interval` and `met goal ignores the anchor` |
| IntervalWindowReminderSchedule.plan > respects maxCount and horizon bounds | N/A-FRAMEWORK | — | batch parameters have no Kotlin analog |
| DailyTimeReminderSchedule.plan > lists today then the following days at the same time | N/A-FRAMEWORK | — | batch pre-scheduling |
| DailyTimeReminderSchedule.plan > a met goal starts tomorrow | N/A-FRAMEWORK | — | single-shot equivalent PORTED via `goal met schedules tomorrow` |
| DailyTimeReminderSchedule.plan > the default horizon pre-schedules about two weeks of daily reminders | N/A-FRAMEWORK | — | batch horizon; Kotlin reschedules on each fire so no horizon exists |
| schedules preserve the reference instant's zone | N/A-FRAMEWORK | — | Dart DateTime UTC/local flag concern; Kotlin ZonedDateTime carries the zone by construction |

## test/core/reminders/reminder_time_zone_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| points tz.local at the device time zone | N/A-FRAMEWORK | — | Dart timezone-package initialization; Kotlin java.time uses the system zone natively |
| an unknown zone name reports failure and keeps the previous zone | N/A-FRAMEWORK | — | same |
| a platform-channel failure is swallowed | N/A-FRAMEWORK | — | platform-channel concern |
| is idempotent | N/A-FRAMEWORK | — | same |

## test/core/result/result_test.dart
Kotlin counterpart: none (Kotlin uses kotlin.Result / runCatching; no AppFailure hierarchy)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| map transforms an Ok value | N/A-FRAMEWORK | — | kotlin.Result.map is stdlib |
| map carries a failure through untouched | N/A-FRAMEWORK | — | stdlib behavior |
| flatMap chains on Ok | N/A-FRAMEWORK | — | stdlib mapCatching/fold |
| flatMap short-circuits on Err without invoking next | N/A-FRAMEWORK | — | stdlib behavior |
| getOrNull returns the value on Ok and null on Err | N/A-FRAMEWORK | — | kotlin.Result.getOrNull is stdlib |
| orThrow unwraps an Ok value | N/A-FRAMEWORK | — | kotlin.Result.getOrThrow is stdlib |
| orThrow rethrows the original cause with its original stack | N/A-FRAMEWORK | — | JVM exceptions carry their stack natively |
| orThrow on a cause-less failure throws a StateError naming it | N/A-FRAMEWORK | — | kotlin.Result always wraps a Throwable; no cause-less failure exists |
| Ok has value equality | N/A-FRAMEWORK | — | stdlib value-class equality |
| Err equals another Err over the same failure | N/A-FRAMEWORK | — | stdlib |

## test/core/stats/bucketed_series_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/BucketedSeriesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| empty input yields no buckets | PORTED | BucketedSeriesTest.kt: `empty input yields no buckets` | — |
| non-positive bucket width yields no buckets | PORTED | BucketedSeriesTest.kt: `non-positive bucket width yields no buckets` | — |
| computes average, min and max per bucket | PORTED | BucketedSeriesTest.kt: `computes average, min and max per bucket` | — |
| splits samples into separate buckets and orders them by time | PORTED | BucketedSeriesTest.kt: `splits samples into separate buckets and orders them by time` | — |
| bucket centre sits in the middle of the window | PORTED | BucketedSeriesTest.kt: `bucket centre sits in the middle of the window` | — |
| skips samples before day start and non-finite values | PORTED | BucketedSeriesTest.kt: `skips samples before day start and non-finite values` | — |

## test/core/stats/stats_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/core/stats/StatsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| average — null means "no samples" > averages the values | PORTED | StatsTest.kt: `averageOrNull averages a populated list` | — |
| average — null means "no samples" > returns null on empty, never zero | PORTED | StatsTest.kt: `averageOrNull returns null for an empty list rather than NaN` | — |
| average — null means "no samples" > is never NaN | PORTED | StatsTest.kt: `averageOrNull returns null for an empty list rather than NaN` (+ `rounding an empty average throws…`) | — |
| average — null means "no samples" > averages ints and doubles alike | PORTED | StatsTest.kt: `averageOrNull works for long and int samples` | — |
| average — null means "no samples" > a genuine zero average is zero, not null | PORTED | StatsTest.kt: `averageOrNull keeps a genuine zero distinct from no data` | — |
| averageOrZero — zero is a real value > returns 0 on empty | PORTED | StatsTest.kt: `averageOrZero reports zero only where zero is the answer we want` | — |
| averageOrZero — zero is a real value > otherwise agrees with average | PORTED | StatsTest.kt: `averageOrZero reports zero only where zero is the answer we want` | asserts the populated value directly rather than cross-checking against average |
| minOf / maxOf > find the extremes | N/A-FRAMEWORK | — | Kotlin uses stdlib minOrNull/maxOrNull |
| minOf / maxOf > return null on empty rather than throwing | N/A-FRAMEWORK | — | null-on-empty is the stdlib contract of minOrNull/maxOrNull |
| minOf / maxOf > handle a single value | N/A-FRAMEWORK | — | stdlib |

## test/core/time/local_date_test.dart
Kotlin counterpart: none (Kotlin uses java.time.LocalDate)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| withDayOfMonth clamps to the month length (no phantom Feb 31) | N/A-FRAMEWORK | — | hand-rolled Dart LocalDate; note java.time.withDayOfMonth THROWS on invalid days rather than clamping — Kotlin callers must not rely on clamping |
| the constructor rejects a grossly invalid month/day (debug assert) | N/A-FRAMEWORK | — | java.time.LocalDate.of validates natively (DateTimeException) |

## test/bootstrap/background_health_access_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| hands back a data source whose availability is already resolved | N/A-FRAMEWORK | — | Dart isolate/pigeon handoff of HealthConnectNativeDataSource; Kotlin reads HC directly, availability covered by healthconnect/HealthConnectAvailabilityServiceTest.kt |
| an unavailable provider is an answer, not an error | N/A-FRAMEWORK | — | Same isolate-handoff contract; no Kotlin analog component |

## test/bootstrap/data_refresh_bootstrap_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| mounting alone does not refresh | N/A-FRAMEWORK | — | Riverpod DataRefreshBootstrap/RefreshCoordinator does not exist in Kotlin; refresh is per-ViewModel |
| returning to the foreground re-resolves availability and refreshes | N/A-FRAMEWORK | — | Partially mirrored by DashboardViewModelTest.kt `resumeCurrentDay advances unpinned past date to today`; no availability re-resolve/refresh-signal analog |
| a second resume within the guard interval emits no second signal | N/A-FRAMEWORK | — | Guard-interval concept exists only in the Flutter bootstrap |
| a resume past the guard interval refreshes again | N/A-FRAMEWORK | — | Same |
| a resume after the day rolls over refreshes inside the guard interval | N/A-FRAMEWORK | — | Day-rollover-on-resume behavior mirrored by DashboardViewModelTest.kt resumeCurrentDay tests, different mechanism |

## test/bootstrap/reminder_bootstrap_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderControllerTest.kt (partial)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| restores an enabled reminder and clears a disabled one | DIVERGED | HydrationReminderControllerTest.kt: `enabled config schedules next reminder` / `disabled config clears alarm and notification` (and Mindfulness twin) | Per-feature sync semantics covered; the app-start/BootReceiver restore path itself is untested in Kotlin |
| starts the alarm service on Android (for the widget refresh) | N/A-FRAMEWORK | — | Flutter android_alarm_manager service init; Kotlin uses AlarmManager directly |
| does not touch the alarm service off Android | N/A-FRAMEWORK | — | Flutter platform switch; Kotlin is Android-only |
| a failed time-zone init still restores the schedules | N/A-FRAMEWORK | — | Dart timezone-package init; Kotlin uses the system zone, no init step |
| a throwing time-zone init is swallowed and does not abort startup | N/A-FRAMEWORK | — | Same |
| a throwing alarm service is swallowed and schedules still restore | N/A-FRAMEWORK | — | Flutter alarm-service plumbing only |

## test/bootstrap/reminder_resume_bootstrap_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| returning to the foreground re-plans both reminder batches | PORTED | ReminderRestoreBootstrapTest.kt | the dismissal was wrong twice over: nothing re-planned on resume, AND alarms do not always persist — a reminder firing without POST_NOTIFICATIONS cancels its own alarm and never reschedules, and that permission is auto-revoked for unused apps. ReminderRestoreBootstrap now restores both on every foreground, each in its own runCatching |
| a failed hydration re-plan neither escapes the lifecycle handler nor blocks mindfulness | N/A-FRAMEWORK | — | Same |

## test/bootstrap/reminder_tap_bootstrap_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a tapped reminder opens the route its payload names | N/A-WIDGET | — | Router pump via widget tester; Kotlin tap routing is a PendingIntent, untested |
| a cold start from a notification opens its route after the first frame | N/A-WIDGET | — | Same |
| a payload that is not an in-app location is ignored | N/A-WIDGET | — | Same |

## test/contract/background_health_source_construction_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no isolate can hand-build a HealthConnectNativeDataSource that skips the availability refresh | N/A-FRAMEWORK | — | Text-scan lint over the Flutter lib/ tree; class does not exist in Kotlin |

## test/contract/kotlin_msg_population_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the fields Kotlin hard-codes to null are exactly these | N/A-FRAMEWORK | — | Golden set over the pigeon native readers in the Flutter repo; the Kotlin app has no Msg layer |

## test/contract/local_day_fixture_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| test fixtures derive calendar days from LocalDate, not hour offsets | N/A-FRAMEWORK | — | Lint of the Dart test suite's fixture style; Kotlin tests use java.time directly |

## test/contract/pigeon_domain_contract_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ${binding.msg} <-> ${binding.domain} > every domain field can actually be populated (per binding) | N/A-FRAMEWORK | — | Pigeon Msg<->domain contract; no pigeon boundary in the Kotlin app |
| ${binding.msg} <-> ${binding.domain} > every Msg field is accounted for (per binding) | N/A-FRAMEWORK | — | Same |
| ${binding.msg} <-> ${binding.domain} > the binding table names fields that exist (per binding) | N/A-FRAMEWORK | — | Same |
| every domainOnly entry carries a reason | N/A-FRAMEWORK | — | Same |

(test/contract/dart_fields.dart and test/contract/msg_domain_bindings.dart contain no test cases — support files.)

## test/di/reminder_scheduler_wiring_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| hydration reminders use the batch scheduler on TargetPlatform.android / .iOS (x2) | N/A-FRAMEWORK | — | Riverpod DI wiring for the Flutter batch-notification scheduler; Kotlin uses AlarmManager one-shot reminders (controller-tested) |
| mindfulness reminders use the batch scheduler on TargetPlatform.android / .iOS (x2) | N/A-FRAMEWORK | — | Same |
| the two reminders use distinct notification id ranges | PORTED | ReminderSchedulerWiringTest.kt: `the two reminders use distinct notification id ranges` | — |

## test/l10n/app_localizations_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| all five app languages are supported | N/A-FRAMEWORK | — | Dart AppLanguage/gen-l10n; Kotlin ships locales as Android res (values-de/es/et/gl/it — also Galician) checked by the resource pipeline/lint |
| the app offers only SHIPPED locales > every shipped language, and nothing that has no AppLanguage constant | N/A-FRAMEWORK | — | Same |
| the app offers only SHIPPED locales > an in-progress locale is NOT offered, even once gen-l10n knows it | N/A-FRAMEWORK | — | Weblate in-progress-ARB gating is a Flutter-repo concept |
| English and German load with distinct translations | N/A-FRAMEWORK | — | Loads Dart AppLocalizations delegates |
| Spanish, Italian and Estonian also carry real translations | N/A-FRAMEWORK | — | Same |
| placeholder messages interpolate their arguments | N/A-FRAMEWORK | — | gen-l10n ICU interpolation; Android uses format strings validated by aapt/lint |

## test/l10n/verify_l10n_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the shipped catalogs > pass every check | N/A-FRAMEWORK | — | Entire file tests the Dart ARB verifier script; Kotlin l10n is Android XML resources validated by aapt/lint, no analog verifier |
| the shipped catalogs > every SHIPPED locale is above the coverage floor | N/A-FRAMEWORK | — | Same |
| the shipped catalogs > the locale ARBs are genuinely partial, not English back-fills | N/A-FRAMEWORK | — | Same |
| check 1: JSON and @@locale > rejects invalid JSON | N/A-FRAMEWORK | — | Same |
| check 1: JSON and @@locale > rejects an @@locale that disagrees with the filename | N/A-FRAMEWORK | — | Same |
| check 2: duplicate keys > catches a duplicate that jsonDecode would silently swallow | N/A-FRAMEWORK | — | Same |
| check 2: duplicate keys > is not fooled by a colon inside a string value | N/A-FRAMEWORK | — | Same |
| check 2: duplicate keys > is not fooled by a repeated key nested inside @-metadata | N/A-FRAMEWORK | — | Same |
| check 3: coverage > fails a locale below the floor | N/A-FRAMEWORK | — | Same |
| check 3: coverage > fails at EXACTLY the threshold (the bound is exclusive) | N/A-FRAMEWORK | — | Same |
| check 3: coverage > passes just above the threshold | N/A-FRAMEWORK | — | Same |
| check 4: stale keys > flags a locale key the template no longer has | N/A-FRAMEWORK | — | Same |
| check 5: placeholder-set equality > flags an ADDED placeholder (it widens the generated signature) | N/A-FRAMEWORK | — | Same |
| check 5: placeholder-set equality > flags a DROPPED placeholder (it renders a missing value) | N/A-FRAMEWORK | — | Same |
| check 5: placeholder-set equality > accepts a reordered but identical placeholder set | N/A-FRAMEWORK | — | Same |
| check 6: template self-consistency > flags a used placeholder that is not declared | N/A-FRAMEWORK | — | Same |
| check 6: template self-consistency > flags a declared placeholder that is not used | N/A-FRAMEWORK | — | Same |
| check 6: template self-consistency > flags a placeholder message with no @key metadata at all | N/A-FRAMEWORK | — | Same |
| check 6: template self-consistency > flags a dangling @key with no message | N/A-FRAMEWORK | — | Same |
| check 7: plurals > accepts a locale that adds categories English does not have | N/A-FRAMEWORK | — | Same |
| check 7: plurals > accepts a locale that drops a category English has | N/A-FRAMEWORK | — | Same |
| check 7: plurals > requires an "other" branch | N/A-FRAMEWORK | — | Same |
| check 7: plurals > rejects an invalid plural category | N/A-FRAMEWORK | — | Same |
| check 7: plurals > accepts the explicit =N form | N/A-FRAMEWORK | — | Same |
| check 7: plurals > flags a plural that the translation turned into a plain string | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > reports a stray opening brace with the file and key | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > reports a brace followed by something that is not an identifier | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > reports an unclosed placeholder | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > reports an unbalanced closing brace | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > accepts a formatted argument like {n, number} | N/A-FRAMEWORK | — | Same |
| check 8: ICU syntax > treats an apostrophe as a literal, matching use-escaping: false | N/A-FRAMEWORK | — | Same |
| check 9: @-prefixed keys in LOCALE files are inert > Weblate metadata does not count as a stale key or hurt coverage | N/A-FRAMEWORK | — | Same |
| check 10: picker <-> ARB agreement > flags an AppLanguage constant with no ARB (a silent no-op picker) | N/A-FRAMEWORK | — | Same |
| check 10: picker <-> ARB agreement > flags an AppLanguage constant with no autonym in the dropdown | N/A-FRAMEWORK | — | Same |
| check 10: picker <-> ARB agreement > the real picker and the real ARBs agree | N/A-FRAMEWORK | — | Same |
| in-progress locales > a 5% in-progress locale PASSES, and does not block the other locales | N/A-FRAMEWORK | — | Same |
| in-progress locales > an in-progress locale with a BROKEN placeholder still FAILS | N/A-FRAMEWORK | — | Same |
| in-progress locales > an in-progress locale keeps the parse-stage structural checks | N/A-FRAMEWORK | — | Same |
| in-progress locales > an in-progress locale keeps the catalog-stage structural checks | N/A-FRAMEWORK | — | Same |
| in-progress locales > a SHIPPED locale below the floor still FAILS | N/A-FRAMEWORK | — | Same |
| in-progress locales > an in-progress locale that crosses the floor is reported, not failed | N/A-FRAMEWORK | — | Same |
| in-progress locales > with no picker source, every locale is gated (the strict fallback) | N/A-FRAMEWORK | — | Same |

## test/navigation/csv_import_route_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the CSV import path sits under the data-import section | PORTED | CsvImportRouteTest.kt: `the CSV import path sits under the data-import section` | — |
| the router has a route registered for the CSV importer | N/A-FRAMEWORK | — | go_router configuration traversal; Kotlin NavHost registration is Compose wiring |

## test/navigation/metric_dispatch_test.dart
Kotlin counterpart: none (dispatch lives in app/src/main/kotlin/tech/mmarca/openvitals/navigation/AppNavigationMetricRoutes.kt `MetricRouteContent`, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| calories ids land on the calories aggregate, not the activity screen | PORTED | MetricDispatchTest.kt: `calories ids land on the calories aggregate, not the activity screen` | seam: the id->destination decision extracted from MetricRouteContent into the pure `metricRouteDestinationFor` |
| body ids land on the body aggregate, not a per-metric screen | PORTED | MetricDispatchTest.kt: `body ids land on the body aggregate, not a per-metric screen` | — |
| nutrition ids land on the per-metric nutrition screen | PORTED | MetricDispatchTest.kt: `nutrition ids land on the per-metric nutrition screen` | — |
| movement ids land on the activity metric screen | PORTED | MetricDispatchTest.kt: `movement ids land on the activity metric screen` | — |
| heart and vitals ids land on the heart metric screen | PORTED | MetricDispatchTest.kt: `heart and vitals ids land on the heart metric screen` | — |
| explicit tail: workout/sleep/hydration/caffeine/mindfulness/cycle | PORTED | MetricDispatchTest.kt: `explicit tail workout sleep hydration caffeine mindfulness cycle` | — |
| unknown ids fall back to the generic metric placeholder | PORTED | MetricDispatchTest.kt: `unknown ids fall back to the generic metric placeholder` | — |

## test/navigation/selected_day_location_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/navigation/SelectedDayArgTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a past day is pinned onto the location | PORTED | SelectedDayArgTest.kt: `a past day is appended as a query parameter` | — |
| today adds nothing — it is what the screens already do | PORTED | SelectedDayArgTest.kt: `today is omitted so the ordinary location stays clean` | — |
| it joins a location that already carries a query | PORTED | SelectedDayArgTest.kt: `joins an existing query string with an ampersand` | Kotlin adds SavedStateHandle parse tests on top |

## test/state/refresh_coordinator_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| expandDomains > a hydration write also marks nutrition, caffeine and calories stale | N/A-FRAMEWORK | — | Kotlin has no DataDomain/RefreshCoordinator; cross-feature refresh flows through repositories and per-ViewModel reloads |
| expandDomains > an activity write reaches calories, steps, body energy and achievements | N/A-FRAMEWORK | — | Same |
| expandDomains > a domain with no derived entries expands to just itself | N/A-FRAMEWORK | — | Same |
| expandDomains > an empty set expands to nothing | N/A-FRAMEWORK | — | Same |
| RefreshSignal.touches > the initial state is not a signal | N/A-FRAMEWORK | — | Same |
| RefreshSignal.touches > a signal for an unrelated domain does not touch a screen | N/A-FRAMEWORK | — | Same |
| RefreshSignal.touches > a signal overlapping one of several interests touches the screen | N/A-FRAMEWORK | — | Same |
| RefreshCoordinator > an app-open signal is emitted immediately and names every domain | N/A-FRAMEWORK | — | Riverpod StateNotifier mechanics |
| RefreshCoordinator > fifty writes inside the debounce window produce exactly one signal | N/A-FRAMEWORK | — | Closest Kotlin coalescing test: core/performance/DashboardLoadCoalescerTest.kt `concurrent callers share one dashboard load` (different mechanism) |
| RefreshCoordinator > a write burst longer than the max wait flushes without waiting for it to end | N/A-FRAMEWORK | — | Same |
| RefreshCoordinator > an empty domain set emits nothing | N/A-FRAMEWORK | — | Same |
| RefreshCoordinator > consecutive signals for the same domains are distinguishable by revision | N/A-FRAMEWORK | — | Same |
| RefreshCoordinator > an app-open signal supersedes a pending write burst | N/A-FRAMEWORK | — | Same |
| RefreshCoordinator > the debounce timer does not outlive the container | N/A-FRAMEWORK | — | ProviderContainer lifetime |
| RefreshCoordinator > the burst clock reads the injected clock, not the wall clock | N/A-FRAMEWORK | — | Dart `clock` package rule |

## test/state/week_period_mode_provider_test.dart
Kotlin counterpart: app/src/test/kotlin/tech/mmarca/openvitals/features/settings/SettingsViewModelTest.kt (partial)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| weekPeriodModeProvider follows the preference without a restart | DIVERGED | SettingsViewModelTest.kt (activityWeekMode set + uiState assertions) | Kotlin pins the preference write and settings state, but not live propagation to period-consuming screens (the Riverpod listenable-bridge regression) |

## test/widget_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders onboarding start screen when onboarding incomplete | PORTED | MainActivityStartDestinationTest: `rendersTheOnboardingStartScreenWhenOnboardingIsIncomplete` | Compose instrumentation; runs on a device, not in CI |
| renders dashboard start screen when onboarding complete | N/A-WIDGET | — | Same |
| a user who onboarded under an older permission set is sent back through onboarding | N/A-WIDGET | — | Kotlin's start gate has no permission-set-version check; re-prompting is handled by HealthConnectScreenUxCoordinator instead |
| the shell offers the SHIPPED locales, not every ARB present | PORTED | ShippedLanguagesTest (4 tests over the generated translation_picker_language_tags) | Compose instrumentation; runs on a device, not in CI |

## Summary

| Status | Count |
|---|---|
| PORTED | 715 |
| DIVERGED | 77 |
| MISSING | 12 |
| N/A-WIDGET | 10 |
| N/A-FRAMEWORK | 225 |
| N/A-BEHAVIOR | 14 |
| **Total cases** | **1053** |

### Portable gaps

None. Every remaining MISSING case is listed below as blocked on a behavior decision;
the other non-ported cases carry N/A-BEHAVIOR / N/A-WIDGET / N/A-FRAMEWORK with the
reason in their Note column.

### Blocked on a behavior decision

Kotlin behaves differently from Flutter here, or the seam the Flutter test
Rows reclassified during the latest porting wave carry N/A-BEHAVIOR with a
"blocked on behavior decision" note in place, rather than being repeated here:
the watch-fit watermark use case, the history-sync drain isolation and re-drain,
the Flutter-side anti-clobber migration guard, the reminder-apply serialization,
the mid-load display clear, and the unsupported-write-permission filter.

needs does not exist in the Kotlin port. The Kotlin behavior was left alone.

- **test/data/repository/dashboard/dashboard_data_loader_test.dart**
  - bounds how many metric reads run at once [MISSING]
  - body energy timeline > populates the timeline when set up and heart-rate is granted [DIVERGED]
  - body energy timeline > skips the load when calibration is not set up [DIVERGED]
  - body energy timeline > skips the load when heart-rate read is not granted [DIVERGED]
- **test/domain/model/activity_backfill_test.dart**
  - route backfill fills missing distance and elevation [DIVERGED]
  - session-metrics backfill fills the totals the session never carried [MISSING]
  - session-metrics backfill preserves what the session did record [MISSING]
  - an ungranted or unrecorded metric stays missing, never zero [MISSING]
- **test/data/source/health/health_connect_native_data_source_test.dart**
  - availability > resolveSupportedPermissions drops permissions the provider does not recognize from every set [MISSING]
  - permissions > requestPermissions returns false for an empty set [MISSING]
  - permissions > requestPermissions never forwards an unsupported permission [MISSING]
  - permissions > a request of nothing BUT unsupported permissions never reaches the plugin [MISSING]
  - readRawActivityProgress > a metric the device never reports stays null, not a zero line [DIVERGED]
  - elevation + wheelchair aggregates > stay null when the device records neither [DIVERGED]
  - planned exercise sessions > an unsupported provider reads empty and refuses to write [MISSING]
- **test/data/source/health/health_permissions_test.dart**
  - feature gating > the device answer and the opt-in are separate flags [DIVERGED]
- **test/data/repository/heart_repository_impl_test.dart**
  - loadHeartRateSamplesInstant > asks for exactly the window it was given [DIVERGED]
  - loadHeartRateSamplesInstant > returns the samples the native reader found [DIVERGED]
- **test/core/diagnostics/debug_log_sanitizer_test.dart**
  - DebugLogSanitizer.buildExportText > emits the header block then the sanitized lines [MISSING]
