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
 * Folds newly synced watch Body Battery readings into the personal gains.
 * Each hour bucket counts once: a watermark records the last bucket fitted,
 * so syncing often does not teach the model faster. Best-effort: a failed
 * fit leaves the watermark in place.
 */
@Singleton
class FitBodyEnergyFromWatchUseCase(
    private val wellnessRepository: GarminWellnessRepository,
    private val preferencesRepository: PreferencesRepository,
    private val bodyEnergyRepository: BodyEnergyRepository,
    private val zone: ZoneId,
) {
    // Dagger does not read default arguments, so the injectable constructor supplies the zone.
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

        // Grouped by local day: a timeline is computed per day.
        val byDay = samples.groupBy(
            keySelector = { sample -> sample.time.atZone(zone).toLocalDate() },
            valueTransform = { sample ->
                WatchBodyEnergySample(time = sample.time, score = sample.value.toInt())
            },
        )

        // Oldest first: the watermark is a single scalar.
        val days = byDay.keys.sorted()
        val coldDayCutoff = now.atZone(zone).toLocalDate().minusDays(ColdDayGraceDays)

        var fitted = 0
        var retiredThrough: Long? = null
        for (date in days) {
            val daySamples = byDay.getValue(date)
            val readings = observationsForDay(date, daySamples)

            if (readings.isEmpty()) {
                // No timeline yet: the chain has not reached it. Stop and leave the
                // rest for next run, or the watermark would retire unexamined evidence.
                if (date.isAfter(coldDayCutoff)) break
                // Old enough that waiting is no longer a bet on the chain; retire it.
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
            // Every bucket of a day with a timeline is retired, paired or not.
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
            // No timeline for that day; treated like an empty one.
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

        /** How far back a first run looks, so months of history are not fitted at once. */
        val MaxLookback: Duration = Duration.ofDays(7)

        /**
         * How long a day with no timeline is waited for. The watermark is one
         * scalar, so a permanently cold day would block every day after it.
         */
        const val ColdDayGraceDays = 2L
    }
}
