package tech.mmarca.openvitals.features.imports.csv

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** Which step of the importer the screen is showing. */
enum class CsvImportStep { PICK, MAPPING, CONFIRM, IMPORTING, DONE }

/**
 * The importer's state.
 *
 * Progress and result are a tally, not a success-or-failure command: a finished
 * import is "written 4,102, already present 210, rejected 68", and it is
 * observable *while it runs*.
 */
data class CsvImportState(
    val step: CsvImportStep = CsvImportStep.PICK,

    /** The picked document. Never its bytes. */
    val uri: Uri? = null,
    val fileName: String? = null,

    /** File size from the SAF provider; zero when it does not say. */
    val totalBytes: Long = 0,
    val sample: CsvSample? = null,
    val mapping: CsvImportMapping? = null,
    val issues: List<CsvMappingIssue> = emptyList(),
    val isLoadingFile: Boolean = false,
    val isImporting: Boolean = false,
    val progress: CsvImportProgress? = null,
    val result: CsvImportResult? = null,

    /**
     * A failure that stopped the screen doing anything useful — an unreadable
     * file, not a rejected row.
     */
    val error: String? = null,
    val granted: Set<String> = emptySet(),

    /**
     * The write permissions the import flow can actually ask for on this
     * device. Requesting one outside this set would be refused rather than
     * prompted, so the request set is intersected with it.
     */
    val supportedWritePermissions: Set<String> = emptySet(),
) {
    /** Whether the mapping is complete enough to import. */
    val canContinue: Boolean
        get() = issues.isEmpty() && (mapping?.metricColumns?.isNotEmpty() == true)

    /**
     * The write permissions this mapping needs, can actually be asked for on
     * this device, and does not already have.
     */
    val missingPermissions: Set<String>
        get() {
            val required = mapping?.requiredWritePermissions ?: emptySet()
            return required.intersect(supportedWritePermissions) - granted
        }

    /** Sampled rows, for validation and the preview table. */
    val sampleRows: List<List<String>> get() = sample?.dataRows ?: emptyList()
}

