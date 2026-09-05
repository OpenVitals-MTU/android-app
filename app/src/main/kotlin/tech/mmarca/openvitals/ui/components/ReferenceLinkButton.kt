package tech.mmarca.openvitals.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/** Full-width outlined button opening [url] in the browser. A missing handler is swallowed. */
@Composable
fun ReferenceLinkButton(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    OpenVitalsOutlinedButton(
        onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Outlined.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
