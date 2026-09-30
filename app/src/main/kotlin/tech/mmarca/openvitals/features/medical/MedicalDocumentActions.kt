package tech.mmarca.openvitals.features.medical

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import java.io.File
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo

/**
 * Hands the kept file to a viewer app. JSON is text, so a phone with no JSON viewer is offered
 * it as plain text. Says so when nothing opens it.
 */
internal fun openMedicalDocument(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val types = listOf(mimeType) + if (mimeType.endsWith("json")) listOf("text/plain") else emptyList()
    val opened = types.any { type ->
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
    if (!opened) Toast.makeText(context, R.string.medical_documents_no_viewer, Toast.LENGTH_LONG).show()
}

internal fun shareMedicalDocument(context: Context, file: File, mimeType: String, fileName: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, fileName, uri)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.medical_documents_share_chooser)))
}

internal suspend fun copyMedicalDocument(context: Context, file: File, destination: Uri): Boolean =
    offMainIo {
        context.contentResolver.openOutputStream(destination)?.use { output -> file.inputStream().use { it.copyTo(output) } }
            ?: error("no output stream for $destination")
    }.isSuccess

/** The system "save as" picker for a file of any type: the input is the name and the type. */
internal class CreateTypedDocument : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.second)
            .putExtra(Intent.EXTRA_TITLE, input.first)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data.takeIf { resultCode == Activity.RESULT_OK }
}
