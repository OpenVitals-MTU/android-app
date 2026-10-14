package tech.mmarca.openvitals.features.imports.csv

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.units.Temperature
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.reflect.KClass
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CervixFirmness
import tech.mmarca.openvitals.domain.model.CervixOpening
import tech.mmarca.openvitals.domain.model.CervixPosition
import tech.mmarca.openvitals.domain.model.CycleJournalEntry

/**
 * The drip cycle tracker's CSV export, recognised by its header and read
 * without a column mapping. drip (gitlab.com/bloodyhealth/drip) writes one
 * row per day with dotted column names, `bleeding.value`, `mucus.feeling`
 * and so on, integers for its option lists, `true`/`false` for its switches,
 * and empty cells for anything not recorded. Bleeding, temperature, mucus
 * and sex become the Health Connect records the cycle day log writes; pain,
 * mood, the cervix, desire and notes become the day's journal entry, which
 * Health Connect has no record type for.
 *
 * Pure: a row in, records and a journal entry out, like the row converter.
 */

/** What a drip import can bring over. Each is a switch on the options step. */
enum class DripImportOption {
    BLEEDING,
    TEMPERATURE,
    CERVICAL_MUCUS,
    SEXUAL_ACTIVITY,
    JOURNAL,
}

/** A drip export's columns, by header name, plus what the user chose to import. */
data class DripImportPlan(
    /** Header name, lowercased, to column index. */
    val columns: Map<String, Int>,
    /** The zone a day's records are placed in: the phone's, since drip stores local days. */
    val zone: ZoneId,
    val enabled: Set<DripImportOption> = DripImportOption.entries.toSet(),
) {
    val importsAnything: Boolean get() = enabled.isNotEmpty()

    /** The write permissions the enabled options need. The journal needs none. */
    val requiredWritePermissions: Set<String>
        get() = enabled.flatMapTo(mutableSetOf()) { option ->
            when (option) {
                DripImportOption.BLEEDING -> listOf(
                    HealthPermission.getWritePermission(MenstruationFlowRecord::class),
                    HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class),
                )
                DripImportOption.TEMPERATURE -> listOf(HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class))
                DripImportOption.CERVICAL_MUCUS -> listOf(HealthPermission.getWritePermission(CervicalMucusRecord::class))
                DripImportOption.SEXUAL_ACTIVITY -> listOf(HealthPermission.getWritePermission(SexualActivityRecord::class))
                DripImportOption.JOURNAL -> emptyList()
            }
        }

    fun withOption(option: DripImportOption, on: Boolean): DripImportPlan =
        copy(enabled = if (on) enabled + option else enabled - option)
}

/** The columns a header must carry to be read as a drip export. */
private val DripSignatureColumns = listOf("date", "bleeding.value", "temperature.value", "mucus.feeling")

/**
 * [headerRow] as a drip plan, or null when it is not a drip export. The
 * signature is the date and the three value columns drip has had since its
 * first schema; a later export with extra columns still matches.
 */
fun detectDripExport(headerRow: List<String>, zone: ZoneId = ZoneId.systemDefault()): DripImportPlan? {
    val columns = mutableMapOf<String, Int>()
    headerRow.forEachIndexed { index, name ->
        val key = name.trim().lowercase(Locale.US)
        if (key.isNotEmpty() && key !in columns) columns[key] = index
    }
    if (DripSignatureColumns.any { it !in columns }) return null
    return DripImportPlan(columns = columns, zone = zone)
}

/** Health Connect record class names, segments of the clientRecordId. */
private const val FlowTargetType = "MenstruationFlowRecord"
private const val SpottingTargetType = "IntermenstrualBleedingRecord"
private const val TemperatureTargetType = "BasalBodyTemperatureRecord"
private const val MucusTargetType = "CervicalMucusRecord"
private const val SexTargetType = "SexualActivityRecord"

/** The temperature bounds the basal body temperature metric uses. */
private const val TemperatureMinCelsius = 25.0
private const val TemperatureMaxCelsius = 45.0

