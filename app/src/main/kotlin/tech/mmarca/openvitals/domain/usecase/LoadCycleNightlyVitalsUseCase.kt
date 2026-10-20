package tech.mmarca.openvitals.domain.usecase

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.cycle.NightSample
import tech.mmarca.openvitals.domain.cycle.NightWindow
import tech.mmarca.openvitals.domain.cycle.NightlySignal
import tech.mmarca.openvitals.domain.cycle.NightlyVitals
import tech.mmarca.openvitals.domain.cycle.NightlyVitalsCalculator

/**
 * The current cycle's nightly resting pulse, HRV and breathing rate, cut by the sleep
 * sessions. It reuses the repositories the heart, sleep and vitals screens read through.
 * One failing read costs that signal, not the card: a permission failure marks the signal
 * as not allowed (so the card can say "permission" instead of "no data"), any other failure
 * leaves it empty. Cancellation is never swallowed.
 */
class LoadCycleNightlyVitalsUseCase @Inject constructor(
    private val sleep: SleepRepository,
    private val heart: HeartRepository,
    private val vitals: VitalsRepository,
) {
    private class Read(val samples: List<NightSample>, val denied: Boolean)

    suspend operator fun invoke(
        cycleStart: LocalDate,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): NightlyVitals = coroutineScope {
        // A night that wakes on the cycle's first day began the evening before.
        val from = cycleStart.minusDays(1)
        val fromInstant = from.atStartOfDay(zone).toInstant()
        val toInstant = today.plusDays(1).atStartOfDay(zone).toInstant()

        val sessions = async { guarded { sleep.loadSleepSessions(from, today) } }
        val pulse = async {
            read {
                heart.loadRestingHeartRateSamples(fromInstant, toInstant)
                    .map { NightSample(it.time, it.beatsPerMinute.toDouble(), it.source) }
            }
        }
        val hrv = async {
            read { heart.loadHrvSamples(fromInstant, toInstant).map { NightSample(it.time, it.rmssdMs, it.source) } }
        }
        val breathing = async {
            read { vitals.loadRespiratoryRate(from, today).map { NightSample(it.time, it.breathsPerMinute, it.source) } }
        }

        val reads = mapOf(
            NightlySignal.RESTING_HEART_RATE to pulse.await(),
            NightlySignal.HRV_RMSSD to hrv.await(),
            NightlySignal.RESPIRATORY_RATE to breathing.await(),
        )
        val sleepRead = sessions.await()
        NightlyVitalsCalculator.build(
            cycleStart = cycleStart,
            today = today,
            sleep = sleepRead.first.map { NightWindow(it.startTime, it.endTime) },
            samples = reads.mapValues { it.value.samples },
            zone = zone,
            // No sleep read means no night to place: the card says so; the signals' own denials count too.
            permissionDenied = reads.filterValues { it.denied }.keys +
                if (sleepRead.second) NightlySignal.entries.toSet() else emptySet(),
        )
    }

    private suspend fun read(block: suspend () -> List<NightSample>): Read {
        val (samples, denied) = guarded(block)
        return Read(samples, denied)
    }

    /** The result, or an empty list and whether the failure was a missing permission. */
    private suspend fun <T> guarded(block: suspend () -> List<T>): Pair<List<T>, Boolean> =
        try {
            block() to false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            emptyList<T>() to failure.isPermissionFailure()
        }
}
