package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Withings scale export this feature was built for:
 * `Date,"Weight (kg)","Fat mass (kg)","Bone mass (kg)","Muscle mass (kg)","Hydration (kg)",Comments`
 */
private fun withingsMapping(
    bodyFatInterpretation: CsvValueInterpretation? = null,
    includeWeight: Boolean = true,
): CsvImportMapping = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(
            columnIndex = 1,
            role = if (includeWeight) CsvColumnRole.METRIC else CsvColumnRole.IGNORE,
            metric = if (includeWeight) CsvImportMetric.WEIGHT else null,
            interpretation = if (includeWeight) CsvDirectValue(CsvUnit.KILOGRAMS) else null,
        ),
        CsvColumnMapping(
            columnIndex = 2,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.BODY_FAT,
            interpretation = bodyFatInterpretation ?: CsvMassShareOfWeight(CsvUnit.KILOGRAMS),
        ),
        CsvColumnMapping(
            columnIndex = 3,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.BONE_MASS,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
        CsvColumnMapping(
            columnIndex = 4,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.LEAN_BODY_MASS,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
        CsvColumnMapping(
            columnIndex = 5,
            role = CsvColumnRole.METRIC,
            metric = CsvImportMetric.BODY_WATER_MASS,
            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
        ),
        CsvColumnMapping(columnIndex = 6),
    ),
    dateTime = CsvDateTimeSettings(
        format = CsvDateTimeFormat.YEAR_FIRST,
        zone = CsvTimeZoneMode.UTC,
    ),
)

private fun row(fields: List<String>, rowNumber: Int = 2): CsvRow =
    CsvRow(rowNumber = rowNumber, fields = fields)

private fun weightMapping(interpretation: CsvValueInterpretation): CsvImportMapping =
    CsvImportMapping(
        columns = listOf(
            CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
            CsvColumnMapping(
                columnIndex = 1,
                role = CsvColumnRole.METRIC,
                metric = CsvImportMetric.WEIGHT,
                interpretation = interpretation,
            ),
        ),
        dateTime = CsvDateTimeSettings(
            format = CsvDateTimeFormat.YEAR_FIRST,
            zone = CsvTimeZoneMode.UTC,
        ),
    )

/** A mapping of one timestamp column plus one metric column. */
private fun singleMetricMapping(
    metric: CsvImportMetric,
    interpretation: CsvValueInterpretation,
): CsvImportMapping = CsvImportMapping(
    columns = listOf(
        CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
        CsvColumnMapping(
            columnIndex = 1,
            role = CsvColumnRole.METRIC,
            metric = metric,
            interpretation = interpretation,
        ),
    ),
    dateTime = CsvDateTimeSettings(
        format = CsvDateTimeFormat.YEAR_FIRST,
        zone = CsvTimeZoneMode.UTC,
    ),
)

private fun convertOne(
    value: String,
    metric: CsvImportMetric,
    interpretation: CsvValueInterpretation,
): CsvConvertedRecord = convertCsvRow(
    row = CsvRow(rowNumber = 2, fields = listOf("2023-10-09 07:08:01", value)),
    mapping = singleMetricMapping(metric, interpretation),
).records.single()

class CsvRowConverterTest {

    // ── convertCsvRow ────────────────────────────────────────────────────────