/** drip's `temperature.time`, `HH:mm`, with or without the leading zero. */
private val DripTimeRegex = Regex("""^(\d{1,2}):(\d{2})$""")

/**
 * Converts one drip row under [plan]. An unreadable date costs the row; a
 * bad number costs only its record. Columns of a disabled option are not read.
 */
fun convertDripRow(row: CsvRow, plan: DripImportPlan): CsvRowConversion {
    val dateColumn = plan.columns.getValue("date")
    val dateText = row.cell(dateColumn)
        ?: return CsvRowConversion(
            diagnostics = listOf(
                CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = CsvImportDiagnosticReason.MISSING_TIMESTAMP,
                    columnIndex = dateColumn,
                ),
            ),
        )
    val date = parseDripDate(dateText)
        ?: return CsvRowConversion(
            diagnostics = listOf(
                CsvImportDiagnostic(
                    rowNumber = row.rowNumber,
                    reason = CsvImportDiagnosticReason.UNPARSABLE_TIMESTAMP,
                    columnIndex = dateColumn,
                    detail = dateText,
                ),
            ),
        )

    val cells = DripCells(row, plan.columns)
    val diagnostics = mutableListOf<CsvImportDiagnostic>()
    val records = mutableListOf<CsvConvertedRecord>()
    // Noon, the instant the day log gives a past day's records.
    val noon = date.atTime(LocalTime.NOON).atZone(plan.zone).toInstant()
    val offset = plan.zone.rules.getOffset(noon)

    if (DripImportOption.BLEEDING in plan.enabled) {
        dripBleedingRecord(cells, row.rowNumber, noon, offset, diagnostics)?.let { records += it }
    }
    if (DripImportOption.TEMPERATURE in plan.enabled) {
        dripTemperatureRecord(cells, row.rowNumber, date, plan.zone, diagnostics)?.let { records += it }
    }
    val mucusExcluded = cells.flag("mucus.exclude")
    if (DripImportOption.CERVICAL_MUCUS in plan.enabled && !mucusExcluded) {
        dripMucusRecord(cells, noon, offset)?.let { records += it }
    }
    if (DripImportOption.SEXUAL_ACTIVITY in plan.enabled) {
        dripSexRecord(cells, noon, offset)?.let { records += it }
    }
    val journal = if (DripImportOption.JOURNAL in plan.enabled) {
        dripJournalEntry(cells, date, mucusExcluded)
    } else {
        null
    }

    return CsvRowConversion(records = records, diagnostics = diagnostics, journalEntry = journal)
}

/** The sampled rows' first and last day, or null when none parses. For the confirm step. */
fun dripSampleDateRange(rows: List<List<String>>, plan: DripImportPlan): Pair<LocalDate, LocalDate>? {
    val dateColumn = plan.columns.getValue("date")
    val dates = rows.mapNotNull { fields -> fields.getOrNull(dateColumn)?.trim()?.let(::parseDripDate) }
    if (dates.isEmpty()) return null
    return dates.min() to dates.max()
}

/** How many of the sampled [rows] produce a record or a journal entry under [plan]. */
fun dripSampleDaysWithContent(rows: List<List<String>>, plan: DripImportPlan): Int =
    rows.count { fields ->
        val conversion = convertDripRow(CsvRow(rowNumber = 0, fields = fields), plan)
        conversion.records.isNotEmpty() || conversion.journalEntry != null
    }

/**
 * [imported] laid over [existing]: drip's observations are added, what only
 * this phone knows is kept. Null when nothing would change, so an unchanged
 * day keeps its edit time.
 */
fun mergeDripJournalEntry(existing: CycleJournalEntry?, imported: CycleJournalEntry): CycleJournalEntry? {
    if (existing == null) return imported
    val notes = mergeNotes(existing.notes, imported.notes)
    val merged = existing.copy(
        symptoms = existing.symptoms + imported.symptoms,
        moodLevel = imported.moodLevel ?: existing.moodLevel,
        energyLevel = imported.energyLevel ?: existing.energyLevel,
        desireLevel = imported.desireLevel ?: existing.desireLevel,
        cervicalSensation = imported.cervicalSensation ?: existing.cervicalSensation,
        cervixOpening = imported.cervixOpening ?: existing.cervixOpening,
        cervixFirmness = imported.cervixFirmness ?: existing.cervixFirmness,
        cervixPosition = imported.cervixPosition ?: existing.cervixPosition,
        notes = notes,
    )
    return merged.takeIf { it != existing }
}

