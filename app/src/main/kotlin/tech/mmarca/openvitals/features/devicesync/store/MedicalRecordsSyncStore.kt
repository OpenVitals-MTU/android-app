package tech.mmarca.openvitals.features.devicesync.store

import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.contract.MedicalRecordsRepository
import tech.mmarca.openvitals.domain.medical.FhirBundleWriter
import tech.mmarca.openvitals.domain.medical.FhirFileParser
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.FhirParseResult
import tech.mmarca.openvitals.domain.medical.FhirSourceGrouper
import tech.mmarca.openvitals.domain.medical.MedicalImportPlanner
import tech.mmarca.openvitals.domain.medical.OpenVitalsFhir
import tech.mmarca.openvitals.domain.medical.PatientCheck
import tech.mmarca.openvitals.domain.medical.PatientCheckResult
import tech.mmarca.openvitals.domain.medical.canonical
import tech.mmarca.openvitals.domain.medical.obj
import tech.mmarca.openvitals.domain.medical.objects
import tech.mmarca.openvitals.domain.medical.resourceType
import tech.mmarca.openvitals.domain.medical.sha256Hex
import tech.mmarca.openvitals.domain.medical.string
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecord
import tech.mmarca.openvitals.domain.model.MedicalRecordSource
import tech.mmarca.openvitals.domain.usecase.ImportMedicalRecordsUseCase
import tech.mmarca.openvitals.features.devicesync.protocol.SyncItem
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRecordStore
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport

/** The wire type name of medical records. Health Connect holds them, but not as a record type the other stores know. */
object MedicalRecordsSyncTypes {
    const val RECORD = "MedicalRecord"
    val all: List<String> = listOf(RECORD)
}

/** Why this phone kept none of the other phone's medical records. */
enum class MedicalHeldBack {
    /** The other phone's Patient records name someone other than the people here. */
    OTHER_PERSON,

    /** The other phone's Patient records name more than one person. */
    SEVERAL_PEOPLE,
}

/**
 * Medical records as a [SyncRecordStore].
 *
 * - One item per record: the entry the FHIR export writes. `fullUrl` holds the source's
 *   base URI; the OpenVitals extension holds its name, app, and FHIR version.
 * - The key is the base URI, the FHIR version, the type, the id, and a hash of the content.
 *   An unchanged record is already present. An edited one replaces the other phone's copy
 *   only when its `meta.lastUpdated` is later, so two phones never swap versions.
 * - The date range does not apply: a vaccination history cut at one year is not useful.
 * - The receiver runs the import pipeline: grouping by source, matching by base URI, and
 *   pre-flight. A source another app holds on this phone is refused, as in an import.
 * - Patient records go first, in one batch. When they name someone other than the people
 *   here, or more than one person, every medical record of the session is held back.
 */
