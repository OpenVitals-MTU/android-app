# Flutter → Kotlin test-parity matrix: ui / goldens / integration / fixtures

The device sections this file opened with (GFDI, notifications, watch sync, Wear OS) left this repository with the device layer, which is now OpenWatch. The summary counts below predate that and still include those rows.

Legend: PORTED = same logic + equivalent assertions (byte fixtures identical unless noted). DIVERGED = covered but weaker/different. MISSING = no Kotlin coverage and JVM-portable. N/A-WIDGET = widget/golden rendering. N/A-FRAMEWORK = Flutter-only plumbing. BLOCKED = the Kotlin behaviour genuinely differs, so porting the case would mean changing production code; see "Blocked on a behavior decision" at the end.

## test/ui/charts/bar_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/PeriodChartTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week chart draws every day until it is pinched | PORTED | ChartPinchAxisTest: `aWeekBarChart_drawsEveryDayUntilItIsPinched` | Compose instrumentation; runs on a device, not in CI |
| tapping a bar on a ZOOMED chart selects the day under the finger | PORTED | PeriodChartTest: `a tap selects the bucket it lands on, zoomed or not` | - |

## test/ui/charts/bar_label_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/BarLabelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a label that fits is drawn at full size | PORTED | BarLabelTest: `a label that fits is drawn at full size` | Kotlin measures at ONE size, so "full size" is the only size; the fits-therefore-drawn half is identical |
| a label that does not fit SHRINKS — it does not vanish | BLOCKED | - | Kotlin measures at one size and drops on overflow — there is no shrink-to-fit ladder to assert |
| the longer the number, the smaller it is drawn — but it is drawn | BLOCKED | - | Same one-size measurement: no step-down exists in Kotlin |
| the unit goes on its own line, so the number keeps the room | PORTED | BarLabelTest: `the unit goes on its own line, so the number keeps the room` | `splitBarValueLabel` also strips the grouping separator — a deliberate divergence from Dart's ['21,104','steps'] |
| a label nobody could read is still dropped | PORTED | BarLabelTest: `a label nobody could read is still dropped` | Overflow, blank and zero-width cases |

## test/ui/charts/chart_curve_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartCurveTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a rising series never dips on the way up | PORTED | ChartCurveTest: `a rising series never dips on the way up` | samples Bézier segments instead of walking a Path; same inputs/assertion |
| a flat run stays flat | PORTED | ChartCurveTest: `a flat run stays flat` | - |
| never overshoots below the lowest sample | PORTED | ChartCurveTest: `never overshoots beyond the extreme samples` | - |
| draws a vertical riser straight rather than looping through it | PORTED | ChartCurveTest: `draws a vertical riser straight rather than looping through it` | Kotlin additionally asserts the segment is a Line |
| degenerate inputs do not throw | PORTED | ChartCurveTest: `degenerate inputs yield no segments and do not throw` | - |
| — | KOTLIN-ONLY | ChartCurveTest: `does not overshoot a trough when the next sample is far away` | uneven x spacing: the Fritsch-Carlson sign-change rule was missing and a skin temperature day overshot its low |

## Kotlin-only: app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/MetricLineChartFrameTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| — | KOTLIN-ONLY | MetricLineChartFrameTest: `points outside the period and values that are not numbers are left out of the range`, `nothing in the period, or a day with one instant, draws no line`, `a point sits in the middle of its day's slot, and the highest value at the top` | what the line chart works out once per data change |
| — | KOTLIN-ONLY | MetricLineChartFrameTest: `a series narrower than one unit still fills its axis` | the y range was floored at one unit; a ±0.3 degree day sat under its own labels |
| — | KOTLIN-ONLY | MetricLineChartFrameTest: `the padded axis keeps the line charts' own rule`, `a zero-centred axis is symmetric, so the middle label is zero`, `a zero-centred axis gives a flat zero series room` | `LineAxisRange`: Padded is the old rule, ZeroCentred is for a signed variation |

## test/ui/charts/chart_decimation_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartDecimationTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| decimateOffsets > returns the same list unchanged when already at or below target | PORTED | ChartDecimationTest: `returns the same list unchanged when already at or below target` | - |
| decimateOffsets > does not downsample when target is degenerate | PORTED | ChartDecimationTest: `does not downsample when target is degenerate` | - |
| decimateOffsets > reduces to exactly the target count | PORTED | ChartDecimationTest: `reduces to exactly the target count` | - |
| decimateOffsets > keeps the first and last point | PORTED | ChartDecimationTest: `keeps the first and last point` | - |
| decimateOffsets > preserves an isolated peak (LTTB keeps extremes) | PORTED | ChartDecimationTest: `preserves an isolated peak because LTTB keeps extremes` | - |

