package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private val audioFocus = SpeechAudioFocus(context)
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
            textToSpeech?.speakDucking(audioFocus, text, TextToSpeech.QUEUE_ADD, "openvitals_activity_${System.nanoTime()}")
        }
    }

    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
        audioFocus.release()
    }

    private fun ensureTextToSpeech() {
        if (textToSpeech != null) return
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            textToSpeech?.language = Locale.getDefault()
        }
    }
}

/**
 * Holds transient audio focus while speech plays, so music ducks under the
 * announcement and comes back once the queue is empty.
 */
internal class SpeechAudioFocus(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var request: AudioFocusRequest? = null
    private val pending = mutableSetOf<String>()

    /**
     * Speech attributes: routed like navigation prompts, so headphones hear it over music.
     * Built on first use: the framework builder is a stub on the JVM, and a
     * controller is constructed in unit tests that never speak.
     */
    val attributes: AudioAttributes by lazy {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
    }

    /** Engine callbacks arrive on a binder thread; each one drops its utterance from [pending]. */
    val listener: UtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) = onUtteranceFinished(utteranceId)
        override fun onStop(utteranceId: String?, interrupted: Boolean) = onUtteranceFinished(utteranceId)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = onUtteranceFinished(utteranceId)
        override fun onError(utteranceId: String?, errorCode: Int) = onUtteranceFinished(utteranceId)
    }

    @Synchronized
    fun onUtteranceQueued(utteranceId: String) {
        pending += utteranceId
        if (request != null) return
        val manager = audioManager ?: return
        val built = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setWillPauseWhenDucked(false)
            .build()
        val granted = runCatching { manager.requestAudioFocus(built) }
            .getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        if (granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) request = built
    }

    @Synchronized
    fun onUtteranceFinished(utteranceId: String?) {
        pending -= utteranceId ?: return
        if (pending.isEmpty()) release()
    }

    @Synchronized
    fun release() {
        pending.clear()
        request?.let { runCatching { audioManager?.abandonAudioFocusRequest(it) } }
        request = null
    }
}

/** Speaks with [focus] held until the engine reports the utterance done, stopped or failed. */
internal fun TextToSpeech.speakDucking(
    focus: SpeechAudioFocus,
    text: String,
    queueMode: Int,
    utteranceId: String,
) {
    runCatching { setAudioAttributes(focus.attributes) }
    setOnUtteranceProgressListener(focus.listener)
    focus.onUtteranceQueued(utteranceId)
    if (speak(text, queueMode, null, utteranceId) != TextToSpeech.SUCCESS) focus.onUtteranceFinished(utteranceId)
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