class MedicalRecordsSyncStore(
    private val repository: MedicalRecordsRepository,
    private val today: () -> LocalDate = LocalDate::now,
    private val chunkPayloadByteCap: Int = ChunkPayloadByteCap,
) : SyncRecordStore {

    private val importRecords = ImportMedicalRecordsUseCase(repository)

    /** Set when the Patient check fails. The wizard reads it for the report. */
    @Volatile
    var heldBack: MedicalHeldBack? = null
        private set

    // Read once, with the keys, before any record arrives. The sender reuses them.
    @Volatile
    private var local: LocalRecords? = null

    // Touched only by the receiver, one item at a time.
    private val incomingPatients = mutableListOf<JsonObject>()

    override fun readKeys(types: Set<String>): Flow<String> = flow {
        if (MedicalRecordsSyncTypes.RECORD in types) loadLocal().records.forEach { emit(it.key) }
    }

    override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> = flow {
        if (MedicalRecordsSyncTypes.RECORD !in types) return@flow
        val (patients, others) = loadLocal().records.partition { it.record.ref.resourceType == PATIENT }
        // Every Patient record in the first batch, whatever its size, so the receiver checks them all before writing any.
        if (patients.isNotEmpty()) emit(patients.map(::itemOf))
        val chunk = mutableListOf<SyncItem>()
        var chunkBytes = 0
        for (record in others) {
            val item = itemOf(record)
            chunk += item
            chunkBytes += item.payload.size
            if (chunk.size >= chunkSize.coerceAtLeast(1) || chunkBytes >= chunkPayloadByteCap) {
                emit(chunk.toList())
                chunk.clear()
                chunkBytes = 0
            }
        }
        if (chunk.isNotEmpty()) emit(chunk.toList())
    }

    override fun accepts(item: SyncItem): Boolean {
        if (heldBack != null) return false
        val entry = decode(item.payload) ?: return false
        if (entry.type == PATIENT && !samePeople(entry.resource)) return false
        val local = local ?: return false
        // A copy of a record this phone holds counts as already present, whoever wrote it.
        if (item.key in local.keys) return true
        if (local.otherAppBases.any { FhirSourceGrouper.sameBase(it, entry.baseUri) }) return false
        val identity = entry.identity
        if (identity !in local.editTimes) return true
        // Another version of a record held here. Only a newer edit replaces it, so two phones never swap versions.
        val incoming = editedAt(entry.resource) ?: return false
        return local.editTimes[identity]?.let { incoming.isAfter(it) } ?: true
    }

    override suspend fun writeItems(items: List<SyncItem>): Set<String> {
        // A Patient record accepted before a later one failed the check is held back too.
        if (heldBack != null) return emptySet()
        val entries = items.mapNotNull { item -> decode(item.payload)?.let { item.key to it } }
        if (entries.isEmpty()) return emptySet()
        return try {
            val landed = importEntries(entries.map { it.second })
            entries.filter { (_, entry) -> entry.identity in landed }.mapTo(mutableSetOf()) { it.first }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "medical records write failed: ${e.message}")
            emptySet()
        }
    }

    /** Runs the import pipeline on the entries, as one Bundle, and returns what landed. */
    private suspend fun importEntries(entries: List<IncomingEntry>): Set<RecordIdentity> {
        val bundle = buildJsonObject {
            put("resourceType", "Bundle")
            put("type", "collection")
            put("entry", JsonArray(entries.map { it.entry }))
        }
        val file = (FhirFileParser.parse(bundle.toString()) as? FhirParseResult.Parsed)?.file ?: return emptySet()
        val analysis = FhirImportAnalyzer.analyze(file, SyncFileName)
        val own = repository.ownSources()
        val ownIds = own.mapTo(mutableSetOf()) { it.id }
        val others = sourcesOrEmpty().filter { it.id !in ownIds }
        val result = importRecords(MedicalImportPlanner.plan(analysis, own, others), today())
        result.stoppedBy?.let { Log.w(TAG, "medical records sync stopped: $it") }
        return result.groups.flatMapTo(mutableSetOf()) { group ->
            val base = group.plan.group.baseUri ?: return@flatMapTo emptyList()
            // An entry without a stated version was guessed; it matches as unstated.
            val version = group.plan.fhirVersion.takeIf { group.plan.group.versionStated }
            group.refs.map { RecordIdentity.of(base, version, it.resourceType, it.resourceId) }
        }
    }

    /** False, and every later record held back, when the people do not match. */
    private fun samePeople(patient: JsonObject): Boolean {
        heldBack = when (PatientCheck.check(incomingPatients + patient, local?.patients.orEmpty())) {
            is PatientCheckResult.Mismatch -> MedicalHeldBack.OTHER_PERSON
            is PatientCheckResult.SeveralPeople -> MedicalHeldBack.SEVERAL_PEOPLE
            else -> null
        }
        if (heldBack != null) return false
        incomingPatients += patient
        return true
    }

    private suspend fun loadLocal(): LocalRecords = local ?: readLocal().also { local = it }

    private suspend fun readLocal(): LocalRecords {
        val own = repository.ownSources()
        val sources = (sourcesOrEmpty() + own).distinctBy { it.id }
        val byId = sources.associateBy { it.id }
        val ownIds = own.mapTo(mutableSetOf()) { it.id }
        val records = MedicalCategory.entries.flatMap { readCategory(it) }
            // A record whose source this app cannot see has no origin to send.
            .mapNotNull { record -> byId[record.ref.dataSourceId]?.let { LocalRecord.of(record, it) } }
        return LocalRecords(
            records = records,
            otherAppBases = sources.filter { it.id !in ownIds }.map { it.fhirBaseUri },
        )
    }

    private fun itemOf(record: LocalRecord): SyncItem = SyncItem(
        key = record.key,
        recordType = MedicalRecordsSyncTypes.RECORD,
        payload = FhirBundleWriter.entry(record.record, record.source).toString().toByteArray(Charsets.UTF_8),
    )

    /** Own records in a declined category, as the export reads them. No access at all is no records. */
    private suspend fun readCategory(category: MedicalCategory): List<MedicalRecord> = try {
        repository.readCategory(category)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!e.isPermissionFailure()) throw e
        emptyList()
    }

    private suspend fun sourcesOrEmpty(): List<MedicalRecordSource> =
        runCatching { repository.allSources() }.getOrElse { if (it is CancellationException) throw it else emptyList() }

    private class LocalRecord(val record: MedicalRecord, val source: MedicalRecordSource, val resource: JsonObject) {
        val identity = RecordIdentity.of(source.fhirBaseUri, source.fhirVersion, record.ref.resourceType, record.ref.resourceId)
        val key = medicalRecordKey(identity, resource)

        companion object {
            fun of(record: MedicalRecord, source: MedicalRecordSource): LocalRecord? =
                runCatching { Json.parseToJsonElement(record.json) as? JsonObject }.getOrNull()?.let { LocalRecord(record, source, it) }
        }
    }

    private class LocalRecords(val records: List<LocalRecord>, val otherAppBases: List<String>) {
        val keys: Set<String> = records.mapTo(mutableSetOf()) { it.key }
        val editTimes: Map<RecordIdentity, Instant?> = records.associate { it.identity to editedAt(it.resource) }
        val patients: List<JsonObject> = records.filter { it.record.ref.resourceType == PATIENT }.map { it.resource }
    }

    private companion object {
        const val TAG = "DeviceSync"
        const val PATIENT = "Patient"

        /** Names a source only when a record has no origin, which a sent record always has. */
        const val SyncFileName = "device-sync.json"
    }
}

