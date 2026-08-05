package tech.mmarca.openvitals.features.reports.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.asAndroidPath
import java.io.OutputStream
import tech.mmarca.openvitals.domain.model.BpMealContext
import tech.mmarca.openvitals.domain.model.ReportBloodPressureDetail
import tech.mmarca.openvitals.domain.model.ReportGlucoseDetail
import tech.mmarca.openvitals.domain.model.ReportReadingsDetail
import tech.mmarca.openvitals.domain.model.ReportSleepDetail
import tech.mmarca.openvitals.domain.model.ReportWorkoutsDetail
import tech.mmarca.openvitals.domain.model.ReportData
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportMetric
import tech.mmarca.openvitals.domain.model.ReportMetricStatus
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportValueKind
import tech.mmarca.openvitals.ui.components.smoothPath

/**
 * Every localized string the PDF needs, resolved BEFORE the writer runs — the
 * writer never touches a Context, so it stays constructible in any test.
 */
data class ReportPdfLabels(
    val reportTitle: String,
    val subtitleLines: List<String>,
    val notices: List<String>,
    val metricTitles: Map<ReportMetric, String>,
    val chartCaptions: Map<ReportMetric, String>,
    val statAverage: String,
    val statMin: String,
    val statMax: String,
    val statTotal: String,
    val statDays: String,
    val tablePeriod: String,
    val tableValue: String,
    val tableMin: String,
    val tableMax: String,
    val tableContinued: String,
    val statusEmpty: String,
    val statusFailed: String,
    val statusSkipped: String,
    val statusMissingPermission: String,
    val bpSystolic: String,
    val bpDiastolic: String,
    val bpTimeOfDay: String,
    val bpReadings: String,
    val bpAllReadings: String,
    val bpDateTime: String,
    val bpContexts: Map<BpMealContext, String>,
    val bpEstimatedNote: String,
    val bpPosition: String,
    val bpPositions: Map<Int, String>,
    val bpLocations: Map<Int, String>,
    val statChange: String,
    val glucoseContext: String,
    val glucoseContexts: Map<Int, String>,
    val workoutActivity: String,
    val workoutSessions: String,
    val workoutDate: String,
    val workoutDuration: String,
    val workoutDistance: String,
    val sleepBedtime: String,
    val sleepWake: String,
    val sleepNights: String,
    val sleepAsleep: String,
    val sleepDeep: String,
    val sleepRem: String,
    val sleepLight: String,
    val sleepAwake: String,
    val workoutTypeLabel: (exerciseType: Int) -> String,
    val pageLabel: (page: Int, pageCount: Int) -> String,
)

/** Locale- and unit-aware number rendering, provided by the caller. */
interface ReportValueFormatter {
    /** A summary/stat value with its unit, e.g. "8,432" or "72 bpm". */
    fun value(metric: ReportMetric, value: Double): String

    /** A compact Y-axis value, e.g. "8k". */
    fun axisValue(metric: ReportMetric, value: Double): String

    /** A table-cell value; blood pressure renders "122/81" here. */
    fun pointValue(metric: ReportMetric, point: ReportPoint): String

    /** The bucket's date label, granularity-appropriate. */
    fun bucketLabel(point: ReportPoint, granularity: ReportGranularity): String

    /** A systolic/diastolic pair, e.g. "122/81 mmHg". */
    fun bloodPressure(systolic: Double, diastolic: Double): String

    /** A single reading's date and time, e.g. "Jun 5, 2026, 8:12 AM". */
    fun readingTime(time: java.time.Instant): String

    /** A clock time for an instant, e.g. "11:42 PM". */
    fun timeOfDay(time: java.time.Instant): String

    /** A clock time from minutes-of-day (circular averages), e.g. "23:32". */
    fun clockTime(minutesOfDay: Int): String

    /** An hours-and-minutes duration, e.g. "7h 32m". */
    fun durationHm(durationMs: Long): String

    /** A signed change, e.g. "+1.2 kg" / "-0.8 kg". */
    fun signedValue(metric: ReportMetric, delta: Double): String

    /** A percentage share, e.g. "18%". */
    fun percent(value: Double): String

    /** A plain date, e.g. "Jun 5, 2026". */
    fun date(date: java.time.LocalDate): String
}

/**
 * Renders [ReportData] to a PDF via [PdfDocument] — no dependencies beyond the
 * platform, which is the entire reason this app can ship a PDF exporter at all
 * (offline-only, F-Droid, no third-party engines). Two passes: pure layout
 * planning first (page count known), then drawing.
 *
 * A fixed print palette rather than the Material theme: the artifact is a
 * document handed to a doctor, not a themed screen, and must read the same on
 * paper as in a viewer's dark mode.
 */
