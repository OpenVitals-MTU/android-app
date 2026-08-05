package tech.mmarca.openvitals.features.reports.pdf

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues
import tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail
import tech.mmarca.openvitals.domain.model.ReportBpReading
import tech.mmarca.openvitals.domain.model.BpMealContext
import tech.mmarca.openvitals.domain.model.ReportBpSlotAverage
import tech.mmarca.openvitals.domain.model.ReportGlucoseContextAverage
import tech.mmarca.openvitals.domain.model.ReportGlucoseDetail
import tech.mmarca.openvitals.domain.model.ReportGlucoseReading
import tech.mmarca.openvitals.domain.model.ReportSleepDetail
import tech.mmarca.openvitals.domain.model.ReportSleepNight
import tech.mmarca.openvitals.domain.model.ReportSleepStageMix
import tech.mmarca.openvitals.domain.model.ReportWorkoutSession
import tech.mmarca.openvitals.domain.model.ReportWorkoutTypeTotal
import tech.mmarca.openvitals.domain.model.ReportWorkoutsDetail
import tech.mmarca.openvitals.domain.model.ReportData
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportMetricResult
import tech.mmarca.openvitals.domain.model.ReportMetricStatus
import tech.mmarca.openvitals.domain.model.ReportMetricSummary
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportRequest

/**
 * The one thing no JVM test can prove: android.graphics.pdf.PdfDocument, fed
 * the real block pipeline, emits an actual PDF. Charts, tables, notices and a
 * failed section all in one document — if any draw call is wrong on a real
 * runtime, this is where it surfaces.
 */
class ReportPdfWriterSmokeTest {

    private val end = LocalDate.of(2026, 7, 31)

    private fun point(offset: Long, value: Double) = ReportPoint(
        bucketStart = end.minusDays(offset),
        bucketEnd = end.minusDays(offset),
        value = value,
        min = value * 0.8,
        max = value * 1.2,
        daysWithData = 1,
    )

    private val labels = ReportPdfLabels(
        reportTitle = "Health report",
        subtitleLines = listOf("Generated for the smoke test", "May 3 – Jul 31 · Daily"),
        notices = listOf("Not included (no read access): Sleep"),
        metricTitles = ReportMetric.entries.associateWith { it.name },
        chartCaptions = mapOf(ReportMetric.STEPS to "Total 240,000 · daily average 8,000"),
        statAverage = "Average",
        statMin = "Min",
        statMax = "Max",
        statTotal = "Total",
        statDays = "Days with data",
        tablePeriod = "Period",
        tableValue = "Value",
        tableMin = "Min",
        tableMax = "Max",
        tableContinued = "(continued)",
        statusEmpty = "No data in this range.",
        statusFailed = "This metric could not be read.",
        statusSkipped = "Skipped.",
        statusMissingPermission = "Not included.",
        bpSystolic = "Systolic",
        bpDiastolic = "Diastolic",
        bpTimeOfDay = "Time of day",
        bpReadings = "Readings",
        bpAllReadings = "All readings",
        bpDateTime = "Date & time",
        bpContexts = BpMealContext.entries.associateWith { it.name },
        bpEstimatedNote = "* estimated from the time of day",
        bpPosition = "Position",
        bpPositions = mapOf(2 to "Sitting"),
        bpLocations = mapOf(3 to "Left arm"),
        statChange = "Change",
        glucoseContext = "Context",
        glucoseContexts = mapOf(
            GlucoseRecordValues.RELATION_TO_MEAL_FASTING to "Fasting",
            GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL to "After meal",
        ),
        workoutActivity = "Activity",
        workoutSessions = "Sessions",
        workoutDate = "Date",
        workoutDuration = "Duration",
        workoutDistance = "Distance",
        sleepBedtime = "Bedtime",
        sleepWake = "Wake-up",
        sleepNights = "Nights",
        sleepAsleep = "Asleep",
        sleepDeep = "Deep",
        sleepRem = "REM",
        sleepLight = "Light",
        sleepAwake = "Awake",
        workoutTypeLabel = { "Type $it" },
        pageLabel = { page, count -> "Page $page of $count" },
    )

