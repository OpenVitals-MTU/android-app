package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

data class RouteFileImport(
    val fileName: String?,
    val points: List<ExerciseRoutePoint>,
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
    val activeCaloriesKcal: Double? = null,
    val totalCaloriesKcal: Double? = null,
    val startTime: Instant,
    val endTime: Instant,
    val durationSeconds: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    /**
     * The per-second series the file recorded: heart rate, cadence, speed.
     *
     * A FIT import used to arrive with a route and NOTHING else — the parser
     * read only lat/long/altitude off each `record` message and threw away the
     * heart rate, the cadence and the speed sitting beside them. So an imported
     * activity had no graphs at all, and an indoor ride (no GPS) had nothing
     * whatsoever.
     *
     * Named for the BLE recorder because it is the same series and the same
     * write path; the source is the file, not a sensor.
     */
    val bleSamples: BleRecordingSampleBuffer = BleRecordingSampleBuffer(),
    val hasRecordedTimestamps: Boolean = true,
    val hasImportedTimeRange: Boolean = true,
    val originalPointCount: Int = points.size,
)

@Singleton
class RouteFileImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun import(uri: Uri): RouteFileImport = withContext(Dispatchers.IO) {
        val fileName = uri.displayName(context)
        val routeBytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytesBounded(MaxRouteFileBytes, "Activity file is too large.") }
            ?: throw IllegalArgumentException("Unable to read activity file.")

        RouteFileParser.parseFile(routeBytes, fileName = fileName)
    }

    /**
     * The nightly HRV readings a Garmin wellness FIT carries — the fallback for
     * a FIT file that failed to parse as an activity. Empty for non-FIT files
     * and for FIT files with no HRV summary.
     */
    internal suspend fun importFitWellnessHrv(uri: Uri): List<FitHrvReading> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytesBounded(MaxRouteFileBytes, "Activity file is too large.") }
            ?: return@withContext emptyList()
        if (!bytes.isFitFile()) return@withContext emptyList()
        runCatching { FitRouteParser.parseWellnessHrv(bytes) }.getOrDefault(emptyList())
    }
}

internal object RouteFileParser {
    fun parseFile(fileBytes: ByteArray, fileName: String? = null): RouteFileImport {
        require(fileBytes.size <= MaxRouteFileBytes) {
            "Activity file is too large."
        }
        try {
            if (fileBytes.isFitFile() || fileName.hasExtension("fit")) {
                return FitRouteParser.parse(fileBytes, fileName = fileName)
            }

            if (fileBytes.isZipArchive() || fileName.hasExtension("kmz")) {
                return KmzRouteParser.parse(fileBytes, fileName = fileName)
            }

            val routeText = fileBytes.toString(Charsets.UTF_8)
            return when {
                fileName.hasExtension("kml") || routeText.contains("<kml", ignoreCase = true) -> {
                    KmlRouteParser.parse(routeText, fileName = fileName)
                }
                // Before the GPX fallback, and that ORDER is the fix. A TCX is
                // XML, so it used to fall through to the GPX parser, which found
                // no `trkpt` and threw "GPX route must contain at least 2
                // timestamped location points" — the message users reported when
                // importing an indoor activity, on a file that was never a GPX
                // and carried a perfectly complete session.
                fileName.hasExtension("tcx") || TcxRouteParser.looksLikeTcx(routeText) -> {
                    TcxRouteParser.parse(routeText, fileName = fileName)
                }
                else -> parse(routeText, fileName = fileName)
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalArgumentException("Activity file is not a valid GPX, KML, KMZ, TCX, or FIT file.", error)
        }
    }

    fun parse(gpxText: String, fileName: String? = null): RouteFileImport =
        GpxRouteParser.parse(gpxText, fileName = fileName)
}

private fun Uri.displayName(context: Context): String? {
    context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
    return lastPathSegment
}
