package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.wearlink.WearLinkProfile

/**
 * After a sync, hands the watch what its tiles need from the phone
 * ([WearLinkProfile]): the step length, body weight, distance unit and daily
 * goals, and Health Connect's totals for today, every source together, read
 * after the sync wrote the watch's own. A watch app from before the tiles is
 * skipped ([WearOsNodePort.pushProfile]).
 *
 * Only what is known for sure is sent; the watch keeps the rest from the
 * last time. In the background without the grant to read other apps' data,
 * Health Connect shows this app its own records only, so the totals would be
 * too low and the weight could be an older one than another app holds: then
 * nothing is sent, and the next sync in the foreground brings both. Whether
 * other apps' data is visible is asked before and after the reads, since the
 * app can leave the foreground between them. A weight read that fails keeps
 * the profile back, and a totals read that fails keeps the totals back.
 *
 * The totals hold the watch's steps up to the newest minute the sync wrote
 * ([push]'s `watchStepsThrough`); the watch adds its own minutes from the
 * next one on ([WearLinkProfile.Totals.asOfEpochMillis]).
 *
 * Best effort: a failure is logged and never fails the sync.
 */
class WearOsTileProfilePush(
    private val nodePort: WearOsNodePort,
    private val settings: () -> Settings,
    private val weightKg: suspend () -> Double?,
    private val seesOtherApps: suspend () -> Boolean,
    private val todayTotals: suspend (LocalDate) -> DayTotals?,
    private val clock: () -> Instant,
    private val zone: () -> ZoneId,
) {
    /** The phone's settings the watch's tiles use, as the app stores them; [distanceUnits] with its override applied. */
    data class Settings(
        val strideMeters: Double,
        val distanceUnits: UnitSystem,
        val goalSteps: Double,
        val goalDistanceMeters: Double,
        val goalActiveKcal: Double,
    )

    /** Health Connect's totals for one day; null where the phone may not read that type. */
    data class DayTotals(val steps: Long?, val distanceMeters: Double?, val activeKcal: Double?)

    /**
     * Sends what is certain. [watchStepsThrough] is the start of the newest
     * step minute the sync wrote to Health Connect, null when it never wrote
     * one: the totals hold the watch's steps up to that minute.
     */
    suspend fun push(device: BleSensorDevice, watchStepsThrough: Instant?) {
        try {
            val now = clock()
            val today = now.atZone(zone()).toLocalDate()
            val dayStart = today.atStartOfDay(zone()).toInstant()
            if (!othersVisible()) {
                WearOsLog.log("tiles: other apps' data not visible, nothing sent")
                return
            }
            val weight = readWeight()
            val day = readOrNull("today's totals") { todayTotals(today) }
            if (!othersVisible()) {
                WearOsLog.log("tiles: other apps' data no longer visible, nothing sent")
                return
            }
            val sentProfile = weight?.let { profile(settings(), it.kg) }
            // The first of the watch's minutes the totals do not hold; midnight when none of today's was written.
            val frontier = watchStepsThrough?.plusSeconds(SECONDS_PER_MINUTE)?.takeIf { it > dayStart } ?: dayStart
            val sentTotals = day?.let { totals(it, dayStart, frontier) }
            if (sentProfile == null && sentTotals == null) {
                WearOsLog.log("tiles: nothing certain to send")
                return
            }
            val sent = nodePort.pushProfile(device.address, device.bluetoothName, sentProfile, sentTotals)
            WearOsLog.log(if (sent) "tiles: sent" else "tiles: watch app takes no profile")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WearOsLog.log("tiles: not sent: ${error::class.java.simpleName}")
        }
    }

    private suspend fun othersVisible(): Boolean = readOrNull("access to other apps' data") { seesOtherApps() } ?: false

    /** The body weight as read, or null when the read failed. */
    private suspend fun readWeight(): Weight? = try {
        Weight(weightKg())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        WearOsLog.log("tiles: body weight not read: ${error::class.java.simpleName}")
        null
    }

    /** A weight that was read; [kg] null means none is known. */
    private class Weight(val kg: Double?)

    private suspend fun <T> readOrNull(what: String, read: suspend () -> T?): T? = try {
        read()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        WearOsLog.log("tiles: $what not read: ${error::class.java.simpleName}")
        null
    }

    companion object {
        /** The wire's whole millimetres, grams and goals; a weight outside what the watch accepts goes as none. */
        fun profile(settings: Settings, weightKg: Double?): WearLinkProfile.Profile =
            WearLinkProfile.Profile(
                strideMillimetres = (StrideLength.normalize(settings.strideMeters) * MILLIMETRES_PER_METRE).roundToInt(),
                weightGrams = weightKg?.takeIf { it.isFinite() }?.let { (it * GRAMS_PER_KILOGRAM).roundToInt() }?.takeIf { it in WEIGHT_GRAMS },
                units = when (settings.distanceUnits) {
                    UnitSystem.METRIC -> WearLinkProfile.Units.METRIC
                    UnitSystem.IMPERIAL -> WearLinkProfile.Units.IMPERIAL
                },
                goalSteps = settings.goalSteps.roundToInt().coerceAtLeast(1),
                goalDistanceMetres = settings.goalDistanceMeters.roundToInt().coerceAtLeast(1),
                goalActiveKcal = settings.goalActiveKcal.roundToInt().coerceAtLeast(1),
            )

        /** Today's totals from midnight, [dayStart]; [frontier] is the first of the watch's minutes they do not hold. */
        fun totals(day: DayTotals, dayStart: Instant, frontier: Instant): WearLinkProfile.Totals =
            WearLinkProfile.Totals(
                dayStartEpochMillis = dayStart.toEpochMilli(),
                asOfEpochMillis = frontier.toEpochMilli(),
                steps = day.steps,
                distanceMetres = day.distanceMeters?.takeIf { it.isFinite() }?.roundToLong(),
                activeKcal = day.activeKcal?.takeIf { it.isFinite() }?.roundToLong(),
            )

        private const val SECONDS_PER_MINUTE = 60L
        private const val MILLIMETRES_PER_METRE = 1_000.0
        private const val GRAMS_PER_KILOGRAM = 1_000.0

        /** What the watch reads as a body weight (`WearLinkProfile`); anything else goes as none. */
        private val WEIGHT_GRAMS = 20_000..400_000
    }
}
