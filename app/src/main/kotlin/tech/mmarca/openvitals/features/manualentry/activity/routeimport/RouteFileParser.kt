package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

data class RouteFileImport(
    val fileName: String?,
    val points: List<ExerciseRoutePoint>,
    val distanceMeters: Double,
    val elevationGainedMeters: Double,
    val startTime: Instant,
    val endTime: Instant,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
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
            ?.use { it.readBytesBounded(MaxRouteFileBytes, "Route file is too large.") }
            ?: throw IllegalArgumentException("Unable to read route file.")

        RouteFileParser.parseFile(routeBytes, fileName = fileName)
    }
}

internal object RouteFileParser {
    fun parseFile(fileBytes: ByteArray, fileName: String? = null): RouteFileImport {
        require(fileBytes.size <= MaxRouteFileBytes) {
            "Route file is too large."
        }
        try {
            if (fileBytes.isFitFile() || fileName.hasExtension("fit")) {
                return FitRouteParser.parse(fileBytes, fileName = fileName)
            }

            if (fileBytes.isZipArchive() || fileName.hasExtension("kmz")) {
                return KmzRouteParser.parse(fileBytes, fileName = fileName)
            }

            val routeText = fileBytes.toString(Charsets.UTF_8)
            return if (fileName.hasExtension("kml") || routeText.contains("<kml", ignoreCase = true)) {
                KmlRouteParser.parse(routeText, fileName = fileName)
            } else {
                parse(routeText, fileName = fileName)
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalArgumentException("Route file is not a valid GPX, KML, KMZ, or FIT file.", error)
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
