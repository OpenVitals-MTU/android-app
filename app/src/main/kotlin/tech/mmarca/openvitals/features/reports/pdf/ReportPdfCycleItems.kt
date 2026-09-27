package tech.mmarca.openvitals.features.reports.pdf

import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.model.ReportCycleDetail

/** The cycle section's strings, resolved before the writer runs. */
data class ReportPdfCycleLabels(
    val cycles: String,
    val meanLength: String,
    val medianLength: String,
    val sdLength: String,
    val range: String,
    val meanBleeding: String,
    val bleedingDays: String,
    val spottingDays: String,
    val intermenstrualDays: String,
    val painOnBleeding: String,
    val painOffBleeding: String,
    val severePainDays: String,
    val meanPain: String,
    val start: String,
    val end: String,
    val length: String,
    val peakFlow: String,
    val painDays: String,
    val excluded: String,
    val inProgress: String,
    val symptom: String,
    val onBleeding: String,
    val offBleeding: String,
    val notes: String,
    val date: String,
    val disclaimer: String,
    /** Flow level to its label; an unknown level falls back to [flowUnknown]. */
    val flowLabels: Map<Int, String>,
    val flowUnknown: String,
    val symptomLabels: Map<CycleSymptom, String>,
    val exclusionReasons: Map<CycleExclusionReason, String>,
)

private val CycleColumns = listOf(0.17f, 0.17f, 0.12f, 0.14f, 0.14f, 0.12f, 0.14f)
private val SymptomColumns = listOf(0.5f, 0.25f, 0.25f)
private val NoteColumns = listOf(0.25f, 0.75f)
private const val LastRowGap = 12f
private const val EmptyCell = "–"

/**
 * The cycle section past the chart: length statistics, bleeding and pain
 * counts, one row per cycle, the symptoms by phase, then the notes. Lives
 * outside the writer, which is at its size ceiling.
 */
internal fun cycleItems(
    detail: ReportCycleDetail,
    labels: ReportPdfCycleLabels,
    values: ReportValueFormatter,
    statsHeight: Float,
    headerHeight: Float,
    rowHeight: Float,
): List<LayoutItem> {
    val items = mutableListOf<LayoutItem>()

    fun stats(cells: List<ReportBlock.StatsRow.StatCell>) {
        if (cells.isNotEmpty()) items += LayoutItem(ReportBlock.StatsRow(cells), statsHeight, keepWithNext = true)
    }

    fun cell(label: String, value: String) = ReportBlock.StatsRow.StatCell(label, value)

    stats(
        buildList {
            add(cell(labels.cycles, detail.completedCycles.toString()))
            detail.meanLengthDays?.let { add(cell(labels.meanLength, values.decimal(it, 1))) }
            detail.medianLengthDays?.let { add(cell(labels.medianLength, values.decimal(it, 1))) }
            detail.sdLengthDays?.let { add(cell(labels.sdLength, values.decimal(it, 1))) }
            if (detail.minLengthDays != null && detail.maxLengthDays != null) {
                add(cell(labels.range, "${detail.minLengthDays}–${detail.maxLengthDays}"))
            }
            detail.meanBleedingDays?.let { add(cell(labels.meanBleeding, values.decimal(it, 1))) }
        },
    )
    stats(
        listOf(
            cell(labels.bleedingDays, detail.bleedingDays.toString()),
            cell(labels.spottingDays, detail.spottingDays.toString()),
            cell(labels.intermenstrualDays, detail.intermenstrualDays.toString()),
        ),
    )
    if (detail.meanPain != null) {
        stats(
            listOf(
                cell(labels.painOnBleeding, detail.painDaysOnBleeding.toString()),
                cell(labels.painOffBleeding, detail.painDaysOffBleeding.toString()),
                cell(labels.severePainDays, detail.severePainDays.toString()),
                cell(labels.meanPain, values.decimal(detail.meanPain, 1)),
            ),
        )
    }

    items += table(
        header = listOf(labels.start, labels.end, labels.length, labels.bleedingDays, labels.peakFlow, labels.painDays, labels.excluded),
        columns = CycleColumns,
        rows = detail.cycles.map { row ->
            listOf(
                values.date(row.start),
                row.end?.let(values::date) ?: labels.inProgress,
                row.lengthDays?.toString() ?: EmptyCell,
                row.bleedingDays.toString(),
                row.peakFlow?.let { labels.flowLabels[it] ?: labels.flowUnknown } ?: EmptyCell,
                row.painDays.toString(),
                when {
                    !row.excluded -> EmptyCell
                    row.exclusionReason != null -> labels.exclusionReasons[row.exclusionReason] ?: labels.excluded
                    else -> labels.excluded
                },
            )
        },
        headerHeight = headerHeight,
        rowHeight = rowHeight,
    )
    if (detail.symptomFrequency.isNotEmpty()) {
        items += table(
            header = listOf(labels.symptom, labels.onBleeding, labels.offBleeding),
            columns = SymptomColumns,
            rows = detail.symptomFrequency.map { frequency ->
                listOf(
                    labels.symptomLabels[frequency.symptom] ?: frequency.symptom.id,
                    frequency.onBleeding.toString(),
                    frequency.offBleeding.toString(),
                )
            },
            headerHeight = headerHeight,
            rowHeight = rowHeight,
        )
    }
    if (detail.notes.isNotEmpty()) {
        items += table(
            header = listOf(labels.date, labels.notes),
            columns = NoteColumns,
            rows = detail.notes.map { note -> listOf(values.date(note.date), note.text) },
            headerHeight = headerHeight,
            rowHeight = rowHeight,
        )
    }
    items += LayoutItem(ReportBlock.StatusLine(labels.disclaimer), height = rowHeight + LastRowGap)
    return items
}

/** A header that repeats after a page break, then striped rows; nothing for an empty list. */
private fun table(
    header: List<String>,
    columns: List<Float>,
    rows: List<List<String>>,
    headerHeight: Float,
    rowHeight: Float,
): List<LayoutItem> {
    if (rows.isEmpty()) return emptyList()
    val continuation = LayoutItem(
        block = ReportBlock.TableHeader(header, continued = true, columnFractions = columns),
        height = headerHeight,
    )
    return buildList {
        add(LayoutItem(ReportBlock.TableHeader(header, continued = false, columnFractions = columns), headerHeight, keepWithNext = true))
        rows.forEachIndexed { index, cells ->
            add(
                LayoutItem(
                    block = ReportBlock.TableRow(cells, striped = index % 2 == 1, columnFractions = columns),
                    height = rowHeight + if (index == rows.size - 1) LastRowGap else 0f,
                    continuationHeader = continuation,
                ),
            )
        }
    }
}
