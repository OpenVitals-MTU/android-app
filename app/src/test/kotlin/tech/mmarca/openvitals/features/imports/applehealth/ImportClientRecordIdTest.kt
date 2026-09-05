package tech.mmarca.openvitals.features.imports.applehealth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The deterministic `clientRecordId` scheme the Apple Health importer writes.
 * Kotlin keeps a builder per importer; the csv namespace is covered in CsvRowConverterTest.
 */
class ImportClientRecordIdTest {

    // buildImportClientRecordId.

    @Test
    fun `an empty prefix still yields a three-part id`() {
        assertTrue(buildStableClientRecordId("", "parts").startsWith("apple_health_record_"))
    }

    // toStableIdSegment.

    @Test
    fun `a mixed-case type name slugs to lowercase`() {
        assertEquals("weightrecord", "WeightRecord".toStableIdSegment())
    }

    @Test
    fun `runs of punctuation collapse to a single underscore`() {
        assertEquals("body_fat_record", "Body  Fat--Record".toStableIdSegment())
    }

    @Test
    fun `leading and trailing separators are dropped`() {
        assertEquals("weight", "--weight--".toStableIdSegment())
    }

    @Test
    fun `a segment with nothing usable becomes record`() {
        assertEquals("record", "---".toStableIdSegment())
    }

    // buildStableClientRecordId.

    /** Golden ids. If these change, every record a previous release wrote becomes unreachable. */
    @Test
    fun `the apple_health namespace still produces the ids it always has`() {
        val goldens = mapOf(
            "weight|2026-07-01T08:12:00Z|78.4" to
                "apple_health_weight_1e4b72bbd84fa5d0f6e3153cd1dd3016",
            "HKQuantityTypeIdentifierBodyMass|2019-01-02T03:04:05Z" to
                "apple_health_hkquantitytypeidentifierbodymass_9bdc0b35d955abb84ac6ec24c9389560",
        )

        goldens.forEach { (parts, expected) ->
            val prefix = parts.substringBefore('|')
            assertEquals(
                "clientRecordId changed for $parts — every record a previous release " +
                    "wrote would become unreachable",
                expected,
                buildStableClientRecordId(prefix, parts),
            )
        }
    }
}