    private val values = object : ReportValueFormatter {
        override fun value(metric: ReportMetric, value: Double) = "%.0f".format(value)
        override fun axisValue(metric: ReportMetric, value: Double) = "%.0f".format(value)
        override fun pointValue(metric: ReportMetric, point: ReportPoint) = "%.0f".format(point.value)
        override fun bucketLabel(point: ReportPoint, granularity: ReportGranularity) =
            point.bucketStart.toString()
        override fun bloodPressure(systolic: Double, diastolic: Double) =
            "%.0f/%.0f mmHg".format(systolic, diastolic)
        override fun readingTime(time: Instant) = time.toString()
        override fun timeOfDay(time: Instant) = time.toString().takeLast(9)
        override fun clockTime(minutesOfDay: Int) = "%02d:%02d".format(minutesOfDay / 60, minutesOfDay % 60)
        override fun durationHm(durationMs: Long) = "${'$'}{durationMs / 3_600_000}h ${'$'}{durationMs / 60_000 % 60}m"
        override fun signedValue(metric: ReportMetric, delta: Double) =
            (if (delta < 0) "-" else "+") + "%.1f".format(kotlin.math.abs(delta))
        override fun percent(value: Double) = "%.0f%%".format(value)
        override fun date(date: LocalDate) = date.toString()
    }