/** Owns one CSV import from pick to result. */
@HiltViewModel
class CsvImportViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val importService: CsvImportService,
    private val healthRepository: HealthRepository,
    healthConnectManager: HealthConnectManager,
) : ViewModel() {

    private val reader = CsvTableReader()
    private var cancellation: CsvImportCancellation? = null

    private val _uiState = MutableStateFlow(
        CsvImportState(supportedWritePermissions = healthConnectManager.dataImportWritePermissions),
    )
    val uiState: StateFlow<CsvImportState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    /** Re-reads what Health Connect has granted; call after a request returns. */
    fun refreshPermissions() {
        viewModelScope.launch {
            val granted = runCatching { healthRepository.grantedPermissions() }.getOrNull() ?: return@launch
            _uiState.update { it.copy(granted = granted) }
        }
    }

    /** Loads the head of [uri] and advances to the mapping step. */
    fun pickFile(uri: Uri) {
        _uiState.update {
            it.copy(isLoadingFile = true, error = null, result = null, progress = null)
        }
        viewModelScope.launch {
            try {
                val (fileName, totalBytes) = withContext(Dispatchers.IO) { describeDocument(uri) }
                val sample = withContext(Dispatchers.IO) { reader.sample(sourceFor(uri)) }

                if (sample.isEmpty) {
                    _uiState.update {
                        it.copy(
                            isLoadingFile = false,
                            uri = uri,
                            fileName = fileName,
                            totalBytes = totalBytes,
                            sample = sample,
                            mapping = null,
                            step = CsvImportStep.MAPPING,
                            issues = emptyList(),
                        )
                    }
                    return@launch
                }

                val mapping = initialCsvMapping(headerRow = sample.headerRow, sample = sample.dataRows)
                _uiState.update {
                    it.copy(
                        isLoadingFile = false,
                        uri = uri,
                        fileName = fileName,
                        totalBytes = totalBytes,
                        sample = sample,
                        mapping = mapping,
                        issues = validateCsvMapping(mapping, sample.dataRows),
                        step = CsvImportStep.MAPPING,
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoadingFile = false, error = error.message ?: error.toString()) }
            }
        }
    }

    /** Re-reads the head of the file under a changed dialect. */
    fun setDialect(dialect: CsvDialect, hasHeaderRow: Boolean? = null) {
        val uri = _uiState.value.uri ?: return
        val header = hasHeaderRow ?: _uiState.value.sample?.hasHeaderRow ?: true

        _uiState.update { it.copy(isLoadingFile = true) }
        viewModelScope.launch {
            try {
                val sample = withContext(Dispatchers.IO) {
                    reader.sample(sourceFor(uri), dialect = dialect, hasHeaderRow = header)
                }
                val mapping = initialCsvMapping(headerRow = sample.headerRow, sample = sample.dataRows)
                _uiState.update {
                    it.copy(
                        isLoadingFile = false,
                        sample = sample,
                        mapping = mapping,
                        issues = validateCsvMapping(mapping, sample.dataRows),
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoadingFile = false, error = error.message ?: error.toString()) }
            }
        }
    }

    /**
     * Points column [columnIndex] at [role]/[metric], defaulting the
     * interpretation from the column's own header unit.
     */
    fun setColumnRole(
        columnIndex: Int,
        role: CsvColumnRole,
        metric: CsvImportMetric? = null,
    ) {
        val mapping = _uiState.value.mapping ?: return

        val headerUnit = headerUnit(columnIndex)
        val spec = metric?.let { CsvMetricCatalog[it] }
        val interpretation = when {
            spec == null -> null
            headerUnit == null -> spec.defaultInterpretation
            else -> interpretationForUnit(spec, headerUnit) ?: spec.defaultInterpretation
        }

        applyMapping(
            mapping.withColumn(
                CsvColumnMapping(
                    columnIndex = columnIndex,
                    role = role,
                    metric = if (role == CsvColumnRole.METRIC) metric else null,
                    interpretation = if (role == CsvColumnRole.METRIC) interpretation else null,
                ),
            ),
        )
    }

    /** Changes how one already-mapped column's number is read. */
    fun setColumnInterpretation(columnIndex: Int, interpretation: CsvValueInterpretation) {
        val mapping = _uiState.value.mapping ?: return
        val column = mapping.columns.firstOrNull { it.columnIndex == columnIndex }
            ?: CsvColumnMapping(columnIndex = columnIndex)
        applyMapping(mapping.withColumn(column.copy(interpretation = interpretation)))
    }

    fun setDateTimeSettings(settings: CsvDateTimeSettings) {
        val mapping = _uiState.value.mapping ?: return
        applyMapping(mapping.copy(dateTime = settings))
    }

    fun goToStep(step: CsvImportStep) {
        _uiState.update { it.copy(step = step) }
    }

    /** Runs the import. Progress lands on the state as it goes. */
    fun startImport() {
        val state = _uiState.value
        val uri = state.uri ?: return
        val mapping = state.mapping ?: return
        val sample = state.sample ?: return

        val cancel = CsvImportCancellation()
        cancellation = cancel
        _uiState.update {
            it.copy(
                step = CsvImportStep.IMPORTING,
                isImporting = true,
                progress = CsvImportProgress(),
                result = null,
                error = null,
            )
        }

        viewModelScope.launch {
            val result = importService.run(
                source = sourceFor(uri),
                totalBytes = state.totalBytes,
                dialect = sample.dialect,
                mapping = mapping,
                hasHeaderRow = sample.hasHeaderRow,
                cancellation = cancel,
                onProgress = { progress ->
                    _uiState.update { it.copy(progress = progress) }
                },
            )
            _uiState.update {
                it.copy(
                    isImporting = false,
                    step = CsvImportStep.DONE,
                    progress = result.progress,
                    result = result,
                )
            }
            cancellation = null
        }
    }

    fun cancelImport() {
        cancellation?.cancel()
    }

    /** The finished run rendered as text, or null when there is nothing to save. */
    fun reportText(): String? {
        val state = _uiState.value
        val result = state.result ?: return null
        val mapping = state.mapping ?: return null
        return buildCsvImportReport(
            fileName = state.fileName,
            mapping = mapping,
            result = result,
            headerRow = state.sample?.headerRow ?: emptyList(),
            fieldDelimiter = state.sample?.dialect?.fieldDelimiter,
            hasHeaderRow = state.sample?.hasHeaderRow,
        )
    }

    /** Returns to the pick step, keeping nothing from the finished run. */
    fun reset() {
        _uiState.update {
            CsvImportState(
                granted = it.granted,
                supportedWritePermissions = it.supportedWritePermissions,
            )
        }
    }

    private fun applyMapping(mapping: CsvImportMapping) {
        _uiState.update {
            it.copy(
                mapping = mapping,
                issues = validateCsvMapping(mapping, it.sampleRows),
            )
        }
    }

    private fun headerUnit(columnIndex: Int): CsvUnit? {
        val header = _uiState.value.sample?.headerRow ?: return null
        return header.getOrNull(columnIndex)?.let(::detectCsvUnitInHeader)
    }

    private fun sourceFor(uri: Uri): CsvInputSource =
        CsvInputSource { context.contentResolver.openInputStream(uri) }

    private fun describeDocument(uri: Uri): Pair<String?, Long> {
        var name: String? = null
        var size = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        return name to size
    }
}
