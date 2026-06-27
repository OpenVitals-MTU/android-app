package tech.mmarca.openvitals.domain.preferences

data class ActivityRecordingPreferences(
    val autoIdleEnabled: Boolean = DefaultAutoIdleEnabled,
    val autoIdleTimeoutSeconds: Int = DefaultAutoIdleTimeoutSeconds,
    val keepScreenOnDuringRecording: Boolean = DefaultKeepScreenOnDuringRecording,
    val requiredGpsAccuracyMeters: Int = DefaultRequiredGpsAccuracyMeters,
    val routeGapMeters: Int? = DefaultRouteGapMeters,
    val barometerClimbEnabled: Boolean = DefaultBarometerClimbEnabled,
    val recordingDistanceIntervalMeters: Int? = DefaultRecordingDistanceIntervalMeters,
    val recordingTimeIntervalMillis: Int = DefaultRecordingTimeIntervalMillis,
    val voiceAnnouncementsEnabled: Boolean = DefaultVoiceAnnouncementsEnabled,
    val voiceAnnouncementTimeIntervalMinutes: Int? = DefaultVoiceAnnouncementTimeIntervalMinutes,
    val voiceAnnouncementDistanceIntervalMeters: Int? = DefaultVoiceAnnouncementDistanceIntervalMeters,
    val voiceIdleAnnouncementsEnabled: Boolean = DefaultVoiceIdleAnnouncementsEnabled,
    val voiceLapAnnouncementsEnabled: Boolean = DefaultVoiceLapAnnouncementsEnabled,
) {
    fun normalized(): ActivityRecordingPreferences =
        copy(
            autoIdleTimeoutSeconds = autoIdleTimeoutSeconds.coerceIn(
                MinAutoIdleTimeoutSeconds,
                MaxAutoIdleTimeoutSeconds,
            ),
            requiredGpsAccuracyMeters = requiredGpsAccuracyMeters.closestAllowed(AllowedGpsAccuracyMeters),
            routeGapMeters = routeGapMeters?.closestAllowed(AllowedRouteGapMeters),
            recordingDistanceIntervalMeters = recordingDistanceIntervalMeters
                ?.closestAllowed(AllowedRecordingDistanceIntervalMeters),
            recordingTimeIntervalMillis = recordingTimeIntervalMillis
                .closestAllowed(AllowedRecordingTimeIntervalMillis),
            voiceAnnouncementTimeIntervalMinutes = voiceAnnouncementTimeIntervalMinutes
                ?.closestAllowed(AllowedVoiceAnnouncementTimeIntervalMinutes),
            voiceAnnouncementDistanceIntervalMeters = voiceAnnouncementDistanceIntervalMeters
                ?.closestAllowed(AllowedVoiceAnnouncementDistanceIntervalMeters),
        )

    companion object {
        const val DefaultAutoIdleEnabled = true
        const val DefaultAutoIdleTimeoutSeconds = 10
        const val MinAutoIdleTimeoutSeconds = 5
        const val MaxAutoIdleTimeoutSeconds = 60
        const val DefaultKeepScreenOnDuringRecording = false
        const val DefaultRequiredGpsAccuracyMeters = 30
        const val DefaultBarometerClimbEnabled = true
        val DefaultRecordingDistanceIntervalMeters: Int? = null
        const val DefaultRecordingTimeIntervalMillis = 500
        const val DefaultVoiceAnnouncementsEnabled = false
        val DefaultVoiceAnnouncementTimeIntervalMinutes: Int? = 5
        val DefaultVoiceAnnouncementDistanceIntervalMeters: Int? = 1_000
        const val DefaultVoiceIdleAnnouncementsEnabled = true
        const val DefaultVoiceLapAnnouncementsEnabled = true
        val DefaultRouteGapMeters: Int? = 200
        val AllowedGpsAccuracyMeters = listOf(10, 30, 50, 100)
        val AllowedRouteGapMeters = listOf(100, 200, 500)
        val AllowedRecordingDistanceIntervalMeters = listOf(5, 10, 25, 50)
        val AllowedRecordingTimeIntervalMillis = listOf(500, 1_000, 5_000, 10_000)
        val AllowedVoiceAnnouncementTimeIntervalMinutes = listOf(1, 5, 10)
        val AllowedVoiceAnnouncementDistanceIntervalMeters = listOf(500, 1_000, 5_000)
    }
}

private fun Int.closestAllowed(allowedValues: List<Int>): Int =
    allowedValues.minBy { kotlin.math.abs(it - this) }
