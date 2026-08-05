package tech.mmarca.openvitals.features.reports.pdf

/**
 * The report's page geometry, in PDF points. A4 at 72 dpi; margins sized for
 * print, with extra room at the bottom for the page-number footer.
 */
object PdfPageMetrics {
    const val PageWidth = 595f
    const val PageHeight = 842f
    const val MarginHorizontal = 40f
    const val MarginTop = 36f
    const val MarginBottom = 44f

    const val ContentWidth = PageWidth - 2 * MarginHorizontal
    const val ContentBottom = PageHeight - MarginBottom
}

/**
 * Everything a report page can carry. Blocks are pure data — the writer draws
 * them, the layout only stacks them — so pagination is a JVM-testable function
 * of block heights alone.
 */
sealed interface ReportBlock {
    /** Page-1 masthead: logo, app name, generated-at and range lines. */
    data class BrandHeader(val title: String, val subtitleLines: List<String>) : ReportBlock

    /** A callout line: missing permissions, truncated range, cancelled build. */
    data class Notice(val text: String) : ReportBlock

    /** A metric's section title. */
    data class MetricTitle(val text: String) : ReportBlock

    /** The metric's chart with its one-line caption. */
    data class Chart(val chart: PdfChartData, val caption: String?) : ReportBlock

    /**
     * The avg/min/max/total/days stats strip under a chart. [title] is a bold
     * lead-in cell — blood pressure uses it to keep systolic and diastolic
     * stats on separate, labeled rows.
     */
    data class StatsRow(
        val cells: List<StatCell>,
        val title: String? = null,
    ) : ReportBlock {
        data class StatCell(val label: String, val value: String)
    }

    /**
     * The table header row; repeated (as "continued") after a page break.
     * [columnFractions] splits the content width; defaults to the generic
     * bucket-table layout.
     */
    data class TableHeader(
        val cells: List<String>,
        val continued: Boolean,
        val columnFractions: List<Float> = DefaultTableColumns,
    ) : ReportBlock

    data class TableRow(
        val cells: List<String>,
        val striped: Boolean,
        val columnFractions: List<Float> = DefaultTableColumns,
    ) : ReportBlock

    /** One-line status body for EMPTY / FAILED / SKIPPED / MISSING_PERMISSION. */
    data class StatusLine(val text: String) : ReportBlock
}

/** The generic bucket table's column split: period wide, three value columns. */
val DefaultTableColumns = listOf(0.34f, 0.22f, 0.22f, 0.22f)

/**
 * A block plus what pagination needs to know about it. [keepWithNext] glues
 * this item to the one after it — a metric title never strands at the bottom
 * of a page with its chart on the next. [continuationHeader] is placed at the
 * top of the new page when a break lands between this item and the previous
 * one of the same run (table rows carry their header this way).
 */
data class LayoutItem(
    val block: ReportBlock,
    val height: Float,
    val keepWithNext: Boolean = false,
    val continuationHeader: LayoutItem? = null,
)

data class PlacedBlock(val block: ReportBlock, val y: Float)

data class ReportPage(val blocks: List<PlacedBlock>)

/**
 * Flows [items] into pages. Keep-together chains move to the next page as a
 * unit when they don't fit — unless the chain is taller than a whole page, in
 * which case it places anyway and breaks inside (never an infinite loop).
 * Page count is known when this returns, so the writer can stamp
 * "Page X of Y" without a second pass over the content.
 */
fun planPages(items: List<LayoutItem>): List<ReportPage> {
    val pages = mutableListOf<ReportPage>()
    var current = mutableListOf<PlacedBlock>()
    var y = PdfPageMetrics.MarginTop

    fun breakPage() {
        pages += ReportPage(current)
        current = mutableListOf()
        y = PdfPageMetrics.MarginTop
    }

    fun place(item: LayoutItem) {
        current += PlacedBlock(item.block, y)
        y += item.height
    }

    var index = 0
    while (index < items.size) {
        val item = items[index]

        // The maximal run this item pins to the same page: every item whose
        // keepWithNext points forward, plus the one that run lands on.
        var chainEnd = index
        while (chainEnd < items.size - 1 && items[chainEnd].keepWithNext) chainEnd++
        val chainHeight = (index..chainEnd).sumOf { items[it].height.toDouble() }.toFloat()

        val remaining = PdfPageMetrics.ContentBottom - y
        val pageIsEmpty = current.isEmpty()
        val fullPage = PdfPageMetrics.ContentBottom - PdfPageMetrics.MarginTop

        if (chainHeight <= remaining) {
            (index..chainEnd).forEach { place(items[it]) }
            index = chainEnd + 1
            continue
        }
        if (!pageIsEmpty) {
            breakPage()
            // A table run resuming on the new page gets its header again.
            item.continuationHeader?.let(::place)
            continue
        }
        // An empty page still can't hold the chain: give up on keep-together
        // for this chain and flow it item by item.
        var i = index
        while (i <= chainEnd) {
            val one = items[i]
            if (one.height > PdfPageMetrics.ContentBottom - y && current.isNotEmpty()) {
                breakPage()
                one.continuationHeader?.let(::place)
            }
            place(one)
            i++
        }
        index = chainEnd + 1
        if (chainHeight > fullPage && index < items.size && PdfPageMetrics.ContentBottom - y <= 0f) {
            breakPage()
        }
    }
    if (current.isNotEmpty() || pages.isEmpty()) pages += ReportPage(current)
    return pages
}