## test/ui/charts/chart_scrubber_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartScrubberTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a VERTICAL drag starting on the chart still scrolls the page | PORTED | ChartScrubberGestureTest: `aVerticalDragStartingOnTheChart_stillScrollsThePage` | Compose instrumentation; runs on a device, not in CI |
| a HORIZONTAL drag reads the chart | PORTED | ChartScrubberGestureTest: `aHorizontalDragReadsTheChart` | Compose instrumentation; runs on a device, not in CI |
| it snaps to the nearest SAMPLE, never between two | PORTED | ChartScrubberTest: `snaps to the nearest target by x` + `a tie keeps the first target` | - |
| it stands down while a pinch is in progress | PORTED | ChartScrubberGestureTest: `itStandsDownWhileAPinchIsInProgress` | Compose instrumentation; runs on a device, not in CI |
| a chart with nothing to say stays inert | PORTED | ChartScrubberGestureTest: `aChartWithNothingToSayStaysInert` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/chart_skeleton_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the skeleton settles when motion is off | PORTED | ChartMotionTest: `theSkeletonSettlesWhenMotionIsOff` | Compose instrumentation; runs on a device, not in CI |
| the skeleton does animate when motion is on | PORTED | ChartMotionTest: `theSkeletonDoesAnimateWhenMotionIsOn` | Compose instrumentation; runs on a device, not in CI |
| the reveal is fully drawn on the first frame when motion is off | PORTED | ChartMotionTest: `theRevealIsFullyDrawnOnTheFirstFrameWhenMotionIsOff` | Compose instrumentation; runs on a device, not in CI |
| the reveal animates from nothing when motion is on | PORTED | ChartMotionTest: `theRevealAnimatesFromNothingWhenMotionIsOn` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartViewportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| ChartViewport > starts as the whole chart | PORTED | ChartViewportTest: `starts as the whole chart` | - |
| ChartViewport > zooming keeps the point between the fingers under the fingers | PORTED | ChartViewportTest: `zooming keeps the point between the fingers under the fingers` | - |
| ChartViewport > a point outside the window is NOT clamped into it | PORTED | ChartViewportTest: `a point outside the window is NOT clamped into it` | - |
| ChartViewport > panning moves the data under the finger by the distance dragged | PORTED | ChartViewportTest: `panning moves the data under the finger by the distance dragged` | - |
| ChartViewport > the window stops at the ends rather than sliding off | PORTED | ChartViewportTest: `the window stops at the ends rather than sliding off` | - |
| ChartViewport > there is a floor on how far you can zoom in | PORTED | ChartViewportTest: `there is a floor on how far you can zoom in` | - |
| ChartViewport > zooming back out never overshoots the whole chart | PORTED | ChartViewportTest: `zooming back out never overshoots the whole chart` | - |
| ChartZoom > two fingers pinching apart zooms in | PORTED | ChartZoomGestureTest: `twoFingersPinchingApart_zoomsIn` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > two fingers zoom even inside a scrolling page | PORTED | ChartZoomGestureTest: `twoFingersZoom_evenInsideAScrollingPage` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > ONE finger dragging horizontally does not zoom | PORTED | ChartZoomGestureTest: `oneFingerDraggingHorizontally_doesNotZoom` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > the page still scrolls when dragged from inside a chart | PORTED | ChartZoomGestureTest: `thePageStillScrolls_whenDraggedFromInsideAChart` | Compose instrumentation; runs on a device, not in CI |
| ChartZoom > double tap returns the whole chart | PORTED | ChartZoomGestureTest: `doubleTap_returnsTheWholeChart` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/charts_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/PeriodChartTest.kt (plus ui/components/PeriodHeatmapTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SparklineChart renders | PORTED | ChartsRenderTest: `sparkline_renders` | Compose instrumentation; needs a device, so not run in CI |
| SparklineChart with a single value renders | PORTED | ChartsRenderTest: `sparkline_withASingleValue_renders` | Compose instrumentation; needs a device, so not run in CI |
| PeriodBarChart (week) renders bars and axis | PORTED | ChartsRenderTest: `periodBarChart_week_rendersBarsAndAxis` | Compose instrumentation; needs a device, so not run in CI |
| PeriodBarChart tap selects a day | PORTED | ChartsRenderTest: `periodBarChart_tapSelectsADay` | Compose instrumentation; needs a device, so not run in CI |
| PeriodHistoryChart month renders a calendar heatmap | PORTED | ChartsRenderTest: `periodHistoryChart_month_rendersACalendarHeatmap` | Compose instrumentation; needs a device, so not run in CI |
| PeriodHistoryChart year renders a year heatmap | PORTED | ChartsRenderTest: `periodHistoryChart_year_rendersAYearHeatmap` | Compose instrumentation; needs a device, so not run in CI |
| periodYearHeatmapCells (rolling) spans across the calendar-year edge | BLOCKED | - | Kotlin's `periodYearHeatmapCells` has no `rolling` parameter — the year heatmap always draws the calendar year of `period.start` |
| periodYearHeatmapCells (calendar) draws Jan 1 to Dec 31 | PORTED | PeriodHeatmapTest: `year heatmap includes leap day` (+ `year heatmap includes each day in the year and aggregates values by date`) | - |
| MetricBarChart builds its summary from the period title | PORTED | ChartsRenderTest: `metricBarChart_buildsItsSummaryFromThePeriodTitle` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart (week) renders line + axis | PORTED | ChartsRenderTest: `metricLineChart_week_rendersLineAndAxis` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart (day) renders time axis with distinct times | PORTED | ChartsRenderTest: `metricLineChart_day_rendersATimeAxisWithDistinctTimes` | Compose instrumentation; needs a device, so not run in CI |
| MetricLineChart renders nothing when there are no points | PORTED | ChartsRenderTest: `metricLineChart_rendersNothingWhenThereAreNoPoints` | Compose instrumentation; needs a device, so not run in CI |
| PeriodChartXAxis renders its labels | PORTED | ChartsRenderTest: `periodChartXAxis_rendersItsLabels` | Compose instrumentation; needs a device, so not run in CI |
| charts render in dark theme | PORTED | ChartsRenderTest: `charts_renderInDarkTheme` | Compose instrumentation; needs a device, so not run in CI |
| formatCompactAxisValue formats compactly | PORTED | PeriodChartTest: `compact y axis values abbreviate large numbers` | All four Dart cases: 0, 12, 1.5k, 2M |

## test/ui/charts/day_axis_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| DayAxis > places a time at its real hour, not at its share of the elapsed day | PORTED | ChartTimeAxesTest: `places a time at its real hour, not at its share of the elapsed day` | Same 12:49 / 09:29 → 0.395 regression fixture, against `axisFractionOf` over a whole day |
| DayAxis > spans the whole day, so the labels under it are true | PORTED | ChartTimeAxesTest: `the day axis spans the whole day, so the labels under it are true` | Same 0 / 0.25 / 0.5 / 0.75 quarter grid |
| DayAxis > today's series stops at now, rather than claiming the rest of the day | PORTED | ChartTimeAxesTest: `today's series stops at now, rather than claiming the rest of the day` | Against the new `dayEndFraction` seam; same 12:00 → 0.5 and 06:00 → 0.25 |
| DayAxis > a past day runs to its right edge | PORTED | ChartTimeAxesTest: `a past day runs to its right edge` | Same next-morning-04:00 clock, isToday false and endFraction 1 |
| DayAxis > clamps a time from outside the day onto it | PORTED | ChartTimeAxesTest: `clamps a time from outside the day onto it` | Same previous-day 22:00 → 0 and next-day 02:00 → 1 |
| DayAxis > honours the injected clock rather than the wall clock | PORTED | ChartTimeAxesTest: `honours the injected clock rather than the wall clock` | `isDayToday` takes the clock; also pins that midnight opens the day and the next midnight does not |
| DayAxisLabels > reads midnight to midnight | PORTED | ChartTimeAxesTest: `full viewport gives the classic five hour labels` | - |
| DayAxisLabels > starts where the plot starts, not where the card does | PORTED | ChartAxisLayoutTest: `dayAxisLabels_startWhereThePlotStarts_notWhereTheCardDoes` | Compose instrumentation; runs on a device, not in CI |
| DayAxisLabels > a painter with no y axis can opt out | PORTED | ChartAxisLayoutTest: `dayAxisLabels_aPainterWithNoYAxisCanOptOut` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/day_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| dayAxisLabelsFor > the whole day is the five labels it always was | PORTED | ChartTimeAxesTest: `full viewport gives the classic five hour labels` | - |
| dayAxisLabelsFor > zoomed in, the row says the hours actually on the plot | PORTED | ChartTimeAxesTest: `zoomed viewport labels the hours actually under the plot` (+ `zoomed labels round to whole minutes`) | different viewport values, identical rule |
| pinching a day chart zooms the plot AND its hours | PORTED | ChartPinchAxisTest: `pinchingADayChart_zoomsThePlotAndItsHours` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/heatmap_cells_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/components/PeriodMonthHeatmapCellsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| periodMonthHeatmapCells > a calendar month renders the whole month, greying past the window | PORTED | PeriodMonthHeatmapCellsTest: `a calendar month draws the whole month` + `a calendar month greys the days outside the loaded window on both sides` | - |
| periodMonthHeatmapCells > a rolling window spans both months and keeps every day | PORTED | PeriodMonthHeatmapCellsTest: `a rolling window keeps the values on both sides of the month boundary` | Same 21 Jun – 20 Jul window and the same 1800 / 2200 values either side of the boundary |

## test/ui/charts/metric_day_chart_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt, ChartRangeTest.kt, BucketedSeriesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| DaySeriesShape > raw plots the readings and invents nothing | PORTED | ChartTimeAxesTest: `raw plots the readings and invents nothing` | Same 06:00/18:00 → 0.25/0.75 two-point fixture; no midnight anchor, no trailing hold |
| DaySeriesShape > cumulative anchors at midnight and holds the total to the end | PORTED | ChartTimeAxesTest: `cumulative shape anchors at zero and plateaus out to the end fraction` | - |
| DaySeriesShape > cumulative on today stops at now, not at the right edge | PORTED | ChartTimeAxesTest: `cumulative on today stops at now, not at the right edge` | endFraction now derived from `now` through `dayEndFraction`; same 400 held to 0.5 |
| DaySeriesShape > every shape survives an empty day | PORTED | ChartTimeAxesTest: `every shape survives an empty day` | Both shapes, not just the cumulative one |
| ChartRange.padded > clears the data without dropping below the floor | PORTED | ChartRangeTest: `the floor stops the padding from dipping below it` (+ `pads by the given fraction of the span`) | Kotlin pins exact values rather than inequalities |
| ChartRange.padded > a flat series still gets an axis to breathe in | PORTED | ChartRangeTest: `a flat series pads against the value's own magnitude` (+ `a flat series below one pads against a unit basis`) | - |
| ChartRange.padded > an empty series does not divide by nothing | PORTED | ChartRangeTest: `an empty series falls back to a unit range` | - |
| MetricDayChart > an empty day says so, and draws no plot | PORTED | IntradayActivityChartCardTest: `anEmptyDaySaysSoAndDrawsNoPlot` | Compose instrumentation; runs on a device, not in CI |
| MetricDayChart > a day with readings draws the plot and the hour row | PORTED | IntradayActivityChartCardTest: `aDayWithReadingsDrawsThePlotAndTheHourRow` | Compose instrumentation; runs on a device, not in CI |
| MetricDayChart > sorts the samples it is handed | BLOCKED | - | Kotlin's `rawDayPlotPoints` plots in caller order; only the bucketed branch sorts, so the guarantee does not exist to assert |
| MetricDayChart > the header and footer slots replace the defaults | N/A-WIDGET | - | slot composition |
| MetricDayChart > off draws the raw readings and no band | DIVERGED | ChartTimeAxesTest: `raw plots the readings and invents nothing`; BucketedSeriesTest: `non-positive bucket width yields no buckets` | The point list is pinned; the band-is-empty half lives in the composable's non-bucketed branch |
| MetricDayChart > aggregated buckets the readings into an average line with a band | PORTED | BucketedSeriesTest: `computes average, min and max per bucket` + `splits samples into separate buckets and orders them by time` | - |

## test/ui/charts/metric_line_chart_zoom_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| pinching a day MetricLineChart zooms the line and its hours | PORTED | ChartPinchAxisTest: `pinchingADayChart_zoomsThePlotAndItsHours` | Compose instrumentation; runs on a device, not in CI |
| pinching a year MetricLineChart zooms it too | PORTED | ChartPinchAxisTest: `pinchingAYearLineChart_zoomsItsDateRowToo` | Compose instrumentation; runs on a device, not in CI |
| a period chart still pinches when one finger moves before the other lands (real-device timing) | PORTED | ChartZoomGestureTest: `aPinchStillLands_whenOneFingerMovesBeforeTheOther` | Compose instrumentation; runs on a device, not in CI |
| switching the year resets a zoom rather than carrying it over | PORTED | ChartZoomGestureTest: `changingTheChartsKey_resetsTheZoomRatherThanCarryingItOver` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/session_axis_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| SessionAxis > places a sample at its elapsed position in the session | PORTED | ChartTimeAxesTest: `places a sample at its elapsed position in the session` | Same 0 / 0.25 / 0.5 / 1 quarter grid |
| SessionAxis > spans the recorded session, not the samples that exist | PORTED | ChartTimeAxesTest: `spans the recorded session, not the samples that exist` | Same twenty-minutes-into-an-hour → 1/3 regression |
| SessionAxis > clamps a sample from outside the session onto it | PORTED | ChartTimeAxesTest: `clamps a sample from outside the session onto it` | Asserted on SessionAxis itself |
| SessionAxis > a zero-length session does not divide by zero | PORTED | ChartTimeAxesTest: `a zero length session still has a positive axis` | - |
| SessionAxis > labels the quarters in elapsed time | PORTED | ChartTimeAxesTest: `elapsed labels at full zoom span the whole session` | - |
| SessionAxis with pauses > the axis is moving time, not wall-clock | PORTED | ChartTimeAxesTest: `duration is the moving time` | - |
| SessionAxis with pauses > the fixes either side of a pause end up next to each other | PORTED | ChartTimeAxesTest: `an instant inside a pause resolves to the moment the pause began` | - |
| SessionAxis with pauses > what was recorded DURING a pause sits where the pause began | PORTED | ChartTimeAxesTest: `an instant inside a pause resolves to the moment the pause began` | - |
| SessionAxis with pauses > the scrubber and the labels agree, both in moving time | PORTED | ChartTimeAxesTest: `the scrubber and the labels agree, both in moving time` | Paused axis: elapsedAt(0.5) == 15 min and the same 0:00/7:30/15:00/22:30/30:00 row |
| SessionAxis with pauses > overlapping pauses are counted once | PORTED | ChartTimeAxesTest: `overlapping pauses are merged so shared time is not subtracted twice` | - |
| SessionAxis with pauses > a pause reaching outside the session is clipped to it | PORTED | ChartTimeAxesTest: `pauses are clipped to the session` | - |
| SessionAxis with pauses > an entirely paused session does not divide by zero | PORTED | ChartTimeAxesTest: `an entirely paused session does not divide by zero` | Same durationMs 1 and mid-session fraction 0 |
| SessionAxisLabels starts where the plot starts | PORTED | ChartAxisLayoutTest: `sessionAxisLabels_startWhereThePlotStarts` | Compose instrumentation; runs on a device, not in CI |

## test/ui/charts/session_chart_zoom_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| elapsedLabelsFor > the whole session is the five labels it always was | PORTED | ChartTimeAxesTest: `elapsed labels at full zoom span the whole session` | - |
| elapsedLabelsFor > zoomed in, the row says the minutes actually on the plot | PORTED | ChartTimeAxesTest: `elapsed labels under zoom describe the visible slice` | 0.5..1 slice vs 0.75..1; same rule |
| pinching a session chart zooms the trace AND its elapsed row | PORTED | ChartPinchAxisTest: `pinchingASessionChart_zoomsTheTraceAndItsElapsedRow` | Compose instrumentation; runs on a device, not in CI |

## test/ui/components/health_connect_gate_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectFeatureTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows the access gate when Health Connect is unavailable | BLOCKED | HealthConnectFeatureTest: `unavailable Health Connect bypasses the access gate and surfaces availability on the state` | Kotlin deliberately routes unavailability out of band (state.availability + the dashboard promo) instead of a blocking gate; the Kotlin test pins that routing |
| shows the child when available and permitted | PORTED | HealthConnectFeatureTest: `shows the child when available and permitted` | Pass-through: no gate mode, no prompt, nothing missing |
| shows the permission gate when a required permission is missing | BLOCKED | HealthConnectFeatureTest: `a missing required permission raises the contextual prompt over a still-visible child` | Kotlin raises a contextual prompt with the child still shown rather than a blocking gate; the Kotlin test pins that divergence |
| shows the sync-paused gate when sync is disabled | PORTED | HealthConnectFeatureTest: `buildStateShowsAccessGateWhenSyncPaused` | - |
| shows a recoverable error (not a permanent spinner) when a read throws | N/A-FRAMEWORK | - | Riverpod AsyncValue error branch |

## test/ui/components/metric_card_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/ui/components/MaterialUxComponentsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| MetricCard shows title, value, unit, subtitle and source | PORTED | MetricCardTest: `metricCard_showsTitleValueUnitSubtitleAndSource` | Compose instrumentation; needs a device, so not run in CI |
| MetricCard onTap fires | PORTED | MetricCardTest: `metricCard_onClickFires` | Compose instrumentation; needs a device, so not run in CI |
| MetricCardPlaceholder shows its message | PORTED | MetricCardTest: `metricCardPlaceholder_showsItsMessage` | Compose instrumentation; needs a device, so not run in CI |
| TimeRangeSelector renders all ranges and reports selection | PORTED | MaterialUxComponentsTest: `timeRangeSelector_reportsSegmentedSelection` | androidTest; now asserts every range renders, not just the tapped one |
| SectionHeader renders its text | PORTED | MetricCardTest: `sectionHeader_rendersItsText` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/components/metric_detail_scaffold_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffoldTest.kt (plus core/period/PeriodNavigatorTest.kt, PeriodSelectionDriverSelectDayTest.kt, PeriodLoadQueryTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders header, range selector and content for the period | PORTED | MetricDetailScaffoldHeaderTest: `rendersTheHeaderSlotTheRangeLabelsAndTheContent` | Compose instrumentation; runs on a device, not in CI |
| selecting a range drives the navigator + content | PORTED | MetricDetailScaffoldHeaderTest: `selectingARangeReportsItAndRetitlesTheNavigator`, `aDayRangeAnchoredOnTodayNamesItToday` | Compose instrumentation; runs on a device, not in CI |
| provides a day opener that drills into that day's Day view | PORTED | PeriodSelectionDriverSelectDayTest: `selectDay switches to the day range anchored on the tapped date` (+ `selectDay pins a past day so resuming does not bounce back to today`) | Kotlin tests the driver directly rather than a scaffold-provided opener |
| tapping next past today is a no-op (forward-capped) | PORTED | PeriodNavigatorTest: `periodFor end is never after today`; PeriodLoadQueryTest: `selection driver persists range and clamps next period` | The cap is the whole behaviour; the greyed-out Next button is Compose-only |
| rolling dates retitle the week/month/year periods | PORTED | PeriodLoadQueryTest (rolling windows) + PeriodTitleTest: `rollingPeriodTitlesUseFixedDayWindowLabels` | "Last 7/30/365 days" pinned at the title layer, plus the dated-span fallback once the window stops ending today |
| changing the week mode reloads the selection and retitles | PORTED | PeriodSelectionDriverWeekModeTest: `changing the week mode reloads the selection and retitles` (+ `flipping the week mode back re-derives the calendar week for the same anchor`) | - |
| renders the error block from a ScreenError | PORTED | MetricDetailScaffoldTest: `metricDetailScaffold_displaysErrorMessage` | androidTest |
| reserves the system navigation bar below the last item | N/A-WIDGET | - | edge-to-edge inset padding |
| reserves nothing extra when the bar is a gesture sliver | N/A-WIDGET | - | - |

## test/ui/components/metric_detail_scaffold_refresh_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a signal for the screen own domain refreshes it once | N/A-FRAMEWORK | - | Kotlin has no RefreshCoordinator/DataDomain mechanism; screens reload via their own VM lifecycle |
| a signal for an unrelated domain does not refresh the screen | N/A-FRAMEWORK | - | no DataDomain type exists in Kotlin main to match against |
| a derived domain refreshes the screen that reads it | N/A-FRAMEWORK | - | no hydration→nutrition refresh derivation exists in Kotlin main |
| an app-open signal refreshes the screen | N/A-FRAMEWORK | - | no app-open refresh signal exists in Kotlin main |
| a screen with no declared domains never refreshes | N/A-FRAMEWORK | - | no refresh-domain declaration exists in Kotlin main |
| a signal arriving while a detail route is pushed on top defers the refresh until it is popped | N/A-FRAMEWORK | - | RouteObserver visibility plumbing |
| several signals while covered collapse into one refresh on pop | N/A-FRAMEWORK | - | nearest analogue is DashboardLoadCoalescerTest `concurrent callers share one dashboard load`, a different mechanism |
| a plain back navigation with nothing changed does not refresh | N/A-FRAMEWORK | - | - |
| an app-open signal refreshes only the visible screen, not every mounted one | N/A-FRAMEWORK | - | - |

## test/ui/components/permission_callout_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| PermissionCallout shows title, body and grant action | PORTED | PermissionCalloutTest: `showsTitleBodyAndGrantAction_withNoDismissUntilOneIsGiven` | Compose instrumentation; needs a device, so not run in CI |
| PermissionCallout shows dismiss when provided | PORTED | PermissionCalloutTest: `showsDismissWhenProvided_andHonoursACustomActionLabel` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/components/swipe_to_delete_entry_row_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| end-to-start swipe deletes | PORTED | SwipeToDeleteEntryRowTest: `endToStartSwipe_deletes` | Compose instrumentation; needs a device, so not run in CI |
| start-to-end swipe does nothing (Kotlin disables it) | PORTED | SwipeToDeleteEntryRowTest: `startToEndSwipe_doesNothing` | Compose instrumentation; needs a device, so not run in CI |

## test/ui/text_scaling_sweep_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| dashboard screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `dashboardScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| sleep screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `sleepScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| hydration screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `hydrationScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| activities screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `activitiesScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| body screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `bodyScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| mindfulness screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `mindfulnessScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| heart vitals overview screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `heartVitalsOverviewScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |
| settings screen survives 2.0 text scale | PORTED | TextScalingSweepTest: `settingsScreenSurvivesTheLargestFontScale` | Compose instrumentation; runs on a device, not in CI |

## test/integration/activity_pipeline_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (plus SwallowingRecordTest.kt, domain/insights/ActivitySplitsTest.kt, domain/model/ExerciseTypeTraitsTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a workout gets the heart rate that lived inside a 17-hour record | PORTED | FixtureReaderTest: `but the real reader finds it anyway` (+ `Health Connect hides that workout's heart rate from a windowed read`, SwallowingRecordTest: `a workout buried inside a 17 hour record still has a heart rate`) | Same byte-identical golden.json; Kotlin additionally bounds every returned sample to the workout window |
| a GPS session gets REAL splits, not evenly-estimated ones | DIVERGED | FixtureReaderTest: `speed samples survive the same trap, which is what the splits ride on` | Fixture speed samples are only proven to reach the reader; no Kotlin test feeds them into `computeActivitySplits` to assert `source != ESTIMATED` |
| a strength session gets NO splits, however much GPS drift it picked up | PORTED | ActivitySplitsTest: `a strength session with GPS drift is not cut into laps` + `a run with the same distance IS cut, so the gate is on the KIND`; ExerciseTypeTraitsTest: `a strength session does not travel, whatever GPS drift says` | Kotlin asserts unconditionally on synthetic sessions instead of the fixture's real session (Flutter's assert is guarded by an `if`) |
| sessions keep the provenance that decides dedup and the manual count | PORTED | FixtureReaderTest: `every record keeps the provenance the Pigeon messages kept dropping` + `sessions come from more than one writer, so dedup has something to do` | Kotlin also asserts zone offset and writer, over the same fixture window |

## test/integration/degraded_device_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectAvailabilityServiceTest.kt (partial; permission degradation spread across data/repository/*Test.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a missing Health Connect is reported as unavailable, not as an error | DIVERGED | HealthConnectAvailabilityServiceTest: `availability is not supported in unsupported profiles` | NOT_SUPPORTED is only reached via the work-profile path; no case for `SDK_UNAVAILABLE` (status 1) |
| an out-of-date provider is distinguishable from a missing one | PORTED | HealthConnectAvailabilityServiceTest: `availability needs provider update when Health Connect reports update required` | - |
| a read without its permission returns EMPTY, and does not throw | DIVERGED | VitalsRepositoryTest: `missing permission skips the daily and latest reads for that metric`; MindfulnessRepositoryTest: `loadMindfulnessSessions skips Health Connect when feature permissions are unavailable`; CycleRepositoryTest: `loadCycleData skips reads when no cycle permissions are granted` | Asserted per-repository against a mocked HealthConnectManager, never through the real reader with a heart read |
| holding ONLY the sleep permission still loads sleep, and nothing else | DIVERGED | SleepRepositoryTest: `loadSleepPeriod includes Health Connect aggregate sleep durations` (granted set = sleep only) | No assertion that a missing sibling category leaves the screen/state error-free |
| the permission set the app asks for is the one the device supports | PORTED | HealthConnectPermissionServiceTest: `the permission set the app asks for is the one the device supports` | Same assertions (non-empty, >10, read sleep, read exercise) against the resolved taxonomy; the runtime grant diff needs a device |
| sync paused degrades reads to empty rather than failing | DIVERGED | HealthConnectFeatureTest: `buildStateShowsAccessGateWhenSyncPaused` | Asserts the UX gate mode only; nothing exercises a read with `syncGate.isEnabled == false` |

## test/integration/repositories_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (fixture-backed half only; rest is mocked repository tests)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| health > availability resolves through the real repository | DIVERGED | HealthConnectAvailabilityServiceTest: `availability remains available on Android 14 even when Play Store is missing` | Service-level with mocked providers, not the repository resolving against a seeded client |
| health > the granted set is the app's permission taxonomy, not a stub | PORTED | HealthConnectPermissionServiceTest: `the permission set the app asks for is the one the device supports` | Same size and membership assertions on the app's own taxonomy |
| sleep > every night in the fixture comes back, with its stages | PORTED | FixtureReaderTest: `every night in the fixture comes back, with its stages` | Real SleepHealthReader over the seeded fixture; same not-empty and stages-survive assertions |
| sleep > two writers on one night are merged into one | PORTED | FixtureReaderTest: `two writers on one night are merged into one` | The fixture's own two-writer night, read back through SleepHealthReader and collapsed to one session |
| heart > the day of the swallowing record has heart rate | PORTED | FixtureReaderTest: `but the real reader finds it anyway` | Window is the swallowed workout rather than the whole local day |
| heart > daily summaries carry min, max and average — not just average | DIVERGED | HeartRepositoryTest: `WEEK average heart rate uses daily aggregate summaries without raw day samples`; HeartVitalsRangeSummaryTest: `resting heart rate summary reports the real average and range` | Routing verified against an empty mock; min ≤ max asserted only on synthetic presentation data |
| activity > every session in the fixture survives the trip, with its writer | PORTED | FixtureReaderTest: `sessions come from more than one writer, so dedup has something to do` | Now asserts the count equals the fixture's own session count as well as the writer multiplicity |
| activity > the GPS session keeps its route points | PORTED | FixtureReaderTest: `the GPS session keeps its route points` | HcFixture now rebuilds the 954-point route onto the record; the session is matched on its boundary because the fake re-stamps ids |
| activity > speed samples reach the repository, which is what splits ride on | PORTED | FixtureReaderTest: `speed samples survive the same trap, which is what the splits ride on` | Same fixture; Kotlin also bounds samples to the window |
| hydration > the fixture's hydration entries come back | PORTED | FixtureReaderTest: `the fixture's hydration entries come back` | HcFixture gained a hydration loader; all 5 entries and their total volume come back through HydrationHealthReader |

## test/integration/use_cases_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/domain/usecase/ (only LoadSleepPeriodUseCaseTest.kt, LoadHeartPeriodUseCaseTest.kt, LoadDashboardDayUseCaseTest.kt exist; the other domains have no use-case layer in Kotlin — coverage is via repository/VM tests over mocks)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| sleep > day returns over real data | DIVERGED | LoadSleepPeriodUseCaseTest: `loads sleep period and daily hrv in parallel windows` | Mocked repositories, one query, no fixture corpus |
| sleep > week returns over real data | DIVERGED | SleepViewModelTest: `initial range is WEEK` | Range only reached via period math, never a load asserted over data |
| sleep > month returns over real data | DIVERGED | SleepViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| sleep > year returns over real data | DIVERGED | SleepViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| sleep > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | SleepViewModelTest: `initial sessions list is empty when repo returns nothing` | VM-level, and no assertion that the empty case is error-free |
| sleep > a forced refresh returns the same shape as a normal load | DIVERGED | LoadSleepPeriodUseCaseTest: `force refresh passes refresh mode to sleep repository` | Verifies the flag is forwarded, not that the answer's shape is unchanged |
| heart > day returns over real data | DIVERGED | HeartRepositoryTest: `DAY average heart rate uses raw full samples for selected day graph`; LoadHeartPeriodUseCaseTest: `combined request merges heart and vitals` | Mocked HealthConnectManager |
| heart > week returns over real data | DIVERGED | HeartRepositoryTest: `WEEK average heart rate uses daily aggregate summaries without raw day samples` | Mocked, empty result |
| heart > month returns over real data | DIVERGED | HeartViewModelTest (MONTH via selectRange) | No load assertions for MONTH |
| heart > year returns over real data | DIVERGED | FixtureRangeLoadTest: `the ranged reads answer inside their window, over real data, and never shrink` | Now loaded over the real corpus at every range, but at the reader layer rather than the use case — see that class's doc for why |
| heart > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | HeartViewModelTest: `DAY range leaves dayRestingBpm null when repo returns null` | Per-field null tolerance, not a whole-result validity check |
| heart > a forced refresh returns the same shape as a normal load | DIVERGED | LoadHeartPeriodUseCaseTest: `force refresh passes refresh mode to repositories` | Flag forwarding only |
| activities > day returns over real data | DIVERGED | ActivityRepositoryTest: `DAY activity metric progress uses raw full data for selected day graph` | Mocked |
| activities > week returns over real data | DIVERGED | ActivitiesViewModelTest: `last seven days week mode loads and displays rolling seven day window` | Mocked |
| activities > month returns over real data | DIVERGED | ActivityViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| activities > year returns over real data | DIVERGED | ActivityViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| activities > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | ActivityViewModelTest: `initial load clears loading and sets empty lists` | VM-level |
| activities > a forced refresh returns the same shape as a normal load | MISSING | - | No force/refresh-mode path on the activities load |
| hydration > day returns over real data | DIVERGED | HydrationRepositoryTest: `DAY hydration uses raw full entries for selected day total` | Mocked |
| hydration > week returns over real data | DIVERGED | HydrationViewModelTest: `previousPeriod moves back one period of the selected range` / `nextPeriod WEEK advances from a past week` | Period math, load not asserted per range |
| hydration > month returns over real data | DIVERGED | HydrationViewModelTest (MONTH via selectRange) | Range switching only |
| hydration > year returns over real data | DIVERGED | FixtureRangeLoadTest: `the ranged reads answer inside their window, over real data, and never shrink` | Now loaded over the real corpus at every range, at the reader layer |
| hydration > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | HydrationViewModelTest: `initial load clears loading and sets empty list` | VM-level |
| hydration > a forced refresh returns the same shape as a normal load | DIVERGED | HydrationViewModelTest: `deleteHydrationEntry removes entry and reloads period data` (coVerify RefreshMode.FORCE) | Verifies FORCE is passed, not shape equality |
| body > day returns over real data | MISSING | - | BodyRepositoryTest covers mutations only; no DAY load |
| body > week returns over real data | DIVERGED | BodyViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| body > month returns over real data | DIVERGED | BodyViewModelTest: `initial range is MONTH` / `load success populates weight entries` | Mocked repo, no corpus |
| body > year returns over real data | DIVERGED | BodyViewModelTest: `previousPeriod moves back one period of the selected range` | Period math only |
| body > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | BodyViewModelTest: `initial state has empty weight entries and all nulls` + `load uses latest body values when selected period entries are empty` | VM-level |
| body > a forced refresh returns the same shape as a normal load | DIVERGED | BodyViewModelTest (coVerify loadBodyPeriod(any(), ALL, RefreshMode.FORCE)) | Flag forwarding only |
| nutrition > day returns over real data | DIVERGED | NutritionRepositoryTest: `DAY nutrition uses raw full entries for selected day metrics` | Mocked |
| nutrition > week returns over real data | DIVERGED | NutritionViewModelTest: `initial range is WEEK` / `nextPeriod WEEK advances from a past week` | Period math |
| nutrition > month returns over real data | DIVERGED | NutritionViewModelTest (MONTH via selectRange) | Range switching only |
| nutrition > year returns over real data | DIVERGED | NutritionViewModelTest: `year range loads raw meal entries` | Mocked, verifies read routing not result validity |
| nutrition > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | NutritionViewModelTest: `initial load clears loading and sets empty lists` | VM-level |
| nutrition > a forced refresh returns the same shape as a normal load | DIVERGED | NutritionViewModelTest: `deleting an entry removes it optimistically and force-reloads` | Flag forwarding only |
| cycle > day returns over real data | BLOCKED | - | Kotlin's cycle screen is month-anchored: there is no DAY load to exercise |
| cycle > week returns over real data | BLOCKED | - | Same month-anchored load — no WEEK range exists on the cycle stack |
| cycle > month returns over real data | DIVERGED | CycleViewModelTest: `initial load requests the current month period` + CycleRepositoryTest: `loadCycleData combines all granted cycle data` | Mocked |
| cycle > year returns over real data | MISSING | - | No YEAR case |
| cycle > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CycleViewModelTest: `initial load clears loading and sets empty data` | VM-level, month range |
| cycle > a forced refresh returns the same shape as a normal load | DIVERGED | CycleViewModelTest (coVerify loadCyclePeriod(any(), RefreshMode.FORCE)) | Flag forwarding only |
| mindfulness > day returns over real data | MISSING | - | No per-range load test |
| mindfulness > week returns over real data | DIVERGED | MindfulnessViewModelTest: `load success populates sessions and derived total minutes` | Default WEEK range, mocked |
| mindfulness > month returns over real data | DIVERGED | MindfulnessViewModelTest: `selectRange updates selectedRange and reloads` (MONTH) | Range switching only |
| mindfulness > year returns over real data | MISSING | - | No YEAR case |
| mindfulness > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | MindfulnessViewModelTest: `initial load clears loading and sets empty list` | VM-level |
| mindfulness > a forced refresh returns the same shape as a normal load | DIVERGED | MindfulnessViewModelTest (coVerify loadMindfulnessPeriod(any(), RefreshMode.FORCE)) | Flag forwarding only |
| caffeine > day returns over real data | DIVERGED | CaffeineRepositoryTest: `loadCaffeinePeriod filters caffeine entries and uses lookback` | Mocked, window-based rather than per-TimeRange |
| caffeine > week returns over real data | DIVERGED | CaffeineViewModelTest: `analytics range selection reloads matching caffeine window` | Window plumbing only |
| caffeine > month returns over real data | DIVERGED | CaffeineViewModelTest: `analytics range selection reloads matching caffeine window` | Same test covers all analytics windows loosely |
| caffeine > year returns over real data | MISSING | - | No YEAR analytics window case |
| caffeine > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CaffeineRepositoryTest: `loadCaffeinePeriod returns empty list when Health Connect has no caffeine` | Repository-level, not the use case |
| caffeine > a forced refresh returns the same shape as a normal load | DIVERGED | CaffeineViewModelTest: `refresh reloads with force mode` | Flag forwarding only |
| calories > day returns over real data | DIVERGED | CaloriesViewModelTest: `initial load combines total active and BMR data` | Mocked |
| calories > week returns over real data | MISSING | - | No WEEK load case |
| calories > month returns over real data | DIVERGED | CaloriesViewModelTest: `selectRange saves range and reloads` (MONTH) | Range switching only |
| calories > year returns over real data | MISSING | - | No YEAR case |
| calories > an EMPTY day returns empty-but-VALID, never null and never a throw | DIVERGED | CaloriesViewModelTest: `latest BMR is used when selected period has no BMR readings` | Partial-empty only |
| calories > a forced refresh returns the same shape as a normal load | MISSING | - | No refresh-mode path tested for calories |

## test/integration/view_models_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/ (per-VM tests; all mocked repositories, none over the fixture corpus) plus /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/core/performance/LoadCoordinatorTest.kt for the shared newest-wins guard
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| sleep > loads a day of real data without erroring | DIVERGED | SleepViewModelTest: `initial load clears loading` + `load success populates sessions` | Hand-built sessions from a mocked repo, not a real day of the corpus |
| sleep > an EMPTY day is empty-but-valid, not an error | DIVERGED | SleepViewModelTest: `initial sessions list is empty when repo returns nothing` | Asserts emptiness but not `error == null` |
| sleep > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| activities > loads a day of real data without erroring | DIVERGED | ActivitiesViewModelTest: `last seven days week mode loads and displays rolling seven day window`; ActivityViewModelTest: `load success populates data` | Mocked repo |
| activities > an EMPTY day is empty-but-valid, not an error | DIVERGED | ActivityViewModelTest: `initial load clears loading and sets empty lists`; ActivitiesViewModelTest: `monday to sunday week mode displays all seven days including empty future days` | No error-is-null assertion on the empty path |
| activities > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| hydration > loads a day of real data without erroring | DIVERGED | HydrationViewModelTest: `load success populates hydration and derived totals` | Mocked repo |
| hydration > an EMPTY day is empty-but-valid, not an error | DIVERGED | HydrationViewModelTest: `initial load clears loading and sets empty list` | No error-is-null assertion |
| hydration > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |
| heartVitalsOverview > loads a day of real data without erroring | DIVERGED | HeartViewModelTest: `load success populates selected vitals and latest values` + `initial load clears loading` | Kotlin has no HeartVitalsOverview VM; covered by HeartViewModel over mocks |
| heartVitalsOverview > an EMPTY day is empty-but-valid, not an error | DIVERGED | HeartViewModelTest: `DAY range produces empty dailyRestingHR and dailyHrv`; HeartVitalsRangeSummaryTest: `resting heart rate summary is null for no days, not an invented 40 to 80 range` | Field-level emptiness, not a state-validity check |
| heartVitalsOverview > a stale load does not clobber a newer one | PORTED | LoadCoordinatorTest: `a stale load does not clobber a newer one` | Kotlin's view models share ONE LoadCoordinator, so the guard Dart copy-pastes per screen is pinned once (+ `a superseded load is cancelled, not merely ignored`) |

## test/fixtures/fixture_scrub_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureScrubTest.kt (same byte-identical golden.json, off the test classpath)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| no real writer, vendor or person appears anywhere in the fixture | PORTED | FixtureScrubTest: `no real writer, vendor or person appears anywhere in the fixture` | Identical 19-needle denylist over the same byte-identical golden.json |
| every writer is either an example alias or OpenVitals itself | PORTED | FixtureScrubTest: `every writer is either an example alias or OpenVitals itself` | Same fail-closed allow rule (com.example.*, tech.mmarca.openvitals*, android, com.android.shell) |
| no coordinate is anywhere near the real route | PORTED | FixtureScrubTest: `no coordinate is anywhere near the real route` | Same 55..57 / 2..4 synthetic bbox |
| no free text survived | PORTED | FixtureScrubTest: `no free text survived` | Same four canned title/notes replacements |

## test/fixtures/fixture_shape_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureShapeTest.kt (the raw-JSON guards) and /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/healthconnect/FixtureReaderTest.kt (the reader-level halves)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a heart-rate record long enough to swallow a workout still exists | PORTED | FixtureReaderTest: `the corpus loads and the swallowing record is still in it` | Same >12 h threshold plus >500 samples and containment of the workout |
| the swallowed workout is invisible to a naive windowed read | PORTED | FixtureReaderTest: `Health Connect hides that workout's heart rate from a windowed read` | Asserted through the real FakeHealthConnectClient rather than on the raw JSON — stronger |
| more than one app wrote sleep on the same night | PORTED | FixtureShapeTest: `more than one app wrote sleep on the same night` | Sleep grouped per UTC night, same "some night has two writers" assertion |
| a GPS route with enough points to compute splits from | PORTED | FixtureShapeTest: `a GPS route with enough points to compute splits from` | Same >500-point threshold |
| the sibling records that a session does NOT carry are present | PORTED | FixtureShapeTest: `the sibling records that a session does NOT carry are present` | Same steps/distance/activeCalories/basalMetabolicRate guards |
| speed is a SERIES record, so splits hit the same bug as heart rate | PORTED | FixtureShapeTest: `speed is a SERIES record, so splits hit the same bug as heart rate` | Raw sample count >10 now asserted, alongside FixtureReaderTest's reader-level case |
| the synthetic records are exactly the two we could not derive | PORTED | FixtureShapeTest: `the synthetic records are exactly the two we could not derive` | Same {power, cyclingCadence} set and the non-empty power guard |
| records carry the provenance the port kept losing | PORTED | FixtureReaderTest: `every record keeps the provenance the Pigeon messages kept dropping` | Same three fields (recordingMethod, lastModifiedTime, zone offset), asserted on reader output rather than on the JSON |

## test/goldens/charts/achievements_cards_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the summary card, part way through the catalogue | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the summary card before the first load lands | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge mid-progress — locked, and honest about it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge barely started | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge earned — the whole card changes, not just the bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a badge earned more than once | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/activity_splits_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| splits cut from the route | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| laps the device recorded itself | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| estimated splits — the numbers, and no bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/axis_rows_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the y-axis label column, beside its plot | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the hour row under a plot that HAS a y axis | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the hour row under a plot that has NO y axis | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the elapsed row, inset and not | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a week keeps every label | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a month drops all but every fifth | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the date strip — a year keeps the twelve month names | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/body_energy_timeline_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a day up to the golden clock | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| early morning — few enough points that the line grows dots | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/caffeine_curve_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a day of drinking — sawtooth, threshold, markers | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a single dose — one rise, one long decay | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a day with nothing in it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/distribution_bars_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| caffeine by source | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine by category — long labels against the value | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine with nothing logged | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| caffeine by time of day | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| hydration drink breakdown | N/A-WIDGET | - | hydration_week_detail_current.png covers the hydration detail surface, not this component in isolation |
| hydration where one drink is nearly everything | N/A-WIDGET | - | - |

## test/goldens/charts/metric_day_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| cumulative, a day that is over | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| cumulative, today — the rest of the day has not happened | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| raw readings, plotted where they were taken | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a day with nothing in it | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/metric_line_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| one series, no legend | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| two series and the legend that comes with them | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| two series with a day selected | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/metric_session_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a recorded trace, dense enough that the dots come off | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a trace stepped per split, sparse enough to show its points | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| cadence, the other card that shares this scaffold | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/period_bar_chart_golden_test.dart
Kotlin counterpart: none (closest surface: app/src/androidTest/assets/goldens/hydration_week_detail_current.png via OpenVitalsVisualRegressionTest.hydrationWeekDetail_matchesCurrentBaseline)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week — seven slots, and a number on every bar | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week with a day selected | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a month — 31 slots, too narrow for a label to survive | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week at 1.5× text — the labels step down instead of vanishing | N/A-WIDGET | - | no androidTest golden equivalent; androidTest pins fontScale = 1f |

## test/goldens/charts/period_chart_golden_test.dart
Kotlin counterpart: none (closest surface: hydration_week_detail_current.png)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| week — bars | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| week — bars, with the selected day highlighted | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| month — the calendar heatmap | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| year — the dot heatmap, twelve averaged months | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_cards_golden_test.dart
Kotlin counterpart: none (SleepScreenWeekTest.kt is semantics-only, no golden)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a night broken down by stage | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a device that only says "asleep" | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_schedule_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week of nights | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a partly-staged night reads at its full duration | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a week with the average bedtime and wake-up marked | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sleep_stage_chart_golden_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a night, with the lane totals the detail screen shows | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| the same night on the day card, labels without totals | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a device that only says "asleep" | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/sparkline_golden_test.dart
Kotlin counterpart: none (dashboard_current.png renders the full DashboardContent, which includes sparkline widgets)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a week of buckets | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| one point — a flat run across the whole width | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| one point with singlePointLine off — the dot on its own | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## test/goldens/charts/summary_ring_golden_test.dart
Kotlin counterpart: none (dashboard_current.png includes ring widgets on the dashboard surface)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| nothing yet — the track, and no fill | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| part way round | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| past the goal — the arc closes, the number keeps going | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |
| a value long enough to fight the ring for room | PORTED | see the golden tests under app/src/androidTest (…GoldenTest.kt) | Compose instrumentation golden; baseline recorded on a Pixel 6 Pro, theme/density/dynamic-colour pinned |

## Summary

| Status | Count |
|---|---|
| PORTED | 534 |
| DIVERGED | 63 |
| MISSING | 11 |
| BLOCKED | 8 |
| N/A-WIDGET | 118 |
| N/A-FRAMEWORK | 10 |
| Total cases | 744 |

### Blocked on a behavior decision

Porting these would mean changing production code, which this wave does not do.

- test/ui/charts/bar_label_test.dart — a label that does not fit SHRINKS — it does not vanish — Kotlin measures at one size and drops on overflow — there is no shrink-to-fit ladder to assert
- test/ui/charts/bar_label_test.dart — the longer the number, the smaller it is drawn — but it is drawn — Same one-size measurement: no step-down exists in Kotlin
- test/ui/charts/charts_test.dart — periodYearHeatmapCells (rolling) spans across the calendar-year edge — Kotlin's `periodYearHeatmapCells` has no `rolling` parameter — the year heatmap always draws the calendar year of `period.start`
- test/ui/charts/metric_day_chart_test.dart — MetricDayChart > sorts the samples it is handed — Kotlin's `rawDayPlotPoints` plots in caller order; only the bucketed branch sorts, so the guarantee does not exist to assert
- test/ui/components/health_connect_gate_test.dart — shows the access gate when Health Connect is unavailable — Kotlin deliberately routes unavailability out of band (state.availability + the dashboard promo) instead of a blocking gate; the Kotlin test pins that routing
- test/ui/components/health_connect_gate_test.dart — shows the permission gate when a required permission is missing — Kotlin raises a contextual prompt with the child still shown rather than a blocking gate; the Kotlin test pins that divergence
- test/integration/use_cases_test.dart — cycle > day returns over real data — Kotlin's cycle screen is month-anchored: there is no DAY load to exercise
- test/integration/use_cases_test.dart — cycle > week returns over real data — Same month-anchored load — no WEEK range exists on the cycle stack

### Portable gaps

Every remaining MISSING or DIVERGED row. All of them now sit in `features/**`, `data/repository/**` or the repository-level integration surfaces, i.e. outside this wave's file ownership (`**/ui/**`, `**/sensors/**`); the charts and fixture areas are closed.

- [DIVERGED] test/ui/charts/metric_day_chart_test.dart — MetricDayChart > off draws the raw readings and no band
- [DIVERGED] test/integration/activity_pipeline_test.dart — a GPS session gets REAL splits, not evenly-estimated ones
- [DIVERGED] test/integration/degraded_device_test.dart — a missing Health Connect is reported as unavailable, not as an error
- [DIVERGED] test/integration/degraded_device_test.dart — a read without its permission returns EMPTY, and does not throw
- [DIVERGED] test/integration/degraded_device_test.dart — holding ONLY the sleep permission still loads sleep, and nothing else
- [DIVERGED] test/integration/degraded_device_test.dart — sync paused degrades reads to empty rather than failing
- [DIVERGED] test/integration/repositories_test.dart — health > availability resolves through the real repository
- [DIVERGED] test/integration/repositories_test.dart — heart > daily summaries carry min, max and average — not just average
- [DIVERGED] test/integration/use_cases_test.dart — sleep > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — sleep > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — sleep > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — heart > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — heart > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — heart > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — heart > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — activities > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — activities > an EMPTY day returns empty-but-VALID, never null and never a throw
- [MISSING] test/integration/use_cases_test.dart — activities > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — hydration > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — hydration > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — hydration > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — hydration > a forced refresh returns the same shape as a normal load
- [MISSING] test/integration/use_cases_test.dart — body > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — body > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — body > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > month returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — nutrition > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — cycle > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — cycle > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — cycle > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — cycle > a forced refresh returns the same shape as a normal load
- [MISSING] test/integration/use_cases_test.dart — mindfulness > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — mindfulness > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — mindfulness > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > day returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — caffeine > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > an EMPTY day returns empty-but-VALID, never null and never a throw
- [DIVERGED] test/integration/use_cases_test.dart — caffeine > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/use_cases_test.dart — calories > day returns over real data
- [MISSING] test/integration/use_cases_test.dart — calories > week returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — calories > month returns over real data
- [MISSING] test/integration/use_cases_test.dart — calories > year returns over real data
- [DIVERGED] test/integration/use_cases_test.dart — calories > an EMPTY day returns empty-but-VALID, never null and never a throw
- [MISSING] test/integration/use_cases_test.dart — calories > a forced refresh returns the same shape as a normal load
- [DIVERGED] test/integration/view_models_test.dart — sleep > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — sleep > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — activities > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — activities > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — hydration > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — hydration > an EMPTY day is empty-but-valid, not an error
- [DIVERGED] test/integration/view_models_test.dart — heartVitalsOverview > loads a day of real data without erroring
- [DIVERGED] test/integration/view_models_test.dart — heartVitalsOverview > an EMPTY day is empty-but-valid, not an error
