package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



internal val RouteImportMimeTypes = arrayOf(
    "application/gpx",
    "application/gpx+xml",
    "application/vnd.google-earth.kml+xml",
    "application/vnd.google-earth.kmz",
    "application/vnd.google-earth.kmz+xml",
    "application/xml",
    "text/xml",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)

internal val FitImportMimeTypes = arrayOf(
    "application/vnd.ant.fit",
    "application/vnd.garmin.fit",
    "application/fit",
    "application/x-fit",
    "application/octet-stream",
    "*/*",
)
