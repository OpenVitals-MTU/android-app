package tech.mmarca.openvitals.features.medical

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.core.export.stageSoleExport
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.toScreenError
import tech.mmarca.openvitals.domain.medical.FhirBundleWriter
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.usecase.ExportMedicalRecordsUseCase
import tech.mmarca.openvitals.domain.usecase.MedicalExportScope

internal const val MedicalExportCacheDirectory = "medical_exports"

/** Stages a medical export for sharing. Each export deletes the one before, so no copy lingers. */
class MedicalExportFiles(private val directory: File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.cacheDir, MedicalExportCacheDirectory))

    fun stage(fileName: String, text: String): File = directory.stageSoleExport(fileName) { it.write(text.toByteArray()) }
}

sealed interface MedicalExportUiState {
    data object Building : MedicalExportUiState

    /** [ownOnly] and [leftOut] say what the file could not hold. */
    data class Ready(
        val file: File,
        val recordCount: Int,
        val ownOnly: Set<MedicalCategory>,
        val leftOut: Set<MedicalCategory>,
    ) : MedicalExportUiState

    data class Failed(val error: ScreenError) : MedicalExportUiState
}

/** Builds one export for the screen that hosts it. The screen then shares or saves the file. */
@HiltViewModel
class MedicalExportViewModel(
    private val exportRecords: ExportMedicalRecordsUseCase,
    private val files: MedicalExportFiles,
    private val dispatchers: DispatcherProvider,
    private val appVersion: String,
    private val now: () -> Instant,
) : ViewModel() {

    @Inject
    constructor(
        exportRecords: ExportMedicalRecordsUseCase,
        files: MedicalExportFiles,
        dispatchers: DispatcherProvider,
    ) : this(exportRecords, files, dispatchers, BuildConfig.VERSION_NAME, Instant::now)

    private val _state = MutableStateFlow<MedicalExportUiState?>(null)
    val state: StateFlow<MedicalExportUiState?> = _state.asStateFlow()
    private var job: Job? = null

    fun export(scope: MedicalExportScope) {
        job?.cancel()
        _state.value = MedicalExportUiState.Building
        job = viewModelScope.launch {
            runCatching {
                val exportedAt = now()
                val export = withContext(dispatchers.default) { exportRecords(scope, appVersion, exportedAt) }
                val fileName = FhirBundleWriter.fileName(exportedAt.atZone(ZoneId.systemDefault()).toLocalDate(), scope.fileNamePart())
                val file = withContext(dispatchers.io) { files.stage(fileName, export.bundleJson) }
                MedicalExportUiState.Ready(file, export.recordCount, export.ownOnly, export.leftOut)
            }.onSuccess { ready ->
                _state.value = ready
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                _state.value = MedicalExportUiState.Failed(failure.toScreenError(logTag = TAG))
            }
        }
    }

    /** Closes the dialog. An export still being built is stopped. */
    fun dismiss() {
        job?.cancel()
        _state.value = null
    }

    private fun MedicalExportScope.fileNamePart(): String? = when (this) {
        MedicalExportScope.All -> null
        is MedicalExportScope.Category -> category.name
        is MedicalExportScope.Record -> "${ref.resourceType}-${ref.resourceId}"
    }

    private companion object {
        const val TAG = "MedicalExport"
    }
}
