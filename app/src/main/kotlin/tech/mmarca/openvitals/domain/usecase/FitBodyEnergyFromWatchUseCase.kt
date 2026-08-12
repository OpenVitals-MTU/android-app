package tech.mmarca.openvitals.domain.usecase

import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.GarminWellnessRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyWatchReading
import tech.mmarca.openvitals.domain.insights.WatchBodyEnergySample
import tech.mmarca.openvitals.domain.insights.WatchObservationBucket
import tech.mmarca.openvitals.domain.insights.buildWatchObservations
import tech.mmarca.openvitals.domain.insights.fitBodyEnergyGains
import tech.mmarca.openvitals.domain.insights.watchObservationBucketIndex
import tech.mmarca.openvitals.domain.model.GarminWellnessMetric

/**
 * Folds newly-synced watch Body Battery readings into the personal gains.
 *
 * The watch measures what this app models, so where the two disagree the watch
 * is evidence about which gain is mis-set. Without this, the Body Battery a
 * Garmin sync stores is only ever drawn on the watch-data screen — it never
 * teaches the model anything, and the gains sit at their defaults forever no
 * matter how much watch data accumulates.
 *
 * Follows the feel-check rule exactly: **each observation is counted once.**
 * The unit counted is an hour BUCKET, not a sample. A watermark records the
 * last bucket already fitted and only later buckets are considered, so an hour
 * contributes exactly one observation however many times the watch is synced
 * during it. Keying on the newest sample instead would make the learning rate
 * depend on how often the user taps Sync — ten syncs an hour would teach the
 * model ten times as fast as one, from identical watch data.
 *
 * Best-effort throughout. Calibration is an enhancement, so a failure to fit
 * must never fail the sync that triggered it: the watermark simply does not
 * advance and the readings are retried next time.
 */
