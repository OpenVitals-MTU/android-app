package tech.mmarca.openvitals.core.diagnostics

import android.app.Activity
import android.app.ApplicationErrorReport
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.R

class CrashReportEmailActivity : AppCompatActivity() {
    private var fallbackDraft: CrashReportEmailDraft? = null

    private val saveReportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val draft = fallbackDraft
        if (draft == null || uri == null) {
            finish()
            return@registerForActivityResult
        }
        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(CrashReportEmail.buildShareText(draft).toByteArray(Charsets.UTF_8))
            } ?: error("Unable to open destination.")
        }.fold(
            onSuccess = {
                Toast.makeText(this, R.string.crash_report_fallback_saved, Toast.LENGTH_SHORT).show()
            },
            onFailure = {
                Toast.makeText(this, R.string.crash_report_fallback_save_failed, Toast.LENGTH_SHORT).show()
            },
        )
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val draft = withContext(Dispatchers.IO) {
                val appContext = applicationContext
                CrashReportEmail.buildDraft(
                    appInfo = CrashReportStore.appInfo(appContext),
                    reportDetails = CrashReportStore.detailsFromApplicationErrorReport(
                        intent.applicationErrorReport()
                    ),
                    savedCrashReport = CrashReportStore.readLastCrashReport(appContext),
                    diagnosticsLog = CrashReportStore.captureDiagnosticsLog(appContext),
                )
            }
            if (openEmailDraft(draft)) {
                finish()
            }
        }
    }

    private fun openEmailDraft(draft: CrashReportEmailDraft): Boolean {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("mailto", CrashReportEmailRecipient, null)
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CrashReportEmailRecipient))
            putExtra(Intent.EXTRA_SUBJECT, draft.subject)
            putExtra(Intent.EXTRA_TEXT, draft.body)
        }
        if (packageManager.queryIntentActivities(emailIntent, 0).isEmpty()) {
            showFallbackDialog(draft)
            return false
        }
        return runCatching {
            startActivity(
                Intent.createChooser(
                    emailIntent,
                    getString(R.string.crash_report_email_chooser_title),
                )
            )
        }.fold(
            onSuccess = { true },
            onFailure = { throwable ->
                if (throwable is ActivityNotFoundException) {
                    showFallbackDialog(draft)
                    false
                } else {
                    throw throwable
                }
            },
        )
    }

    private fun showFallbackDialog(draft: CrashReportEmailDraft) {
        fallbackDraft = draft
        AlertDialog.Builder(this)
            .setTitle(R.string.crash_report_fallback_title)
            .setMessage(getString(R.string.crash_report_fallback_body, CrashReportEmailRecipient))
            .setNegativeButton(R.string.crash_report_fallback_copy) { _, _ ->
                copyReportToClipboard(draft)
                finish()
            }
            .setPositiveButton(R.string.crash_report_fallback_save) { _, _ ->
                saveReport(draft)
            }
            .setNeutralButton(android.R.string.cancel) { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun copyReportToClipboard(draft: CrashReportEmailDraft) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.crash_report_clipboard_label),
                CrashReportEmail.buildShareText(draft),
            )
        )
        Toast.makeText(this, R.string.crash_report_fallback_copied, Toast.LENGTH_SHORT).show()
    }

    private fun saveReport(draft: CrashReportEmailDraft) {
        fallbackDraft = draft
        runCatching {
            saveReportLauncher.launch("openvitals-report.txt")
        }.onFailure { throwable ->
            if (throwable is ActivityNotFoundException) {
                copyReportToClipboard(draft)
                Toast.makeText(
                    this,
                    R.string.crash_report_fallback_save_unavailable,
                    Toast.LENGTH_SHORT,
                ).show()
                finish()
            } else {
                throw throwable
            }
        }
    }

    private fun Intent.applicationErrorReport(): ApplicationErrorReport? {
        if (action != Intent.ACTION_APP_ERROR) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_BUG_REPORT, ApplicationErrorReport::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_BUG_REPORT) as? ApplicationErrorReport
        }
    }

    companion object {
        fun createIssueReportIntent(context: Context): Intent =
            Intent(context, CrashReportEmailActivity::class.java).apply {
                action = ActionReportIssue
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

        private const val ActionReportIssue = "tech.mmarca.openvitals.action.REPORT_ISSUE"
    }
}
