package tech.mmarca.openvitals.features.imports.medical

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo
import tech.mmarca.openvitals.domain.medical.FhirRejection
import tech.mmarca.openvitals.domain.medical.MedicalImportGroupPlan
import tech.mmarca.openvitals.domain.medical.MedicalImportTarget
import tech.mmarca.openvitals.domain.medical.MedicalRejectionReason
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkip
import tech.mmarca.openvitals.domain.medical.MedicalSourceSkipReason
import tech.mmarca.openvitals.domain.medical.PatientCheckResult
import tech.mmarca.openvitals.domain.medical.PatientIdentity
import tech.mmarca.openvitals.features.medical.titleRes
import tech.mmarca.openvitals.ui.components.DetailRow
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.StepBar
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** What each group holds and where it goes. The patient check comes first, and stops the wizard until answered. */
@Composable
internal fun MedicalImportReviewStep(state: MedicalImportUiState, viewModel: MedicalImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(LayoutMetrics.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.patientCheck != PatientCheckResult.Pass) {
                item { PatientCheckCard(state.patientCheck, state.patientConfirmed, onConfirm = viewModel::confirmPatient) }
            }
            if (state.unverifiedCard) {
                item { UnverifiedCardNotice() }
            }
            if (state.sourceSkips.isNotEmpty()) {
                item { SourceSkipsCard(state.sourceSkips) }
            }
            itemsIndexed(state.plans) { index, plan ->
                GroupReviewCard(plan = plan, onIncludedChange = { viewModel.setIncluded(index, it) })
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.medical_import_continue),
            onNext = if (state.canContinue) viewModel::goToConfirm else null,
            backLabel = stringResource(R.string.medical_import_choose_another),
            onBack = viewModel::back,
        )
    }
}

