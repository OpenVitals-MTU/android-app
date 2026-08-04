package tech.mmarca.openvitals.features.imports.csv

/**
 * What the user decided each CSV column means, and whether that decision is
 * usable. Pure immutable values — no repository, no clock, no context — so the
 * screen can re-validate on every edit and the tests can assert one issue at a
 * time.
 */

/** What a column is used for. */
enum class CsvColumnRole {
    /** Not imported. The default — a column has to be opted in. */
    IGNORE,

    /** The measurement's date and time. */
    TIMESTAMP,

    /** A body metric. */
    METRIC,
}

/** One column's assignment. */
data class CsvColumnMapping(
    val columnIndex: Int,
    val role: CsvColumnRole = CsvColumnRole.IGNORE,
    /** Set only when [role] is [CsvColumnRole.METRIC]. */
    val metric: CsvImportMetric? = null,
    /**
     * How this column's number becomes the metric's canonical value. Set only
     * when [role] is [CsvColumnRole.METRIC].
     */
    val interpretation: CsvValueInterpretation? = null,
) {
    val isMetric: Boolean get() = role == CsvColumnRole.METRIC && metric != null

    val isTimestamp: Boolean get() = role == CsvColumnRole.TIMESTAMP

    /**
     * The interpretation to actually use, falling back to the metric's default
     * so a mapping is never half-specified.
     */
    val effectiveInterpretation: CsvValueInterpretation?
        get() {
            val selected = metric ?: return null
            return interpretation ?: CsvMetricCatalog[selected]?.defaultInterpretation
        }
}

/** The complete decision: every column, plus how to read the timestamps. */
data class CsvImportMapping(
    val columns: List<CsvColumnMapping>,
    val dateTime: CsvDateTimeSettings = CsvDateTimeSettings(),
) {
    /** Every column mapped to a metric, in column order. */
    val metricColumns: List<CsvColumnMapping> get() = columns.filter { it.isMetric }

    /** The single timestamp column, or null when none or several are set. */
    val timestampColumn: CsvColumnMapping?
        get() = columns.filter { it.isTimestamp }.singleOrNull()

    /** The column supplying body weight, which a mass-share derivation needs. */
    val weightColumn: CsvColumnMapping?
        get() = metricColumns.firstOrNull { it.metric == CsvImportMetric.WEIGHT }

    /** Whether any mapped metric derives its value from the row's weight. */
    val needsWeightColumn: Boolean
        get() = metricColumns.any { it.effectiveInterpretation?.needsRowWeight == true }

    /**
     * The Health Connect write permissions this mapping actually needs — not
     * every import write, only the metrics in use.
     */
    val requiredWritePermissions: Set<String>
        get() = metricColumns.mapNotNullTo(mutableSetOf()) { column ->
            column.metric?.let { CsvMetricCatalog[it]?.writePermission }
        }

    /** Replaces the mapping for one column. */
    fun withColumn(column: CsvColumnMapping): CsvImportMapping = copy(
        columns = columns.map { existing ->
            if (existing.columnIndex == column.columnIndex) column else existing
        },
    )
}

/** Why a mapping cannot be imported yet. Each maps to one string resource. */
enum class CsvMappingIssue {
    /** Nothing says when the measurements happened. */
    NO_TIMESTAMP_COLUMN,

    /** More than one column claims to be the timestamp. */
    MULTIPLE_TIMESTAMP_COLUMNS,

    /** Nothing to import. */
    NO_METRIC_COLUMNS,

    /** Two columns map to the same metric, so one would overwrite the other. */
    DUPLICATE_METRIC,

    /** Body fat is given as a mass but no weight column is mapped to divide by. */
    MASS_SHARE_NEEDS_WEIGHT_COLUMN,

    /** The chosen timestamp format parses none of the sampled rows. */
    TIMESTAMP_FORMAT_MATCHES_NO_SAMPLE_ROW,

    /** Day-first and month-first both fit; the user has to say which. */
    AMBIGUOUS_DAY_MONTH_ORDER,
}

/** Checks [mapping] against [sample] rows, returning every reason it cannot run. */
fun validateCsvMapping(
    mapping: CsvImportMapping,
    sample: List<List<String>>,
): List<CsvMappingIssue> {
    val issues = mutableListOf<CsvMappingIssue>()

    val timestamps = mapping.columns.filter { it.isTimestamp }
    if (timestamps.isEmpty()) {
        issues += CsvMappingIssue.NO_TIMESTAMP_COLUMN
    } else if (timestamps.size > 1) {
        issues += CsvMappingIssue.MULTIPLE_TIMESTAMP_COLUMNS
    }

    val metricColumns = mapping.metricColumns
    if (metricColumns.isEmpty()) {
        issues += CsvMappingIssue.NO_METRIC_COLUMNS
    }

    val seen = mutableSetOf<CsvImportMetric>()
    for (column in metricColumns) {
        if (!seen.add(column.metric!!)) {
            issues += CsvMappingIssue.DUPLICATE_METRIC
            break
        }
    }

    // Asked of the INTERPRETATION, not the metric: "body fat as a percentage"
    // needs no weight column, "body fat as a mass in kg" does. Modelling the
    // requirement on the value is what keeps this from being a special case.
    if (mapping.needsWeightColumn && mapping.weightColumn == null) {
        issues += CsvMappingIssue.MASS_SHARE_NEEDS_WEIGHT_COLUMN
    }

    if (timestamps.size == 1 && sample.isNotEmpty()) {
        val index = timestamps.single().columnIndex
        val values = sample.mapNotNull { row ->
            row.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (values.isNotEmpty()) {
            val parsed = values.count {
                parseCsvWallClock(it, mapping.dateTime.format, mapping.dateTime.customPattern) != null
            }
            if (parsed == 0) {
                issues += CsvMappingIssue.TIMESTAMP_FORMAT_MATCHES_NO_SAMPLE_ROW
            } else if (mapping.dateTime.format == CsvDateTimeFormat.AUTO &&
                detectCsvDateTimeFormat(values).ambiguousDayMonth
            ) {
                // Only while the format is still AUTO. Once the user has picked
                // day-first or month-first they have answered the question, and
                // repeating it would block a mapping that is now fully specified.
                issues += CsvMappingIssue.AMBIGUOUS_DAY_MONTH_ORDER
            }
        }
    }

    return issues
}

/**
 * A starting mapping for [headerRow]: everything ignored, except the first
 * column that parses as a date, which is pre-selected as the timestamp.
 *
 * Deliberately does NOT guess metrics from header text — that would be the
 * vendor-preset behaviour this importer does without. The date guess is safe
 * because it is checked against the DATA, not the label.
 */
fun initialCsvMapping(
    headerRow: List<String>,
    sample: List<List<String>>,
): CsvImportMapping {
    var timestampIndex = -1
    for (index in headerRow.indices) {
        val values = sample.mapNotNull { row ->
            row.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (values.isEmpty()) continue
        val detection = detectCsvDateTimeFormat(values)
        if (detection.matchedRows == detection.totalRows) {
            timestampIndex = index
            break
        }
    }

    return CsvImportMapping(
        columns = headerRow.indices.map { index ->
            CsvColumnMapping(
                columnIndex = index,
                role = if (index == timestampIndex) CsvColumnRole.TIMESTAMP else CsvColumnRole.IGNORE,
            )
        },
    )
}
