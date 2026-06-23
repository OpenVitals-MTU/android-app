package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.units.Pressure

internal fun AppleHealthImportConverter.convertBloodPressureCorrelations(
    correlations: List<AppleCorrelation>,
): List<ConvertedAppleRecord> =
    correlations
        .filter { it.type == AppleBloodPressureCorrelation }
        .mapNotNull { correlation ->
            val systolic = correlation.records.firstOrNull { it.type == AppleBloodPressureSystolic }
            val diastolic = correlation.records.firstOrNull { it.type == AppleBloodPressureDiastolic }
            if (systolic == null || diastolic == null) {
                invalid(
                    appleType = correlation.type,
                    detail = "Blood pressure correlation is missing systolic or diastolic child record.",
                    timeRange = correlation.timeRangeOrNull()?.toString(),
                )
                return@mapNotNull null
            }
            consumedRecordFingerprints += systolic.sourceFingerprint
            consumedRecordFingerprints += diastolic.sourceFingerprint
            buildBloodPressureRecord(
                appleType = correlation.type,
                start = correlation.startDate ?: systolic.startDate ?: diastolic.startDate,
                sourceEnd = correlation.endDate ?: systolic.endDate ?: diastolic.endDate,
                sourceName = correlation.sourceName ?: systolic.sourceName ?: diastolic.sourceName,
                unit = systolic.unit ?: diastolic.unit,
                value = "${systolic.rawValue}/${diastolic.rawValue}",
                systolic = systolic.numericValue,
                diastolic = diastolic.numericValue,
                stableParts = listOf("bp_correlation", correlation.stableParts(), systolic.stableParts(), diastolic.stableParts()),
            )
        }

internal fun AppleHealthImportConverter.convertStandaloneBloodPressure(records: List<AppleRecord>): List<ConvertedAppleRecord> {
    val grouped = records
        .filter { it.type == AppleBloodPressureSystolic || it.type == AppleBloodPressureDiastolic }
        .groupBy { listOf(it.sourceName.orEmpty(), it.creationDate?.instant?.toString().orEmpty(), it.startDate?.instant?.toString().orEmpty(), it.endDate?.instant?.toString().orEmpty()) }

    return grouped.mapNotNull { (_, group) ->
        val systolic = group.firstOrNull { it.type == AppleBloodPressureSystolic }
        val diastolic = group.firstOrNull { it.type == AppleBloodPressureDiastolic }
        if (systolic == null || diastolic == null) {
            group.forEach {
                if (it.sourceFingerprint !in consumedRecordFingerprints) {
                    invalid(it, "Standalone blood pressure value could not be paired with systolic and diastolic values.")
                }
            }
            return@mapNotNull null
        }
        if (systolic.sourceFingerprint in consumedRecordFingerprints || diastolic.sourceFingerprint in consumedRecordFingerprints) {
            return@mapNotNull null
        }
        consumedRecordFingerprints += systolic.sourceFingerprint
        consumedRecordFingerprints += diastolic.sourceFingerprint
        buildBloodPressureRecord(
            appleType = AppleBloodPressureCorrelation,
            start = systolic.startDate ?: diastolic.startDate,
            sourceEnd = systolic.endDate ?: diastolic.endDate,
            sourceName = systolic.sourceName ?: diastolic.sourceName,
            unit = systolic.unit ?: diastolic.unit,
            value = "${systolic.rawValue}/${diastolic.rawValue}",
            systolic = systolic.numericValue,
            diastolic = diastolic.numericValue,
            stableParts = listOf("bp_pair", systolic.stableParts(), diastolic.stableParts()),
        )
    }
}

private fun AppleHealthImportConverter.buildBloodPressureRecord(
    appleType: String,
    start: AppleDateTime?,
    sourceEnd: AppleDateTime?,
    sourceName: String?,
    unit: String?,
    value: String?,
    systolic: Double?,
    diastolic: Double?,
    stableParts: List<String>,
): ConvertedAppleRecord? {
    val time = start ?: return invalid(
        appleType = appleType,
        detail = "Blood pressure is missing measurement time.",
        timeRange = null,
    )
    val sys = systolic?.takeIf { it in 20.0..300.0 } ?: return invalid(
        appleType = appleType,
        detail = "Systolic value is missing or outside supported range.",
        timeRange = time.instant.toString(),
    )
    val dia = diastolic?.takeIf { it in 10.0..180.0 } ?: return invalid(
        appleType = appleType,
        detail = "Diastolic value is missing or outside supported range.",
        timeRange = time.instant.toString(),
    )
    val fingerprint = buildStableClientRecordId("blood_pressure", stableParts + sourceName.orEmpty())
    markConverted(appleType)
    return ConvertedAppleRecord(
        appleType = appleType,
        targetType = "BloodPressureRecord",
        fingerprint = fingerprint,
        recordType = BloodPressureRecord::class,
        record = BloodPressureRecord(
            time = time.instant,
            zoneOffset = time.offset,
            metadata = appleMetadata("BloodPressureRecord", fingerprint),
            systolic = Pressure.millimetersOfMercury(sys),
            diastolic = Pressure.millimetersOfMercury(dia),
        ),
        sourceTimeRange = AppleImportTimeRange(time.instant, sourceEnd?.instant ?: time.instant),
        unit = unit,
        value = value,
    )
}