/** New sources get a name the user can change. Health Connect never renames a source afterwards. */
@Composable
internal fun MedicalImportConfirmStep(
    state: MedicalImportUiState,
    viewModel: MedicalImportViewModel,
    accessBlocked: Boolean,
    onRequestAccess: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(LayoutMetrics.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            itemsIndexed(state.plans) { index, plan ->
                if (!plan.include || plan.group.ready.isEmpty()) return@itemsIndexed
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        val editable = plan.group.baseUri == null || plan.target is MedicalImportTarget.New
                        if (editable) {
                            var name by remember(index) { mutableStateOf(plan.targetName) }
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; viewModel.rename(index, it) },
                                label = { Text(stringResource(R.string.medical_import_source_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        val target = plan.target
                        if (target is MedicalImportTarget.Existing) {
                            Text(
                                text = stringResource(R.string.medical_import_adds_to_existing, target.source.displayName),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.medical_records_count, plan.group.ready.size, plan.group.ready.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    text = pluralStringResource(R.plurals.medical_import_summary, state.recordsToWrite, state.recordsToWrite),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                if (!state.canWrite) {
                    Text(text = stringResource(R.string.medical_import_needs_write), style = MaterialTheme.typography.bodySmall)
                }
                if (accessBlocked) {
                    Text(text = stringResource(R.string.medical_access_blocked), style = MaterialTheme.typography.bodySmall)
                }
            }
            state.documentSize?.let { size ->
                item { MedicalKeepDocumentCard(size, state.documentsBytes, state.documentCount > 1, state.keepDocument, viewModel::setKeepDocument) }
            }
        }
        StepBar(
            nextLabel = stringResource(accessLabelRes(state.canWrite, accessBlocked, ready = R.string.medical_import_start)),
            onNext = if (state.canWrite) viewModel::startImport else onRequestAccess,
            backLabel = stringResource(R.string.medical_import_back),
            onBack = viewModel::back,
        )
    }
}

/** [ready] once write access is there; otherwise the way to get it. */
@StringRes
internal fun accessLabelRes(canWrite: Boolean, accessBlocked: Boolean, @StringRes ready: Int): Int = when {
    canWrite -> ready
    accessBlocked -> R.string.medical_record_open_health_connect
    else -> R.string.medical_import_allow_access
}

/** Counts, every reason a record was not added, and the report. The report holds medical data, so it warns first. */
@Composable
internal fun MedicalImportDoneStep(state: MedicalImportUiState, viewModel: MedicalImportViewModel, onDone: () -> Unit) {
    val result = state.result ?: return
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.medical_import_report_copied)
    val saved = stringResource(R.string.medical_import_report_saved)
    val saveFailed = stringResource(R.string.medical_import_report_save_failed)
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = viewModel.reportText()
        if (uri != null && text != null) {
            scope.launch {
                offMainIo {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("No stream")
                }.fold(
                    onSuccess = { Toast.makeText(context, saved, Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, saveFailed, Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(LayoutMetrics.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text = result.stoppedBy?.let { stringResource(R.string.medical_import_stopped_title, it) }
                                ?: stringResource(R.string.medical_import_done_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (result.stoppedBy != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                        DetailRow(stringResource(R.string.medical_import_written), result.written.toString())
                        DetailRow(stringResource(R.string.medical_import_updated), result.updated.toString())
                        DetailRow(stringResource(R.string.medical_import_skipped), result.skipped.toString())
                        DetailRow(stringResource(R.string.medical_import_rejected), result.rejected.toString())
                        state.documentKept?.let { kept ->
                            Text(
                                text = keptText(kept, many = state.documentCount > 1, state.keptCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (kept) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            val reasons = result.rejectionReasons
            if (reasons.isNotEmpty()) {
                item {
                    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(text = stringResource(R.string.medical_import_reasons), style = MaterialTheme.typography.titleSmall)
                            reasons.forEach { (reason, count) ->
                                Text(
                                    text = stringResource(R.string.medical_import_reason_line, reasonText(reason), count),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.medical_import_report_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(top = Spacing.sm)) {
                    OpenVitalsOutlinedButton(
                        onClick = {
                            val text = viewModel.reportText() ?: return@OpenVitalsOutlinedButton
                            scope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText("OpenVitals", text).toClipEntry())
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.medical_import_copy_report)) }
                    OpenVitalsOutlinedButton(onClick = { saver.launch(ReportFileName) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.medical_import_save_report))
                    }
                }
            }
        }
        StepBar(
            nextLabel = stringResource(R.string.medical_import_done),
            onNext = onDone,
            backLabel = stringResource(R.string.medical_import_another),
            onBack = viewModel::reset,
        )
    }
}

@Composable
private fun PatientCheckCard(check: PatientCheckResult, confirmed: Boolean, onConfirm: () -> Unit) {
    val title = when (check) {
        is PatientCheckResult.Mismatch -> R.string.medical_import_patient_mismatch_title
        is PatientCheckResult.SeveralPeople -> R.string.medical_import_patient_several_title
        is PatientCheckResult.CannotCompare -> R.string.medical_import_patient_unknown_title
        PatientCheckResult.Pass -> return
    }
    val inFile = when (check) {
        is PatientCheckResult.Mismatch -> check.file
        is PatientCheckResult.SeveralPeople -> check.file
        is PatientCheckResult.CannotCompare -> check.file
        PatientCheckResult.Pass -> emptyList()
    }
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.medical_import_patient_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            inFile.forEach { Text(stringResource(R.string.medical_import_patient_in_file, identityText(it)), style = MaterialTheme.typography.bodyMedium) }
            (check as? PatientCheckResult.Mismatch)?.store?.forEach {
                Text(stringResource(R.string.medical_import_patient_here, identityText(it)), style = MaterialTheme.typography.bodyMedium)
            }
            if (confirmed) {
                Text(text = stringResource(R.string.medical_import_patient_confirmed), style = MaterialTheme.typography.bodySmall)
            } else {
                OpenVitalsOutlinedButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.medical_import_patient_confirm))
                }
            }
        }
    }
}

@Composable
private fun GroupReviewCard(plan: MedicalImportGroupPlan, onIncludedChange: (Boolean) -> Unit) {
    val group = plan.group
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = plan.targetName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOf(
                    stringResource(if (plan.target is MedicalImportTarget.Existing) R.string.medical_import_existing_source else R.string.medical_import_new_source),
                    stringResource(R.string.medical_import_fhir_version, plan.fhirVersion),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            group.likelyCategories.entries.sortedBy { it.key?.ordinal ?: Int.MAX_VALUE }.forEach { (category, count) ->
                DetailRow(
                    label = category?.let { stringResource(it.titleRes) } ?: stringResource(R.string.medical_import_classified_by_hc),
                    value = count.toString(),
                )
            }
            if (group.skippedTypes.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.medical_import_skipped_types, group.skippedTypes.entries.joinToString { "${it.key} (${it.value})" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (group.rejected.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.medical_import_held_back, group.rejected.size, group.rejected.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                group.rejected.take(ShownRejections).forEach {
                    Text(
                        text = "${it.resource.type}/${it.resource.id} · ${reasonText(MedicalRejectionReason.Preflight(it.problem.reason))}" +
                            (it.problem.field?.let { field -> " ($field)" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            plan.otherApp?.let { other ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.medical_import_other_app, appLabel(other.packageName)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = stringResource(R.string.medical_import_include_anyway), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = plan.include, onCheckedChange = onIncludedChange, modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
    }
}

/** A SMART Health Card's signature needs the issuer's keys from the internet, which the app does not use. */
@Composable
private fun UnverifiedCardNotice() {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(R.string.medical_import_card_title), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.medical_import_card_unverified), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Records the source left out before analysis, by reason, such as FHIR DSTU2 in an Apple export. */
@Composable
private fun SourceSkipsCard(skips: List<MedicalSourceSkip>) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = stringResource(R.string.medical_import_source_skips_title), style = MaterialTheme.typography.titleSmall)
            skips.groupingBy { it.reason }.eachCount().forEach { (reason, count) ->
                Text(
                    text = stringResource(R.string.medical_import_reason_line, stringResource(reason.labelRes), count),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private val MedicalSourceSkipReason.labelRes: Int
    get() = when (this) {
        MedicalSourceSkipReason.FHIR_DSTU2 -> R.string.medical_import_skip_dstu2
        MedicalSourceSkipReason.UNSUPPORTED_FHIR_VERSION -> R.string.medical_import_skip_version
        MedicalSourceSkipReason.FILE_MISSING -> R.string.medical_import_skip_missing
        MedicalSourceSkipReason.NOT_INDEXED -> R.string.medical_import_skip_not_indexed
        MedicalSourceSkipReason.UNREADABLE -> R.string.medical_import_skip_unreadable
        MedicalSourceSkipReason.UNREADABLE_DOCUMENT -> R.string.medical_import_skip_unreadable_document
        MedicalSourceSkipReason.NOT_A_RECORD -> R.string.medical_import_skip_not_record
        MedicalSourceSkipReason.CARD_INCOMPLETE -> R.string.medical_import_skip_card_incomplete
    }

/** What happened to the kept files: one picked file, or each document's PDF. */
@Composable
private fun keptText(kept: Boolean, many: Boolean, count: Int): String = when {
    !kept -> stringResource(if (many) R.string.medical_documents_keep_failed_some else R.string.medical_documents_keep_failed)
    many -> stringResource(R.string.medical_documents_kept_count, count)
    else -> stringResource(R.string.medical_documents_kept)
}

@Composable
private fun identityText(identity: PatientIdentity): String {
    val name = identity.displayName.ifBlank { "—" }
    return identity.birthDate?.let { stringResource(R.string.medical_import_patient_born, name, it) } ?: name
}

@Composable
private fun reasonText(reason: MedicalRejectionReason): String = when (reason) {
    is MedicalRejectionReason.Platform -> reason.text
    is MedicalRejectionReason.Preflight -> stringResource(
        when (reason.reason) {
            FhirRejection.UNSUPPORTED_TYPE -> R.string.medical_import_reason_unsupported
            FhirRejection.INVALID_ID -> R.string.medical_import_reason_invalid_id
            FhirRejection.CONTAINED -> R.string.medical_import_reason_contained
            FhirRejection.UNCLASSIFIABLE_OBSERVATION -> R.string.medical_import_reason_unclassifiable
            FhirRejection.EMPTY_VALUE -> R.string.medical_import_reason_empty
        },
    )
}

/** The name another app shows, else its package name, which is all a hidden app gives away. */
@Composable
private fun appLabel(packageName: String): String {
    val packageManager = LocalContext.current.packageManager
    return remember(packageName) {
        runCatching {
            @Suppress("DEPRECATION")
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}

private const val ShownRejections = 5
private const val ReportFileName = "openvitals-medical-import-report.txt"
