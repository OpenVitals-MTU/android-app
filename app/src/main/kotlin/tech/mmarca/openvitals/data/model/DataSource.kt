package tech.mmarca.openvitals.data.model

data class DataSource(
    val packageName: String,
    val deviceManufacturer: String?,
    val deviceModel: String?,
) {
    val displayName: String
        get() = when {
            packageName.contains("samsung") -> "Samsung Health"
            packageName.contains("fitbit") -> "Fitbit"
            packageName.contains("opentracks") -> "OpenTracks"
            packageName.contains("strava") -> "Strava"
            packageName.contains("garmin") -> "Garmin Connect"
            packageName.contains("polar") -> "Polar Flow"
            packageName.contains("google.android.apps.fitness") -> "Google Fit"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
}
