package tech.mmarca.openvitals.features.reports.pdf

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ReportPoint
import tech.mmarca.openvitals.domain.model.ReportValueKind

class PdfChartGeometryTest {

    private val day = LocalDate.of(2026, 6, 1)

    private fun point(
        offset: Long,
        value: Double,
        min: Double = value,
        max: Double = value,
        secondary: Double? = null,
    ) = ReportPoint(
        bucketStart = day.plusDays(offset),
        bucketEnd = day.plusDays(offset),
        value = value,
        min = min,
        max = max,
        daysWithData = 1,
        secondaryValue = secondary,
    )

    private fun chart(
        points: List<ReportPoint>,
        kind: ReportValueKind,
        width: Float = 515f,
        height: Float = 150f,
    ) = buildPdfChart(
        points = points,
        valueKind = kind,
        width = width,
        height = height,
        formatAxisValue = { String.format(java.util.Locale.ROOT, "%.0f", it) },
        bucketLabel = { it.bucketStart.toString() },
    )

    @Test fun `SUM metrics draw zero-based bars, one per bucket`() {
        val chart = chart(listOf(point(0, 100.0), point(1, 300.0)), ReportValueKind.SUM)

        assertEquals(2, chart.bars.size)
        assertTrue(chart.linePoints.isEmpty())
        // The taller bar's top sits higher (smaller y), both share the baseline.
        assertTrue(chart.bars[1].top < chart.bars[0].top)
        assertEquals(chart.bars[0].bottom, chart.bars[1].bottom, 1e-3f)
        // A zero-floored range keeps the baseline at the plot bottom.
        assertEquals(chart.plotBottom, chart.bars[0].bottom, 1.0f)
    }

    @Test fun `AVERAGE metrics draw a line and a band only when the extremes differ`() {
        val withBand = chart(
            listOf(point(0, 70.0, min = 50.0, max = 150.0), point(1, 72.0, min = 51.0, max = 140.0)),
            ReportValueKind.AVERAGE,
        )
        val flat = chart(listOf(point(0, 70.0), point(1, 72.0)), ReportValueKind.AVERAGE)

        assertTrue(withBand.bars.isEmpty())
        assertEquals(2, withBand.linePoints.size)
        assertEquals(2, withBand.bandMaxPoints.size)
        assertEquals(2, withBand.bandMinPoints.size)
        assertTrue(flat.bandMaxPoints.isEmpty())
        assertTrue(flat.bandMinPoints.isEmpty())
    }

    @Test fun `the band's max edge sits above its min edge in chart coordinates`() {
        val chart = chart(
            listOf(point(0, 70.0, min = 50.0, max = 150.0), point(1, 72.0, min = 51.0, max = 140.0)),
            ReportValueKind.AVERAGE,
        )

        chart.bandMaxPoints.zip(chart.bandMinPoints).forEach { (max, min) ->
            assertTrue(max.y < min.y)
        }
    }

    @Test fun `blood pressure's diastolic becomes the secondary line`() {
        val chart = chart(
            listOf(point(0, 120.0, secondary = 80.0), point(1, 130.0, secondary = 84.0)),
            ReportValueKind.AVERAGE,
        )

        assertEquals(2, chart.secondaryLinePoints.size)
        // Diastolic is lower, so its line sits below (larger y).
        chart.linePoints.zip(chart.secondaryLinePoints).forEach { (systolic, diastolic) ->
            assertTrue(diastolic.y > systolic.y)
        }
    }

    @Test fun `a dense series decimates instead of emitting hundreds of points`() {
        val chart = chart(
            (0 until 730L).map { point(it, 70.0 + (it % 20)) },
            ReportValueKind.AVERAGE,
        )

        assertTrue(chart.linePoints.size <= 240)
        assertTrue(chart.linePoints.size > 2)
    }

    @Test fun `every drawn coordinate stays inside the plot box`() {
        val chart = chart(
            (0 until 90L).map { point(it, (it % 30).toDouble(), min = 0.0, max = 40.0) },
            ReportValueKind.AVERAGE,
        )

        (chart.linePoints + chart.bandMaxPoints + chart.bandMinPoints).forEach { offset ->
            assertTrue(offset.x >= chart.plotLeft - 1e-3f && offset.x <= chart.plotRight + 1e-3f)
            assertTrue(offset.y >= chart.plotTop - 1e-3f && offset.y <= chart.plotBottom + 1e-3f)
        }
    }

    @Test fun `sparse line charts mark every point, dense ones drop the markers`() {
        val sparse = chart(
            (0 until 30L).map { point(it, 70.0 + it, secondary = 60.0 + it) },
            ReportValueKind.AVERAGE,
        )
        val dense = chart(
            (0 until 365L).map { point(it, 70.0) },
            ReportValueKind.AVERAGE,
        )
        val bars = chart(listOf(point(0, 100.0), point(1, 200.0)), ReportValueKind.SUM)

        assertEquals(30, sparse.lineMarkers.size)
        assertEquals(30, sparse.secondaryLineMarkers.size)
        // Markers sit exactly on the (undecimated) line coordinates.
        assertEquals(sparse.linePoints, sparse.lineMarkers)
        assertTrue(dense.lineMarkers.isEmpty())
        assertTrue(dense.secondaryLineMarkers.isEmpty())
        assertTrue(bars.lineMarkers.isEmpty())
    }

    @Test fun `three y labels ride the grid lines`() {
        val chart = chart(listOf(point(0, 10.0), point(1, 20.0)), ReportValueKind.AVERAGE)

        assertEquals(3, chart.yAxisLabels.size)
        assertEquals(3, chart.gridLineYs.size)
        assertEquals(chart.yAxisLabels.map { it.position }, chart.gridLineYs)
    }

    @Test fun `x labels never overlap`() {
        val chart = chart(
            (0 until 365L).map { point(it, 100.0) },
            ReportValueKind.SUM,
        )

        assertTrue(chart.xAxisLabels.isNotEmpty())
        val approxCharWidth = 6.5f * 0.55f
        chart.xAxisLabels.zipWithNext().forEach { (a, b) ->
            val aEnd = a.position + a.text.length * approxCharWidth / 2f
            val bStart = b.position - b.text.length * approxCharWidth / 2f
            assertTrue(bStart > aEnd)
        }
    }

    @Test fun `an empty series still yields a drawable frame`() {
        val chart = chart(emptyList(), ReportValueKind.SUM)

        assertTrue(chart.bars.isEmpty())
        assertTrue(chart.xAxisLabels.isEmpty())
        assertEquals(3, chart.yAxisLabels.size)
    }
}
