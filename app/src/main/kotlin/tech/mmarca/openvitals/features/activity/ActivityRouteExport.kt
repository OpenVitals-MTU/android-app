package tech.mmarca.openvitals.features.activity

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import org.xmlpull.v1.XmlSerializer
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.ExerciseRoutePoint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal enum class ActivityRouteExportFormat(
    val mimeType: String,
    val extension: String,
) {
    GPX(GpxMimeType, "gpx"),
    KMZ(KmzMimeType, "kmz"),
}

internal fun Context.openActivityRouteInMap(workout: ExerciseData): Result<Unit> =
    runCatching {
        val routeIntent = createActivityRouteViewIntent(workout)
        startActivity(
            Intent.createChooser(
                routeIntent,
                getString(R.string.activity_route_open_chooser_title),
            )
        )
    }

internal fun Context.saveActivityRouteExport(
    workout: ExerciseData,
    format: ActivityRouteExportFormat,
    destination: Uri,
): Result<Unit> =
    runCatching {
        contentResolver.openOutputStream(destination)?.use { output ->
            writeActivityRouteExport(
                workout = workout,
                format = format,
                output = output,
            )
        } ?: error("Unable to open export destination.")
    }

private fun Context.createActivityRouteViewIntent(workout: ExerciseData): Intent {
    val exportFile = createActivityRouteGpxFile(workout)
    val uri = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        exportFile,
    )
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, GpxMimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(contentResolver, exportFile.name, uri)
    }
}

private fun Context.createActivityRouteGpxFile(workout: ExerciseData): File {
    val routePoints = workout.sortedRoutePointsForExport()

    val exportDir = File(cacheDir, RouteExportCacheDirectory).apply {
        mkdirs()
        deleteOldRouteExports()
    }
    val exportFile = File(exportDir, workout.routeExportFileName(ActivityRouteExportFormat.GPX))
    exportFile.outputStream().use { output ->
        writeActivityRouteGpx(
            workout = workout,
            routePoints = routePoints,
            output = output,
        )
    }
    return exportFile
}

private fun writeActivityRouteExport(
    workout: ExerciseData,
    format: ActivityRouteExportFormat,
    output: OutputStream,
) {
    val routePoints = workout.sortedRoutePointsForExport()
    when (format) {
        ActivityRouteExportFormat.GPX -> writeActivityRouteGpx(workout, routePoints, output)
        ActivityRouteExportFormat.KMZ -> writeActivityRouteKmz(workout, routePoints, output)
    }
}

internal fun writeActivityRouteGpx(
    workout: ExerciseData,
    routePoints: List<ExerciseRoutePoint>,
    output: OutputStream,
) {
    val serializer = Xml.newSerializer()
    serializer.setOutput(output, Charsets.UTF_8.name())
    serializer.startDocument(Charsets.UTF_8.name(), true)
    serializer.startTag(null, "gpx")
    serializer.attribute(null, "version", "1.1")
    serializer.attribute(null, "creator", "OpenVitals")
    serializer.attribute(null, "xmlns", GpxNamespace)

    serializer.startTag(null, "trk")
    workout.title?.takeIf { it.isNotBlank() }?.let { title ->
        serializer.textElement("name", title)
    }
    workout.notes?.takeIf { it.isNotBlank() }?.let { notes ->
        serializer.textElement("desc", notes)
    }
    serializer.startTag(null, "trkseg")
    routePoints.forEach { point ->
        serializer.startTag(null, "trkpt")
        serializer.attribute(null, "lat", point.latitude.toRouteCoordinate())
        serializer.attribute(null, "lon", point.longitude.toRouteCoordinate())
        point.altitudeMeters?.let { altitude ->
            serializer.textElement("ele", altitude.toRouteDecimal())
        }
        serializer.textElement("time", point.time.toString())
        serializer.endTag(null, "trkpt")
    }
    serializer.endTag(null, "trkseg")
    serializer.endTag(null, "trk")
    serializer.endTag(null, "gpx")
    serializer.endDocument()
    serializer.flush()
}

