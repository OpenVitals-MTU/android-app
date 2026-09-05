package tech.mmarca.openvitals.comaps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/** What one query of CoMaps' live-navigation provider found. */
sealed interface CoMapsProviderAnswer {
    /** No known CoMaps package is installed. */
    data object AppUnavailable : CoMapsProviderAnswer

    /** CoMaps is installed but this build exposes no navigation provider. */
    data object ProviderUnavailable : CoMapsProviderAnswer

    /** The provider exists but reading it is not granted. */
    data object PermissionMissing : CoMapsProviderAnswer

    /** The provider answered an empty cursor: nobody is being guided. */
    data object NotNavigating : CoMapsProviderAnswer

    /** The raw row, column values typed as the cursor held them. */
    data class Active(val row: Map<String, Any?>) : CoMapsProviderAnswer

    /** The query itself failed. */
    data class Failure(val message: String?) : CoMapsProviderAnswer
}

/**
 * One emission of the live feed. [live] marks a `notifyChange`, the only
 * evidence a route is still being driven: CoMaps never clears its routing
 * info. [observing] says whether that evidence can arrive. [initial] marks
 * the first emission of a new watch.
 */
data class CoMapsLiveEvent(
    val answer: CoMapsProviderAnswer,
    val live: Boolean,
    val observing: Boolean,
    val initial: Boolean,
)

/**
 * The platform surface of the CoMaps integration: a `ContentResolver` query,
 * a `PackageManager` lookup, an intent launch. It classifies nothing.
 *
 * CoMaps exposes `content://<comapsPackage>.provider.navigation/live`,
 * guarded by `<comapsPackage>.permission.READ_NAVIGATION_DATA`. Every known
 * package and authority must be in the manifest `<queries>`, or resolves
 * silently return null.
 */
