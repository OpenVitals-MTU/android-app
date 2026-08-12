package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * Holds the GPS ephemeris files the user supplied, and remembers what the
 * watch has been asking for.
 *
 * The files are small (tens of kilobytes) and are copied into app storage on
 * import rather than read back through the picker's URI: a watch asks for
 * ephemeris days later, in the background, long after any grant on the
 * original file has gone.
 */
@Singleton
class GarminAgpsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val folder: File by lazy { File(context.filesDir, FOLDER).apply { mkdirs() } }

    private val state = MutableStateFlow(read())
    val agps: StateFlow<GarminAgpsState> = state

    /**
     * Copies an ephemeris file in, deciding what it is from its contents
     * rather than its name — the user is downloading these from a third party
     * and the filename tells us nothing we can trust.
     */
    fun import(uri: Uri): GarminAgpsImport {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes(limit = MAX_FILE_BYTES)
            }
        }.getOrNull() ?: return GarminAgpsImport.Unreadable

        if (bytes.isEmpty()) return GarminAgpsImport.Unreadable
        val kind = GarminAgpsFile.classify(bytes) ?: return GarminAgpsImport.NotEphemeris
        if (!GarminAgpsFile.isValid(bytes, kind)) return GarminAgpsImport.Stale

        runCatching { fileFor(kind).writeBytes(bytes) }
            .onFailure { return GarminAgpsImport.Unreadable }

        prefs.edit {
            putLong(importedAtKey(kind), Instant.now().toEpochMilli())
            putInt(sizeKey(kind), bytes.size)
            remove(servedAtKey(kind))
        }
        state.value = read()
        GarminLog.log("[GARMIN-AGPS] imported ${bytes.size}B of $kind ephemeris")
        return GarminAgpsImport.Imported(kind)
    }

    fun forget(kind: GarminAgpsKind) {
        fileFor(kind).delete()
        prefs.edit {
            remove(importedAtKey(kind))
            remove(sizeKey(kind))
            remove(servedAtKey(kind))
        }
        state.value = read()
    }

    /** A source the protocol layer can serve from, with no Android in it. */
    fun source(): GarminAgpsSource = GarminAgpsSource(
        load = { kind -> fileFor(kind).takeIf { it.isFile }?.readBytes() },
        onRequested = { url, kind -> recordRequest(url, kind) },
        onServed = { kind ->
            prefs.edit { putLong(servedAtKey(kind), Instant.now().toEpochMilli()) }
            state.value = read()
        },
        onRejected = { kind, reason ->
            prefs.edit { putString(problemKey(kind), reason) }
            state.value = read()
        },
    )

    /**
     * Remembers a URL the watch asked for. This is the only way a user can
     * find out WHICH ephemeris file their particular watch needs — the URL
     * names the format, and no two chipsets want the same one.
     */
    private fun recordRequest(url: String, kind: GarminAgpsKind?) {
        val known = prefs.readStringList(KEY_KNOWN_URLS).orEmpty()
        if (url in known) return
        prefs.edit {
            putString(
                KEY_KNOWN_URLS,
                JSONArray((known + url).takeLast(MAX_KNOWN_URLS)).toString(),
            )
        }
        GarminLog.log("[GARMIN-AGPS] watch asked for $url ($kind)")
        state.value = read()
    }

    private fun read(): GarminAgpsState = GarminAgpsState(
        files = GarminAgpsKind.entries.mapNotNull { kind ->
            val importedAt = prefs.getLong(importedAtKey(kind), 0L)
            if (importedAt == 0L || !fileFor(kind).isFile) {
                null
            } else {
                GarminAgpsFileState(
                    kind = kind,
                    importedAt = Instant.ofEpochMilli(importedAt),
                    sizeBytes = prefs.getInt(sizeKey(kind), 0),
                    lastServedAt = prefs.getLong(servedAtKey(kind), 0L)
                        .takeIf { it > 0L }
                        ?.let(Instant::ofEpochMilli),
                    problem = prefs.getString(problemKey(kind), null),
                )
            }
        },
        requestedUrls = prefs.readStringList(KEY_KNOWN_URLS).orEmpty(),
    )

    private fun fileFor(kind: GarminAgpsKind) = File(folder, "${kind.name.lowercase()}.bin")

    private fun SharedPreferences.readStringList(key: String): List<String>? {
        val raw = getString(key, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }
        }.getOrNull()
    }

    /** Reads at most [limit] bytes; ephemeris files are tens of kilobytes. */
    private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (buffer.size() < limit) {
            val read = read(chunk)
            if (read <= 0) break
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun importedAtKey(kind: GarminAgpsKind) = "garmin_agps_imported_${kind.name}"
    private fun sizeKey(kind: GarminAgpsKind) = "garmin_agps_size_${kind.name}"
    private fun servedAtKey(kind: GarminAgpsKind) = "garmin_agps_served_${kind.name}"
    private fun problemKey(kind: GarminAgpsKind) = "garmin_agps_problem_${kind.name}"

    private companion object {
        const val PREFS_FILE = "garmin_agps"
        const val FOLDER = "garmin-agps"
        const val KEY_KNOWN_URLS = "garmin_agps_known_urls"

        /** Upstream notes these are usually ~60 KB; 1 MB is a generous cap. */
        const val MAX_FILE_BYTES = 1024 * 1024

        const val MAX_KNOWN_URLS = 8
    }
}

/** What happened to an imported file, for the message the user sees. */
sealed interface GarminAgpsImport {
    data class Imported(val kind: GarminAgpsKind) : GarminAgpsImport
    data object NotEphemeris : GarminAgpsImport
    data object Stale : GarminAgpsImport
    data object Unreadable : GarminAgpsImport
}

data class GarminAgpsState(
    val files: List<GarminAgpsFileState> = emptyList(),
    /** Ephemeris URLs the watch has asked for, oldest first. */
    val requestedUrls: List<String> = emptyList(),
)

data class GarminAgpsFileState(
    val kind: GarminAgpsKind,
    val importedAt: Instant,
    val sizeBytes: Int,
    /** When the watch last took it, or null if it never has. */
    val lastServedAt: Instant?,
    val problem: String?,
)
