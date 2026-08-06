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
 * One emission of the live feed.
 *
 * [live] marks an emission that came from a `notifyChange` — CoMaps saying
 * something moved. It is the ONLY evidence a route is still being driven:
 * CoMaps never clears the routing info its provider answers from, so a
 * finished route keeps being served until the CoMaps process dies.
 *
 * [observing] says whether that evidence can arrive at all. When the observer
 * could not be registered there is none, and the reader must go back to
 * believing what it reads.
 *
 * [initial] marks the first emission of a NEW watch — a fresh recording.
 * Whatever a previous watch learned about a previous route does not carry over.
 */
data class CoMapsLiveEvent(
    val answer: CoMapsProviderAnswer,
    val live: Boolean,
    val observing: Boolean,
    val initial: Boolean,
)

/**
 * The platform surface of the CoMaps integration — a `ContentResolver` query,
 * a `PackageManager` lookup, an intent launch — and nothing else. It
 * classifies nothing: it reports what it found and hands the raw row up,
 * because "is this navigating?" is a domain question and domain questions
 * belong where they can be answered without a device.
 *
 * CoMaps exposes a live-navigation `ContentProvider` (upstream PR #4588):
 * `content://<comapsPackage>.provider.navigation/live`, read-protected by the
 * runtime permission `<comapsPackage>.permission.READ_NAVIGATION_DATA`.
 *
 * Package visibility is load-bearing. From Android 11 an app cannot see a
 * package it has not declared, so `AndroidManifest.xml` lists every known
 * CoMaps package *and* every provider authority in `<queries>`. Without that
 * the resolves below silently return null and CoMaps looks uninstalled while
 * it is running.
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
                    // The provider answers an empty cursor when nobody is
                    // being guided anywhere. That is not an error.
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
     * Watches the live row and pushes a fresh reading on every change.
     *
     * CoMaps' `NavigationService` calls `notifyChange` on the provider URI at
     * every location fix while it guides, so guidance arrives as fast as
     * CoMaps makes it and a phone that is navigating nowhere is never queried.
     *
     * The first emission is the CURRENT state, not a change: a subscriber that
     * attached mid-route would otherwise see nothing until the next fix.
     *
     * Registration is best-effort. A missing app, a build with no provider, a
     * revoked grant — all ordinary states the query already describes, so a
     * failure to observe emits that state and stops there rather than erroring
     * the flow. The recording never depends on guidance.
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
        // Registered FIRST so `observing` is the truth by the time it is sent.
        // This one is a read of whatever the provider is holding, not evidence
        // that anybody is navigating.
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
     * The followed route as interleaved `lat, lon` doubles, or null when there
     * is no route to read. Its own method, never folded into [queryLive]: that
     * runs per location fix, and a route is thousands of points that only
     * change with `route_revision`.
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
     * The permission guarding the provider, for the CoMaps that is actually
     * installed.
     *
     * CoMaps declares it as `${applicationId}.permission.READ_NAVIGATION_DATA`,
     * so the NAME varies per flavour exactly as the authority does — Play is
     * `app.comaps.google.…` and F-Droid is `app.comaps.fdroid.…`. Asking for
     * the wrong one is denied silently and forever. Null when no CoMaps is
     * installed — there is then no permission to hold.
     */
    fun permissionName(): String? =
        installedPackage()?.let { packageName -> "$packageName$PermissionSuffix" }

    fun hasPermission(): Boolean {
        val permission = permissionName() ?: return false
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Drops the cached package choice — after a grant, the probe may answer differently. */
    fun invalidateResolvedPackage() {
        resolvedPackage = null
    }

    fun canLaunchCoMaps(): Boolean = launchPackage() != null

    /**
     * Hands CoMaps the map, centred on our latest fix when we have one, so the
     * user can plan a route there. OpenVitals never plans or navigates: CoMaps
     * owns the route, we own the recording.
     */
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

    /**
     * Column values arrive typed: the distances are *strings already
     * formatted* by CoMaps against its own locale and units, the times are
     * ints, the completion is a double. Read each as what it is, and let a
     * column CoMaps did not send simply be absent.
     */
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
     * The one CoMaps this build talks to: provider, permission and launch
     * intent must come from the SAME package, or the app asks one flavour for
     * a grant and queries another. Preferred in order: one whose provider
     * answers `/route`; one that refused the probe, since a refusal means
     * "not granted yet" and choosing it is what puts it in front of the
     * prompt; whichever resolved first. Cached, because [queryLive] runs once
     * per fix.
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

    /** Asked of the URI, not of a column list: a CoMaps without the geometry
     * contract does not match `/route` and answers a null cursor. */
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
        /**
         * A SUFFIX, not a whole name: CoMaps declares
         * `${applicationId}.permission.READ_NAVIGATION_DATA`, so the
         * flavour's package goes in front (see [permissionName]).
         */
        private const val PermissionSuffix = ".permission.READ_NAVIGATION_DATA"

        /**
         * Every CoMaps flavour we know of. The authority is the package plus
         * `.provider.navigation` (CoMaps builds it from its own
         * applicationId), so one list drives both package detection and
         * provider resolution.
         */
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
            // Null on a CoMaps predating the geometry contract, which is how
            // the package probe tells the two apart.
            "route_revision",
            "route_point_count",
            "dest_lat",
            "dest_lon",
            "dest_title",
        )

        val RouteColumns = arrayOf("lat", "lon")
    }
}