/** [existing] plus every line of [imported] it does not already contain. */
private fun mergeNotes(existing: String, imported: String): String {
    val lines = existing.lines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    for (line in imported.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
        if (line !in lines) lines += line
    }
    return lines.joinToString("\n")
}

private fun parseDripDate(text: String): LocalDate? =
    try {
        LocalDate.parse(text.trim())
    } catch (_: DateTimeParseException) {
        null
    }

/** Typed access to one row's cells by drip column name. A missing column reads as empty. */
private class DripCells(private val row: CsvRow, private val columns: Map<String, Int>) {
    fun text(name: String): String? = columns[name]?.let { row.cell(it) }

    fun columnIndex(name: String): Int? = columns[name]

    /** drip writes its switches as `true`/`false`; anything else is off. */
    fun flag(name: String): Boolean = text(name)?.equals("true", ignoreCase = true) == true

    fun anyFlag(vararg names: String): Boolean = names.any { flag(it) }

    /** An option-list value: a whole number, or null when empty or not a number. */
    fun int(name: String): Int? = text(name)?.let(::parseCsvNumber)?.takeIf { it == it.toInt().toDouble() }?.toInt()
}

private fun dripBleedingRecord(
    cells: DripCells,
    rowNumber: Int,
    time: Instant,
    offset: java.time.ZoneOffset,
    diagnostics: MutableList<CsvImportDiagnostic>,
): CsvConvertedRecord? {
    val text = cells.text("bleeding.value") ?: return null
    val value = cells.int("bleeding.value")
    if (value == null || value !in 0..3) {
        diagnostics += CsvImportDiagnostic(
            rowNumber = rowNumber,
            reason = CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
            columnIndex = cells.columnIndex("bleeding.value"),
            detail = text,
        )
        return null
    }
    // drip's exclude means "not menstrual bleeding", and its 0 is spotting:
    // both are bleeding between periods. 1..3 are light, medium and heavy,
    // the same numbers Health Connect uses.
    val spotting = cells.flag("bleeding.exclude") || value == 0
    return if (spotting) {
        dripRecord(SpottingTargetType, IntermenstrualBleedingRecord::class, time) { metadata ->
            IntermenstrualBleedingRecord(time = time, zoneOffset = offset, metadata = metadata)
        }
    } else {
        dripRecord(FlowTargetType, MenstruationFlowRecord::class, time) { metadata ->
            MenstruationFlowRecord(time = time, zoneOffset = offset, flow = value, metadata = metadata)
        }
    }
}

/**
 * The morning temperature at its recorded clock time, noon without one. A
 * temperature excluded in drip was judged unreliable there and is left out.
 */
private fun dripTemperatureRecord(
    cells: DripCells,
    rowNumber: Int,
    date: LocalDate,
    zone: ZoneId,
    diagnostics: MutableList<CsvImportDiagnostic>,
): CsvConvertedRecord? {
    val text = cells.text("temperature.value") ?: return null
    if (cells.flag("temperature.exclude")) return null
    val celsius = parseCsvNumber(text)
    val columnIndex = cells.columnIndex("temperature.value")
    if (celsius == null) {
        diagnostics += CsvImportDiagnostic(
            rowNumber = rowNumber,
            reason = CsvImportDiagnosticReason.UNPARSABLE_NUMBER,
            columnIndex = columnIndex,
            detail = text,
        )
        return null
    }
    if (celsius < TemperatureMinCelsius || celsius > TemperatureMaxCelsius) {
        diagnostics += CsvImportDiagnostic(
            rowNumber = rowNumber,
            reason = CsvImportDiagnosticReason.OUT_OF_RANGE,
            columnIndex = columnIndex,
            detail = String.format(Locale.US, "%.2f", celsius),
        )
        return null
    }
    val clock = cells.text("temperature.time")?.let(::parseDripTime) ?: LocalTime.NOON
    val time = date.atTime(clock).atZone(zone).toInstant()
    val offset = zone.rules.getOffset(time)
    return dripRecord(TemperatureTargetType, BasalBodyTemperatureRecord::class, time, celsius) { metadata ->
        BasalBodyTemperatureRecord(
            time = time,
            zoneOffset = offset,
            temperature = Temperature.celsius(celsius),
            metadata = metadata,
        )
    }
}

