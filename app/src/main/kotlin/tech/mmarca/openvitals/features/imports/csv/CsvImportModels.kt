package tech.mmarca.openvitals.features.imports.csv

/** Progress, diagnostics and outcome for a CSV import run. */

/** Why a row, or one metric on a row, did not produce a record. */
enum class CsvImportDiagnosticReason {
    /** The timestamp cell was empty. Costs the whole row. */
    MISSING_TIMESTAMP,

    /** The timestamp cell did not parse under the chosen format. Whole row. */
    UNPARSABLE_TIMESTAMP,

    /** The row had fewer fields than the mapping refers to. Whole row. */
    WRONG_FIELD_COUNT,

    /** A metric cell was not a number. Costs that metric only. */
    UNPARSABLE_NUMBER,

    /** The converted value is outside what a human body can be. That metric only. */
    OUT_OF_RANGE,

    /**
     * Body fat was mapped as a mass, but this row has no usable weight to
     * divide by. That metric only.
     */
    DERIVATION_MISSING_WEIGHT,

    /**
     * The end-timestamp cell did not parse. Costs the interval metrics only —
     * the instant metrics on the row never read it. (A BLANK end is not an
     * error: the row falls back to a one-minute span.)
     */
    UNPARSABLE_END_TIMESTAMP,

    /**
     * The end resolves on or before the start, which no interval can survive —
     * usually TimeFrom and TimeTo mapped the wrong way round. Interval metrics
     * only.
     */
    END_NOT_AFTER_START,

    /** Health Connect refused the record. */
    WRITE_FAILED,
}

/** One rejected row or metric, named well enough for the user to go find it. */
data class CsvImportDiagnostic(
    /** 1-based line number in the file. */
    val rowNumber: Int,
    val reason: CsvImportDiagnosticReason,
    val columnIndex: Int? = null,
    val detail: String? = null,
)

/** How far along a run is. */
data class CsvImportProgress(
    val rowsRead: Int = 0,

    /** Records handed to Health Connect. Includes upserts over existing records. */
    val written: Int = 0,

    /**
     * Records whose id was already in Health Connect. They are still written —
     * the id cannot say whether the value changed — so this is "you have
     * imported this before", not "skipped".
     */
    val alreadyPresent: Int = 0,

    /** Metrics or rows that produced no record. */
    val rejected: Int = 0,

    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
) {
    /** 0..1, or null when the file size is unknown. */
    val fraction: Float?
        get() {
            if (totalBytes <= 0) return null
            return (bytesRead.toDouble() / totalBytes).coerceIn(0.0, 1.0).toFloat()
        }
}

/** Why a run stopped. */
enum class CsvImportOutcome {
    /** Reached the end of the file. */
    COMPLETED,

    /** The user cancelled. Everything written before that stays written. */
    CANCELLED,

    /**
     * Health Connect rate-limited us. Re-running later resumes from the top and
     * re-dedupes, which is cheap at the sizes this importer targets.
     */
    RATE_LIMITED,

    /** Something else failed; [CsvImportResult.error] says what. */
    FAILED,
}

/** The end of a run. */
data class CsvImportResult(
    val outcome: CsvImportOutcome,
    val progress: CsvImportProgress,

    /** Capped at [CSV_MAX_RETAINED_DIAGNOSTICS]; [diagnosticCounts] stays complete. */
    val diagnostics: List<CsvImportDiagnostic> = emptyList(),

    /** Every rejection, counted by reason, with nothing dropped. */
    val diagnosticCounts: Map<CsvImportDiagnosticReason, Int> = emptyMap(),

    val error: String? = null,
) {
    val wroteNothing: Boolean get() = progress.written == 0
}

/**
 * Matches the Apple importer's cap: grouped counts stay exact, the per-row log
 * stops growing so a re-import of an already-imported file cannot produce an
 * unbounded report.
 */
const val CSV_MAX_RETAINED_DIAGNOSTICS = 1000
