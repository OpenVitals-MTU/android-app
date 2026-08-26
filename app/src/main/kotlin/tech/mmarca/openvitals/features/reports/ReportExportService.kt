package tech.mmarca.openvitals.features.reports

import android.content.Context
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.export.stageExport
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.report.ReportCancellation
import tech.mmarca.openvitals.data.repository.report.ReportDataLoader
import tech.mmarca.openvitals.data.repository.report.ReportProgress
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.GlucoseRecordValues
import tech.mmarca.openvitals.domain.model.ReportData
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportMetricStatus
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportRequest
import tech.mmarca.openvitals.domain.model.ReportValueKind
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.features.reports.pdf.ReportPdfLabels
import tech.mmarca.openvitals.features.reports.pdf.ReportPdfWriter
import tech.mmarca.openvitals.features.reports.pdf.ReportValueFormatter

internal const val ReportExportCacheDirectory = "report_exports"

/**
 * The whole report pipeline behind one call: read via [ReportDataLoader],
 * render via [ReportPdfWriter], stage the PDF in the cache for share/save.
 * Runs in the caller's coroutine (the builder ViewModel) — this repo runs
 * user-attended work in ViewModels, not WorkManager (see CsvImportService).
 */
@Singleton
class ReportExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loader: ReportDataLoader,
    private val unitFormatter: UnitFormatter,
    private val preferencesRepository: PreferencesRepository,
) {
    fun supportedMetrics(): Set<ReportMetric> = loader.supportedReportMetrics()

    fun requestablePermissionsFor(metrics: Set<ReportMetric>): Set<String> =
        loader.requestablePermissionsFor(metrics)

    suspend fun build(
        metrics: Set<ReportMetric>,
        granularity: ReportGranularity,
        start: LocalDate,
        end: LocalDate,
        onProgress: (ReportProgress) -> Unit,
        cancellation: ReportCancellation,
    ): File {
        val request = ReportRequest(
            metrics = metrics,
            granularity = granularity,
            start = start,
            end = end,
            sleepWindow = preferencesRepository.sleepWindowFlow.value,
            weekMode = preferencesRepository.activityWeekModeFlow.value,
        )
        val data = loader.load(request, onProgress, cancellation)
        return withContext(Dispatchers.IO) {
            val writer = ReportPdfWriter(
                labels = buildLabels(data),
                values = formatter(granularity),
                logo = BitmapFactory.decodeResource(context.resources, R.drawable.open_vitals_logo_wide),
            )
            File(context.cacheDir, ReportExportCacheDirectory)
                .stageExport(reportFileName(data.generatedAt.atZone(ZoneId.systemDefault()).toLocalDateTime())) { output ->
                    writer.write(data, output)
                }
        }
    }

    fun metricTitle(metric: ReportMetric): String = context.getString(metricTitleRes(metric))

    // ── labels ──────────────────────────────────────────────────────────────

    private fun buildLabels(data: ReportData): ReportPdfLabels {
        val formatters = DateTimeFormatterProvider()
        val dates = formatters.mediumDate()
        val granularityLabel = context.getString(
            when (data.request.granularity) {
                ReportGranularity.DAILY -> R.string.report_granularity_daily
                ReportGranularity.WEEKLY -> R.string.report_granularity_weekly
                ReportGranularity.MONTHLY -> R.string.report_granularity_monthly
            },
        )
        val notices = buildList {
            if (data.missingPermissions.isNotEmpty()) {
                add(
                    context.getString(
                        R.string.report_pdf_notice_missing_permissions,
                        data.missingPermissions
                            .sortedBy { it.ordinal }
                            .joinToString { metricTitle(it) },
                    ),
                )
            }
            data.truncatedToDays?.let { add(context.getString(R.string.report_pdf_notice_truncated, it)) }
            if (data.cancelled) add(context.getString(R.string.report_pdf_notice_cancelled))
        }
        return ReportPdfLabels(
            reportTitle = context.getString(R.string.report_builder_title),
            subtitleLines = listOf(
                context.getString(
                    R.string.report_pdf_generated_at,
                    formatters.mediumDateTime().format(data.generatedAt.atZone(ZoneId.systemDefault())),
                ),
                context.getString(
                    R.string.report_pdf_range_line,
                    dates.format(data.effectiveStart),
                    dates.format(data.request.end),
                    granularityLabel,
                ),
            ),
            notices = notices,
            metricTitles = ReportMetric.entries.associateWith { metricTitle(it) },
            chartCaptions = chartCaptions(data),
            statAverage = context.getString(R.string.report_pdf_stat_average),
            statMin = context.getString(R.string.report_pdf_stat_min),
            statMax = context.getString(R.string.report_pdf_stat_max),
            statTotal = context.getString(R.string.report_pdf_stat_total),
            statDays = context.getString(R.string.report_pdf_stat_days),
            tablePeriod = context.getString(R.string.report_pdf_table_period),
            tableValue = context.getString(R.string.report_pdf_table_value),
            tableMin = context.getString(R.string.report_pdf_stat_min),
            tableMax = context.getString(R.string.report_pdf_stat_max),
            tableContinued = context.getString(R.string.report_pdf_table_continued),
            statusEmpty = context.getString(R.string.report_pdf_status_empty),
            statusFailed = context.getString(R.string.report_pdf_status_failed),
            statusSkipped = context.getString(R.string.report_pdf_status_skipped),
            statusMissingPermission = context.getString(R.string.report_pdf_status_missing_permission),
            bpSystolic = context.getString(R.string.report_pdf_bp_systolic),
            bpDiastolic = context.getString(R.string.report_pdf_bp_diastolic),
            bpTimeOfDay = context.getString(R.string.report_pdf_bp_time_of_day),
            bpReadings = context.getString(R.string.report_pdf_bp_readings),
            bpAllReadings = context.getString(R.string.report_pdf_bp_all_readings),
            bpDateTime = context.getString(R.string.report_pdf_bp_date_time),
            bpContexts = tech.mmarca.openvitals.domain.model.BpMealContext.entries.associateWith { bpContext ->
                context.getString(
                    when (bpContext) {
                        tech.mmarca.openvitals.domain.model.BpMealContext.BEFORE_BREAKFAST -> R.string.bp_context_before_breakfast
                        tech.mmarca.openvitals.domain.model.BpMealContext.AFTER_BREAKFAST -> R.string.bp_context_after_breakfast
                        tech.mmarca.openvitals.domain.model.BpMealContext.BEFORE_LUNCH -> R.string.bp_context_before_lunch
                        tech.mmarca.openvitals.domain.model.BpMealContext.AFTER_LUNCH -> R.string.bp_context_after_lunch
                        tech.mmarca.openvitals.domain.model.BpMealContext.BEFORE_DINNER -> R.string.bp_context_before_dinner
                        tech.mmarca.openvitals.domain.model.BpMealContext.AFTER_DINNER -> R.string.bp_context_after_dinner
                    },
                )
            },
            bpEstimatedNote = context.getString(R.string.report_pdf_bp_estimated_note),
            bpPosition = context.getString(R.string.report_pdf_bp_position),
            bpPositions = mapOf(
                tech.mmarca.openvitals.domain.model.BpRecordValues.BODY_POSITION_SITTING_DOWN to
                    context.getString(R.string.bp_position_sitting),
                tech.mmarca.openvitals.domain.model.BpRecordValues.BODY_POSITION_STANDING_UP to
                    context.getString(R.string.bp_position_standing),
                tech.mmarca.openvitals.domain.model.BpRecordValues.BODY_POSITION_LYING_DOWN to
                    context.getString(R.string.bp_position_lying),
                tech.mmarca.openvitals.domain.model.BpRecordValues.BODY_POSITION_RECLINING to
                    context.getString(R.string.bp_position_reclining),
            ),
            bpLocations = mapOf(
                tech.mmarca.openvitals.domain.model.BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to
                    context.getString(R.string.bp_location_left_arm),
                tech.mmarca.openvitals.domain.model.BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM to
                    context.getString(R.string.bp_location_right_arm),
                tech.mmarca.openvitals.domain.model.BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST to
                    context.getString(R.string.bp_location_left_wrist),
                tech.mmarca.openvitals.domain.model.BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST to
                    context.getString(R.string.bp_location_right_wrist),
            ),
            statChange = context.getString(R.string.report_pdf_stat_change),
            glucoseContext = context.getString(R.string.report_pdf_glucose_context),
            glucoseContexts = mapOf(
                GlucoseRecordValues.RELATION_TO_MEAL_FASTING to
                    context.getString(R.string.report_pdf_glucose_fasting),
                GlucoseRecordValues.RELATION_TO_MEAL_BEFORE_MEAL to
                    context.getString(R.string.report_pdf_glucose_before_meal),
                GlucoseRecordValues.RELATION_TO_MEAL_AFTER_MEAL to
                    context.getString(R.string.report_pdf_glucose_after_meal),
                GlucoseRecordValues.RELATION_TO_MEAL_GENERAL to
                    context.getString(R.string.report_pdf_glucose_general),
                GlucoseRecordValues.RELATION_TO_MEAL_UNKNOWN to
                    context.getString(R.string.report_pdf_glucose_unspecified),
            ),
            workoutActivity = context.getString(R.string.report_pdf_workout_activity),
            workoutSessions = context.getString(R.string.report_pdf_workout_sessions),
            workoutDate = context.getString(R.string.report_pdf_workout_date),
            workoutDuration = context.getString(R.string.report_pdf_workout_duration),
            workoutDistance = context.getString(R.string.report_pdf_workout_distance),
            sleepBedtime = context.getString(R.string.report_pdf_sleep_bedtime),
            sleepWake = context.getString(R.string.report_pdf_sleep_wake),
            sleepNights = context.getString(R.string.report_pdf_sleep_nights),
            sleepAsleep = context.getString(R.string.report_pdf_sleep_asleep),
            sleepDeep = context.getString(R.string.report_pdf_sleep_deep),
            sleepRem = context.getString(R.string.report_pdf_sleep_rem),
            sleepLight = context.getString(R.string.report_pdf_sleep_light),
            sleepAwake = context.getString(R.string.report_pdf_sleep_awake),
            workoutTypeLabel = { exerciseTypeLabel(context, it) },
            pageLabel = { page, count -> context.getString(R.string.report_pdf_page_of, page, count) },
        )
    }

    private fun chartCaptions(data: ReportData): Map<ReportMetric, String> {
        val formatter = formatter(data.request.granularity)
        return data.results
            .filter { it.status == ReportMetricStatus.OK && it.summary != null }
            .associate { result ->
                val summary = result.summary!!
                val detail = result.detail as? tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail
                val caption = if (detail != null) {
                    context.getString(
                        R.string.report_pdf_caption_blood_pressure,
                        formatter.bloodPressure(detail.systolic.average, detail.diastolic.average),
                        formatter.value(result.metric, detail.systolic.min),
                        formatter.value(result.metric, detail.systolic.max),
                        formatter.value(result.metric, detail.diastolic.min),
                        formatter.value(result.metric, detail.diastolic.max),
                    )
                } else when (result.metric.valueKind) {
                    ReportValueKind.SUM -> context.getString(
                        R.string.report_pdf_caption_sum,
                        formatter.value(result.metric, summary.total ?: 0.0),
                        formatter.value(result.metric, summary.average),
                    )
                    ReportValueKind.AVERAGE -> context.getString(
                        R.string.report_pdf_caption_average,
                        formatter.value(result.metric, summary.average),
                        formatter.value(result.metric, summary.min),
                        formatter.value(result.metric, summary.max),
                    )
                }
                result.metric to caption
            }
    }

    // ── values ──────────────────────────────────────────────────────────────

    private fun formatter(granularity: ReportGranularity): ReportValueFormatter =
        object : ReportValueFormatter {
            private val formatters = DateTimeFormatterProvider()
            private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
            private val shortDay = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

            override fun value(metric: ReportMetric, value: Double): String = displayValue(metric, value)

            override fun axisValue(metric: ReportMetric, value: Double): String =
                displayValue(metric, value, withUnit = false)

            override fun pointValue(metric: ReportMetric, point: ReportPoint): String =
                if (metric == ReportMetric.BLOOD_PRESSURE && point.secondaryValue != null) {
                    unitFormatter.bloodPressure(
                        point.value.roundToInt(),
                        point.secondaryValue!!.roundToInt(),
                    ).text
                } else {
                    displayValue(metric, point.value)
                }

            override fun bucketLabel(point: ReportPoint, granularity: ReportGranularity): String =
                when (granularity) {
                    ReportGranularity.DAILY -> formatters.mediumDate().format(point.bucketStart)
                    ReportGranularity.WEEKLY -> context.getString(
                        R.string.report_pdf_week_range,
                        shortDay.format(point.bucketStart),
                        shortDay.format(point.bucketEnd),
                    )
                    ReportGranularity.MONTHLY -> monthYear.format(point.bucketStart)
                }

            override fun bloodPressure(systolic: Double, diastolic: Double): String =
                unitFormatter.bloodPressure(systolic.roundToInt(), diastolic.roundToInt()).text

            override fun readingTime(time: java.time.Instant): String =
                formatters.mediumDateTime().format(time.atZone(ZoneId.systemDefault()))

            override fun timeOfDay(time: java.time.Instant): String =
                formatters.shortTime().format(time.atZone(ZoneId.systemDefault()))

            override fun clockTime(minutesOfDay: Int): String =
                formatters.shortTime().format(java.time.LocalTime.of(minutesOfDay / 60, minutesOfDay % 60))

            override fun durationHm(durationMs: Long): String = unitFormatter.duration(durationMs)

            override fun signedValue(metric: ReportMetric, delta: Double): String {
                val formatted = displayValue(metric, kotlin.math.abs(delta))
                return if (delta < 0) "-$formatted" else "+$formatted"
            }

            override fun percent(value: Double): String = unitFormatter.percent(value, 0).text

            override fun date(date: LocalDate): String = formatters.mediumDate().format(date)

            private fun displayValue(metric: ReportMetric, value: Double, withUnit: Boolean = true): String {
                val display = when (metric) {
                    ReportMetric.STEPS,
                    ReportMetric.FLOORS,
                    ReportMetric.WHEELCHAIR_PUSHES,
                    -> DisplayValue(unitFormatter.count(value.roundToLong()), "")
                    ReportMetric.DISTANCE -> unitFormatter.distance(value)
                    ReportMetric.ELEVATION -> unitFormatter.elevation(value)
                    ReportMetric.CALORIES_OUT,
                    ReportMetric.ACTIVE_CALORIES,
                    ReportMetric.CALORIES_IN,
                    ReportMetric.BMR,
                    -> unitFormatter.energy(value)
                    ReportMetric.WORKOUT,
                    ReportMetric.SLEEP,
                    ReportMetric.MINDFULNESS,
                    -> DisplayValue(
                        unitFormatter.duration((value * 60_000.0).roundToLong()),
                        "",
                    )
                    ReportMetric.HYDRATION -> unitFormatter.hydration(value)
                    ReportMetric.PROTEIN,
                    ReportMetric.CARBS,
                    ReportMetric.FAT,
                    -> DisplayValue(unitFormatter.decimal(value, 0), "g")
                    ReportMetric.CAFFEINE ->
                        DisplayValue(unitFormatter.count(value.roundToLong()), "mg")
                    ReportMetric.WEIGHT -> unitFormatter.weight(value)
                    ReportMetric.HEIGHT -> unitFormatter.height(value)
                    ReportMetric.BODY_FAT,
                    ReportMetric.SPO2,
                    -> unitFormatter.percent(value)
                    ReportMetric.LEAN_MASS,
                    ReportMetric.BONE_MASS,
                    ReportMetric.BODY_WATER_MASS,
                    -> unitFormatter.bodyMass(value)
                    ReportMetric.AVG_HEART_RATE,
                    ReportMetric.RESTING_HEART_RATE,
                    -> unitFormatter.heartRate(value.roundToLong())
                    ReportMetric.HRV -> unitFormatter.hrv(value)
                    ReportMetric.BLOOD_PRESSURE ->
                        DisplayValue(unitFormatter.decimal(value, 0), "mmHg")
                    ReportMetric.VO2_MAX -> unitFormatter.vo2Max(value)
                    ReportMetric.RESPIRATORY_RATE -> unitFormatter.respiratoryRate(value)
                    ReportMetric.BODY_TEMPERATURE,
                    ReportMetric.SKIN_TEMPERATURE,
                    -> unitFormatter.temperature(value)
                    ReportMetric.BLOOD_GLUCOSE -> unitFormatter.bloodGlucose(value)
                }
                return if (withUnit) display.text else display.value
            }
        }

    private fun metricTitleRes(metric: ReportMetric): Int = when (metric) {
        ReportMetric.STEPS -> R.string.metric_steps
        ReportMetric.DISTANCE -> R.string.metric_distance
        ReportMetric.CALORIES_OUT -> R.string.metric_calories_out
        ReportMetric.ACTIVE_CALORIES -> R.string.metric_active_calories
        ReportMetric.FLOORS -> R.string.metric_floors_climbed
        ReportMetric.ELEVATION -> R.string.metric_elevation
        ReportMetric.WHEELCHAIR_PUSHES -> R.string.metric_wheelchair_pushes
        ReportMetric.WORKOUT -> R.string.metric_workout
        ReportMetric.SLEEP -> R.string.metric_sleep
        ReportMetric.HYDRATION -> R.string.metric_hydration
        ReportMetric.CALORIES_IN -> R.string.metric_calories_in
        ReportMetric.PROTEIN -> R.string.metric_protein
        ReportMetric.CARBS -> R.string.metric_carbs
        ReportMetric.FAT -> R.string.metric_fat
        ReportMetric.CAFFEINE -> R.string.metric_caffeine
        ReportMetric.WEIGHT -> R.string.metric_latest_weight
        ReportMetric.HEIGHT -> R.string.metric_height
        ReportMetric.BODY_FAT -> R.string.metric_body_fat
        ReportMetric.LEAN_MASS -> R.string.metric_lean_mass
        ReportMetric.BMR -> R.string.metric_bmr
        ReportMetric.BONE_MASS -> R.string.metric_bone_mass
        ReportMetric.BODY_WATER_MASS -> R.string.metric_body_water_mass
        ReportMetric.AVG_HEART_RATE -> R.string.metric_avg_heart_rate
        ReportMetric.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
        ReportMetric.HRV -> R.string.metric_hrv
        ReportMetric.BLOOD_PRESSURE -> R.string.metric_blood_pressure
        ReportMetric.SPO2 -> R.string.metric_spo2
        ReportMetric.VO2_MAX -> R.string.metric_vo2_max
        ReportMetric.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
        ReportMetric.BODY_TEMPERATURE -> R.string.metric_body_temp
        ReportMetric.BLOOD_GLUCOSE -> R.string.metric_blood_glucose
        ReportMetric.SKIN_TEMPERATURE -> R.string.metric_skin_temperature
        ReportMetric.MINDFULNESS -> R.string.metric_mindfulness
    }
}

/** `openvitals-report-20260805-1432.pdf` — sortable, collision-free per minute. */
internal fun reportFileName(generatedAt: LocalDateTime): String =
    "openvitals-report-${generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}.pdf"
