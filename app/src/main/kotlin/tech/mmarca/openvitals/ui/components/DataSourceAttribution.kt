package tech.mmarca.openvitals.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.HealthConnectSourceResolver
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import java.util.concurrent.ConcurrentHashMap

private const val DataSourceLabelMaxCharacters = 24
private const val DataSourceLabelOverflow = "..."
private val DataSourceLabelMaxWidth = 168.dp

// The chip draws 16 dp; 64 px covers the densest screen.
private const val DataSourceIconPx = 64

internal class DataSourceChipContent(val label: String, val icon: ImageBitmap?)

/**
 * Chip content per package, held for the process. Every row used to ask the
 * PackageManager and redraw the app icon on the main thread, which froze long lists.
 */
internal object DataSourceChipCache {
    private val contents = ConcurrentHashMap<String, DataSourceChipContent>()

    fun get(packageName: String, resolve: (String) -> DataSourceChipContent): DataSourceChipContent =
        contents.getOrPut(packageName) { resolve(packageName) }
}

@Composable
fun DataSourceAttribution(
    packageName: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    /** True for a synced record showing its original source; the label gains "(synced)". */
    synced: Boolean = false,
) {
    val context = LocalContext.current.applicationContext
    val content = remember(packageName) {
        DataSourceChipCache.get(packageName) {
            val source = HealthConnectSourceResolver(context).resolve(it)
            DataSourceChipContent(
                label = truncatedDataSourceLabel(source.label),
                icon = source.icon?.toChipIcon(),
            )
        }
    }
    val iconPainter = remember(content) { content.icon?.let(::BitmapPainter) }
    val truncatedLabel = content.label
    val label = if (synced) {
        stringResource(R.string.data_source_synced_label, truncatedLabel)
    } else {
        truncatedLabel
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

private fun Drawable.toChipIcon(): ImageBitmap =
    Bitmap.createBitmap(DataSourceIconPx, DataSourceIconPx, Bitmap.Config.ARGB_8888).also { created ->
        setBounds(0, 0, DataSourceIconPx, DataSourceIconPx)
        draw(Canvas(created))
    }.asImageBitmap()
