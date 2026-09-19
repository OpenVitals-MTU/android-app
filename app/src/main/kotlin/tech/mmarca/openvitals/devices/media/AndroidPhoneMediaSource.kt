package tech.mmarca.openvitals.devices.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.devices.notifications.OpenVitalsNotificationListenerService

/**
 * The real source. Android hands media sessions only to an app with
 * notification access, named by its listener service. Callbacks arrive on
 * the main thread; they only read fields.
 */
@Singleton
class AndroidPhoneMediaSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhoneMediaSource {

    // Lazy: framework lookups in a constructor break the JVM tests.
    private val sessions by lazy { context.getSystemService(MediaSessionManager::class.java) }
    private val audio by lazy { context.getSystemService(AudioManager::class.java) }
    private val listenerComponent by lazy {
        ComponentName(context, OpenVitalsNotificationListenerService::class.java)
    }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private var onChanged: ((PhoneMediaState?) -> Unit)? = null
    private var controller: MediaController? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> follow(controllers.orEmpty()) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = report()
        override fun onPlaybackStateChanged(state: PlaybackState?) = report()
        override fun onSessionDestroyed() = follow(emptyList())
    }

    override fun hasAccess(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    @Synchronized
    override fun start(onChanged: (PhoneMediaState?) -> Unit): Boolean {
        if (this.onChanged != null) return true
        return try {
            sessions.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
            this.onChanged = onChanged
            follow(sessions.getActiveSessions(listenerComponent))
            true
        } catch (error: SecurityException) {
            // Notification access is off, or was just revoked.
            GarminLog.log("[MEDIA] no access to media sessions: $error")
            false
        }
    }

    @Synchronized
    override fun stop() {
        if (onChanged == null) return
        runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        onChanged = null
    }

    @Synchronized
    override fun current(): PhoneMediaState? {
        val current = controller ?: return null
        val metadata = current.metadata
        val playback = current.playbackState
        val playing = playback?.state == PlaybackState.STATE_PLAYING
        val rate = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        return PhoneMediaState(
            playerName = playerName(current.packageName),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            durationSeconds = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ?.takeIf { it > 0 }?.let { (it / 1000).toInt() },
            playing = playing,
            playbackRate = rate,
            positionSeconds = playback?.let { positionNow(it, playing, rate) } ?: 0,
            volume = volume(),
        )
    }

    override fun perform(command: PhoneMediaCommand) {
        when (command) {
            PhoneMediaCommand.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            PhoneMediaCommand.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            else -> transport(command)
        }
    }

    /** The first session is the one Android ranks foremost: the playing one, when any plays. */
    @Synchronized
    private fun follow(controllers: List<MediaController>) {
        val next = controllers.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = next
            next?.registerCallback(controllerCallback, handler)
        }
        report()
    }

    private fun report() {
        onChanged?.invoke(current())
    }

    private fun transport(command: PhoneMediaCommand) {
        val controls = synchronized(this) { controller }?.transportControls
        if (controls == null) {
            // No session: a media key wakes the last player, as a headset button does.
            mediaKey(command)?.let(::pressMediaKey)
            return
        }
        when (command) {
            PhoneMediaCommand.TOGGLE_PLAY_PAUSE ->
                if (current()?.playing == true) controls.pause() else controls.play()
            PhoneMediaCommand.PLAY -> controls.play()
            PhoneMediaCommand.PAUSE -> controls.pause()
            PhoneMediaCommand.NEXT -> controls.skipToNext()
            PhoneMediaCommand.PREVIOUS -> controls.skipToPrevious()
            PhoneMediaCommand.FAST_FORWARD -> controls.fastForward()
            PhoneMediaCommand.REWIND -> controls.rewind()
            PhoneMediaCommand.VOLUME_UP, PhoneMediaCommand.VOLUME_DOWN -> Unit
        }
    }

    private fun adjustVolume(direction: Int) {
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        // No callback follows a volume change, so the wrist is told here.
        report()
    }

    private fun pressMediaKey(keyCode: Int) {
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun mediaKey(command: PhoneMediaCommand): Int? = when (command) {
        PhoneMediaCommand.TOGGLE_PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        PhoneMediaCommand.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
        PhoneMediaCommand.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
        PhoneMediaCommand.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
        PhoneMediaCommand.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        PhoneMediaCommand.FAST_FORWARD -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
        PhoneMediaCommand.REWIND -> KeyEvent.KEYCODE_MEDIA_REWIND
        PhoneMediaCommand.VOLUME_UP, PhoneMediaCommand.VOLUME_DOWN -> null
    }

    /** A player reports its position once and lets the clock run, so the clock is added back. */
    private fun positionNow(playback: PlaybackState, playing: Boolean, rate: Float): Int {
        val reported = playback.position.coerceAtLeast(0)
        if (!playing || playback.lastPositionUpdateTime <= 0) return (reported / 1000).toInt()
        val elapsed = SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime
        return ((reported + (elapsed * rate).toLong()).coerceAtLeast(0) / 1000).toInt()
    }

    private fun volume(): Float? {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return null
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    /** Null for an app Android hides from this one: the manifest queries name launchable apps only. */
    private fun playerName(packageName: String): String? = runCatching {
        val manager = context.packageManager
        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface PhoneMediaModule {
    @Binds
    @Singleton
    fun bindPhoneMediaSource(implementation: AndroidPhoneMediaSource): PhoneMediaSource
}
