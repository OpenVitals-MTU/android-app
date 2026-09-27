package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.cycle.CycleTrackingProfile
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.features.cycle.CycleJournalJson
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore

/** The wire type names of the cycle journal. It lives in Room; Health Connect has no record type for it. */
object CycleJournalSyncTypes {
    const val ENTRY = "CycleJournalEntry"
    const val EXCLUSION = "CycleExclusion"
    const val PROFILE = "CycleTrackingProfile"
    val all: List<String> = listOf(ENTRY, EXCLUSION, PROFILE)
}

/**
 * The cycle journal's [SyncRecordStore]: one item per day log, one per
 * excluded cycle, and one for the declared contexts and age band. Keys are
 * content fingerprints, so an unchanged day is "already present". A day
 * edited on both phones keeps the later edit. The contexts and age band
 * move only to a phone that has declared none. Reminder settings stay
 * local: they are alarms on one phone.
 */
class CycleJournalSyncStore(
    private val journal: CycleJournalRepository,
    private val preferences: CyclePreferences,
    private val windowStart: Instant,
    private val windowEnd: Instant,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : SyncRecordStore {

    /** This phone's edit time per day, read with the keys, so [accepts] can prefer the newer side. */
    @Volatile
    private var localUpdatedAt: Map<LocalDate, Instant> = emptyMap()

    override fun readKeys(types: Set<String>): Flow<String> = flow {
        if (CycleJournalSyncTypes.ENTRY in types) {
            val entries = entriesInWindow()
            localUpdatedAt = entries.associate { it.date to it.updatedAt }
            entries.forEach { emit(entryKey(it)) }
        }
        if (CycleJournalSyncTypes.EXCLUSION in types) {
            exclusionsInWindow().forEach { (start, reason) -> emit(exclusionKey(start, reason)) }
        }
        if (CycleJournalSyncTypes.PROFILE in types) {
            profileItem(preferences.cycleTrackingProfile())?.let { emit(it.key) }
        }
    }

    override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = flow {
        val items = buildList {
            if (CycleJournalSyncTypes.ENTRY in types) entriesInWindow().forEach { add(entryItem(it)) }
            if (CycleJournalSyncTypes.EXCLUSION in types) {
                exclusionsInWindow().forEach { (start, reason) -> add(exclusionItem(start, reason)) }
            }
            if (CycleJournalSyncTypes.PROFILE in types) profileItem(preferences.cycleTrackingProfile())?.let { add(it) }
        }
        items.chunked(chunkSize.coerceAtLeast(1)).forEach { emit(it) }
    }

    /** The window this phone's user chose, and for a day log, the newer edit. */
    override fun accepts(item: SyncItem): Boolean = when (item.recordType) {
        CycleJournalSyncTypes.ENTRY -> decodeEntry(item.payload)?.let { entry ->
            inWindow(entry.date) && localUpdatedAt[entry.date]?.let { entry.updatedAt.isAfter(it) } ?: true
        } ?: true
        CycleJournalSyncTypes.EXCLUSION -> decodeExclusion(item.payload)?.let { inWindow(it.first) } ?: true
        CycleJournalSyncTypes.PROFILE -> preferences.cycleTrackingProfile().let { it.contexts.isEmpty() && it.ageBand == null }
        else -> false
    }

    override suspend fun writeItems(items: List<SyncItem>): Set<String> {
        val written = mutableSetOf<String>()
        for (item in items) {
            try {
                val landed = when (item.recordType) {
                    CycleJournalSyncTypes.ENTRY -> decodeEntry(item.payload)?.let { entry ->
                        journal.restore(entry)
                        localUpdatedAt = localUpdatedAt + (entry.date to entry.updatedAt)
                        true
                    } ?: false
                    CycleJournalSyncTypes.EXCLUSION -> decodeExclusion(item.payload)?.let { (start, reason) ->
                        // The same start twice is one row: the cycle spans a single start date.
                        journal.exclude(start, start, reason)
                        true
                    } ?: false
                    CycleJournalSyncTypes.PROFILE -> decodeProfile(item.payload)?.let { profile ->
                        preferences.setCycleTrackingProfile(profile)
                        true
                    } ?: false
                    else -> false
                }
                if (landed) written += item.key else Log.w(TAG, "skipping undecodable ${item.recordType}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "write failed for ${item.recordType}: ${e.message}")
            }
        }
        return written
    }

    private suspend fun entriesInWindow(): List<CycleJournalEntry> =
        journal.allEntries().filter { it.hasObservations && inWindow(it.date) }

    private suspend fun exclusionsInWindow(): List<Pair<LocalDate, CycleExclusionReason?>> =
        journal.exclusions().entries.filter { inWindow(it.key) }.map { it.key to it.value }.sortedBy { it.first }

    private fun inWindow(date: LocalDate): Boolean {
        val start = date.atStartOfDay(zone).toInstant()
        return !start.isBefore(windowStart) && !start.isAfter(windowEnd.plus(WindowEndSlack))
    }

    private companion object {
        const val TAG = "DeviceSync"

        /** The window ends when Start sync is pressed. The other phone's clock may run ahead. */
        val WindowEndSlack: Duration = Duration.ofDays(1)
    }
}

// Items and keys. Kept as top-level functions so a test can check both phones agree.

internal fun entryItem(entry: CycleJournalEntry): SyncItem =
    SyncItem(key = entryKey(entry), recordType = CycleJournalSyncTypes.ENTRY, payload = encodeEntry(entry))

internal fun exclusionItem(start: LocalDate, reason: CycleExclusionReason?): SyncItem =
    SyncItem(key = exclusionKey(start, reason), recordType = CycleJournalSyncTypes.EXCLUSION, payload = encodeExclusion(start, reason))

/** Nothing declared is nothing to send. */
internal fun profileItem(profile: CycleTrackingProfile): SyncItem? {
    if (profile.contexts.isEmpty() && profile.ageBand == null) return null
    return SyncItem(key = profileKey(profile), recordType = CycleJournalSyncTypes.PROFILE, payload = encodeProfile(profile))
}

/** The content, not the edit time: the same observations on both phones are one record. */
internal fun entryKey(entry: CycleJournalEntry): String = fingerprintOf(
    listOf(
        entry.date, entry.bleedingNone, entry.painLevel, entry.moodLevel, entry.energyLevel,
        entry.symptoms.map { it.id }.sorted(), entry.notes, entry.hcgTest?.name,
        entry.bbtDisturbances.map { it.name }.sorted(), entry.cervicalSensation?.name,
    ),
)

internal fun exclusionKey(start: LocalDate, reason: CycleExclusionReason?): String =
    fingerprintOf(listOf("exclusion", start, reason?.name))

internal fun profileKey(profile: CycleTrackingProfile): String =
    fingerprintOf(listOf("profile", profile.contexts.map { it.id }.sorted(), profile.ageBand?.id))

/** The same `sync_<hex>` shape Health Connect records use, so one report reads alike. */
private fun fingerprintOf(parts: List<Any?>): String {
    val joined = parts.joinToString("|") { it?.toString() ?: "" }
    val digest = MessageDigest.getInstance("SHA-256").digest(joined.toByteArray(Charsets.UTF_8))
    val hex = buildString(32) {
        for (index in 0 until 16) {
            val byte = digest[index].toInt() and 0xFF
            append(HEX_DIGITS[byte ushr 4])
            append(HEX_DIGITS[byte and 0x0F])
        }
    }
    return "$SYNC_CLIENT_RECORD_ID_PREFIX$hex"
}

private const val HEX_DIGITS = "0123456789abcdef"

private val JournalJson = Json { ignoreUnknownKeys = true }

private fun bytes(json: JsonObject): ByteArray = json.toString().toByteArray(Charsets.UTF_8)

private fun objectOrNull(payload: ByteArray): JsonObject? =
    runCatching { JournalJson.parseToJsonElement(payload.toString(Charsets.UTF_8)).jsonObject }.getOrNull()

internal fun encodeEntry(entry: CycleJournalEntry): ByteArray = bytes(CycleJournalJson.entry(entry))

/** Null for a payload this build cannot read; the caller skips it. */
internal fun decodeEntry(payload: ByteArray): CycleJournalEntry? = objectOrNull(payload)?.let(CycleJournalJson::entryOrNull)

internal fun encodeExclusion(start: LocalDate, reason: CycleExclusionReason?): ByteArray = bytes(CycleJournalJson.exclusion(start, reason))

internal fun decodeExclusion(payload: ByteArray): Pair<LocalDate, CycleExclusionReason?>? =
    objectOrNull(payload)?.let(CycleJournalJson::exclusionOrNull)

internal fun encodeProfile(profile: CycleTrackingProfile): ByteArray = bytes(CycleJournalJson.profile(profile))

internal fun decodeProfile(payload: ByteArray): CycleTrackingProfile? = objectOrNull(payload)?.let(CycleJournalJson::profileOrNull)
