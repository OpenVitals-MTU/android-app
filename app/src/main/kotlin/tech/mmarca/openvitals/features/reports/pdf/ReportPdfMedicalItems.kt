package tech.mmarca.openvitals.features.reports.pdf

import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.ReportMedicalRow
import tech.mmarca.openvitals.domain.model.ReportMedicalSection

/** The medical section's strings, resolved before the writer runs. */
data class ReportPdfMedicalLabels(
    val title: String,
    val scope: String,
    val categoryTitles: Map<MedicalCategory, String>,
    val date: String,
    val status: String,
    val value: String,
    val source: String,
    /** "<category>: none recorded", for the standing lists. */
    val noneRecorded: (category: String) -> String,
    /** "<category>: none in this range", for vaccines and lab results. */
    val noneInRange: (category: String) -> String,
    val ownOnly: (categories: String) -> String,
    val leftOut: (categories: String) -> String,
    val failed: (categories: String) -> String,
    val disclaimer: String,
    /** A FHIR status code as a word. */
    val statusLabel: (code: String) -> String,
    /** A FHIR date as written, in the locale's format. */
    val dateLabel: (text: String) -> String,
)

private val RecordColumns = listOf(0.42f, 0.18f, 0.16f, 0.24f)
private val LabColumns = listOf(0.34f, 0.16f, 0.28f, 0.22f)
private const val LastRowGap = 12f
private const val EmptyCell = "–"

/**
 * The medical section: what the range covers, then one table per category, then what access
 * kept out. Rows are shown as received; nothing is computed. [notice] builds a wrapping line.
 */
internal fun medicalItems(
    section: ReportMedicalSection,
    labels: ReportPdfMedicalLabels,
    titleItem: LayoutItem,
    notice: (String) -> LayoutItem,
    headerHeight: Float,
    rowHeight: Float,
): List<LayoutItem> {
    val items = mutableListOf(titleItem, notice(labels.scope))

    fun category(category: MedicalCategory, rows: List<ReportMedicalRow>, inRange: Boolean, lab: Boolean = false) {
        val title = labels.categoryTitles[category] ?: category.name
        if (rows.isEmpty()) {
            val text = if (inRange) labels.noneInRange(title) else labels.noneRecorded(title)
            items += LayoutItem(ReportBlock.StatusLine(text), height = rowHeight + LastRowGap)
            return
        }
        val header = if (lab) listOf(title, labels.date, labels.value, labels.source) else listOf(title, labels.date, labels.status, labels.source)
        val columns = if (lab) LabColumns else RecordColumns
        items += table(header = header, columns = columns, rows = rows.map { row -> cells(row, lab, labels) }, headerHeight = headerHeight, rowHeight = rowHeight)
    }

    category(MedicalCategory.ALLERGIES, section.allergies, inRange = false)
    category(MedicalCategory.CONDITIONS, section.conditions, inRange = false)
    category(MedicalCategory.MEDICATIONS, section.medications, inRange = false)
    category(MedicalCategory.VACCINES, section.vaccines, inRange = true)
    category(MedicalCategory.LAB_RESULTS, section.labResults, inRange = true, lab = true)

    fun names(categories: Set<MedicalCategory>) = categories.sortedBy { it.ordinal }.joinToString(", ") { labels.categoryTitles[it] ?: it.name }
    if (section.ownOnly.isNotEmpty()) items += notice(labels.ownOnly(names(section.ownOnly)))
    if (section.leftOut.isNotEmpty()) items += notice(labels.leftOut(names(section.leftOut)))
    if (section.failed.isNotEmpty()) items += notice(labels.failed(names(section.failed)))
    items += LayoutItem(ReportBlock.StatusLine(labels.disclaimer), height = rowHeight + LastRowGap)
    return items
}

/** A lab row shows its value and the flag the lab set; the others their status. Untitled rows name their type. */
private fun cells(row: ReportMedicalRow, lab: Boolean, labels: ReportPdfMedicalLabels): List<String> = listOf(
    row.title ?: row.resourceType,
    row.date?.let(labels.dateLabel) ?: EmptyCell,
    if (lab) {
        listOfNotNull(row.value, row.flag).joinToString(" · ").ifEmpty { EmptyCell }
    } else {
        row.status?.let(labels.statusLabel) ?: EmptyCell
    },
    row.source ?: EmptyCell,
)
