package tech.mmarca.openvitals.navigation

import android.net.Uri

data class ExternalRouteImportRequest(
    val id: Long,
    val uri: Uri,
)
