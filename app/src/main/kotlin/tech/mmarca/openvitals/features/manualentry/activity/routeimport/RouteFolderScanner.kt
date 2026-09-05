package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One activity file found inside a picked folder: its display name, and the
 * document URI that can open it.
 *
 * A URI, not a path. The folder pick hands back a SAF **tree URI**, and that
 * URI — not any `/storage/emulated/0/...` string derived from it — is what
 * grants access to the files under it. See [RouteFolderScanner].
 */
data class RouteFolderFile(
    val uri: Uri,
    val name: String,
)

/** What a folder scan found. */
data class RouteFolderScan(
    val files: List<RouteFolderFile>,
    /**
     * The folder held more files than the scan is willing to list. Reported
     * rather than swallowed: an import that quietly skips half a folder reads
     * to the user like an import that finished.
     */
    val truncated: Boolean,
)

/**
 * Lists the activity files under a folder the user picked with
 * `OpenDocumentTree`, recursively, so the whole folder can go through the bulk
 * importer one file at a time.
 *
 * ## Why a tree URI and not a path
 *
 * Under scoped storage the app cannot open a non-media file at a raw
 * filesystem path — a `.fit` under `Documents/` reads back as
 * `FileNotFoundException` — and the only way to make such a path work is
 * All-files access, a permission a health app has no business holding. The
 * tree URI is kept, walked with [DocumentsContract], and each child opened
 * through the `ContentResolver`. The pick grants access to the tree; nothing
 * else is needed, and no permission is declared.
 *
 * ## Why the bytes are not returned by the scan
 *
 * [scan] returns names and URIs only. A folder of a year's rides is hundreds
 * of megabytes, and the heap should never carry more than one file: the bulk
 * importer opens each URI when it reaches it and drops the bytes when done.
 *
 * Port of the Flutter build's native `MainActivity.scanFolder`, which served
 * the Dart `RouteFolderSource` over a method channel.
 */
@Singleton
class RouteFolderScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /**
     * Walks [treeUri] and returns every file whose name ends in one of
     * [extensions] (compared case-insensitively, e.g. `[".fit"]`), sorted by
     * name.
     *
     * Name order: a watch names its files by timestamp, so this is the order
     * the rides were ridden in, and the import reads like a diary rather than
     * a shuffle.
     *
     * Throws when the tree cannot be read at all. A single file that later
     * fails to open is the bulk importer's business, counted as one failed
     * file rather than a failed folder.
     */
    suspend fun scan(treeUri: Uri, extensions: List<String>): RouteFolderScan = withContext(Dispatchers.IO) {
        val files = mutableListOf<RouteFolderFile>()
        var truncated = false

        // Breadth-first, so the folder the user actually picked is listed
        // before whatever is buried under it.
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
        /**
         * Guards against a pick of the storage root: a walk that deep, or a
         * list that long, is a mis-pick rather than an import, and neither the
         * walk nor the heap should pay for it. Hitting the file cap is reported
         * as [RouteFolderScan.truncated] rather than silently dropped.
         */
        const val MaxFolderDepth = 8
        const val MaxFolderFiles = 50_000

        /** The extensions a FIT folder import scans for. */
        val FitExtensions: List<String> = listOf(".fit")
    }
}