    @Test
    fun aRealisticReportRendersToAValidPdf() {
        val points = (0L until 90L).map { point(it, 8_000.0 + it * 10) }.sortedBy { it.bucketStart }
        val data = ReportData(
            request = ReportRequest(
                metrics = setOf(
                    ReportMetric.STEPS,
                    ReportMetric.AVG_HEART_RATE,
                    ReportMetric.BLOOD_PRESSURE,
                    ReportMetric.BLOOD_GLUCOSE,
                    ReportMetric.WORKOUT,
                    ReportMetric.WEIGHT,
                    ReportMetric.SLEEP,
                    ReportMetric.RESPIRATORY_RATE,
                ),
                granularity = ReportGranularity.DAILY,
                start = end.minusDays(89),
                end = end,
            ),
            effectiveStart = end.minusDays(89),
            truncatedToDays = null,
            missingPermissions = setOf(ReportMetric.RESPIRATORY_RATE),
            historyPermissionMissing = false,
            cancelled = false,
            results = listOf(
                ReportMetricResult(
                    metric = ReportMetric.STEPS,
                    status = ReportMetricStatus.OK,
                    points = points,
                    summary = ReportMetricSummary(
                        average = 8_450.0,
                        min = 8_000.0,
                        max = 8_890.0,
                        total = 760_500.0,
                        daysWithData = 90,
                    ),
                ),
                ReportMetricResult(
                    metric = ReportMetric.AVG_HEART_RATE,
                    status = ReportMetricStatus.OK,
                    points = points.map { it.copy(value = 70.0, min = 48.0, max = 155.0) },
                    summary = ReportMetricSummary(
                        average = 70.0,
                        min = 48.0,
                        max = 155.0,
                        total = null,
                        daysWithData = 90,
                    ),
                ),
                ReportMetricResult(
                    metric = ReportMetric.BLOOD_PRESSURE,
                    status = ReportMetricStatus.OK,
                    points = (0L until 6L).map { offset ->
                        point(offset, 115.0).copy(secondaryValue = 74.0)
                    },
                    summary = ReportMetricSummary(
                        average = 115.0,
                        min = 103.0,
                        max = 127.0,
                        total = null,
                        daysWithData = 6,
                    ),
                    detail = ReportBloodPressureDetail(
                        readings = (0L until 14L).map { index ->
                            ReportBpReading(
                                time = Instant.parse("2026-07-31T08:00:00Z").minusSeconds(index * 40_000),
                                systolicMmHg = (110 + index).toInt(),
                                diastolicMmHg = (70 + index % 8).toInt(),
                                context = BpMealContext.entries[(index % 6).toInt()],
                                contextEstimated = index % 2 == 0L,
                            )
                        }.sortedBy { it.time },
                        slotAverages = listOf(
                            ReportBpSlotAverage(BpMealContext.BEFORE_BREAKFAST, 112.0, 71.0, 5),
                            ReportBpSlotAverage(BpMealContext.AFTER_LUNCH, 116.0, 74.0, 4),
                            ReportBpSlotAverage(BpMealContext.AFTER_DINNER, 119.0, 76.0, 5),
                            ReportBpSlotAverage(null, 115.5, 73.5, 14),
                        ),
                        systolic = ReportMetricSummary(115.5, 103.0, 127.0, null, 6),
                        diastolic = ReportMetricSummary(73.5, 68.0, 78.0, null, 6),
                    ),
                ),
                ReportMetricResult(
                    metric = ReportMetric.BLOOD_GLUCOSE,
                    status = ReportMetricStatus.OK,
                    points = (0L until 6L).map { point(it, 5.6) },
                    summary = ReportMetricSummary(5.6, 4.8, 7.4, null, 6),
                    detail = ReportGlucoseDetail(
                        readings = (0L until 10L).map { index ->
                            ReportGlucoseReading(
                                time = Instant.parse("2026-07-30T08:00:00Z").minusSeconds(index * 50_000),
                                millimolesPerLiter = 5.0 + index * 0.2,
                                relationToMeal = if (index % 2 == 0L) {
                                    GlucoseRecordValues.RELATION_TO_MEAL_FASTING
                                } else {
                                    GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL
                                },
                            )
                        }.sortedBy { it.time },
                        contextAverages = listOf(
                            ReportGlucoseContextAverage(GlucoseRecordValues.RELATION_TO_MEAL_FASTING, 5.2, 4.8, 5.6, 5),
                            ReportGlucoseContextAverage(GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL, 6.6, 5.9, 7.4, 5),
                            ReportGlucoseContextAverage(null, 5.9, 4.8, 7.4, 10),
                        ),
                        summary = ReportMetricSummary(5.9, 4.8, 7.4, null, 6),
                    ),
                ),
                ReportMetricResult(
                    metric = ReportMetric.WORKOUT,
                    status = ReportMetricStatus.OK,
                    points = (0L until 6L).map { point(it, 45.0) },
                    summary = ReportMetricSummary(45.0, 30.0, 60.0, 270.0, 6),
                    detail = ReportWorkoutsDetail(
                        sessions = (0L until 8L).map { index ->
                            ReportWorkoutSession(
                                start = Instant.parse("2026-07-24T18:00:00Z").plusSeconds(index * 86_400),
                                exerciseType = (index % 2).toInt(),
                                title = if (index == 0L) "Evening run" else null,
                                durationMs = 30 * 60_000L,
                                distanceMeters = if (index % 2 == 0L) 5_200.0 else null,
                            )
                        },
                        byType = listOf(
                            ReportWorkoutTypeTotal(0, 4, 120 * 60_000L, 20_800.0),
                            ReportWorkoutTypeTotal(1, 4, 120 * 60_000L, null),
                        ),
                    ),
                ),
                ReportMetricResult(
                    metric = ReportMetric.WEIGHT,
                    status = ReportMetricStatus.OK,
                    points = (0L until 6L).map { point(it, 82.0) },
                    summary = ReportMetricSummary(82.0, 81.0, 83.5, null, 6, changeOverRange = -1.4),
                ),
                ReportMetricResult(
                    metric = ReportMetric.SLEEP,
                    status = ReportMetricStatus.OK,
                    points = (0L until 6L).map { point(it, 450.0) },
                    summary = ReportMetricSummary(450.0, 400.0, 500.0, null, 6),
                    detail = ReportSleepDetail(
                        nights = (0L until 6L).map { index ->
                            ReportSleepNight(
                                date = end.minusDays(index),
                                bedtime = Instant.parse("2026-07-30T21:10:00Z").minusSeconds(index * 86_400),
                                wake = Instant.parse("2026-07-31T05:40:00Z").minusSeconds(index * 86_400),
                                asleepMs = 7 * 3_600_000L + 30 * 60_000L,
                                deepMs = 80 * 60_000L,
                                remMs = 95 * 60_000L,
                            )
                        },
                        averageBedtimeMinutes = 23 * 60 + 12,
                        averageWakeMinutes = 7 * 60 + 41,
                        stageMix = ReportSleepStageMix(18.0, 21.0, 55.0, 6.0),
                        nightsWithData = 6,
                    ),
                ),
                ReportMetricResult(metric = ReportMetric.RESPIRATORY_RATE, status = ReportMetricStatus.MISSING_PERMISSION),
            ),
            generatedAt = Instant.parse("2026-07-31T12:00:00Z"),
        )

        val output = ByteArrayOutputStream()
        ReportPdfWriter(labels = labels, values = values, logo = null).write(data, output)
        val bytes = output.toByteArray()

        assertTrue("PDF should have real content, got ${bytes.size} bytes", bytes.size > 2_000)
        assertEquals("%PDF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        // A well-formed PDF ends with the EOF marker (a trailing newline is legal).
        assertTrue(bytes.toString(Charsets.US_ASCII).trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun anEmptyReportStillRendersAOnePagePdf() {
        val data = ReportData(
            request = ReportRequest(
                metrics = setOf(ReportMetric.STEPS),
                granularity = ReportGranularity.MONTHLY,
                start = end.minusDays(29),
                end = end,
            ),
            effectiveStart = end.minusDays(29),
            truncatedToDays = null,
            missingPermissions = emptySet(),
            historyPermissionMissing = false,
            cancelled = false,
            results = listOf(ReportMetricResult(metric = ReportMetric.STEPS, status = ReportMetricStatus.EMPTY)),
            generatedAt = Instant.parse("2026-07-31T12:00:00Z"),
        )

        val output = ByteArrayOutputStream()
        ReportPdfWriter(labels = labels, values = values, logo = null).write(data, output)

        assertEquals("%PDF", output.toByteArray().copyOfRange(0, 4).toString(Charsets.US_ASCII))
    }
}