    @Test
    fun `a Withings row produces one record for each mapped metric`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        assertEquals(
            listOf(
                "WeightRecord",
                "BodyFatRecord",
                "BoneMassRecord",
                "LeanBodyMassRecord",
                "BodyWaterMassRecord",
            ),
            conversion.records.map { it.targetType },
        )
    }

    @Test
    fun `fat mass in kilograms becomes a body-fat percentage of the row weight`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        val bodyFat = conversion.records.single { it.targetType == "BodyFatRecord" }

        // 15.2 / 78.4 * 100
        assertEquals(19.3877, (bodyFat.record as BodyFatRecord).percentage.value, 0.001)
    }

    @Test
    fun `a fat-mass row with no weight value keeps its other metrics and reports the missing derivation`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertEquals(
            listOf("BoneMassRecord", "LeanBodyMassRecord", "BodyWaterMassRecord"),
            conversion.records.map { it.targetType },
        )
        assertEquals(
            CsvImportDiagnosticReason.DERIVATION_MISSING_WEIGHT,
            conversion.diagnostics.single().reason,
        )
    }

    @Test
    fun `body fat given directly as a percentage needs no weight column`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "", "19.4", "", "", "", "")),
            mapping = withingsMapping(
                bodyFatInterpretation = CsvDirectValue(CsvUnit.PERCENT),
                includeWeight = false,
            ),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        val bodyFat = conversion.records.single { it.targetType == "BodyFatRecord" }
        assertEquals(19.4, (bodyFat.record as BodyFatRecord).percentage.value, 1e-9)
    }

    @Test
    fun `a weight in pounds converts to kilograms`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "172.8")),
            mapping = weightMapping(CsvDirectValue(CsvUnit.POUNDS)),
        )

        val weight = conversion.records.single().record as WeightRecord
        assertEquals(78.38, weight.weight.inKilograms, 0.01)
    }

    @Test
    fun `a height in centimetres is stored in metres`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "183")),
            mapping = singleMetricMapping(CsvImportMetric.HEIGHT, CsvDirectValue(CsvUnit.CENTIMETERS)),
        )

        val height = conversion.records.single().record as HeightRecord
        assertEquals(1.83, height.height.inMeters, 0.0001)
    }

    @Test
    fun `an unparsable timestamp rejects the whole row`() {
        val conversion = convertCsvRow(
            row = row(listOf("not a date", "78.4", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertTrue(conversion.records.isEmpty())
        assertEquals(
            CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP,
            conversion.diagnostics.single().reason,
        )
    }

    @Test
    fun `an empty timestamp cell rejects the whole row`() {
        val conversion = convertCsvRow(
            row = row(listOf("", "78.4", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertTrue(conversion.records.isEmpty())
        assertEquals(
            CsvImportDiagnosticReason.MISSING_TIMESTAMP,
            conversion.diagnostics.single().reason,
        )
    }

    @Test
    fun `a row shorter than the mapped columns is rejected as malformed`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4")),
            mapping = withingsMapping(),
        )

        assertTrue(conversion.records.isEmpty())
        assertEquals(
            CsvImportDiagnosticReason.WRONG_FIELD_COUNT,
            conversion.diagnostics.single().reason,
        )
    }

    @Test
    fun `an implausible weight is rejected while the rest of the row still lands`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "900", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(bodyFatInterpretation = CsvDirectValue(CsvUnit.PERCENT)),
        )

        assertEquals(
            CsvImportDiagnosticReason.OUT_OF_RANGE,
            conversion.diagnostics.single().reason,
        )
        assertTrue(conversion.records.none { it.targetType == "WeightRecord" })
        assertEquals(1, conversion.records.count { it.targetType == "BoneMassRecord" })
    }

    @Test
    fun `a derived body fat outside the plausible range is rejected rather than stored`() {
        // Fat mass divided by a weight column that is not body weight — the
        // failure this guard exists for.
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "16.0", "15.2", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertTrue(
            conversion.diagnostics.map { it.reason }.contains(CsvImportDiagnosticReason.OUT_OF_RANGE),
        )
        assertTrue(conversion.records.none { it.targetType == "BodyFatRecord" })
    }

    @Test
    fun `an unparsable metric cell costs only that metric`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4", "n/a", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(bodyFatInterpretation = CsvDirectValue(CsvUnit.PERCENT)),
        )

        assertEquals(
            CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
            conversion.diagnostics.single().reason,
        )
        assertEquals(4, conversion.records.size)
    }

    @Test
    fun `a blank metric cell is a gap, not an error`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4", "", "3.1", "55.0", "42.3", "")),
            mapping = withingsMapping(),
        )

        assertTrue(conversion.diagnostics.isEmpty())
        assertTrue(conversion.records.none { it.targetType == "BodyFatRecord" })
        assertEquals(4, conversion.records.size)
    }

    @Test
    fun `the record carries the resolved instant and its wall-clock offset`() {
        val conversion = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4")),
            mapping = CsvImportMapping(
                columns = listOf(
                    CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                    CsvColumnMapping(
                        columnIndex = 1,
                        role = CsvColumnRole.METRIC,
                        metric = CsvImportMetric.WEIGHT,
                        interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
                    ),
                ),
                dateTime = CsvDateTimeSettings(
                    format = CsvDateTimeFormat.YEAR_FIRST,
                    zone = CsvTimeZoneMode.FIXED_OFFSET,
                    fixedOffset = ZoneOffset.ofHours(2),
                ),
            ),
        )

        val record = conversion.records.single().record as WeightRecord
        assertEquals(Instant.parse("2026-07-01T06:12:00Z"), record.time)
        assertEquals(ZoneOffset.ofHours(2), record.zoneOffset)
    }

    // ── buildCsvClientRecordId ───────────────────────────────────────────────

    @Test
    fun `the id is namespaced to csv so it cannot collide with apple_health`() {
        val id = buildCsvClientRecordId(
            targetType = "WeightRecord",
            utc = Instant.parse("2026-07-01T06:12:00Z"),
        )

        assertTrue(id.startsWith("csv_weightrecord_"))
    }

    @Test
    fun `the id is byte-identical to the Flutter build's`() {
        // Pinned against the Dart implementation: sha256("WeightRecord|1782886320000"),
        // first 16 bytes as hex. Users who imported via the Flutter build dedup
        // against exactly this string.
        assertEquals(
            "csv_weightrecord_dc1bc96fac534f11b1fc16459d2da1fa",
            buildCsvClientRecordId(
                targetType = "WeightRecord",
                utc = Instant.parse("2026-07-01T06:12:00Z"),
            ),
        )
    }

    @Test
    fun `the same measurement in pounds and kilograms yields the same id`() {
        val metric = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4")),
            mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
        ).records.single()
        val imperial = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "172.84")),
            mapping = weightMapping(CsvDirectValue(CsvUnit.POUNDS)),
        ).records.single()

        assertEquals(metric.clientRecordId, imperial.clientRecordId)
    }

    @Test
    fun `a corrected value at the same instant keeps the id, so the re-import replaces the record instead of duplicating it`() {
        // This IS the upsert contract: Health Connect replaces on a matching
        // clientRecordId, so excluding the value from the id is what makes a
        // corrected file overwrite rather than double up.
        val before = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.4")),
            mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
        ).records.single()
        val after = convertCsvRow(
            row = row(listOf("2026-07-01 08:12:00", "78.6")),
            mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
        ).records.single()

        assertEquals(before.clientRecordId, after.clientRecordId)
        assertNotEquals(
            (before.record as WeightRecord).weight.inKilograms,
            (after.record as WeightRecord).weight.inKilograms,
            1e-9,
        )
    }

    @Test
    fun `a different instant yields a different id`() {
        val first = buildCsvClientRecordId(
            targetType = "WeightRecord",
            utc = Instant.parse("2026-07-01T06:12:00Z"),
        )
        val second = buildCsvClientRecordId(
            targetType = "WeightRecord",
            utc = Instant.parse("2026-07-02T06:12:00Z"),
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `two metrics at the same instant get different ids`() {
        assertNotEquals(
            buildCsvClientRecordId(targetType = "WeightRecord", utc = Instant.parse("2026-07-01T00:00:00Z")),
            buildCsvClientRecordId(targetType = "BodyFatRecord", utc = Instant.parse("2026-07-01T00:00:00Z")),
        )
    }

    // ── vitals metrics ───────────────────────────────────────────────────────

    @Test
    fun `a Withings temperature export row becomes a BodyTemperatureRecord`() {
        val converted = convertOne("36.6", CsvImportMetric.BODY_TEMPERATURE, CsvDirectValue(CsvUnit.CELSIUS))

        val record = converted.record as BodyTemperatureRecord
        assertEquals(36.6, record.temperature.inCelsius, 1e-9)
        assertEquals(Instant.parse("2023-10-09T07:08:01Z"), record.time)
    }

    @Test
    fun `a temperature in Fahrenheit converts to Celsius`() {
        val converted = convertOne("98.6", CsvImportMetric.BODY_TEMPERATURE, CsvDirectValue(CsvUnit.FAHRENHEIT))

        assertEquals(37.0, (converted.record as BodyTemperatureRecord).temperature.inCelsius, 0.001)
    }

    @Test
    fun `a heart rate becomes a one-sample series at that instant`() {
        // Health Connect models heart rate as a series even for a spot reading.
        val converted = convertOne("62", CsvImportMetric.HEART_RATE, CsvDirectValue(CsvUnit.BEATS_PER_MINUTE))

        val hr = converted.record as HeartRateRecord
        assertEquals(hr.startTime, hr.endTime)
        assertEquals(1, hr.samples.size)
        assertEquals(62L, hr.samples.single().beatsPerMinute)
        assertEquals(hr.startTime, hr.samples.single().time)
    }

    @Test
    fun `a fractional heart rate rounds, because the record stores an integer`() {
        val converted = convertOne("61.6", CsvImportMetric.RESTING_HEART_RATE, CsvDirectValue(CsvUnit.BEATS_PER_MINUTE))

        assertEquals(62L, (converted.record as RestingHeartRateRecord).beatsPerMinute)
    }

    @Test
    fun `HRV in seconds converts to milliseconds`() {
        val converted = convertOne("0.045", CsvImportMetric.HEART_RATE_VARIABILITY, CsvDirectValue(CsvUnit.SECONDS))

        assertEquals(
            45.0,
            (converted.record as HeartRateVariabilityRmssdRecord).heartRateVariabilityMillis,
            0.001,
        )
    }

    @Test
    fun `SpO2 given as a fraction becomes a percentage`() {
        val converted = convertOne("0.97", CsvImportMetric.OXYGEN_SATURATION, CsvDirectValue(CsvUnit.FRACTION))

        assertEquals(97.0, (converted.record as OxygenSaturationRecord).percentage.value, 0.001)
    }

    @Test
    fun `blood glucose in mg per dL converts to mmol per L`() {
        val converted = convertOne("90", CsvImportMetric.BLOOD_GLUCOSE, CsvDirectValue(CsvUnit.MILLIGRAMS_PER_DECILITER))

        assertEquals(5.0, (converted.record as BloodGlucoseRecord).level.inMillimolesPerLiter, 0.001)
    }

    @Test
    fun `respiratory rate and VO2 max map to their records`() {
        assertTrue(
            convertOne("14", CsvImportMetric.RESPIRATORY_RATE, CsvDirectValue(CsvUnit.BREATHS_PER_MINUTE))
                .record is RespiratoryRateRecord,
        )
        assertTrue(
            convertOne("48", CsvImportMetric.VO2_MAX, CsvDirectValue(CsvUnit.MILLILITERS_PER_KG_PER_MINUTE))
                .record is Vo2MaxRecord,
        )
    }

    @Test
    fun `basal body temperature is distinct from body temperature`() {
        val converted = convertOne("36.4", CsvImportMetric.BASAL_BODY_TEMPERATURE, CsvDirectValue(CsvUnit.CELSIUS))

        assertTrue(converted.record is BasalBodyTemperatureRecord)
        assertEquals("BasalBodyTemperatureRecord", converted.targetType)
    }

    @Test
    fun `a temperature of 300 is rejected as implausible`() {
        // A Fahrenheit column mapped as Celsius, most likely.
        val conversion = convertCsvRow(
            row = CsvRow(rowNumber = 2, fields = listOf("2023-10-09 07:08:01", "300")),
            mapping = singleMetricMapping(CsvImportMetric.BODY_TEMPERATURE, CsvDirectValue(CsvUnit.CELSIUS)),
        )

        assertTrue(conversion.records.isEmpty())
        assertEquals(
            CsvImportDiagnosticReason.OUT_OF_RANGE,
            conversion.diagnostics.single().reason,
        )
    }

    @Test
    fun `every catalog metric can build a record from its canonical value`() {
        // Guards the switch in buildCsvImportRecord against a metric added to
        // the enum without a case building the WRONG record type.
        for (metric in CsvImportMetric.entries) {
            val spec = CsvMetricCatalog.getValue(metric)
            val converted = buildCsvImportRecord(
                metric = metric,
                value = (spec.plausibleMin + spec.plausibleMax) / 2,
                instant = CsvInstant(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC),
            )
            assertEquals(metric.name, spec.targetType, converted.targetType)
            assertEquals(metric.name, spec.recordType, converted.record::class)
        }
    }

    // ── previewInstantRange ──────────────────────────────────────────────────

    private val previewRows = listOf(
        listOf("2026-07-03 08:11:00", "78.2"),
        listOf("2026-07-01 08:12:00", "78.4"),
        listOf("2026-07-02 08:14:00", "78.6"),
    )

    @Test
    fun `the span covers the earliest and latest row, not the file order`() {
        val range = previewInstantRange(
            rows = previewRows,
            mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
        )

        assertEquals(LocalDateTime.of(2026, 7, 1, 8, 12), range!!.first)
        assertEquals(LocalDateTime.of(2026, 7, 3, 8, 11), range.second)
    }

    @Test
    fun `reading the same file day-first instead of month-first moves the span to a different month`() {
        // The whole point of showing the span: `01/07` is plausible either way
        // on its own, but the RANGE it implies is not.
        val ambiguous = listOf(
            listOf("01/07/2026", "78.4"),
            listOf("02/07/2026", "78.6"),
            listOf("03/07/2026", "78.2"),
        )

        fun mappingFor(format: CsvDateTimeFormat): CsvImportMapping = CsvImportMapping(
            columns = listOf(
                CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                CsvColumnMapping(
                    columnIndex = 1,
                    role = CsvColumnRole.METRIC,
                    metric = CsvImportMetric.WEIGHT,
                    interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
                ),
            ),
            dateTime = CsvDateTimeSettings(format = format, zone = CsvTimeZoneMode.UTC),
        )

        val dayFirst = previewInstantRange(rows = ambiguous, mapping = mappingFor(CsvDateTimeFormat.DAY_FIRST))
        val monthFirst = previewInstantRange(rows = ambiguous, mapping = mappingFor(CsvDateTimeFormat.MONTH_FIRST))

        // Day-first: three days in July. Month-first: three months, Jan–Mar.
        assertEquals(7, dayFirst!!.first.monthValue)
        assertEquals(7, dayFirst.second.monthValue)
        assertEquals(1, monthFirst!!.first.monthValue)
        assertEquals(3, monthFirst.second.monthValue)
    }

    @Test
    fun `rows that do not parse are skipped rather than widening the span`() {
        val range = previewInstantRange(
            rows = listOf(
                listOf("2026-07-01 08:12:00", "78.4"),
                listOf("not a date", "78.5"),
                listOf("", "78.6"),
            ),
            mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
        )

        assertEquals(LocalDateTime.of(2026, 7, 1, 8, 12), range!!.first)
        assertEquals(LocalDateTime.of(2026, 7, 1, 8, 12), range.second)
    }

    @Test
    fun `a sample where nothing parses reports no span`() {
        assertNull(
            previewInstantRange(
                rows = listOf(listOf("not a date", "78.4")),
                mapping = weightMapping(CsvDirectValue(CsvUnit.KILOGRAMS)),
            ),
        )
    }

    @Test
    fun `a mapping with no timestamp column reports no span`() {
        assertNull(
            previewInstantRange(
                rows = previewRows,
                mapping = CsvImportMapping(
                    columns = listOf(
                        CsvColumnMapping(
                            columnIndex = 1,
                            role = CsvColumnRole.METRIC,
                            metric = CsvImportMetric.WEIGHT,
                            interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `the span is the wall clock in the file, not the UTC instant`() {
        // A +02:00 file says 08:12 on the wall; showing 06:12 would look like a
        // bug to the user comparing against their own spreadsheet.
        val range = previewInstantRange(
            rows = listOf(listOf("2026-07-01 08:12:00", "78.4")),
            mapping = CsvImportMapping(
                columns = listOf(
                    CsvColumnMapping(columnIndex = 0, role = CsvColumnRole.TIMESTAMP),
                    CsvColumnMapping(
                        columnIndex = 1,
                        role = CsvColumnRole.METRIC,
                        metric = CsvImportMetric.WEIGHT,
                        interpretation = CsvDirectValue(CsvUnit.KILOGRAMS),
                    ),
                ),
                dateTime = CsvDateTimeSettings(
                    format = CsvDateTimeFormat.YEAR_FIRST,
                    zone = CsvTimeZoneMode.FIXED_OFFSET,
                    fixedOffset = ZoneOffset.ofHours(2),
                ),
            ),
        )

        assertEquals(8, range!!.first.hour)
    }

    // ── parseCsvNumber ───────────────────────────────────────────────────────

    @Test
    fun `a comma decimal separator parses as a decimal, not a thousands mark`() {
        assertEquals(78.4, parseCsvNumber("78,4")!!, 1e-9)
    }

    @Test
    fun `a dot decimal separator parses unchanged`() {
        assertEquals(78.4, parseCsvNumber("78.4")!!, 1e-9)
    }

    @Test
    fun `European grouping with a comma decimal parses correctly`() {
        assertEquals(1234.5, parseCsvNumber("1.234,5")!!, 1e-9)
    }

    @Test
    fun `English grouping with a dot decimal parses correctly`() {
        assertEquals(1234.5, parseCsvNumber("1,234.5")!!, 1e-9)
    }

    @Test
    fun `a trailing unit is stripped rather than failing the cell`() {
        assertEquals(78.4, parseCsvNumber("78.4 kg")!!, 1e-9)
    }

    @Test
    fun `a negative value keeps its sign`() {
        assertEquals(-3.2, parseCsvNumber("-3.2")!!, 1e-9)
    }

    @Test
    fun `a non-numeric cell parses as null`() {
        assertNull(parseCsvNumber("n/a"))
        assertNull(parseCsvNumber(""))
    }
}