/** One record's place: source base URI, FHIR version, type, and id. Made with [of], which normalizes the base. */
internal data class RecordIdentity(val baseUri: String, val fhirVersion: String?, val type: String, val id: String) {
    companion object {
        fun of(baseUri: String, fhirVersion: String?, type: String, id: String) =
            RecordIdentity(baseUri.trimEnd('/').lowercase(), fhirVersion, type, id)
    }
}

/** A received entry, read far enough to route it. */
private class IncomingEntry(val entry: JsonObject, val resource: JsonObject, val type: String, val baseUri: String) {
    val identity: RecordIdentity
        get() = RecordIdentity.of(baseUri, sourceFhirVersion(entry), type, resource.string("id").orEmpty())
}

/** Null for a payload that is not an entry with an origin. Such an item is refused. */
private fun decode(payload: ByteArray): IncomingEntry? {
    val entry = runCatching { Json.parseToJsonElement(payload.toString(Charsets.UTF_8)) as? JsonObject }.getOrNull() ?: return null
    val resource = entry["resource"] as? JsonObject ?: return null
    val type = resource.resourceType ?: return null
    val base = entry.string("fullUrl")?.let { FhirSourceGrouper.baseOf(it, type) } ?: return null
    return IncomingEntry(entry, resource, type, base)
}

private fun sourceFhirVersion(entry: JsonObject): String? =
    entry.objects("extension").firstOrNull { it.string("url") == OpenVitalsFhir.SourceExtensionUrl }
        ?.objects("extension")?.firstOrNull { it.string("url") == OpenVitalsFhir.SourceFhirVersionUrl }
        ?.string("valueString")

/** The same record on both phones gets the same key. The content hash ignores key order and spacing. */
internal fun medicalRecordKey(identity: RecordIdentity, resource: JsonObject): String =
    listOf("medical", identity.baseUri, identity.fhirVersion, identity.type, identity.id, sha256Hex(resource.canonical())).joinToString("|")

/** `meta.lastUpdated`: when the record was last edited, if it says. */
private fun editedAt(resource: JsonObject): Instant? =
    resource.obj("meta")?.string("lastUpdated")?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

/** The medical part of the English sync report. Empty when no medical record came in. */
fun medicalSyncReportText(report: SyncReport, heldBack: MedicalHeldBack?): String = buildString {
    val summary = report.typeSummaries.firstOrNull { it.recordType == MedicalRecordsSyncTypes.RECORD }
    if (summary == null && heldBack == null) return@buildString
    appendLine()
    appendLine("Medical records")
    summary?.let {
        val rejected = it.received - it.imported - it.duplicateSkipped - it.refused
        appendLine("Written ${it.imported}, already present ${it.duplicateSkipped}, skipped ${it.refused}, rejected $rejected")
    }
    when (heldBack) {
        MedicalHeldBack.OTHER_PERSON -> appendLine("Held back: the other phone's Patient records name someone else.")
        MedicalHeldBack.SEVERAL_PEOPLE -> appendLine("Held back: the other phone's Patient records name more than one person.")
        null -> return@buildString
    }
    appendLine("To move them anyway, export them on that phone and import them here.")
}
