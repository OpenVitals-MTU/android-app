package tech.mmarca.openvitals.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile

/** The zone every Body Energy test pins itself to, so a fixed clock is a fixed day. */
val TestZone: ZoneId = ZoneId.of("UTC")

/**
 * Keeps the whole load on the test dispatcher.
 *
 * `runTest` advances virtual time whenever no test-dispatcher work is pending,
 * so a hop to `Dispatchers.Default` mid-load lets the chain fill's 12-second
 * budget expire instantly and every gap fill degrade to CHAIN_GAP.
 */
object TestDispatcherProvider : DispatcherProvider {
    override val main: CoroutineContext = Dispatchers.Unconfined
    override val io: CoroutineContext = Dispatchers.Unconfined
    override val default: CoroutineContext = Dispatchers.Unconfined
}

/**
 * A heart repository that reports a steady waking heart rate all day, so every
 * day drains a predictable amount and the chain is observable.
 *
 * [dayGraphCalls] is what tells a stored-chain read (no Health Connect work)
 * apart from a recompute.
 */
class FakeHeartRepository(
    private val wakingBpm: Long? = 70L,
) {
    var dayGraphCalls = 0
        private set
    val daysRead = mutableListOf<LocalDate>()
    var dailyRestingCalls = 0
        private set

    val repository: HeartRepository = mockk<HeartRepository>().also { heart ->
        val dateSlot = slot<LocalDate>()
        coEvery { heart.loadRawHeartRateSamplesForDayGraph(capture(dateSlot)) } answers {
            dayGraphCalls++
            val date = dateSlot.captured
            daysRead += date
            val bpm = wakingBpm ?: return@answers emptyList()
            (7 until 22).map { hour ->
                HeartRateSample(
                    time = date.atStartOfDay(TestZone).plusHours(hour.toLong()).toInstant(),
                    beatsPerMinute = bpm,
                    source = "test",
                )
            }
        }
        coEvery { heart.loadHrvSamples(any(), any()) } returns emptyList()
        coEvery { heart.loadRestingHeartRate(any()) } returns 55L
        val endSlot = slot<LocalDate>()
        coEvery { heart.loadDailyRestingHR(any(), capture(endSlot)) } answers {
            dailyRestingCalls++
            listOf(DailyRestingHR(date = endSlot.captured, bpm = 54L))
        }
        coEvery { heart.loadDailyHRV(any(), any()) } returns emptyList()
        coEvery { heart.loadHeartRateSamples(any<Instant>(), any<Instant>()) } returns emptyList()
    }
}

/** Sleep / activity / vitals / body collaborators that report nothing at all. */
fun emptySleepRepository(): SleepRepository = mockk<SleepRepository>().also {
    coEvery { it.loadSleepSessions(any(), any()) } returns emptyList()
}

fun emptyActivityRepository(): ActivityRepository = mockk<ActivityRepository>().also {
    coEvery { it.loadWorkouts(any(), any()) } returns emptyList()
    coEvery { it.loadActivityProgress(any()) } returns emptyList()
}

fun emptyVitalsRepository(): VitalsRepository = mockk<VitalsRepository>().also {
    coEvery { it.loadRespiratoryRate(any(), any()) } returns emptyList()
}

fun emptyBodyRepository(): BodyRepository = mockk<BodyRepository>().also {
    coEvery { it.loadLatestBMR() } returns null
}

fun grantedHealthRepository(
    granted: Set<String> = setOf("read-heart-rate"),
): HealthRepository = mockk<HealthRepository>().also {
    every { it.availability() } returns HealthConnectAvailability.AVAILABLE
    coEvery { it.grantedPermissions() } returns granted
}

/**
 * A health repository whose permission read fails — Health Connect throttling a
 * background caller, or the provider mid-update. Data reads are not routed
 * through it, so a day can still compute while this read is down.
 */
fun failingPermissionsHealthRepository(): HealthRepository = mockk<HealthRepository>().also {
    every { it.availability() } returns HealthConnectAvailability.AVAILABLE
    coEvery { it.grantedPermissions() } throws IllegalStateException("rate limited")
}

/** A baseline store backed by a map instead of SharedPreferences. */
fun inMemoryBaselineStore(): BodyEnergyBaselineCacheStore = mockk<BodyEnergyBaselineCacheStore>().also { store ->
    val entries = mutableMapOf<String, BodyEnergyBaselineCacheEntry>()
    val dateSlot = slot<LocalDate>()
    val signatureSlot = slot<String>()
    every { store.loadBaseline(capture(dateSlot), capture(signatureSlot)) } answers {
        entries["${dateSlot.captured}|${signatureSlot.captured}"]
    }
    val saveDate = slot<LocalDate>()
    val saveSignature = slot<String>()
    val saveEntry = slot<BodyEnergyBaselineCacheEntry>()
    every {
        store.saveBaseline(capture(saveDate), capture(saveSignature), capture(saveEntry))
    } answers {
        entries["${saveDate.captured}|${saveSignature.captured}"] = saveEntry.captured
    }
    every { store.purgeLegacyTimelineEntries() } just runs
}

/**
 * The handful of preferences the chain reads and writes, backed by local state
 * rather than SharedPreferences (there is no Android context in the unit suite).
 */
fun inMemoryPreferences(
    calibration: BodyEnergyCalibration = BodyEnergyCalibration.Automatic,
    bodyProfile: BodyProfile = BodyProfile(),
): PreferencesRepository = mockk<PreferencesRepository>().also { prefs ->
    var storedCalibration = calibration.normalized()
    var gainsAlgorithmVersion = 0
    var watchFitEpoch = 0
    var watermarkMillis = 0L
    var seedMirror: String? = null
    var permissionSignature: Int? = null

    every { prefs.bodyEnergyCalibration() } answers { storedCalibration }
    every { prefs.setBodyEnergyCalibration(any()) } answers {
        storedCalibration = firstArg<BodyEnergyCalibration>().normalized()
    }
    every { prefs.bodyProfile() } returns bodyProfile

    every { prefs.bodyEnergyGainsAlgorithmVersion } answers { gainsAlgorithmVersion }
    every { prefs.bodyEnergyGainsAlgorithmVersion = any() } answers {
        gainsAlgorithmVersion = firstArg()
    }

    every { prefs.bodyEnergyWatchFitEpoch } answers { watchFitEpoch }
    every { prefs.bodyEnergyWatchFitEpoch = any() } answers { watchFitEpoch = firstArg() }

    every { prefs.bodyEnergyWatchFitWatermarkMillis } answers { watermarkMillis }
    every { prefs.bodyEnergyWatchFitWatermarkMillis = any() } answers {
        watermarkMillis = firstArg()
    }

    every { prefs.bodyEnergyChainSeedMirror } answers { seedMirror }
    every { prefs.bodyEnergyChainSeedMirror = any() } answers { seedMirror = firstArg() }

    every { prefs.bodyEnergyPermissionSignature } answers { permissionSignature }
    every { prefs.bodyEnergyPermissionSignature = any() } answers {
        permissionSignature = firstArg()
    }
}
