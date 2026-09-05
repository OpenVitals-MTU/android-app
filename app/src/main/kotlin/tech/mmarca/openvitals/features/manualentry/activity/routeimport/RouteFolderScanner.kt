package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One activity file in a picked folder. A SAF URI, not a path: the URI is what grants access. */
data class RouteFolderFile(
    val uri: Uri,
    val name: String,
)

/** What a folder scan found. */
data class RouteFolderScan(
    val files: List<RouteFolderFile>,
    /** The folder held more files than the scan lists. Reported, never swallowed. */
    val truncated: Boolean,
)

/**
 * Lists the activity files under a folder picked with `OpenDocumentTree`,
 * recursively. A tree URI, because scoped storage cannot open a raw path
 * without All-files access. Names and URIs only: the importer opens each
 * file when it reaches it, so the heap never carries more than one.
 */
@Singleton
class RouteFolderScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Every file under [treeUri] whose name ends in one of [extensions],
     * sorted by name, which for a watch is ride order. Throws when the tree
     * cannot be read; a file that fails to open later is the importer's.
     */
    suspend fun scan(treeUri: Uri, extensions: List<String>): RouteFolderScan = withContext(Dispatchers.IO) {
        val files = mutableListOf<RouteFolderFile>()
        var truncated = false

        // Breadth-first, so the picked folder is listed before what is under it.
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(DocumentsContract.getTreeDocumentId(treeUri) to 0)
        while (queue.isNotEmpty() && !truncated) {
            val (documentId, depth) = queue.removeFirst()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(0) ?: continue
                    val name = cursor.getString(1) ?: continue
                    val isDirectory = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                    if (isDirectory) {
                        if (depth < MaxFolderDepth) queue.add(childId to depth + 1)
                        continue
                    }
                    if (extensions.none { name.endsWith(it, ignoreCase = true) }) continue
                    if (files.size >= MaxFolderFiles) {
                        truncated = true
                        return@use
                    }
                    files.add(
                        RouteFolderFile(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId),
                            name = name,
                        ),
                    )
                }
            }
        }

        files.sortBy { it.name }
        RouteFolderScan(files = files, truncated = truncated)
    }

    companion object {
        /** Guards against a pick of the storage root. Hitting the cap is reported as truncated. */
        const val MaxFolderDepth = 8
        const val MaxFolderFiles = 50_000

        /** The extensions a FIT folder import scans for. */
        val FitExtensions: List<String> = listOf(".fit")
    }
}
