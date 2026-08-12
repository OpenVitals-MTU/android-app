package tech.mmarca.openvitals.devices.garmin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R

/**
 * Rings the phone when the watch asks (GFDI find-my-phone, 5039/5040) — the
 * other half of the find pairing whose watch-ward direction already exists in
 * [GarminFindMyWatch].
 *
 * Plays the default alarm sound on the ALARM stream, looping, with a
 * high-priority notification carrying the stop action. Stops on: the watch's
 * cancel, the notification's stop button, or the watch-supplied duration
 * running out — whichever comes first. The alarm stream is deliberate: a phone
 * being looked for is face-down on silent, and the media stream would be
 * muted with it.
 */
@Singleton
class GarminFindPhoneRinger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        const val CHANNEL_ID = "garmin_find_phone"
        const val NOTIFICATION_ID = 41013
        const val DEFAULT_DURATION_SECONDS = 60
        /** The watch's own ceiling; a runaway duration must not ring forever. */
        const val MAX_DURATION_SECONDS = 120
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var stopJob: Job? = null

    /** Starts ringing. A second ask while ringing just restarts the clock. */
    fun start(durationSeconds: Int) {
        scope.launch {
            val duration = durationSeconds
                .let { if (it <= 0) DEFAULT_DURATION_SECONDS else it }
                .coerceAtMost(MAX_DURATION_SECONDS)
            stopJob?.cancel()
            if (player == null) {
                player = runCatching { buildPlayer() }.getOrNull()
                player?.start()
                postNotification()
            }
            stopJob = scope.launch {
                delay(duration * 1000L)
                stopInternal()
            }
        }
    }

    fun stop() {
        scope.launch { stopInternal() }
    }

    private fun stopInternal() {
        stopJob?.cancel()
        stopJob = null
        player?.let { current ->
            runCatching { current.stop() }
            runCatching { current.release() }
        }
        player = null
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICATION_ID)
    }

    private fun buildPlayer(): MediaPlayer {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        return MediaPlayer().apply {
            setDataSource(context, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            isLooping = true
            prepare()
        }
    }

    private fun postNotification() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.garmin_find_phone_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // The MediaPlayer is the sound; a channel sound on top would
                // double up.
                setSound(null, null)
            },
        )
        val stopIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GarminFindPhoneStopReceiver::class.java)
                .setAction(GarminFindPhoneStopReceiver.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_activity_recording)
            .setContentTitle(context.getString(R.string.garmin_find_phone_title))
            .setContentText(context.getString(R.string.garmin_find_phone_body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.garmin_find_phone_stop), stopIntent)
            // Tapping anywhere on it stops the noise — nobody hunting for a
            // ringing phone wants to aim for a small button.
            .setContentIntent(stopIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

/** The notification's stop button. */
@AndroidEntryPoint
class GarminFindPhoneStopReceiver : BroadcastReceiver() {

    @Inject
    lateinit var ringer: GarminFindPhoneRinger

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOP) return
        ringer.stop()
    }

    companion object {
        const val ACTION_STOP = "tech.mmarca.openvitals.GARMIN_FIND_PHONE_STOP"
    }
}
