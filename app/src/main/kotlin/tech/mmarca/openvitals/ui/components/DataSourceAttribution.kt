package tech.mmarca.openvitals.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.HealthConnectSourceResolver

@Composable
fun DataSourceAttribution(
    packageName: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
) {
    val context = LocalContext.current
    val source = remember(packageName) {
        HealthConnectSourceResolver(context).resolve(packageName)
    }
    val iconPainter = remember(source.icon) {
        source.icon?.toPainter()
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showIcon && iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
fun DataSourceEducationLink(
    onManageDataSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onManageDataSources,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.health_connect_data_source_manage))
    }
}

private fun Drawable.toPainter(): Painter {
    val bitmap = when (this) {
        is BitmapDrawable -> bitmap
        else -> {
            val width = intrinsicWidth.coerceAtLeast(1)
            val height = intrinsicHeight.coerceAtLeast(1)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { created ->
                setBounds(0, 0, width, height)
                draw(Canvas(created))
            }
        }
    }
    return BitmapPainter(bitmap.asImageBitmap())
}
