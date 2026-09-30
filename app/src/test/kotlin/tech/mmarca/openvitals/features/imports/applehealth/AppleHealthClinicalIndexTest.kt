package tech.mmarca.openvitals.features.imports.applehealth

import com.google.common.truth.Truth.assertThat
import java.io.BufferedInputStream
import org.junit.Test

/** export.xml indexes clinical records. The Health Connect import ignores them; the medical import reads them. */
class AppleHealthClinicalIndexTest {

    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <HealthData locale="en_US">
         <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone" unit="count" startDate="2024-01-01 08:00:00 +0000" endDate="2024-01-01 08:10:00 +0000" value="120"/>
         <ClinicalRecord type="HKClinicalTypeIdentifierAllergyRecord" identifier="x1" sourceName="Hospital &amp; Clinic" sourceURL="https://fhir.example/r4" fhirVersion="4.0.1" receivedDate="2024-02-01 10:00:00 +0000" resourceFilePath="/clinical-records/AllergyIntolerance-x1.json"/>
        </HealthData>
    """.trimIndent()

    @Test
    fun `a ClinicalRecord element reaches the consumer with every attribute, unescaped`() {
        val entries = mutableListOf<AppleClinicalRecordIndexEntry>()
        val consumer = object : AppleHealthXmlEventConsumer {
            override fun onParsedType(type: String) = Unit
            override fun onRecord(record: AppleRecord) = Unit
            override fun onWorkout(workout: AppleWorkout) = Unit
            override fun onCorrelation(correlation: AppleCorrelation) = Unit
            override fun onActivitySummary() = Unit
            override fun onClinicalRecord(entry: AppleClinicalRecordIndexEntry) {
                entries += entry
            }
        }

        val parsed = AppleHealthImportParser.parse(BufferedInputStream(xml.byteInputStream()), consumer)

        assertThat(entries).containsExactly(
            AppleClinicalRecordIndexEntry(
                type = "HKClinicalTypeIdentifierAllergyRecord",
                sourceName = "Hospital & Clinic",
                sourceUrl = "https://fhir.example/r4",
                fhirVersion = "4.0.1",
                resourceFilePath = "/clinical-records/AllergyIntolerance-x1.json",
            ),
        )
        // Not a Health Connect record, so none of the record counts change.
        assertThat(parsed.parsedRecords).isEqualTo(1)
    }
}
