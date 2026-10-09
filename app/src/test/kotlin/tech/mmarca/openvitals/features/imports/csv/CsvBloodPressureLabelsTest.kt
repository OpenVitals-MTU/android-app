package tech.mmarca.openvitals.features.imports.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BpRecordValues

class CsvBloodPressureLabelsTest {

    private val labels = CsvBloodPressureLabels(
        bodyPositions = csvLabelLookup(
            listOf(
                mapOf(BpRecordValues.BODY_POSITION_SITTING_DOWN to "Istuen"),
                mapOf(
                    BpRecordValues.BODY_POSITION_SITTING_DOWN to "Sitting",
                    BpRecordValues.BODY_POSITION_LYING_DOWN to "Lying down",
                ),
            ),
        ),
        cuffLocations = csvLabelLookup(
            listOf(mapOf(BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST to "Left wrist")),
        ),
    )

    @Test
    fun `normalising drops case, whitespace and punctuation`() {
        assertEquals("lyingdown", normalizeCsvLabel("  Lying-DOWN \t"))
        assertEquals("", normalizeCsvLabel(" - "))
    }

    @Test
    fun `body positions match the app language and the fallback`() {
        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, labels.bodyPosition("ISTUEN"))
        assertEquals(BpRecordValues.BODY_POSITION_SITTING_DOWN, labels.bodyPosition("sitting"))
        assertEquals(BpRecordValues.BODY_POSITION_LYING_DOWN, labels.bodyPosition("Lying  down"))
    }

    @Test
    fun `cuff locations match ignoring case and whitespace`() {
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST, labels.cuffLocation(" left   WRIST "))
    }

    @Test
    fun `text that is no label, or a label of the other kind, does not match`() {
        assertNull(labels.bodyPosition("seated"))
        assertNull(labels.bodyPosition("2"))
        assertNull(labels.bodyPosition(""))
        assertNull(labels.cuffLocation("Sitting"))
    }

    @Test
    fun `a label two sets share keeps the earlier constant`() {
        val lookup = csvLabelLookup(
            listOf(
                mapOf(BpRecordValues.BODY_POSITION_STANDING_UP to "Same"),
                mapOf(BpRecordValues.BODY_POSITION_LYING_DOWN to "same"),
            ),
        )

        assertEquals(BpRecordValues.BODY_POSITION_STANDING_UP, lookup["same"])
    }
}
