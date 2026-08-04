package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

internal object KmzRouteParser {
    fun parse(kmzBytes: ByteArray, fileName: String? = null): RouteFileImport {
        val candidates = kmzBytes.zipRouteCandidates()
        require(candidates.isNotEmpty()) {
            "KMZ file must contain a .gpx or .kml route file."
        }

        val failures = mutableListOf<Throwable>()
        candidates.sortedWith(routeCandidateComparator()).forEach { candidate ->
            val result = runCatching {
                if (candidate.name.endsWith(".gpx", ignoreCase = true)) {
                    GpxRouteParser.parse(candidate.bytes.toString(Charsets.UTF_8), fileName = fileName ?: candidate.name)
                } else {
                    KmlRouteParser.parse(candidate.bytes.toString(Charsets.UTF_8), fileName = fileName ?: candidate.name)
                }
            }
            result.onSuccess { return it }
            result.onFailure(failures::add)
        }

        throw IllegalArgumentException(
            failures.firstOrNull()?.message
                ?: "KMZ file must contain at least $MinRoutePoints route coordinates.",
        )
    }
}

private data class ZipRouteCandidate(
    val name: String,
    val bytes: ByteArray,
)

private fun ByteArray.zipRouteCandidates(): List<ZipRouteCandidate> {
    val candidates = mutableListOf<ZipRouteCandidate>()
    ZipInputStream(ByteArrayInputStream(this)).use { zipInput ->
        while (true) {
            val entry = zipInput.nextEntry ?: break
            if (!entry.isDirectory && (entry.name.hasExtension("gpx") || entry.name.hasExtension("kml"))) {
                require(entry.size <= MaxKmzRouteEntryBytes || entry.size == -1L) {
                    "KMZ route entry is too large."
                }
                candidates += ZipRouteCandidate(
                    name = entry.name,
                    bytes = zipInput.readBytesBounded(MaxKmzRouteEntryBytes, "KMZ route entry is too large."),
                )
            }
            zipInput.closeEntry()
        }
    }
    return candidates
}

private fun routeCandidateComparator(): Comparator<ZipRouteCandidate> =
    compareBy<ZipRouteCandidate>(
        { if (it.name.equals("doc.kml", ignoreCase = true)) 0 else 1 },
        { if (it.name.endsWith(".gpx", ignoreCase = true)) 0 else 1 },
        { it.name },
    )
