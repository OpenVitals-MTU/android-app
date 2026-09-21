package tech.mmarca.openvitals.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.ui.components.heatmapCellDescription

/** A heatmap cell shows a bare day number. Spoken, that is neither a date nor a value. */
class HeatmapCellDescriptionTest {

    @Test
    fun `a day with a value is spoken as its date and the value`() {
        assertEquals("14 Sept 2026, 12", heatmapCellDescription("14 Sept 2026", 12.0, "No data"))
    }

    @Test
    fun `a day with nothing recorded says so, and so does a day outside the loaded period`() {
        assertEquals("14 Sept 2026, No data", heatmapCellDescription("14 Sept 2026", 0.0, "No data"))
        assertEquals("14 Sept 2026, No data", heatmapCellDescription("14 Sept 2026", null, "No data"))
    }
}
