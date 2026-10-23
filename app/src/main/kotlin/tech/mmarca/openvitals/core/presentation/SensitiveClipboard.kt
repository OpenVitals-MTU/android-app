package tech.mmarca.openvitals.core.presentation

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry

/**
 * A plain-text clip marked sensitive, so Android 13 and later keep it out of the
 * copy preview and the keyboard's suggestions. Every report the app copies goes
 * through here: even a masked report says what the user imported or synced.
 */
fun sensitiveTextClip(label: CharSequence, text: CharSequence): ClipData =
    ClipData.newPlainText(label, text).apply {
        description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                // The key Android 13 reads; older versions ignore it.
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
    }

suspend fun Clipboard.setSensitiveText(label: String, text: String) =
    setClipEntry(sensitiveTextClip(label, text).toClipEntry())

fun ClipboardManager.setSensitiveText(label: CharSequence, text: CharSequence) =
    setPrimaryClip(sensitiveTextClip(label, text))
