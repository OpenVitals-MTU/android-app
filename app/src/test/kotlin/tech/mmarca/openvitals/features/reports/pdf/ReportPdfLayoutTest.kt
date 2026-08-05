package tech.mmarca.openvitals.features.reports.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPdfLayoutTest {

    private val usableHeight = PdfPageMetrics.ContentBottom - PdfPageMetrics.MarginTop

    private fun title(height: Float = 20f, keepWithNext: Boolean = true) =
        LayoutItem(ReportBlock.MetricTitle("t"), height, keepWithNext = keepWithNext)

    private fun status(height: Float = 22f) =
        LayoutItem(ReportBlock.StatusLine("s"), height)

    private fun header(continued: Boolean = false, height: Float = 18f, keepWithNext: Boolean = !continued) =
        LayoutItem(ReportBlock.TableHeader(listOf("a", "b"), continued), height, keepWithNext = keepWithNext)

    private fun row(height: Float = 14f, continuation: LayoutItem = header(continued = true)) =
        LayoutItem(ReportBlock.TableRow(listOf("a", "b"), striped = false), height, continuationHeader = continuation)

    @Test fun `items that fit stack onto one page top to bottom`() {
        val pages = planPages(listOf(title(), status(), status()))

        assertEquals(1, pages.size)
        val ys = pages.single().blocks.map { it.y }
        assertEquals(PdfPageMetrics.MarginTop, ys.first(), 1e-3f)
        assertTrue(ys.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun `rows spill onto as many pages as they need`() {
        val rowCount = 200
        val items = listOf(header()) + List(rowCount) { row() }

        val pages = planPages(items)

        assertTrue(pages.size > 1)
        val placedRows = pages.sumOf { page -> page.blocks.count { it.block is ReportBlock.TableRow } }
        assertEquals(rowCount, placedRows)
        pages.forEach { page ->
            page.blocks.forEach { assertTrue(it.y + 1f <= PdfPageMetrics.ContentBottom) }
        }
    }

    @Test fun `a spilled table repeats its header as continued on the next page`() {
        val items = listOf(header()) + List(200) { row() }

        val pages = planPages(items)

        pages.drop(1).forEach { page ->
            val first = page.blocks.first().block
            assertTrue("page must resume with a header, got $first", first is ReportBlock.TableHeader)
            assertTrue((first as ReportBlock.TableHeader).continued)
        }
    }

    @Test fun `a keep-together chain moves to the next page as a unit`() {
        val filler = status(height = usableHeight - 30f)
        val chain = listOf(
            title(height = 20f),
            LayoutItem(ReportBlock.StatusLine("chart"), 150f),
        )

        val pages = planPages(listOf(filler) + chain)

        assertEquals(2, pages.size)
        assertEquals(1, pages[0].blocks.size)
        assertEquals(2, pages[1].blocks.size)
        assertTrue(pages[1].blocks.first().block is ReportBlock.MetricTitle)
    }

    @Test fun `a chain taller than a page still places instead of looping`() {
        val giant = List(4) { index ->
            LayoutItem(
                ReportBlock.StatusLine("giant$index"),
                usableHeight * 0.6f,
                keepWithNext = index < 3,
            )
        }

        val pages = planPages(giant)

        assertTrue(pages.size >= 2)
        assertEquals(4, pages.sumOf { it.blocks.size })
    }

    @Test fun `an empty report still yields one page`() {
        assertEquals(1, planPages(emptyList()).size)
    }

    @Test fun `a title never strands at the bottom without its content`() {
        val almostFull = status(height = usableHeight - 25f)

        val pages = planPages(listOf(almostFull, title(height = 20f), status(height = 30f)))

        assertEquals(2, pages.size)
        assertTrue(pages[1].blocks.first().block is ReportBlock.MetricTitle)
    }
}