@Singleton
class FitBodyEnergyFromWatchUseCase(
    private val wellnessRepository: GarminWellnessRepository,
    private val preferencesRepository: PreferencesRepository,
    private val bodyEnergyRepository: BodyEnergyRepository,
    private val zone: ZoneId,
) {
    // The zone is a seam for tests, and Dagger does not read Kotlin default
    // arguments — so the injectable constructor supplies the real one.
    @Inject
    constructor(
        wellnessRepository: GarminWellnessRepository,
        preferencesRepository: PreferencesRepository,
        bodyEnergyRepository: BodyEnergyRepository,
    ) : this(
        wellnessRepository = wellnessRepository,
        preferencesRepository = preferencesRepository,
        bodyEnergyRepository = bodyEnergyRepository,
        zone = ZoneId.systemDefault(),
    )

    /** Returns how many observations were folded in. */
    suspend operator fun invoke(now: Instant = Instant.now()): Int {
        val bucketMillis = WatchObservationBucket.toMillis()
        val fittedBucketStart = preferencesRepository.bodyEnergyWatchFitWatermarkMillis
        // Start of the first bucket not yet fitted.
        val from = if (fittedBucketStart > 0L) {
            Instant.ofEpochMilli(fittedBucketStart + bucketMillis)
        } else {
            now.minus(MaxLookback)
        }

        val samples = try {
            wellnessRepository.samplesBetween(
                metric = GarminWellnessMetric.BODY_ENERGY,
                from = from,
                to = now.plusMillis(1),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not read watch body-energy samples", error)
            return 0
        }
        if (samples.isEmpty()) return 0

        // Grouped by the local day each sample belongs to: a timeline is
        // computed per day, and pairing needs the one covering the sample's own
        // moment.
        val byDay = samples.groupBy(
            keySelector = { sample -> sample.time.atZone(zone).toLocalDate() },
            valueTransform = { sample ->
                WatchBodyEnergySample(time = sample.time, score = sample.value.toInt())
            },
        )

        // Oldest first, because the watermark is a single scalar: it can only
        // ever say "everything before here is done", so the days have to be
        // retired in order for that to stay true.
        val days = byDay.keys.sorted()
        val coldDayCutoff = now.atZone(zone).toLocalDate().minusDays(ColdDayGraceDays)

        var fitted = 0
        var retiredThrough: Long? = null
        for (date in days) {
            val daySamples = byDay.getValue(date)
            val readings = observationsForDay(date, daySamples)

            if (readings.isEmpty()) {
                // A day with no timeline yet — the chain has not reached it, or
                // the permissions were not there when it was asked. Its readings
                // are not unpairable, merely early, so stop and leave the whole
                // remainder for the next run.
                //
                // Advancing over it is what would make the watermark lossy:
                // jumping to the newest bucket of every sample READ the moment
                // any single day fitted silently retires evidence that was never
                // examined, and combined with the epoch reset in
                // BodyEnergyRepository that leaves the gains pinned at their
                // defaults with thousands of stored samples they are no longer
                // allowed to see.
                if (date.isAfter(coldDayCutoff)) break
                // Old enough that waiting has stopped being a bet on the chain
                // catching up, so retire it: it is the only thing that keeps the
                // watermark moving and lets the days behind it be read at all.
                retiredThrough = newestBucket(daySamples)
                continue
            }

            preferencesRepository.setBodyEnergyCalibration(
                fitBodyEnergyGains(
                    current = preferencesRepository.bodyEnergyCalibration(),
                    watchReadings = readings,
                ),
            )
            fitted += readings.size
            // Every bucket of a day that HAS a timeline is retired, not only the
            // ones that paired: within such a day, a reading that found no point
            // within the pairing gap never will.
            retiredThrough = newestBucket(daySamples)
        }

        retiredThrough?.let { bucket ->
            preferencesRepository.bodyEnergyWatchFitWatermarkMillis = bucket * bucketMillis
        }
        if (fitted > 0) {
            Log.i(
                TAG,
                "Folded $fitted watch readings into the gains " +
                    "(${preferencesRepository.bodyEnergyCalibration().watchObservationCount} total)",
            )
        }
        return fitted
    }

    private fun newestBucket(samples: List<WatchBodyEnergySample>): Long =
        samples.maxOf { sample -> watchObservationBucketIndex(sample.time) }

    private suspend fun observationsForDay(
        date: LocalDate,
        samples: List<WatchBodyEnergySample>,
    ): List<BodyEnergyWatchReading> {
        val result = try {
            bodyEnergyRepository.loadTimeline(
                BodyEnergyTimelineQuery(
                    period = DatePeriod(date, date),
                    range = TimeRange.DAY,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // No timeline for that day (missing permissions, no heart data) —
            // the readings simply have nothing to be compared against, and the
            // caller treats that the same as an empty one.
            Log.w(TAG, "No Body Energy timeline for $date", error)
            return emptyList()
        }
        val timeline = result.days.firstOrNull { day -> day.date == date }
            ?: result.latestDay
            ?: return emptyList()
        return buildWatchObservations(samples = samples, timeline = timeline)
    }

    private companion object {
        const val TAG = "BodyEnergyWatchFit"

        /**
         * How far back to look for unfitted samples on a first run, so an
         * install with months of history does not try to fit all of it at once.
         */
        val MaxLookback: Duration = Duration.ofDays(7)

        /**
         * How long a day with no timeline is waited for before it is retired.
         *
         * The watermark is one scalar, so holding for a day that yields nothing
         * holds every day after it too. Waiting is right when the reason is "the
         * chain has not reached it yet", which resolves within a warm pass or
         * two; it is wrong when the day has no heart data and never will,
         * because then the wait never ends. Two days separates them about as
         * well as anything can, and bounds the damage either way — using
         * [MaxLookback] as the cutoff instead means a single permanently-cold
         * day inside the window blocks the whole refit behind it.
         */
        const val ColdDayGraceDays = 2L
    }
}
