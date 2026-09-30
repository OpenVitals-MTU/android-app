package tech.mmarca.openvitals.features.imports.medical

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * The file types the picker offers: FHIR JSON and NDJSON, which many apps label loosely, CDA
 * documents as XML, export zips, and SMART Health Cards as a file, an image of the QR code,
 * or a PDF that shows it.
 */
internal val MedicalImportMimeTypes = arrayOf(
    "application/json",
    "application/fhir+json",
    "application/x-ndjson",
    "text/plain",
    "application/octet-stream",
    "application/zip",
    "text/xml",
    "application/xml",
    "application/smart-health-card",
    "application/pdf",
    "image/*",
)

/** The name the picker shows for [uri], if the provider gives one. Call off the main thread. */
internal fun documentName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null }
    }.getOrNull()

/** Opens [uri] each time it is called. A source may read a file more than once. */
internal fun documentOpener(context: Context, uri: Uri): () -> InputStream = {
    context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
}
