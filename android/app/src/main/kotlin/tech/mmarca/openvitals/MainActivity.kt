package tech.mmarca.openvitals

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.location.altitude.AltitudeConverter
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The `health` plugin requires the host activity to be a
 * [FlutterFragmentActivity] (a `ComponentActivity`) so that Health Connect
 * permission requests can use `registerForActivityResult`. Extending
 * `FlutterActivity` would make Health Connect permission launches fail at
 * runtime.
 *
 * Also hosts the activity-recording sensor bridge: `sensors_plus` exposes
 * neither the proximity sensor nor the step detector, so the recording
 * controller reads them through the channels registered here (port of the
 * Kotlin `ActivityRecordingService` sensor listener), plus the WGS84→MSL
 * altitude conversion the Kotlin controller performs with [AltitudeConverter].
 */
class MainActivity : FlutterFragmentActivity() {
    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * `AltitudeConverter.addMslAltitudeToLocation` may read its geoid grid from
     * disk, so conversions run off the platform thread (the Kotlin controller
     * hops to a single-thread dispatcher for the same reason).
     */
    private val altitudeExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Kotlin `ActivityRecordingController.altitudeConverter` (API 34+ only). */
    private val altitudeConverter: AltitudeConverter? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            AltitudeConverter()
        } else {
            null
        }
    }

    private var pendingPermissionResult: MethodChannel.Result? = null

    /** The route file the app was opened with, until Dart takes it. */
    private var pendingRouteImportUri: Uri? = null

    /**
     * Scanning a folder walks the document tree with one `ContentResolver` query
     * per directory. A Garmin card with a thousand rides would ANR on the
     * platform thread, so the walk (and every file read it later serves) runs
     * here and answers on the main handler.
     */
    private val importExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var pendingFolderResult: MethodChannel.Result? = null

    /** Extensions the in-flight folder pick is scanning for, e.g. `[".fit"]`. */
    private var pendingFolderExtensions: List<String> = emptyList()

    /**
     * The folder picker.
     *
     * Registered as a field: `registerForActivityResult` must be called before
     * the activity is STARTED, which a field initializer satisfies and a lazy
     * call from the channel handler would not.
     *
     * `OpenDocumentTree` hands back a **tree URI**, and that URI — not a
     * filesystem path — is the thing that grants access. `file_picker` offers a
     * folder pick too, but it converts the tree URI into a `/storage/emulated/0`
     * path and throws the URI away; under scoped storage the app cannot open a
     * non-media file at a raw path like that, so those paths read as
     * `FileNotFoundException` for exactly the `.fit` files we want. Keeping the
     * URI and walking it with [DocumentsContract] is what makes this work without
     * asking for All-files access, a permission a health app has no business
     * holding.
     */
    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            val result = pendingFolderResult
            val extensions = pendingFolderExtensions
            pendingFolderResult = null
            pendingFolderExtensions = emptyList()
            when {
                result == null -> Unit
                // The user backed out. Null, not an error: cancelling is a normal
                // thing to do and the card must go quiet, not red.
                treeUri == null -> result.success(null)
                else -> scanFolder(treeUri, extensions, result)
            }
        }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        val messenger = flutterEngine.dartExecutor.binaryMessenger
        MethodChannel(messenger, METHOD_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "hasSensor" -> {
                    val type = sensorTypeFor(call.argument<String>("type"))
                    result.success(type != null && sensorManager.getDefaultSensor(type) != null)
                }
                "hasActivityRecognitionPermission" ->
                    result.success(hasActivityRecognitionPermission())
                "requestActivityRecognitionPermission" ->
                    requestActivityRecognitionPermission(result)
                "convertToMsl" -> {
                    val latitude = call.argument<Double>("latitude")
                    val longitude = call.argument<Double>("longitude")
                    val altitudeMeters = call.argument<Double>("altitudeMeters")
                    if (latitude == null || longitude == null || altitudeMeters == null) {
                        result.success(null)
                    } else {
                        convertToMsl(latitude, longitude, altitudeMeters, result)
                    }
                }
                else -> result.notImplemented()
            }
        }
        // Debug-diagnostics bridge: reads the current process logcat so the
        // Dart `DebugLogSanitizer` (port of `PrivacySafeDebugLogExporter`) can
        // privacy-sanitize it. `-v tag` yields the `LEVEL/Tag: message` format
        // the sanitizer regex expects. Privacy handling stays entirely in Dart.
        MethodChannel(messenger, DIAGNOSTICS_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "readCurrentProcessLogcat" -> result.success(readCurrentProcessLogcat())
                else -> result.notImplemented()
            }
        }
        // Kotlin registers one SensorEventListener at SENSOR_DELAY_NORMAL per
        // recording; here each event channel owns its listener, registered only
        // while the Dart side is subscribed.
        EventChannel(messenger, PROXIMITY_CHANNEL).setStreamHandler(
            SensorStreamHandler(sensorManager, Sensor.TYPE_PROXIMITY) { event ->
                val value = event.values.firstOrNull() ?: return@SensorStreamHandler null
                mapOf(
                    "value" to value.toDouble(),
                    "timestampMillis" to System.currentTimeMillis(),
                )
            },
        )
        EventChannel(messenger, STEP_DETECTOR_CHANNEL).setStreamHandler(
            SensorStreamHandler(sensorManager, Sensor.TYPE_STEP_DETECTOR) {
                System.currentTimeMillis()
            },
        )
        // "Open with" / "Share" of a .gpx/.kml/.kmz/.fit/.tcx. Port of the Kotlin
        // `MainActivity.updateRouteImportRequest` + `ExternalRouteImportRequest`:
        // resolve the URI, read the bytes, and park them until Dart takes them
        // (exactly once). Dart drains this on launch and on every resume, which
        // covers both a cold start and an `onNewIntent` on a running app.
        MethodChannel(messenger, ROUTE_IMPORT_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "takePendingRouteImport" -> {
                    val uri = pendingRouteImportUri
                    pendingRouteImportUri = null
                    result.success(uri?.let(::readRouteImport))
                }
                else -> result.notImplemented()
            }
        }
        // Reads the *Kotlin* app's local data, which an in-place Play update
        // leaves behind under the same `/data/data/tech.mmarca.openvitals/`
        // (same applicationId, same certificate). Dart migrates it exactly once
        // — see `lib/data/migration/kotlin_data_migration.dart`.
        //
        // This bridge is deliberately READ-ONLY. It never writes the Flutter
        // preferences file: `shared_preferences_android` encodes a Dart `double`
        // and a `List<String>` as *prefixed strings*, an internal encoding that
        // has already changed between plugin versions. So native hands Dart plain
        // typed values and Dart writes them back through the `SharedPreferences`
        // API, letting the plugin own its own encoding.
        MethodChannel(messenger, LEGACY_MIGRATION_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "hasLegacyData" -> result.success(hasLegacyData())
                "readLegacyPrefs" ->
                    result.success(readLegacyPrefs(call.argument<String>("name")))
                "legacyDatabasePath" ->
                    result.success(getDatabasePath(LEGACY_DATABASE_NAME).absolutePath)
                "legacyFilesDir" -> result.success(filesDir.absolutePath)
                else -> result.notImplemented()
            }
        }
        // Folder import: pick a directory once, then import every activity file
        // inside it. `pickFolder` returns the file LIST (names + document URIs)
        // and never the bytes; Dart calls `readFile` per file, when it reaches
        // it. That is deliberate — a folder can hold hundreds of megabytes, and
        // neither the channel nor the heap should ever carry more than one file.
        MethodChannel(messenger, FOLDER_IMPORT_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "pickFolder" -> {
                    if (pendingFolderResult != null) {
                        // The picker is already up. Answering twice on one
                        // Result crashes the engine.
                        result.error("busy", "A folder pick is already in progress.", null)
                    } else {
                        pendingFolderResult = result
                        pendingFolderExtensions = call.argument<List<String>>("extensions")
                            ?.map { if (it.startsWith(".")) it.lowercase() else ".${it.lowercase()}" }
                            ?: ROUTE_IMPORT_EXTENSIONS
                        runCatching { pickFolder.launch(null) }.onFailure { error ->
                            pendingFolderResult = null
                            result.error("pick_failed", error.message, null)
                        }
                    }
                }
                "readFile" -> {
                    val uri = call.argument<String>("uri")
                    if (uri == null) {
                        result.error("bad_argument", "readFile needs a uri.", null)
                    } else {
                        readDocument(Uri.parse(uri), result)
                    }
                }
                else -> result.notImplemented()
            }
        }
        // Cold start: the launching intent is already set on the activity.
        capturePendingRouteImport(intent)
    }

    /**
     * Walks the picked tree and lists every file whose name ends in one of
     * [extensions], deepest folders included.
     *
     * Recursive because a watch export is not flat: Garmin buries its rides in
     * `GARMIN/ACTIVITY`, and a user who picks the card's root and is told it
     * holds no FIT files would be right to call that broken. The depth and count
     * caps are there so picking the root of a 64 GB card cannot walk forever;
     * hitting the file cap is reported as [truncated] rather than silently
     * dropping the tail, because an import that quietly skips half a folder is
     * worse than one that says it did.
     */
    private fun scanFolder(
        treeUri: Uri,
        extensions: List<String>,
        result: MethodChannel.Result,
    ) {
        importExecutor.execute {
            val files = mutableListOf<Map<String, Any?>>()
            var truncated = false
            try {
                // Breadth-first, so the folder the user actually picked is listed
                // before whatever is buried under it.
                val queue = ArrayDeque<Pair<String, Int>>()
                queue.add(DocumentsContract.getTreeDocumentId(treeUri) to 0)
                while (queue.isNotEmpty() && !truncated) {
                    val (documentId, depth) = queue.removeFirst()
                    val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                        treeUri,
                        documentId,
                    )
                    contentResolver.query(
                        children,
                        arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                        ),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val childId = cursor.getString(0) ?: continue
                            val name = cursor.getString(1) ?: continue
                            val isDirectory =
                                cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                            if (isDirectory) {
                                if (depth < MAX_FOLDER_DEPTH) queue.add(childId to depth + 1)
                                continue
                            }
                            if (extensions.none { name.endsWith(it, ignoreCase = true) }) continue
                            if (files.size >= MAX_FOLDER_FILES) {
                                truncated = true
                                return@use
                            }
                            files.add(
                                mapOf(
                                    "uri" to DocumentsContract
                                        .buildDocumentUriUsingTree(treeUri, childId)
                                        .toString(),
                                    "name" to name,
                                ),
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    result.error("scan_failed", error.message, null)
                }
                return@execute
            }
            // Name order: a watch names its files by timestamp, so this is the
            // order the rides were ridden in, and the import reads like a diary
            // rather than a shuffle.
            files.sortBy { it["name"] as String }
            mainHandler.post {
                result.success(mapOf("files" to files, "truncated" to truncated))
            }
        }
    }

    /**
     * Streams one document's bytes to Dart. One file at a time is the whole
     * contract: the Dart importer calls this when it reaches a file and drops the
     * bytes when it is done, so a thousand-file folder never costs more than the
     * biggest single file in it.
     */
    private fun readDocument(uri: Uri, result: MethodChannel.Result) {
        importExecutor.execute {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                mainHandler.post {
                    if (bytes == null) {
                        result.error("unreadable", "Could not open $uri.", null)
                    } else {
                        result.success(bytes)
                    }
                }
            } catch (error: Exception) {
                mainHandler.post {
                    result.error("unreadable", error.message, null)
                }
            }
        }
    }

    /**
     * Whether the Kotlin app ever ran here: its main preferences file is written
     * on first launch, so its presence is the migration's trigger.
     *
     * `getSharedPreferences(...).all.isEmpty()` would be wrong — asking for a
     * missing file *creates* an empty one, and the Flutter app writing its own
     * prefs must never be mistaken for legacy data. So the file is probed
     * directly on disk instead.
     */
    private fun hasLegacyData(): Boolean = legacyPrefsFile(LEGACY_PREFS_NAME).exists()

    private fun legacyPrefsFile(name: String): File =
        File(File(applicationInfo.dataDir, "shared_prefs"), "$name.xml")

    /**
     * Every entry of a legacy preferences file, as values the
     * `StandardMessageCodec` can carry.
     *
     * `Float` widens to `Double` **through its shortest decimal string**, not
     * through `toDouble()`: the Kotlin app wrote goals and weights with
     * `putFloat`, and `2.2f.toDouble()` is 2.200000047683716, which would surface
     * in a Dart text field. `2.2f.toString()` is the shortest decimal that
     * round-trips the float ("2.2"), so parsing it as a double reproduces the
     * number the user actually typed.
     *
     * `Set<String>` becomes a `List<String>` (the codec has no set); the Dart
     * repository reads those keys with `getStringList`, and the two Kotlin
     * set-valued keys are unordered there too. `Int`/`Long` both arrive in Dart
     * as `int`, and `Boolean`/`String` map across unchanged.
     */
    private fun readLegacyPrefs(name: String?): Map<String, Any?> {
        if (name.isNullOrBlank() || !legacyPrefsFile(name).exists()) return emptyMap()
        return getSharedPreferences(name, Context.MODE_PRIVATE).all
            .mapValues { (_, value) ->
                when (value) {
                    is Float -> value.toString().toDouble()
                    is Set<*> -> value.filterIsInstance<String>()
                    else -> value
                }
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        capturePendingRouteImport(intent)
    }

    /** Kotlin `updateRouteImportRequest`: stash a supported route URI, if any. */
    private fun capturePendingRouteImport(intent: Intent?) {
        routeImportUri(intent)?.let { pendingRouteImportUri = it }
    }

    /**
     * Kotlin `Intent.routeImportUri()`: VIEW carries the file in `data`, SEND in
     * `EXTRA_STREAM`, SEND_MULTIPLE in the first supported stream. Only
     * .gpx/.kml/.kmz/.fit are accepted — the manifest's wildcard-MIME
     * path-pattern filter is deliberately broad, so the real check happens here.
     */
    private fun routeImportUri(intent: Intent?): Uri? {
        if (intent == null) return null
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.streamExtra()
            Intent.ACTION_SEND_MULTIPLE -> {
                val streams = intent.streamExtras()
                streams.firstOrNull { isSupportedRouteImport(it) }
                    ?: streams.firstOrNull()
            }
            else -> null
        }
        return uri?.takeIf { isSupportedRouteImport(it) }
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamExtra(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun Intent.streamExtras(): List<Uri> =
        (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            ) ?: emptyList()

    private fun isSupportedRouteImport(uri: Uri): Boolean {
        val name = routeImportFileName(uri) ?: uri.lastPathSegment ?: return false
        return ROUTE_IMPORT_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
    }

    /** Resolves the display name of a `content://` (or `file://`) URI. */
    private fun routeImportFileName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        }.getOrNull() ?: uri.lastPathSegment
    }

    /**
     * Reads the file's bytes for Dart. Returned as a raw byte array so it lands
     * as a `Uint8List`, matching the `ActivityRouteFileHandle(bytes, fileName)`
     * the Settings import cards already build.
     */
    private fun readRouteImport(uri: Uri): Map<String, Any?>? = runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@runCatching null
        mapOf(
            "fileName" to routeImportFileName(uri),
            "bytes" to bytes,
        )
    }.getOrNull()

    override fun onDestroy() {
        importExecutor.shutdown()
        altitudeExecutor.shutdown()
        super.onDestroy()
    }

    /**
     * Reads this process's logcat buffer (`logcat -d --pid <mypid> -v tag`),
     * mirroring the Kotlin `PrivacySafeDebugLogExporter.readCurrentProcessLogcat`.
     * Returns the raw lines; the Dart side does all privacy sanitizing. Any
     * failure/timeout is surfaced as a single diagnostic line rather than
     * throwing, so the Dart channel call always resolves.
     */
    private fun readCurrentProcessLogcat(): List<String> =
        runCatching {
            val process = ProcessBuilder(
                "logcat",
                "-d",
                "--pid",
                android.os.Process.myPid().toString(),
                "-v",
                "tag",
            )
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroy()
                return listOf("W/OpenVitalsDiagnostics: logcat capture timed out")
            }
            process.inputStream.bufferedReader().useLines { it.toList() }
        }.getOrElse { throwable ->
            listOf(
                "E/OpenVitalsDiagnostics: logcat capture failed " +
                    "type=${throwable::class.java.simpleName}",
            )
        }

    private fun sensorTypeFor(name: String?): Int? =
        when (name) {
            "proximity" -> Sensor.TYPE_PROXIMITY
            "stepDetector" -> Sensor.TYPE_STEP_DETECTOR
            "accelerometer" -> Sensor.TYPE_ACCELEROMETER
            else -> null
        }

    /** Kotlin `ActivityRecordingController.hasActivityRecognitionPermission`. */
    private fun hasActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ) == PackageManager.PERMISSION_GRANTED

    private fun requestActivityRecognitionPermission(result: MethodChannel.Result) {
        if (hasActivityRecognitionPermission()) {
            result.success(true)
            return
        }
        if (pendingPermissionResult != null) {
            // A request dialog is already up; report the current (denied) state
            // instead of stacking a second system dialog.
            result.success(false)
            return
        }
        pendingPermissionResult = result
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            ACTIVITY_RECOGNITION_REQUEST_CODE,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            pendingPermissionResult?.success(granted)
            pendingPermissionResult = null
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Kotlin `Location.withMslAltitude`: convert the fix's WGS84 ellipsoid
     * altitude to mean sea level, best-effort, on a background thread. Returns
     * null below API 34 or on any conversion failure so the Dart side keeps the
     * raw altitude.
     */
    private fun convertToMsl(
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        result: MethodChannel.Result,
    ) {
        val converter = altitudeConverter
        if (converter == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            result.success(null)
            return
        }
        altitudeExecutor.execute {
            val mslAltitudeMeters = runCatching {
                mslAltitudeMetersOrNull(converter, latitude, longitude, altitudeMeters)
            }.getOrNull()
            mainHandler.post { result.success(mslAltitudeMeters) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun mslAltitudeMetersOrNull(
        converter: AltitudeConverter,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
    ): Double? {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = latitude
            this.longitude = longitude
            this.altitude = altitudeMeters
        }
        converter.addMslAltitudeToLocation(this, location)
        return if (location.hasMslAltitude()) location.mslAltitudeMeters else null
    }

    private companion object {
        const val METHOD_CHANNEL = "tech.mmarca.openvitals/recording_sensors"
        const val DIAGNOSTICS_CHANNEL = "tech.mmarca.openvitals/diagnostics"
        const val PROXIMITY_CHANNEL = "tech.mmarca.openvitals/recording_sensors/proximity"
        const val STEP_DETECTOR_CHANNEL = "tech.mmarca.openvitals/recording_sensors/step_detector"
        const val ROUTE_IMPORT_CHANNEL = "tech.mmarca.openvitals/route_import"
        const val FOLDER_IMPORT_CHANNEL = "tech.mmarca.openvitals/folder_import"
        const val LEGACY_MIGRATION_CHANNEL = "tech.mmarca.openvitals/legacy_migration"
        const val ACTIVITY_RECOGNITION_REQUEST_CODE = 4031

        /** Room database of the Kotlin app; drift reads the same file. */
        const val LEGACY_DATABASE_NAME = "openvitals.db"

        /** The Kotlin `PreferencesRepository.PREFS_FILE`. */
        const val LEGACY_PREFS_NAME = "openvitals_prefs"

        /**
         * Guards against a pick of the storage root: a walk that deep, or a list
         * that long, is a mis-pick rather than an import, and neither the walk
         * nor the batch that follows should run for an hour because of it.
         */
        const val MAX_FOLDER_DEPTH = 8
        const val MAX_FOLDER_FILES = 2000

        /** The route formats the activity-entry form can parse. */
        val ROUTE_IMPORT_EXTENSIONS = listOf(".gpx", ".kml", ".kmz", ".fit", ".tcx")
    }
}

/**
 * Bridges one sensor type onto an [EventChannel]: registers a listener at
 * `SENSOR_DELAY_NORMAL` (matching the Kotlin service's registration) while the
 * Dart stream has a subscriber, and unregisters when it cancels. Emits
 * `endOfStream` immediately when the sensor does not exist so the Dart side
 * completes instead of waiting forever.
 */
private class SensorStreamHandler(
    private val sensorManager: SensorManager,
    private val sensorType: Int,
    private val mapEvent: (SensorEvent) -> Any?,
) : EventChannel.StreamHandler {
    private var listener: SensorEventListener? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            events.endOfStream()
            return
        }
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != sensorType) return
                mapEvent(event)?.let(events::success)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = sensorListener
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onCancel(arguments: Any?) {
        listener?.let { runCatching { sensorManager.unregisterListener(it) } }
        listener = null
    }
}