class ReportPdfWriter(
    private val labels: ReportPdfLabels,
    private val values: ReportValueFormatter,
    private val logo: Bitmap? = null,
) {
    private companion object {
        const val InkColor = 0xFF1B1B1B.toInt()
        const val MutedColor = 0xFF5F6368.toInt()
        const val FaintColor = 0xFFDADCE0.toInt()
        const val StripeColor = 0xFFF4F5F6.toInt()
        const val AccentColor = 0xFF1B6E53.toInt()
        const val SecondaryAccentColor = 0xFF1A5B8F.toInt()
        const val BandAlpha = 46

        const val ChartHeight = 150f
        const val LogoHeight = 26f
        const val MarkerRadius = 1.7f

        /** What a cell shows when the metric was not recorded for that row. */
        const val EmptyCell = "\u2013"

        /** Width fraction a stats row's lead-in title takes when present. */
        const val StatsTitleFraction = 0.16f
    }

    private fun textPaint(sizePt: Float, color: Int, bold: Boolean = false, italic: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePt
            this.color = color
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                when {
                    bold && italic -> Typeface.BOLD_ITALIC
                    bold -> Typeface.BOLD
                    italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                },
            )
        }

    private val titlePaint = textPaint(15f, InkColor, bold = true)
    private val subtitlePaint = textPaint(8.5f, MutedColor)
    private val noticePaint = textPaint(8.5f, MutedColor, italic = true)
    private val metricTitlePaint = textPaint(11.5f, InkColor, bold = true)
    private val captionPaint = textPaint(7.5f, MutedColor, italic = true)
    private val statLabelPaint = textPaint(7f, MutedColor)
    private val statValuePaint = textPaint(9.5f, InkColor, bold = true)
    private val tableHeaderPaint = textPaint(8f, MutedColor, bold = true)
    private val tableCellPaint = textPaint(8f, InkColor)
    private val statusPaint = textPaint(8.5f, MutedColor, italic = true)
    private val footerPaint = textPaint(7.5f, MutedColor)
    private val axisPaint = textPaint(6.5f, MutedColor)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AccentColor
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        strokeCap = Paint.Cap.ROUND
    }
    private val secondaryLinePaint = Paint(linePaint).apply { color = SecondaryAccentColor }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AccentColor
        alpha = BandAlpha
        style = Paint.Style.FILL
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AccentColor
        style = Paint.Style.FILL
    }
    private val secondaryMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SecondaryAccentColor
        style = Paint.Style.FILL
    }
    private val statsTitlePaint = textPaint(9f, InkColor, bold = true)
    private val gridPaint = Paint().apply {
        color = FaintColor
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }
    private val stripePaint = Paint().apply {
        color = StripeColor
        style = Paint.Style.FILL
    }
    private val rulePaint = Paint().apply {
        color = FaintColor
        strokeWidth = 0.75f
    }
    private val noticeBarPaint = Paint().apply {
        color = MutedColor
        strokeWidth = 1.5f
    }

    fun write(data: ReportData, output: OutputStream) {
        val pages = planPages(buildItems(data))
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val pdfPage = document.startPage(
                    PdfDocument.PageInfo.Builder(
                        PdfPageMetrics.PageWidth.toInt(),
                        PdfPageMetrics.PageHeight.toInt(),
                        index + 1,
                    ).create(),
                )
                drawPage(pdfPage.canvas, page)
                drawFooter(pdfPage.canvas, index + 1, pages.size)
                document.finishPage(pdfPage)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    // ── layout ──────────────────────────────────────────────────────────────

    private fun lineHeight(paint: TextPaint): Float =
        paint.fontMetrics.let { it.descent - it.ascent }

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    private fun buildItems(data: ReportData): List<LayoutItem> {
        val items = mutableListOf<LayoutItem>()
        val width = PdfPageMetrics.ContentWidth.toInt()

        val headerHeight = LogoHeight + 6f + lineHeight(titlePaint) +
            labels.subtitleLines.size * (lineHeight(subtitlePaint) + 1f) + 14f
        items += LayoutItem(
            block = ReportBlock.BrandHeader(labels.reportTitle, labels.subtitleLines),
            height = headerHeight,
        )

        labels.notices.forEach { notice ->
            val height = staticLayout(notice, noticePaint, width - 10).height + 8f
            items += LayoutItem(ReportBlock.Notice(notice), height)
        }
        if (labels.notices.isNotEmpty()) {
            items[items.size - 1] = items.last().copy(height = items.last().height + 6f)
        }

        data.results.forEach { result ->
            val title = labels.metricTitles[result.metric] ?: result.metric.name
            items += LayoutItem(
                block = ReportBlock.MetricTitle(title),
                height = lineHeight(metricTitlePaint) + 8f,
                keepWithNext = true,
            )
            when (result.status) {
                ReportMetricStatus.OK -> {
                    val caption = labels.chartCaptions[result.metric]
                    val chart = buildPdfChart(
                        points = result.points,
                        valueKind = result.metric.valueKind,
                        width = PdfPageMetrics.ContentWidth,
                        height = ChartHeight,
                        formatAxisValue = { values.axisValue(result.metric, it) },
                        bucketLabel = { values.bucketLabel(it, data.request.granularity) },
                        approxCharWidth = axisPaint.textSize * 0.55f,
                    )
                    val captionHeight = if (caption != null) lineHeight(captionPaint) + 3f else 0f
                    items += LayoutItem(
                        block = ReportBlock.Chart(chart, caption),
                        height = ChartHeight + captionHeight + 6f,
                        keepWithNext = true,
                    )
                    when (val detail = result.detail) {
                        is ReportBloodPressureDetail -> items += bloodPressureItems(detail)
                        is ReportGlucoseDetail -> items += glucoseItems(detail)
                        is ReportWorkoutsDetail -> {
                            items += statsItem(result)
                            items += workoutsItems(detail)
                        }
                        is ReportSleepDetail -> {
                            items += statsItem(result)
                            items += sleepItems(detail)
                        }
                        is ReportReadingsDetail -> {
                            items += statsItem(result)
                            items += readingsItems(result.metric, detail)
                        }
                        null -> {
                            items += statsItem(result)
                            items += tableItems(result, data.request.granularity)
                        }
                    }
                }
                ReportMetricStatus.EMPTY -> items += statusItem(labels.statusEmpty)
                ReportMetricStatus.FAILED -> items += statusItem(labels.statusFailed)
                ReportMetricStatus.SKIPPED -> items += statusItem(labels.statusSkipped)
                ReportMetricStatus.MISSING_PERMISSION -> items += statusItem(labels.statusMissingPermission)
            }
        }
        return items
    }

    private fun statusItem(text: String) = LayoutItem(
        block = ReportBlock.StatusLine(text),
        height = lineHeight(statusPaint) + 14f,
    )

    private fun statsItem(result: tech.mmarca.openvitals.domain.model.ReportMetricResult): LayoutItem {
        val summary = result.summary
        val cells = buildList {
            if (summary != null) {
                add(ReportBlock.StatsRow.StatCell(labels.statAverage, values.value(result.metric, summary.average)))
                add(ReportBlock.StatsRow.StatCell(labels.statMin, values.value(result.metric, summary.min)))
                add(ReportBlock.StatsRow.StatCell(labels.statMax, values.value(result.metric, summary.max)))
                summary.total?.let {
                    add(ReportBlock.StatsRow.StatCell(labels.statTotal, values.value(result.metric, it)))
                }
                if (result.metric.section == tech.mmarca.openvitals.domain.model.ReportSection.BODY) {
                    summary.changeOverRange?.let {
                        add(ReportBlock.StatsRow.StatCell(labels.statChange, values.signedValue(result.metric, it)))
                    }
                }
                add(ReportBlock.StatsRow.StatCell(labels.statDays, summary.daysWithData.toString()))
            }
        }
        return LayoutItem(
            block = ReportBlock.StatsRow(cells),
            height = lineHeight(statLabelPaint) + lineHeight(statValuePaint) + 12f,
            keepWithNext = true,
        )
    }

    private fun tableItems(
        result: tech.mmarca.openvitals.domain.model.ReportMetricResult,
        granularity: ReportGranularity,
    ): List<LayoutItem> {
        val headerCells = listOf(labels.tablePeriod, labels.tableValue, labels.tableMin, labels.tableMax)
        val headerHeight = lineHeight(tableHeaderPaint) + 7f
        val rowHeight = lineHeight(tableCellPaint) + 5f
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(headerCells, continued = true),
            height = headerHeight,
        )
        val items = mutableListOf(
            LayoutItem(
                block = ReportBlock.TableHeader(headerCells, continued = false),
                height = headerHeight,
                keepWithNext = true,
            ),
        )
        result.points.forEachIndexed { index, point ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.bucketLabel(point, granularity),
                        values.pointValue(result.metric, point),
                        values.value(result.metric, point.min),
                        values.value(result.metric, point.max),
                    ),
                    striped = index % 2 == 1,
                ),
                height = rowHeight + if (index == result.points.size - 1) 12f else 0f,
                continuationHeader = continuation,
            )
        }
        return items
    }

    /**
     * The blood-pressure section past the chart: per-component stats rows, the
     * time-of-day averages, and every reading — replacing the generic bucket
     * table whose Min/Max columns cannot say WHICH component they mean.
     */
    private fun bloodPressureItems(detail: ReportBloodPressureDetail): List<LayoutItem> {
        val metric = ReportMetric.BLOOD_PRESSURE
        val items = mutableListOf<LayoutItem>()
        val statsHeight = lineHeight(statLabelPaint) + lineHeight(statValuePaint) + 12f

        fun componentRow(title: String, summary: tech.mmarca.openvitals.domain.model.ReportMetricSummary) =
            LayoutItem(
                block = ReportBlock.StatsRow(
                    cells = listOf(
                        ReportBlock.StatsRow.StatCell(labels.statAverage, values.value(metric, summary.average)),
                        ReportBlock.StatsRow.StatCell(labels.statMin, values.value(metric, summary.min)),
                        ReportBlock.StatsRow.StatCell(labels.statMax, values.value(metric, summary.max)),
                        ReportBlock.StatsRow.StatCell(labels.statDays, summary.daysWithData.toString()),
                    ),
                    title = title,
                ),
                height = statsHeight,
                keepWithNext = true,
            )
        items += componentRow(labels.bpSystolic, detail.systolic)
        items += componentRow(labels.bpDiastolic, detail.diastolic)

        val slotColumns = listOf(0.4f, 0.35f, 0.25f)
        val headerHeight = lineHeight(tableHeaderPaint) + 7f
        val rowHeight = lineHeight(tableCellPaint) + 5f
        items += LayoutItem(
            block = ReportBlock.TableHeader(
                cells = listOf(labels.bpTimeOfDay, labels.statAverage, labels.bpReadings),
                continued = false,
                columnFractions = slotColumns,
            ),
            height = headerHeight,
            keepWithNext = true,
        )
        detail.slotAverages.forEachIndexed { index, slot ->
            val name = slot.context?.let { labels.bpContexts[it] } ?: labels.bpAllReadings
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        name,
                        values.bloodPressure(slot.systolic, slot.diastolic),
                        slot.readings.toString(),
                    ),
                    striped = index % 2 == 1,
                    columnFractions = slotColumns,
                ),
                height = rowHeight + if (index == detail.slotAverages.size - 1) 10f else 0f,
            )
        }

        val readingColumns = listOf(0.32f, 0.19f, 0.25f, 0.24f)
        val readingsHeader = listOf(labels.bpDateTime, labels.tableValue, labels.bpTimeOfDay, labels.bpPosition)
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(readingsHeader, continued = true, columnFractions = readingColumns),
            height = headerHeight,
        )
        items += LayoutItem(
            block = ReportBlock.TableHeader(readingsHeader, continued = false, columnFractions = readingColumns),
            height = headerHeight,
            keepWithNext = true,
        )
        detail.readings.forEachIndexed { index, reading ->
            val contextText = (labels.bpContexts[reading.context] ?: "") +
                if (reading.contextEstimated) " *" else ""
            val positionText = listOfNotNull(
                labels.bpPositions[reading.bodyPosition],
                labels.bpLocations[reading.measurementLocation],
            ).joinToString(" \u00b7 ").ifEmpty { EmptyCell }
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.readingTime(reading.time),
                        values.bloodPressure(reading.systolicMmHg.toDouble(), reading.diastolicMmHg.toDouble()),
                        contextText,
                        positionText,
                    ),
                    striped = index % 2 == 1,
                    columnFractions = readingColumns,
                ),
                height = rowHeight + if (index == detail.readings.size - 1) 4f else 0f,
                continuationHeader = continuation,
            )
        }
        if (detail.readings.any { it.contextEstimated }) {
            items += LayoutItem(
                block = ReportBlock.StatusLine(labels.bpEstimatedNote),
                height = lineHeight(statusPaint) + 12f,
            )
        }
        return items
    }

    private fun statsHeight() = lineHeight(statLabelPaint) + lineHeight(statValuePaint) + 12f

    private fun headerHeight() = lineHeight(tableHeaderPaint) + 7f

    private fun rowHeight() = lineHeight(tableCellPaint) + 5f

    /**
     * The glucose section past the chart: overall stats, per-meal-context
     * averages (fasting first, the number a doctor asks for), then every
     * reading with its context.
     */
    private fun glucoseItems(detail: ReportGlucoseDetail): List<LayoutItem> {
        val metric = ReportMetric.BLOOD_GLUCOSE
        val items = mutableListOf<LayoutItem>()
        val summary = detail.summary
        items += LayoutItem(
            block = ReportBlock.StatsRow(
                cells = listOf(
                    ReportBlock.StatsRow.StatCell(labels.statAverage, values.value(metric, summary.average)),
                    ReportBlock.StatsRow.StatCell(labels.statMin, values.value(metric, summary.min)),
                    ReportBlock.StatsRow.StatCell(labels.statMax, values.value(metric, summary.max)),
                    ReportBlock.StatsRow.StatCell(labels.statDays, summary.daysWithData.toString()),
                ),
            ),
            height = statsHeight(),
            keepWithNext = true,
        )

        val contextColumns = listOf(0.34f, 0.22f, 0.22f, 0.22f)
        items += LayoutItem(
            block = ReportBlock.TableHeader(
                cells = listOf(labels.glucoseContext, labels.statAverage, labels.statMin, labels.statMax),
                continued = false,
                columnFractions = contextColumns,
            ),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.contextAverages.forEachIndexed { index, context ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        contextLabel(context.relationToMeal),
                        values.value(metric, context.average),
                        values.value(metric, context.min),
                        values.value(metric, context.max),
                    ),
                    striped = index % 2 == 1,
                    columnFractions = contextColumns,
                ),
                height = rowHeight() + if (index == detail.contextAverages.size - 1) 10f else 0f,
            )
        }

        val readingColumns = listOf(0.42f, 0.28f, 0.3f)
        val readingsHeader = listOf(labels.bpDateTime, labels.tableValue, labels.glucoseContext)
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(readingsHeader, continued = true, columnFractions = readingColumns),
            height = headerHeight(),
        )
        items += LayoutItem(
            block = ReportBlock.TableHeader(readingsHeader, continued = false, columnFractions = readingColumns),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.readings.forEachIndexed { index, reading ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.readingTime(reading.time),
                        values.value(metric, reading.millimolesPerLiter),
                        contextLabel(reading.relationToMeal),
                    ),
                    striped = index % 2 == 1,
                    columnFractions = readingColumns,
                ),
                height = rowHeight() + if (index == detail.readings.size - 1) 12f else 0f,
                continuationHeader = continuation,
            )
        }
        return items
    }

    private fun contextLabel(relationToMeal: Int?): String =
        relationToMeal?.let { labels.glucoseContexts[it] } ?: labels.bpAllReadings

    /** Totals per activity type, then the session list: what was actually done. */
    private fun workoutsItems(detail: ReportWorkoutsDetail): List<LayoutItem> {
        val items = mutableListOf<LayoutItem>()

        val typeColumns = listOf(0.4f, 0.2f, 0.2f, 0.2f)
        items += LayoutItem(
            block = ReportBlock.TableHeader(
                cells = listOf(labels.workoutActivity, labels.workoutSessions, labels.workoutDuration, labels.workoutDistance),
                continued = false,
                columnFractions = typeColumns,
            ),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.byType.forEachIndexed { index, total ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        labels.workoutTypeLabel(total.exerciseType),
                        total.sessions.toString(),
                        values.durationHm(total.totalDurationMs),
                        total.totalDistanceMeters?.let { values.value(ReportMetric.DISTANCE, it) } ?: EmptyCell,
                    ),
                    striped = index % 2 == 1,
                    columnFractions = typeColumns,
                ),
                height = rowHeight() + if (index == detail.byType.size - 1) 10f else 0f,
            )
        }

        val sessionColumns = listOf(0.3f, 0.3f, 0.2f, 0.2f)
        val sessionsHeader =
            listOf(labels.workoutDate, labels.workoutActivity, labels.workoutDuration, labels.workoutDistance)
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(sessionsHeader, continued = true, columnFractions = sessionColumns),
            height = headerHeight(),
        )
        items += LayoutItem(
            block = ReportBlock.TableHeader(sessionsHeader, continued = false, columnFractions = sessionColumns),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.sessions.forEachIndexed { index, session ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.readingTime(session.start),
                        session.title ?: labels.workoutTypeLabel(session.exerciseType),
                        values.durationHm(session.durationMs),
                        session.distanceMeters?.let { values.value(ReportMetric.DISTANCE, it) } ?: EmptyCell,
                    ),
                    striped = index % 2 == 1,
                    columnFractions = sessionColumns,
                ),
                height = rowHeight() + if (index == detail.sessions.size - 1) 12f else 0f,
                continuationHeader = continuation,
            )
        }
        return items
    }

    /** Schedule averages, stage mix, then one row per night. */
    private fun sleepItems(detail: ReportSleepDetail): List<LayoutItem> {
        val items = mutableListOf<LayoutItem>()

        items += LayoutItem(
            block = ReportBlock.StatsRow(
                cells = buildList {
                    detail.averageBedtimeMinutes?.let {
                        add(ReportBlock.StatsRow.StatCell(labels.sleepBedtime, values.clockTime(it)))
                    }
                    detail.averageWakeMinutes?.let {
                        add(ReportBlock.StatsRow.StatCell(labels.sleepWake, values.clockTime(it)))
                    }
                    add(ReportBlock.StatsRow.StatCell(labels.sleepNights, detail.nightsWithData.toString()))
                },
            ),
            height = statsHeight(),
            keepWithNext = true,
        )
        detail.stageMix?.let { mix ->
            items += LayoutItem(
                block = ReportBlock.StatsRow(
                    cells = listOf(
                        ReportBlock.StatsRow.StatCell(labels.sleepDeep, values.percent(mix.deepPct)),
                        ReportBlock.StatsRow.StatCell(labels.sleepRem, values.percent(mix.remPct)),
                        ReportBlock.StatsRow.StatCell(labels.sleepLight, values.percent(mix.lightPct)),
                        ReportBlock.StatsRow.StatCell(labels.sleepAwake, values.percent(mix.awakePct)),
                    ),
                ),
                height = statsHeight(),
                keepWithNext = true,
            )
        }

        val nightColumns = listOf(0.21f, 0.16f, 0.16f, 0.17f, 0.15f, 0.15f)
        val header = listOf(
            labels.workoutDate,
            labels.sleepBedtime,
            labels.sleepWake,
            labels.sleepAsleep,
            labels.sleepDeep,
            labels.sleepRem,
        )
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(header, continued = true, columnFractions = nightColumns),
            height = headerHeight(),
        )
        items += LayoutItem(
            block = ReportBlock.TableHeader(header, continued = false, columnFractions = nightColumns),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.nights.forEachIndexed { index, night ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.date(night.date),
                        values.timeOfDay(night.bedtime),
                        values.timeOfDay(night.wake),
                        values.durationHm(night.asleepMs),
                        night.deepMs?.let { values.durationHm(it) } ?: EmptyCell,
                        night.remMs?.let { values.durationHm(it) } ?: EmptyCell,
                    ),
                    striped = index % 2 == 1,
                    columnFractions = nightColumns,
                ),
                height = rowHeight() + if (index == detail.nights.size - 1) 12f else 0f,
                continuationHeader = continuation,
            )
        }
        return items
    }

    /** The sparse readings list (body temperature): every measurement, dated. */
    private fun readingsItems(metric: ReportMetric, detail: ReportReadingsDetail): List<LayoutItem> {
        val items = mutableListOf<LayoutItem>()
        val columns = listOf(0.55f, 0.45f)
        val header = listOf(labels.bpDateTime, labels.tableValue)
        val continuation = LayoutItem(
            block = ReportBlock.TableHeader(header, continued = true, columnFractions = columns),
            height = headerHeight(),
        )
        items += LayoutItem(
            block = ReportBlock.TableHeader(header, continued = false, columnFractions = columns),
            height = headerHeight(),
            keepWithNext = true,
        )
        detail.readings.forEachIndexed { index, reading ->
            items += LayoutItem(
                block = ReportBlock.TableRow(
                    cells = listOf(
                        values.readingTime(reading.time),
                        values.value(metric, reading.value),
                    ),
                    striped = index % 2 == 1,
                    columnFractions = columns,
                ),
                height = rowHeight() + if (index == detail.readings.size - 1) 12f else 0f,
                continuationHeader = continuation,
            )
        }
        return items
    }

    // ── drawing ─────────────────────────────────────────────────────────────

    private fun drawPage(canvas: Canvas, page: ReportPage) {
        page.blocks.forEach { placed ->
            when (val block = placed.block) {
                is ReportBlock.BrandHeader -> drawBrandHeader(canvas, block, placed.y)
                is ReportBlock.Notice -> drawNotice(canvas, block, placed.y)
                is ReportBlock.MetricTitle -> drawText(canvas, block.text, metricTitlePaint, placed.y)
                is ReportBlock.Chart -> drawChart(canvas, block, placed.y)
                is ReportBlock.StatsRow -> drawStatsRow(canvas, block, placed.y)
                is ReportBlock.TableHeader -> drawTableHeader(canvas, block, placed.y)
                is ReportBlock.TableRow -> drawTableRow(canvas, block, placed.y)
                is ReportBlock.StatusLine -> drawText(canvas, block.text, statusPaint, placed.y)
            }
        }
    }

    private fun drawText(canvas: Canvas, text: String, paint: TextPaint, y: Float, x: Float = PdfPageMetrics.MarginHorizontal) {
        canvas.drawText(
            TextUtils.ellipsize(text, paint, PdfPageMetrics.ContentWidth, TextUtils.TruncateAt.END).toString(),
            x,
            y - paint.fontMetrics.ascent,
            paint,
        )
    }

    private fun drawBrandHeader(canvas: Canvas, block: ReportBlock.BrandHeader, y: Float) {
        var cursor = y
        logo?.let { bitmap ->
            val aspect = bitmap.width.toFloat() / bitmap.height
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    PdfPageMetrics.MarginHorizontal,
                    cursor,
                    PdfPageMetrics.MarginHorizontal + LogoHeight * aspect,
                    cursor + LogoHeight,
                ),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        cursor += LogoHeight + 6f
        drawText(canvas, block.title, titlePaint, cursor)
        cursor += lineHeight(titlePaint)
        block.subtitleLines.forEach { line ->
            drawText(canvas, line, subtitlePaint, cursor)
            cursor += lineHeight(subtitlePaint) + 1f
        }
        val ruleY = y + heightOfHeader(block) - 7f
        canvas.drawLine(
            PdfPageMetrics.MarginHorizontal,
            ruleY,
            PdfPageMetrics.PageWidth - PdfPageMetrics.MarginHorizontal,
            ruleY,
            rulePaint,
        )
    }

    private fun heightOfHeader(block: ReportBlock.BrandHeader): Float =
        LogoHeight + 6f + lineHeight(titlePaint) +
            block.subtitleLines.size * (lineHeight(subtitlePaint) + 1f) + 14f

    private fun drawNotice(canvas: Canvas, block: ReportBlock.Notice, y: Float) {
        val layout = staticLayout(block.text, noticePaint, PdfPageMetrics.ContentWidth.toInt() - 10)
        canvas.drawLine(
            PdfPageMetrics.MarginHorizontal + 1f,
            y + 1f,
            PdfPageMetrics.MarginHorizontal + 1f,
            y + layout.height + 1f,
            noticeBarPaint,
        )
        canvas.withTranslation(PdfPageMetrics.MarginHorizontal + 10f, y) { layout.draw(this) }
    }

    private fun drawChart(canvas: Canvas, block: ReportBlock.Chart, y: Float) {
        val chart = block.chart
        canvas.withTranslation(PdfPageMetrics.MarginHorizontal, y) {
            chart.gridLineYs.forEach { gridY ->
                drawLine(chart.plotLeft, gridY, chart.plotRight, gridY, gridPaint)
            }
            chart.yAxisLabels.forEach { label ->
                val textWidth = axisPaint.measureText(label.text)
                drawText(
                    label.text,
                    chart.plotLeft - 6f - textWidth,
                    label.position - (axisPaint.fontMetrics.ascent + axisPaint.fontMetrics.descent) / 2f,
                    axisPaint,
                )
            }
            chart.bars.forEach { bar ->
                drawRect(bar.left, bar.top, bar.right, bar.bottom, barPaint)
            }
            if (chart.bandMaxPoints.size > 1 && chart.bandMinPoints.size > 1) {
                // A closed polygon rather than two joined splines: appending a
                // second smooth contour would start its own subpath and break
                // the fill, and at band alpha the faceting is invisible.
                val band = Path()
                band.moveTo(chart.bandMaxPoints.first().x, chart.bandMaxPoints.first().y)
                chart.bandMaxPoints.drop(1).forEach { band.lineTo(it.x, it.y) }
                chart.bandMinPoints.asReversed().forEach { band.lineTo(it.x, it.y) }
                band.close()
                drawPath(band, bandPaint)
            }
            if (chart.linePoints.size > 1) {
                drawPath(smoothPath(chart.linePoints).asAndroidPath(), linePaint)
            } else if (chart.linePoints.size == 1) {
                drawCircle(chart.linePoints.single().x, chart.linePoints.single().y, 2f, barPaint)
            }
            if (chart.secondaryLinePoints.size > 1) {
                drawPath(smoothPath(chart.secondaryLinePoints).asAndroidPath(), secondaryLinePaint)
            }
            chart.lineMarkers.forEach { marker ->
                drawCircle(marker.x, marker.y, MarkerRadius, barPaint)
            }
            chart.secondaryLineMarkers.forEach { marker ->
                drawCircle(marker.x, marker.y, MarkerRadius, secondaryMarkerPaint)
            }
            chart.xAxisLabels.forEach { label ->
                val textWidth = axisPaint.measureText(label.text)
                drawText(
                    label.text,
                    label.position - textWidth / 2f,
                    chart.plotBottom - axisPaint.fontMetrics.ascent + 3f,
                    axisPaint,
                )
            }
        }
        block.caption?.let { caption ->
            drawText(canvas, caption, captionPaint, y + chart.height + 3f)
        }
    }

    private fun drawStatsRow(canvas: Canvas, block: ReportBlock.StatsRow, y: Float) {
        if (block.cells.isEmpty()) return
        var cellsLeft = PdfPageMetrics.MarginHorizontal
        var cellsWidth = PdfPageMetrics.ContentWidth
        block.title?.let { title ->
            canvas.drawText(
                title,
                cellsLeft,
                y + lineHeight(statLabelPaint) + 2f - statsTitlePaint.fontMetrics.ascent,
                statsTitlePaint,
            )
            cellsLeft += PdfPageMetrics.ContentWidth * StatsTitleFraction
            cellsWidth -= PdfPageMetrics.ContentWidth * StatsTitleFraction
        }
        val cellWidth = cellsWidth / block.cells.size
        block.cells.forEachIndexed { index, cell ->
            val x = cellsLeft + cellWidth * index
            canvas.drawText(cell.label, x, y - statLabelPaint.fontMetrics.ascent, statLabelPaint)
            canvas.drawText(
                TextUtils.ellipsize(cell.value, statValuePaint, cellWidth - 6f, TextUtils.TruncateAt.END).toString(),
                x,
                y + lineHeight(statLabelPaint) + 2f - statValuePaint.fontMetrics.ascent,
                statValuePaint,
            )
        }
    }

    private fun tableColumnX(fractions: List<Float>, index: Int): Float {
        var x = PdfPageMetrics.MarginHorizontal
        for (i in 0 until index) x += PdfPageMetrics.ContentWidth * fractions[i]
        return x
    }

    private fun drawTableHeader(canvas: Canvas, block: ReportBlock.TableHeader, y: Float) {
        block.cells.forEachIndexed { index, cell ->
            val text = if (index == 0 && block.continued) "$cell ${labels.tableContinued}" else cell
            canvas.drawText(
                TextUtils.ellipsize(
                    text,
                    tableHeaderPaint,
                    PdfPageMetrics.ContentWidth * block.columnFractions[index] - 4f,
                    TextUtils.TruncateAt.END,
                ).toString(),
                tableColumnX(block.columnFractions, index),
                y - tableHeaderPaint.fontMetrics.ascent,
                tableHeaderPaint,
            )
        }
        val ruleY = y + lineHeight(tableHeaderPaint) + 3f
        canvas.drawLine(
            PdfPageMetrics.MarginHorizontal,
            ruleY,
            PdfPageMetrics.PageWidth - PdfPageMetrics.MarginHorizontal,
            ruleY,
            rulePaint,
        )
    }

    private fun drawTableRow(canvas: Canvas, block: ReportBlock.TableRow, y: Float) {
        if (block.striped) {
            canvas.drawRect(
                PdfPageMetrics.MarginHorizontal,
                y - 1f,
                PdfPageMetrics.PageWidth - PdfPageMetrics.MarginHorizontal,
                y + lineHeight(tableCellPaint) + 3f,
                stripePaint,
            )
        }
        block.cells.forEachIndexed { index, cell ->
            canvas.drawText(
                TextUtils.ellipsize(
                    cell,
                    tableCellPaint,
                    PdfPageMetrics.ContentWidth * block.columnFractions[index] - 4f,
                    TextUtils.TruncateAt.END,
                ).toString(),
                tableColumnX(block.columnFractions, index),
                y + 1f - tableCellPaint.fontMetrics.ascent,
                tableCellPaint,
            )
        }
    }

    private fun drawFooter(canvas: Canvas, page: Int, pageCount: Int) {
        val text = labels.pageLabel(page, pageCount)
        val width = footerPaint.measureText(text)
        canvas.drawText(
            text,
            (PdfPageMetrics.PageWidth - width) / 2f,
            PdfPageMetrics.PageHeight - 18f,
            footerPaint,
        )
    }

    private inline fun Canvas.withTranslation(x: Float, y: Float, block: Canvas.() -> Unit) {
        val checkpoint = save()
        translate(x, y)
        try {
            block()
        } finally {
            restoreToCount(checkpoint)
        }
    }
}