private fun parseDripTime(text: String): LocalTime? {
    val match = DripTimeRegex.find(text.trim()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute)
}

/**
 * drip's texture (0 nothing, 1 creamy, 2 egg white) names the appearance
 * outright; its feeling (0 dry, 1 nothing, 2 wet, 3 slippery) stands in
 * when the texture says nothing. Health Connect's sensation is an amount
 * drip does not record.
 */
private fun dripMucusRecord(cells: DripCells, time: Instant, offset: java.time.ZoneOffset): CsvConvertedRecord? {
    val texture = cells.int("mucus.texture")
    val feeling = cells.int("mucus.feeling")
    val appearance = when (texture) {
        1 -> CervicalMucusRecord.APPEARANCE_CREAMY
        2 -> CervicalMucusRecord.APPEARANCE_EGG_WHITE
        else -> when (feeling) {
            0 -> CervicalMucusRecord.APPEARANCE_DRY
            2 -> CervicalMucusRecord.APPEARANCE_WATERY
            3 -> CervicalMucusRecord.APPEARANCE_EGG_WHITE
            else -> CervicalMucusRecord.APPEARANCE_UNKNOWN
        }
    }
    if (appearance == CervicalMucusRecord.APPEARANCE_UNKNOWN) return null
    return dripRecord(MucusTargetType, CervicalMucusRecord::class, time) { metadata ->
        CervicalMucusRecord(
            time = time,
            zoneOffset = offset,
            appearance = appearance,
            sensation = CervicalMucusRecord.SENSATION_UNKNOWN,
            metadata = metadata,
        )
    }
}

/** A day with sex. Protected when drip lists a contraceptive, unprotected when it says none. */
private fun dripSexRecord(cells: DripCells, time: Instant, offset: java.time.ZoneOffset): CsvConvertedRecord? {
    if (!cells.anyFlag("sex.partner", "sex.solo")) return null
    val protection = when {
        cells.anyFlag("sex.condom", "sex.pill", "sex.iud", "sex.patch", "sex.ring", "sex.implant", "sex.diaphragm") ->
            SexualActivityRecord.PROTECTION_USED_PROTECTED
        cells.flag("sex.none") -> SexualActivityRecord.PROTECTION_USED_UNPROTECTED
        else -> SexualActivityRecord.PROTECTION_USED_UNKNOWN
    }
    return dripRecord(SexTargetType, SexualActivityRecord::class, time) { metadata ->
        SexualActivityRecord(time = time, zoneOffset = offset, protectionUsed = protection, metadata = metadata)
    }
}

/** drip's pain and mood switches as the journal's symptoms. Migraine is a headache here; ovulation pain, pelvic pain outside the period. */
private val DripSymptomColumns: Map<String, CycleSymptom> = mapOf(
    "pain.cramps" to CycleSymptom.CRAMPS,
    "pain.ovulationPain" to CycleSymptom.PELVIC_PAIN_OUTSIDE_PERIOD,
    "pain.headache" to CycleSymptom.HEADACHE,
    "pain.migraine" to CycleSymptom.HEADACHE,
    "pain.backache" to CycleSymptom.BACKACHE,
    "pain.nausea" to CycleSymptom.NAUSEA,
    "pain.tenderBreasts" to CycleSymptom.BREAST_TENDERNESS,
    "mood.anxious" to CycleSymptom.ANXIETY,
    "mood.fatigue" to CycleSymptom.FATIGUE,
    "mood.sad" to CycleSymptom.MOOD_CHANGES,
    "mood.stressed" to CycleSymptom.MOOD_CHANGES,
    "mood.angry" to CycleSymptom.MOOD_CHANGES,
).mapKeys { it.key.lowercase(Locale.US) }