internal fun writeActivityRouteKmz(
    workout: ExerciseData,
    routePoints: List<ExerciseRoutePoint>,
    output: OutputStream,
) {
    val kmlOutput = ByteArrayOutputStream()
    writeActivityRouteKml(
        workout = workout,
        routePoints = routePoints,
        output = kmlOutput,
    )

    val zip = ZipOutputStream(output)
    zip.putNextEntry(ZipEntry(KmzDocumentFileName))
    zip.write(kmlOutput.toByteArray())
    zip.closeEntry()
    zip.finish()
}

private fun writeActivityRouteKml(
    workout: ExerciseData,
    routePoints: List<ExerciseRoutePoint>,
    output: OutputStream,
) {
    val title = workout.title?.takeIf { it.isNotBlank() }
    val notes = workout.notes?.takeIf { it.isNotBlank() }
    val kml = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?>""")
        append('\n')
        append("""<kml xmlns="$KmlNamespace" xmlns:gx="$KmlGxNamespace">""")
        append('\n')
        append("  <Document>\n")
        title?.let { append("    <name>${it.routeXmlEscaped()}</name>\n") }
        append("    <Placemark>\n")
        title?.let { append("      <name>${it.routeXmlEscaped()}</name>\n") }
        notes?.let { append("      <description>${it.routeXmlEscaped()}</description>\n") }
        append("      <gx:Track>\n")
        routePoints.forEach { point ->
            append("        <when>${point.time}</when>\n")
            append(
                "        <gx:coord>${point.longitude.toRouteCoordinate()} " +
                    "${point.latitude.toRouteCoordinate()} " +
                    "${(point.altitudeMeters ?: 0.0).toRouteDecimal()}</gx:coord>\n",
            )
        }
        append("      </gx:Track>\n")
        append("    </Placemark>\n")
        append("  </Document>\n")
        append("</kml>\n")
    }
    output.write(kml.toByteArray(Charsets.UTF_8))
}

private fun XmlSerializer.textElement(name: String, value: String) {
    startTag(null, name)
    text(value)
    endTag(null, name)
}

private fun File.deleteOldRouteExports() {
    val cutoffMillis = System.currentTimeMillis() - RouteExportRetentionMillis
    listFiles()
        ?.filter { file -> file.isFile && file.lastModified() < cutoffMillis }
        ?.forEach { file -> runCatching { file.delete() } }
}

internal fun ExerciseData.routeExportFileName(format: ActivityRouteExportFormat): String {
    val titlePart = title
        ?.takeIf { it.isNotBlank() }
        ?.sanitizeRouteFileName()
        ?: "activity-route"
    val timePart = startTime
        .atZone(ZoneId.systemDefault())
        .format(RouteExportTimeFormatter)
    return "$titlePart-$timePart.${format.extension}"
}

private fun ExerciseData.sortedRoutePointsForExport(): List<ExerciseRoutePoint> {
    val routePoints = route.points.sortedBy { it.time }
    require(routePoints.isNotEmpty()) { "Activity has no route points to export." }
    return routePoints
}

private fun String.sanitizeRouteFileName(): String =
    trim()
        .lowercase(Locale.US)
        .replace(UnsafeRouteFileNameChars, "-")
        .trim('-')
        .take(MaxRouteFileNamePrefixLength)
        .ifBlank { "activity-route" }

private fun Double.toRouteCoordinate(): String =
    "%.7f".format(Locale.US, this)

private fun Double.toRouteDecimal(): String =
    "%.2f".format(Locale.US, this)

private fun String.routeXmlEscaped(): String =
    buildString(length) {
        this@routeXmlEscaped.forEach { char ->
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> char
                }
            )
        }
    }

private const val GpxMimeType = "application/gpx+xml"
private const val KmzMimeType = "application/vnd.google-earth.kmz"
private const val GpxNamespace = "http://www.topografix.com/GPX/1/1"
private const val KmlNamespace = "http://www.opengis.net/kml/2.2"
private const val KmlGxNamespace = "http://www.google.com/kml/ext/2.2"
private const val KmzDocumentFileName = "doc.kml"
private const val RouteExportCacheDirectory = "route_exports"
private const val RouteExportRetentionMillis = 24 * 60 * 60 * 1000L
private const val MaxRouteFileNamePrefixLength = 48
private val RouteExportTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
private val UnsafeRouteFileNameChars = Regex("[^a-z0-9._-]+")