@Singleton
class CoMapsNavigationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var resolvedPackage: String? = null

    suspend fun queryLive(): CoMapsProviderAnswer = withContext(Dispatchers.IO) {
        queryLiveBlocking()
    }

    private fun queryLiveBlocking(): CoMapsProviderAnswer {
        val authority = providerAuthority()
            ?: return if (installedPackage() != null) {
                CoMapsProviderAnswer.ProviderUnavailable
            } else {
                CoMapsProviderAnswer.AppUnavailable
            }
        if (!hasPermission()) return CoMapsProviderAnswer.PermissionMissing

        return try {
            context.contentResolver.query(
                Uri.parse("content://$authority/live"),
                LiveColumns,
                null,
                null,
                null,
            ).use { cursor ->
                when {
                    cursor == null -> CoMapsProviderAnswer.ProviderUnavailable
                    // An empty cursor means nobody is being guided. Not an error.
                    !cursor.moveToFirst() -> CoMapsProviderAnswer.NotNavigating
                    else -> CoMapsProviderAnswer.Active(
                        LiveColumns.associateWith { column -> cursor.typedValue(column) },
                    )
                }
            }
        } catch (error: SecurityException) {
            // The grant can be revoked while we hold it.
            CoMapsProviderAnswer.PermissionMissing
        } catch (error: Exception) {
            CoMapsProviderAnswer.Failure(error.message)
        }
    }

    /**
     * Watches the live row and pushes a reading on every change. The first
     * emission is the current state. Registration is best-effort: a failure
     * emits the state the query describes and stops.
     */
    fun liveUpdates(): Flow<CoMapsLiveEvent> = callbackFlow {
        var observer: ContentObserver? = null
        val authority = providerAuthority()
        if (authority != null) {
            val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(
                        CoMapsLiveEvent(
                            answer = queryLiveBlocking(),
                            live = true,
                            observing = true,
                            initial = false,
                        ),
                    )
                }
            }
            observer = try {
                context.contentResolver.registerContentObserver(
                    Uri.parse("content://$authority/live"),
                    false,
                    contentObserver,
                )
                contentObserver
            } catch (error: SecurityException) {
                null
            }
        }
        // Registered first so `observing` is the truth. This is a read, not evidence.
        trySend(
            CoMapsLiveEvent(
                answer = withContext(Dispatchers.IO) { queryLiveBlocking() },
                live = false,
                observing = observer != null,
                initial = true,
            ),
        )
        awaitClose {
            observer?.let(context.contentResolver::unregisterContentObserver)
        }
    }

    /**
     * The followed route as interleaved `lat, lon`, or null. Separate from
     * [queryLive], which runs per fix; a route only changes with `route_revision`.
     */
    suspend fun queryRoute(): DoubleArray? = withContext(Dispatchers.IO) {
        val authority = providerAuthority() ?: return@withContext null
        if (!hasPermission()) return@withContext null
        try {
            context.contentResolver.query(
                Uri.parse("content://$authority/route"),
                RouteColumns,
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor == null) return@use null
                val latitude = cursor.getColumnIndex("lat")
                val longitude = cursor.getColumnIndex("lon")
                if (latitude < 0 || longitude < 0) return@use null
                val points = DoubleArray(cursor.count * 2)
                var index = 0
                while (cursor.moveToNext()) {
                    points[index++] = cursor.getDouble(latitude)
                    points[index++] = cursor.getDouble(longitude)
                }
                points
            }
        } catch (error: Exception) {
            null
        }
    }

    /**
     * The provider's permission for the installed CoMaps. The name varies per
     * flavour; asking for the wrong one is denied forever. Null without CoMaps.
     */
    fun permissionName(): String? =
        installedPackage()?.let { packageName -> "$packageName$PermissionSuffix" }

    fun hasPermission(): Boolean {
        val permission = permissionName() ?: return false
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Drops the cached package choice; after a grant the probe may answer differently. */
    fun invalidateResolvedPackage() {
        resolvedPackage = null
    }

    fun canLaunchCoMaps(): Boolean = launchPackage() != null

    /** Hands CoMaps the map, centred on our latest fix, for route planning. */
    fun launchForPlanning(latitude: Double?, longitude: Double?): Boolean {
        val packageName = launchPackage() ?: return false
        val intent = if (latitude != null && longitude != null) {
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("cm://map?v=1&ll=$latitude,$longitude&n=OpenVitals")
                setPackage(packageName)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (error: Exception) {
            false
        }
    }

    /** Column values arrive typed; read each as what it is. */
    private fun Cursor.typedValue(column: String): Any? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
            else -> getString(index)
        }
    }

    private enum class RouteSupport { YES, NO, DENIED }

    /**
     * The one CoMaps this build talks to. Provider, permission and launch
     * intent must share a package. Prefers one whose provider answers
     * `/route`, then one that refused the probe (not granted yet). Cached.
     */
    private fun coMapsPackage(): String? {
        val cached = resolvedPackage
        if (cached != null && isPackageInstalled(cached)) return cached
        val chosen = choosePackage()
        resolvedPackage = chosen
        return chosen
    }

    private fun choosePackage(): String? {
        val candidates = KnownCoMapsPackages.filter { packageName ->
            resolveProviderAuthority("$packageName.provider.navigation")
        }
        if (candidates.isEmpty()) {
            return KnownCoMapsPackages.firstOrNull(::isPackageInstalled)
        }
        val probed = candidates.map { packageName ->
            packageName to probeRouteGeometry("$packageName.provider.navigation")
        }
        return probed.firstOrNull { it.second == RouteSupport.YES }?.first
            ?: probed.firstOrNull { it.second == RouteSupport.DENIED }?.first
            ?: candidates.first()
    }

    /** Asked of the URI: a CoMaps without the geometry contract answers a null cursor. */
    private fun probeRouteGeometry(authority: String): RouteSupport = try {
        context.contentResolver.query(
            Uri.parse("content://$authority/route"),
            RouteColumns,
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor == null) RouteSupport.NO else RouteSupport.YES
        }
    } catch (error: SecurityException) {
        RouteSupport.DENIED
    } catch (error: Exception) {
        RouteSupport.NO
    }

    private fun providerAuthority(): String? =
        coMapsPackage()
            ?.let { packageName -> "$packageName.provider.navigation" }
            ?.takeIf(::resolveProviderAuthority)

    private fun resolveProviderAuthority(authority: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveContentProvider(
                authority,
                PackageManager.ComponentInfoFlags.of(0),
            ) != null
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveContentProvider(authority, 0) != null
        }

    private fun installedPackage(): String? = coMapsPackage()

    private fun launchPackage(): String? {
        val launchable = { packageName: String ->
            isPackageInstalled(packageName) &&
                context.packageManager.getLaunchIntentForPackage(packageName) != null
        }
        return coMapsPackage()?.takeIf(launchable)
            ?: KnownCoMapsPackages.firstOrNull(launchable)
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (error: PackageManager.NameNotFoundException) {
            false
        }

    companion object {
        /** A suffix: the flavour's package goes in front. See [permissionName]. */
        private const val PermissionSuffix = ".permission.READ_NAVIGATION_DATA"

        /** Every known CoMaps flavour. The authority is the package plus `.provider.navigation`. */
        val KnownCoMapsPackages = listOf(
            "app.comaps",
            "app.comaps.fdroid",
            "app.comaps.google",
            "app.comaps.huawei",
            "app.comaps.test",
            "app.comaps.debug",
            "app.comaps.fdroid.debug",
            "app.comaps.google.debug",
            "app.comaps.huawei.debug",
        )

        /** CoMaps' `NavigationContract.Live.Columns`, verbatim. */
        val LiveColumns = arrayOf(
            "session_state",
            "car_direction",
            "pedestrian_direction",
            "dist_to_turn",
            "dist_to_target",
            "dist_to_next_stop",
            "total_time_seconds",
            "time_to_next_stop",
            "current_street",
            "next_street",
            "completion_percent",
            "exit_num",
            // Null on a CoMaps predating the geometry contract.
            "route_revision",
            "route_point_count",
            "dest_lat",
            "dest_lon",
            "dest_title",
        )

        val RouteColumns = arrayOf("lat", "lon")
    }
}