private val DripPositiveMoods = listOf("mood.happy", "mood.balanced", "mood.fine")
private val DripNegativeMoods = listOf("mood.sad", "mood.stressed", "mood.anxious", "mood.angry")
private val DripNoteColumns = listOf("note.value", "temperature.note", "sex.note", "pain.note", "mood.note")

/** The journal's 1..5 scales: a good day reads 4, a hard one 2, both at once the middle. */
private const val LevelGood = 4
private const val LevelMiddle = 3
private const val LevelLow = 2

/**
 * The day's journal entry: symptoms from drip's pain and mood switches, mood
 * and energy levels from the same switches, the vulvar sensation from the
 * mucus feeling, the cervix as drip felt it (0 closed, 1 medium, 2 open;
 * 0 hard, 1 soft; 0 low, 1 medium, 2 high), desire (0 low, 1 medium,
 * 2 high) on the 1 to 5 scale, and every note. Null when the row records
 * none of it.
 */
private fun dripJournalEntry(cells: DripCells, date: LocalDate, mucusExcluded: Boolean): CycleJournalEntry? {
    val symptoms = DripSymptomColumns.filterKeys { cells.flag(it) }.values.toSet()
    val positive = DripPositiveMoods.any { cells.flag(it) }
    val negative = DripNegativeMoods.any { cells.flag(it) }
    val mood = when {
        positive && negative -> LevelMiddle
        positive -> LevelGood
        negative -> LevelLow
        else -> null
    }
    val energetic = cells.flag("mood.energetic")
    val fatigued = cells.flag("mood.fatigue")
    val energy = when {
        energetic && fatigued -> LevelMiddle
        energetic -> LevelGood
        fatigued -> LevelLow
        else -> null
    }
    val sensation = if (mucusExcluded) {
        null
    } else {
        when (cells.int("mucus.feeling")) {
            0 -> CervicalSensation.DRY
            2 -> CervicalSensation.WET
            3 -> CervicalSensation.SLIPPERY
            else -> null
        }
    }
    val cervixExcluded = cells.flag("cervix.exclude")
    val desire = when (cells.int("desire.value")) {
        0 -> LevelLow
        1 -> LevelMiddle
        2 -> LevelGood
        else -> null
    }
    val notes = DripNoteColumns.mapNotNull { cells.text(it) }.joinToString("\n")
    val entry = CycleJournalEntry(
        date = date,
        symptoms = symptoms,
        moodLevel = mood,
        energyLevel = energy,
        desireLevel = desire,
        cervicalSensation = sensation,
        cervixOpening = if (cervixExcluded) null else CervixOpening.entries.getOrNull(cells.int("cervix.opening") ?: -1),
        cervixFirmness = if (cervixExcluded) null else CervixFirmness.entries.getOrNull(cells.int("cervix.firmness") ?: -1),
        cervixPosition = if (cervixExcluded) null else CervixPosition.entries.getOrNull(cells.int("cervix.position") ?: -1),
        notes = notes,
    )
    return entry.takeIf { it.hasObservations }
}

private fun dripRecord(
    targetType: String,
    recordType: KClass<out Record>,
    time: Instant,
    canonicalValue: Double? = null,
    build: (androidx.health.connect.client.records.metadata.Metadata) -> Record,
): CsvConvertedRecord {
    val clientRecordId = buildCsvClientRecordId(targetType = targetType, utc = time)
    return CsvConvertedRecord(
        metric = null,
        targetType = targetType,
        recordType = recordType,
        clientRecordId = clientRecordId,
        instant = time,
        canonicalValue = canonicalValue,
        record = build(csvMetadata(clientRecordId)),
    )
}
