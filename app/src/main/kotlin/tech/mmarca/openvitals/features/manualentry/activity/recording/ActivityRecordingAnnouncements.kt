package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import android.speech.tts.TextToSpeech
import java.time.Instant
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

internal class ActivityRecordingVoiceAnnouncer(
    private val context: Context,
    private val unitFormatter: UnitFormatter,
) {
    private val tracker = ActivityRecordingAnnouncementTracker()
    private var textToSpeech: TextToSpeech? = null
    private var ready = false

    fun onRecordingState(
        state: ActivityRecordingState,
        preferences: ActivityRecordingPreferences,
        now: Instant = Instant.now(),
    ) {
        if (!preferences.voiceAnnouncementsEnabled || state.recordingKind != ActivityRecordingKind.GPS_ROUTE) return
        ensureTextToSpeech()
        val text = tracker.announcementFor(state, preferences, now, context, unitFormatter) ?: return
        if (ready) {
            textToSpeech?.speak(
                text,
                TextToSpeech.QUEUE_ADD,
                null,
                "openvitals_activity_${System.nanoTime()}",
            )
        }
    }

    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
    }

    private fun ensureTextToSpeech() {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            textToSpeech?.language = Locale.getDefault()
        }
    }
}

internal class ActivityRecordingAnnouncementTracker {
    private var lastTimeBucket = 0L
    private var lastDistanceBucket = 0L
    private var lastLapCount = 0
    private var wasIdle = false

    fun announcementFor(
        state: ActivityRecordingState,
        preferences: ActivityRecordingPreferences,
        now: Instant,
        context: Context,
        unitFormatter: UnitFormatter,
    ): String? {
        if (state.status != ActivityRecordingStatus.RECORDING) return null

        if (preferences.voiceLapAnnouncementsEnabled && state.manualLaps.size > lastLapCount) {
            lastLapCount = state.manualLaps.size
            return context.getString(
                R.string.activity_recording_voice_lap,
                lastLapCount,
                state.summaryAnnouncement(now, context, unitFormatter),
            )
        }

        val idle = state.isAutoIdle(now)
        if (preferences.voiceIdleAnnouncementsEnabled && idle && !wasIdle) {
            wasIdle = true
            return context.getString(R.string.activity_recording_voice_idle)
        }
        if (preferences.voiceIdleAnnouncementsEnabled && wasIdle && !idle) {
            wasIdle = false
            return context.getString(R.string.activity_recording_voice_resumed)
        }

        preferences.voiceAnnouncementTimeIntervalMinutes?.let { minutes ->
            val intervalMillis = minutes * 60_000L
            if (intervalMillis > 0L) {
                val bucket = state.elapsedDuration(now).toMillis() / intervalMillis
                if (bucket > lastTimeBucket) {
                    lastTimeBucket = bucket
                    return state.summaryAnnouncement(now, context, unitFormatter)
                }
            }
        }

        preferences.voiceAnnouncementDistanceIntervalMeters?.let { meters ->
            if (meters > 0) {
                val bucket = (state.distanceMeters / meters.toDouble()).toLong()
                if (bucket > lastDistanceBucket) {
                    lastDistanceBucket = bucket
                    return state.summaryAnnouncement(now, context, unitFormatter)
                }
            }
        }

        return null
    }

    fun reset() {
        lastTimeBucket = 0L
        lastDistanceBucket = 0L
        lastLapCount = 0
        wasIdle = false
    }
}

private fun ActivityRecordingState.summaryAnnouncement(
    now: Instant,
    context: Context,
    unitFormatter: UnitFormatter,
): String {
    val elapsed = formatRecordingElapsed(elapsedDuration(now))
    val distance = unitFormatter.distance(distanceMeters).text
    val averageSpeed = unitFormatter.averageSpeed(distanceMeters, movingDuration(now).toMillis()).text
    val lap = manualLaps.size + 1
    return context.getString(
        R.string.activity_recording_voice_summary,
        elapsed,
        distance,
        averageSpeed,
        lap,
    )
}
