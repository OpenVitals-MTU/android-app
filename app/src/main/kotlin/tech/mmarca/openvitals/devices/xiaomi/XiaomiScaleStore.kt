package tech.mmarca.openvitals.devices.xiaomi

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Why the last attempt to save a weigh-in to Health Connect did not go through. */
enum class ScaleWriteFailure { PERMISSION, SYNC_PAUSED, OTHER }

/** A weigh-in left out because the scale filed it under another user slot. No measurement is kept. */
data class IgnoredScaleProfile(val profile: Int, val atMillis: Long)

/**
 * What the app knows about the user's scale, without the bind key: that one
 * is read through [XiaomiScaleStore.bindKey] and never travels in state.
 */
data class XiaomiScaleConfig(
    private val hasKey: Boolean = false,
    /** The scale's Bluetooth address, from Android's companion dialog. */
    val address: String? = null,
    /** What the user calls the scale; the advertised name until renamed. */
    val name: String? = null,
    /** The scale's user slot this phone's owner weighs in under, learned from the first weigh-in. */
    val profile: Int? = null,
    /** The scale kept broadcasting but the key stopped opening it: it was paired again. */
    val keyRejected: Boolean = false,
    val writeFailure: ScaleWriteFailure? = null,
    val ignoredProfile: IgnoredScaleProfile? = null,
    /** The weigh-in the user deleted last. The scale may still be repeating it. */
    val deletedScaleTimestamp: Long? = null,
) {
    /** A scale was added: its address and key are known. */
    val isSetUp: Boolean
        get() = hasKey && address != null
}

/**
 * The scale's settings, kept out of the device registry: a scale takes no
 * connection, so it has no capabilities, no bond and no sync. One scale,
 * identified by the address Android's companion dialog returned.
 * SharedPreferences-backed and never backed up; the bind key stays on this
 * phone.
 */
class XiaomiScaleStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    private val _config = MutableStateFlow(read())
    val config: StateFlow<XiaomiScaleConfig> = _config.asStateFlow()

    /** The 16-byte key that opens the scale's broadcasts, or null before one is set. */
    fun bindKey(): ByteArray? = prefs.getString(KEY_BIND_KEY, null)?.let(::parseBindKey)

    /** Adds the scale. Whatever an earlier scale left behind goes with it. */
    fun setUp(address: String, name: String, key: ByteArray) {
        require(key.size == BIND_KEY_BYTES)
        prefs.edit {
            clear()
            putString(KEY_ADDRESS, address.uppercase())
            putString(KEY_NAME, name)
            putString(KEY_BIND_KEY, key.toHexString())
        }
        publish()
    }

    /** A new key for the same scale, after it was paired again in Xiaomi Home. The scale stays known. */
    fun changeKey(key: ByteArray) {
        require(key.size == BIND_KEY_BYTES)
        prefs.edit {
            putString(KEY_BIND_KEY, key.toHexString())
            remove(KEY_REJECTED)
        }
        publish()
    }

    fun rename(name: String) {
        prefs.edit { putString(KEY_NAME, name) }
        publish()
    }

    /** Moves to another of the scale's user slots, when it filed this person under a new one. */
    fun setProfile(profile: Int) {
        prefs.edit {
            putInt(KEY_PROFILE, profile)
            remove(KEY_IGNORED_PROFILE)
            remove(KEY_IGNORED_AT)
        }
        publish()
    }

    fun setKeyRejected(rejected: Boolean) {
        if (_config.value.keyRejected == rejected) return
        prefs.edit { putBoolean(KEY_REJECTED, rejected) }
        publish()
    }

    fun setWriteFailure(failure: ScaleWriteFailure?) {
        if (_config.value.writeFailure == failure) return
        prefs.edit {
            if (failure == null) remove(KEY_WRITE_FAILURE) else putString(KEY_WRITE_FAILURE, failure.name)
        }
        publish()
    }

    fun noteIgnoredProfile(profile: Int, atMillis: Long) {
        prefs.edit {
            putInt(KEY_IGNORED_PROFILE, profile)
            putLong(KEY_IGNORED_AT, atMillis)
        }
        publish()
    }

    fun setDeletedScaleTimestamp(scaleTimestamp: Long) {
        prefs.edit { putLong(KEY_DELETED_TIMESTAMP, scaleTimestamp) }
        publish()
    }

    /** Forgets the scale and its key. */
    fun clear() {
        prefs.edit { clear() }
        publish()
    }

    private fun publish() {
        _config.value = read()
    }

    private fun read(): XiaomiScaleConfig = XiaomiScaleConfig(
        hasKey = prefs.getString(KEY_BIND_KEY, null)?.let(::parseBindKey) != null,
        address = prefs.getString(KEY_ADDRESS, null),
        name = prefs.getString(KEY_NAME, null),
        profile = prefs.getInt(KEY_PROFILE, NONE).takeIf { it != NONE },
        keyRejected = prefs.getBoolean(KEY_REJECTED, false),
        writeFailure = prefs.getString(KEY_WRITE_FAILURE, null)
            ?.let { name -> ScaleWriteFailure.entries.firstOrNull { it.name == name } },
        ignoredProfile = prefs.getInt(KEY_IGNORED_PROFILE, NONE).takeIf { it != NONE }
            ?.let { IgnoredScaleProfile(it, prefs.getLong(KEY_IGNORED_AT, 0L)) },
        deletedScaleTimestamp = prefs.getLong(KEY_DELETED_TIMESTAMP, NONE.toLong()).takeIf { it != NONE.toLong() },
    )

    companion object {
        const val BIND_KEY_BYTES = 16

        private const val PREFS_FILE = "xiaomi_scale"
        private const val KEY_BIND_KEY = "bind_key"
        private const val KEY_ADDRESS = "address"
        private const val KEY_NAME = "name"
        private const val KEY_PROFILE = "profile"
        private const val KEY_REJECTED = "key_rejected"
        private const val KEY_WRITE_FAILURE = "write_failure"
        private const val KEY_IGNORED_PROFILE = "ignored_profile"
        private const val KEY_IGNORED_AT = "ignored_at"
        private const val KEY_DELETED_TIMESTAMP = "deleted_scale_timestamp"
        private const val NONE = -1

        private val BindKeyText = Regex("[0-9a-fA-F]{${BIND_KEY_BYTES * 2}}")
        private val Separators = Regex("[\\s:-]")

        /**
         * The key as the extraction tools print it: 32 hex digits, whatever
         * the case, with or without separators. Null for anything else.
         */
        fun parseBindKey(text: String): ByteArray? {
            val digits = text.replace(Separators, "")
            if (!BindKeyText.matches(digits)) return null
            return digits.hexToByteArray()
        }
    }
}
