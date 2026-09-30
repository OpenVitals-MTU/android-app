package tech.mmarca.openvitals.features.imports.medical

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Synthetic Apple Health export zips with clinical records, laid out as Apple lays them out. */
internal object AppleExportZips {

    /** A `ClinicalRecord` element as export.xml writes it. */
    fun clinicalRecord(file: String, source: String, url: String?, version: String) =
        """<ClinicalRecord type="HKClinicalTypeIdentifierImmunizationRecord" identifier="$file" sourceName="$source"""" +
            (url?.let { """ sourceURL="$it"""" } ?: "") +
            """ fhirVersion="$version" receivedDate="2024-02-01 10:00:00 +0000" resourceFilePath="/clinical-records/$file.json"/>"""

    /** The declaration must be the very first bytes, so this is built line by line, not trimmed. */
    fun exportXml(vararg clinicalRecords: String): String = buildList {
        add("""<?xml version="1.0" encoding="UTF-8"?>""")
        add("""<HealthData locale="en_US">""")
        add(
            """ <Record type="HKQuantityTypeIdentifierStepCount" sourceName="Phone" unit="count" """ +
                """startDate="2024-01-01 08:00:00 +0000" endDate="2024-01-01 08:10:00 +0000" value="120"/>""",
        )
        clinicalRecords.forEach { add(" $it") }
        add("</HealthData>")
    }.joinToString("\n")

    fun immunization(id: String, vaccine: String = "Influenza") =
        """{"resourceType":"Immunization","id":"$id","status":"completed","vaccineCode":{"text":"$vaccine"},""" +
            """"patient":{"reference":"Patient/p1"},"occurrenceDateTime":"2023-10-02"}"""

    /** [before] entries come ahead of export.xml, which Apple's own zips do not do but re-zipped exports can. */
    fun zip(exportXml: String?, clinical: Map<String, String>, before: Set<String> = emptySet()): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun put(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
            clinical.filterKeys { it in before }.forEach { (name, text) -> put("apple_health_export/clinical-records/$name.json", text) }
            exportXml?.let { put("apple_health_export/export.xml", it) }
            put("apple_health_export/export_cda.xml", "<ClinicalDocument/>")
            clinical.filterKeys { it !in before }.forEach { (name, text) -> put("apple_health_export/clinical-records/$name.json", text) }
        }
        return output.toByteArray()
    }
}
