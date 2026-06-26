package tech.mmarca.openvitals.core.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class HealthDataSourceUiModel(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@Singleton
class HealthConnectSourceResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolve(packageName: String): HealthDataSourceUiModel {
        val (label, icon) = resolveLabelAndIcon(packageName)
        return HealthDataSourceUiModel(
            packageName = packageName,
            label = label ?: fallbackLabel(packageName),
            icon = icon,
        )
    }

    private fun resolveLabelAndIcon(packageName: String): Pair<String?, Drawable?> =
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            label to icon
        } catch (_: PackageManager.NameNotFoundException) {
            null to null
        }

    private fun fallbackLabel(packageName: String): String = when {
        packageName.contains("samsung") -> "Samsung Health"
        packageName.contains("fitbit") -> "Fitbit"
        packageName.contains("opentracks") -> "OpenTracks"
        packageName.contains("strava") -> "Strava"
        packageName.contains("polar") -> "Polar"
        packageName.contains("google.android.apps.fitness") -> "Google Fit"
        packageName.contains("garmin") -> "Garmin"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}
