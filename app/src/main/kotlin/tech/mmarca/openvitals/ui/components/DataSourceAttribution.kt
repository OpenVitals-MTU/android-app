package tech.mmarca.openvitals.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.HealthConnectSourceResolver
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

private const val DataSourceLabelMaxCharacters = 24
private const val DataSourceLabelOverflow = "..."
private val DataSourceLabelMaxWidth = 168.dp

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
    val label = remember(source.label) {
        truncatedDataSourceLabel(source.label)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Row(
                modifier = Modifier.widthIn(max = DataSourceLabelMaxWidth),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showIcon && iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

fun LazyListScope.dataSourceEducationItem(
    onManageDataSources: () -> Unit,
) {
    item {
        DataSourceEducationLink(
            onManageDataSources = onManageDataSources,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun DataSourceEducationItem(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    DataSourceEducationLink(
        onManageDataSources = { openHealthConnectPermissionSettings(context) },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

fun LazyListScope.dataSourceEducationItem() {
    item {
        DataSourceEducationItem()
    }
}

internal fun truncatedDataSourceLabel(label: String): String {
    val trimmedLabel = label.trim()
    if (trimmedLabel.length <= DataSourceLabelMaxCharacters) {
        return trimmedLabel
    }

    return trimmedLabel
        .take(DataSourceLabelMaxCharacters - DataSourceLabelOverflow.length)
        .trimEnd() + DataSourceLabelOverflow
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
