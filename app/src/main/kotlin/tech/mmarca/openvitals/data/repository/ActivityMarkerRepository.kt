package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarkerType

@Singleton
class ActivityMarkerRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun markersForActivity(activityId: String): List<ActivityRecordingMarker> =
        preferences.getString(activityId.key(), null)
            .orEmpty()
            .decodeMarkers()
            .sortedBy { it.time }

    fun setMarkersForActivity(activityId: String, markers: List<ActivityRecordingMarker>) {
        preferences.edit().apply {
            if (activityId.isBlank() || markers.isEmpty()) {
                remove(activityId.key())
            } else {
                putString(activityId.key(), markers.encodeMarkers())
            }
        }.apply()
    }

    fun deleteMarkersForActivity(activityId: String) {
        if (activityId.isBlank()) return
        preferences.edit().remove(activityId.key()).apply()
    }

    private fun String.key(): String = "activity_markers_$this"

    private fun List<ActivityRecordingMarker>.encodeMarkers(): String =
        joinToString(separator = "\n") { marker ->
            listOf(
                marker.id,
                marker.time.toEpochMilli().toString(),
                marker.latitude.toString(),
                marker.longitude.toString(),
                marker.altitudeMeters?.toString().orEmpty(),
                marker.name.encodeCompactText(),
                marker.note.encodeCompactText(),
                marker.type.encodeCompactText(),
            ).joinToString(separator = ",")
        }

    private fun String.decodeMarkers(): List<ActivityRecordingMarker> =
        lineSequence()
            .mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size < 8) return@mapNotNull null
                ActivityRecordingMarker(
                    id = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    time = parts[1].toLongOrNull()?.let(Instant::ofEpochMilli) ?: return@mapNotNull null,
                    latitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null,
                    longitude = parts[3].toDoubleOrNull() ?: return@mapNotNull null,
                    altitudeMeters = parts[4].toDoubleOrNull(),
                    name = parts[5].decodeCompactText().ifBlank { "Marker" },
                    note = parts[6].decodeCompactText(),
                    type = parts[7].decodeCompactText().ifBlank { ActivityRecordingMarkerType.Generic.value },
                )
            }
            .toList()

    private fun String.encodeCompactText(): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(toByteArray(StandardCharsets.UTF_8))

    private fun String.decodeCompactText(): String =
        runCatching {
            String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)
        }.getOrDefault("")

    private companion object {
        const val PreferencesName = "activity_marker_metadata"
    }
}
